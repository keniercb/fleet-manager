package com.fleet.management.controller;
import com.fleet.management.util.PaginationUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.municipio.MunicipioRequest;
import com.fleet.management.dto.municipio.MunicipioResponse;
import com.fleet.management.service.MunicipioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Municipios")
@RestController
@RequestMapping("/api/municipios")
@RequiredArgsConstructor
public class MunicipioController {

    private final MunicipioService service;

    @GetMapping
    public ResponseEntity<Page<MunicipioResponse>> findAll(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/provincia/{provinciaId}")
    public ResponseEntity<Page<MunicipioResponse>> findByProvinciaId(@PathVariable Long provinciaId, @RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(service.findByProvinciaId(provinciaId, pageable));
    }

    @GetMapping("/provincia/{provinciaId}/list")
    public ResponseEntity<List<MunicipioResponse>> listByProvinciaId(@PathVariable Long provinciaId) {
        return ResponseEntity.ok(service.listByProvinciaId(provinciaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MunicipioResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<MunicipioResponse> create(@Valid @RequestBody MunicipioRequest request) {
        MunicipioResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MunicipioResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody MunicipioRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}