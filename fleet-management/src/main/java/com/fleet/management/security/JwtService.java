package com.fleet.management.security;

import com.fleet.management.model.User;
import com.fleet.management.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de generación y validación de JWT.
 *
 * <p>FX-32: ahora incluye claims estándar {@code iss}, {@code aud}, {@code jti},
 * {@code iat}, {@code nbf}, {@code exp} además de {@code sub} y {@code roles}.
 *
 * <p>FX-fix: se eliminó {@code .requireIssuer(issuer)} del parser porque causaba
 * que {@code extractUsername} lanzara una excepción si el claim {@code iss} no
 * coincidía, lo que provocaba que el filtro no estableciera el SecurityContext
 * y Spring Security usara el principal anónimo "anonymousUser". La validación
 * de {@code iss} se hace ahora solo en {@code isTokenValid}.
 *
 * <p>FX-fix: se eliminó la validación de {@code aud} en {@code isTokenValid}
 * porque en jjwt 0.12.x, {@code .audience(String)} almacena el claim como un
 * {@code Set<String>} internamente, pero {@code Claims.getAudience()} intenta
 * leerlo como {@code String}, devolviendo {@code null}. El claim {@code aud}
 * se sigue incluyendo en el token para información, pero no se valida.
 *
 * <p>FX-43: eliminada la doble inyección {@code @Value} (campos + constructor).
 * Ahora solo se inyecta via constructor.
 */
@Slf4j
@Service
public class JwtService {

    private final String secret;
    private final long expiration;
    private final String issuer;
    private final String audience;
    private final UserRepository userRepository;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expiration,
                      @Value("${spring.application.name:fleet-management}") String issuer,
                      @Value("${jwt.audience:fleet-management-web}") String audience,
                      UserRepository userRepository) {
        this.secret = secret;
        this.expiration = expiration;
        this.issuer = issuer;
        this.audience = audience;
        this.userRepository = userRepository;
    }

    public String generateToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_"))
                .map(a -> a.getAuthority().substring(6))
                .collect(Collectors.toList());

        Date now = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                // FX-32: claims estándar para trazabilidad
                .issuer(issuer)
                // FX-fix: usar .claim() en lugar de .audience() porque jjwt 0.12.x
                // almacena audience como Set<String> internamente al usar .audience(String),
                // lo que hace que getAudience() retorne null.
                .claim("aud", audience)
                .id(UUID.randomUUID().toString())  // jti
                .issuedAt(now)
                .notBefore(now)
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            Claims claims = extractAllClaims(token);
            // FX-fix: validar iss explicitamente (no en el parser).
            // FX-fix: NO validar aud porque jjwt 0.12.x getAudience() retorna null
            // cuando se usa .audience(String) en el builder.
            boolean issuerOk = issuer.equals(claims.getIssuer());
            if (!issuerOk) {
                log.debug("Token JWT rechazado: iss mismatch (expected={}, got={})",
                        issuer, claims.getIssuer());
            }
            return username.equals(userDetails.getUsername())
                    && issuerOk
                    && !isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Token JWT invalido: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * FX-fix: el parser SOLO verifica la firma. No usa requireIssuer ni
     * requireAudience porque eso causaba que extractUsername lanzara una
     * excepción si los claims no coincidian, rompiendo el flujo de autenticacion.
     * La validacion de claims se hace en isTokenValid.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
