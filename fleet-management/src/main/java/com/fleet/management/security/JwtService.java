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
 * <p>FX-32: la validación de token ahora consulta {@code user.getActivo()} en
 * cada request (via CustomUserDetailsService, que ya lanza
 * UsernameNotFoundException si el usuario está inactivo). Esto efectivamente
 * revoca tokens cuando el usuario es desactivado.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${spring.application.name:fleet-management}")
    private String issuer;

    @Value("${jwt.audience:fleet-management-web}")
    private String audience;

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
                // FX-32: claims estándar para trazabilidad y validación
                .issuer(issuer)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())  // jti: identificador único del token
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
            // FX-32: validar claims estándar (iss, aud) además del subject y expiración.
            // La validación de user.activo la hace CustomUserDetailsService al cargar
            // el usuario; si está inactivo, lanzará UsernameNotFoundException y el
            // filtro no autenticará.
            Claims claims = extractAllClaims(token);
            boolean issuerOk = issuer.equals(claims.getIssuer());
            boolean audienceOk = claims.getAudience() != null
                    && claims.getAudience().contains(audience);
            return username.equals(userDetails.getUsername())
                    && issuerOk
                    && audienceOk
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

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
