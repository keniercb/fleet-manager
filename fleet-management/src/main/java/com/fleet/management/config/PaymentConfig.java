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
}
