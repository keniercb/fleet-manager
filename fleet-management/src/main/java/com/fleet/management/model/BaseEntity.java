package com.fleet.management.model;

import com.fleet.management.security.AuthenticatedUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private User creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modificado_por_id")
    private User modificadoPor;

    /**
     * Auto set timestamps and audit user on create
     */
    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        creadoPor = getAuthenticatedUser();
        modificadoPor = getAuthenticatedUser();
    }

    /**
     * Auto set timestamp and audit user on update
     */
    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
        modificadoPor = getAuthenticatedUser();
    }

    /**
     * Obtains the currently authenticated User entity from SecurityContext.
     * Returns null if there is no authenticated user (e.g. during DataInitializer bootstrap).
     */
    private User getAuthenticatedUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof AuthenticatedUser authenticatedUser) {
                return authenticatedUser.getUser();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
