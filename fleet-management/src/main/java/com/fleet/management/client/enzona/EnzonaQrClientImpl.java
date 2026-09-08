package com.fleet.management.client.enzona;

import com.fleet.management.client.enzona.dto.EnzonaPagosResponse;
import com.fleet.management.client.enzona.dto.EnzonaQrInfoResponse;
import com.fleet.management.client.enzona.dto.EnzonaQrMerchantRequest;
import com.fleet.management.client.enzona.dto.EnzonaQrMerchantResponse;
import com.fleet.management.client.enzona.dto.EnzonaTokenResponse;
import com.fleet.management.config.PaymentConfig;
import com.fleet.management.exception.BusinessError;
import com.fleet.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cliente REST para la API de Enzona QR.
 *
 * <p>FX-22: se inyecta un unico {@link RestTemplate} configurado con timeouts
 * y pool de conexiones (bean {@code enzonaRestTemplate}).
 *
 * <p>FX-23: eliminado el codigo muerto {@code buildAuthenticatedRestTemplate()}
 * y el interceptor {@code EnzonaAuthInterceptor}; ya no son necesarios porque
 * el token se inyecta directamente en cada llamada via header.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnzonaQrClientImpl implements EnzonaQrClient {

    private final PaymentConfig config;
    // FX-22: RestTemplate unico inyectado con timeouts y pool
    private final RestTemplate enzonaRestTemplate;

    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    @Override
    public synchronized String obtenerToken() {
        CachedToken current = cachedToken.get();
        if (current != null && current.expiresAt.isAfter(Instant.now().plusSeconds(60))) {
            return current.accessToken;
        }

        String credentials = config.getClientId() + ":" + config.getClientSecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encoded);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<EnzonaTokenResponse> response = enzonaRestTemplate.exchange(
                    config.getTokenUrl(),
                    HttpMethod.POST,
                    request,
                    EnzonaTokenResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw BusinessError.enzonaErrorToken(String.valueOf(response.getStatusCode()));
            }

            EnzonaTokenResponse tokenResponse = response.getBody();
            Instant expiresAt = Instant.now().plusSeconds(tokenResponse.getExpiresIn());
            cachedToken.set(new CachedToken(tokenResponse.getAccessToken(), expiresAt));

            return tokenResponse.getAccessToken();
        } catch (BusinessException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.error("Timeout o error de conexion al obtener token de Enzona", e);
            throw BusinessError.enzonaTimeoutConexion();
        } catch (Exception e) {
            log.error("Error de conexion al obtener token de Enzona", e);
            throw BusinessError.enzonaErrorConexion();
        }
    }

    @Override
    public EnzonaQrMerchantResponse crearQrMerchant(BigDecimal amount, String description) {
        String token = obtenerToken();

        String url = config.getBaseUrl() + config.getQrBasePath() + "/qr/merchant";

        EnzonaQrMerchantRequest body = EnzonaQrMerchantRequest.builder()
                .merchantUuid(config.getMerchantUuid())
                .amount(amount.toPlainString())
                .currency(config.getCurrency())
                .description(description)
                .terminalId("FLM-Caja1")
                .returnUrl(config.getReturnUrl())
                .notifyUrl(config.getNotifyUrl())
                .permanent("0")
                .build();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/json");

            HttpEntity<EnzonaQrMerchantRequest> request = new HttpEntity<>(body, headers);

            ResponseEntity<EnzonaQrMerchantResponse> response = enzonaRestTemplate.exchange(
                    url, HttpMethod.POST, request, EnzonaQrMerchantResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            throw BusinessError.enzonaErrorCrearQR(String.valueOf(response.getStatusCode()));
        } catch (BusinessException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("Error 4xx de Enzona al crear QR: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw BusinessError.enzonaErrorCrearQR(String.valueOf(e.getStatusCode()));
        } catch (HttpServerErrorException | ResourceAccessException e) {
            log.error("Error de conexion / 5xx de Enzona al crear QR", e);
            throw BusinessError.enzonaErrorGenerarQR();
        } catch (Exception e) {
            log.error("Error al crear QR de comercio en Enzona", e);
            throw BusinessError.enzonaErrorGenerarQR();
        }
    }

    @Override
    public EnzonaQrInfoResponse consultarQr(String qrCode) {
        String token = obtenerToken();

        String url = config.getBaseUrl() + config.getQrBasePath() + "/qr/" + qrCode;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/json");

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<EnzonaQrInfoResponse> response = enzonaRestTemplate.exchange(
                    url, HttpMethod.GET, request, EnzonaQrInfoResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            return null;
        } catch (Exception e) {
            log.error("Error al consultar QR {} en Enzona", qrCode, e);
            return null;
        }
    }

    @Override
    public EnzonaPagosResponse consultarPagos(String qrCode) {
        String token = obtenerToken();

        String url = config.getBaseUrl() + config.getQrBasePath() + "/qr/payments/" + qrCode;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/json");

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Cuando el QR ya fue utilizado (pago confirmado), Enzona devuelve HTTP 400
            // con body { "fault": { "code": 4078, "message": "..." } }.
            // HttpClientErrorException captura el 4xx para que podamos interpretar el body.
            try {
                ResponseEntity<EnzonaPagosResponse> response = enzonaRestTemplate.exchange(
                        url, HttpMethod.GET, request, EnzonaPagosResponse.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response.getBody();
                }
                return new EnzonaPagosResponse();
            } catch (HttpClientErrorException e) {
                // HTTP 4xx: interpretar fault.code 4078 como pago confirmado
                if (e.getStatusCode().value() == 400 && e.getResponseBodyAsString() != null) {
                    try {
                        EnzonaPagosResponse resp = new com.fasterxml.jackson.databind.ObjectMapper()
                                .readValue(e.getResponseBodyAsString(), EnzonaPagosResponse.class);
                        if (resp.isPagoConfirmado()) {
                            log.info("QR {} ya fue utilizado (fault.code=4078) - pago confirmado", qrCode);
                        }
                        return resp;
                    } catch (Exception parseEx) {
                        log.warn("No se pudo parsear fault de Enzona (400): {}",
                                e.getResponseBodyAsString());
                        return new EnzonaPagosResponse();
                    }
                }
                log.warn("HTTP {} al consultar pagos del QR {}: {}",
                        e.getStatusCode().value(), qrCode, e.getResponseBodyAsString());
                return new EnzonaPagosResponse();
            }
        } catch (Exception e) {
            log.error("Error al consultar pagos del QR {} en Enzona", qrCode, e);
            return null;
        }
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
    }
}
