package com.agustinpalma.comandas.application.dto;

/**
 * DTO para la creación de mesas.
 * Solo requiere el número; el ID y localId se asignan en el caso de uso.
 */
public record CrearMesaRequest(int numero) {}
