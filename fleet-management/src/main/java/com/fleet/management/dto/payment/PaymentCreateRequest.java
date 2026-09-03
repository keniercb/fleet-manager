package com.fleet.management.dto.payment;

import com.fleet.management.model.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreateRequest {

    @NotNull(message = "El ID del plan es obligatorio")
    private Long planId;

    @NotNull(message = "El tipo de pago es obligatorio")
    private PaymentType type;

    private Long subscriptionId;
}
