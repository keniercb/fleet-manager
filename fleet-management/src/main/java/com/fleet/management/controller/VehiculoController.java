package com.fleet.management.controller;
import com.fleet.management.util.PaginationUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.vehiculo.VehiculoRequest;
import com.fleet.management.dto.vehiculo.VehiculoResponse;
import com.fleet.management.dto.reporte.ReporteMovimientoMensualResponse;
import com.fleet.management.service.RecorridoService;
import com.fleet.management.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Vehicles")
@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService service;
    private final RecorridoService recorridoService;

    @GetMapping
    public ResponseEntity<Page<VehiculoResponse>> findAll(@RequestParam(required = false) String filter, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(service.findAll(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehiculoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/chofer/{choferId}")
    public ResponseEntity<Page<VehiculoResponse>> findByChoferId(@PathVariable Long choferId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));

        return ResponseEntity.ok(service.findByChoferId(choferId, pageable));
    }

    @GetMapping("/tipo-vehiculo/{tipoVehiculoId}")
    public ResponseEntity<Page<VehiculoResponse>> findByTipoVehiculoId(@PathVariable Long tipoVehiculoId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));

        return ResponseEntity.ok(service.findByTipoVehiculoId(tipoVehiculoId, pageable));
    }

    @GetMapping("/tipo-combustible/{tipoCombustibleId}")
    public ResponseEntity<Page<VehiculoResponse>> findByTipoCombustibleId(@PathVariable Long tipoCombustibleId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));

        return ResponseEntity.ok(service.findByTipoCombustibleId(tipoCombustibleId, pageable));
    }

    @GetMapping("/sin-chofer")
    public ResponseEntity<Page<VehiculoResponse>> findSinChoferAsignado(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));

        return ResponseEntity.ok(service.findSinChoferAsignado(pageable));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<Page<VehiculoResponse>> findByEmpresaId(@PathVariable Long empresaId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(service.findByEmpresaId(empresaId, pageable));
    }

    @GetMapping("/reporte-movimiento-mensual/{vehiculoId}")
    public ResponseEntity<ReporteMovimientoMensualResponse> reporteMovimientoMensual(
            @PathVariable Long vehiculoId,
            @RequestParam Integer mes,
            @RequestParam Integer anio) {
        return ResponseEntity.ok(recorridoService.reporteMovimientoMensual(vehiculoId, mes, anio));
    }

    @PostMapping
    public ResponseEntity<VehiculoResponse> create(@Valid @RequestBody VehiculoRequest request) {
        VehiculoResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehiculoResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody VehiculoRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}