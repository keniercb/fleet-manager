package com.fleet.management.client.enzona.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Respuesta de GET /qr/payments/{qr_code} de Enzona.
 *
 * <p>Cuando el QR ya fue utilizado (pago confirmado), Enzona devuelve HTTP 400
 * con body:
 * <pre>
 * {
 *   "fault": {
 *     "code": 4078,
 *     "message": "El código QR ya fue utilizado."
 *   }
 * }
 * </pre>
 *
 * <p>Cuando el QR está pendiente (no utilizado), devuelve HTTP 200 con array
 * de pagos (puede ser vacío).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnzonaPagosResponse {

    /**
     * Presente cuando Enzona responde 400 con fault.
     */
    private Fault fault;

    /**
     * Indica si la respuesta corresponde a un pago confirmado.
     * true si fault.code == 4078 ("El código QR ya fue utilizado").
     */
    public boolean isPagoConfirmado() {
        return fault != null && Integer.valueOf(4078).equals(fault.getCode());
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fault {
        private Integer code;
        private String message;
    }
}
