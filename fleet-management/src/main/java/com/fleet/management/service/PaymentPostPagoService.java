package com.fleet.management.service;

import com.fleet.management.model.Payment;

/**
 * Servicio separado para ejecutar la acción post-pago en una transacción nueva
 * (REQUIRES_NEW). Se extrae del PaymentServiceImpl para evitar el problema de
 * self-invocation de Spring AOP: si un método @Transactional llama internamente
 * a otro método @Transactional en el mismo bean, las anotaciones no se respetan.
 *
 * Refs: FX-05 del plan-defectos.md
 */
public interface PaymentPostPagoService {

    /**
     * Ejecuta la acción post-pago correspondiente al tipo de pago
     * (NUEVA_SUSCRIPCION, RENOVACION, UPGRADE) en una transacción nueva.
     * Si la acción falla, la transacción del pago PAGADO ya quedó commiteada
     * y la acción puede reintentarse posteriormente (vía scheduler de
     * reconciliación o DLQ).
     *
     * @param payment pago ya marcado como PAGADO
     */
    void ejecutar(Payment payment);
}
