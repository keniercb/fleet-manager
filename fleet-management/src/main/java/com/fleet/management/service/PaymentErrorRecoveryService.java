package com.fleet.management.service;

/**
 * Servicio separado para persistir el estado FALLIDO de un pago cuando ocurre
 * un error durante una operación transaccional que ya está marcada para rollback.
 *
 * <p>FX-21: si {@code PaymentServiceImpl.reintentar} captura una excepción
 * genérica dentro de su {@code @Transactional}, Spring marca la transacción
 * para rollback y cualquier {@code save()} posterior se ignora. Para garantizar
 * que el pago quede como FALLIDO en BD, esta operación debe ejecutarse en una
 * transacción nueva (REQUIRES_NEW) en un bean separado (evitando el problema
 * de self-invocation de Spring AOP).
 */
public interface PaymentErrorRecoveryService {

    /**
     * Marca un pago como FALLIDO con el mensaje de error dado, en una transacción
     * nueva (REQUIRES_NEW). No lanza excepciones: si la persistencia falla, lo
     * loguea y retorna sin propagar el error.
     *
     * @param paymentId    ID del pago a marcar como FALLIDO.
     * @param errorMessage Mensaje de error a persistir.
     */
    void markAsFailed(Long paymentId, String errorMessage);
}
