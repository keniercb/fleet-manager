package com.fleet.management.service.impl;

import com.fleet.management.client.enzona.EnzonaQrClient;
import com.fleet.management.client.enzona.dto.EnzonaQrMerchantResponse;
import com.fleet.management.config.PaymentConfig;
import com.fleet.management.dto.payment.PaymentCreateRequest;
import com.fleet.management.dto.payment.PaymentNotificationDto;
import com.fleet.management.dto.payment.PaymentResponse;
import com.fleet.management.dto.subscription.SubscriptionCreateRequest;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.*;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.repository.PaymentRepository;
import com.fleet.management.repository.PlanRepository;
import com.fleet.management.repository.SubscriptionRepository;
import com.fleet.management.service.PaymentService;
import com.fleet.management.service.SubscriptionService;
import com.fleet.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final int MAX_RETRIES = 3;
    private static final List<PaymentStatus> ACTIVE_STATUSES = List.of(PaymentStatus.PENDIENTE, PaymentStatus.QR_GENERADO);

    private final PaymentRepository paymentRepository;
    private final EmpresaRepository empresaRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EnzonaQrClient enzonaQrClient;
    private final PaymentConfig paymentConfig;
    private final SubscriptionService subscriptionService;

    @Override
    @Transactional
    public PaymentResponse crearPago(PaymentCreateRequest request) {
        Long empresaId = SecurityUtils.resolveEmpresaId();

        // Validar que no exista un pago activo para la misma empresa y tipo
        Optional<Payment> pagoActivo = paymentRepository.findActivoByEmpresaAndType(
                empresaId, request.getType(), ACTIVE_STATUSES);
        if (pagoActivo.isPresent()) {
            throw new BusinessException("Ya existe un pago activo en estado "
                    + pagoActivo.get().getStatus() + " para este tipo de operacion");
        }

        // Validar tipo UPGRADE/RENOVACION requiere subscriptionId
        if ((request.getType() == PaymentType.RENOVACION || request.getType() == PaymentType.UPGRADE)
                && request.getSubscriptionId() == null) {
            throw new BusinessException("El tipo " + request.getType()
                    + " requiere una suscripcion existente");
        }

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.getPlanId()));

        // Resolver suscripcion si aplica
        Subscription subscription = null;
        if (request.getSubscriptionId() != null) {
            subscription = subscriptionRepository.findById(request.getSubscriptionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", request.getSubscriptionId()));
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", empresaId));

        String description = "Fleet Management - Suscripcion " + plan.getNombre() + " - Empresa " + empresa.getNombre();

        Payment payment = Payment.builder()
                .empresa(empresa)
                .plan(plan)
                .subscription(subscription)
                .amount(plan.getPrecioMensual())
                .currency(paymentConfig.getCurrency())
                .description(description)
                .status(PaymentStatus.PENDIENTE)
                .type(request.getType())
                .expiresAt(LocalDateTime.now().plusHours(paymentConfig.getQrTimeoutHours()))
                .retryCount(0)
                .activo(true)
                .build();

        payment = paymentRepository.save(payment);

        // Generar QR con Enzona
        try {
            EnzonaQrMerchantResponse qrResponse = enzonaQrClient.crearQrMerchant(
                    plan.getPrecioMensual(), description);

            payment.setQrCode(qrResponse.getVendorIdentityCode());
            payment.setQrImageBase64(qrResponse.getImage());
            payment.setStatus(PaymentStatus.QR_GENERADO);
            payment = paymentRepository.save(payment);

            log.info("QR generado exitosamente para pago {}: {}", payment.getId(), payment.getQrCode());
        } catch (Exception e) {
            log.error("Error al generar QR para pago {}: {}", payment.getId(), e.getMessage());
            payment.setStatus(PaymentStatus.FALLIDO);
            payment.setErrorMessage(e.getMessage());
            payment = paymentRepository.save(payment);
        }

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse findActiveByMyCompany() {
        Long empresaId = SecurityUtils.resolveEmpresaId();
        List<Payment> activos = paymentRepository.findActivosByEmpresaAndStatusIn(empresaId, ACTIVE_STATUSES);
        if (activos.isEmpty()) {
            throw new ResourceNotFoundException("Payment", "empresaId", empresaId);
        }
        return toResponse(activos.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> findByEmpresa(Long empresaId, String status, Pageable pageable) {
        Page<Payment> page;
        if (status != null && !status.isBlank()) {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            page = paymentRepository.findByEmpresaIdAndStatus(empresaId, paymentStatus, pageable);
        } else {
            page = paymentRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable);
        }
        return new PageImpl<>(
                page.getContent().stream().map(this::toResponse).toList(),
                pageable, page.getTotalElements());
    }

    @Override
    @Transactional
    public PaymentResponse cancelar(Long id) {
        try {
            Payment payment = paymentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

            if (payment.getStatus() != PaymentStatus.PENDIENTE
                    && payment.getStatus() != PaymentStatus.QR_GENERADO) {
                throw new BusinessException("Solo se pueden cancelar pagos en estado PENDIENTE o QR_GENERADO");
            }

            payment.setStatus(PaymentStatus.CANCELADO);
            return toResponse(paymentRepository.save(payment));
        } catch (OptimisticLockingFailureException ex) {
            throw new BusinessException("El pago fue modificado por otro proceso. Intente nuevamente.");
        }
    }

    @Override
    @Transactional
    public PaymentResponse reintentar(Long id) {
        try {
            Payment payment = paymentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

            if (payment.getStatus() != PaymentStatus.FALLIDO) {
                throw new BusinessException("Solo se pueden reintentar pagos en estado FALLIDO");
            }

            if (payment.getRetryCount() >= MAX_RETRIES) {
                throw new BusinessException("Se ha alcanzado el maximo de " + MAX_RETRIES + " reintentos permitidos");
            }

            payment.setRetryCount(payment.getRetryCount() + 1);
            payment.setErrorMessage(null);
            payment.setStatus(PaymentStatus.PENDIENTE);
            paymentRepository.save(payment);

            // Re-generar QR
            String description = "Fleet Management - Suscripcion " + payment.getPlan().getNombre();
            EnzonaQrMerchantResponse qrResponse = enzonaQrClient.crearQrMerchant(
                    payment.getAmount(), description);

            payment.setQrCode(qrResponse.getVendorIdentityCode());
            payment.setQrImageBase64(qrResponse.getImage());
            payment.setStatus(PaymentStatus.QR_GENERADO);
            payment.setExpiresAt(LocalDateTime.now().plusHours(paymentConfig.getQrTimeoutHours()));

            return toResponse(paymentRepository.save(payment));
        } catch (BusinessException e) {
            throw e;
        } catch (OptimisticLockingFailureException ex) {
            throw new BusinessException("El pago fue modificado por otro proceso. Intente nuevamente.");
        } catch (Exception e) {
            log.error("Error al reintentar pago {}: {}", id, e.getMessage());
            // Marcar como FALLIDO de nuevo
            Payment payment = paymentRepository.findById(id).orElseThrow();
            payment.setStatus(PaymentStatus.FALLIDO);
            payment.setErrorMessage(e.getMessage());
            paymentRepository.save(payment);
            throw new BusinessException("Error al reintentar la generacion del QR: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void procesarNotificacion(PaymentNotificationDto notification) {
        if (notification.getQrCode() == null || notification.getQrCode().isBlank()) {
            log.warn("Notificacion de Enzona sin qrCode");
            return;
        }

        Payment payment = paymentRepository.findByQrCode(notification.getQrCode()).orElse(null);
        if (payment == null) {
            log.warn("No se encontro pago con qrCode: {}", notification.getQrCode());
            return;
        }

        // Idempotencia: si ya fue procesado, no hacer nada
        if (payment.getStatus() == PaymentStatus.PAGADO) {
            log.info("Pago {} ya fue procesado como PAGADO, ignorando notificacion duplicada", payment.getId());
            return;
        }

        // Validar monto
        if (notification.getAmount() != null
                && notification.getAmount().compareTo(payment.getAmount()) != 0) {
            log.error("Monto mismatch en notificacion: esperado={}, recibido={}",
                    payment.getAmount(), notification.getAmount());
            return;
        }

        // Marcar como pagado
        payment.setStatus(PaymentStatus.PAGADO);
        payment.setPaidAt(LocalDateTime.now());
        payment.setExternalTransactionId(notification.getTransactionId());
        paymentRepository.save(payment);

        log.info("Pago {} marcado como PAGADO via webhook, txn={}",
                payment.getId(), notification.getTransactionId());

        // Ejecutar accion post-pago
        ejecutarAccionPostPago(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse consultarEstadoExterno(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

        if (payment.getQrCode() == null) {
            return toResponse(payment);
        }

        try {
            Object pagos = enzonaQrClient.consultarPagos(payment.getQrCode());
            // Si la API retorna datos, el pago fue completado
            boolean pagadoEnzona = pagos != null;

            return PaymentResponse.builder()
                    .id(payment.getId())
                    .status(payment.getStatus())
                    .qrCode(payment.getQrCode())
                    .amount(payment.getAmount())
                    .externalStatus(pagadoEnzona ? "COMPLETED" : "PENDING")
                    .confirmed(payment.getStatus() == PaymentStatus.PAGADO || pagadoEnzona)
                    .build();
        } catch (Exception e) {
            log.error("Error al consultar estado externo del pago {}", id, e);
            return toResponse(payment);
        }
    }

    @Override
    @Transactional
    public void expirarPagosVencidos() {
        List<Payment> expirados = paymentRepository.findExpiredPayments(
                PaymentStatus.QR_GENERADO, LocalDateTime.now());
        for (Payment p : expirados) {
            p.setStatus(PaymentStatus.EXPIRADO);
            paymentRepository.save(p);
            log.info("Pago {} expirado (QR: {})", p.getId(), p.getQrCode());
        }
    }

    @Override
    @Transactional
    public void pollingPagosPendientes() {
        List<Payment> pendientes = paymentRepository.findPendingQrPayments(
                PaymentStatus.QR_GENERADO, LocalDateTime.now());
        for (Payment p : pendientes) {
            try {
                Object pagos = enzonaQrClient.consultarPagos(p.getQrCode());
                if (pagos != null) {
                    p.setStatus(PaymentStatus.PAGADO);
                    p.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(p);
                    log.info("Pago {} confirmado via polling", p.getId());
                    ejecutarAccionPostPago(p);
                }
            } catch (Exception e) {
                log.error("Error en polling de pago {}: {}", p.getId(), e.getMessage());
            }
        }
    }

    // ---- Private helpers ----

    private void ejecutarAccionPostPago(Payment payment) {
        try {
            Long empresaId = payment.getEmpresa().getId();
            Long planId = payment.getPlan().getId();

            switch (payment.getType()) {
                case NUEVA_SUSCRIPCION -> {
                    SubscriptionCreateRequest req = SubscriptionCreateRequest.builder()
                            .empresaId(empresaId)
                            .planId(planId)
                            .build();
                    subscriptionService.create(req);
                    log.info("Suscripcion creada para empresa {} (plan {}) por pago {}",
                            empresaId, planId, payment.getId());
                }
                case RENOVACION -> {
                    if (payment.getSubscription() != null) {
                        Subscription sub = subscriptionRepository.findById(payment.getSubscription().getId())
                                .orElse(null);
                        if (sub != null) {
                            sub.setEndDate(sub.getEndDate().plusDays(payment.getPlan().getDuracion()));
                            sub.setStatus(SubscriptionStatus.ACTIVE);
                            subscriptionRepository.save(sub);
                            log.info("Suscripcion {} renovada por pago {}", sub.getId(), payment.getId());
                        }
                    }
                }
                case UPGRADE -> {
                    if (payment.getSubscription() != null) {
                        Subscription sub = subscriptionRepository.findById(payment.getSubscription().getId())
                                .orElse(null);
                        if (sub != null) {
                            sub.setPlan(payment.getPlan());
                            sub.setMaxVehiculos(payment.getPlan().getMaxVehiculos());
                            sub.setMaxUsuarios(payment.getPlan().getMaxUsuarios());
                            subscriptionRepository.save(sub);
                            log.info("Suscripcion {} upgradeada al plan {} por pago {}",
                                    sub.getId(), payment.getPlan().getNombre(), payment.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al ejecutar accion post-pago para pago {}: {}",
                    payment.getId(), e.getMessage(), e);
        }
    }

    private PaymentResponse toResponse(Payment p) {
        PaymentResponse.EmpresaResumida empresa = null;
        if (p.getEmpresa() != null) {
            empresa = PaymentResponse.EmpresaResumida.builder()
                    .id(p.getEmpresa().getId())
                    .codigo(p.getEmpresa().getCodigo())
                    .nombre(p.getEmpresa().getNombre())
                    .build();
        }

        PaymentResponse.PlanResumido plan = null;
        if (p.getPlan() != null) {
            plan = PaymentResponse.PlanResumido.builder()
                    .id(p.getPlan().getId())
                    .nombre(p.getPlan().getNombre())
                    .precioMensual(p.getPlan().getPrecioMensual())
                    .build();
        }

        return PaymentResponse.builder()
                .id(p.getId())
                .empresa(empresa)
                .plan(plan)
                .subscriptionId(p.getSubscription() != null ? p.getSubscription().getId() : null)
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .description(p.getDescription())
                .status(p.getStatus())
                .type(p.getType())
                .qrCode(p.getQrCode())
                .qrImageBase64(p.getQrImageBase64())
                .externalTransactionId(p.getExternalTransactionId())
                .paidAt(p.getPaidAt())
                .expiresAt(p.getExpiresAt())
                .errorMessage(p.getErrorMessage())
                .retryCount(p.getRetryCount())
                .activo(p.getActivo())
                .fechaCreacion(p.getFechaCreacion())
                .fechaActualizacion(p.getFechaActualizacion())
                .build();
    }
}
