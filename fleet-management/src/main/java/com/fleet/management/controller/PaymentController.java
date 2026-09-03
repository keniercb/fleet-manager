package com.fleet.management.controller;

import com.fleet.management.dto.payment.PaymentCreateRequest;
import com.fleet.management.dto.payment.PaymentNotificationDto;
import com.fleet.management.dto.payment.PaymentResponse;
import com.fleet.management.service.PaymentService;
import com.fleet.management.util.PaginationUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Pagos - Enzona QR")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> crearPago(@Valid @RequestBody PaymentCreateRequest request) {
        PaymentResponse response = paymentService.crearPago(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-company")
    public ResponseEntity<PaymentResponse> findActiveByMyCompany() {
        return ResponseEntity.ok(paymentService.findActiveByMyCompany());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.findById(id));
    }

    @GetMapping("/empresa/{empresaId}")
    public ResponseEntity<Page<PaymentResponse>> findByEmpresa(
            @PathVariable Long empresaId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer perPage,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "DESC") String sortOrder) {
        Pageable pageable = PaginationUtils.of(PaginationUtils.params(page, perPage, sort, sortOrder));
        return ResponseEntity.ok(paymentService.findByEmpresa(empresaId, status, pageable));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.cancelar(id));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<PaymentResponse> reintentar(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.reintentar(id));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<PaymentResponse> consultarEstadoExterno(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.consultarEstadoExterno(id));
    }

    @PostMapping("/webhook/enzona")
    public ResponseEntity<Void> webhookEnzona(@RequestBody PaymentNotificationDto notification) {
        paymentService.procesarNotificacion(notification);
        return ResponseEntity.ok().build();
    }
}
