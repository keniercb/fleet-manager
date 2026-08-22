package com.fleet.management.controller;

import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleRequest;
import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleResponse;
import com.fleet.management.service.TarjetaCombustibleService;
import com.fleet.management.util.PaginationUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Fuel Cards")
@RestController
@RequestMapping("/api/tarjetas-combustible")
@RequiredArgsConstructor
public class TarjetaCombustibleController {

    private final TarjetaCombustibleService service;

    @GetMapping
    public ResponseEntity<Page<TarjetaCombustibleResponse>> findAll(
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer perPage,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(service.findAll(filter, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TarjetaCombustibleResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<TarjetaCombustibleResponse> findByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(service.findByNumero(numero));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<Page<TarjetaCombustibleResponse>> findByEmpresaId(
            @PathVariable Long empresaId,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer perPage,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(service.findByEmpresaId(empresaId, filter, pageable));
    }

    @PostMapping
    public ResponseEntity<TarjetaCombustibleResponse> create(@Valid @RequestBody TarjetaCombustibleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TarjetaCombustibleResponse> update(@PathVariable Long id,
                                                              @Valid @RequestBody TarjetaCombustibleRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}