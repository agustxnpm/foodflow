package com.agustinpalma.comandas.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Command para crear una promoción con triggers configurables.
 * 
 * Implementa el patrón Specification/Composite Trigger.
 * Acepta una lista polimórfica de criterios de activación (triggers) que
 * deben cumplirse simultáneamente (lógica AND) para que la promoción aplique.
 * 
 * Tipos de triggers soportados:
 * - TEMPORAL: Valida fechas, días de semana, horarios
 * - CONTENIDO: Valida presencia de productos en el pedido
 * - MONTO_MINIMO: Valida que el total del pedido supere un umbral
 * 
 * La estrategia define el beneficio que se otorga una vez activada la promoción.
 */
public record CrearPromocionCommand(

        @NotBlank(message = "El nombre de la promoción es obligatorio")
        String nombre,

        String descripcion,

        @NotNull(message = "La prioridad es obligatoria")
        @Min(value = 0, message = "La prioridad no puede ser negativa")
        Integer prioridad,

        @NotNull(message = "El tipo de estrategia es obligatorio")
        String tipoEstrategia,

        @Valid
        DescuentoDirectoParams descuentoDirecto,

        @Valid
        CantidadFijaParams cantidadFija,

        @Valid
        ComboCondicionalParams comboCondicional,

        @NotNull(message = "Los triggers son obligatorios")
        @Size(min = 1, message = "Debe especificar al menos un trigger")
        @Valid
        List<TriggerParams> triggers
) {

    /**
     * Parámetros para estrategia DESCUENTO_DIRECTO.
     */
    public record DescuentoDirectoParams(
            @NotBlank(message = "El modo de descuento es obligatorio (PORCENTAJE o MONTO_FIJO)")
            String modo,

            @NotNull(message = "El valor del descuento es obligatorio")
            BigDecimal valor
    ) {
    }

    /**
     * Parámetros para estrategia CANTIDAD_FIJA (NxM).
     */
    public record CantidadFijaParams(
            @NotNull(message = "La cantidad que se lleva es obligatoria")
            @Min(value = 1, message = "La cantidad que se lleva debe ser al menos 1")
            Integer cantidadLlevas,

            @NotNull(message = "La cantidad que se paga es obligatoria")
            @Min(value = 1, message = "La cantidad que se paga debe ser al menos 1")
            Integer cantidadPagas
    ) {
    }

    /**
     * Parámetros para estrategia COMBO_CONDICIONAL.
     */
    public record ComboCondicionalParams(
            @NotNull(message = "La cantidad mínima del trigger es obligatoria")
            @Min(value = 1, message = "La cantidad mínima del trigger debe ser al menos 1")
            Integer cantidadMinimaTrigger,

            @NotNull(message = "El porcentaje de beneficio es obligatorio")
            BigDecimal porcentajeBeneficio
    ) {
    }

    /**
     * Parámetros polimórficos para triggers (criterios de activación).
     * 
     * El tipo determina qué campos son obligatorios:
     * - TEMPORAL: fechaDesde, fechaHasta, diasSemana, horaDesde, horaHasta
     * - CONTENIDO: productosRequeridos
     * - MONTO_MINIMO: montoMinimo
     */
    public record TriggerParams(
            @NotBlank(message = "El tipo de trigger es obligatorio")
            String tipo,

            // Para TEMPORAL
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Set<DayOfWeek> diasSemana,
            LocalTime horaDesde,
            LocalTime horaHasta,

            // Para CONTENIDO
            List<String> productosRequeridos,

            // Para MONTO_MINIMO
            BigDecimal montoMinimo
    ) {
    }
}
