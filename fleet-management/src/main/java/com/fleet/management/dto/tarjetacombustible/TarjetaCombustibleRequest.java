package com.fleet.management.dto.tarjetacombustible;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarjetaCombustibleRequest {

    @NotBlank(message = "El numero de tarjeta es obligatorio")
    @Size(max = 50, message = "El numero de tarjeta no puede exceder 50 caracteres")
    private String numero;

    @NotNull(message = "El saldo es obligatorio")
    @Positive(message = "El saldo debe ser mayor a cero")
    private Double saldo;

    @NotNull(message = "La moneda es obligatoria")
    private Long currencyId;

    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;
}