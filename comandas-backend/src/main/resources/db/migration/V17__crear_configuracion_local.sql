-- ============================================================
-- V17: Tabla de configuración por local (impresora predeterminada).
--
-- Almacena preferencias de configuración del local, como la
-- impresora seleccionada por el operador desde la UI.
-- Una fila por local (1:1 con locales).
-- ============================================================

CREATE TABLE configuracion_local (
    local_id UUID NOT NULL PRIMARY KEY REFERENCES locales(id),
    impresora_predeterminada VARCHAR(255),
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
