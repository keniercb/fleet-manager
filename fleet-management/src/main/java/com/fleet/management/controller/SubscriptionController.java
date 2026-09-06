package com.fleet.management.controller;

import com.fleet.management.dto.subscription.SubscriptionCreateRequest;
import com.fleet.management.dto.subscription.SubscriptionRequest;
import com.fleet.management.dto.subscription.SubscriptionResponse;
import com.fleet.management.security.AuthenticatedUser;
import com.fleet.management.service.SubscriptionService;
import com.fleet.management.util.PaginationUtils;
import com.fleet.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Subscriptions")
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SubscriptionController {

    private final SubscriptionService service;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<SubscriptionResponse>> findAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer perPage,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/my-company")
    public ResponseEntity<SubscriptionResponse> findMyCompanySubscription(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        Long empresaId = SecurityUtils.resolveEmpresaId();
        return ResponseEntity.ok(service.findActiveByEmpresa(empresaId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<SubscriptionResponse>> findByEmpresa(
            @PathVariable Long empresaId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer perPage,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(service.findByEmpresa(empresaId, pageable));
    }

    @GetMapping("/plan/{planId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<SubscriptionResponse>> findByPlan(
            @PathVariable Long planId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer perPage,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "ASC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(service.findByPlan(planId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionResponse> create(@Valid @RequestBody SubscriptionCreateRequest request) {
        SubscriptionResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
