package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Jerarquía sellada de criterios de activación para promociones.
 * 
 * Implementa el patrón Specification/Composite Trigger.
 * Cada criterio evalúa una condición específica sobre un ContextoValidacion.
 * 
 * Tipos soportados:
 * - TEMPORAL: Valida fechas, días de semana, horarios
 * - CONTENIDO: Valida presencia de productos específicos en el pedido
 * - MONTO_MINIMO: Valida que el total del pedido supere un umbral
 * 
 * Inmutables (records), validados en construcción.
 * El método `esSatisfechoPor()` implementa la lógica de evaluación.
 */
public sealed interface CriterioActivacion permits
        CriterioActivacion.CriterioTemporal,
        CriterioActivacion.CriterioContenido,
        CriterioActivacion.CriterioMontoMinimo {

    /**
     * Evalúa si este criterio se cumple dado el contexto de validación.
     * 
     * @param contexto información sobre el pedido, fecha, hora, productos, etc.
     * @return true si el criterio se satisface, false en caso contrario
     */
    boolean esSatisfechoPor(ContextoValidacion contexto);

    /**
     * Retorna el tipo de criterio para serialización/persistencia.
     */
    TipoCriterio getTipo();

    // ============================================
    // CRITERIO TEMPORAL: Fechas, días, horarios
    // ============================================

    /**
     * Criterio de activación basado en condiciones temporales.
     * 
     * Valida:
     * - Rango de fechas (desde/hasta)
     * - Días de la semana permitidos
     * - Rango horario opcional
     * 
     * Si no se especifican días, aplica todos los días.
     * Si no se especifica horario, aplica todo el día.
     * 
     * Reglas de negocio:
     * - fechaDesde ≤ fechaHasta
     * - horaDesde < horaHasta (si ambas presentes)
     */
    record CriterioTemporal(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Set<DayOfWeek> diasSemana,
            LocalTime horaDesde,
            LocalTime horaHasta
    ) implements CriterioActivacion {

        public CriterioTemporal {
            Objects.requireNonNull(fechaDesde, "La fecha de inicio es obligatoria");
            Objects.requireNonNull(fechaHasta, "La fecha de fin es obligatoria");

            if (fechaDesde.isAfter(fechaHasta)) {
                throw new IllegalArgumentException(
                        "La fecha de inicio (" + fechaDesde + ") no puede ser posterior a la fecha de fin (" + fechaHasta + ")"
                );
            }

            if (diasSemana == null || diasSemana.isEmpty()) {
                diasSemana = EnumSet.allOf(DayOfWeek.class);
            } else {
                diasSemana = EnumSet.copyOf(diasSemana);
            }

            if (horaDesde != null && horaHasta != null && horaDesde.isAfter(horaHasta)) {
                throw new IllegalArgumentException(
                        "La hora de inicio (" + horaDesde + ") no puede ser posterior a la hora de fin (" + horaHasta + ")"
                );
            }
        }

        /**
         * Factory method para criterio temporal simple: solo fechas.
         */
        public static CriterioTemporal soloFechas(LocalDate fechaDesde, LocalDate fechaHasta) {
            return new CriterioTemporal(fechaDesde, fechaHasta, null, null, null);
        }

        @Override
        public boolean esSatisfechoPor(ContextoValidacion contexto) {
            Objects.requireNonNull(contexto, "El contexto no puede ser null");

            LocalDate fecha = contexto.getFecha();
            
            // Validar rango de fechas
            if (fecha.isBefore(fechaDesde) || fecha.isAfter(fechaHasta)) {
                return false;
            }

            // Validar día de la semana
            if (!diasSemana.contains(contexto.getDiaSemana())) {
                return false;
            }

            // Validar horario (si está especificado)
            if (horaDesde != null && horaHasta != null) {
                LocalTime hora = contexto.getHora();
                if (hora == null) {
                    return false; // Si el criterio requiere horario pero el contexto no lo tiene, falla
                }
                if (hora.isBefore(horaDesde) || hora.isAfter(horaHasta)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public TipoCriterio getTipo() {
            return TipoCriterio.TEMPORAL;
        }

        @Override
        public Set<DayOfWeek> diasSemana() {
            return Collections.unmodifiableSet(diasSemana);
        }
    }

    // ============================================
    // CRITERIO CONTENIDO: Productos en el pedido
    // ============================================

    /**
     * Criterio de activación basado en el contenido del pedido.
     * 
     * Valida que uno o más productos específicos estén presentes en el pedido.
     * Útil para promociones tipo "Si comprás X, obtenés beneficio en Y".
     * 
     * En el MVP, valida presencia simple. En HU-09 se extenderá para validar
     * cantidades mínimas y roles (TRIGGER vs TARGET).
     */
    record CriterioContenido(
            Set<ProductoId> productosRequeridos
    ) implements CriterioActivacion {

        public CriterioContenido {
            Objects.requireNonNull(productosRequeridos, "Los productos requeridos son obligatorios");
            if (productosRequeridos.isEmpty()) {
                throw new IllegalArgumentException("Debe especificar al menos un producto requerido");
            }
            productosRequeridos = Set.copyOf(productosRequeridos);
        }

        @Override
        public boolean esSatisfechoPor(ContextoValidacion contexto) {
            Objects.requireNonNull(contexto, "El contexto no puede ser null");

            // Verifica que TODOS los productos requeridos estén en el pedido
            return productosRequeridos.stream()
                    .allMatch(contexto::contieneProducto);
        }

        @Override
        public TipoCriterio getTipo() {
            return TipoCriterio.CONTENIDO;
        }

        @Override
        public Set<ProductoId> productosRequeridos() {
            return Collections.unmodifiableSet(productosRequeridos);
        }
    }

    // ============================================
    // CRITERIO MONTO MINIMO: Umbral de compra
    // ============================================

    /**
     * Criterio de activación basado en el total del pedido.
     * 
     * Valida que el subtotal del pedido (antes de aplicar descuentos) supere
     * un monto mínimo.
     * 
     * Ejemplo: "Esta promoción solo aplica si el total es mayor a $5000"
     */
    record CriterioMontoMinimo(
            BigDecimal montoMinimo
    ) implements CriterioActivacion {

        public CriterioMontoMinimo {
            Objects.requireNonNull(montoMinimo, "El monto mínimo es obligatorio");
            if (montoMinimo.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El monto mínimo debe ser mayor a cero");
            }
        }

        @Override
        public boolean esSatisfechoPor(ContextoValidacion contexto) {
            Objects.requireNonNull(contexto, "El contexto no puede ser null");
            return contexto.getTotalPedido().compareTo(montoMinimo) >= 0;
        }

        @Override
        public TipoCriterio getTipo() {
            return TipoCriterio.MONTO_MINIMO;
        }
    }
}
