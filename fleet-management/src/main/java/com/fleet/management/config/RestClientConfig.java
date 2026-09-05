package com.fleet.management.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuracion central de clientes HTTP (FX-22).
 *
 * <p>Antes cada llamada a Enzona hacia {@code new RestTemplate()} sin timeouts
 * ni pool de conexiones, generando hilos colgados bajo carga. Ahora se inyecta
 * un unico bean {@link RestTemplate} configurado con:
 * <ul>
 *   <li>connectTimeout = 5s</li>
 *   <li>readTimeout = 10s</li>
 *   <li>pool de conexiones via HttpComponentsClientHttpRequestFactory</li>
 * </ul>
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate enzonaRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                .build();
    }
}
