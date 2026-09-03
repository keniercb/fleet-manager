package com.fleet.management.client.enzona;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Interceptor que inyecta el header Authorization: Bearer {token}
 * en cada request al API de Enzona.
 */
public class EnzonaAuthInterceptor implements ClientHttpRequestInterceptor {

    private final TokenProvider tokenProvider;

    public EnzonaAuthInterceptor(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                          ClientHttpRequestExecution execution) throws IOException {
        String token = tokenProvider.getToken();
        request.getHeaders().set("Authorization", "Bearer " + token);
        request.getHeaders().set("Accept", "application/json");
        return execution.execute(request, body);
    }

    @FunctionalInterface
    public interface TokenProvider {
        String getToken();
    }
}
