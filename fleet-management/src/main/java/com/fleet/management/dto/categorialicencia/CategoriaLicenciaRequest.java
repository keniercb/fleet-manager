package com.fleet.management.dto.categorialicencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaLicenciaRequest {

    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "^[A-Za-z]$", message = "El codigo debe ser una unica letra")
    private String codigo;

    @NotBlank(message = "La denominacion es obligatoria")
    @Size(max = 100, message = "La denominacion no puede exceder 100 caracteres")
    private String denominacion;

    @Size(max = 255, message = "La descripcion no puede exceder 255 caracteres")
    private String descripcion;
}