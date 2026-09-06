package com.fleet.management.service.impl;

import com.fleet.management.model.Payment;
import com.fleet.management.model.PaymentStatus;
import com.fleet.management.repository.PaymentRepository;
import com.fleet.management.service.PaymentErrorRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link PaymentErrorRecoveryService} que persiste el estado
 * FALLIDO en una transacción nueva (REQUIRES_NEW).
 *
 * <p>FX-21: soluciona el problema donde el catch {@code Exception} dentro de
 * {@code PaymentServiceImpl.reintentar} intentaba persistir el pago como FALLIDO
 * pero la transacción ya estaba marcada para rollback, dejando el pago en estado
 * intermedio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentErrorRecoveryServiceImpl implements PaymentErrorRecoveryService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long paymentId, String errorMessage) {
        try {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                log.warn("No se encontro pago {} para marcar como FALLIDO", paymentId);
                return;
            }
            payment.setStatus(PaymentStatus.FALLIDO);
            // Truncar mensaje a 500 chars para caber en la columna
            String msg = errorMessage != null && errorMessage.length() > 500
                    ? errorMessage.substring(0, 497) + "..."
                    : errorMessage;
            payment.setErrorMessage(msg);
            paymentRepository.save(payment);
            log.info("Pago {} marcado como FALLIDO tras error en reintentar", paymentId);
        } catch (Exception e) {
            log.error("No se pudo marcar el pago {} como FALLIDO: {}", paymentId, e.getMessage(), e);
        }
    }
}
