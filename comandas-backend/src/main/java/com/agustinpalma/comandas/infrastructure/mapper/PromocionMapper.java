package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.AlcancePromocion;
import com.agustinpalma.comandas.domain.model.CriterioActivacion;
import com.agustinpalma.comandas.domain.model.CriterioActivacion.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.*;
import com.agustinpalma.comandas.domain.model.ItemPromocion;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.infrastructure.persistence.entity.ItemPromocionEntity;
import com.agustinpalma.comandas.infrastructure.persistence.entity.PromocionEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper bidireccional entre Promocion (dominio) y PromocionEntity (JPA).
 * 
 * Responsabilidades:
 * - Traducir la jerarquía sellada EstrategiaPromocion ↔ columnas aplanadas
 * - Traducir List<CriterioActivacion> ↔ JSON en columna triggers_json
 * - Traducir AlcancePromocion ↔ tabla intermedia promocion_productos_scope (HU-09)
 * - Manejar serialización/deserialización JSONB con estructura polimórfica
 */
@Component
public class PromocionMapper {

    private final ObjectMapper objectMapper;

    public PromocionMapper() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Convierte la entidad JPA a dominio.
     * El alcance (scope) se carga por separado en PromocionRepositoryImpl.
     */
    public Promocion toDomain(PromocionEntity entity) {
        EstrategiaPromocion estrategia = reconstruirEstrategia(entity);
        List<CriterioActivacion> triggers = deserializarTriggers(entity.getTriggersJson());

        return new Promocion(
                new PromocionId(entity.getId()),
                new LocalId(entity.getLocalId()),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getPrioridad(),
                EstadoPromocion.valueOf(entity.getEstado()),
                estrategia,
                triggers
        );
    }

    /**
     * Convierte el dominio a entidad JPA.
     * El alcance (scope) se persiste por separado en PromocionRepositoryImpl.
     */
    public PromocionEntity toEntity(Promocion domain) {
        PromocionEntity entity = new PromocionEntity();
        entity.setId(domain.getId().getValue());
        entity.setLocalId(domain.getLocalId().getValue());
        entity.setNombre(domain.getNombre());
        entity.setDescripcion(domain.getDescripcion());
        entity.setPrioridad(domain.getPrioridad());
        entity.setEstado(domain.getEstado().name());

        // Estrategia → columnas aplanadas
        aplanarEstrategia(domain.getEstrategia(), entity);

        // Triggers → JSON
        entity.setTriggersJson(serializarTriggers(domain.getTriggers()));

        return entity;
    }

    // ============================================
    // Alcance (Scope) - HU-09
    // ============================================

    /**
     * Convierte una lista de entidades ItemPromocion a dominio AlcancePromocion.
     */
    public AlcancePromocion toDomainAlcance(List<ItemPromocionEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return AlcancePromocion.vacio();
        }

        List<ItemPromocion> items = entities.stream()
                .map(this::toDomainItem)
                .toList();

        return new AlcancePromocion(items);
    }

    /**
     * Convierte una entidad ItemPromocion a dominio.
     */
    private ItemPromocion toDomainItem(ItemPromocionEntity entity) {
        return new ItemPromocion(
                new ItemPromocionId(entity.getId()),
                entity.getReferenciaId(),
                entity.getTipoAlcance(),
                entity.getRol()
        );
    }

    /**
     * Convierte un AlcancePromocion del dominio a entidades JPA.
     */
    public List<ItemPromocionEntity> toEntityAlcance(AlcancePromocion alcance, UUID promocionId) {
        if (alcance == null || !alcance.tieneItems()) {
            return Collections.emptyList();
        }

        return alcance.getItems().stream()
                .map(item -> toEntityItem(item, promocionId))
                .toList();
    }

    /**
     * Convierte un ItemPromocion del dominio a entidad JPA.
     */
    private ItemPromocionEntity toEntityItem(ItemPromocion item, UUID promocionId) {
        return new ItemPromocionEntity(
                item.getId().getValue(),
                promocionId,
                item.getReferenciaId(),
                item.getTipo(),
                item.getRol()
        );
    }

    // ============================================
    // Estrategia: dominio → entity
    // ============================================

    private void aplanarEstrategia(EstrategiaPromocion estrategia, PromocionEntity entity) {
        entity.setTipoEstrategia(estrategia.getTipo().name());

        switch (estrategia) {
            case DescuentoDirecto dd -> {
                entity.setModoDescuento(dd.modo().name());
                entity.setValorDescuento(dd.valor());
            }
            case CantidadFija cf -> {
                entity.setCantidadLlevas(cf.cantidadLlevas());
                entity.setCantidadPagas(cf.cantidadPagas());
            }
            case ComboCondicional cc -> {
                entity.setCantidadMinimaTrigger(cc.cantidadMinimaTrigger());
                entity.setPorcentajeBeneficio(cc.porcentajeBeneficio());
            }
        }
    }

    // ============================================
    // Estrategia: entity → dominio
    // ============================================

    private EstrategiaPromocion reconstruirEstrategia(PromocionEntity entity) {
        TipoEstrategia tipo = TipoEstrategia.valueOf(entity.getTipoEstrategia());

        return switch (tipo) {
            case DESCUENTO_DIRECTO -> new DescuentoDirecto(
                    ModoDescuento.valueOf(entity.getModoDescuento()),
                    entity.getValorDescuento()
            );
            case CANTIDAD_FIJA -> new CantidadFija(
                    entity.getCantidadLlevas(),
                    entity.getCantidadPagas()
            );
            case COMBO_CONDICIONAL -> new ComboCondicional(
                    entity.getCantidadMinimaTrigger(),
                    entity.getPorcentajeBeneficio()
            );
        };
    }

    // ============================================
    // Triggers: dominio ↔ JSON
    // ============================================

    /**
     * Serializa la lista de criterios de activación a JSON.
     * 
     * Cada criterio se convierte a un mapa con estructura plana:
     * {tipo, ...campos específicos del tipo}
     */
    private String serializarTriggers(List<CriterioActivacion> triggers) {
        List<Map<String, Object>> triggersMaps = triggers.stream()
                .map(this::triggerToMap)
                .toList();

        try {
            return objectMapper.writeValueAsString(triggersMaps);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al serializar triggers a JSON", e);
        }
    }

    /**
     * Convierte un CriterioActivacion a un Map para JSON.
     */
    private Map<String, Object> triggerToMap(CriterioActivacion trigger) {
        Map<String, Object> map = new HashMap<>();
        map.put("tipo", trigger.getTipo().name());

        switch (trigger) {
            case CriterioTemporal ct -> {
                map.put("fechaDesde", ct.fechaDesde().toString());
                map.put("fechaHasta", ct.fechaHasta().toString());
                if (ct.diasSemana() != null) {
                    map.put("diasSemana", ct.diasSemana().stream().map(DayOfWeek::name).toList());
                }
                if (ct.horaDesde() != null) {
                    map.put("horaDesde", ct.horaDesde().toString());
                }
                if (ct.horaHasta() != null) {
                    map.put("horaHasta", ct.horaHasta().toString());
                }
            }
            case CriterioContenido cc -> {
                map.put("productosRequeridos",
                        cc.productosRequeridos().stream()
                                .map(pid -> pid.getValue().toString())
                                .toList()
                );
            }
            case CriterioMontoMinimo cm -> {
                map.put("montoMinimo", cm.montoMinimo());
            }
        }

        return map;
    }

    /**
     * Deserializa el JSON de triggers a la lista de criterios de dominio.
     */
    private List<CriterioActivacion> deserializarTriggers(String triggersJson) {
        if (triggersJson == null || triggersJson.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            List<Map<String, Object>> triggersMaps =
                    objectMapper.readValue(triggersJson, new TypeReference<>() {});

            return triggersMaps.stream()
                    .map(this::mapToTrigger)
                    .toList();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error al deserializar triggers desde JSON", e);
        }
    }

    /**
     * Reconstruye un CriterioActivacion desde un Map JSON.
     */
    @SuppressWarnings("unchecked")
    private CriterioActivacion mapToTrigger(Map<String, Object> map) {
        TipoCriterio tipo = TipoCriterio.valueOf((String) map.get("tipo"));

        return switch (tipo) {
            case TEMPORAL -> {
                LocalDate fechaDesde = LocalDate.parse((String) map.get("fechaDesde"));
                LocalDate fechaHasta = LocalDate.parse((String) map.get("fechaHasta"));

                Set<DayOfWeek> dias = null;
                if (map.containsKey("diasSemana")) {
                    List<String> diasStr = (List<String>) map.get("diasSemana");
                    if (diasStr != null) {
                        dias = diasStr.stream()
                                .map(DayOfWeek::valueOf)
                                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
                    }
                }

                String horaDesdeStr = (String) map.get("horaDesde");
                LocalTime horaDesde = (map.containsKey("horaDesde") && horaDesdeStr != null)
                        ? LocalTime.parse(horaDesdeStr)
                        : null;
                
                String horaHastaStr = (String) map.get("horaHasta");
                LocalTime horaHasta = (map.containsKey("horaHasta") && horaHastaStr != null)
                        ? LocalTime.parse(horaHastaStr)
                        : null;

                yield new CriterioTemporal(fechaDesde, fechaHasta, dias, horaDesde, horaHasta);
            }
            case CONTENIDO -> {
                List<String> productosStr = (List<String>) map.get("productosRequeridos");
                Set<ProductoId> productos = productosStr.stream()
                        .map(UUID::fromString)
                        .map(ProductoId::new)
                        .collect(Collectors.toSet());
                yield new CriterioContenido(productos);
            }
            case MONTO_MINIMO -> {
                Object montoObj = map.get("montoMinimo");
                java.math.BigDecimal monto = montoObj instanceof Number num
                        ? java.math.BigDecimal.valueOf(num.doubleValue())
                        : new java.math.BigDecimal(montoObj.toString());
                yield new CriterioMontoMinimo(monto);
            }
        };
    }
}
