package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad que representa un ítem dentro de un pedido.
 * Pertenece al aggregate Pedido.
 * 
 * PATRÓN SNAPSHOT:
 * ItemPedido captura el precio y nombre del producto AL MOMENTO DE SU CREACIÓN.
 * Esto asegura que cambios futuros en el catálogo NO afecten pedidos históricos.
 */
public class ItemPedido {

    private final ItemPedidoId id;
    private final PedidoId pedidoId;
    private final ProductoId productoId;
    private final String nombreProducto;  // Snapshot del nombre
    private int cantidad;
    private final BigDecimal precioUnitario;  // Snapshot del precio
    private String observacion;

    /**
     * Constructor para reconstrucción desde persistencia.
     * Usado por la capa de infraestructura (JPA).
     */
    public ItemPedido(ItemPedidoId id, PedidoId pedidoId, ProductoId productoId, String nombreProducto, 
                      int cantidad, BigDecimal precioUnitario, String observacion) {
        this.id = Objects.requireNonNull(id, "El id del item no puede ser null");
        this.pedidoId = Objects.requireNonNull(pedidoId, "El pedidoId no puede ser null");
        this.productoId = Objects.requireNonNull(productoId, "El productoId no puede ser null");
        this.nombreProducto = Objects.requireNonNull(nombreProducto, "El nombre del producto no puede ser null");
        this.cantidad = validarCantidad(cantidad);
        this.precioUnitario = validarPrecioUnitario(precioUnitario);
        this.observacion = observacion; // Puede ser null
    }

    /**
     * Constructor de dominio con PATRÓN SNAPSHOT.
     * Captura el precio y nombre del producto en el momento de la creación del ítem.
     * 
     * AC3 - Inmutabilidad de Precios:
     * El precio se copia del producto al momento de agregar el ítem.
     * Cambios posteriores en el catálogo NO afectan este ítem.
     * 
     * @param id identificador único del ítem
     * @param pedidoId identificador del pedido al que pertenece
     * @param producto entidad Producto desde la cual se capturan los valores
     * @param cantidad cantidad del producto (debe ser > 0)
     * @param observacion notas adicionales del cliente (ej: "sin cebolla")
     * @throws IllegalArgumentException si producto es null o cantidad <= 0
     */
    public static ItemPedido crearDesdeProducto(ItemPedidoId id, PedidoId pedidoId, 
                                                 Producto producto, int cantidad, String observacion) {
        Objects.requireNonNull(producto, "El producto no puede ser null");
        
        // SNAPSHOT: Copiamos los valores del producto en este momento
        return new ItemPedido(
            id,
            pedidoId,
            producto.getId(),
            producto.getNombre(),      // ← Snapshot del nombre
            cantidad,
            producto.getPrecio(),      // ← Snapshot del precio
            observacion
        );
    }

    private int validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        return cantidad;
    }

    private BigDecimal validarPrecioUnitario(BigDecimal precioUnitario) {
        if (precioUnitario == null) {
            throw new IllegalArgumentException("El precio unitario no puede ser null");
        }
        if (precioUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser negativo");
        }
        return precioUnitario;
    }

    public ItemPedidoId getId() {
        return id;
    }

    public PedidoId getPedidoId() {
        return pedidoId;
    }

    public ProductoId getProductoId() {
        return productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public String getObservacion() {
        return observacion;
    }

    /**
     * Calcula el subtotal de este ítem.
     * Subtotal = precioUnitario * cantidad
     * No considera descuentos (esos se aplican a nivel Pedido).
     *
     * @return el monto total del ítem
     */
    public BigDecimal calcularSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemPedido that = (ItemPedido) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
