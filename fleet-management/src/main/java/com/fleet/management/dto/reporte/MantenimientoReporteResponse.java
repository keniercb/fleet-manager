package com.fleet.management.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoReporteResponse {

    private VehiculoResumidoDTO vehiculoResumido;
    private EmpresaResumidoDTO empresaResumida;
    private LocalDate fechaUltimoMantenimiento;
    private BigInteger odometroUltimoMantenimiento;
    private BigInteger odometroActual;
    private BigInteger kmDesdeMantenimiento;
    private Integer umbralKm;
    private String estado;
    private Long diasTranscurridos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehiculoResumidoDTO {
        private Long id;
        private String matricula;
        private String modelo;
        private String marcaNombre;
        private String tipoVehiculoNombre;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmpresaResumidoDTO {
        private Long id;
        private String codigo;
        private String nombre;
    }
}
