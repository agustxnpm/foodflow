package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de dominio puro para Pago (Value Object contable).
 *
 * Pago es inmutable y validado en construcción (fail fast):
 * - monto > 0 (valor absoluto del pago)
 * - medio != null (canal de pago)
 *
 * Sin Spring. Sin Mockito. Dominio puro.
 */
class PagoTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 4, 21, 14, 30);

    // ============================================================
    // Construcción válida
    // ============================================================

    @Test
    void deberia_crear_pago_valido() {
        // Given / When
        Pago pago = new Pago(MedioPago.EFECTIVO, new BigDecimal("1500"), FECHA);

        // Then — campos correctamente asignados; monto positivo
        assertThat(pago.getMedio()).isEqualTo(MedioPago.EFECTIVO);
        assertThat(pago.getMonto()).isPositive();
        assertThat(pago.getMonto()).isEqualByComparingTo("1500");
        assertThat(pago.getFecha()).isEqualTo(FECHA);
    }

    // ============================================================
    // Guards fail fast — validación de monto
    // ============================================================

    @Nested
    class ValidacionMonto {

        @Test
        void deberia_rechazar_monto_cero() {
            // Given / When / Then — un pago de $0 no tiene sentido contable
            assertThatThrownBy(() ->
                new Pago(MedioPago.EFECTIVO, BigDecimal.ZERO, FECHA)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deberia_rechazar_monto_negativo() {
            // Given / When / Then — un monto negativo rompería la contabilidad del pedido
            assertThatThrownBy(() ->
                new Pago(MedioPago.TARJETA, new BigDecimal("-100"), FECHA)
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    // Guards fail fast — campos obligatorios
    // ============================================================

    @Nested
    class ValidacionCampos {

        @Test
        void deberia_rechazar_medio_de_pago_null() {
            // Given / When / Then — el medio de pago es obligatorio para auditoría
            assertThatThrownBy(() ->
                new Pago(null, new BigDecimal("500"), FECHA)
            ).isInstanceOf(NullPointerException.class);
        }
    }
}

