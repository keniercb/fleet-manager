package com.fleet.management.controller;

import com.fleet.management.dto.permission.PermissionRequest;
import com.fleet.management.dto.permission.PermissionResponse;
import com.fleet.management.service.PermissionService;
import com.fleet.management.util.PaginationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Permissions")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService service;

    @GetMapping
    public ResponseEntity<Page<PermissionResponse>> findAll(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "20") Integer perPage, @RequestParam(defaultValue = "id") String sort, @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<PermissionResponse> findByName(@PathVariable String name) {
        return ResponseEntity.ok(service.findByName(name));
    }

    @PostMapping
    public ResponseEntity<PermissionResponse> create(@Valid @RequestBody PermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionResponse> update(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}