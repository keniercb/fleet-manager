package com.fleet.management.dto.currency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyRequest {

    @NotBlank(message = "El codigo ISO es obligatorio")
    @Size(max = 10, message = "El codigo ISO no puede exceder 10 caracteres")
    private String isoCode;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 100, message = "La descripcion no puede exceder 100 caracteres")
    private String descripcion;
}
