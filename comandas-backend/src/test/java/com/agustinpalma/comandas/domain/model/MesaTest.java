package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de dominio puro para la máquina de estados de Mesa.
 *
 * Escenarios:
 * - Estado inicial: toda mesa nace LIBRE.
 * - Transiciones válidas: LIBRE→ABIERTA (abrir), ABIERTA→LIBRE (liberar),
 *   LIBRE→ABIERTA (reocupar — HU-14 reapertura).
 * - Guards: no se puede abrir una ABIERTA ni liberar una LIBRE.
 *
 * Sin Spring. Sin Mockito. Dominio puro.
 */
class MesaTest {

    private LocalId localId;

    @BeforeEach
    void setUp() {
        localId = LocalId.generate();
    }

    private Mesa nuevaMesa() {
        return new Mesa(MesaId.generate(), localId, 1);
    }

    // ============================================================
    // Estado inicial
    // ============================================================

    @Test
    void deberia_crear_mesa_con_estado_libre() {
        // Given / When
        Mesa mesa = new Mesa(MesaId.generate(), localId, 5);

        // Then — invariante de construcción: toda mesa nace LIBRE
        assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.LIBRE);
        assertThat(mesa.getNumero()).isEqualTo(5);
        assertThat(mesa.getId()).isNotNull();
    }

    // ============================================================
    // Transiciones LIBRE → ABIERTA (abrir)
    // ============================================================

    @Nested
    class Apertura {

        @Test
        void deberia_abrir_mesa_libre() {
            // Given
            Mesa mesa = nuevaMesa();

            // When
            mesa.abrir();

            // Then
            assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.ABIERTA);
        }

        @Test
        void deberia_rechazar_abrir_mesa_ya_abierta() {
            // Given — mesa ya tiene un pedido activo
            Mesa mesa = nuevaMesa();
            mesa.abrir();

            // When / Then — la invariante impide doble apertura
            assertThatThrownBy(mesa::abrir)
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    // Transiciones ABIERTA → LIBRE (liberar)
    // ============================================================

    @Nested
    class Liberacion {

        @Test
        void deberia_liberar_mesa_abierta() {
            // Given
            Mesa mesa = nuevaMesa();
            mesa.abrir();

            // When
            mesa.liberar();

            // Then
            assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.LIBRE);
        }

        @Test
        void deberia_rechazar_liberar_mesa_ya_libre() {
            // Given — mesa recién creada, nunca abierta
            Mesa mesa = nuevaMesa();

            // When / Then — liberar una mesa LIBRE no tiene sentido operativo
            assertThatThrownBy(mesa::liberar)
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    // Reapertura LIBRE → ABIERTA (reocupar) — HU-14
    // ============================================================

    @Nested
    class Reapertura {

        @Test
        void deberia_reocupar_mesa_libre() {
            // Given — mesa liberada después de un cierre normal
            Mesa mesa = nuevaMesa();
            mesa.abrir();
            mesa.liberar();
            assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.LIBRE);

            // When
            mesa.reocupar();

            // Then — la mesa vuelve a ABIERTA para el nuevo pedido
            assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.ABIERTA);
        }
    }
}
