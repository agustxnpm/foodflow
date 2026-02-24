package com.agustinpalma.comandas.domain.service;

import com.agustinpalma.comandas.domain.model.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de Dominio que orquesta la evaluación y aplicación de promociones.
 * 
 * HU-10: Aplicar promociones automáticamente.
 * 
 * Responsabilidades:
 * - Evaluar qué promociones aplican al agregar un producto
 * - Resolver conflictos de prioridad (mayor prioridad gana)
 * - Calcular el descuento según la estrategia
 * - Generar el ItemPedido con el snapshot del descuento
 * 
 * Principios:
 * - Servicio de dominio PURO: no depende de repositorios ni infraestructura
 * - Recibe todos los datos necesarios como parámetros
 * - Lógica de negocio compleja que no pertenece a una sola entidad
 * 
 * Regla de Oro del Dominio:
 * "El cálculo se hace UNA vez al agregar. Si mañana cambio el porcentaje de la promo,
 * el ítem guardado HOY no debe cambiar su montoDescuento" (Patrón Snapshot)
 */
public class MotorReglasService {

    /**
     * Evalúa las promociones candidatas y genera un ItemPedido con el descuento aplicado.
     * 
     * Algoritmo de Evaluación:
     * 1. Filtra promociones ACTIVAS con alcance definido
     * 2. Verifica criterios de activación (día, hora, etc.) usando ContextoValidacion
     * 3. Verifica que el producto sea TARGET en la promoción
     * 4. Para COMBO_CONDICIONAL: verifica que exista el TRIGGER en el pedido
     * 5. Resuelve conflictos por prioridad (mayor prioridad gana)
     * 6. Calcula el descuento según la estrategia ganadora
     * 7. Crea el ItemPedido con snapshot de precio y descuento
     * 
     * @param pedido el pedido actual (para verificar triggers y calcular contexto)
     * @param producto el producto a agregar
     * @param cantidad la cantidad solicitada
     * @param observacion la observación del cliente (puede ser null)
     * @param promocionesActivas lista de promociones activas del local
     * @param fechaHora fecha/hora actual para evaluar criterios temporales
     * @return ItemPedido con descuento aplicado (si corresponde) o sin descuento
     */
    public ItemPedido aplicarReglas(
            Pedido pedido,
            Producto producto,
            int cantidad,
            String observacion,
            List<Promocion> promocionesActivas,
            LocalDateTime fechaHora
    ) {
        Objects.requireNonNull(pedido, "El pedido no puede ser null");
        Objects.requireNonNull(producto, "El producto no puede ser null");
        Objects.requireNonNull(promocionesActivas, "La lista de promociones no puede ser null");
        Objects.requireNonNull(fechaHora, "La fecha/hora no puede ser null");

        // Construir contexto de validación desde el pedido actual
        ContextoValidacion contexto = construirContexto(pedido, fechaHora);

        // Buscar la promoción ganadora (si hay alguna que aplique)
        Optional<PromocionEvaluada> promoGanadora = promocionesActivas.stream()
                .filter(promo -> evaluarPromocion(promo, producto, pedido, contexto))
                .map(promo -> new PromocionEvaluada(promo, calcularDescuento(promo, producto, cantidad)))
                .filter(evaluada -> evaluada.montoDescuento().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparingInt(evaluada -> evaluada.promocion().getPrioridad()));

        // Generar el ItemPedido con o sin descuento
        return promoGanadora
                .map(evaluada -> crearItemConDescuento(pedido.getId(), producto, cantidad, observacion, evaluada))
                .orElseGet(() -> crearItemSinDescuento(pedido.getId(), producto, cantidad, observacion));
    }
    
    /**
     * HU-05.1 + HU-22: Evalúa promociones y genera ItemPedido CON extras.
     * 
     * CRÍTICO: Las promociones SOLO aplican sobre el precio base del producto.
     * Los extras NUNCA participan en el cálculo de descuentos (aislamiento).
     * 
     * Algoritmo idéntico a aplicarReglas(), pero:
     * - Construye el ItemPedido incluyendo la lista de extras
     * - Las promociones calculan descuento SOLO sobre precioUnitario × cantidad
     * - Los extras se suman al final SIN descuento
     * 
     * @param pedido el pedido actual
     * @param producto el producto a agregar (posiblemente normalizado)
     * @param cantidad la cantidad solicitada
     * @param observacion la observación del cliente
     * @param extras lista de extras con snapshot de precio
     * @param promocionesActivas lista de promociones activas del local
     * @param fechaHora fecha/hora actual para evaluar criterios temporales
     * @return ItemPedido con descuento aplicado (si corresponde) y extras
     */
    public ItemPedido aplicarReglasConExtras(
            Pedido pedido,
            Producto producto,
            int cantidad,
            String observacion,
            List<ExtraPedido> extras,
            List<Promocion> promocionesActivas,
            LocalDateTime fechaHora
    ) {
        Objects.requireNonNull(pedido, "El pedido no puede ser null");
        Objects.requireNonNull(producto, "El producto no puede ser null");
        Objects.requireNonNull(extras, "La lista de extras no puede ser null");
        Objects.requireNonNull(promocionesActivas, "La lista de promociones no puede ser null");
        Objects.requireNonNull(fechaHora, "La fecha/hora no puede ser null");

        // ─── REGLA DE NEGOCIO: Las promociones solo aplican al producto base ───
        // Un producto con extras es una personalización. Las promos son para el producto
        // tal cual está en el catálogo, sin agregados. Dos hamburguesas con cheddar
        // NO entran en un 2×1 de hamburguesas; solo las hamburguesas base.
        boolean tieneExtras = extras != null && !extras.isEmpty();

        if (tieneExtras) {
            // Producto personalizado → sin evaluación de promos, directo sin descuento
            return crearItemConExtras(pedido.getId(), producto, cantidad, observacion, extras);
        }

        // Construir contexto de validación desde el pedido actual
        ContextoValidacion contexto = construirContexto(pedido, fechaHora);

        // Buscar la promoción ganadora (si hay alguna que aplique)
        // CRÍTICO: calcularDescuento() usa SOLO el precio base del producto
        Optional<PromocionEvaluada> promoGanadora = promocionesActivas.stream()
                .filter(promo -> evaluarPromocion(promo, producto, pedido, contexto))
                .map(promo -> new PromocionEvaluada(promo, calcularDescuento(promo, producto, cantidad)))
                .filter(evaluada -> evaluada.montoDescuento().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparingInt(evaluada -> evaluada.promocion().getPrioridad()));

        // Producto base (sin extras) → evaluar promo normalmente
        return promoGanadora
                .map(evaluada -> crearItemConDescuentoYExtras(pedido.getId(), producto, cantidad, observacion, extras, evaluada))
                .orElseGet(() -> crearItemConExtras(pedido.getId(), producto, cantidad, observacion, extras));
    }

    /**
     * Construye el contexto de validación desde el pedido y la fecha actual.
     */
    private ContextoValidacion construirContexto(Pedido pedido, LocalDateTime fechaHora) {
        List<ProductoId> productosEnPedido = pedido.getItems().stream()
                .map(ItemPedido::getProductoId)
                .toList();

        return ContextoValidacion.builder()
                .fecha(fechaHora.toLocalDate())
                .hora(fechaHora.toLocalTime())
                .productosEnPedido(productosEnPedido)
                .totalPedido(pedido.calcularSubtotalItems())
                .build();
    }

    /**
     * Evalúa si una promoción aplica al producto en el contexto actual.
     * 
     * Verifica:
     * 1. La promoción está ACTIVA
     * 2. Tiene alcance definido con al menos un TARGET
     * 3. El producto es un TARGET de la promoción
     * 4. Los criterios de activación (triggers temporales) se satisfacen
     * 5. Para COMBO_CONDICIONAL: los productos TRIGGER están en el pedido
     */
    private boolean evaluarPromocion(
            Promocion promocion,
            Producto producto,
            Pedido pedido,
            ContextoValidacion contexto
    ) {
        // 1. Verificar estado ACTIVA
        if (promocion.getEstado() != EstadoPromocion.ACTIVA) {
            return false;
        }

        AlcancePromocion alcance = promocion.getAlcance();
        
        // 2. Verificar que tenga alcance definido con targets
        if (alcance == null || !alcance.tieneTargets()) {
            return false;
        }

        // 3. Verificar que el producto sea TARGET
        UUID productoUUID = producto.getId().getValue();
        if (!alcance.esProductoTarget(productoUUID)) {
            return false;
        }

        // 4. Verificar criterios de activación (triggers temporales, monto mínimo, etc.)
        if (!promocion.puedeActivarse(contexto)) {
            return false;
        }

        // 5. Para COMBO_CONDICIONAL: verificar que existan los productos TRIGGER en el pedido
        if (promocion.getEstrategia() instanceof ComboCondicional combo) {
            return verificarTriggersComboPresentesEnPedido(promocion, pedido, combo);
        }

        return true;
    }

    /**
     * Para promociones COMBO_CONDICIONAL, verifica que los productos TRIGGER
     * estén presentes en el pedido con la cantidad mínima requerida.
     */
    private boolean verificarTriggersComboPresentesEnPedido(
            Promocion promocion,
            Pedido pedido,
            ComboCondicional combo
    ) {
        AlcancePromocion alcance = promocion.getAlcance();
        
        // Si no hay triggers definidos, la promo aplica directamente
        if (!alcance.tieneTriggers()) {
            return true;
        }

        Set<UUID> triggerIds = alcance.getProductosTriggerIds();
        
        // Verificar que AL MENOS UN trigger esté presente con cantidad mínima
        for (UUID triggerId : triggerIds) {
            int cantidadTriggerEnPedido = pedido.getItems().stream()
                    .filter(item -> item.getProductoId().getValue().equals(triggerId))
                    .mapToInt(ItemPedido::getCantidad)
                    .sum();

            if (cantidadTriggerEnPedido >= combo.cantidadMinimaTrigger()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calcula el monto de descuento según la estrategia de la promoción.
     * 
     * Estrategias soportadas:
     * - DESCUENTO_DIRECTO: % o monto fijo sobre el subtotal
     * - CANTIDAD_FIJA: NxM (ej: 2x1) calculando unidades gratis
     * - COMBO_CONDICIONAL: % sobre el producto target
     * - PRECIO_FIJO_CANTIDAD: Pack con precio especial (ej: 2×$22.000)
     */
    private BigDecimal calcularDescuento(Promocion promocion, Producto producto, int cantidad) {
        BigDecimal precioBase = producto.getPrecio();
        BigDecimal subtotal = precioBase.multiply(BigDecimal.valueOf(cantidad));
        EstrategiaPromocion estrategia = promocion.getEstrategia();

        return switch (estrategia) {
            case DescuentoDirecto descuento -> calcularDescuentoDirecto(descuento, subtotal, precioBase, cantidad);
            case CantidadFija cantidadFija -> calcularDescuentoCantidadFija(cantidadFija, precioBase, cantidad);
            case ComboCondicional combo -> calcularDescuentoCombo(combo, subtotal);
            case PrecioFijoPorCantidad precioFijo -> calcularDescuentoPrecioFijoCantidad(precioFijo, precioBase, cantidad);
        };
    }

    /**
     * Calcula descuento para estrategia DESCUENTO_DIRECTO.
     * 
     * - PORCENTAJE: (subtotal * porcentaje) / 100
     * - MONTO_FIJO: monto fijo por unidad, máximo el subtotal
     */
    private BigDecimal calcularDescuentoDirecto(
            DescuentoDirecto descuento,
            BigDecimal subtotal,
            BigDecimal precioBase,
            int cantidad
    ) {
        if (descuento.modo() == ModoDescuento.PORCENTAJE) {
            return subtotal.multiply(descuento.valor())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            // MONTO_FIJO: aplicamos el descuento fijo por cada unidad, máximo el subtotal
            BigDecimal descuentoTotal = descuento.valor().multiply(BigDecimal.valueOf(cantidad));
            return descuentoTotal.min(subtotal);
        }
    }

    /**
     * Calcula descuento para estrategia CANTIDAD_FIJA (NxM).
     * 
     * Ejemplo 2x1: si lleva 2, paga 1, entonces 1 unidad es gratis.
     * Si lleva 4, son 2 ciclos de 2x1, entonces 2 unidades gratis.
     */
    private BigDecimal calcularDescuentoCantidadFija(
            CantidadFija estrategia,
            BigDecimal precioBase,
            int cantidad
    ) {
        int llevas = estrategia.cantidadLlevas();
        int pagas = estrategia.cantidadPagas();
        
        // Cuántos ciclos completos de la promo aplican
        int ciclosCompletos = cantidad / llevas;
        
        // Unidades gratis = ciclos * (llevas - pagas)
        int unidadesGratis = ciclosCompletos * (llevas - pagas);
        
        return precioBase.multiply(BigDecimal.valueOf(unidadesGratis));
    }

    /**
     * Calcula descuento para estrategia COMBO_CONDICIONAL.
     * 
     * Aplica el porcentaje de beneficio sobre el subtotal del target.
     */
    private BigDecimal calcularDescuentoCombo(ComboCondicional combo, BigDecimal subtotal) {
        return subtotal.multiply(combo.porcentajeBeneficio())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula descuento para estrategia PRECIO_FIJO_CANTIDAD.
     * 
     * Fórmula:
     * - ciclos = cantidad / cantidadActivacion
     * - costoSinPromo = ciclos × cantidadActivacion × precioUnitario
     * - costoConPromo = ciclos × precioPaquete
     * - descuento = costoSinPromo - costoConPromo
     * 
     * Las unidades restantes (cantidad % cantidadActivacion) se cobran a precio completo.
     * 
     * Ejemplo: "2 hamburguesas por $22.000", precio base $13.000
     * - cantidad=1: ciclos=0 → descuento=$0
     * - cantidad=2: ciclos=1 → descuento = ($13.000×2) - $22.000 = $4.000
     * - cantidad=3: ciclos=1 → descuento = ($13.000×2) - $22.000 = $4.000
     * - cantidad=4: ciclos=2 → descuento = ($13.000×4) - $44.000 = $8.000
     */
    private BigDecimal calcularDescuentoPrecioFijoCantidad(
            PrecioFijoPorCantidad estrategia,
            BigDecimal precioBase,
            int cantidad
    ) {
        int cantidadActivacion = estrategia.cantidadActivacion();
        BigDecimal precioPaquete = estrategia.precioPaquete();
        
        // Calcular cuántos ciclos completos aplican
        int ciclos = cantidad / cantidadActivacion;
        
        // Si no hay ciclos completos, no hay descuento
        if (ciclos == 0) {
            return BigDecimal.ZERO;
        }
        
        // Calcular costos
        BigDecimal costoSinPromo = precioBase
                .multiply(BigDecimal.valueOf(cantidadActivacion))
                .multiply(BigDecimal.valueOf(ciclos));
        
        BigDecimal costoConPromo = precioPaquete
                .multiply(BigDecimal.valueOf(ciclos));
        
        // El descuento es la diferencia
        BigDecimal descuento = costoSinPromo.subtract(costoConPromo);
        
        // Validar que el descuento sea positivo (debería serlo si la promo está bien configurada)
        return descuento.max(BigDecimal.ZERO);
    }

    /**
     * Crea un ItemPedido con el descuento de la promoción aplicado (Patrón Snapshot).
     */
    private ItemPedido crearItemConDescuento(
            PedidoId pedidoId,
            Producto producto,
            int cantidad,
            String observacion,
            PromocionEvaluada evaluada
    ) {
        return ItemPedido.crearConPromocion(
                ItemPedidoId.generate(),
                pedidoId,
                producto,
                cantidad,
                observacion,
                evaluada.montoDescuento(),
                evaluada.promocion().getNombre(),
                evaluada.promocion().getId().getValue()
        );
    }

    /**
     * Crea un ItemPedido sin descuento (precio completo).
     */
    private ItemPedido crearItemSinDescuento(
            PedidoId pedidoId,
            Producto producto,
            int cantidad,
            String observacion
    ) {
        return ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(),
                pedidoId,
                producto,
                cantidad,
                observacion
        );
    }
    
    /**
     * HU-05.1 + HU-22: Crea un ItemPedido CON extras y CON descuento de promoción.
     * 
     * CRÍTICO: El descuento se calcula SOLO sobre el precio base.
     * Los extras se agregan SIN descuento.
     */
    private ItemPedido crearItemConDescuentoYExtras(
            PedidoId pedidoId,
            Producto producto,
            int cantidad,
            String observacion,
            List<ExtraPedido> extras,
            PromocionEvaluada evaluada
    ) {
        return ItemPedido.crearCompleto(
                ItemPedidoId.generate(),
                pedidoId,
                producto,
                cantidad,
                observacion,
                evaluada.montoDescuento(),
                evaluada.promocion().getNombre(),
                evaluada.promocion().getId().getValue(),
                extras
        );
    }
    
    /**
     * HU-05.1 + HU-22: Crea un ItemPedido CON extras SIN descuento.
     */
    private ItemPedido crearItemConExtras(
            PedidoId pedidoId,
            Producto producto,
            int cantidad,
            String observacion,
            List<ExtraPedido> extras
    ) {
        return ItemPedido.crearConExtras(
                ItemPedidoId.generate(),
                pedidoId,
                producto,
                cantidad,
                observacion,
                extras
        );
    }

    /**
     * Record auxiliar para transportar promoción evaluada con su descuento calculado.
     */
    private record PromocionEvaluada(Promocion promocion, BigDecimal montoDescuento) {}

    // ============================================
    // HU-20/HU-21: Recálculo completo de promociones
    // ============================================

    /**
     * Re-evalúa y aplica promociones a TODOS los ítems existentes en el pedido.
     * 
     * HU-20/HU-21: Tras eliminar o modificar un ítem, las promociones de todo el pedido
     * deben recalcularse porque:
     * - Un combo puede romperse al eliminar su trigger
     * - Los ciclos NxM cambian al modificar cantidades
     * - La prioridad de promociones puede resolverse diferente con el nuevo estado
     * 
     * PRECONDICIÓN: Todos los ítems deben tener sus promociones limpiadas previamente
     * mediante Pedido.limpiarPromocionesItems().
     * 
     * Algoritmo:
     * 1. Construir contexto de validación desde el pedido actual
     * 2. Para cada ítem del pedido:
     *    a. Evaluar qué promoción aplica (si alguna)
     *    b. Calcular el descuento usando el precioUnitario snapshot del ítem
     *    c. Aplicar la promoción ganadora directamente sobre el ítem
     * 
     * @param pedido el pedido con los ítems ya modificados y promociones limpiadas
     * @param promocionesActivas lista de promociones activas del local
     * @param fechaHora fecha/hora actual para evaluar criterios temporales
     */
    public void aplicarPromociones(
            Pedido pedido,
            List<Promocion> promocionesActivas,
            LocalDateTime fechaHora
    ) {
        Objects.requireNonNull(pedido, "El pedido no puede ser null");
        Objects.requireNonNull(promocionesActivas, "La lista de promociones no puede ser null");
        Objects.requireNonNull(fechaHora, "La fecha/hora no puede ser null");

        // Construir contexto de validación
        ContextoValidacion contexto = construirContexto(pedido, fechaHora);

        // ─── AGREGACIÓN CROSS-LÍNEA ───
        // Las promociones basadas en cantidad (NxM, PrecioFijo) deben considerar la cantidad
        // TOTAL del producto en el pedido, no la cantidad de cada línea por separado.
        //
        // Ejemplo: 3x hamburguesa + 1x hamburguesa "sin cebolla" = 4 unidades para la promo.
        // Las observaciones NO afectan elegibilidad — solo existen para la cocina/ticket.
        //
        // Los ítems con extras quedan excluidos (regla: producto personalizado ≠ producto base).
        Map<UUID, List<ItemPedido>> gruposPorProducto = pedido.getItems().stream()
                .filter(item -> !item.tieneExtras())
                .collect(Collectors.groupingBy(item -> item.getProductoId().getValue()));

        for (List<ItemPedido> grupo : gruposPorProducto.values()) {
            evaluarYAplicarPromocionAGrupo(grupo, pedido, promocionesActivas, contexto);
        }
    }

    /**
     * Evalúa y aplica la mejor promoción a un GRUPO de ítems del mismo producto.
     * 
     * REGLA DE NEGOCIO CLAVE: Las observaciones no afectan la elegibilidad para promociones.
     * "Hamburguesa" y "Hamburguesa sin cebolla" son el mismo producto base para la promo.
     * Las diferentes líneas existen solo para la visualización en comanda/ticket.
     * 
     * La cantidad total del grupo se usa para calcular el descuento,
     * que luego se distribuye proporcionalmente entre las líneas.
     */
    private void evaluarYAplicarPromocionAGrupo(
            List<ItemPedido> items,
            Pedido pedido,
            List<Promocion> promocionesActivas,
            ContextoValidacion contexto
    ) {
        if (items.isEmpty()) return;

        // Todos los ítems del grupo comparten productoId y precioUnitario (snapshot)
        ItemPedido referencia = items.get(0);
        UUID productoUUID = referencia.getProductoId().getValue();
        BigDecimal precioBase = referencia.getPrecioUnitario();

        // Cantidad TOTAL del producto en el pedido (sumando todas las líneas)
        int cantidadTotal = items.stream().mapToInt(ItemPedido::getCantidad).sum();

        // Buscar la mejor promoción usando la cantidad TOTAL
        Optional<PromocionEvaluada> promoGanadora = promocionesActivas.stream()
                .filter(promo -> evaluarPromocionParaItem(promo, productoUUID, pedido, contexto))
                .map(promo -> new PromocionEvaluada(promo, calcularDescuentoParaItem(promo, precioBase, cantidadTotal)))
                .filter(evaluada -> evaluada.montoDescuento().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparingInt(evaluada -> evaluada.promocion().getPrioridad()));

        if (promoGanadora.isEmpty()) return;

        PromocionEvaluada evaluada = promoGanadora.get();
        BigDecimal descuentoTotal = evaluada.montoDescuento();

        if (items.size() == 1) {
            // Caso simple: una sola línea → descuento completo
            items.get(0).aplicarPromocion(
                    descuentoTotal,
                    evaluada.promocion().getNombre(),
                    evaluada.promocion().getId().getValue()
            );
        } else {
            // Múltiples líneas → distribuir solo entre unidades que forman ciclos completos
            int cantidadEnCiclos = calcularCantidadEnCiclos(
                    evaluada.promocion().getEstrategia(), cantidadTotal
            );
            distribuirDescuentoProporcional(items, cantidadEnCiclos, descuentoTotal, evaluada);
        }
    }

    /**
     * Calcula cuántas unidades participan en ciclos completos de la promoción.
     * 
     * Solo las unidades dentro de ciclos completos reciben descuento.
     * Las unidades sobrantes se cobran a precio completo y NO muestran promo.
     * 
     * Ejemplo: promo "2 por $24.000" con 3 unidades → 2 en ciclo, 1 sobrante.
     */
    private int calcularCantidadEnCiclos(EstrategiaPromocion estrategia, int cantidadTotal) {
        return switch (estrategia) {
            case CantidadFija cf ->
                    (cantidadTotal / cf.cantidadLlevas()) * cf.cantidadLlevas();
            case PrecioFijoPorCantidad pf ->
                    (cantidadTotal / pf.cantidadActivacion()) * pf.cantidadActivacion();
            // Descuento directo y combo aplican a TODAS las unidades
            case DescuentoDirecto ignored -> cantidadTotal;
            case ComboCondicional ignored -> cantidadTotal;
        };
    }

    /**
     * Distribuye el descuento total entre múltiples líneas del mismo producto,
     * asignando solo a unidades que participan en ciclos completos de promo.
     * 
     * Algoritmo:
     * 1. Ordena líneas por cantidad descendente (las más grandes llenan ciclos primero)
     * 2. Asigna greedy: cada línea aporta min(suCantidad, unidadesRestantesDelCiclo)
     * 3. Líneas con 0 unidades participantes NO reciben promo (quedan limpias)
     * 4. Distribuye el descuento proporcionalmente entre las unidades participantes
     * 
     * Ejemplo: 3 cheeseburgers (línea 2 + línea 1), promo pack-de-2:
     * - cantidadEnCiclos=2 → línea1(2) llena el ciclo → $3.000
     * - línea2(1) sobrante → $0, sin label de promo
     */
    private void distribuirDescuentoProporcional(
            List<ItemPedido> items,
            int cantidadEnCiclos,
            BigDecimal descuentoTotal,
            PromocionEvaluada evaluada
    ) {
        // Ordenar por cantidad descendente: las líneas más grandes llenan ciclos primero
        List<ItemPedido> ordenados = new ArrayList<>(items);
        ordenados.sort(Comparator.comparingInt(ItemPedido::getCantidad).reversed());

        // Fase 1: Determinar cuántas unidades de cada línea participan en ciclos
        record Participacion(ItemPedido item, int unidades) {}
        List<Participacion> participantes = new ArrayList<>();
        int unidadesRestantes = cantidadEnCiclos;

        for (ItemPedido item : ordenados) {
            int contribucion = Math.min(item.getCantidad(), unidadesRestantes);
            unidadesRestantes -= contribucion;
            if (contribucion > 0) {
                participantes.add(new Participacion(item, contribucion));
            }
            // Líneas con contribucion=0 no reciben promo (quedan con limpiarPromocion)
        }

        // Fase 2: Distribuir descuento proporcionalmente entre participantes
        BigDecimal descuentoAsignado = BigDecimal.ZERO;

        for (int i = 0; i < participantes.size(); i++) {
            Participacion p = participantes.get(i);
            BigDecimal descuentoItem;

            if (i == participantes.size() - 1) {
                // Última línea participante: asignar residuo (evita pérdida por redondeo)
                descuentoItem = descuentoTotal.subtract(descuentoAsignado);
            } else {
                descuentoItem = descuentoTotal
                        .multiply(BigDecimal.valueOf(p.unidades()))
                        .divide(BigDecimal.valueOf(cantidadEnCiclos), 2, RoundingMode.HALF_UP);
                descuentoAsignado = descuentoAsignado.add(descuentoItem);
            }

            p.item().aplicarPromocion(
                    descuentoItem,
                    evaluada.promocion().getNombre(),
                    evaluada.promocion().getId().getValue()
            );
        }
    }

    /**
     * Evalúa si una promoción aplica a un productoId en el contexto actual.
     * Variante que no requiere la entidad Producto completa.
     */
    private boolean evaluarPromocionParaItem(
            Promocion promocion,
            UUID productoUUID,
            Pedido pedido,
            ContextoValidacion contexto
    ) {
        if (promocion.getEstado() != EstadoPromocion.ACTIVA) {
            return false;
        }

        AlcancePromocion alcance = promocion.getAlcance();
        if (alcance == null || !alcance.tieneTargets()) {
            return false;
        }

        if (!alcance.esProductoTarget(productoUUID)) {
            return false;
        }

        if (!promocion.puedeActivarse(contexto)) {
            return false;
        }

        if (promocion.getEstrategia() instanceof ComboCondicional combo) {
            return verificarTriggersComboPresentesEnPedido(promocion, pedido, combo);
        }

        return true;
    }

    /**
     * Calcula el descuento para un ítem usando su precio snapshot.
     * Variante que no requiere la entidad Producto completa.
     */
    private BigDecimal calcularDescuentoParaItem(Promocion promocion, BigDecimal precioBase, int cantidad) {
        BigDecimal subtotal = precioBase.multiply(BigDecimal.valueOf(cantidad));
        EstrategiaPromocion estrategia = promocion.getEstrategia();

        return switch (estrategia) {
            case DescuentoDirecto descuento -> calcularDescuentoDirecto(descuento, subtotal, precioBase, cantidad);
            case CantidadFija cantidadFija -> calcularDescuentoCantidadFija(cantidadFija, precioBase, cantidad);
            case ComboCondicional combo -> calcularDescuentoCombo(combo, subtotal);
            case PrecioFijoPorCantidad precioFijo -> calcularDescuentoPrecioFijoCantidad(precioFijo, precioBase, cantidad);
        };
    }
}
