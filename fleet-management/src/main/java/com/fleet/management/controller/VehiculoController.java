package com.fleet.management.controller;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<Page<VehiculoResponse>> findAll(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehiculoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/chofer/{choferId}")
    public ResponseEntity<Page<VehiculoResponse>> findByChoferId(@PathVariable Long choferId, @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(service.findByChoferId(choferId, pageable));
    }

    @GetMapping("/tipo-vehiculo/{tipoVehiculoId}")
    public ResponseEntity<Page<VehiculoResponse>> findByTipoVehiculoId(@PathVariable Long tipoVehiculoId, @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(service.findByTipoVehiculoId(tipoVehiculoId, pageable));
    }

    @GetMapping("/tipo-combustible/{tipoCombustibleId}")
    public ResponseEntity<Page<VehiculoResponse>> findByTipoCombustibleId(@PathVariable Long tipoCombustibleId, @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(service.findByTipoCombustibleId(tipoCombustibleId, pageable));
    }

    @GetMapping("/sin-chofer")
    public ResponseEntity<Page<VehiculoResponse>> findSinChoferAsignado(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

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