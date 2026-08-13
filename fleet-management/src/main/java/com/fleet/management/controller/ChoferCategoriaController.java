package com.fleet.management.controller;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.chofercategoria.ChoferCategoriaRequest;
import com.fleet.management.dto.chofercategoria.ChoferCategoriaResponse;
import com.fleet.management.service.ChoferCategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Driver License Categories")
@RestController
@RequestMapping("/api/choferes-categorias")
@RequiredArgsConstructor
public class ChoferCategoriaController {

    private final ChoferCategoriaService service;

    @GetMapping
    public ResponseEntity<Page<ChoferCategoriaResponse>> findAll(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PageRequest.of(page, perPage, Sort.Direction.fromString(sortOrder), sort);

        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChoferCategoriaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/chofer/{choferId}")
    public ResponseEntity<Page<ChoferCategoriaResponse>> findByChoferId(@PathVariable Long choferId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PageRequest.of(page, perPage, Sort.Direction.fromString(sortOrder), sort);

        return ResponseEntity.ok(service.findByChoferId(choferId, pageable));
    }

    @GetMapping("/categoria/{categoriaLicenciaId}")
    public ResponseEntity<Page<ChoferCategoriaResponse>> findByCategoriaLicenciaId(@PathVariable Long categoriaLicenciaId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PageRequest.of(page, perPage, Sort.Direction.fromString(sortOrder), sort);

        return ResponseEntity.ok(service.findByCategoriaLicenciaId(categoriaLicenciaId, pageable));
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