package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Contexto de validación para evaluar si una promoción puede activarse.
 * 
 * Encapsula toda la información necesaria que los criterios pueden necesitar
 * para tomar decisiones:
 * - Información temporal (fecha, hora, día de semana)
 * - Contenido del pedido (productos, cantidades)
 * - Información financiera (total del pedido)
 * 
 * Inmutable, construido vía builder pattern para facilitar tests.
 */
public final class ContextoValidacion {

    private final LocalDate fecha;
    private final LocalTime hora;
    private final DayOfWeek diaSemana;
    private final List<ProductoId> productosEnPedido;
    private final BigDecimal totalPedido;

    private ContextoValidacion(Builder builder) {
        this.fecha = Objects.requireNonNull(builder.fecha, "La fecha es obligatoria");
        this.hora = builder.hora;
        this.diaSemana = builder.fecha.getDayOfWeek();
        this.productosEnPedido = builder.productosEnPedido != null 
            ? List.copyOf(builder.productosEnPedido) 
            : Collections.emptyList();
        this.totalPedido = builder.totalPedido != null ? builder.totalPedido : BigDecimal.ZERO;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public DayOfWeek getDiaSemana() {
        return diaSemana;
    }

    public List<ProductoId> getProductosEnPedido() {
        return productosEnPedido;
    }

    public BigDecimal getTotalPedido() {
        return totalPedido;
    }

    public boolean contieneProducto(ProductoId productoId) {
        return productosEnPedido.contains(productoId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate fecha;
        private LocalTime hora;
        private List<ProductoId> productosEnPedido;
        private BigDecimal totalPedido;

        public Builder fecha(LocalDate fecha) {
            this.fecha = fecha;
            return this;
        }

        public Builder hora(LocalTime hora) {
            this.hora = hora;
            return this;
        }

        public Builder productosEnPedido(List<ProductoId> productos) {
            this.productosEnPedido = productos;
            return this;
        }

        public Builder totalPedido(BigDecimal total) {
            this.totalPedido = total;
            return this;
        }

        public ContextoValidacion build() {
            return new ContextoValidacion(this);
        }
    }
}
