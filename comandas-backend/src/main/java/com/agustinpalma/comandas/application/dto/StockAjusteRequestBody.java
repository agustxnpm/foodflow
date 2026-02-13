package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimientoStock;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for the stock adjustment REST endpoint.
 * HU-22: Stock management.
 */
public record StockAjusteRequestBody(
    @NotNull(message = "La cantidad es obligatoria")
    Integer cantidad,

    @NotNull(message = "El tipo de movimiento es obligatorio")
    TipoMovimientoStock tipo,

    @NotBlank(message = "El motivo es obligatorio")
    String motivo
) {}
