package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Command para asociar productos/categorías a una promoción (HU-09).
 * 
 * Define el alcance (scope) de la promoción:
 * - Qué productos/categorías ACTIVAN la promoción (rol TRIGGER)
 * - Qué productos/categorías RECIBEN el beneficio (rol TARGET)
 * 
 * Ejemplo de uso (Caso Torta + Licuado):
 * {
 *   "items": [
 *     { "referenciaId": "uuid-torta-chocolate", "tipo": "PRODUCTO", "rol": "TRIGGER" },
 *     { "referenciaId": "uuid-cat-licuados", "tipo": "CATEGORIA", "rol": "TARGET" },
 *     { "referenciaId": "uuid-agua-mineral", "tipo": "PRODUCTO", "rol": "TARGET" }
 *   ]
 * }
 */
public record AsociarScopeCommand(
        @NotNull(message = "La lista de items es obligatoria")
        @Size(min = 1, message = "Debe especificar al menos un item")
        @Valid
        List<ItemScopeParams> items
) {

    /**
     * Parámetros para un item del alcance.
     */
    public record ItemScopeParams(
            @NotNull(message = "La referenciaId es obligatoria")
            String referenciaId,

            @NotNull(message = "El tipo de alcance es obligatorio")
            TipoAlcance tipo,

            @NotNull(message = "El rol de promoción es obligatorio")
            RolPromocion rol
    ) {
    }
}
