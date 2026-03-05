package com.agustinpalma.comandas.application.dto;

import java.math.BigDecimal;

/**
 * DTO de lectura para el reporte de ventas por producto.
 *
 * Representa una fila agregada: el total de unidades vendidas y el
 * total recaudado (bruto) de un producto en un día operativo,
 * considerando únicamente pedidos con estado CERRADO.
 *
 * No pertenece al dominio — es una proyección plana para analytics.
 *
 * @param productoNombre nombre del producto (snapshot del item)
 * @param cantidadTotal  suma de cantidades vendidas en el período
 * @param totalRecaudado suma de (precio_unitario × cantidad) en el período
 */
public record ProductoVendidoReporte(
    String productoNombre,
    Long cantidadTotal,
    BigDecimal totalRecaudado
) {
    public ProductoVendidoReporte {
        if (productoNombre == null || productoNombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto no puede ser vacío");
        }
        if (cantidadTotal == null || cantidadTotal < 0) {
            throw new IllegalArgumentException("La cantidad total no puede ser negativa");
        }
        if (totalRecaudado == null || totalRecaudado.signum() < 0) {
            throw new IllegalArgumentException("El total recaudado no puede ser negativo");
        }
    }
}
