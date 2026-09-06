package com.fleet.management.controller;
import com.fleet.management.util.PaginationUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.provincia.ProvinciaRequest;
import com.fleet.management.dto.provincia.ProvinciaResponse;
import com.fleet.management.service.ProvinciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Provincias")
@RestController
@RequestMapping("/api/provincias")
@RequiredArgsConstructor
public class ProvinciaController {

    private final ProvinciaService service;

    @GetMapping
    public ResponseEntity<Page<ProvinciaResponse>> findAll(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProvinciaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProvinciaResponse> create(@Valid @RequestBody ProvinciaRequest request) {
        ProvinciaResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProvinciaResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ProvinciaRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
