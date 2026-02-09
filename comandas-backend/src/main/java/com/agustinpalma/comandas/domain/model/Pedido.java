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
    
    // HU-14: Descuento global dinámico (opcional)
    private DescuentoManual descuentoGlobal;  // null si no tiene descuento global

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
     * NOTA: Este método NO aplica promociones. Para aplicar promociones automáticamente,
     * usar agregarItem(ItemPedido) con un ítem construido por MotorReglasService.
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
     * HU-10: Agrega un ítem ya construido al pedido.
     * 
     * Este método permite agregar un ItemPedido que ya fue creado externamente
     * (por ejemplo, por el MotorReglasService con promociones aplicadas).
     * 
     * Validaciones:
     * - AC4: El pedido debe estar ABIERTO
     * - AC5: El ítem debe pertenecer a este pedido (pedidoId coincide)
     * 
     * @param item el ítem ya construido (con o sin promoción)
     * @throws IllegalStateException si el pedido NO está en estado ABIERTO
     * @throws IllegalArgumentException si el ítem no pertenece a este pedido
     */
    public void agregarItem(ItemPedido item) {
        Objects.requireNonNull(item, "El item no puede ser null");
        
        // AC4 - Validación de Estado
        validarPermiteModificacion();
        
        // Validar que el ítem pertenece a este pedido
        if (!this.id.equals(item.getPedidoId())) {
            throw new IllegalArgumentException(
                String.format("El ítem pertenece al pedido %s, pero se intentó agregar al pedido %s",
                    item.getPedidoId().getValue(),
                    this.id.getValue())
            );
        }
        
        this.items.add(item);
    }

    // ============================================
    // HU-20/HU-21: Gestión dinámica de ítems
    // ============================================

    /**
     * HU-21: Actualiza la cantidad de un ítem existente en el pedido.
     * 
     * Este método es un comando de intención del usuario con interpretación semántica:
     * - cantidad == actual → Operación idempotente (no hacer nada)
     * - cantidad == 0     → El usuario no desea más el producto → eliminar ítem
     * - cantidad < 0      → Input inválido → IllegalArgumentException
     * - cantidad > 0      → Actualizar cantidad y limpiar promoción para re-evaluación
     * 
     * IMPORTANTE: Después de actualizar, las promociones del ítem se limpian.
     * El MotorReglasService debe re-evaluar las promociones de todo el pedido.
     * 
     * @param itemId identificador del ítem a modificar
     * @param nuevaCantidad la nueva cantidad deseada
     * @throws IllegalStateException si el pedido NO está en estado ABIERTO
     * @throws IllegalArgumentException si nuevaCantidad es negativa
     * @throws IllegalArgumentException si el ítem no se encuentra en el pedido
     */
    public void actualizarCantidadItem(ItemPedidoId itemId, int nuevaCantidad) {
        Objects.requireNonNull(itemId, "El itemId no puede ser null");
        validarPermiteModificacion();

        if (nuevaCantidad < 0) {
            throw new IllegalArgumentException(
                String.format("La cantidad no puede ser negativa. Recibido: %d", nuevaCantidad)
            );
        }

        // Buscar el ítem dentro del aggregate
        ItemPedido item = buscarItemPorId(itemId);

        // Idempotencia: si la cantidad es igual, no hacer nada
        if (item.getCantidad() == nuevaCantidad) {
            return;
        }

        // Cantidad 0: el usuario quiere eliminar el ítem
        if (nuevaCantidad == 0) {
            eliminarItem(itemId);
            return;
        }

        // Cantidad > 0: actualizar y limpiar promoción para re-evaluación
        item.actualizarCantidad(nuevaCantidad);
        item.limpiarPromocion();
    }

    /**
     * HU-20: Elimina un ítem del pedido.
     * 
     * Gracias a orphanRemoval = true en la capa JPA, esto provocará
     * un DELETE físico en la tabla items_pedido al persistir el Pedido.
     * 
     * @param itemId identificador del ítem a eliminar
     * @throws IllegalStateException si el pedido NO está en estado ABIERTO
     * @throws IllegalArgumentException si el ítem no se encuentra en el pedido
     */
    public void eliminarItem(ItemPedidoId itemId) {
        Objects.requireNonNull(itemId, "El itemId no puede ser null");
        validarPermiteModificacion();

        ItemPedido item = buscarItemPorId(itemId);
        this.items.remove(item);
    }

    /**
     * HU-20/HU-21: Limpia las promociones de TODOS los ítems del pedido.
     * 
     * Esto es necesario antes de re-evaluar promociones con el MotorReglasService,
     * ya que cambios en un ítem pueden afectar combos y promociones de otros ítems.
     * 
     * Ejemplo: eliminar el trigger de un combo debe hacer que el target pierda su descuento.
     */
    public void limpiarPromocionesItems() {
        this.items.forEach(ItemPedido::limpiarPromocion);
    }

    /**
     * Busca un ítem dentro del pedido por su ID.
     * 
     * @param itemId identificador del ítem
     * @return el ítem encontrado
     * @throws IllegalArgumentException si el ítem no existe en este pedido
     */
    private ItemPedido buscarItemPorId(ItemPedidoId itemId) {
        return this.items.stream()
            .filter(item -> item.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("No se encontró el ítem con ID %s en el pedido %s",
                    itemId.getValue(), this.id.getValue())
            ));
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
     * Calcula el total final del pedido aplicando todos los descuentos.
     * 
     * Regla de Oro del Dominio:
     * "El total del pedido se calcula a partir de los ítems base + descuentos acumulables"
     * 
     * HU-14: Implementación de descuento global dinámico.
     * 
     * Fórmula:
     * 1. baseGravable = Sumatoria(item.calcularPrecioFinal())
     *    -> Cada item ya incluye sus promociones automáticas (HU-10) y descuentos manuales por ítem
     * 2. montoDescuentoGlobal = descuentoGlobal.calcularMonto(baseGravable)
     * 3. totalFinal = baseGravable - montoDescuentoGlobal
     * 
     * IMPORTANTE: La base gravable usa calcularPrecioFinal() de cada ítem, que ya incluye
     * promociones automáticas y descuentos manuales por ítem. Así se respeta la jerarquía:
     * 1° Promociones automáticas (HU-10)
     * 2° Descuentos manuales por ítem
     * 3° Descuento global (sobre el total final de los ítems)
     * 
     * @return el total final del pedido (subtotal de ítems - descuento global)
     */
    public BigDecimal calcularTotal() {
        // 1. Base gravable: suma de todos los ítems con sus descuentos aplicados
        BigDecimal baseGravable = items.stream()
            .map(ItemPedido::calcularPrecioFinal)  // Usa precioFinal (ya con promos y desc. manuales)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 2. Aplicar descuento global sobre la base gravable (HU-14)
        if (descuentoGlobal != null) {
            BigDecimal montoDescuentoGlobal = descuentoGlobal.calcularMonto(baseGravable);
            return baseGravable.subtract(montoDescuentoGlobal);
        }
        
        // Sin descuento global
        return baseGravable;
    }

    // ============================================
    // HU-14: Getters y setters de descuento global
    // ============================================

    /**
     * Retorna el descuento global aplicado a todo el pedido.
     * 
     * @return DescuentoManual o null si no tiene descuento global
     */
    public DescuentoManual getDescuentoGlobal() {
        return descuentoGlobal;
    }

    /**
     * Aplica un descuento global a todo el pedido.
     * Sobrescribe cualquier descuento global previo.
     * 
     * HU-14: El descuento global es dinámico. Se recalcula en cada invocación de calcularTotal().
     * 
     * @param descuentoGlobal el descuento a aplicar, o null para remover el descuento
     */
    public void aplicarDescuentoGlobal(DescuentoManual descuentoGlobal) {
        this.descuentoGlobal = descuentoGlobal;
    }

    /**
     * Indica si este pedido tiene un descuento global aplicado.
     * 
     * @return true si tiene descuento global
     */
    public boolean tieneDescuentoGlobal() {
        return descuentoGlobal != null;
    }

    /**
     * Calcula el monto del descuento global aplicado a este pedido.
     * 
     * HU-14: Este cálculo es dinámico. Depende del total de los ítems con sus descuentos.
     * 
     * @return monto del descuento global, o BigDecimal.ZERO si no tiene
     */
    public BigDecimal calcularMontoDescuentoGlobal() {
        if (descuentoGlobal == null) {
            return BigDecimal.ZERO;
        }
        
        // Base gravable = suma de ítems con sus descuentos aplicados
        BigDecimal baseGravable = items.stream()
            .map(ItemPedido::calcularPrecioFinal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        return descuentoGlobal.calcularMonto(baseGravable);
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
