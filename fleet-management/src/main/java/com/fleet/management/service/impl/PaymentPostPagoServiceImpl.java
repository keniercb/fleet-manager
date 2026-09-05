package com.fleet.management.service.impl;

import com.fleet.management.dto.subscription.SubscriptionCreateRequest;
import com.fleet.management.model.Payment;
import com.fleet.management.model.Subscription;
import com.fleet.management.model.SubscriptionStatus;
import com.fleet.management.repository.SubscriptionRepository;
import com.fleet.management.service.PaymentPostPagoService;
import com.fleet.management.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Implementación de {@link PaymentPostPagoService} que ejecuta la acción post-pago
 * en una transacción nueva (REQUIRES_NEW).
 *
 * <p>Si esta acción falla, la transacción original que marcó el pago como PAGADO
 * ya quedó commiteada y puede reintentarse posteriormente (vía scheduler de
 * reconciliación o DLQ). De este modo evitamos el problema descrito en FX-05:
 * "si la acción falla, el rollback deja el pago en QR_GENERADO pero Enzona ya
 * confirma → dinero sin suscripción".
 *
 * <p>FX-26: RENOVACION con endDate ya expirado usa max(endDate, today) en lugar
 * de sumar duracion a un endDate pasado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPostPagoServiceImpl implements PaymentPostPagoService {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ejecutar(Payment payment) {
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
                            LocalDate baseDate = sub.getEndDate() != null
                                    && sub.getEndDate().isAfter(LocalDate.now())
                                    ? sub.getEndDate()
                                    : LocalDate.now();
                            sub.setEndDate(baseDate.plusDays(payment.getPlan().getDuracion()));
                            sub.setStatus(SubscriptionStatus.ACTIVE);
                            subscriptionRepository.save(sub);
                            log.info("Suscripcion {} renovada por pago {} (nueva endDate={})",
                                    sub.getId(), payment.getId(), sub.getEndDate());
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
            // No relanzamos para no romper la tx original (que ya commiteó el PAGADO).
            // El scheduler de reconciliación (T-PAY-15) detectará la divergencia
            // y podrá reintentar la acción post-pago.
        }
    }
}
