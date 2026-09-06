-- =====================================================================
-- FX-13: Migracion Double -> BigDecimal para precision monetaria
-- =====================================================================
-- Convierte columnas de saldo e importe de DOUBLE/FLOAT a DECIMAL(12,2)
-- para evitar perdida de precision en aritmetica monetaria.
--
-- Nota: Si la BD ya tiene datos, el CAST redondea a 2 decimales (HALF_UP).
-- Si la columna no existe (despliegue limpio), el IF NOT EXISTS evita error.
-- =====================================================================

-- Tarjeta de combustible: saldo
ALTER TABLE tarjetas_combustible
    ALTER COLUMN saldo TYPE DECIMAL(12,2) USING COALESCE(saldo, 0);

-- Recorrido: importe_abastecido (nullable)
ALTER TABLE recorridos
    ALTER COLUMN importe_abastecido TYPE DECIMAL(12,2) USING COALESCE(importe_abastecido, 0);

-- =====================================================================
-- FX-29: Tabla shedlock para ShedLock (schedulers multi-instancia)
-- =====================================================================
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
