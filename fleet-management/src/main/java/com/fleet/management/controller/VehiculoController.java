package com.fleet.management.controller;

import com.fleet.management.dto.vehiculo.VehiculoRequest;
import com.fleet.management.dto.vehiculo.VehiculoResponse;
import com.fleet.management.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Vehicles")
@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService service;

    @GetMapping
    public ResponseEntity<List<VehiculoResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehiculoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/chofer/{choferId}")
    public ResponseEntity<List<VehiculoResponse>> findByChoferId(@PathVariable Long choferId) {
        return ResponseEntity.ok(service.findByChoferId(choferId));
    }

    @GetMapping("/tipo-vehiculo/{tipoVehiculoId}")
    public ResponseEntity<List<VehiculoResponse>> findByTipoVehiculoId(@PathVariable Long tipoVehiculoId) {
        return ResponseEntity.ok(service.findByTipoVehiculoId(tipoVehiculoId));
    }

    @GetMapping("/tipo-combustible/{tipoCombustibleId}")
    public ResponseEntity<List<VehiculoResponse>> findByTipoCombustibleId(@PathVariable Long tipoCombustibleId) {
        return ResponseEntity.ok(service.findByTipoCombustibleId(tipoCombustibleId));
    }

    @GetMapping("/sin-chofer")
    public ResponseEntity<List<VehiculoResponse>> findSinChoferAsignado() {
        return ResponseEntity.ok(service.findSinChoferAsignado());
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