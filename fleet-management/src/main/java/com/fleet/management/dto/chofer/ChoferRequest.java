package com.fleet.management.dto.chofer;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoferRequest {

    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    private String apellidos;

    @NotBlank(message = "El carne de identidad es obligatorio")
    @Size(max = 20, message = "El carne de identidad no puede exceder 20 caracteres")
    private String carneIdentidad;

    @NotBlank(message = "El numero de licencia es obligatorio")
    @Size(max = 30, message = "El numero de licencia no puede exceder 30 caracteres")
    private String numeroLicencia;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    private LocalDate fechaNacimiento;

    /**
     * Lista de categorias de licencia con sus fechas de emision.
     * Se gestiona a traves del endpoint /api/choferes-categorias,
     * pero se puede usar aqui para crear chofer con categorias en un solo paso.
     */
    private List<CategoriaConFechaRequest> categorias;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoriaConFechaRequest {

        @NotNull(message = "La categoria de licencia es obligatoria")
        private Long categoriaLicenciaId;

        @NotNull(message = "La fecha de emision es obligatoria")
        @PastOrPresent(message = "La fecha de emision no puede ser futura")
        private LocalDate fechaEmision;
    }
}