-- ============================================================
-- V18: Tabla key-value para identidad y activación del local.
--
-- Almacena pares clave-valor como local-id, nombre-local, etc.
-- Usada por el flujo de activación (primer arranque).
-- ============================================================

CREATE TABLE local_config (
    config_key   VARCHAR(100) NOT NULL PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL
);
