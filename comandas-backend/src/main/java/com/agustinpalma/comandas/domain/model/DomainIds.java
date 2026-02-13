package com.agustinpalma.comandas.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Objects que representan identidades en el dominio.
 * Todos son inmutables, validados en construcción y comparados por valor.
 */
public final class DomainIds {

    private DomainIds() {
        throw new UnsupportedOperationException("Clase de utilidad no instanciable");
    }

    // ============================================
    // LOCAL AGGREGATE
    // ============================================

    public static final class LocalId {
        private final UUID value;

        public LocalId(UUID value) {
            if (value == null) throw new IllegalArgumentException("LocalId no puede ser null");
            this.value = value;
        }

        public static LocalId generate() {
            return new LocalId(UUID.randomUUID());
        }

        public static LocalId from(String value) {
            return new LocalId(UUID.fromString(value));
        }

        public UUID getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LocalId localId = (LocalId) o;
            return Objects.equals(value, localId.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static final class UsuarioId {
        private final UUID value;

        public UsuarioId(UUID value) {
            if (value == null) throw new IllegalArgumentException("UsuarioId no puede ser null");
            this.value = value;
        }

        public static UsuarioId generate() {
            return new UsuarioId(UUID.randomUUID());
        }

        public static UsuarioId from(String value) {
            return new UsuarioId(UUID.fromString(value));
        }

        public UUID getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UsuarioId that = (UsuarioId) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static final class MesaId {
        private final UUID value;

        public MesaId(UUID value) {
            if (value == null) throw new IllegalArgumentException("MesaId no puede ser null");
            this.value = value;
        }

        public static MesaId generate() {
            return new MesaId(UUID.randomUUID());
        }

        public static MesaId from(String value) {
            return new MesaId(UUID.fromString(value));
        }

        public UUID getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MesaId mesaId = (MesaId) o;
            return Objects.equals(value, mesaId.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static final class ProductoId {
        private final UUID value;

        public ProductoId(UUID value) {
            if (value == null) throw new IllegalArgumentException("ProductoId no puede ser null");
            this.value = value;
        }

        public static ProductoId generate() {
            return new ProductoId(UUID.randomUUID());
        }

        public static ProductoId from(String value) {
            return new ProductoId(UUID.fromString(value));
        }

        public UUID getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductoId that = (ProductoId) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static final class PromocionId {
        private final UUID value;

        public PromocionId(UUID value) {
            if (value == null) throw new IllegalArgumentException("PromocionId no puede ser null");
            this.value = value;
        }

        public static PromocionId generate() {
            return new PromocionId(UUID.randomUUID());
        }

        public static PromocionId from(String value) {
            return new PromocionId(UUID.fromString(value));
        }

        public UUID getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PromocionId that = (PromocionId) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    // ============================================
    // PEDIDO AGGREGATE
    // ============================================

    public static final class PedidoId {
        private final UUID value;

        public PedidoId(UUID value) {
            if (value == null) throw new IllegalArgumentException("PedidoId no puede ser null");
            this.value = value;
        }

        public static PedidoId generate() {
            return new PedidoId(UUID.randomUUID());
        }

        public static PedidoId from(String value) {
            return new PedidoId(UUID.fromString(value));
        }

        public UUID getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PedidoId pedidoId = (PedidoId) o;
            return Objects.equals(value, pedidoId.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static final class ItemPedidoId {
        private final UUID value;

        public ItemPedidoId(UUID value) {
            if (value == null) throw new IllegalArgumentException("ItemPedidoId no puede ser null");
            this.value = value;
        }

        public static ItemPedidoId generate() {
            return new ItemPedidoId(UUID.randomUUID());
        }

        public static ItemPedidoId from(String value) {
            return new ItemPedidoId(UUID.fromString(value));
        }

        public UUID getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemPedidoId that = (ItemPedidoId) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static final class DescuentoId {
        private final UUID value;

        public DescuentoId(UUID value) {
            if (value == null) throw new IllegalArgumentException("DescuentoId no puede ser null");
            this.value = value;
        }

        public static DescuentoId generate() {
            return new DescuentoId(UUID.randomUUID());
        }

        public static DescuentoId from(String value) {
            return new DescuentoId(UUID.fromString(value));
        }

        public UUID getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DescuentoId that = (DescuentoId) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    // ============================================
    // CAJA AGGREGATE
    // ============================================

    public static final class MovimientoCajaId {
        private final UUID value;

        public MovimientoCajaId(UUID value) {
            if (value == null) throw new IllegalArgumentException("MovimientoCajaId no puede ser null");
            this.value = value;
        }

        public static MovimientoCajaId generate() {
            return new MovimientoCajaId(UUID.randomUUID());
        }

        public static MovimientoCajaId from(String value) {
            return new MovimientoCajaId(UUID.fromString(value));
        }

        public UUID getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MovimientoCajaId that = (MovimientoCajaId) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    /**
     * Identidad de un ítem dentro del alcance de una promoción.
     * HU-09: Asociar productos a promociones.
     */
    public static final class ItemPromocionId {
        private final UUID value;

        public ItemPromocionId(UUID value) {
            if (value == null) throw new IllegalArgumentException("ItemPromocionId no puede ser null");
            this.value = value;
        }

        public static ItemPromocionId generate() {
            return new ItemPromocionId(UUID.randomUUID());
        }

        public static ItemPromocionId from(String value) {
            return new ItemPromocionId(UUID.fromString(value));
        }

        public UUID getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemPromocionId that = (ItemPromocionId) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }
}
