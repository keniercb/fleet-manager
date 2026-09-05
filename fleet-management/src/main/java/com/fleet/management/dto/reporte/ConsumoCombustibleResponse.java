package com.fleet.management.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumoCombustibleResponse {

    private ResumenEjecutivo resumenEjecutivo;
    private List<DetalleTipoCombustible> detalle;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenEjecutivo {
        private String periodo;
        private int totalTiposCombustible;
        private BigDecimal volumenConsumidoTotal;
        private BigDecimal volumenAbastecidoTotal;
        private BigDecimal costoEstimadoTotal;
        private int totalRecorridos;
        private BigDecimal costoPromedioPorLitro;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleTipoCombustible {
        private String tipoCombustible;
        private BigDecimal volumenConsumido;
        private BigDecimal volumenAbastecido;
        private BigDecimal costoEstimado;
        private BigDecimal porcentajeDelTotal;
        private BigDecimal variacionVsPeriodoAnterior;
        private int cantidadRecorridos;
        private BigDecimal costoPromedioPorLitro;
    }
}
