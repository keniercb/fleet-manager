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
public class AbastecimientoReporteResponse {

    private VehiculoResumidoDTO vehiculoResumido;
    private Long totalAbastecimientos;
    private BigDecimal totalLitros;
    private BigDecimal promedioLitrosPorCarga;
    private Long frecuenciaDias;
    private String lugarMasFrecuente;
    private String periodo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehiculoResumidoDTO {
        private Long id;
        private String matricula;
        private String modelo;
        private String marcaNombre;
        private String tipoCombustibleCodigo;
    }
}
