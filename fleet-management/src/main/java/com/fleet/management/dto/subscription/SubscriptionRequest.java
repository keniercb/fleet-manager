package com.fleet.management.dto.subscription;

import com.fleet.management.model.SubscriptionStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequest {

    @NotNull(message = "El ID de la empresa es obligatorio")
    private Long empresaId;

    @NotNull(message = "El ID del plan es obligatorio")
    private Long planId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate endDate;

    @NotNull(message = "El estado es obligatorio")
    private SubscriptionStatus status;

    @Min(value = 0, message = "La cantidad de vehiculos no puede ser negativa")
    private Integer currentVehicleCount;
}