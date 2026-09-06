package com.fleet.management.client.enzona;

import com.fleet.management.client.enzona.dto.EnzonaPagosResponse;
import com.fleet.management.client.enzona.dto.EnzonaQrInfoResponse;
import com.fleet.management.client.enzona.dto.EnzonaQrMerchantRequest;
import com.fleet.management.client.enzona.dto.EnzonaQrMerchantResponse;

import java.math.BigDecimal;

public interface EnzonaQrClient {

    /**
     * Obtener token Bearer usando client_credentials.
     */
    String obtenerToken();

    /**
     * Crear QR de comercio para cobro.
     *
     * @param amount      monto a cobrar
     * @param description descripcion del pago
     * @return respuesta con vendor_identity_code e imagen base64
     */
    EnzonaQrMerchantResponse crearQrMerchant(BigDecimal amount, String description);

    /**
     * Consultar informacion de un QR creado.
     *
     * @param qrCode vendor_identity_code
     * @return informacion del QR
     */
    EnzonaQrInfoResponse consultarQr(String qrCode);

    /**
     * Consultar pagos asociados a un QR.
     *
     * <p>Cuando el QR ya fue utilizado (pago confirmado), Enzona devuelve HTTP 400
     * con fault.code=4078. En ese caso, la respuesta no es null y
     * {@link EnzonaPagosResponse#isPagoConfirmado()} retorna true.
     *
     * @param qrCode vendor_identity_code
     * @return respuesta interpretada; null si hubo error de conexion
     */
    EnzonaPagosResponse consultarPagos(String qrCode);
}
