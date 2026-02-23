package com.agustinpalma.comandas.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Command para editar una promoción existente.
 * 
 * Permite actualización parcial: solo se modifican los campos presentes.
 * Los triggers y la estrategia se actualizan completos (no se hace merge parcial).
 */
public record EditarPromocionCommand(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String descripcion,

        @Min(value = 0, message = "La prioridad no puede ser negativa")
        Integer prioridad,

        @Size(min = 1, message = "Debe especificar al menos un trigger")
        List<CrearPromocionCommand.TriggerParams> triggers,

        // ── Campos opcionales de estrategia (si se quiere cambiar el beneficio) ──

        /** Tipo de estrategia. Si es null, se mantiene la estrategia actual. */
        String tipoEstrategia,

        @Valid
        CrearPromocionCommand.DescuentoDirectoParams descuentoDirecto,

        @Valid
        CrearPromocionCommand.CantidadFijaParams cantidadFija,

        @Valid
        CrearPromocionCommand.ComboCondicionalParams comboCondicional,

        @Valid
        CrearPromocionCommand.PrecioFijoPorCantidadParams precioFijoPorCantidad
) {
}
