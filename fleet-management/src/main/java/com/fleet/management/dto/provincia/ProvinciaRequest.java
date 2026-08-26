package com.fleet.management.dto.provincia;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvinciaRequest {

    @NotNull(message = "El codigo es obligatorio")
    @Min(value = 1, message = "El codigo debe ser al menos 1")
    @Max(value = 99, message = "El codigo no puede exceder 99")
    private Integer codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;
}