package com.fleet.management.controller;

import com.fleet.management.dto.tipovehiculo.TipoVehiculoRequest;
import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;
import com.fleet.management.service.TipoVehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-vehiculo")
@RequiredArgsConstructor
public class TipoVehiculoController {

    private final TipoVehiculoService service;

    @GetMapping
    public ResponseEntity<List<TipoVehiculoResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoVehiculoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<TipoVehiculoResponse> create(@Valid @RequestBody TipoVehiculoRequest request) {
        TipoVehiculoResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoVehiculoResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody TipoVehiculoRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}