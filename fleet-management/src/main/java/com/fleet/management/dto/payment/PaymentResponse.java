package com.fleet.management.dto.payment;

import com.fleet.management.model.PaymentStatus;
import com.fleet.management.model.PaymentType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private EmpresaResumida empresa;
    private PlanResumido plan;
    private Long subscriptionId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private PaymentStatus status;
    private PaymentType type;
    private String qrCode;
    private String qrImageBase64;
    private String externalTransactionId;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
    private String errorMessage;
    private Integer retryCount;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    // Campos para consulta de estado externo
    private String externalStatus;
    private Boolean confirmed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpresaResumida {
        private Long id;
        private String codigo;
        private String nombre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanResumido {
        private Long id;
        private String nombre;
        private BigDecimal precioMensual;
    }
}
