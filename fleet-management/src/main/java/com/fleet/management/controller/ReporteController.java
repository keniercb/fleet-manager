package com.fleet.management.controller;

import com.fleet.management.dto.reporte.MantenimientoReporteResponse;
import com.fleet.management.service.ReporteMantenimientoService;
import com.fleet.management.util.PaginationUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reportes")
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteMantenimientoService reporteMantenimientoService;

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
}
