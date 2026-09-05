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
import com.fleet.management.service.PaymentErrorRecoveryService;
import com.fleet.management.service.PaymentPostPagoService;
import com.fleet.management.service.PaymentService;
import com.fleet.management.service.SubscriptionService;
import com.fleet.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
    // FX-05: servicio separado para ejecutar la accion post-pago en REQUIRES_NEW
    private final PaymentPostPagoService paymentPostPagoService;
    // FX-21: servicio separado para persistir estado FALLIDO en REQUIRES_NEW
    private final PaymentErrorRecoveryService paymentErrorRecoveryService;

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
        validateOwnership(payment);
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
        // FX-02: validacion IDOR: solo SUPER_ADMIN puede consultar pagos de cualquier empresa;
        // ADMIN/USER solo pueden consultar los de su propia empresa.
        Long currentUserEmpresaId = SecurityUtils.resolveEmpresaId();
        if (!empresaId.equals(currentUserEmpresaId) && !isSuperAdmin()) {
            throw new BusinessException("No tiene permisos para consultar pagos de otra empresa");
        }
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
            validateOwnership(payment);

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
            validateOwnership(payment);

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
            // FX-21: persistir estado FALLIDO en tx nueva (REQUIRES_NEW) para
            // evitar que el rollback de la tx original deje el pago en estado
            // intermedio. El bean separado evita el problema de self-invocation.
            paymentErrorRecoveryService.markAsFailed(id, e.getMessage());
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

        // FX-06: Idempotencia DB. Si externalTransactionId ya existe en otro pago,
        // el INSERT/UPDATE violara la constraint uk_payment_external_txn_id.
        // Pre-verificamos para retornar idempotente sin lanzar excepcion.
        if (notification.getTransactionId() != null && !notification.getTransactionId().isBlank()) {
            boolean txnYaProcesada = paymentRepository
                    .existsByExternalTransactionIdAndIdNot(notification.getTransactionId(), payment.getId());
            if (txnYaProcesada) {
                log.info("Notificacion con transactionId={} ya procesada en otro pago, ignorando",
                        notification.getTransactionId());
                return;
            }
        }

        // Validar monto
        if (notification.getAmount() != null
                && notification.getAmount().compareTo(payment.getAmount()) != 0) {
            log.error("Monto mismatch en notificacion: esperado={}, recibido={}",
                    payment.getAmount(), notification.getAmount());
            return;
        }

        // Validar moneda si viene en la notificacion
        if (notification.getCurrency() != null && !notification.getCurrency().isBlank()
                && !notification.getCurrency().equalsIgnoreCase(payment.getCurrency())) {
            log.error("Moneda mismatch en notificacion: esperada={}, recibida={}",
                    payment.getCurrency(), notification.getCurrency());
            return;
        }

        // Marcar como pagado
        payment.setStatus(PaymentStatus.PAGADO);
        payment.setPaidAt(LocalDateTime.now());
        payment.setExternalTransactionId(notification.getTransactionId());
        try {
            payment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            // FX-06: la constraint uk_payment_external_txn_id se violo por concurrencia
            // (otra notificacion con mismo txnId proceso el pago primero).
            log.warn("Conflicto de idempotencia al procesar notificacion (txnId={}): {}",
                    notification.getTransactionId(), ex.getMessage());
            return;
        }

        log.info("Pago {} marcado como PAGADO via webhook, txn={}",
                payment.getId(), notification.getTransactionId());

        // FX-05: ejecutar accion post-pago en tx nueva (REQUIRES_NEW).
        // Si la accion falla, el pago queda PAGADO y la accion puede reintentarse
        // posteriormente (scheduler de reconciliacion T-PAY-15).
        paymentPostPagoService.ejecutar(payment);
    }

    @Override
    @Transactional
    public PaymentResponse consultarEstadoExterno(Long id) {
        // FX-24: este método ahora persiste el estado si detecta pago confirmado
        // en Enzona. Antes estaba marcado @Transactional(readOnly=true) lo cual
        // era contradictorio con el comportamiento de actualización.
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        validateOwnership(payment);

        if (payment.getQrCode() == null) {
            return toResponse(payment);
        }

        try {
            Object pagos = enzonaQrClient.consultarPagos(payment.getQrCode());
            boolean pagadoEnzona = pagos != null;

            // FX-24: si Enzona confirma el pago y el estado local no es PAGADO,
            // persistir la actualización y ejecutar acción post-pago.
            if (pagadoEnzona && payment.getStatus() != PaymentStatus.PAGADO) {
                payment.setStatus(PaymentStatus.PAGADO);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
                log.info("Pago {} confirmado como PAGADO via consulta manual", payment.getId());
                paymentPostPagoService.ejecutar(payment);
            }

            return PaymentResponse.builder()
                    .id(payment.getId())
                    .status(payment.getStatus())
                    .qrCode(payment.getQrCode())
                    .amount(payment.getAmount())
                    .externalStatus(pagadoEnzona ? "COMPLETED" : "PENDING")
                    .confirmed(payment.getStatus() == PaymentStatus.PAGADO)
                    .build();
        } catch (Exception e) {
            log.error("Error al consultar estado externo del pago {}", id, e);
            return toResponse(payment);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse consultarEstadoLocal(Long id) {
        // FX-24: método de solo lectura, sin side-effects.
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        validateOwnership(payment);
        return toResponse(payment);
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
                    // FX-05: ejecutar accion post-pago en tx nueva
                    paymentPostPagoService.ejecutar(p);
                }
            } catch (Exception e) {
                log.error("Error en polling de pago {}: {}", p.getId(), e.getMessage());
            }
        }
    }

    // ---- Private helpers ----

    /**
     * FX-02: valida que el pago pertenezca a la empresa del usuario autenticado.
     * SUPER_ADMIN puede operar sobre pagos de cualquier empresa.
     */
    private void validateOwnership(Payment payment) {
        if (isSuperAdmin()) {
            return;
        }
        Long currentUserEmpresaId = SecurityUtils.resolveEmpresaId();
        if (payment.getEmpresa() == null
                || !currentUserEmpresaId.equals(payment.getEmpresa().getId())) {
            throw new BusinessException("No tiene permisos para operar sobre este pago");
        }
    }

    /**
     * FX-02: determina si el usuario autenticado tiene rol SUPER_ADMIN.
     */
    private boolean isSuperAdmin() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
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
