package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO que representa un ítem individual dentro del detalle del pedido.
 * Contiene información snapshot del producto y cálculos financieros.
 * 
 * HU-10: Incluye información de promoción para UX:
 * - precioUnitarioBase: precio de lista (para tachar)
 * - descuentoTotal: el ahorro del cliente
 * - precioFinal: lo que paga el cliente
 * - nombrePromocion: etiqueta a mostrar
 * 
 * HU-05.1 + HU-22: Incluye lista de extras como sub-elementos.
 * Los extras se muestran debajo del producto principal, NO como líneas independientes.
 * 
 * Se usa como parte de la respuesta en la consulta de detalle de pedido.
 * 
 * puedeAgregarDiscoExtra: true si el ítem está en la variante estructural máxima
 * de su grupo y puede recibir un modificador estructural como extra.
 * El frontend NO calcula nada: este flag es la fuente de verdad.
 */
public record ItemDetalleDTO(
    String id,
    String nombreProducto,
    int cantidad,
    BigDecimal precioUnitarioBase,   // Precio de lista (para tachar en UI)
    BigDecimal subtotal,              // precioBase * cantidad (incluye extras)
    BigDecimal descuentoTotal,        // Monto de descuento aplicado
    BigDecimal precioFinal,           // Lo que paga el cliente
    String observacion,
    String nombrePromocion,           // Nombre de la promo aplicada (puede ser null)
    boolean tienePromocion,           // Flag para condicionales en UI
    List<ExtraDetalleDTO> extras,     // Extras aplicados como sub-elementos

    // Regla única de extras estructurales
    boolean puedeAgregarDiscoExtra    // true si está en la variante máxima del grupo
) {
    /**
     * Valida que los campos obligatorios no sean nulos.
     * Se ejecuta automáticamente por el compilador en el constructor canónico.
     */
    public ItemDetalleDTO {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del ítem no puede ser nulo o vacío");
        }
        if (nombreProducto == null || nombreProducto.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto no puede ser nulo o vacío");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        if (precioUnitarioBase == null || precioUnitarioBase.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser nulo ni negativo");
        }
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El subtotal no puede ser nulo ni negativo");
        }
        if (descuentoTotal == null || descuentoTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El descuento no puede ser nulo ni negativo");
        }
        if (precioFinal == null || precioFinal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio final no puede ser nulo ni negativo");
        }
        if (extras == null) {
            throw new IllegalArgumentException("La lista de extras no puede ser nula");
        }
    }
}
