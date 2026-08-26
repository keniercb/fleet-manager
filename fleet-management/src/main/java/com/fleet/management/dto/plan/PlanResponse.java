package com.fleet.management.dto.plan;

import com.fleet.management.dto.feature.FeatureResponse;
import com.fleet.management.dto.user.UserAuditResponse;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanResponse {

    private Long id;
    private String nombre;
    private BigDecimal precioMensual;
    private Integer maxUsuarios;
    private Integer maxVehiculos;
    private Integer duracion;
    private BigDecimal porcientoDescuentoAnual;
    private List<FeatureResponse> features;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private UserAuditResponse creadoPor;
    private UserAuditResponse modificadoPor;
}