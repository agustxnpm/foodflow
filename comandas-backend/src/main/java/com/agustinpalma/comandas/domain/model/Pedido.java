package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate Root del contexto de Pedidos.
 * Representa una comanda/pedido asociado a una mesa.
 */
public class Pedido {

    private final PedidoId id;
    private final LocalId localId;
    private final MesaId mesaId;
    private final int numero;  // Número secuencial legible por humanos, único por local
    private EstadoPedido estado;
    private final LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private MedioPago medioPago;
    private final List<ItemPedido> items;
    private final List<DescuentoAplicado> descuentos;

    public Pedido(PedidoId id, LocalId localId, MesaId mesaId, int numero, EstadoPedido estado, LocalDateTime fechaApertura) {
        this.id = Objects.requireNonNull(id, "El id del pedido no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.mesaId = Objects.requireNonNull(mesaId, "El mesaId no puede ser null");
        this.numero = validarNumero(numero);
        this.estado = Objects.requireNonNull(estado, "El estado del pedido no puede ser null");
        this.fechaApertura = Objects.requireNonNull(fechaApertura, "La fecha de apertura no puede ser null");
        this.items = new ArrayList<>();
        this.descuentos = new ArrayList<>();
    }

    private int validarNumero(int numero) {
        if (numero <= 0) {
            throw new IllegalArgumentException("El número de pedido debe ser mayor a 0");
        }
        return numero;
    }

    public PedidoId getId() {
        return id;
    }

    public LocalId getLocalId() {
        return localId;
    }

    public MesaId getMesaId() {
        return mesaId;
    }

    public int getNumero() {
        return numero;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public MedioPago getMedioPago() {
        return medioPago;
    }

    public List<ItemPedido> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Método público SOLO para reconstrucción desde persistencia.
     * 
     * ADVERTENCIA: Este método NO ejecuta validaciones de negocio.
     * Solo debe ser usado por la capa de infraestructura (mappers) al hidratar el aggregate desde BD.
     * Para agregar items con lógica de negocio, usar agregarProducto() en su lugar.
     * 
     * @param item el item a agregar directamente (sin validaciones)
     */
    public void agregarItemDesdePersistencia(ItemPedido item) {
        this.items.add(item);
    }

    public List<DescuentoAplicado> getDescuentos() {
        return Collections.unmodifiableList(descuentos);
    }

    /**
     * Valida que el pedido permita modificaciones.
     * Solo los pedidos en estado ABIERTO pueden ser modificados.     * 
     * @throws IllegalStateException si el pedido NO está en estado ABIERTO
     */
    public void validarPermiteModificacion() {
        if (this.estado != EstadoPedido.ABIERTO) {
            throw new IllegalStateException(
                String.format("No se puede modificar un pedido en estado %s. Solo se permiten modificaciones en estado ABIERTO.", 
                    this.estado)
            );
        }
    }

    /**
     * Agrega un producto al pedido con la lógica de Snapshot.
     *      * 
     * AC1 - Gestión de ítems: Permite agregar el mismo producto múltiples veces.
     * AC2 - Personalización: Soporta cantidad y observaciones.
     * AC3 - Inmutabilidad de Precios (Snapshot): Captura el precio actual del producto.
     * AC4 - Validación de Estado: Solo permite agregar a pedidos ABIERTOS.
     * AC5 - Aislamiento Multi-tenant: Valida que producto y pedido pertenezcan al mismo local.
     * 
     * @param producto el producto del catálogo a agregar
     * @param cantidad cantidad de unidades (debe ser > 0)
     * @param observaciones notas adicionales (ej: "sin cebolla"), puede ser null
     * @throws IllegalStateException si el pedido NO está en estado ABIERTO
     * @throws IllegalArgumentException si el producto no pertenece al mismo local
     * @throws IllegalArgumentException si la cantidad es <= 0
     */
    public void agregarProducto(Producto producto, int cantidad, String observaciones) {
        Objects.requireNonNull(producto, "El producto no puede ser null");
        
        // AC4 - Validación de Estado (HU-07)
        validarPermiteModificacion();
        
        // AC5 - Aislamiento Multi-tenant
        if (!this.localId.equals(producto.getLocalId())) {
            throw new IllegalArgumentException(
                String.format("El producto (local: %s) no pertenece al mismo local que el pedido (local: %s)", 
                    producto.getLocalId().getValue(), 
                    this.localId.getValue())
            );
        }
        
        // AC3 - Patrón Snapshot: Crear ítem capturando precio y nombre actuales
        ItemPedido nuevoItem = ItemPedido.crearDesdeProducto(
            ItemPedidoId.generate(),
            this.id,
            producto,
            cantidad,
            observaciones
        );
        
        // AC1 - Permitir múltiples líneas del mismo producto
        this.items.add(nuevoItem);
    }

    /**
     * Calcula el subtotal del pedido sumando los subtotales de todos los ítems.
     * Este cálculo NO incluye descuentos.
     * 
     * Regla de Oro del Dominio:
     * "El total del pedido se calcula a partir de los ítems base + descuentos acumulables"
     * 
     * @return el subtotal antes de aplicar descuentos
     */
    public BigDecimal calcularSubtotalItems() {
        return items.stream()
            .map(ItemPedido::calcularSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el total final del pedido aplicando descuentos sobre el subtotal.
     * 
     * Regla de Oro del Dominio:
     * "El total del pedido se calcula a partir de los ítems base + descuentos acumulables"
     * 
     * Actualmente (sin implementación de descuentos), este método retorna
     * el mismo valor que calcularSubtotalItems(). Cuando se implementen descuentos,
     * este método restará los descuentos acumulados al subtotal.
     * 
     * @return el total final del pedido (subtotal - descuentos)
     */
    public BigDecimal calcularTotal() {
        BigDecimal subtotal = calcularSubtotalItems();
        
        // MVP: Sin descuentos implementados aún
        // Cuando se agreguen descuentos, aquí se aplicarían:
        // BigDecimal totalDescuentos = descuentos.stream()
        //     .map(DescuentoAplicado::getMonto)
        //     .reduce(BigDecimal.ZERO, BigDecimal::add);
        // return subtotal.subtract(totalDescuentos);
        
        return subtotal;
    }


    /**
     * Finaliza el pedido registrando el medio de pago y la fecha de cierre.
     * Transiciona el estado de ABIERTO a CERRADO.
     *
     * Reglas de negocio:
     * - Solo se pueden cerrar pedidos en estado ABIERTO
     * - El pedido debe tener al menos un ítem cargado
     * - El medio de pago es obligatorio
     * - La fecha de cierre queda registrada para auditoría
     *
     * @param medio el medio de pago utilizado (obligatorio)
     * @param fechaCierre la fecha y hora del cierre (obligatorio)
     * @throws IllegalStateException si el pedido no está ABIERTO
     * @throws IllegalArgumentException si el pedido no tiene ítems o el medio de pago es nulo
     */
    public void finalizar(MedioPago medio, LocalDateTime fechaCierre) {
        Objects.requireNonNull(medio, "El medio de pago es obligatorio para cerrar el pedido");
        Objects.requireNonNull(fechaCierre, "La fecha de cierre es obligatoria");

        if (this.estado != EstadoPedido.ABIERTO) {
            throw new IllegalStateException("Solo se pueden cerrar pedidos que estén en estado ABIERTO");
        }

        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("No se puede cerrar un pedido sin ítems");
        }

        this.medioPago = medio;
        this.fechaCierre = fechaCierre;
        this.estado = EstadoPedido.CERRADO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pedido pedido = (Pedido) o;
        return Objects.equals(id, pedido.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
