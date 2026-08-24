package com.fleet.management.dto.subscription;

import com.fleet.management.dto.user.UserAuditResponse;
import com.fleet.management.model.SubscriptionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {

    private Long id;
    private EmpresaResumidaResponse empresa;
    private PlanResumidoResponse plan;
    private LocalDate startDate;
    private LocalDate endDate;
    private SubscriptionStatus status;
    private Integer currentVehicleCount;
    private Long version;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private UserAuditResponse creadoPor;
    private UserAuditResponse modificadoPor;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmpresaResumidaResponse {
        private Long id;
        private String codigo;
        private String nombre;
        private Boolean activo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlanResumidoResponse {
        private Long id;
        private String nombre;
        private BigDecimal precioMensual;
        private Integer maxUsuarios;
        private Integer maxVehiculos;
        private Integer duracion;
        private Boolean activo;
    }
}