package com.fleet.management.dto.chofer;

import com.fleet.management.dto.chofercategoria.ChoferCategoriaEmbeddedResponse;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private List<ChoferCategoriaEmbeddedResponse> categorias;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}