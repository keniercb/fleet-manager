package com.fleet.management.dto.subscription;

import com.fleet.management.model.SubscriptionStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequest {

    @Min(value = 1, message = "La cantidad maxima de vehiculos debe ser al menos 1")
    private Integer maxVehiculos;

    @Min(value = 1, message = "La cantidad maxima de usuarios debe ser al menos 1")
    private Integer maxUsuarios;

    private SubscriptionStatus status;

    @DecimalMin(value = "0.00", message = "El porciento de descuento no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porciento de descuento no puede superar 100")
    private BigDecimal porcientoDescuentoAnual;
}