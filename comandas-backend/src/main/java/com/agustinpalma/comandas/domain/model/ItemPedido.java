package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.ModoDescuento;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * 
 * HU-05.1 + HU-22: Soporte para extras dinámicos y controlados.
 * Los extras se agregan como Value Objects (ExtraPedido) con su propio snapshot de precio.
 * Fórmula de subtotal: cantidad × (precioUnitarioBase + sum(precioExtras))
 * Las promociones SOLO aplican a precioUnitarioBase, NUNCA a extras.
 */
public class ItemPedido {

    private final ItemPedidoId id;
    private final PedidoId pedidoId;
    private final ProductoId productoId;
    private final String nombreProducto;  // Snapshot del nombre
    private int cantidad;
    private final BigDecimal precioUnitario;  // Snapshot del precio BASE del producto
    private String observacion;
    
    // Snapshot de clasificación del producto al momento de la venta
    // Estos campos son inmutables y NO dependen del Producto vivo en catálogo.
    private final ProductoId grupoVarianteIdSnapshot;  // Grupo de variantes al momento de la venta (nullable)
    private final Integer cantidadDiscosSnapshot;       // Jerarquía de variante capturada (nullable)
    private final CategoriaId categoriaIdSnapshot;      // Categoría al momento de la venta (nullable)
    
    // HU-10: Campos de snapshot de promoción
    // HU-20/HU-21: Ya no son final — se resetean al recalcular promociones tras cambios en ítems
    private BigDecimal montoDescuento;             // Valor monetario descontado (ej: $250)
    private String nombrePromocion;                // Para mostrar al cliente (ej: "Promo Merienda")
    private UUID promocionId;                      // Referencia para auditoría
    
    // HU-14: Descuento manual dinámico (opcional)
    private DescuentoManual descuentoManual;       // null si no tiene descuento manual
    
    // HU-05.1 + HU-22: Extras dinámicos con snapshot de precio
    private final List<ExtraPedido> extras;        // Lista mutable pero encapsulada

    /**
     * Constructor completo para reconstrucción desde persistencia.
     * Usado por la capa de infraestructura (JPA).
     * 
     * HU-10: Incluye campos de promoción para snapshot completo.
     * HU-14: Incluye descuento manual dinámico (opcional).
     * HU-05.1 + HU-22: Incluye lista de extras con snapshot.
     * Snapshot de clasificación: grupoVarianteId, cantidadDiscos, categoriaId.
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
            UUID promocionId,
            DescuentoManual descuentoManual,
            List<ExtraPedido> extras,
            ProductoId grupoVarianteIdSnapshot,
            Integer cantidadDiscosSnapshot,
            CategoriaId categoriaIdSnapshot
    ) {
        this.id = Objects.requireNonNull(id, "El id del item no puede ser null");
        this.pedidoId = Objects.requireNonNull(pedidoId, "El pedidoId no puede ser null");
        this.productoId = Objects.requireNonNull(productoId, "El productoId no puede ser null");
        this.nombreProducto = Objects.requireNonNull(nombreProducto, "El nombre del producto no puede ser null");
        this.cantidad = validarCantidad(cantidad);
        this.precioUnitario = validarPrecioUnitario(precioUnitario);
        this.observacion = observacion; // Puede ser null
        
        // Snapshot de clasificación (inmutables, pueden ser null)
        this.grupoVarianteIdSnapshot = grupoVarianteIdSnapshot;
        this.cantidadDiscosSnapshot = cantidadDiscosSnapshot;
        this.categoriaIdSnapshot = categoriaIdSnapshot;
        
        // HU-10: Campos de promoción (pueden ser null si no hay promo)
        this.montoDescuento = montoDescuento != null ? montoDescuento : BigDecimal.ZERO;
        this.nombrePromocion = nombrePromocion;
        this.promocionId = promocionId;
        
        // HU-14: Descuento manual (puede ser null)
        this.descuentoManual = descuentoManual;
        
        // HU-05.1 + HU-22: Extras (copia defensiva)
        this.extras = extras != null ? new ArrayList<>(extras) : new ArrayList<>();
    }

    /**
     * Constructor de compatibilidad para reconstrucción sin promoción ni extras.
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
             observacion, BigDecimal.ZERO, null, null, null, Collections.emptyList(),
             null, null, null);
    }

    /**
     * Factory method con PATRÓN SNAPSHOT (sin promoción ni extras).
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
        
        // SNAPSHOT: Copiamos los valores del producto en este momento (sin promoción ni extras)
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
            null,              // Sin ID de promoción
            null,              // Sin descuento manual
            Collections.emptyList(),  // Sin extras
            producto.getGrupoVarianteId(),      // Snapshot grupoVarianteId
            producto.getCantidadDiscosCarne(),   // Snapshot cantidadDiscos
            producto.getCategoriaId()            // Snapshot categoriaId
        );
    }

    /**
     * Factory method con PATRÓN SNAPSHOT incluyendo extras (HU-05.1 + HU-22).
     * 
     * Captura el precio del producto Y los extras al momento de creación.
     * Todos los valores quedan congelados (snapshot) para garantizar inmutabilidad histórica.
     * 
     * @param id identificador único del ítem
     * @param pedidoId identificador del pedido al que pertenece
     * @param producto entidad Producto desde la cual se capturan los valores
     * @param cantidad cantidad del producto (debe ser > 0)
     * @param observacion notas adicionales del cliente (ej: "sin cebolla")
     * @param extras lista de extras aplicados (ya con snapshot de precio)
     * @throws IllegalArgumentException si producto es null o cantidad <= 0
     */
    public static ItemPedido crearConExtras(
            ItemPedidoId id, 
            PedidoId pedidoId, 
            Producto producto, 
            int cantidad, 
            String observacion,
            List<ExtraPedido> extras
    ) {
        Objects.requireNonNull(producto, "El producto no puede ser null");
        
        // SNAPSHOT: Copiamos precio del producto Y extras (sin promoción por ahora)
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
            null,              // Sin ID de promoción
            null,              // Sin descuento manual
            extras != null ? new ArrayList<>(extras) : Collections.emptyList(),
            producto.getGrupoVarianteId(),
            producto.getCantidadDiscosCarne(),
            producto.getCategoriaId()
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
        
        // SNAPSHOT: Copiamos precio del producto Y descuento de la promoción (sin extras)
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
            promocionId,
            null,              // Sin descuento manual por defecto
            Collections.emptyList(),  // Sin extras
            producto.getGrupoVarianteId(),
            producto.getCantidadDiscosCarne(),
            producto.getCategoriaId()
        );
    }

    /**
     * Factory method completo con PROMOCIÓN y EXTRAS (HU-10 + HU-05.1 + HU-22).
     * 
     * Captura:
     * - Precio base del producto
     * - Descuento de promoción (SOLO sobre precio base)
     * - Extras con sus precios (NUNCA descontados)
     * 
     * Regla crítica: Las promociones NO descuentan extras.
     * 
     * @param id identificador único del ítem
     * @param pedidoId identificador del pedido al que pertenece
     * @param producto entidad Producto desde la cual se capturan los valores
     * @param cantidad cantidad del producto (debe ser > 0)
     * @param observacion notas adicionales del cliente
     * @param montoDescuento valor monetario del descuento calculado
     * @param nombrePromocion nombre de la promoción
     * @param promocionId UUID de la promoción
     * @param extras lista de extras aplicados (con snapshot de precio)
     * @throws IllegalArgumentException si producto es null o cantidad <= 0
     */
    public static ItemPedido crearCompleto(
            ItemPedidoId id, 
            PedidoId pedidoId, 
            Producto producto, 
            int cantidad, 
            String observacion,
            BigDecimal montoDescuento,
            String nombrePromocion,
            UUID promocionId,
            List<ExtraPedido> extras
    ) {
        Objects.requireNonNull(producto, "El producto no puede ser null");
        Objects.requireNonNull(montoDescuento, "El monto del descuento no puede ser null");
        
        // SNAPSHOT: Copiamos precio del producto, descuento Y extras
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
            promocionId,
            null,              // Sin descuento manual por defecto
            extras != null ? new ArrayList<>(extras) : Collections.emptyList(),
            producto.getGrupoVarianteId(),
            producto.getCantidadDiscosCarne(),
            producto.getCategoriaId()
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
    // Snapshot de clasificación del producto
    // ============================================

    /**
     * Grupo de variantes del producto al momento de la venta.
     * Inmutable: no cambia aunque el catálogo se actualice.
     * 
     * @return ProductoId del grupo de variantes, o null si no aplica
     */
    public ProductoId getGrupoVarianteIdSnapshot() {
        return grupoVarianteIdSnapshot;
    }

    /**
     * Cantidad de discos (jerarquía de variante) al momento de la venta.
     * Inmutable: no cambia aunque el catálogo se actualice.
     * 
     * @return cantidad de discos capturada, o null si no aplica
     */
    public Integer getCantidadDiscosSnapshot() {
        return cantidadDiscosSnapshot;
    }

    /**
     * Categoría del producto al momento de la venta.
     * Inmutable: no cambia aunque el catálogo se actualice.
     * 
     * @return CategoriaId capturada, o null si no estaba clasificado
     */
    public CategoriaId getCategoriaIdSnapshot() {
        return categoriaIdSnapshot;
    }
    
    // ============================================
    // HU-05.1 + HU-22: Getters y manejo de extras
    // ============================================
    
    /**
     * Retorna la lista de extras aplicados a este ítem.
     * 
     * @return lista inmutable de ExtraPedido
     */
    public List<ExtraPedido> getExtras() {
        return Collections.unmodifiableList(extras);
    }
    
    /**
     * Indica si este ítem tiene extras aplicados.
     * 
     * @return true si tiene al menos un extra
     */
    public boolean tieneExtras() {
        return extras != null && !extras.isEmpty();
    }
    
    /**
     * Calcula el precio total de los extras.
     * 
     * HU-05.1 + HU-22: Los extras se suman al precio base.
     * Fórmula: sum(precioSnapshot de cada extra)
     * 
     * @return precio total de extras (BigDecimal.ZERO si no hay extras)
     */
    public BigDecimal calcularPrecioExtrasTotal() {
        if (extras == null || extras.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return extras.stream()
            .map(ExtraPedido::getPrecioSnapshot)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calcula el precio base total (sin extras).
     * 
     * HU-05.1 + HU-22: Este es el precio sobre el cual aplican las promociones.
     * Fórmula: cantidad × precioUnitario
     * 
     * @return precio base total del producto (sin extras)
     */
    public BigDecimal calcularPrecioBaseTotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    // ============================================
    // HU-20/HU-21: Métodos de mutación controlada
    // ============================================

    /**
     * HU-21: Actualiza la cantidad del ítem.
     * Solo debe invocarse desde el Aggregate Root (Pedido).
     * 
     * El precioUnitario se mantiene (snapshot histórico).
     * 
     * @param nuevaCantidad la nueva cantidad (debe ser > 0)
     * @throws IllegalArgumentException si la cantidad es <= 0
     */
    void actualizarCantidad(int nuevaCantidad) {
        this.cantidad = validarCantidad(nuevaCantidad);
    }

    /**
     * HU-20/HU-21: Limpia el estado de promoción del ítem.
     * 
     * Esto es obligatorio antes de re-evaluar promociones con el MotorReglasService,
     * ya que el ítem debe volver a ser evaluado desde cero.
     * 
     * Resetea:
     * - montoDescuento → BigDecimal.ZERO
     * - nombrePromocion → null
     * - promocionId → null
     */
    void limpiarPromocion() {
        this.montoDescuento = BigDecimal.ZERO;
        this.nombrePromocion = null;
        this.promocionId = null;
    }

    /**
     * HU-20/HU-21: Aplica una promoción calculada al ítem.
     * 
     * Público porque es invocado por el MotorReglasService (domain.service),
     * que al estar en un paquete diferente necesita acceso directo.
     * 
     * ⚠️ Uso restringido: solo debe invocarse desde Domain Services
     * durante el recálculo de promociones.
     * 
     * @param montoDescuento el monto de descuento calculado
     * @param nombrePromocion el nombre de la promoción
     * @param promocionId el ID de la promoción para auditoría
     */
    public void aplicarPromocion(BigDecimal montoDescuento, String nombrePromocion, UUID promocionId) {
        this.montoDescuento = montoDescuento != null ? montoDescuento : BigDecimal.ZERO;
        this.nombrePromocion = nombrePromocion;
        this.promocionId = promocionId;
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

    // ============================================
    // HU-14: Getters y setters de descuento manual
    // ============================================

    /**
     * Retorna el descuento manual aplicado a este ítem.
     * 
     * @return DescuentoManual o null si no tiene descuento manual
     */
    public DescuentoManual getDescuentoManual() {
        return descuentoManual;
    }

    /**
     * Aplica un descuento manual a este ítem.
     * Sobrescribe cualquier descuento manual previo.
     * 
     * HU-14: El descuento manual es dinámico. Se recalcula en cada invocación de calcularPrecioFinal().
     * 
     * @param descuentoManual el descuento a aplicar, o null para remover el descuento
     */
    /**
     * Aplica un descuento manual a este ítem.
     * Sobrescribe cualquier descuento manual previo.
     * 
     * HU-14: El descuento manual es dinámico. Se recalcula en cada invocación de calcularPrecioFinal().
     * 
     * Invariante para MONTO_FIJO: el valor no puede exceder el remanente después de
     * promociones automáticas, para evitar precios finales negativos.
     * 
     * @param descuentoManual el descuento a aplicar, o null para remover el descuento
     * @throws IllegalArgumentException si es MONTO_FIJO y el valor excede el remanente
     */
    public void aplicarDescuentoManual(DescuentoManual descuentoManual) {
        if (descuentoManual != null 
                && descuentoManual.getTipo() == ModoDescuento.MONTO_FIJO) {
            BigDecimal remanente = calcularSubtotal().subtract(montoDescuento);
            if (descuentoManual.getValor().compareTo(remanente) > 0) {
                throw new IllegalArgumentException(
                    String.format("El monto fijo ($%s) no puede superar el remanente del ítem ($%s)",
                        descuentoManual.getValor().toPlainString(), remanente.toPlainString())
                );
            }
        }
        this.descuentoManual = descuentoManual;
    }

    /**
     * Indica si este ítem tiene un descuento manual aplicado.
     * 
     * @return true si tiene descuento manual
     */
    public boolean tieneDescuentoManual() {
        return descuentoManual != null;
    }

    /**
     * Calcula el monto del descuento manual aplicado a este ítem.
     * 
     * HU-14: Este cálculo es dinámico. Depende del remanente después de promociones.
     * 
     * @return monto del descuento manual, o BigDecimal.ZERO si no tiene
     */
    public BigDecimal calcularMontoDescuentoManual() {
        if (descuentoManual == null) {
            return BigDecimal.ZERO;
        }
        
        // Base gravable = remanente después de promociones automáticas
        BigDecimal remanenteDespuesPromo = calcularSubtotal().subtract(montoDescuento);
        return descuentoManual.calcularMonto(remanenteDespuesPromo);
    }

    /**
     * Calcula el subtotal de este ítem (precio base * cantidad).
     * Este es el precio SIN descuento.
     * 
     * HU-05.1 + HU-22: Mantiene retrocompatibilidad. Solo precio base.
     * Para incluir extras, usar calcularSubtotalLinea().
     *
     * @return el monto bruto del ítem (solo producto base)
     */
    public BigDecimal calcularSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
    
    /**
     * Calcula el subtotal de línea completo (base + extras).
     * 
     * HU-05.1 + HU-22: Incluye el costo de todos los extras.
     * Fórmula: cantidad × (precioUnitarioBase + sum(precioExtras))
     * 
     * Este es el monto BRUTO antes de aplicar descuentos.
     * Las promociones SOLO descuentan sobre precioBase, NO sobre extras.
     * 
     * @return monto bruto total de la línea (producto + extras)
     */
    public BigDecimal calcularSubtotalLinea() {
        BigDecimal precioBaseTotal = calcularPrecioBaseTotal();
        BigDecimal precioExtrasUnitario = calcularPrecioExtrasTotal();
        BigDecimal precioExtrasTotal = precioExtrasUnitario.multiply(BigDecimal.valueOf(cantidad));
        
        return precioBaseTotal.add(precioExtrasTotal);
    }

    /**
     * Calcula el precio final de este ítem aplicando descuentos.
     * 
     * HU-10: Primero aplica descuento de promoción (snapshot fijo SOLO sobre base)
     * HU-14: Luego aplica descuento manual (dinámico) sobre el remanente
     * HU-05.1 + HU-22: Los extras NUNCA se descuentan (aislamiento)
     * 
     * Fórmula:
     * 1. precioBaseTotal = precioUnitario * cantidad
     * 2. precioExtrasTotal = sum(extras) * cantidad
     * 3. remanenteDespuesPromo = precioBaseTotal - montoDescuentoAuto
     * 4. montoDescuentoManual = descuentoManual.calcularMonto(remanenteDespuesPromo)
     * 5. precioFinal = (remanenteDespuesPromo - montoDescuentoManual) + precioExtrasTotal
     * 
     * @return el monto neto del ítem después de todos los descuentos
     */
    public BigDecimal calcularPrecioFinal() {
        // 1. Precio base total (cantidad × precioUnitario)
        BigDecimal precioBaseTotal = calcularPrecioBaseTotal();
        
        // 2. Precio extras total (se suma al final SIN descuento)
        BigDecimal precioExtrasUnitario = calcularPrecioExtrasTotal();
        BigDecimal precioExtrasTotal = precioExtrasUnitario.multiply(BigDecimal.valueOf(cantidad));
        
        // 3. Remanente después de promoción automática (HU-10)
        // CRÍTICO: Las promociones SOLO descuentan sobre precio base
        BigDecimal remanenteDespuesPromo = precioBaseTotal.subtract(montoDescuento);
        
        // 4. Aplicar descuento manual sobre el remanente (HU-14)
        BigDecimal remanenteFinal = remanenteDespuesPromo;
        if (descuentoManual != null) {
            BigDecimal montoDescuentoManual = descuentoManual.calcularMonto(remanenteDespuesPromo);
            remanenteFinal = remanenteDespuesPromo.subtract(montoDescuentoManual);
        }
        
        // 5. Sumar extras (NUNCA descontados)
        return remanenteFinal.add(precioExtrasTotal);
    }

    // ============================================
    // Identidad de configuración para fusión
    // ============================================

    /**
     * Determina si otro ItemPedido tiene la misma configuración.
     * 
     * Dos ítems son fusionables SOLO si comparten:
     * - Mismo productoId
     * - Misma observación (null-safe, case-sensitive)
     * - Mismos extras (mismo conjunto de productoId + nombre + precio)
     * 
     * Si cualquiera de estos difiere, son platos personalizados distintos
     * y NO deben fusionarse.
     * 
     * Ejemplo:
     *   "Hamburguesa" vs "Hamburguesa sin cebolla" → false (distinta observación)
     *   "Hamburguesa + huevo" vs "Hamburguesa" → false (distintos extras)
     *   "Hamburguesa" vs "Hamburguesa" → true (fusionable)
     * 
     * @param otro el ítem contra el cual comparar configuración
     * @return true si la configuración es idéntica y pueden fusionarse
     */
    public boolean esMismaConfiguracion(ItemPedido otro) {
        if (otro == null) return false;
        
        // 1. Mismo producto
        if (!this.productoId.equals(otro.productoId)) return false;
        
        // 2. Misma observación (null-safe)
        if (!Objects.equals(this.observacion, otro.observacion)) return false;
        
        // 3. Mismos extras (comparación por valor — ExtraPedido implementa equals por valor)
        if (this.extras.size() != otro.extras.size()) return false;
        
        // Comparar como multiset: cada extra debe tener la misma frecuencia
        List<ExtraPedido> copiaOtro = new ArrayList<>(otro.extras);
        for (ExtraPedido extra : this.extras) {
            if (!copiaOtro.remove(extra)) {
                return false;  // Extra no encontrado en el otro
            }
        }
        return copiaOtro.isEmpty();
    }

    /**
     * Determina si este ítem tiene la misma configuración que los parámetros dados.
     * 
     * Usado por el Aggregate Root (Pedido) para buscar ítems fusionables
     * ANTES de crear un nuevo ItemPedido.
     * 
     * @param productoId el productoId a comparar
     * @param observacion la observación a comparar (null-safe)
     * @param extras los extras a comparar
     * @return true si la configuración coincide
     */
    public boolean coincideConfiguracion(ProductoId productoId, String observacion, List<ExtraPedido> extras) {
        if (!this.productoId.equals(productoId)) return false;
        if (!Objects.equals(this.observacion, observacion)) return false;
        
        List<ExtraPedido> otrosExtras = extras != null ? extras : Collections.emptyList();
        if (this.extras.size() != otrosExtras.size()) return false;
        
        List<ExtraPedido> copia = new ArrayList<>(otrosExtras);
        for (ExtraPedido extra : this.extras) {
            if (!copia.remove(extra)) return false;
        }
        return copia.isEmpty();
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
