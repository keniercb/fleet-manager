package com.fleet.management.controller;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.tipocombustible.TipoCombustibleRequest;
import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;
import com.fleet.management.service.TipoCombustibleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Fuel Types")
@RestController
@RequestMapping("/api/tipos-combustible")
@RequiredArgsConstructor
public class TipoCombustibleController {

    private final TipoCombustibleService service;

    @GetMapping
    public ResponseEntity<Page<TipoCombustibleResponse>> findAll(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PageRequest.of(page, perPage, Sort.Direction.fromString(sortOrder), sort);

        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoCombustibleResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<TipoCombustibleResponse> findByCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(service.findByCodigo(codigo));
    }

    @PostMapping
    public ResponseEntity<TipoCombustibleResponse> create(@Valid @RequestBody TipoCombustibleRequest request) {
        TipoCombustibleResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoCombustibleResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody TipoCombustibleRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
