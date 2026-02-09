package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Jerarquía sellada de estrategias de promoción.
 * 
 * Cada tipo de estrategia encapsula sus propios parámetros y validaciones.
 * Esto permite extensibilidad (agregar nuevos tipos) sin modificar la entidad Promocion
 * ni crear IFs gigantes.
 * 
 * Tipos soportados:
 * - DESCUENTO_DIRECTO: Porcentaje o monto fijo sobre un producto
 * - CANTIDAD_FIJA: NxM (ej: 2x1, 3x2, 4x3)
 * - COMBO_CONDICIONAL: Si comprás X (trigger), obtenés Y con Z% de descuento (benefit)
 * - PRECIO_FIJO_CANTIDAD: Precio fijo por cantidad (ej: 2 hamburguesas por $22.000)
 * 
 * Inmutables, validados en construcción, comparados por valor.
 * 
 * Nota: La estrategia define "qué beneficio se otorga" y "bajo qué condiciones".
 * La asociación con productos concretos vendrá en HU-09.
 */
public sealed interface EstrategiaPromocion permits
        EstrategiaPromocion.DescuentoDirecto,
        EstrategiaPromocion.CantidadFija,
        EstrategiaPromocion.ComboCondicional,
        EstrategiaPromocion.PrecioFijoPorCantidad {

    /**
     * Retorna el tipo de estrategia para serialización/persistencia.
     */
    TipoEstrategia getTipo();

    // ============================================
    // DESCUENTO DIRECTO: % o Monto fijo
    // ============================================

    /**
     * Estrategia de descuento simple sobre un producto.
     * 
     * Ejemplos:
     * - "20% de descuento en empanadas"
     * - "$500 de descuento en pizza grande"
     * 
     * Reglas de negocio:
     * - Si modo = PORCENTAJE → valor debe estar entre 0.01 y 100
     * - Si modo = MONTO_FIJO → valor debe ser mayor a 0
     */
    record DescuentoDirecto(
            ModoDescuento modo,
            BigDecimal valor
    ) implements EstrategiaPromocion {

        public DescuentoDirecto {
            Objects.requireNonNull(modo, "El modo de descuento es obligatorio");
            Objects.requireNonNull(valor, "El valor del descuento es obligatorio");

            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El valor del descuento debe ser mayor a cero");
            }

            if (modo == ModoDescuento.PORCENTAJE && valor.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("El porcentaje de descuento no puede superar el 100%");
            }
        }

        @Override
        public TipoEstrategia getTipo() {
            return TipoEstrategia.DESCUENTO_DIRECTO;
        }
    }

    // ============================================
    // CANTIDAD FIJA: NxM (2x1, 3x2, etc.)
    // ============================================

    /**
     * Estrategia de cantidad: llevás N, pagás M.
     * 
     * Ejemplos:
     * - 2x1 → cantidadLlevas=2, cantidadPagas=1
     * - 3x2 → cantidadLlevas=3, cantidadPagas=2
     * 
     * Reglas de negocio:
     * - cantidadLlevas debe ser mayor que cantidadPagas (si no, no hay beneficio)
     * - Ambos deben ser >= 1
     */
    record CantidadFija(
            int cantidadLlevas,
            int cantidadPagas
    ) implements EstrategiaPromocion {

        public CantidadFija {
            if (cantidadLlevas < 1) {
                throw new IllegalArgumentException("La cantidad que se lleva debe ser al menos 1");
            }
            if (cantidadPagas < 1) {
                throw new IllegalArgumentException("La cantidad que se paga debe ser al menos 1");
            }
            if (cantidadLlevas <= cantidadPagas) {
                throw new IllegalArgumentException(
                        "La cantidad que se lleva (" + cantidadLlevas +
                                ") debe ser mayor que la que se paga (" + cantidadPagas + ")"
                );
            }
        }

        @Override
        public TipoEstrategia getTipo() {
            return TipoEstrategia.CANTIDAD_FIJA;
        }
    }

    // ============================================
    // COMBO CONDICIONAL: Si comprás X, obtenés Y con Z% off
    // ============================================

    /**
     * Estrategia condicional: "Si comprás el producto trigger, obtenés el producto
     * target con un descuento".
     * 
     * Ejemplo:
     * - "Si comprás una hamburguesa, la gaseosa tiene 50% off"
     * 
     * Nota importante: Los productos concretos (trigger/target) se asociarán en HU-09.
     * Esta estrategia define los parámetros del beneficio (porcentaje de descuento)
     * y la cantidad mínima del trigger para activar la promo.
     * 
     * Reglas de negocio:
     * - cantidadMinimaTrigger >= 1
     * - porcentajeBeneficio entre 0.01 y 100
     */
    record ComboCondicional(
            int cantidadMinimaTrigger,
            BigDecimal porcentajeBeneficio
    ) implements EstrategiaPromocion {

        public ComboCondicional {
            if (cantidadMinimaTrigger < 1) {
                throw new IllegalArgumentException(
                        "La cantidad mínima del trigger debe ser al menos 1"
                );
            }
            Objects.requireNonNull(porcentajeBeneficio, "El porcentaje de beneficio es obligatorio");
            if (porcentajeBeneficio.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El porcentaje de beneficio debe ser mayor a cero");
            }
            if (porcentajeBeneficio.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("El porcentaje de beneficio no puede superar el 100%");
            }
        }

        @Override
        public TipoEstrategia getTipo() {
            return TipoEstrategia.COMBO_CONDICIONAL;
        }
    }

    // ============================================
    // PRECIO FIJO POR CANTIDAD: Pack con precio especial
    // ============================================

    /**
     * Estrategia de precio fijo por cantidad: N unidades por $X.
     * 
     * Ejemplos:
     * - "2 hamburguesas por $22.000" (precio base: $13.000 c/u)
     * - "3 empanadas por $5.000" (precio base: $2.000 c/u)
     * 
     * Cálculo del descuento:
     * - ciclos = cantidadTotal / cantidadActivacion
     * - costoSinPromo = ciclos × cantidadActivacion × precioUnitario
     * - costoConPromo = ciclos × precioPaquete
     * - descuento = costoSinPromo - costoConPromo
     * 
     * Unidades restantes (cantidadTotal % cantidadActivacion) se cobran a precio completo.
     * 
     * Ejemplo: 2×$22.000, precio base $13.000
     * - 1 unidad: descuento = $0 (sin ciclo completo)
     * - 2 unidades: descuento = $4.000 (1 ciclo: $26.000 - $22.000)
     * - 3 unidades: descuento = $4.000 (1 ciclo + 1 unidad suelta)
     * - 4 unidades: descuento = $8.000 (2 ciclos: $52.000 - $44.000)
     * 
     * Reglas de negocio:
     * - cantidadActivacion >= 2 (mínimo para que sea un "pack")
     * - precioPaquete > 0
     * - precioPaquete debe ser menor que (cantidadActivacion × precio base) para que haya beneficio
     */
    record PrecioFijoPorCantidad(
            int cantidadActivacion,
            BigDecimal precioPaquete
    ) implements EstrategiaPromocion {

        public PrecioFijoPorCantidad {
            if (cantidadActivacion < 2) {
                throw new IllegalArgumentException(
                        "La cantidad de activación debe ser al menos 2 para un pack"
                );
            }
            Objects.requireNonNull(precioPaquete, "El precio del paquete es obligatorio");
            if (precioPaquete.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El precio del paquete debe ser mayor a cero");
            }
        }

        @Override
        public TipoEstrategia getTipo() {
            return TipoEstrategia.PRECIO_FIJO_CANTIDAD;
        }
    }
}
