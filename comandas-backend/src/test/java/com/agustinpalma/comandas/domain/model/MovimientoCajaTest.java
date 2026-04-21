package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimiento;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MovimientoCajaId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de dominio puro para MovimientoCaja.
 *
 * Escenarios:
 * - Generación de comprobante con formato auditable EGR/ING-yyyyMMdd-HHmmss-XXXX.
 * - Validaciones de construcción: monto > 0, descripción no vacía.
 *
 * Formato del comprobante: {prefijo}-yyyyMMdd-HHmmss-XXXX
 *   donde prefijo = EGR (EGRESO) o ING (INGRESO),
 *   y XXXX = primeros 4 caracteres del UUID en mayúsculas.
 *
 * Sin Spring. Sin Mockito. Dominio puro.
 */
class MovimientoCajaTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 4, 21, 15, 30, 45);
    private LocalId localId;

    @BeforeEach
    void setUp() {
        localId = LocalId.generate();
    }

    // ============================================================
    // Generación de comprobante auditable
    // ============================================================

    @Nested
    class NumeroComprobante {

        @Test
        void deberia_generar_comprobante_egreso_con_formato_correcto() {
            // Given / When
            MovimientoCaja egreso = new MovimientoCaja(
                MovimientoCajaId.generate(), localId,
                new BigDecimal("500"), "Compra de insumos",
                FECHA, TipoMovimiento.EGRESO
            );

            // Then — EGR + fecha legible + sufijo del UUID para trazabilidad
            assertThat(egreso.getNumeroComprobante())
                .matches("EGR-\\d{8}-\\d{6}-[0-9A-F]{4}");
        }

        @Test
        void deberia_generar_comprobante_ingreso_con_formato_correcto() {
            // Given / When
            MovimientoCaja ingreso = new MovimientoCaja(
                MovimientoCajaId.generate(), localId,
                new BigDecimal("300"), "Ingreso de plataforma",
                FECHA, TipoMovimiento.INGRESO
            );

            // Then — mismo formato que EGRESO pero con prefijo ING
            assertThat(ingreso.getNumeroComprobante())
                .matches("ING-\\d{8}-\\d{6}-[0-9A-F]{4}");
        }
    }

    // ============================================================
    // Validaciones de monto
    // ============================================================

    @Nested
    class ValidacionMonto {

        @Test
        void deberia_rechazar_monto_cero() {
            // Given / When / Then — un movimiento de $0 no tiene impacto contable
            assertThatThrownBy(() ->
                new MovimientoCaja(
                    MovimientoCajaId.generate(), localId,
                    BigDecimal.ZERO, "Sin monto",
                    FECHA, TipoMovimiento.EGRESO
                )
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deberia_rechazar_monto_negativo() {
            // Given / When / Then — el monto representa valor absoluto (siempre > 0)
            assertThatThrownBy(() ->
                new MovimientoCaja(
                    MovimientoCajaId.generate(), localId,
                    new BigDecimal("-100"), "Monto negativo",
                    FECHA, TipoMovimiento.INGRESO
                )
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    // Validación de descripción
    // ============================================================

    @Nested
    class ValidacionDescripcion {

        @Test
        void deberia_rechazar_descripcion_vacia() {
            // Given / When / Then — la descripción es obligatoria para auditoría
            assertThatThrownBy(() ->
                new MovimientoCaja(
                    MovimientoCajaId.generate(), localId,
                    new BigDecimal("200"), "",
                    FECHA, TipoMovimiento.EGRESO
                )
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
