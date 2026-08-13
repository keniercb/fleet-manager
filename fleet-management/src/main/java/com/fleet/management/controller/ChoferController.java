package com.fleet.management.controller;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;


import com.fleet.management.dto.chofer.ChoferRequest;
import com.fleet.management.dto.chofer.ChoferResponse;
import com.fleet.management.service.ChoferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Drivers")
@RestController
@RequestMapping("/api/choferes")
@RequiredArgsConstructor
public class ChoferController {

    private final ChoferService service;

    @GetMapping
    public ResponseEntity<Page<ChoferResponse>> findAll(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChoferResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ChoferResponse> create(@Valid @RequestBody ChoferRequest request) {
        ChoferResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChoferResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ChoferRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
