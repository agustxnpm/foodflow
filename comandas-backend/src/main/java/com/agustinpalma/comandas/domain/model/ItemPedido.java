package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad que representa un ítem dentro de un pedido.
 * Pertenece al aggregate Pedido.
 * 
 * PATRÓN SNAPSHOT:
 * ItemPedido captura el precio y nombre del producto AL MOMENTO DE SU CREACIÓN.
 * Esto asegura que cambios futuros en el catálogo NO afecten pedidos históricos.
 * 
 * HU-10: También captura información de promoción aplicada (si corresponde).
 * El descuento se calcula y fija al momento de agregar el ítem.
 * Cambios futuros en la promoción NO afectan ítems ya creados.
 */
public class ItemPedido {

    private final ItemPedidoId id;
    private final PedidoId pedidoId;
    private final ProductoId productoId;
    private final String nombreProducto;  // Snapshot del nombre
    private int cantidad;
    private final BigDecimal precioUnitario;  // Snapshot del precio
    private String observacion;
    
    // HU-10: Campos de snapshot de promoción
    private final BigDecimal montoDescuento;      // Valor monetario descontado (ej: $250)
    private final String nombrePromocion;          // Para mostrar al cliente (ej: "Promo Merienda")
    private final UUID promocionId;                // Referencia para auditoría

    /**
     * Constructor completo para reconstrucción desde persistencia.
     * Usado por la capa de infraestructura (JPA).
     * 
     * HU-10: Incluye campos de promoción para snapshot completo.
     */
    public ItemPedido(
            ItemPedidoId id, 
            PedidoId pedidoId, 
            ProductoId productoId, 
            String nombreProducto, 
            int cantidad, 
            BigDecimal precioUnitario, 
            String observacion,
            BigDecimal montoDescuento,
            String nombrePromocion,
            UUID promocionId
    ) {
        this.id = Objects.requireNonNull(id, "El id del item no puede ser null");
        this.pedidoId = Objects.requireNonNull(pedidoId, "El pedidoId no puede ser null");
        this.productoId = Objects.requireNonNull(productoId, "El productoId no puede ser null");
        this.nombreProducto = Objects.requireNonNull(nombreProducto, "El nombre del producto no puede ser null");
        this.cantidad = validarCantidad(cantidad);
        this.precioUnitario = validarPrecioUnitario(precioUnitario);
        this.observacion = observacion; // Puede ser null
        
        // HU-10: Campos de promoción (pueden ser null si no hay promo)
        this.montoDescuento = montoDescuento != null ? montoDescuento : BigDecimal.ZERO;
        this.nombrePromocion = nombrePromocion;
        this.promocionId = promocionId;
    }

    /**
     * Constructor de compatibilidad para reconstrucción sin promoción.
     * Mantiene retrocompatibilidad con código existente.
     */
    public ItemPedido(
            ItemPedidoId id, 
            PedidoId pedidoId, 
            ProductoId productoId, 
            String nombreProducto, 
            int cantidad, 
            BigDecimal precioUnitario, 
            String observacion
    ) {
        this(id, pedidoId, productoId, nombreProducto, cantidad, precioUnitario, 
             observacion, BigDecimal.ZERO, null, null);
    }

    /**
     * Factory method con PATRÓN SNAPSHOT (sin promoción).
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
    public static ItemPedido crearDesdeProducto(
            ItemPedidoId id, 
            PedidoId pedidoId, 
            Producto producto, 
            int cantidad, 
            String observacion
    ) {
        Objects.requireNonNull(producto, "El producto no puede ser null");
        
        // SNAPSHOT: Copiamos los valores del producto en este momento (sin promoción)
        return new ItemPedido(
            id,
            pedidoId,
            producto.getId(),
            producto.getNombre(),
            cantidad,
            producto.getPrecio(),
            observacion,
            BigDecimal.ZERO,  // Sin descuento
            null,              // Sin nombre de promoción
            null               // Sin ID de promoción
        );
    }

    /**
     * Factory method con PATRÓN SNAPSHOT incluyendo promoción aplicada (HU-10).
     * 
     * Captura el precio del producto Y el descuento de la promoción al momento de creación.
     * Ambos valores quedan congelados (snapshot) para garantizar:
     * - AC3: Inmutabilidad de precios ante cambios en el catálogo
     * - Inmutabilidad de descuentos ante cambios en promociones
     * 
     * @param id identificador único del ítem
     * @param pedidoId identificador del pedido al que pertenece
     * @param producto entidad Producto desde la cual se capturan los valores
     * @param cantidad cantidad del producto (debe ser > 0)
     * @param observacion notas adicionales del cliente (ej: "sin cebolla")
     * @param montoDescuento valor monetario del descuento calculado
     * @param nombrePromocion nombre de la promoción para mostrar al cliente
     * @param promocionId UUID de la promoción para auditoría
     * @throws IllegalArgumentException si producto es null o cantidad <= 0
     */
    public static ItemPedido crearConPromocion(
            ItemPedidoId id, 
            PedidoId pedidoId, 
            Producto producto, 
            int cantidad, 
            String observacion,
            BigDecimal montoDescuento,
            String nombrePromocion,
            UUID promocionId
    ) {
        Objects.requireNonNull(producto, "El producto no puede ser null");
        Objects.requireNonNull(montoDescuento, "El monto del descuento no puede ser null");
        
        // SNAPSHOT: Copiamos precio del producto Y descuento de la promoción
        return new ItemPedido(
            id,
            pedidoId,
            producto.getId(),
            producto.getNombre(),
            cantidad,
            producto.getPrecio(),
            observacion,
            montoDescuento,
            nombrePromocion,
            promocionId
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

    // ============================================
    // HU-10: Getters de campos de promoción
    // ============================================

    /**
     * Retorna el monto de descuento aplicado a este ítem.
     * Este valor es un snapshot calculado al momento de agregar el ítem.
     * 
     * @return monto del descuento (BigDecimal.ZERO si no hay promoción)
     */
    public BigDecimal getMontoDescuento() {
        return montoDescuento;
    }

    /**
     * Retorna el nombre de la promoción aplicada.
     * Útil para mostrar al cliente en el ticket.
     * 
     * @return nombre de la promoción, o null si no hay promoción
     */
    public String getNombrePromocion() {
        return nombrePromocion;
    }

    /**
     * Retorna el ID de la promoción aplicada.
     * Útil para auditoría y trazabilidad.
     * 
     * @return UUID de la promoción, o null si no hay promoción
     */
    public UUID getPromocionId() {
        return promocionId;
    }

    /**
     * Indica si este ítem tiene una promoción aplicada.
     * 
     * @return true si tiene descuento > 0
     */
    public boolean tienePromocion() {
        return montoDescuento.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calcula el subtotal de este ítem (precio base * cantidad).
     * Este es el precio SIN descuento.
     *
     * @return el monto bruto del ítem
     */
    public BigDecimal calcularSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    /**
     * Calcula el precio final de este ítem aplicando el descuento.
     * 
     * HU-10: precioFinal = (precioBase * cantidad) - montoDescuento
     * 
     * @return el monto neto del ítem después de descuentos
     */
    public BigDecimal calcularPrecioFinal() {
        return calcularSubtotal().subtract(montoDescuento);
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
