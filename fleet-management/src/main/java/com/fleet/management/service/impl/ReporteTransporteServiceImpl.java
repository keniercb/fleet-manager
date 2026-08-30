package com.fleet.management.service.impl;

import com.fleet.management.dto.reporte.VehiculoConsumoReporteDTO;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.repository.ConsumoVehiculoProjection;
import com.fleet.management.repository.RecorridoRepository;
import com.fleet.management.security.AuthenticatedUser;
import com.fleet.management.service.ReporteTransporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteTransporteServiceImpl implements ReporteTransporteService {

    private final RecorridoRepository recorridoRepository;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoConsumoReporteDTO> consumoPorVehiculo(LocalDate fechaDesde, LocalDate fechaHasta,
                                                              Long tipoVehiculoId, Long marcaId,
                                                              Long tipoCombustibleId,
                                                              Pageable pageable) {
        // 1. Validar fechas
        if (fechaDesde == null || fechaHasta == null) {
            throw new BusinessException("Las fechas desde y hasta son obligatorias");
        }
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new BusinessException("fechaDesde no puede ser mayor que fechaHasta");
        }

        // 2. Obtener empresa del usuario autenticado
        Long empresaId = resolveEmpresaId();

        // 3. Obtener datos agregados de la BD
        List<ConsumoVehiculoProjection> datos = recorridoRepository.consumoPorVehiculo(
                empresaId, fechaDesde, fechaHasta, tipoVehiculoId, marcaId, tipoCombustibleId);

        // 4. Convertir proyecciones a DTO con cálculos derivados
        List<VehiculoConsumoReporteDTO> dtos = datos.stream()
                .map(this::buildDTO)
                .toList();

        // 5. Ordenar en memoria según el sort del Pageable
        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            Comparator<VehiculoConsumoReporteDTO> comparator = null;
            for (Sort.Order order : sort) {
                Comparator<VehiculoConsumoReporteDTO> single = switch (order.getProperty()) {
                    case "vehiculo.matricula", "matricula" ->
                            Comparator.comparing(VehiculoConsumoReporteDTO::getMatricula,
                                    Comparator.nullsLast(Comparator.naturalOrder()));
                    case "modelo" ->
                            Comparator.comparing(VehiculoConsumoReporteDTO::getModelo,
                                    Comparator.nullsLast(Comparator.naturalOrder()));
                    case "marcaNombre" ->
                            Comparator.comparing(VehiculoConsumoReporteDTO::getMarcaNombre,
                                    Comparator.nullsLast(Comparator.naturalOrder()));
                    case "kilometrosTotales" ->
                            Comparator.comparing(VehiculoConsumoReporteDTO::getKilometrosTotales,
                                    Comparator.nullsLast(Comparator.naturalOrder()));
                    case "consumoReal" ->
                            Comparator.comparing(VehiculoConsumoReporteDTO::getConsumoReal,
                                    Comparator.nullsLast(Comparator.naturalOrder()));
                    case "desviacionPorcentaje" ->
                            Comparator.comparing(VehiculoConsumoReporteDTO::getDesviacionPorcentaje,
                                    Comparator.nullsLast(Comparator.naturalOrder()));
                    default ->
                            Comparator.comparing(VehiculoConsumoReporteDTO::getMatricula,
                                    Comparator.nullsLast(Comparator.naturalOrder()));
                };
                comparator = comparator == null ? single : comparator.thenComparing(single);
            }
            if (sort.iterator().next().isDescending()) {
                comparator = comparator.reversed();
            }
            dtos = dtos.stream().sorted(comparator).toList();
        }

        // 6. Paginación en memoria
        long total = dtos.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), (int) total);
        List<VehiculoConsumoReporteDTO> pageContent = start < total
                ? dtos.subList(start, end)
                : List.of();

        return new PageImpl<>(pageContent, pageable, total);
    }

    private VehiculoConsumoReporteDTO buildDTO(ConsumoVehiculoProjection p) {
        BigDecimal kmTotal = p.getKmTotal() != null ? p.getKmTotal() : BigDecimal.ZERO;
        BigDecimal litrosTotal = p.getLitrosTotal() != null ? p.getLitrosTotal() : BigDecimal.ZERO;
        BigDecimal consumoTeorico = p.getConsumoTeorico() != null ? p.getConsumoTeorico() : BigDecimal.ZERO;
        BigDecimal consumoReal = litrosTotal;
        BigDecimal desviacionLitros = consumoReal.subtract(consumoTeorico);

        BigDecimal desviacionPorcentaje = BigDecimal.ZERO;
        if (consumoTeorico.compareTo(BigDecimal.ZERO) > 0) {
            desviacionPorcentaje = desviacionLitros
                    .multiply(CIEN)
                    .divide(consumoTeorico, 2, RoundingMode.HALF_UP);
        }

        BigDecimal eficiencia = null;
        if (consumoReal.compareTo(BigDecimal.ZERO) > 0) {
            eficiencia = kmTotal.divide(consumoReal, 2, RoundingMode.HALF_UP);
        }

        return VehiculoConsumoReporteDTO.builder()
                .vehiculoId(p.getVehiculoId())
                .matricula(p.getMatricula())
                .modelo(p.getModelo())
                .marcaNombre(p.getMarcaNombre())
                .tipoCombustibleCodigo(p.getTipoCombustibleCodigo())
                .empresaNombre(p.getEmpresaNombre())
                .kilometrosTotales(kmTotal)
                .consumoTeorico(consumoTeorico)
                .consumoReal(consumoReal)
                .desviacionLitros(desviacionLitros)
                .desviacionPorcentaje(desviacionPorcentaje)
                .eficiencia(eficiencia)
                .build();
    }

    private Long resolveEmpresaId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authUser)) {
            throw new BusinessException("No se pudo determinar la empresa del usuario autenticado");
        }
        var empresaRef = authUser.getUser().getEmpresa();
        if (empresaRef == null) {
            throw new BusinessException("El usuario no tiene una empresa asociada");
        }
        return empresaRef.getId();
    }
}
