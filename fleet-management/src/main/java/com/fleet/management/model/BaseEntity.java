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
     *
     * <p>FX-27: ahora loguea advertencias cuando no se puede determinar el
     * usuario autenticado, en lugar de tragar la excepción silenciosamente.
     * Esto facilita diagnosticar por qué los campos de auditoría quedan null.
     *
     * <p>FX-28: en contextos async, scheduler, batch o cualquier thread que
     * no tenga propagado el SecurityContext, este método retornará null y
     * logueará una advertencia. Para esos casos, la entidad debe recibir
     * explícitamente el usuario "system" u otro apropiado antes del save().
     *
     * @return el User autenticado, o null si no hay contexto de seguridad
     *         (con log de advertencia).
     */
    private User getAuthenticatedUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                // FX-27: log para diagnosticar auditoría null (antes silencioso)
                // No usamos SLF4J aquí porque BaseEntity no es un bean y el
                // static logger podría no estar inicializado en algunos contexts.
                // Imprimimos a stderr como fallback (visible en logs del container).
                System.err.println("[BaseEntity] WARN: auditoria null - no hay usuario autenticado "
                        + "(posible contexto async/scheduler/bootstrap). "
                        + "Considerar pasar el usuario explícitamente al save().");
                return null;
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof AuthenticatedUser authenticatedUser) {
                return authenticatedUser.getUser();
            }
            System.err.println("[BaseEntity] WARN: principal no es AuthenticatedUser: "
                    + principal.getClass().getName());
            return null;
        } catch (Exception e) {
            // FX-27: log del stacktrace para diagnosticar errores de contexto
            System.err.println("[BaseEntity] ERROR al obtener usuario autenticado: "
                    + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
