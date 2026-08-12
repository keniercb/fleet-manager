package com.fleet.management.dto.empresa;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaRequest {

    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 30, message = "El codigo no puede exceder 30 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombre;

    @Size(max = 255, message = "La direccion no puede exceder 255 caracteres")
    private String direccion;

    @Size(max = 20, message = "El telefono no puede exceder 20 caracteres")
    private String telefono;

    @Email(message = "El email debe tener un formato valido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;
}