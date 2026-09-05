package com.fleet.management.security;

import com.fleet.management.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de {@link UserDetailsService} con caché Caffeine de corta
 * duración para mitigar FX-17 (carga de DB en cada request autenticada).
 *
 * <p>El filtro {@link JwtAuthenticationFilter} invoca
 * {@link #loadUserByUsername(String)} en cada request autenticada. Sin caché,
 * esto significa una query DB por request, lo cual bajo carga es costoso
 * (incluso con el {@code @EntityGraph} de FX-18 que optimiza la query).
 *
 * <p>FX-17: ahora caché el {@link AuthenticatedUser} por email durante 60
 * segundos. Tras ese tiempo, la próxima request refresca desde BD. Esto:
 * <ul>
 *   <li>Reduce las queries a 1 por minuto por usuario activo (en lugar de 1 por request).</li>
 *   <li>Mantiene la validación de {@code user.getActivo()} actualizada (si el usuario
 *       se desactiva, en máximo 60s sus tokens dejan de funcionar).</li>
 *   <li>Conserva la entidad {@link com.fleet.management.model.User} completa
 *       necesaria para la auditoría de {@link com.fleet.management.model.BaseEntity}.</li>
 * </ul>
 *
 * <p>El caché se invalida automáticamente si el usuario es desactivado: cuando
 * {@link #loadUserByUsername(String)} se ejecuta desde BD (tras expirar el caché)
 * y el usuario está inactivo, lanza {@link UsernameNotFoundException} y la
 * entrada caché no se actualiza.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // FX-17: caché de 60s por usuario. Reduce queries de N por minuto a 1 por minuto.
    private final Cache<String, AuthenticatedUser> userCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(1_000)
            .build();

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // FX-17 + FX-18: usar caché; en miss, cargar con @EntityGraph.
        AuthenticatedUser cached = userCache.getIfPresent(email);
        if (cached != null) {
            return cached;
        }

        // FX-18: findWithRolesAndPermissionsByEmail carga roles, permisos y empresa
        // en una sola query (evita N+1).
        com.fleet.management.model.User user = userRepository.findWithRolesAndPermissionsByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        if (!user.getActivo()) {
            // No cachear usuarios inactivos.
            throw new UsernameNotFoundException("Usuario inactivo con email: " + email);
        }

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    List<SimpleGrantedAuthority> perms = role.getPermissions().stream()
                            .map(perm -> new SimpleGrantedAuthority(perm.getName()))
                            .collect(Collectors.toList());
                    perms.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                    return perms.stream();
                })
                .collect(Collectors.toList());

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user, authorities);
        userCache.put(email, authenticatedUser);
        return authenticatedUser;
    }

    /**
     * Invalida la entrada de caché para un email. Útil para forzar el refresco
     * tras desactivar un usuario o cambiar sus roles/permisos.
     */
    public void evictFromCache(String email) {
        userCache.invalidate(email);
        log.debug("Entrada de caché invalidada para usuario {}", email);
    }
}
