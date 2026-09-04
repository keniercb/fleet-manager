package com.fleet.management.util;

import com.fleet.management.config.PaymentConfig;
import com.fleet.management.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verificador de integridad para webhook de Enzona (FX-03).
 *
 * <p>Dos capmas de defensa:
 * <ol>
 *   <li><b>Whitelist de IPs</b>: si la propiedad {@code enzona.allowed-ips} está
 *       configurada, solo se aceptan requests cuya IP remota esté en la lista.</li>
 *   <li><b>Firma HMAC-SHA256</b>: si la propiedad {@code enzona.webhook-secret}
 *       está configurada, se valida el header {@code X-Enzona-Signature} contra
 *       HMAC-SHA256 del cuerpo del request. Comparación en tiempo constante
 *       para evitar timing attacks.</li>
 * </ol>
 *
 * <p>Si ambas propiedades están vacías, el webhook queda sin protección (solo
 * recomendable en entorno local de desarrollo). En producción deben configurarse
 * ambas obligatoriamente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnzonaWebhookVerifier {

    public static final String SIGNATURE_HEADER = "X-Enzona-Signature";

    private final PaymentConfig paymentConfig;

    /**
     * Valida IP de origen contra whitelist configurada.
     *
     * @return {@code true} si la whitelist está vacía (no configurada) o si la IP
     *         del cliente está en la lista; {@code false} en caso contrario.
     */
    public boolean isIpAllowed(HttpServletRequest request) {
        String allowedIps = paymentConfig.getAllowedIps();
        if (allowedIps == null || allowedIps.isBlank()) {
            log.debug("enzona.allowed-ips no configurada; saltando validacion de IP (NO usar en prod)");
            return true;
        }
        Set<String> whitelist = Arrays.stream(allowedIps.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        String clientIp = resolveClientIp(request);
        boolean ok = whitelist.contains(clientIp);
        if (!ok) {
            log.warn("Webhook Enzona rechazado: IP {} no esta en whitelist {}", clientIp, whitelist);
        }
        return ok;
    }

    /**
     * Valida firma HMAC-SHA256 del cuerpo del request contra el header
     * {@code X-Enzona-Signature}.
     *
     * @return {@code true} si el secret no está configurado (modo dev, no usar en prod)
     *         o si la firma coincide; {@code false} si la firma no coincide.
     */
    public boolean isSignatureValid(HttpServletRequest request, byte[] body) {
        String secret = paymentConfig.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            log.debug("enzona.webhook-secret no configurado; saltando validacion de firma (NO usar en prod)");
            return true;
        }
        String signatureHeader = request.getHeader(SIGNATURE_HEADER);
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Webhook Enzona rechazado: header {} ausente", SIGNATURE_HEADER);
            return false;
        }
        try {
            String computed = hmacSha256Hex(body, secret);
            boolean ok = DigestUtils.equals(computed.getBytes(StandardCharsets.UTF_8),
                    normalizeHeader(signatureHeader).getBytes(StandardCharsets.UTF_8));
            if (!ok) {
                log.warn("Webhook Enzona rechazado: firma HMAC mismatch (header={}, computed={})",
                        signatureHeader, computed);
            }
            return ok;
        } catch (Exception e) {
            log.error("Error al validar firma HMAC del webhook: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Resuelve la IP real del cliente, considerando X-Forwarded-For solo si la
     * propiedad {@code enzona.trust-forwarded-for} es true (por defecto false).
     * Mitiga el bypass de rate limit vía header spoofing (FX-20).
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (Boolean.TRUE.equals(paymentConfig.getTrustForwardedFor())) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private static String hmacSha256Hex(byte[] data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data);
        return HexFormat.of().formatHex(raw);
    }

    /**
     * Normaliza el header de firma: acepta formatos "sha256=<hex>" o "<hex>".
     */
    private static String normalizeHeader(String header) {
        String trimmed = header.trim();
        if (trimmed.toLowerCase().startsWith("sha256=")) {
            return trimmed.substring(7);
        }
        return trimmed;
    }
}
