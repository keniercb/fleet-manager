package com.fleet.management.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReporteMovimientoMensualResponse {

    private VehiculoReporteData vehiculo;
    private List<LecturaDiariaResponse> lecturas;
    private AnalisisConsumoResponse analisis;
}
