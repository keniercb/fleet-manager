package com.fleet.management.service.impl;

import com.fleet.management.dto.reporte.ConsumoCombustibleResponse;
import com.fleet.management.dto.reporte.ConsumoCombustibleResponse.DetalleTipoCombustible;
import com.fleet.management.dto.reporte.ConsumoCombustibleResponse.ResumenEjecutivo;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.repository.ConsumoPorCombustibleProjection;
import com.fleet.management.repository.RecorridoRepository;
import com.fleet.management.service.ReporteConsumoCombustibleService;
import com.fleet.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteConsumoCombustibleServiceImpl implements ReporteConsumoCombustibleService {

    private final RecorridoRepository recorridoRepository;

    @Override
    @Transactional(readOnly = true)
    public ConsumoCombustibleResponse generarReporte(LocalDate fechaDesde, LocalDate fechaHasta,
                                                      Long tipoVehiculoId) {
        if (fechaDesde == null || fechaHasta == null) {
            throw new BusinessException("Las fechas fechaDesde y fechaHasta son obligatorias");
        }
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new BusinessException("fechaDesde no puede ser mayor que fechaHasta");
        }

        Long empresaId = SecurityUtils.resolveEmpresaId();

        // 1. Datos del periodo actual (ya ordenados por volumen_consumido DESC en SQL)
        List<ConsumoPorCombustibleProjection> datosActuales =
                recorridoRepository.consumoPorTipoCombustible(empresaId, fechaDesde, fechaHasta, tipoVehiculoId);

        // 2. Calcular periodo anterior (misma duracion, inmediatamente antes)
        long diasPeriodo = ChronoUnit.DAYS.between(fechaDesde, fechaHasta) + 1;
        LocalDate desdeAnterior = fechaDesde.minusDays(diasPeriodo);
        LocalDate hastaAnterior = fechaDesde.minusDays(1);

        List<ConsumoPorCombustibleProjection> datosAnteriores =
                recorridoRepository.consumoPorTipoCombustiblePeriodoAnterior(
                        empresaId, desdeAnterior, hastaAnterior, tipoVehiculoId);

        // 3. Indexar datos anteriores por tipo de combustible
        Map<String, ConsumoPorCombustibleProjection> mapaAnterior = new HashMap<>();
        for (ConsumoPorCombustibleProjection anterior : datosAnteriores) {
            mapaAnterior.put(anterior.getTipoCombustible(), anterior);
        }

        // 4. Calcular totales generales
        var ref = new Object() {
            BigDecimal volumenConsumidoTotal = BigDecimal.ZERO;
        };
        BigDecimal volumenAbastecidoTotal = BigDecimal.ZERO;
        BigDecimal costoEstimadoTotal = BigDecimal.ZERO;
        int totalRecorridos = 0;

        for (ConsumoPorCombustibleProjection fila : datosActuales) {
            ref.volumenConsumidoTotal = ref.volumenConsumidoTotal.add(
                    safeBigDecimal(fila.getVolumenConsumido()));
            volumenAbastecidoTotal = volumenAbastecidoTotal.add(
                    safeBigDecimal(fila.getVolumenAbastecido()));
            costoEstimadoTotal = costoEstimadoTotal.add(
                    safeBigDecimal(fila.getCostoEstimado()));
            totalRecorridos += fila.getCantidadRecorridos() != null ? fila.getCantidadRecorridos().intValue() : 0;
        }

        // 5. Costo promedio por litro a nivel general
        BigDecimal costoPromedioGeneral = BigDecimal.ZERO;
        if (volumenAbastecidoTotal.compareTo(BigDecimal.ZERO) > 0) {
            costoPromedioGeneral = costoEstimadoTotal.divide(volumenAbastecidoTotal, 2, RoundingMode.HALF_UP);
        }

        // 6. Construir resumen ejecutivo
        String periodo = fechaDesde + " - " + fechaHasta;
        ResumenEjecutivo resumen = ResumenEjecutivo.builder()
                .periodo(periodo)
                .totalTiposCombustible(datosActuales.size())
                .volumenConsumidoTotal(ref.volumenConsumidoTotal)
                .volumenAbastecidoTotal(volumenAbastecidoTotal)
                .costoEstimadoTotal(costoEstimadoTotal)
                .totalRecorridos(totalRecorridos)
                .costoPromedioPorLitro(costoPromedioGeneral)
                .build();

        // 7. Construir detalle con variacion vs periodo anterior
        List<DetalleTipoCombustible> detalle = datosActuales.stream()
                .map(fila -> {
                    BigDecimal volumenConsumido = safeBigDecimal(fila.getVolumenConsumido());
                    BigDecimal volumenAbastecido = safeBigDecimal(fila.getVolumenAbastecido());
                    BigDecimal costo = safeBigDecimal(fila.getCostoEstimado());

                    // Porcentaje del total
                    BigDecimal porcentaje = BigDecimal.ZERO;
                    if (ref.volumenConsumidoTotal.compareTo(BigDecimal.ZERO) > 0) {
                        porcentaje = volumenConsumido.multiply(BigDecimal.valueOf(100))
                                .divide(ref.volumenConsumidoTotal, 2, RoundingMode.HALF_UP);
                    }

                    // Variacion vs periodo anterior
                    BigDecimal variacion = null;
                    ConsumoPorCombustibleProjection anterior =
                            mapaAnterior.get(fila.getTipoCombustible());
                    if (anterior != null) {
                        BigDecimal consumoAnterior = safeBigDecimal(anterior.getVolumenConsumido());
                        if (consumoAnterior.compareTo(BigDecimal.ZERO) > 0) {
                            variacion = volumenConsumido.subtract(consumoAnterior)
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(consumoAnterior, 2, RoundingMode.HALF_UP);
                        } else if (volumenConsumido.compareTo(BigDecimal.ZERO) > 0) {
                            // No habia consumo antes y ahora si: +100%
                            variacion = BigDecimal.valueOf(100);
                        }
                    } else if (volumenConsumido.compareTo(BigDecimal.ZERO) > 0) {
                        // Tipo de combustible nuevo en este periodo
                        variacion = BigDecimal.valueOf(100);
                    }

                    // Costo promedio por litro del tipo
                    BigDecimal costoPromedioLitro = null;
                    if (volumenAbastecido.compareTo(BigDecimal.ZERO) > 0) {
                        costoPromedioLitro = costo.divide(volumenAbastecido, 2, RoundingMode.HALF_UP);
                    }

                    return DetalleTipoCombustible.builder()
                            .tipoCombustible(fila.getTipoCombustible())
                            .volumenConsumido(volumenConsumido)
                            .volumenAbastecido(volumenAbastecido)
                            .costoEstimado(costo)
                            .porcentajeDelTotal(porcentaje)
                            .variacionVsPeriodoAnterior(variacion)
                            .cantidadRecorridos(fila.getCantidadRecorridos() != null
                                    ? fila.getCantidadRecorridos().intValue() : 0)
                            .costoPromedioPorLitro(costoPromedioLitro)
                            .build();
                })
                .toList();

        return ConsumoCombustibleResponse.builder()
                .resumenEjecutivo(resumen)
                .detalle(detalle)
                .build();
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}
