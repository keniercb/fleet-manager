package com.fleet.management.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO con los datos de una fila del listado de vehiculos para el reporte PDF.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehiculoFilaReporteDto {

    private String tipoVehiculo;
    private String matricula;
    private String marca;
    private String modelo;
    private String numeroMotor;
    private String odometro;
    private String combustibleLitros;
}
