package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.CriterioActivacion.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de dominio puro para Promocion y sus Value Objects.
 * Sin Spring, sin base de datos.
 * 
 * Validan:
 * - Construcción correcta de Promocion con cada tipo de estrategia y triggers
 * - Validaciones de invariantes del dominio
 * - Lógica de activación con múltiples triggers (AND logic)
 * - Value Objects inmutables y validados en construcción
 */
class PromocionTest {

    private final LocalId localId = LocalId.generate();

    // ============================================
    // Promocion — Creación exitosa
    // ============================================

    @Test
    void deberia_crear_promocion_con_estrategia_descuento_porcentaje() {
        // Given
        var estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, new BigDecimal("20"));
        var triggerTemporal = new CriterioTemporal(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1),
                null, null, null
        );

        // When
        Promocion promo = new Promocion(
                PromocionId.generate(), localId,
                "20% en empanadas", "Promoción de verano",
                1, EstadoPromocion.ACTIVA,
                estrategia, List.of(triggerTemporal)
        );

        // Then
        assertEquals("20% en empanadas", promo.getNombre());
        assertEquals("Promoción de verano", promo.getDescripcion());
        assertEquals(1, promo.getPrioridad());
        assertEquals(EstadoPromocion.ACTIVA, promo.getEstado());
        assertEquals(TipoEstrategia.DESCUENTO_DIRECTO, promo.getEstrategia().getTipo());
        assertEquals(1, promo.getTriggers().size());

        assertInstanceOf(DescuentoDirecto.class, promo.getEstrategia());
        DescuentoDirecto dd = (DescuentoDirecto) promo.getEstrategia();
        assertEquals(ModoDescuento.PORCENTAJE, dd.modo());
        assertEquals(new BigDecimal("20"), dd.valor());
    }

    @Test
    void deberia_crear_promocion_con_estrategia_cantidad_fija_2x1() {
        // Given
        var estrategia = new CantidadFija(2, 1);
        var triggerTemporal = new CriterioTemporal(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                null, null, null
        );

        // When
        Promocion promo = new Promocion(
                PromocionId.generate(), localId,
                "2x1 en cervezas", null,
                5, EstadoPromocion.ACTIVA,
                estrategia, List.of(triggerTemporal)
        );

        // Then
        assertEquals("2x1 en cervezas", promo.getNombre());
        assertNull(promo.getDescripcion());
        assertEquals(TipoEstrategia.CANTIDAD_FIJA, promo.getEstrategia().getTipo());

        CantidadFija cf = (CantidadFija) promo.getEstrategia();
        assertEquals(2, cf.cantidadLlevas());
        assertEquals(1, cf.cantidadPagas());
    }

    @Test
    void deberia_crear_promocion_con_estrategia_combo_condicional() {
        // Given - "Si comprás 1 hamburguesa, la gaseosa tiene 50% off"
        var estrategia = new ComboCondicional(1, new BigDecimal("50"));
        var triggerTemporal = new CriterioTemporal(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 6, 30),
                EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                LocalTime.of(18, 0),
                LocalTime.of(23, 0)
        );

        // When
        Promocion promo = new Promocion(
                PromocionId.generate(), localId,
                "Combo Burger", "Comprá una hamburguesa y la gaseosa sale 50% off",
                10, EstadoPromocion.ACTIVA,
                estrategia, List.of(triggerTemporal)
        );

        // Then
        assertEquals(TipoEstrategia.COMBO_CONDICIONAL, promo.getEstrategia().getTipo());
        ComboCondicional cc = (ComboCondicional) promo.getEstrategia();
        assertEquals(1, cc.cantidadMinimaTrigger());
        assertEquals(new BigDecimal("50"), cc.porcentajeBeneficio());
        
        // Trigger temporal con restricciones
        CriterioTemporal ct = (CriterioTemporal) promo.getTriggers().get(0);
        assertTrue(ct.diasSemana().contains(DayOfWeek.FRIDAY));
        assertTrue(ct.diasSemana().contains(DayOfWeek.SATURDAY));
        assertEquals(LocalTime.of(18, 0), ct.horaDesde());
    }

    // ============================================
    // Promocion — Validaciones de invariantes
    // ============================================

    @Test
    void deberia_rechazar_prioridad_negativa() {
        var estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, new BigDecimal("10"));
        var triggers = List.<CriterioActivacion>of(CriterioTemporal.soloFechas(LocalDate.now(), LocalDate.now().plusDays(30)));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Promocion(
                        PromocionId.generate(), localId,
                        "Promo inválida", null,
                        -1, EstadoPromocion.ACTIVA,
                        estrategia, triggers
                )
        );
        assertTrue(ex.getMessage().contains("negativa"));
    }

    @Test
    void deberia_rechazar_nombre_vacio() {
        var estrategia = new DescuentoDirecto(ModoDescuento.MONTO_FIJO, new BigDecimal("500"));
        var triggers = List.<CriterioActivacion>of(CriterioTemporal.soloFechas(LocalDate.now(), LocalDate.now().plusDays(30)));

        assertThrows(
                IllegalArgumentException.class,
                () -> new Promocion(
                        PromocionId.generate(), localId,
                        "  ", null,
                        0, EstadoPromocion.ACTIVA,
                        estrategia, triggers
                )
        );
    }

    @Test
    void deberia_rechazar_estrategia_null() {
        var trigger = new CriterioTemporal(LocalDate.now(), LocalDate.now().plusDays(30), null, null, null);

        assertThrows(
                NullPointerException.class,
                () -> new Promocion(
                        PromocionId.generate(), localId,
                        "Promo", null,
                        0, EstadoPromocion.ACTIVA,
                        null, List.of(trigger)
                )
        );
    }

    @Test
    void deberia_rechazar_triggers_null() {
        var estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, new BigDecimal("10"));

        assertThrows(
                IllegalArgumentException.class,
                () -> new Promocion(
                        PromocionId.generate(), localId,
                        "Promo", null,
                        0, EstadoPromocion.ACTIVA,
                        estrategia, null
                )
        );
    }

    @Test
    void deberia_rechazar_triggers_vacios() {
        var estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, new BigDecimal("10"));

        assertThrows(
                IllegalArgumentException.class,
                () -> new Promocion(
                        PromocionId.generate(), localId,
                        "Promo", null,
                        0, EstadoPromocion.ACTIVA,
                        estrategia, List.of()
                )
        );
    }

    // ============================================
    // Promocion — Comportamiento
    // ============================================

    @Test
    void deberia_activar_y_desactivar_promocion() {
        var estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, new BigDecimal("10"));
        var trigger = new CriterioTemporal(LocalDate.now(), LocalDate.now().plusDays(30), null, null, null);
        
        Promocion promo = new Promocion(
                PromocionId.generate(), localId,
                "Promo test", null,
                0, EstadoPromocion.ACTIVA,
                estrategia, List.of(trigger)
        );

        promo.desactivar();
        assertEquals(EstadoPromocion.INACTIVA, promo.getEstado());

        promo.activar();
        assertEquals(EstadoPromocion.ACTIVA, promo.getEstado());
    }

    @Test
    void deberia_activarse_cuando_todos_los_triggers_se_cumplen() {
        // Given - Promoción con múltiples triggers (AND logic)
        var estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, new BigDecimal("20"));
        var triggerTemporal = new CriterioTemporal(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                EnumSet.of(DayOfWeek.FRIDAY),
                LocalTime.of(18, 0),
                LocalTime.of(23, 59)
        );
        var triggerMonto = new CriterioMontoMinimo(new BigDecimal("1000"));

        Promocion promo = new Promocion(
                PromocionId.generate(), localId,
                "Happy Hour Premium", null,
                1, EstadoPromocion.ACTIVA,
                estrategia, List.of(triggerTemporal, triggerMonto)
        );

        // When - Contexto que cumple TODOS los triggers
        var contexto = ContextoValidacion.builder()
                .fecha(LocalDate.of(2026, 2, 6)) // Viernes
                .hora(LocalTime.of(20, 0))
                .totalPedido(new BigDecimal("1500"))
                .build();

        // Then
        assertTrue(promo.puedeActivarse(contexto));
    }

    @Test
    void deberia_no_activarse_si_falla_un_trigger() {
        // Given
        var estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, new BigDecimal("20"));
        var triggerTemporal = new CriterioTemporal(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                null, null, null
        );
        var triggerMonto = new CriterioMontoMinimo(new BigDecimal("1000"));

        Promocion promo = new Promocion(
                PromocionId.generate(), localId,
                "Promo compra mínima", null,
                1, EstadoPromocion.ACTIVA,
                estrategia, List.of(triggerTemporal, triggerMonto)
        );

        // When - Contexto que cumple fecha pero NO el monto
        var contexto = ContextoValidacion.builder()
                .fecha(LocalDate.of(2026, 2, 10))
                .totalPedido(new BigDecimal("500")) // Menor al mínimo
                .build();

        // Then
        assertFalse(promo.puedeActivarse(contexto));
    }

    @Test
    void deberia_activarse_con_trigger_contenido_cuando_producto_esta_en_pedido() {
        // Given
        var estrategia = new CantidadFija(2, 1);
        var productoEspecifico = ProductoId.generate();
        var triggerContenido = new CriterioContenido(Set.of(productoEspecifico));

        Promocion promo = new Promocion(
                PromocionId.generate(), localId,
                "2x1 en producto específico", null,
                1, EstadoPromocion.ACTIVA,
                estrategia, List.of(triggerContenido)
        );

        // When - Pedido contiene el producto requerido
        var contexto = ContextoValidacion.builder()
                .fecha(LocalDate.now())
                .productosEnPedido(List.of(productoEspecifico, ProductoId.generate()))
                .build();

        // Then
        assertTrue(promo.puedeActivarse(contexto));
    }

    @Test
    void deberia_no_activarse_con_trigger_contenido_cuando_producto_no_esta() {
        // Given
        var estrategia = new CantidadFija(2, 1);
        var productoRequerido = ProductoId.generate();
        var triggerContenido = new CriterioContenido(Set.of(productoRequerido));

        Promocion promo = new Promocion(
                PromocionId.generate(), localId,
                "2x1 en producto específico", null,
                1, EstadoPromocion.ACTIVA,
                estrategia, List.of(triggerContenido)
        );

        // When - Pedido NO contiene el producto requerido
        var contexto = ContextoValidacion.builder()
                .fecha(LocalDate.now())
                .productosEnPedido(List.of(ProductoId.generate(), ProductoId.generate()))
                .build();

        // Then
        assertFalse(promo.puedeActivarse(contexto));
    }

    // ============================================
    // EstrategiaPromocion — Validaciones
    // ============================================

    @Test
    void deberia_rechazar_descuento_porcentaje_mayor_a_100() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DescuentoDirecto(ModoDescuento.PORCENTAJE, new BigDecimal("101"))
        );
    }

    @Test
    void deberia_rechazar_descuento_valor_cero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DescuentoDirecto(ModoDescuento.MONTO_FIJO, BigDecimal.ZERO)
        );
    }

    @Test
    void deberia_rechazar_cantidad_fija_sin_beneficio() {
        // 2x2 no tiene sentido — llevas igual que pagas
        assertThrows(
                IllegalArgumentException.class,
                () -> new CantidadFija(2, 2)
        );
    }

    @Test
    void deberia_rechazar_cantidad_fija_invertida() {
        // 1x2 — pagas más de lo que llevas
        assertThrows(
                IllegalArgumentException.class,
                () -> new CantidadFija(1, 2)
        );
    }

    @Test
    void deberia_rechazar_combo_con_porcentaje_cero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ComboCondicional(1, BigDecimal.ZERO)
        );
    }

    @Test
    void deberia_rechazar_combo_con_porcentaje_mayor_a_100() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ComboCondicional(1, new BigDecimal("101"))
        );
    }

    // ============================================
    // CriterioTemporal — Validaciones
    // ============================================

    @Test
    void deberia_rechazar_criterio_temporal_con_fechas_invertidas() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CriterioTemporal(
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 2, 1),
                        null, null, null
                )
        );
    }

    @Test
    void deberia_rechazar_criterio_temporal_con_horarios_invertidos() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CriterioTemporal(
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 3, 1),
                        null,
                        LocalTime.of(23, 0),
                        LocalTime.of(18, 0)
                )
        );
    }

    @Test
    void deberia_aceptar_criterio_temporal_solo_con_fechas() {
        var criterio = CriterioTemporal.soloFechas(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1)
        );

        assertEquals(TipoCriterio.TEMPORAL, criterio.getTipo());
        assertEquals(7, criterio.diasSemana().size()); // Todos los días por defecto
        assertNull(criterio.horaDesde());
    }

    // ============================================
    // CriterioContenido — Validaciones
    // ============================================

    @Test
    void deberia_rechazar_criterio_contenido_sin_productos() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CriterioContenido(Set.of())
        );
    }

    @Test
    void deberia_rechazar_criterio_contenido_con_productos_null() {
        assertThrows(
                NullPointerException.class,
                () -> new CriterioContenido(null)
        );
    }

    // ============================================
    // CriterioMontoMinimo — Validaciones
    // ============================================

    @Test
    void deberia_rechazar_criterio_monto_minimo_cero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CriterioMontoMinimo(BigDecimal.ZERO)
        );
    }

    @Test
    void deberia_rechazar_criterio_monto_minimo_negativo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CriterioMontoMinimo(new BigDecimal("-100"))
        );
    }
}
