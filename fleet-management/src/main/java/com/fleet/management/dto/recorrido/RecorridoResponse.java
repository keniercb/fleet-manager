package com.fleet.management.dto.recorrido;

import com.fleet.management.dto.vehiculo.VehiculoResponse;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecorridoResponse {

    private Long id;
    private VehiculoResponse vehiculo;
    private LocalDate fecha;
    private Integer kilometros;
    private BigInteger odometroInicial;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
