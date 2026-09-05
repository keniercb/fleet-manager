package com.fleet.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "enzona")
public class PaymentConfig {

    private String clientId;
    private String clientSecret;
    private String merchantUuid;
    private String baseUrl = "https://api.enzona.net";
    private String tokenUrl = "https://api.enzona.net/token";
    private String qrBasePath = "/qr/v1.0.0";
    private String currency = "CUP";
    private int qrTimeoutHours = 24;
    private String notifyUrl;
    private String returnUrl;

    // FX-03: seguridad del webhook
    /**
     * Lista de IPs autorizadas a invocar el webhook, separadas por coma.
     * Si está vacía, la validación de IP se omite (solo dev).
     */
    private String allowedIps;

    /**
     * Secreto compartido con Enzona para validar la firma HMAC-SHA256 del webhook.
     * Si está vacío, la validación de firma se omite (solo dev).
     */
    private String webhookSecret;

    /**
     * Si es true, se confía en el header X-Forwarded-For para resolver la IP
     * del cliente. Por defecto false para evitar bypass (FX-20).
     */
    private Boolean trustForwardedFor = false;
}
