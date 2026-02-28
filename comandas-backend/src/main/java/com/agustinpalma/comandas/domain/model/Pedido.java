package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate Root del contexto de Pedidos.
 * Representa una comanda/pedido asociado a una mesa.
 * 
 * Después del cierre, el pedido se vuelve inmutable gracias al snapshot contable
 * que congela montoSubtotalFinal, montoDescuentosFinal y montoTotalFinal.
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
    
    // HU-14: Descuento global dinámico (opcional)
    private DescuentoManual descuentoGlobal;  // null si no tiene descuento global

    // Pagos parciales/split del pedido
    private final List<Pago> pagos;

    // Snapshot contable: se congela al cerrar para inmutabilidad financiera
    private BigDecimal montoSubtotalFinal;
    private BigDecimal montoDescuentosFinal;
    private BigDecimal montoTotalFinal;

    public Pedido(PedidoId id, LocalId localId, MesaId mesaId, int numero, EstadoPedido estado, LocalDateTime fechaApertura) {
        this.id = Objects.requireNonNull(id, "El id del pedido no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.mesaId = Objects.requireNonNull(mesaId, "El mesaId no puede ser null");
        this.numero = validarNumero(numero);
        this.estado = Objects.requireNonNull(estado, "El estado del pedido no puede ser null");
        this.fechaApertura = Objects.requireNonNull(fechaApertura, "La fecha de apertura no puede ser null");
        this.items = new ArrayList<>();
        this.pagos = new ArrayList<>();
    }

    /**
     * Factory method para reconstrucción completa desde persistencia.
     * 
     * Este es el ÚNICO punto de entrada para rehidratar un aggregate Pedido
     * con todo su estado interno (ítems, pagos, descuentos, snapshot contable).
     * Solo debe ser invocado por la capa de infraestructura (mappers).
     * 
     * Al centralizar la reconstrucción aquí, impedimos que capas externas
     * inyecten estado arbitrario en el aggregate vía setters individuales.
     * 
     * @param id identificador del pedido
     * @param localId local al que pertenece
     * @param mesaId mesa asociada
     * @param numero número secuencial
     * @param estado estado actual
     * @param fechaApertura fecha de apertura
     * @param fechaCierre fecha de cierre (null si está abierto)
     * @param items ítems del pedido (ya reconstruidos)
     * @param pagos pagos registrados (vacío si está abierto)
     * @param descuentoGlobal descuento global aplicado (null si no tiene)
     * @param montoSubtotalFinal snapshot contable (null si está abierto)
     * @param montoDescuentosFinal snapshot contable (null si está abierto)
     * @param montoTotalFinal snapshot contable (null si está abierto)
     * @return Pedido completamente reconstruido
     */
    public static Pedido reconstruirDesdePersistencia(
            PedidoId id, LocalId localId, MesaId mesaId, int numero,
            EstadoPedido estado, LocalDateTime fechaApertura, LocalDateTime fechaCierre,
            List<ItemPedido> items, List<Pago> pagos,
            DescuentoManual descuentoGlobal,
            BigDecimal montoSubtotalFinal, BigDecimal montoDescuentosFinal, BigDecimal montoTotalFinal
    ) {
        Pedido pedido = new Pedido(id, localId, mesaId, numero, estado, fechaApertura);
        pedido.fechaCierre = fechaCierre;
        
        if (items != null) {
            pedido.items.addAll(items);
        }
        if (pagos != null) {
            pedido.pagos.addAll(pagos);
        }
        pedido.descuentoGlobal = descuentoGlobal;
        pedido.montoSubtotalFinal = montoSubtotalFinal;
        pedido.montoDescuentosFinal = montoDescuentosFinal;
        pedido.montoTotalFinal = montoTotalFinal;
        
        return pedido;
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
     * Busca un ítem existente en el pedido por su ProductoId.
     * 
     * @deprecated Usar {@link #buscarItemConMismaConfiguracion(ProductoId, String, List)} en su lugar.
     * Este método no considera observaciones ni extras, lo cual causa fusiones incorrectas.
     * 
     * @param productoId el ID del producto a buscar
     * @return Optional con el ItemPedido encontrado, o empty si no existe
     */
    @Deprecated
    public Optional<ItemPedido> buscarItemPorProductoId(ProductoId productoId) {
        Objects.requireNonNull(productoId, "El productoId no puede ser null");
        return this.items.stream()
            .filter(item -> item.getProductoId().equals(productoId))
            .findFirst();
    }

    /**
     * Busca un ítem existente con la misma configuración exacta.
     * 
     * La identidad de configuración considera:
     * - Mismo productoId
     * - Misma observación
     * - Mismos extras (como multiset)
     * 
     * Si no se encuentra un ítem idéntico, se debe crear uno nuevo.
     * Esto evita el bug de fusión incorrecta donde "Hamburguesa" y
     * "Hamburguesa sin cebolla" se combinaban erróneamente.
     * 
     * @param productoId el ID del producto
     * @param observacion la observación del ítem (puede ser null)
     * @param extras los extras del ítem (puede ser vacía)
     * @return Optional con el ItemPedido que tiene la misma configuración
     */
    public Optional<ItemPedido> buscarItemConMismaConfiguracion(
            ProductoId productoId, String observacion, List<ExtraPedido> extras) {
        Objects.requireNonNull(productoId, "El productoId no puede ser null");
        return this.items.stream()
            .filter(item -> item.coincideConfiguracion(productoId, observacion, extras))
            .findFirst();
    }

    /**
     * Calcula el subtotal del pedido sumando los subtotales de todos los ítems.
     * Este cálculo NO incluye descuentos.
     * 
     * Regla de Oro del Dominio:
     * "El total del pedido se calcula a partir de los ítems base + descuentos acumulables"
     * 
     * HU-05.1 + HU-22: Incluye el costo de los extras.
     * Usa calcularSubtotalLinea() que suma base + extras.
     * 
     * @return el subtotal antes de aplicar descuentos (incluye extras)
     */
    public BigDecimal calcularSubtotalItems() {
        return items.stream()
            .map(ItemPedido::calcularSubtotalLinea)  // HU-05.1: Incluye extras
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
     * Invariante para MONTO_FIJO: el valor no puede exceder la base gravable actual
     * del pedido (suma de precios finales de ítems), para evitar totales negativos.
     * 
     * @param descuentoGlobal el descuento a aplicar, o null para remover el descuento
     * @throws IllegalArgumentException si es MONTO_FIJO y el valor excede la base gravable
     */
    public void aplicarDescuentoGlobal(DescuentoManual descuentoGlobal) {
        if (descuentoGlobal != null 
                && descuentoGlobal.getTipo() == ModoDescuento.MONTO_FIJO) {
            BigDecimal baseGravable = items.stream()
                .map(ItemPedido::calcularPrecioFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (descuentoGlobal.getValor().compareTo(baseGravable) > 0) {
                throw new IllegalArgumentException(
                    String.format("El monto fijo ($%s) no puede superar la base gravable del pedido ($%s)",
                        descuentoGlobal.getValor().toPlainString(), baseGravable.toPlainString())
                );
            }
        }
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
     * Materializa la narrativa económica del pedido como lista explícita de ajustes.
     * 
     * Recorre todos los mecanismos de descuento del agregado y los convierte
     * en AjusteEconomico con monto final (snapshot monetario), eliminando
     * la necesidad de inferir descuentos por resta subtotal - total.
     * 
     * El Pedido se convierte en la única fuente de verdad del relato económico.
     * 
     * Orden de materialización:
     * 1. Promociones automáticas por ítem (HU-10) — snapshot fijo
     * 2. Descuentos manuales por ítem (HU-14) — calculados dinámicamente
     * 3. Descuento global (HU-14) — calculado dinámicamente sobre base gravable
     * 
     * @return lista inmutable de ajustes económicos (puede estar vacía)
     */
    public List<AjusteEconomico> obtenerAjustesEconomicos() {
        List<AjusteEconomico> ajustes = new ArrayList<>();

        // 1. Promociones automáticas por ítem (HU-10)
        for (ItemPedido item : items) {
            if (item.tienePromocion()) {
                String descripcion = item.getNombrePromocion() != null
                    ? item.getNombrePromocion()
                    : "Promoción";
                ajustes.add(new AjusteEconomico(
                    AjusteEconomico.TipoAjuste.PROMOCION,
                    AjusteEconomico.AmbitoAjuste.ITEM,
                    descripcion,
                    item.getMontoDescuento()
                ));
            }
        }

        // 2. Descuentos manuales por ítem (HU-14)
        for (ItemPedido item : items) {
            if (item.tieneDescuentoManual()) {
                String descripcion = item.getDescuentoManual().getRazon();
                if (descripcion == null || descripcion.isBlank()) {
                    descripcion = "Descuento manual";
                }
                ajustes.add(new AjusteEconomico(
                    AjusteEconomico.TipoAjuste.MANUAL,
                    AjusteEconomico.AmbitoAjuste.ITEM,
                    descripcion,
                    item.calcularMontoDescuentoManual()
                ));
            }
        }

        // 3. Descuento global (HU-14)
        if (descuentoGlobal != null) {
            String descripcion = descuentoGlobal.getRazon();
            if (descripcion == null || descripcion.isBlank()) {
                descripcion = "Descuento global";
            }
            ajustes.add(new AjusteEconomico(
                AjusteEconomico.TipoAjuste.MANUAL,
                AjusteEconomico.AmbitoAjuste.TOTAL,
                descripcion,
                calcularMontoDescuentoGlobal()
            ));
        }

        return Collections.unmodifiableList(ajustes);
    }

    /**
     * Cierra el pedido registrando los pagos y capturando el snapshot contable.
     * 
     * Este es un evento de negocio crítico que consolida:
     * - La inmutabilidad financiera (snapshot de montos)
     * - El registro de pagos (soporte multi-pago / split)
     * - El cambio de estado a CERRADO
     * 
     * Reglas de negocio:
     * - Solo se pueden cerrar pedidos en estado ABIERTO
     * - El pedido debe tener al menos un ítem cargado
     * - La suma de los pagos debe coincidir exactamente con el total calculado
     * - Los montos se congelan al cerrar: ya no se recalculan
     * - Después del cierre, el pedido se vuelve inmutable
     * 
     * @param pagosRecibidos lista de pagos (puede ser un solo pago o split)
     * @param fechaCierre la fecha y hora del cierre
     * @throws IllegalStateException si el pedido no está ABIERTO
     * @throws IllegalArgumentException si el pedido no tiene ítems
     * @throws IllegalArgumentException si la suma de pagos no coincide con el total
     */
    public void cerrar(List<Pago> pagosRecibidos, LocalDateTime fechaCierre) {
        Objects.requireNonNull(pagosRecibidos, "Los pagos no pueden ser null");
        Objects.requireNonNull(fechaCierre, "La fecha de cierre es obligatoria");

        if (this.estado != EstadoPedido.ABIERTO) {
            throw new IllegalStateException("Solo se pueden cerrar pedidos que estén en estado ABIERTO");
        }

        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("No se puede cerrar un pedido sin ítems");
        }

        if (pagosRecibidos.isEmpty()) {
            throw new IllegalArgumentException("Debe registrarse al menos un pago para cerrar el pedido");
        }

        // Calcular el total actual antes de congelar
        BigDecimal totalCalculado = calcularTotal();
        BigDecimal subtotalCalculado = calcularSubtotalItems();
        BigDecimal descuentosCalculados = subtotalCalculado.subtract(totalCalculado);

        // Validar que la suma de pagos coincide exactamente con el total
        BigDecimal sumaPagos = pagosRecibidos.stream()
            .map(Pago::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumaPagos.compareTo(totalCalculado) != 0) {
            throw new IllegalArgumentException(
                String.format("La suma de pagos (%s) no coincide con el total del pedido (%s)",
                    sumaPagos.toPlainString(), totalCalculado.toPlainString())
            );
        }

        // Congelar snapshot contable (inmutabilidad financiera)
        this.montoSubtotalFinal = subtotalCalculado;
        this.montoDescuentosFinal = descuentosCalculados;
        this.montoTotalFinal = totalCalculado;

        // Registrar pagos
        this.pagos.clear();
        this.pagos.addAll(pagosRecibidos);

        // Transicionar estado
        this.fechaCierre = fechaCierre;
        this.estado = EstadoPedido.CERRADO;
    }

    /**
     * Método de retrocompatibilidad para cierre con pago único.
     * Delega al nuevo método cerrar() con pagos.
     * 
     * @deprecated Usar {@link #cerrar(List, LocalDateTime)} en su lugar
     */
    public void finalizar(MedioPago medio, LocalDateTime fechaCierre) {
        Objects.requireNonNull(medio, "El medio de pago es obligatorio para cerrar el pedido");
        Objects.requireNonNull(fechaCierre, "La fecha de cierre es obligatoria");

        Pago pagoUnico = new Pago(medio, calcularTotal(), fechaCierre);
        cerrar(List.of(pagoUnico), fechaCierre);
    }

    /**
     * HU-14: Reabre un pedido previamente cerrado.
     * 
     * Esta es una "válvula de escape" operativa para corregir errores humanos
     * antes del cierre de caja definitivo. Permite revertir un pedido CERRADO
     * a ABIERTO, eliminando el snapshot contable y los pagos registrados.
     * 
     * Reglas de negocio (Invariantes):
     * - AC1: Solo se pueden reabrir pedidos en estado CERRADO
     * - AC2: Revierte estado a ABIERTO
     * - AC3: Limpia snapshot contable (montos finales vuelven a null)
     * - AC4: Elimina todos los pagos registrados (orphanRemoval en JPA)
     * - AC5: Registra auditoría de quién realizó la reapertura
     * 
     * ADVERTENCIA: Esta operación es destructiva. Los pagos previos se eliminan físicamente.
     * Solo debe usarse en casos excepcionales de corrección operativa.
     * 
     * @param fechaReapertura la fecha y hora de la reapertura
     * @throws IllegalStateException si el pedido no está en estado CERRADO
     */
    public void reabrir(LocalDateTime fechaReapertura) {
        Objects.requireNonNull(fechaReapertura, "La fecha de reapertura es obligatoria");

        // AC1: Validar que el pedido esté CERRADO
        if (this.estado != EstadoPedido.CERRADO) {
            throw new IllegalStateException(
                String.format("Solo se pueden reabrir pedidos en estado CERRADO. Estado actual: %s", 
                    this.estado)
            );
        }

        // AC2: Revertir estado a ABIERTO
        this.estado = EstadoPedido.ABIERTO;

        // AC3: Limpiar snapshot contable (los montos vuelven a ser calculados dinámicamente)
        this.montoSubtotalFinal = null;
        this.montoDescuentosFinal = null;
        this.montoTotalFinal = null;

        // AC4: Eliminar pagos (orphanRemoval=true en JPA provocará DELETE físico)
        this.pagos.clear();

        // Limpiar fecha de cierre
        this.fechaCierre = null;
        
        // AC5: La auditoría se maneja en la capa de aplicación
        // El UseCase registrará quién y cuándo realizó la reapertura
    }

    /**
     * Corrección in-place de un pedido cerrado.
     * 
     * Permite ajustar cantidades de ítems y pagos SIN reabrir la mesa.
     * El pedido permanece en estado CERRADO — solo se actualiza el snapshot contable.
     * 
     * Caso de uso principal: el operador detecta un error en el cierre
     * (cantidad equivocada, medio de pago incorrecto) y necesita corregirlo
     * sin afectar el flujo operativo del salón.
     * 
     * Reglas de negocio:
     * - Solo se pueden corregir pedidos CERRADO
     * - Al menos un ítem debe permanecer tras la corrección
     * - La suma de pagos debe coincidir exactamente con el nuevo total
     * - Los precios unitarios NO se modifican (son snapshot histórico)
     * - La fechaCierre original se preserva (momento real de la operación)
     * 
     * @param cantidadesCorregidas mapa itemId → nueva cantidad (0 = eliminar)
     * @param nuevosPagos lista de pagos corregidos
     * @throws IllegalStateException si el pedido no está CERRADO
     * @throws IllegalArgumentException si quedaría sin ítems o pagos no coinciden
     */
    public void corregir(Map<ItemPedidoId, Integer> cantidadesCorregidas, List<Pago> nuevosPagos) {
        Objects.requireNonNull(nuevosPagos, "Los pagos no pueden ser null");

        if (this.estado != EstadoPedido.CERRADO) {
            throw new IllegalStateException(
                String.format("Solo se pueden corregir pedidos en estado CERRADO. Estado actual: %s", this.estado)
            );
        }

        if (nuevosPagos.isEmpty()) {
            throw new IllegalArgumentException("Debe registrarse al menos un pago para la corrección");
        }

        // Aplicar correcciones de cantidad sobre los ítems existentes
        if (cantidadesCorregidas != null && !cantidadesCorregidas.isEmpty()) {
            List<ItemPedido> itemsARemover = new ArrayList<>();

            for (Map.Entry<ItemPedidoId, Integer> entry : cantidadesCorregidas.entrySet()) {
                ItemPedido item = buscarItemPorId(entry.getKey());
                int nuevaCantidad = entry.getValue();

                if (nuevaCantidad < 0) {
                    throw new IllegalArgumentException(
                        String.format("La cantidad no puede ser negativa. Recibido: %d para ítem %s",
                            nuevaCantidad, entry.getKey().getValue())
                    );
                }

                if (nuevaCantidad == 0) {
                    itemsARemover.add(item);
                } else if (nuevaCantidad != item.getCantidad()) {
                    item.actualizarCantidad(nuevaCantidad);
                }
            }

            this.items.removeAll(itemsARemover);
        }

        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("No se puede dejar un pedido sin ítems");
        }

        // Recalcular snapshot contable con las cantidades corregidas
        BigDecimal nuevoSubtotal = calcularSubtotalItems();
        BigDecimal nuevoTotal = calcularTotal();
        BigDecimal nuevosDescuentos = nuevoSubtotal.subtract(nuevoTotal);

        // Validar que la suma de pagos coincide exactamente con el nuevo total
        BigDecimal sumaPagos = nuevosPagos.stream()
            .map(Pago::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumaPagos.compareTo(nuevoTotal) != 0) {
            throw new IllegalArgumentException(
                String.format("La suma de pagos (%s) no coincide con el total corregido (%s)",
                    sumaPagos.toPlainString(), nuevoTotal.toPlainString())
            );
        }

        // Re-congelar snapshot contable
        this.montoSubtotalFinal = nuevoSubtotal;
        this.montoDescuentosFinal = nuevosDescuentos;
        this.montoTotalFinal = nuevoTotal;

        // Reemplazar pagos
        this.pagos.clear();
        this.pagos.addAll(nuevosPagos);
    }

    // ============================================
    // Getters de pagos y snapshot contable
    // ============================================

    /**
     * Retorna los pagos registrados al cierre del pedido.
     * 
     * @return lista inmutable de pagos (vacía si el pedido está abierto)
     */
    public List<Pago> getPagos() {
        return Collections.unmodifiableList(pagos);
    }

    /**
     * Retorna el subtotal final congelado al cierre.
     * 
     * @return montoSubtotalFinal o null si el pedido está abierto
     */
    public BigDecimal getMontoSubtotalFinal() {
        return montoSubtotalFinal;
    }

    /**
     * Retorna el monto de descuentos congelado al cierre.
     * 
     * @return montoDescuentosFinal o null si el pedido está abierto
     */
    public BigDecimal getMontoDescuentosFinal() {
        return montoDescuentosFinal;
    }

    /**
     * Retorna el total final congelado al cierre.
     * 
     * @return montoTotalFinal o null si el pedido está abierto
     */
    public BigDecimal getMontoTotalFinal() {
        return montoTotalFinal;
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
