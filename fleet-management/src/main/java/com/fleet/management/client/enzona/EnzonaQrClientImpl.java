package com.fleet.management.client.enzona;

import com.fleet.management.client.enzona.dto.EnzonaQrInfoResponse;
import com.fleet.management.client.enzona.dto.EnzonaQrMerchantRequest;
import com.fleet.management.client.enzona.dto.EnzonaQrMerchantResponse;
import com.fleet.management.client.enzona.dto.EnzonaTokenResponse;
import com.fleet.management.config.PaymentConfig;
import com.fleet.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnzonaQrClientImpl implements EnzonaQrClient {

    private final PaymentConfig config;

    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    private RestTemplate authenticatedRestTemplate;

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
            RestTemplate rt = new RestTemplate();
            ResponseEntity<EnzonaTokenResponse> response = rt.exchange(
                    config.getTokenUrl(),
                    HttpMethod.POST,
                    request,
                    EnzonaTokenResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException("Error al obtener token de Enzona: " + response.getStatusCode());
            }

            EnzonaTokenResponse tokenResponse = response.getBody();
            Instant expiresAt = Instant.now().plusSeconds(tokenResponse.getExpiresIn());
            cachedToken.set(new CachedToken(tokenResponse.getAccessToken(), expiresAt));

            // Reconstruir RestTemplate autenticado con el nuevo token
            buildAuthenticatedRestTemplate();

            return tokenResponse.getAccessToken();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error de conexion al obtener token de Enzona", e);
            throw new BusinessException("No se pudo conectar con el servicio de pagos de Enzona");
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

            ResponseEntity<EnzonaQrMerchantResponse> response = new RestTemplate().exchange(
                    url, HttpMethod.POST, request, EnzonaQrMerchantResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            throw new BusinessException("Error al crear QR en Enzona: " + response.getStatusCode());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al crear QR de comercio en Enzona", e);
            throw new BusinessException("No se pudo generar el codigo QR de pago");
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

            ResponseEntity<EnzonaQrInfoResponse> response = new RestTemplate().exchange(
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
    public Object consultarPagos(String qrCode) {
        String token = obtenerToken();

        String url = config.getBaseUrl() + config.getQrBasePath() + "/qr/payments/" + qrCode;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/json");

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Object> response = new RestTemplate().exchange(
                    url, HttpMethod.GET, request, Object.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

            return null;
        } catch (Exception e) {
            log.error("Error al consultar pagos del QR {} en Enzona", qrCode, e);
            return null;
        }
    }

    private void buildAuthenticatedRestTemplate() {
        EnzonaAuthInterceptor interceptor = new EnzonaAuthInterceptor(() -> {
            CachedToken ct = cachedToken.get();
            return ct != null ? ct.accessToken : null;
        });
        this.authenticatedRestTemplate = new RestTemplate();
        this.authenticatedRestTemplate.getInterceptors().add(interceptor);
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
    }
}
