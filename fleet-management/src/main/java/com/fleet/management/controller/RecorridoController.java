package com.fleet.management.controller;

import com.fleet.management.dto.recorrido.RecorridoRequest;
import com.fleet.management.dto.recorrido.RecorridoResponse;
import com.fleet.management.service.RecorridoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Trips")
@RestController
@RequestMapping("/api/recorridos")
@RequiredArgsConstructor
public class RecorridoController {

    private final RecorridoService service;

    @GetMapping
    public ResponseEntity<List<RecorridoResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecorridoResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/vehiculo/{vehiculoId}")
    public ResponseEntity<List<RecorridoResponse>> findByVehiculoId(@PathVariable Long vehiculoId) {
        return ResponseEntity.ok(service.findByVehiculoId(vehiculoId));
    }

    @GetMapping("/vehiculo/{vehiculoId}/rango")
    public ResponseEntity<List<RecorridoResponse>> findByVehiculoIdAndFechaBetween(
            @PathVariable Long vehiculoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.findByVehiculoIdAndFechaBetween(vehiculoId, desde, hasta));
    }

    @PostMapping
    public ResponseEntity<RecorridoResponse> create(@Valid @RequestBody RecorridoRequest request) {
        RecorridoResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecorridoResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody RecorridoRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
