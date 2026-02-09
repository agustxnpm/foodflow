package com.agustinpalma.comandas.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body para el endpoint PATCH de modificar cantidad de un ítem.
 * 
 * HU-21: Modificar cantidad de un producto en pedido abierto.
 * 
 * La cantidad 0 es válida: indica que el usuario desea eliminar el ítem.
 * El dominio se encarga de la interpretación semántica.
 */
public record ModificarCantidadItemRequestBody(
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 0, message = "La cantidad no puede ser negativa")
    Integer cantidad
) {}
