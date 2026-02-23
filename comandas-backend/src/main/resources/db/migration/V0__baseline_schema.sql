-- ============================================================
-- V0__baseline_schema.sql
-- Baseline: Crea todas las tablas base del sistema.
--
-- Contexto: Estas tablas las generaba Hibernate (ddl-auto: update).
-- Se consolidan aquí para que Flyway tenga control total del schema
-- y la BD pueda recrearse desde cero sin depender de Hibernate.
-- ============================================================

-- 1. Locales (tenants)
CREATE TABLE IF NOT EXISTS locales (
    id      UUID PRIMARY KEY,
    nombre  VARCHAR(255) NOT NULL
);

-- 2. Mesas
CREATE TABLE IF NOT EXISTS mesas (
    id        UUID PRIMARY KEY,
    local_id  UUID NOT NULL,
    numero    INTEGER NOT NULL,
    estado    VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mesa_local_id ON mesas(local_id);

-- 3. Productos
CREATE TABLE IF NOT EXISTS productos (
    id                      UUID PRIMARY KEY,
    local_id                UUID NOT NULL,
    nombre                  VARCHAR(100) NOT NULL,
    precio                  DECIMAL(10,2) NOT NULL,
    activo                  BOOLEAN NOT NULL,
    color_hex               VARCHAR(7),
    grupo_variante_id       UUID,
    es_extra                BOOLEAN NOT NULL DEFAULT false,
    cantidad_discos_carne   INTEGER
);

-- 4. Pedidos
CREATE TABLE IF NOT EXISTS pedidos (
    id                      UUID PRIMARY KEY,
    local_id                UUID NOT NULL,
    mesa_id                 UUID NOT NULL,
    numero                  INTEGER NOT NULL,
    estado                  VARCHAR(20) NOT NULL,
    fecha_apertura          TIMESTAMP NOT NULL,
    fecha_cierre            TIMESTAMP,
    medio_pago              VARCHAR(20),
    desc_global_porcentaje  DECIMAL(5,2),
    desc_global_razon       VARCHAR(255),
    desc_global_usuario_id  UUID,
    desc_global_fecha       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pedido_mesa_estado ON pedidos(mesa_id, estado);
CREATE INDEX IF NOT EXISTS idx_pedido_local_numero ON pedidos(local_id, numero);

-- 5. Items de pedido
CREATE TABLE IF NOT EXISTS items_pedido (
    id                      UUID PRIMARY KEY,
    pedido_id               UUID NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    producto_id             UUID NOT NULL,
    nombre_producto         VARCHAR(100) NOT NULL,
    cantidad                INTEGER NOT NULL,
    precio_unitario         DECIMAL(10,2) NOT NULL,
    observacion             VARCHAR(255),
    monto_descuento         DECIMAL(10,2) NOT NULL DEFAULT 0,
    nombre_promocion        VARCHAR(150),
    promocion_id            UUID,
    desc_manual_porcentaje  DECIMAL(5,2),
    desc_manual_razon       VARCHAR(255),
    desc_manual_usuario_id  UUID,
    desc_manual_fecha       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_item_pedido_id ON items_pedido(pedido_id);

-- 6. Extras de items de pedido (collection table del @ElementCollection)
CREATE TABLE IF NOT EXISTS items_pedido_extras (
    item_pedido_id    UUID NOT NULL REFERENCES items_pedido(id) ON DELETE CASCADE,
    producto_id       UUID NOT NULL,
    nombre            VARCHAR(100) NOT NULL,
    precio_snapshot   DECIMAL(10,2) NOT NULL
);

-- 7. Promociones
CREATE TABLE IF NOT EXISTS promociones (
    id                      UUID PRIMARY KEY,
    local_id                UUID NOT NULL,
    nombre                  VARCHAR(150) NOT NULL,
    descripcion             VARCHAR(500),
    prioridad               INTEGER NOT NULL,
    estado                  VARCHAR(20) NOT NULL,
    tipo_estrategia         VARCHAR(30) NOT NULL,
    modo_descuento          VARCHAR(20),
    valor_descuento         DECIMAL(10,2),
    cantidad_llevas         INTEGER,
    cantidad_pagas          INTEGER,
    cantidad_minima_trigger INTEGER,
    porcentaje_beneficio    DECIMAL(5,2),
    triggers_json           TEXT NOT NULL
);

-- 8. Scope de productos en promociones
CREATE TABLE IF NOT EXISTS promocion_productos_scope (
    id              UUID PRIMARY KEY,
    promocion_id    UUID NOT NULL,
    referencia_id   UUID NOT NULL,
    tipo_alcance    VARCHAR(20) NOT NULL,
    rol             VARCHAR(20) NOT NULL
);
