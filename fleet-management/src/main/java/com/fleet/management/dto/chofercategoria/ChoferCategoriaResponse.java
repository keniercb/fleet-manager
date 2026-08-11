package com.fleet.management.dto.chofercategoria;

import com.fleet.management.dto.categorialicencia.CategoriaLicenciaResponse;
import com.fleet.management.dto.chofer.ChoferResponse;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoferCategoriaResponse {

    private Long id;
    private ChoferResponse chofer;
    private CategoriaLicenciaResponse categoriaLicencia;
    private LocalDate fechaEmision;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}