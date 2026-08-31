package com.fleet.management.controller;

import com.fleet.management.dto.reporte.AbastecimientoReporteResponse;
import com.fleet.management.dto.reporte.ConsumoCombustibleResponse;
import com.fleet.management.dto.reporte.MantenimientoReporteResponse;
import com.fleet.management.dto.reporte.VehiculoConsumoReporteDTO;
import com.fleet.management.service.ReporteConsumoCombustibleService;
import com.fleet.management.service.ReporteMantenimientoService;
import com.fleet.management.service.ReporteTransporteService;
import com.fleet.management.util.PaginationUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Reportes Transporte")
@RestController
@RequestMapping("/api/reportes-transporte")
@RequiredArgsConstructor
public class ReporteTransporteController {

    private final ReporteTransporteService reporteTransporteService;
    private final ReporteMantenimientoService reporteMantenimientoService;
    private final ReporteConsumoCombustibleService reporteConsumoCombustibleService;

    @GetMapping("/consumo-vehiculo")
    public ResponseEntity<Page<VehiculoConsumoReporteDTO>> consumoPorVehiculo(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long tipoVehiculoId,
            @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) Long tipoCombustibleId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "vehiculo.matricula") String sort,
            @RequestParam(defaultValue = "ASC") String sortOrder) {

        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, size, sort, sortOrder));
        Page<VehiculoConsumoReporteDTO> resultado = reporteTransporteService.consumoPorVehiculo(
                fechaDesde, fechaHasta, tipoVehiculoId, marcaId, tipoCombustibleId, pageable);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/abastecimiento")
    public ResponseEntity<Page<AbastecimientoReporteResponse>> abastecimientoPorVehiculo(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long vehiculoId,
            @RequestParam(required = false) String lugarAbastecimiento,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "vehiculoResumido.matricula") String sort,
            @RequestParam(defaultValue = "ASC") String sortOrder) {

        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, size, sort, sortOrder));
        Page<AbastecimientoReporteResponse> resultado = reporteTransporteService.abastecimientoPorVehiculo(
                desde, hasta, vehiculoId, lugarAbastecimiento, pageable);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/mantenimiento")
    public ResponseEntity<Page<MantenimientoReporteResponse>> reporteMantenimiento(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "vehiculoResumido.matricula") String sort,
            @RequestParam(defaultValue = "ASC") String sortOrder) {

        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, size, sort, sortOrder));
        Page<MantenimientoReporteResponse> resultado = reporteMantenimientoService.reporteMantenimiento(pageable);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/consumo-por-combustible")
    public ResponseEntity<ConsumoCombustibleResponse> consumoPorTipoCombustible(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long tipoVehiculoId) {

        ConsumoCombustibleResponse resultado = reporteConsumoCombustibleService.generarReporte(
                fechaDesde, fechaHasta, tipoVehiculoId);
        return ResponseEntity.ok(resultado);
    }
}
