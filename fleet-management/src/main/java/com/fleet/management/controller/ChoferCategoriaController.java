package com.fleet.management.controller;

import com.fleet.management.dto.chofercategoria.ChoferCategoriaRequest;
import com.fleet.management.dto.chofercategoria.ChoferCategoriaResponse;
import com.fleet.management.service.ChoferCategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Driver License Categories")
@RestController
@RequestMapping("/api/choferes-categorias")
@RequiredArgsConstructor
public class ChoferCategoriaController {

    private final ChoferCategoriaService service;

    @GetMapping
    public ResponseEntity<List<ChoferCategoriaResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChoferCategoriaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/chofer/{choferId}")
    public ResponseEntity<List<ChoferCategoriaResponse>> findByChoferId(@PathVariable Long choferId) {
        return ResponseEntity.ok(service.findByChoferId(choferId));
    }

    @GetMapping("/categoria/{categoriaLicenciaId}")
    public ResponseEntity<List<ChoferCategoriaResponse>> findByCategoriaLicenciaId(@PathVariable Long categoriaLicenciaId) {
        return ResponseEntity.ok(service.findByCategoriaLicenciaId(categoriaLicenciaId));
    }

    @PostMapping
    public ResponseEntity<ChoferCategoriaResponse> create(@Valid @RequestBody ChoferCategoriaRequest request) {
        ChoferCategoriaResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChoferCategoriaResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody ChoferCategoriaRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}