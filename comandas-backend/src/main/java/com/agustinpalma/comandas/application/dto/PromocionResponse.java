package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.CriterioActivacion;
import com.agustinpalma.comandas.domain.model.CriterioActivacion.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.*;
import com.agustinpalma.comandas.domain.model.Promocion;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO de respuesta para una promoción con triggers configurables.
 * Mapea todos los datos relevantes del aggregate Promocion para la API REST.
 */
public record PromocionResponse(
        String id,
        String nombre,
        String descripcion,
        int prioridad,
        String estado,
        EstrategiaResponse estrategia,
        List<TriggerResponse> triggers
) {

    public static PromocionResponse fromDomain(Promocion promocion) {
        return new PromocionResponse(
                promocion.getId().getValue().toString(),
                promocion.getNombre(),
                promocion.getDescripcion(),
                promocion.getPrioridad(),
                promocion.getEstado().name(),
                EstrategiaResponse.fromDomain(promocion.getEstrategia()),
                promocion.getTriggers().stream()
                        .map(TriggerResponse::fromDomain)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Representación de la estrategia para la respuesta REST.
     * Usa un formato "aplanado" con tipo + parámetros condicionales.
     */
    public record EstrategiaResponse(
            String tipo,
            String modoDescuento,
            BigDecimal valorDescuento,
            Integer cantidadLlevas,
            Integer cantidadPagas,
            Integer cantidadMinimaTrigger,
            BigDecimal porcentajeBeneficio
    ) {

        public static EstrategiaResponse fromDomain(EstrategiaPromocion estrategia) {
            return switch (estrategia) {
                case DescuentoDirecto dd -> new EstrategiaResponse(
                        dd.getTipo().name(),
                        dd.modo().name(),
                        dd.valor(),
                        null, null, null, null
                );
                case CantidadFija cf -> new EstrategiaResponse(
                        cf.getTipo().name(),
                        null, null,
                        cf.cantidadLlevas(),
                        cf.cantidadPagas(),
                        null, null
                );
                case ComboCondicional cc -> new EstrategiaResponse(
                        cc.getTipo().name(),
                        null, null, null, null,
                        cc.cantidadMinimaTrigger(),
                        cc.porcentajeBeneficio()
                );
            };
        }
    }

    /**
     * Representación de un trigger (criterio de activación) para la respuesta REST.
     * Formato aplanado con tipo + parámetros condicionales.
     */
    public record TriggerResponse(
            String tipo,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Set<DayOfWeek> diasSemana,
            LocalTime horaDesde,
            LocalTime horaHasta,
            List<String> productosRequeridos,
            BigDecimal montoMinimo
    ) {

        public static TriggerResponse fromDomain(CriterioActivacion criterio) {
            return switch (criterio) {
                case CriterioTemporal ct -> new TriggerResponse(
                        ct.getTipo().name(),
                        ct.fechaDesde(),
                        ct.fechaHasta(),
                        ct.diasSemana(),
                        ct.horaDesde(),
                        ct.horaHasta(),
                        null, null
                );
                case CriterioContenido cc -> new TriggerResponse(
                        cc.getTipo().name(),
                        null, null, null, null, null,
                        cc.productosRequeridos().stream()
                                .map(pid -> pid.getValue().toString())
                                .collect(Collectors.toList()),
                        null
                );
                case CriterioMontoMinimo cm -> new TriggerResponse(
                        cm.getTipo().name(),
                        null, null, null, null, null, null,
                        cm.montoMinimo()
                );
            };
        }
    }
}
