package com.fleet.management.dto.subscription;

import com.fleet.management.model.SubscriptionStatus;
import jakarta.validation.constraints.*;
import lombok.*;

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

    private SubscriptionStatus status;
}