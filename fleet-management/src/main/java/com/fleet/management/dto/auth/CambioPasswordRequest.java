package com.fleet.management.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CambioPasswordRequest {

    @NotNull(message = "El id del usuario es obligatorio")
    private Long userId;

    @NotBlank(message = "La contrasena anterior es obligatoria")
    private String passwordAnterior;

    @NotBlank(message = "La nueva contrasena es obligatoria")
    private String nuevaPassword;

    @NotBlank(message = "La confirmacion de la contrasena es obligatoria")
    private String confirmacionPassword;
}
