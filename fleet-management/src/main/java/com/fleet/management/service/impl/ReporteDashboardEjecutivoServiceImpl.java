package com.fleet.management.service.impl;

import com.fleet.management.dto.reporte.DashboardEjecutivoResponse;
import com.fleet.management.exception.BusinessError;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.model.Recorrido;
import com.fleet.management.repository.RecorridoRepository;
import com.fleet.management.repository.VehiculoRepository;
import com.fleet.management.service.ReporteDashboardEjecutivoService;
import com.fleet.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteDashboardEjecutivoServiceImpl implements ReporteDashboardEjecutivoService {

    private final RecorridoRepository recorridoRepository;
    private final VehiculoRepository vehiculoRepository;

    @Value("${fleet.reporte.mantenimiento.umbral-km:10000}")
    private int umbralKm;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardEjecutivo", key = "#empresaId + '_' + #mes + '_' + #anio")
    public DashboardEjecutivoResponse generarDashboard(Integer mes, Integer anio) {
        if (mes == null || mes < 1 || mes > 12) {
            throw BusinessError.mesInvalido(mes);
        }
        if (anio == null || anio < 2000) {
            throw BusinessError.anioInvalido(anio);
        }

        Long empresaId = SecurityUtils.resolveEmpresaId();

        // Rango del mes solicitado
        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();
        String periodoLabel = desde.getMonth().name() + " " + anio;

        // Rango del mes anterior
        YearMonth periodoAnterior = periodo.minusMonths(1);
        LocalDate desdeAnterior = periodoAnterior.atDay(1);
        LocalDate hastaAnterior = periodoAnterior.atEndOfMonth();

        // --- KPI 1: costoTotalCombustible ---
        // FX-13: sumCostoCombustible ahora retorna BigDecimal directamente.
        BigDecimal costoActual = recorridoRepository.sumCostoCombustible(empresaId, desde, hasta);
        BigDecimal costoTotalCombustible = (costoActual != null ? costoActual : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // --- KPI 2: kmTotalesFlota ---
        Long kmTotales = recorridoRepository.sumKmTotales(empresaId, desde, hasta);
        int kmTotalesFlota = kmTotales != null ? kmTotales.intValue() : 0;

        // --- KPI 3: consumoPromedioFlota (L/100km) = consumoTotal / kmTotales * 100 ---
        BigDecimal consumoPromedioFlota = BigDecimal.ZERO;
        if (kmTotalesFlota > 0) {
            BigDecimal consumoTotal = recorridoRepository.sumConsumoTotal(empresaId, desde, hasta);
            if (consumoTotal != null && consumoTotal.compareTo(BigDecimal.ZERO) > 0) {
                consumoPromedioFlota = consumoTotal
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(kmTotalesFlota), 2, RoundingMode.HALF_UP);
            }
        }

        // --- KPI 4: tasaUtilizacionFlota = vehiculos_con_recorrido / vehiculos_activos * 100 ---
        long vehiculosActivos = vehiculoRepository.countActivosByEmpresaId(empresaId);
        BigDecimal tasaUtilizacionFlota = BigDecimal.ZERO;
        if (vehiculosActivos > 0) {
            List<com.fleet.management.model.Vehiculo> vehiculosConRecorrido =
                    vehiculoRepository.findVehiculosConRecorridoEnPeriodo(empresaId, desde, hasta);
            tasaUtilizacionFlota = BigDecimal.valueOf(vehiculosConRecorrido.size())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(vehiculosActivos), 2, RoundingMode.HALF_UP);
        }

        // --- KPI 5: eficienciaPromedioChoferes = AVG(consumo_teorico / consumo_real * 100) ---
        BigDecimal eficienciaPromedioChoferes = calcularEficienciaPromedioChoferes(empresaId, desde, hasta);

        // --- KPI 6: vehiculosAlertaMantenimiento ---
        long alertasMantenimiento = vehiculoRepository.countVehiculosAlertaMantenimiento(
                empresaId, BigInteger.valueOf(umbralKm));
        int vehiculosAlertaMantenimiento = (int) alertasMantenimiento;

        // --- KPI 7: variacionCostoVsMesAnterior ---
        BigDecimal variacionCostoVsMesAnterior = calcularVariacionCosto(empresaId, desdeAnterior, hastaAnterior, costoTotalCombustible);

        // --- KPI 8: desviacionConsumoPromedio = AVG(abs(consumo_real - consumo_teorico) / consumo_teorico * 100) ---
        BigDecimal desviacionConsumoPromedio = calcularDesviacionConsumo(empresaId, desde, hasta);

        return DashboardEjecutivoResponse.builder()
                .periodo(periodoLabel)
                .costoTotalCombustible(costoTotalCombustible)
                .consumoPromedioFlota(consumoPromedioFlota)
                .kmTotalesFlota(kmTotalesFlota)
                .tasaUtilizacionFlota(tasaUtilizacionFlota)
                .eficienciaPromedioChoferes(eficienciaPromedioChoferes)
                .vehiculosAlertaMantenimiento(vehiculosAlertaMantenimiento)
                .variacionCostoVsMesAnterior(variacionCostoVsMesAnterior)
                .desviacionConsumoPromedio(desviacionConsumoPromedio)
                .build();
    }

    /**
     * Eficiencia promedio de choferes: AVG(consumo_teorico / consumo_real * 100)
     * consumo_teorico = kilometros * indiceConsumo / 100
     * consumo_real = r.consumo
     */
    private BigDecimal calcularEficienciaPromedioChoferes(Long empresaId, LocalDate desde, LocalDate hasta) {
        List<Recorrido> recorridos = recorridoRepository.findRecorridosPorPeriodo(empresaId, desde, hasta);

        if (recorridos.isEmpty()) {
            return BigDecimal.ZERO;
        }

        var ref = new Object() {
            BigDecimal sumaEficiencia = BigDecimal.ZERO;
            int conteo = 0;
        };

        for (Recorrido r : recorridos) {
            BigDecimal consumoReal = r.getConsumo();
            BigDecimal indiceConsumo = r.getVehiculo().getIndiceConsumo();
            Integer km = r.getKilometros();

            if (consumoReal == null || consumoReal.compareTo(BigDecimal.ZERO) <= 0
                    || km == null || km <= 0 || indiceConsumo == null) {
                continue;
            }

            BigDecimal consumoTeorico = BigDecimal.valueOf(km)
                    .multiply(indiceConsumo)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            if (consumoTeorico.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal eficiencia = consumoTeorico
                        .divide(consumoReal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                ref.sumaEficiencia = ref.sumaEficiencia.add(eficiencia);
                ref.conteo++;
            }
        }

        if (ref.conteo == 0) {
            return BigDecimal.ZERO;
        }
        return ref.sumaEficiencia.divide(BigDecimal.valueOf(ref.conteo), 2, RoundingMode.HALF_UP);
    }

    /**
     * Variacion del costo vs mes anterior: (costo_actual - costo_anterior) / costo_anterior * 100
     */
    private BigDecimal calcularVariacionCosto(Long empresaId, LocalDate desdeAnterior,
                                               LocalDate hastaAnterior, BigDecimal costoActual) {
        // FX-13: sumCostoCombustible ahora retorna BigDecimal directamente.
        BigDecimal costoAnteriorRaw = recorridoRepository.sumCostoCombustible(empresaId, desdeAnterior, hastaAnterior);
        BigDecimal costoAnterior = (costoAnteriorRaw != null ? costoAnteriorRaw : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        if (costoAnterior.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // No hay dato del mes anterior
        }

        return costoActual.subtract(costoAnterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(costoAnterior, 2, RoundingMode.HALF_UP);
    }

    /**
     * Desviacion consumo promedio: AVG(abs(consumo_real - consumo_teorico) / consumo_teorico * 100)
     */
    private BigDecimal calcularDesviacionConsumo(Long empresaId, LocalDate desde, LocalDate hasta) {
        List<Recorrido> recorridos = recorridoRepository.findRecorridosPorPeriodo(empresaId, desde, hasta);

        if (recorridos.isEmpty()) {
            return BigDecimal.ZERO;
        }

        var ref = new Object() {
            BigDecimal sumaDesviacion = BigDecimal.ZERO;
            int conteo = 0;
        };

        for (Recorrido r : recorridos) {
            BigDecimal consumoReal = r.getConsumo();
            BigDecimal indiceConsumo = r.getVehiculo().getIndiceConsumo();
            Integer km = r.getKilometros();

            if (consumoReal == null || km == null || km <= 0 || indiceConsumo == null) {
                continue;
            }

            BigDecimal consumoTeorico = BigDecimal.valueOf(km)
                    .multiply(indiceConsumo)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            if (consumoTeorico.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal desviacion = consumoReal.subtract(consumoTeorico).abs()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(consumoTeorico, 4, RoundingMode.HALF_UP);
                ref.sumaDesviacion = ref.sumaDesviacion.add(desviacion);
                ref.conteo++;
            }
        }

        if (ref.conteo == 0) {
            return BigDecimal.ZERO;
        }
        return ref.sumaDesviacion.divide(BigDecimal.valueOf(ref.conteo), 2, RoundingMode.HALF_UP);
    }
}
