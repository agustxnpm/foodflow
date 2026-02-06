package com.agustinpalma.comandas.domain.service;

import com.agustinpalma.comandas.domain.model.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

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
     */
    private BigDecimal calcularDescuento(Promocion promocion, Producto producto, int cantidad) {
        BigDecimal precioBase = producto.getPrecio();
        BigDecimal subtotal = precioBase.multiply(BigDecimal.valueOf(cantidad));
        EstrategiaPromocion estrategia = promocion.getEstrategia();

        return switch (estrategia) {
            case DescuentoDirecto descuento -> calcularDescuentoDirecto(descuento, subtotal, precioBase, cantidad);
            case CantidadFija cantidadFija -> calcularDescuentoCantidadFija(cantidadFija, precioBase, cantidad);
            case ComboCondicional combo -> calcularDescuentoCombo(combo, subtotal);
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
     * Record auxiliar para transportar promoción evaluada con su descuento calculado.
     */
    private record PromocionEvaluada(Promocion promocion, BigDecimal montoDescuento) {}
}
