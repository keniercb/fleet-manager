package com.fleet.management.service;

import com.fleet.management.dto.payment.PaymentCreateRequest;
import com.fleet.management.dto.payment.PaymentNotificationDto;
import com.fleet.management.dto.payment.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    /**
     * Crear un nuevo pago y generar QR con Enzona.
     */
    PaymentResponse crearPago(PaymentCreateRequest request);

    /**
     * Obtener pago por ID.
     */
    PaymentResponse findById(Long id);

    /**
     * Obtener el pago activo (PENDIENTE o QR_GENERADO) de la empresa del usuario.
     */
    PaymentResponse findActiveByMyCompany();

    /**
     * Listar pagos de una empresa con paginacion.
     */
    Page<PaymentResponse> findByEmpresa(Long empresaId, String status, Pageable pageable);

    /**
     * Cancelar un pago en estado PENDIENTE o QR_GENERADO.
     */
    PaymentResponse cancelar(Long id);

    /**
     * Reintentar generacion de QR para un pago FALLIDO.
     */
    PaymentResponse reintentar(Long id);

    /**
     * Procesar notificacion de webhook de Enzona.
     */
    void procesarNotificacion(PaymentNotificationDto notification);

    /**
     * Consultar estado del pago en Enzona (polling manual).
     */
    PaymentResponse consultarEstadoExterno(Long id);

    /**
     * Expirar pagos con QR vencido (usado por scheduler).
     */
    void expirarPagosVencidos();

    /**
     * Hacer polling de pagos pendientes en Enzona (usado por scheduler).
     */
    void pollingPagosPendientes();
}
