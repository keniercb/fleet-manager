package com.fleet.management.controller;

import com.fleet.management.dto.payment.PaymentCreateRequest;
import com.fleet.management.dto.payment.PaymentNotificationDto;
import com.fleet.management.dto.payment.PaymentResponse;
import com.fleet.management.service.PaymentService;
import com.fleet.management.util.EnzonaWebhookVerifier;
import com.fleet.management.util.PaginationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Pagos - Enzona QR")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final EnzonaWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;

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
        // FX-24: este endpoint persiste el estado si detecta pago confirmado en Enzona.
        return ResponseEntity.ok(paymentService.consultarEstadoExterno(id));
    }

    /**
     * FX-24: consultar estado local del pago sin llamar a Enzona ni persistir cambios.
     * Solo lectura, no tiene side-effects. Útil para consultar rápidamente el estado
     * sin forzar una llamada a la API externa.
     */
    @GetMapping("/{id}/local-status")
    public ResponseEntity<PaymentResponse> consultarEstadoLocal(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.consultarEstadoLocal(id));
    }

    /**
     * Webhook de Enzona (FX-03): valida firma HMAC-SHA256 + IP whitelist antes
     * de procesar la notificación. Devuelve 401 si la firma es inválida, 403 si
     * la IP no está en la whitelist, 200 si fue procesada o ignorada.
     *
     * <p>El body se recibe como {@code byte[]} para poder verificar la firma
     * sobre el contenido crudo (sin parsear) y luego deserializar manualmente.
     */
    @PostMapping(value = "/webhook/enzona")
    public ResponseEntity<Void> webhookEnzona(@RequestBody byte[] rawBody, HttpServletRequest request) {
        // 1. Validar IP
        if (!webhookVerifier.isIpAllowed(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // 2. Validar firma HMAC
        if (!webhookVerifier.isSignatureValid(request, rawBody)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 3. Deserializar body y procesar
        try {
            PaymentNotificationDto notification = objectMapper.readValue(rawBody, PaymentNotificationDto.class);
            paymentService.procesarNotificacion(notification);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error al deserializar o procesar webhook Enzona: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
