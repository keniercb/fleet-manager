package com.fleet.management.dto.marca;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private String paisOrigen;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
