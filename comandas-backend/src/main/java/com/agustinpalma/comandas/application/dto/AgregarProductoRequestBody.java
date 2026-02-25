package com.agustinpalma.comandas.application.dto;

import java.util.List;

/**
 * DTO de entrada HTTP para agregar productos a un pedido.
 * Contiene tipos primitivos/String que vienen del cliente REST.
 * 
 * HU-05: Agregar productos a un pedido
 * HU-05.1: Soporte para extras controlados (extrasIds opcionales)
 */
public record AgregarProductoRequestBody(
    String productoId,
    int cantidad,
    String observaciones,
    /** IDs de productos extra (esExtra=true). Si qty > 1 del mismo extra, repetir el ID. Puede ser null. */
    List<String> extrasIds,
    /** ID de la variante seleccionada explícitamente. Si es null, se usa auto-normalización. */
    String varianteId
) {}
