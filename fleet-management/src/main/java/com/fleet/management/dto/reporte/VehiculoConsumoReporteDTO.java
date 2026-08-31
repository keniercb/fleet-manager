package com.fleet.management.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiculoConsumoReporteDTO {

    private Long vehiculoId;
    private String matricula;
    private String modelo;
    private String marcaNombre;
    private String tipoCombustibleCodigo;
    private String empresaNombre;

    private BigDecimal kilometrosTotales;
    private BigDecimal consumoTeorico;
    private BigDecimal consumoReal;
    private BigDecimal desviacionLitros;
    private BigDecimal desviacionPorcentaje;
    private BigDecimal eficiencia;
}
