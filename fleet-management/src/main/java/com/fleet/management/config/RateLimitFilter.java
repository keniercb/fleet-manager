package com.fleet.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Filtro de rate limiting basado en ventana deslizante por IP.
 * Protege endpoints sensibles (login, cambio de password) contra ataques de fuerza bruta.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter implements Filter {

    private final int maxRequests;
    private final int windowSeconds;
    private final ConcurrentHashMap<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(int maxRequests, int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = resolveClientIp(httpRequest);
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;

        Deque<Long> timestamps = requestLog.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());

        // Limpiar timestamps expirados (fuera de la ventana)
 synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                // Calcular segundos restantes hasta que se libere el primer slot
                long retryAfter = timestamps.peekFirst() + windowSeconds - now;
                if (retryAfter < 1) retryAfter = 1;

                log.warn("Rate limit excedido para IP {} en {}. Rechazado. Reintentar en {}s",
                        clientIp, httpRequest.getRequestURI(), retryAfter);

                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.setHeader("Retry-After", String.valueOf(retryAfter));

                Map<String, Object> body = Map.of(
                        "status", 429,
                        "error", "Too Many Requests",
                        "message", "Demasiados intentos. Intente nuevamente en " + retryAfter + " segundo(s).",
                        "retryAfterSeconds", retryAfter
                );
                httpResponse.getWriter().write(objectMapper.writeValueAsString(body));
                return;
            }

            timestamps.addLast(now);
        }

        chain.doFilter(request, response);
    }

    /**
     * Resuelve la IP real del cliente considerando proxys reversos.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
