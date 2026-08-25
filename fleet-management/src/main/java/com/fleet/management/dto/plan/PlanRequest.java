package com.fleet.management.dto.plan;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @DecimalMin(value = "0.0", message = "El precio mensual no puede ser negativo")
    private BigDecimal precioMensual;

    @Min(value = 1, message = "El maximo de usuarios debe ser al menos 1")
    private Integer maxUsuarios;

    @Min(value = 1, message = "El maximo de vehiculos debe ser al menos 1")
    private Integer maxVehiculos;

    @Min(value = 1, message = "La duracion debe ser al menos 1 dia")
    private Integer duracion;

    private List<Long> featureIds;
}