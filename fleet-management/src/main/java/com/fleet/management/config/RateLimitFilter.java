package com.fleet.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Filtro de rate limiting basado en ventana deslizante por IP.
 * Protege endpoints sensibles (login, cambio de password) contra ataques de fuerza bruta.
 *
 * <p>FX-19: reemplazada la {@code ConcurrentHashMap<String, Deque<Long>>} sin
 * eviccion por un {@link Cache} de Caffeine con TTL de 5 minutos. Elimina el
 * memory leak ilimitado del filtro original.
 *
 * <p>FX-20: la resolucion de IP del cliente solo confia en {@code X-Forwarded-For}
 * si {@code trustForwardedFor=true} (propiedad
 * {@code fleet.security.rate-limit.trust-forwarded-for}). Por defecto es false
 * para evitar bypass del rate limit via header spoofing.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter implements Filter {

    private final int maxRequests;
    private final int windowSeconds;
    private final boolean trustForwardedFor;
    // FX-19: cache Caffeine con expiracion, evita memory leak
    private final Cache<String, Deque<Long>> requestLog;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(int maxRequests, int windowSeconds, boolean trustForwardedFor) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.trustForwardedFor = trustForwardedFor;
        // TTL de 5 minutos: cualquier IP sin actividad reciente se evicta.
        // Suficiente para ventana deslizante de 60s y previene crecimiento ilimitado.
        this.requestLog = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(5))
                .maximumSize(10_000)
                .build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = resolveClientIp(httpRequest);
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;

        // computeIfAbsent de Caffeine es thread-safe y atomica.
        Deque<Long> timestamps = requestLog.get(clientIp, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            // Limpiar timestamps expirados
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
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
     * FX-20: resuelve la IP real del cliente.
     *
     * <p>Solo confia en {@code X-Forwarded-For} y {@code X-Real-IP} si
     * {@code trustForwardedFor=true}. Por defecto es false, por lo que el
     * filtro usa directamente {@link HttpServletRequest#getRemoteAddr()}.
     * Esto evita que un atacante rote el header {@code X-Forwarded-For} en
     * cada request para evadir el rate limit.
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return xRealIp.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
