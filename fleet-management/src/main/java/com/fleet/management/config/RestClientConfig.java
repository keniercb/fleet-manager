package com.fleet.management.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Configuracion central de clientes HTTP (FX-22).
 *
 * <p>Antes cada llamada a Enzona hacia {@code new RestTemplate()} sin timeouts
 * ni pool de conexiones, generando hilos colgados bajo carga. Ahora se inyecta
 * un unico bean {@link RestTemplate} configurado con:
 * <ul>
 *   <li>connectTimeout = 5s</li>
 *   <li>readTimeout = 10s</li>
 *   <li>pool de conexiones via HttpComponentsClientHttpRequestFactory (Apache HttpClient5)</li>
 * </ul>
 *
 * <p>FX-fix: se elimino la dependencia de RestTemplateBuilder porque su paquete
 * org.springframework.boot.web.client no esta disponible en el classpath del
 * proyecto. Se construye el RestTemplate directamente con HttpClient5.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate enzonaRestTemplate() {
        // Pool de conexiones: max 20 conexiones totales, max 2 por ruta
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(5);

        // Timeouts: connect=5s, request=read=10s
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5, TimeUnit.SECONDS)
                .setResponseTimeout(10, TimeUnit.SECONDS)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(requestFactory);
    }
}
