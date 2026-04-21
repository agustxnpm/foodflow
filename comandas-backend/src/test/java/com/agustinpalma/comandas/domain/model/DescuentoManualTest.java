package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.ModoDescuento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios de dominio puro para DescuentoManual (HU-14).
 *
 * Validan:
 * - Construcción válida e inválida del Value Object
 * - Cálculo correcto del monto según tipo (PORCENTAJE / MONTO_FIJO)
 * - Comportamiento en edge cases financieros (bordes de rango, base cero, capeo)
 * - Inmutabilidad del Value Object
 *
 * Sin Spring. Sin base de datos. Dominio puro.
 */
class DescuentoManualTest {

    // ============================================================
    // Fixtures reutilizables
    // ============================================================

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 4, 21, 10, 0);

    private DescuentoManual porcentaje(String valor) {
        return new DescuentoManual(ModoDescuento.PORCENTAJE, new BigDecimal(valor), "Descuento test", USUARIO_ID, FECHA);
    }

    private DescuentoManual montoFijo(String valor) {
        return new DescuentoManual(ModoDescuento.MONTO_FIJO, new BigDecimal(valor), "Descuento test", USUARIO_ID, FECHA);
    }

    private BigDecimal base(String valor) {
        return new BigDecimal(valor);
    }

    // ============================================================
    // Construcción válida
    // ============================================================

    @Test
    void deberia_crear_descuento_porcentaje_con_valor_valido() {
        // Given / When
        DescuentoManual descuento = porcentaje("15");

        // Then
        assertEquals(ModoDescuento.PORCENTAJE, descuento.getTipo());
        assertEquals(new BigDecimal("15"), descuento.getValor());
        assertEquals("Descuento test", descuento.getRazon());
        assertEquals(USUARIO_ID, descuento.getUsuarioId());
        assertEquals(FECHA, descuento.getFechaAplicacion());
    }

    @Test
    void deberia_crear_descuento_monto_fijo_con_valor_valido() {
        // Given / When
        DescuentoManual descuento = montoFijo("500");

        // Then
        assertEquals(ModoDescuento.MONTO_FIJO, descuento.getTipo());
        assertEquals(new BigDecimal("500"), descuento.getValor());
    }

    @Test
    void deberia_crear_descuento_con_razon_vacia() {
        // Given / When / Then — la razón puede estar vacía (solo null es inválido)
        assertDoesNotThrow(() ->
            new DescuentoManual(ModoDescuento.PORCENTAJE, new BigDecimal("10"), "", USUARIO_ID, FECHA)
        );
    }

    @Test
    void deberia_crear_descuento_porcentaje_con_valor_minimo_permitido() {
        // Given / When / Then — 0.01 es el límite inferior válido
        assertDoesNotThrow(() -> porcentaje("0.01"));
    }

    @Test
    void deberia_crear_descuento_porcentaje_con_valor_maximo_permitido() {
        // Given / When / Then — 100 es el límite superior válido (100% de descuento)
        assertDoesNotThrow(() -> porcentaje("100"));
    }

    // ============================================================
    // Validaciones de construcción inválida
    // ============================================================

    @Test
    void deberia_rechazar_tipo_null() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
            new DescuentoManual(null, new BigDecimal("10"), "Razon", USUARIO_ID, FECHA)
        );
    }

    @Test
    void deberia_rechazar_valor_null() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
            new DescuentoManual(ModoDescuento.PORCENTAJE, null, "Razon", USUARIO_ID, FECHA)
        );
    }

    @Test
    void deberia_rechazar_razon_null() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
            new DescuentoManual(ModoDescuento.PORCENTAJE, new BigDecimal("10"), null, USUARIO_ID, FECHA)
        );
    }

    @Test
    void deberia_rechazar_usuarioId_null() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
            new DescuentoManual(ModoDescuento.PORCENTAJE, new BigDecimal("10"), "Razon", null, FECHA)
        );
    }

    @Test
    void deberia_rechazar_fechaAplicacion_null() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
            new DescuentoManual(ModoDescuento.PORCENTAJE, new BigDecimal("10"), "Razon", USUARIO_ID, null)
        );
    }

    @Test
    void deberia_rechazar_porcentaje_igual_a_cero() {
        // Given / When / Then — 0 no tiene sentido como porcentaje de descuento
        assertThrows(IllegalArgumentException.class, () -> porcentaje("0"));
    }

    @Test
    void deberia_rechazar_porcentaje_negativo() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class, () -> porcentaje("-1"));
    }

    @Test
    void deberia_rechazar_porcentaje_mayor_a_cien() {
        // Given / When / Then — 101% no puede existir
        assertThrows(IllegalArgumentException.class, () -> porcentaje("100.01"));
    }

    @Test
    void deberia_rechazar_porcentaje_menor_a_001() {
        // Given — 0.001 está por debajo del límite inferior válido (0.01)
        // El dominio define el rango como [0.01, 100]. Un porcentaje de 0.001%
        // no tiene sentido operativo y podría enmascarar errores de entrada.
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> porcentaje("0.001"));
    }

    @Test
    void deberia_rechazar_monto_fijo_igual_a_cero() {
        // Given / When / Then — un descuento de $0 no tiene sentido
        assertThrows(IllegalArgumentException.class, () -> montoFijo("0"));
    }

    @Test
    void deberia_rechazar_monto_fijo_negativo() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class, () -> montoFijo("-100"));
    }

    // ============================================================
    // calcularMonto — PORCENTAJE
    // ============================================================

    @Test
    void deberia_calcular_monto_con_descuento_porcentaje_simple() {
        // Given
        DescuentoManual descuento = porcentaje("10");
        BigDecimal base = base("1000");

        // When
        BigDecimal monto = descuento.calcularMonto(base);

        // Then — 10% de $1000 = $100
        assertEquals(new BigDecimal("100.00"), monto);
    }

    @Test
    void deberia_calcular_monto_con_descuento_porcentaje_fraccionario() {
        // Given
        DescuentoManual descuento = porcentaje("15.5");
        BigDecimal base = base("200");

        // When
        BigDecimal monto = descuento.calcularMonto(base);

        // Then — 15.5% de $200 = $31 (redondeado HALF_UP a 2 decimales)
        assertEquals(new BigDecimal("31.00"), monto);
    }

    @Test
    void deberia_calcular_monto_con_porcentaje_cien_sobre_base_total() {
        // Given — 100% de descuento debe devolver la base completa
        DescuentoManual descuento = porcentaje("100");
        BigDecimal base = base("850");

        // When
        BigDecimal monto = descuento.calcularMonto(base);

        // Then
        assertEquals(new BigDecimal("850.00"), monto);
    }

    @Test
    void deberia_calcular_monto_cero_cuando_base_es_cero_con_porcentaje() {
        // Given — base gravable $0 → descuento $0, sin importar el porcentaje
        DescuentoManual descuento = porcentaje("50");
        BigDecimal base = BigDecimal.ZERO;

        // When
        BigDecimal monto = descuento.calcularMonto(base);

        // Then
        assertEquals(0, monto.compareTo(BigDecimal.ZERO));
    }

    @Test
    void deberia_redondear_porcentaje_con_resultado_con_mas_de_dos_decimales() {
        // Given — 33.33% de $100 = $33.33 (redondeado HALF_UP)
        DescuentoManual descuento = porcentaje("33.33");
        BigDecimal base = base("100");

        // When
        BigDecimal monto = descuento.calcularMonto(base);

        // Then — 100 * 33.33 / 100 = 33.33
        assertEquals(2, monto.scale());
        assertTrue(monto.compareTo(BigDecimal.ZERO) > 0);
    }

    // ============================================================
    // calcularMonto — MONTO_FIJO
    // ============================================================

    @Test
    void deberia_calcular_monto_fijo_cuando_valor_es_menor_a_la_base() {
        // Given — el monto fijo cabe dentro de la base
        DescuentoManual descuento = montoFijo("300");
        BigDecimal base = base("1000");

        // When
        BigDecimal monto = descuento.calcularMonto(base);

        // Then — el descuento se aplica directamente
        assertEquals(new BigDecimal("300"), monto);
    }

    @Test
    void deberia_capear_monto_fijo_a_la_base_gravable_para_evitar_precio_negativo() {
        // Given — el descuento supera a la base → se capea
        DescuentoManual descuento = montoFijo("1500");
        BigDecimal base = base("500");

        // When
        BigDecimal monto = descuento.calcularMonto(base);

        // Then — el descuento no puede superar la base (min(1500, 500) = 500)
        assertEquals(new BigDecimal("500"), monto);
    }

    @Test
    void deberia_calcular_monto_fijo_exactamente_igual_a_la_base() {
        // Given — el descuento es exactamente la base (precio final = $0)
        DescuentoManual descuento = montoFijo("750");
        BigDecimal base = base("750");

        // When
        BigDecimal monto = descuento.calcularMonto(base);

        // Then — min(750, 750) = 750
        assertEquals(new BigDecimal("750"), monto);
    }

    @Test
    void deberia_calcular_monto_fijo_cero_cuando_base_es_cero() {
        // Given — base $0 → descuento $0 (min(500, 0) = 0)
        DescuentoManual descuento = montoFijo("500");
        BigDecimal base = BigDecimal.ZERO;

        // When
        BigDecimal monto = descuento.calcularMonto(base);

        // Then
        assertEquals(0, monto.compareTo(BigDecimal.ZERO));
    }

    // ============================================================
    // calcularMonto — Validaciones de base gravable
    // ============================================================

    @Test
    void deberia_rechazar_base_gravable_null() {
        // Given
        DescuentoManual descuento = porcentaje("10");

        // When / Then
        assertThrows(NullPointerException.class, () -> descuento.calcularMonto(null));
    }

    @Test
    void deberia_rechazar_base_gravable_negativa() {
        // Given — una base negativa rompe la invariante del dominio
        DescuentoManual descuento = porcentaje("10");

        // When / Then
        assertThrows(IllegalArgumentException.class, () ->
            descuento.calcularMonto(new BigDecimal("-100"))
        );
    }

    // ============================================================
    // Inmutabilidad del Value Object
    // ============================================================

    @Test
    void deberia_ser_inmutable_y_retornar_mismos_valores_en_multiples_llamadas() {
        // Given
        DescuentoManual descuento = porcentaje("20");
        BigDecimal base = base("500");

        // When — calcular dos veces sobre la misma base
        BigDecimal primerCalculo = descuento.calcularMonto(base);
        BigDecimal segundoCalculo = descuento.calcularMonto(base);

        // Then — Value Object inmutable: mismo resultado siempre
        assertEquals(primerCalculo, segundoCalculo);
        assertEquals(ModoDescuento.PORCENTAJE, descuento.getTipo());
        assertEquals(new BigDecimal("20"), descuento.getValor());
    }
}
