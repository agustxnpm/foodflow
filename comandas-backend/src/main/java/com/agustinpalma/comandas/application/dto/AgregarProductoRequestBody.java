package com.agustinpalma.comandas.application.dto;

/**
 * DTO de entrada HTTP para agregar productos a un pedido.
 * Contiene tipos primitivos/String que vienen del cliente REST.
 * 
 * HU-05: Agregar productos a un pedido
 */
public record AgregarProductoRequestBody(
    String productoId,
    int cantidad,
    String observaciones
) {}
