package com.fleet.management.dto.marca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcaRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripcion no puede exceder 255 caracteres")
    private String descripcion;

    @Size(max = 100, message = "El pais de origen no puede exceder 100 caracteres")
    private String paisOrigen;
}