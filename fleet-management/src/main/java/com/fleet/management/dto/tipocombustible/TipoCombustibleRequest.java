package com.fleet.management.dto.tipocombustible;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoCombustibleRequest {

    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 20, message = "El codigo no puede exceder 20 caracteres")
    private String codigo;

    @NotBlank(message = "La denominacion es obligatoria")
    @Size(max = 100, message = "La denominacion no puede exceder 100 caracteres")
    private String denominacion;

    @Size(max = 255, message = "La descripcion no puede exceder 255 caracteres")
    private String descripcion;
}