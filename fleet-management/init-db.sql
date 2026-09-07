-- ============================================================================
# init-db.sql — Script de inicialización para PostgreSQL
# ============================================================================
# Se ejecuta automáticamente por la imagen postgres:16-alpine al primer
# arranque del contenedor (en /docker-entrypoint-initdb.d/).
#
# Crea la tabla shedlock requerida por FX-29 (ShedLock para schedulers
# multi-instancia). Hibernate no la crea automáticamente (no es entidad JPA).
-- ============================================================================

CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);

-- Comentario para documentación
COMMENT ON TABLE shedlock IS 'Tabla de coordinación de ShedLock para schedulers multi-instancia (FX-29)';
