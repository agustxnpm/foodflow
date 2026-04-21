package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.testutil.PedidoTestBuilder;
import com.agustinpalma.comandas.testutil.ProductoTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios de dominio puro para Pedido (Aggregate Root).
 *
 * Cubren la "Regla de Oro" del dominio:
 *   El total del pedido se calcula a partir de los ítems base + descuentos acumulables,
 *   nunca al revés.
 *
 * Escenarios validados:
 * - Construcción e invariantes base
 * - Gestión de ítems: fusión, multi-tenancy, estado
 * - Cálculos financieros: subtotal, descuento porcentaje, monto fijo, jerarquía completa
 * - Transiciones de estado: cierre, reapertura, corrección in-place
 *
 * Sin Spring. Sin base de datos. Dominio puro.
 */
class PedidoTest {

    // ============================================================
    // Fixtures compartidas
    // ============================================================

    private static final LocalDateTime FECHA_APERTURA = LocalDateTime.of(2026, 4, 21, 10, 0);
    private static final LocalDateTime FECHA_CIERRE   = LocalDateTime.of(2026, 4, 21, 14, 0);
    private static final UUID USUARIO_ID              = UUID.randomUUID();

    private LocalId localId;
    private Producto pizza;     // $1000 c/u, mismo local
    private Producto empanada;  // $400 c/u, mismo local

    @BeforeEach
    void setUp() {
        localId  = LocalId.generate();
        pizza    = ProductoTestBuilder.conPrecio("1000").enLocal(localId).build();
        empanada = ProductoTestBuilder.conPrecio("400").enLocal(localId).build();
    }

    // ============================================================
    // Helpers de fixtures
    // ============================================================

    /** Crea un Pago de efectivo por el monto indicado. */
    private Pago pagoEfectivo(String monto) {
        return new Pago(MedioPago.EFECTIVO, new BigDecimal(monto), FECHA_CIERRE);
    }

    /** Crea un DescuentoManual PORCENTAJE. */
    private DescuentoManual descPorcentaje(String valor) {
        return new DescuentoManual(
            ModoDescuento.PORCENTAJE, new BigDecimal(valor),
            "Descuento test", USUARIO_ID, FECHA_APERTURA
        );
    }

    /** Crea un DescuentoManual MONTO_FIJO. */
    private DescuentoManual descMontoFijo(String valor) {
        return new DescuentoManual(
            ModoDescuento.MONTO_FIJO, new BigDecimal(valor),
            "Descuento test", USUARIO_ID, FECHA_APERTURA
        );
    }

    // ============================================================
    // 1. Construcción e invariantes base
    // ============================================================

    @Nested
    class Construccion {

        @Test
        void deberia_crear_pedido_con_estado_abierto() {
            // Given
            PedidoId   id     = PedidoId.generate();
            MesaId     mesaId = MesaId.generate();

            // When
            Pedido pedido = new Pedido(id, localId, mesaId, 1, EstadoPedido.ABIERTO, FECHA_APERTURA);

            // Then — campos inmutables asignados correctamente
            assertThat(pedido.getId()).isEqualTo(id);
            assertThat(pedido.getLocalId()).isEqualTo(localId);
            assertThat(pedido.getMesaId()).isEqualTo(mesaId);
            assertThat(pedido.getNumero()).isEqualTo(1);
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ABIERTO);
            assertThat(pedido.getFechaApertura()).isEqualTo(FECHA_APERTURA);
            assertThat(pedido.getFechaCierre()).isNull();
            assertThat(pedido.getItems()).isEmpty();
            assertThat(pedido.getPagos()).isEmpty();
            // Snapshot contable nulo mientras el pedido está abierto
            assertThat(pedido.getMontoTotalFinal()).isNull();
        }

        @Test
        void deberia_rechazar_numero_pedido_cero_o_negativo() {
            // Given
            MesaId mesaId = MesaId.generate();

            // When / Then — 0 y negativos violan la invariante
            assertThatThrownBy(() ->
                new Pedido(PedidoId.generate(), localId, mesaId, 0, EstadoPedido.ABIERTO, FECHA_APERTURA)
            ).isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() ->
                new Pedido(PedidoId.generate(), localId, mesaId, -5, EstadoPedido.ABIERTO, FECHA_APERTURA)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deberia_rechazar_campos_obligatorios_null() {
            // Given
            MesaId mesaId = MesaId.generate();

            // When / Then — cada campo obligatorio null produce NullPointerException
            assertThatThrownBy(() ->
                new Pedido(null, localId, mesaId, 1, EstadoPedido.ABIERTO, FECHA_APERTURA)
            ).isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() ->
                new Pedido(PedidoId.generate(), null, mesaId, 1, EstadoPedido.ABIERTO, FECHA_APERTURA)
            ).isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() ->
                new Pedido(PedidoId.generate(), localId, null, 1, EstadoPedido.ABIERTO, FECHA_APERTURA)
            ).isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() ->
                new Pedido(PedidoId.generate(), localId, mesaId, 1, null, FECHA_APERTURA)
            ).isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() ->
                new Pedido(PedidoId.generate(), localId, mesaId, 1, EstadoPedido.ABIERTO, null)
            ).isInstanceOf(NullPointerException.class);
        }
    }

    // ============================================================
    // 2. Gestión de ítems
    // ============================================================

    @Nested
    class GestionDeItems {

        @Test
        void deberia_agregar_producto_correctamente() {
            // Given
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();

            // When
            pedido.agregarProducto(pizza, 2, null);

            // Then — se creó 1 línea con el snapshot de precio correcto
            assertThat(pedido.getItems()).hasSize(1);
            ItemPedido item = pedido.getItems().get(0);
            assertThat(item.getCantidad()).isEqualTo(2);
            assertThat(item.getPrecioUnitario()).isEqualByComparingTo("1000");
            assertThat(item.getNombreProducto()).isEqualTo(pizza.getNombre());
        }

        @Test
        void deberia_rechazar_producto_de_otro_local() {
            // Given — pedido en localId, producto de un local diferente
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            Producto productoOtroLocal = ProductoTestBuilder.conPrecio("500")
                .enLocal(LocalId.generate()) // local distinto
                .build();

            // When / Then — AC5: aislamiento multi-tenant
            assertThatThrownBy(() ->
                pedido.agregarProducto(productoOtroLocal, 1, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("local");
        }

        @Test
        void deberia_fusionar_items_con_misma_configuracion() {
            // Given — primer ítem del mismo producto y observación
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza, 2, null);

            // Buscamos si el aggregate tiene ya un ítem con esa config (simula la lógica del UseCase)
            // en este caso el aggregate expone buscarItemConMismaConfiguracion para que el UseCase
            // decida fusionar. Simulamos la fusión directa actualizando la cantidad.
            var itemOpt = pedido.buscarItemConMismaConfiguracion(pizza.getId(), null, List.of());
            assertThat(itemOpt).isPresent();
            pedido.actualizarCantidadItem(itemOpt.get().getId(), 4); // fusión: 2 → 4

            // Then — sigue siendo 1 línea con la cantidad incrementada
            assertThat(pedido.getItems()).hasSize(1);
            assertThat(pedido.getItems().get(0).getCantidad()).isEqualTo(4);
        }

        @Test
        void deberia_crear_linea_separada_si_observacion_distinta() {
            // Given — mismo producto, observación diferente
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza, 1, null);
            pedido.agregarProducto(pizza, 1, "sin sal");

            // Then — el aggregate NO fusiona: líneas distintas por observación
            assertThat(pedido.getItems()).hasSize(2);
            assertThat(pedido.getItems())
                .extracting(ItemPedido::getObservacion)
                .containsExactlyInAnyOrder(null, "sin sal");
        }

        @Test
        void deberia_rechazar_agregar_a_pedido_cerrado() {
            // Given — pedido cerrado
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza, 1, null);
            pedido.cerrar(List.of(pagoEfectivo("1000")), FECHA_CIERRE);

            // When / Then — el aggregate protege la inmutabilidad post-cierre
            assertThatThrownBy(() ->
                pedido.agregarProducto(empanada, 1, null)
            ).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void deberia_limpiar_promociones_al_eliminar_item() {
            // Given — pedido con 2 ítems, el primero con una promoción aplicada
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza, 2, null);
            pedido.agregarProducto(empanada, 1, null);

            // Simulamos que el MotorReglasService aplicó una promo al primer ítem
            ItemPedido itemPizza = pedido.getItems().get(0);
            itemPizza.aplicarPromocion(new BigDecimal("200"), "Promo test", UUID.randomUUID());
            assertThat(itemPizza.tienePromocion()).isTrue();

            // When — eliminamos el segundo ítem; el aggregate debe limpiar promos para recálculo
            pedido.limpiarPromocionesItems();
            pedido.eliminarItem(pedido.getItems().get(1).getId());

            // Then — la promoción fue limpiada por limpiarPromocionesItems
            assertThat(itemPizza.tienePromocion()).isFalse();
            assertThat(pedido.getItems()).hasSize(1);
        }

        @Test
        void deberia_limpiar_promociones_al_modificar_cantidad() {
            // Given — ítem con promoción activa
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza, 3, null);

            ItemPedido item = pedido.getItems().get(0);
            item.aplicarPromocion(new BigDecimal("300"), "Promo vol", UUID.randomUUID());
            assertThat(item.tienePromocion()).isTrue();

            // When — actualizarCantidadItem debe limpiar la promoción del ítem modificado
            pedido.actualizarCantidadItem(item.getId(), 2);

            // Then — la promo se eliminó: pendiente de re-evaluación por el MotorReglasService
            assertThat(item.tienePromocion()).isFalse();
            assertThat(item.getCantidad()).isEqualTo(2);
        }
    }

    // ============================================================
    // 3. Cálculos financieros — La Regla de Oro
    // ============================================================

    @Nested
    class CalculosFinancieros {

        @Test
        void deberia_calcular_subtotal_items_sin_descuentos() {
            // Given — 2 pizzas ($1000 c/u) + 3 empanadas ($400 c/u)
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza,    2, null);
            pedido.agregarProducto(empanada, 3, null);

            // When
            BigDecimal subtotal = pedido.calcularSubtotalItems();

            // Then — (2 × $1000) + (3 × $400) = $2000 + $1200 = $3200
            assertThat(subtotal).isEqualByComparingTo("3200");
        }

        @Test
        void deberia_calcular_total_con_descuento_global_porcentaje() {
            // Given — 2 pizzas ($2000 base), descuento global 10%
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza, 2, null);
            pedido.aplicarDescuentoGlobal(descPorcentaje("10"));

            // When
            BigDecimal total = pedido.calcularTotal();

            // Then — base $2000, 10% = $200, total = $1800
            assertThat(total).isEqualByComparingTo("1800.00");
        }

        @Test
        void deberia_calcular_total_con_descuento_global_monto_fijo() {
            // Given — 2 pizzas ($2000 base), descuento global $300 monto fijo
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza, 2, null);
            pedido.aplicarDescuentoGlobal(descMontoFijo("300"));

            // When
            BigDecimal total = pedido.calcularTotal();

            // Then — $2000 - $300 = $1700
            assertThat(total).isEqualByComparingTo("1700");
        }

        @Test
        void deberia_calcular_total_con_descuento_item_y_global() {
            // Given — 2 pizzas, promo de $200 sobre el ítem + descuento global 10%
            // Jerarquía: 1° promo automática (ítem), 2° desc.item (manual), 3° desc.global
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza, 2, null);

            // Aplicar promoción automática al ítem (snapshot $200)
            ItemPedido item = pedido.getItems().get(0);
            item.aplicarPromocion(new BigDecimal("200"), "Promo Martes", UUID.randomUUID());

            // Aplicar descuento global del 10% sobre el resultado neto de ítems
            pedido.aplicarDescuentoGlobal(descPorcentaje("10"));

            // When
            BigDecimal total = pedido.calcularTotal();

            // Then:
            // precioFinalItem = (2 × $1000) - $200 promo = $1800
            // baseGravableGlobal = $1800
            // descuentoGlobal = 10% de $1800 = $180
            // total = $1800 - $180 = $1620
            assertThat(total).isEqualByComparingTo("1620.00");
        }
    }

    // ============================================================
    // 4. Transiciones de estado
    // ============================================================

    @Nested
    class TransicionesDeEstado {

        @Test
        void deberia_cerrar_pedido_con_pagos_exactos() {
            // Given — pedido con 1 pizza ($1000)
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza, 1, null);

            // When
            pedido.cerrar(List.of(pagoEfectivo("1000")), FECHA_CIERRE);

            // Then — transición de estado + snapshot contable congelado
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CERRADO);
            assertThat(pedido.getFechaCierre()).isEqualTo(FECHA_CIERRE);
            assertThat(pedido.getMontoTotalFinal()).isEqualByComparingTo("1000");
            assertThat(pedido.getMontoSubtotalFinal()).isEqualByComparingTo("1000");
            assertThat(pedido.getMontoDescuentosFinal()).isEqualByComparingTo("0");
            // Pagos registrados
            assertThat(pedido.getPagos()).hasSize(1);
            assertThat(pedido.getPagos()).extracting(Pago::getMonto)
                .containsExactly(new BigDecimal("1000"));
        }

        @Test
        void deberia_rechazar_cierre_si_pagos_no_coinciden_con_total() {
            // Given — pedido con 2 pizzas ($2000 total), pago solo $1500
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza, 2, null);

            // When / Then — invariante financiera crítica: suma pagos ≠ total → excepción
            assertThatThrownBy(() ->
                pedido.cerrar(List.of(pagoEfectivo("1500")), FECHA_CIERRE)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("1500")
             .hasMessageContaining("2000");
        }

        @Test
        void deberia_rechazar_cierre_sin_items() {
            // Given — pedido vacío (ningún producto agregado)
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();

            // When / Then — un pedido sin ítems no tiene sentido de negocio
            assertThatThrownBy(() ->
                pedido.cerrar(List.of(pagoEfectivo("100")), FECHA_CIERRE)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deberia_reabrir_pedido_cerrado() {
            // Given — pedido cerrado con 1 pizza
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza, 1, null);
            pedido.cerrar(List.of(pagoEfectivo("1000")), FECHA_CIERRE);
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CERRADO);

            // When — reapertura (HU-14: válvula de escape)
            pedido.reabrir(FECHA_CIERRE.plusMinutes(5));

            // Then — estado revertido y snapshot limpiado
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ABIERTO);
            assertThat(pedido.getFechaCierre()).isNull();
            assertThat(pedido.getMontoTotalFinal()).isNull();
            assertThat(pedido.getMontoSubtotalFinal()).isNull();
            assertThat(pedido.getMontoDescuentosFinal()).isNull();
            // Pagos eliminados (orphanRemoval simulado en dominio puro)
            assertThat(pedido.getPagos()).isEmpty();
            // Los ítems se conservan
            assertThat(pedido.getItems()).hasSize(1);
        }

        @Test
        void deberia_rechazar_reapertura_de_pedido_abierto() {
            // Given — pedido en estado ABIERTO
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();

            // When / Then — solo se pueden reabrir pedidos CERRADOS
            assertThatThrownBy(() ->
                pedido.reabrir(FECHA_CIERRE)
            ).isInstanceOf(IllegalStateException.class)
             .hasMessageContaining("CERRADO");
        }

        @Test
        void deberia_corregir_pedido_cerrado_in_place() {
            // Given — pedido cerrado con 2 pizzas ($2000) y 1 empanada ($400)
            Pedido pedido = PedidoTestBuilder.abierto().enLocal(localId).build();
            pedido.agregarProducto(pizza,    2, null);
            pedido.agregarProducto(empanada, 1, null);
            pedido.cerrar(List.of(pagoEfectivo("2400")), FECHA_CIERRE);

            ItemPedidoId idPizza    = pedido.getItems().get(0).getId();
            ItemPedidoId idEmpanada = pedido.getItems().get(1).getId();

            // When — corrección: pizza de 2 → 1, empanada eliminada (cantidad 0)
            // Nuevo total = 1 × $1000 = $1000
            Map<ItemPedidoId, Integer> correcciones = Map.of(
                idPizza,    1,
                idEmpanada, 0
            );
            pedido.corregir(correcciones, List.of(pagoEfectivo("1000")));

            // Then — el pedido permanece CERRADO con snapshot recalculado
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CERRADO);
            assertThat(pedido.getItems()).hasSize(1);
            assertThat(pedido.getItems().get(0).getCantidad()).isEqualTo(1);
            assertThat(pedido.getMontoTotalFinal()).isEqualByComparingTo("1000");
            assertThat(pedido.getMontoSubtotalFinal()).isEqualByComparingTo("1000");
            // Los pagos fueron reemplazados
            assertThat(pedido.getPagos()).hasSize(1);
            assertThat(pedido.getPagos().get(0).getMonto()).isEqualByComparingTo("1000");
        }
    }
}
