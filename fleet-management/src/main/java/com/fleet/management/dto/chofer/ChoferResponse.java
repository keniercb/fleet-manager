package com.fleet.management.dto.chofer;

import com.fleet.management.dto.categorialicencia.CategoriaLicenciaResponse;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoferResponse {

    private Long id;
    private String nombre;
    private String apellidos;
    private String carneIdentidad;
    private String numeroLicencia;
    private LocalDate fechaNacimiento;
    private CategoriaLicenciaResponse categoriaLicencia;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}