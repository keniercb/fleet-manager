package com.fleet.management.integration.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuracion de WireMock para mockear la API de Enzona en tests de integracion.
 *
 * <p>Levanta un WireMockServer en puerto aleatorio y carga los mappings desde
 * classpath:wiremock/enzona/mappings/*.json.
 *
 * <p>La URL base se expone via propiedad {@code enzona.base-url} y
 * {@code enzona.token-url} en {@code application-test.properties}.
 */
@Configuration
public class EnzonaWireMockConfig {

    private static WireMockServer wireMockServer;

    public static WireMockServer getServer() {
        if (wireMockServer == null || !wireMockServer.isRunning()) {
            wireMockServer = new WireMockServer(
                    WireMockConfiguration.options()
                            .dynamicPort()
                            .usingFilesUnderClasspath("wiremock/enzona"));
            wireMockServer.start();
        }
        return wireMockServer;
    }

    public static String getBaseUrl() {
        return getServer().baseUrl();
    }

    @Bean(destroyMethod = "stop")
    @Primary
    public WireMockServer enzonaWireMockServer() {
        return getServer();
    }
}
