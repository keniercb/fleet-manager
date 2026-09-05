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
public class DashboardEjecutivoResponse {

    private String periodo;
    private BigDecimal costoTotalCombustible;
    private BigDecimal consumoPromedioFlota;
    private Integer kmTotalesFlota;
    private BigDecimal tasaUtilizacionFlota;
    private BigDecimal eficienciaPromedioChoferes;
    private Integer vehiculosAlertaMantenimiento;
    private BigDecimal variacionCostoVsMesAnterior;
    private BigDecimal desviacionConsumoPromedio;
}
