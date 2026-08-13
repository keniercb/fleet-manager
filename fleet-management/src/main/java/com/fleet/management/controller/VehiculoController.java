package com.fleet.management.controller;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.vehiculo.VehiculoRequest;
import com.fleet.management.dto.vehiculo.VehiculoResponse;
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

    @GetMapping
    public ResponseEntity<Page<VehiculoResponse>> findAll(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PageRequest.of(page, perPage, Sort.Direction.fromString(sortOrder), sort);

        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehiculoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/chofer/{choferId}")
    public ResponseEntity<Page<VehiculoResponse>> findByChoferId(@PathVariable Long choferId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PageRequest.of(page, perPage, Sort.Direction.fromString(sortOrder), sort);

        return ResponseEntity.ok(service.findByChoferId(choferId, pageable));
    }

    @GetMapping("/tipo-vehiculo/{tipoVehiculoId}")
    public ResponseEntity<Page<VehiculoResponse>> findByTipoVehiculoId(@PathVariable Long tipoVehiculoId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PageRequest.of(page, perPage, Sort.Direction.fromString(sortOrder), sort);

        return ResponseEntity.ok(service.findByTipoVehiculoId(tipoVehiculoId, pageable));
    }

    @GetMapping("/tipo-combustible/{tipoCombustibleId}")
    public ResponseEntity<Page<VehiculoResponse>> findByTipoCombustibleId(@PathVariable Long tipoCombustibleId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PageRequest.of(page, perPage, Sort.Direction.fromString(sortOrder), sort);

        return ResponseEntity.ok(service.findByTipoCombustibleId(tipoCombustibleId, pageable));
    }

    @GetMapping("/sin-chofer")
    public ResponseEntity<Page<VehiculoResponse>> findSinChoferAsignado(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PageRequest.of(page, perPage, Sort.Direction.fromString(sortOrder), sort);

        return ResponseEntity.ok(service.findSinChoferAsignado(pageable));
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