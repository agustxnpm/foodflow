package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CorreccionPedidoRequest;
import com.agustinpalma.comandas.application.dto.CorreccionPedidoRequest.ItemCorreccion;
import com.agustinpalma.comandas.application.dto.CorreccionPedidoRequest.PagoCorreccion;
import com.agustinpalma.comandas.application.dto.DetallePedidoCerradoResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests del caso de uso CorregirPedidoCerradoUseCase.
 *
 * Escenarios:
 * - Happy path: corrección de cantidad e ítems en un pedido CERRADO → persiste.
 * - Guard estado: pedido ABIERTO rechaza la corrección (invariante del aggregate).
 * - Guard financiero: pagos no coinciden con el nuevo total → IllegalArgumentException.
 *
 * La lógica de validación (estado, suma de pagos) reside en el dominio (Pedido.corregir()).
 * El use case solo orquesta: recupera, valida tenant, delega, persiste.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 * Sin Spring. Sin base de datos.
 */
@ExtendWith(MockitoExtension.class)
class CorregirPedidoCerradoUseCaseTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private MesaRepository   mesaRepository;

    private CorregirPedidoCerradoUseCase useCase;

    private LocalId localId;
    private Producto pizza;       // $1000 c/u

    private static final LocalDateTime FECHA_APERTURA = LocalDateTime.of(2026, 4, 21,  8,  0);
    private static final LocalDateTime FECHA_CIERRE   = LocalDateTime.of(2026, 4, 21, 14,  0);

    @BeforeEach
    void setUp() {
        localId = LocalId.generate();
        pizza = new Producto(ProductoId.generate(), localId, "Pizza", new BigDecimal("1000"), true, "#FF0000");

        Clock clock = Clock.fixed(
            FECHA_CIERRE.toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
        );
        useCase = new CorregirPedidoCerradoUseCase(pedidoRepository, mesaRepository, clock);
    }

    // ============================================================
    // Helper: crea un pedido CERRADO con 2 pizzas ($2000 total)
    // ============================================================

    /**
     * Construye un pedido cerrado con 2 unidades de pizza y pago exacto de $2000.
     * El mesaId queda expuesto para configurar el mock de MesaRepository.
     */
    private Pedido pedidoCerradoDos() {
        Pedido pedido = new Pedido(
            PedidoId.generate(), localId, MesaId.generate(),
            1, EstadoPedido.ABIERTO, FECHA_APERTURA
        );
        pedido.agregarProducto(pizza, 2, null);
        pedido.cerrar(
            List.of(new Pago(MedioPago.EFECTIVO, new BigDecimal("2000"), FECHA_CIERRE)),
            FECHA_CIERRE
        );
        return pedido;
    }

    // ============================================================
    // 1. Happy path
    // ============================================================

    @Nested
    class CorreccionExitosa {

        @Test
        void deberia_corregir_cantidades_y_pagos() {
            // Given — pedido cerrado con 2 pizzas ($2000)
            Pedido pedido = pedidoCerradoDos();
            PedidoId pedidoId = pedido.getId();
            ItemPedidoId itemId = pedido.getItems().get(0).getId();

            // Mesa requerida para construir el DTO de respuesta
            Mesa mesa = new Mesa(pedido.getMesaId(), localId, 5);
            given(pedidoRepository.buscarPorId(pedidoId)).willReturn(Optional.of(pedido));
            given(mesaRepository.buscarPorId(pedido.getMesaId())).willReturn(Optional.of(mesa));

            // Corrección: bajar de 2 a 1 pizza → nuevo total $1000
            CorreccionPedidoRequest request = new CorreccionPedidoRequest(
                List.of(new ItemCorreccion(itemId.getValue().toString(), 1)),
                List.of(new PagoCorreccion(MedioPago.EFECTIVO, new BigDecimal("1000")))
            );

            // When
            DetallePedidoCerradoResponse response = useCase.ejecutar(localId, pedidoId, request);

            // Then — el pedido fue corregido y persistido
            assertThat(response).isNotNull();
            assertThat(response.montoTotal()).isEqualByComparingTo("1000");
            assertThat(pedido.getItems()).hasSize(1);
            assertThat(pedido.getItems().get(0).getCantidad()).isEqualTo(1);
            then(pedidoRepository).should(times(1)).guardar(pedido);
        }
    }

    // ============================================================
    // 2. Guard: estado del pedido
    // ============================================================

    @Nested
    class GuardEstado {

        @Test
        void deberia_rechazar_si_pedido_no_esta_cerrado() {
            // Given — pedido ABIERTO (no cerrado)
            Pedido pedidoAbierto = new Pedido(
                PedidoId.generate(), localId, MesaId.generate(),
                1, EstadoPedido.ABIERTO, FECHA_APERTURA
            );
            pedidoAbierto.agregarProducto(pizza, 1, null);
            // NO se llama a cerrar()

            ItemPedidoId itemId = pedidoAbierto.getItems().get(0).getId();
            given(pedidoRepository.buscarPorId(pedidoAbierto.getId()))
                .willReturn(Optional.of(pedidoAbierto));

            CorreccionPedidoRequest request = new CorreccionPedidoRequest(
                List.of(new ItemCorreccion(itemId.getValue().toString(), 1)),
                List.of(new PagoCorreccion(MedioPago.EFECTIVO, new BigDecimal("1000")))
            );

            // When / Then — Pedido.corregir() exige estado CERRADO
            assertThatThrownBy(() ->
                useCase.ejecutar(localId, pedidoAbierto.getId(), request)
            ).isInstanceOf(IllegalStateException.class);

            then(pedidoRepository).should(never()).guardar(any());
        }
    }

    // ============================================================
    // 3. Guard: suma de pagos no coincide con el nuevo total
    // ============================================================

    @Nested
    class GuardPagos {

        @Test
        void deberia_rechazar_si_pagos_no_coinciden_con_nuevo_total() {
            // Given — pedido cerrado con 2 pizzas ($2000)
            Pedido pedido = pedidoCerradoDos();
            ItemPedidoId itemId = pedido.getItems().get(0).getId();
            given(pedidoRepository.buscarPorId(pedido.getId())).willReturn(Optional.of(pedido));

            // Corrección: 1 pizza → nuevo total $1000, pero pago declarado es $1500
            CorreccionPedidoRequest request = new CorreccionPedidoRequest(
                List.of(new ItemCorreccion(itemId.getValue().toString(), 1)),
                List.of(new PagoCorreccion(MedioPago.EFECTIVO, new BigDecimal("1500")))
            );

            // When / Then — Pedido.corregir() valida que Σpagos == total
            assertThatThrownBy(() ->
                useCase.ejecutar(localId, pedido.getId(), request)
            ).isInstanceOf(IllegalArgumentException.class);

            then(pedidoRepository).should(never()).guardar(any());
        }
    }

    // ============================================================
    // 4. Guard: multi-tenancy (pedido de otro local)
    // ============================================================

    @Nested
    class GuardMultiTenancy {

        @Test
        void deberia_rechazar_si_pedido_no_pertenece_al_local() {
            // Given — pedido cerrado pertenece a OTRO local distinto del solicitante
            LocalId otroLocalId = LocalId.generate();
            Producto pizzaOtroLocal = new Producto(
                ProductoId.generate(), otroLocalId, "Pizza",
                new BigDecimal("1000"), true, "#FF0000"
            );
            Pedido pedidoAjeno = new Pedido(
                PedidoId.generate(), otroLocalId, MesaId.generate(),
                1, EstadoPedido.ABIERTO, FECHA_APERTURA
            );
            pedidoAjeno.agregarProducto(pizzaOtroLocal, 1, null);
            pedidoAjeno.cerrar(
                List.of(new Pago(MedioPago.EFECTIVO, new BigDecimal("1000"), FECHA_CIERRE)),
                FECHA_CIERRE
            );
            ItemPedidoId itemId = pedidoAjeno.getItems().get(0).getId();
            given(pedidoRepository.buscarPorId(pedidoAjeno.getId()))
                .willReturn(Optional.of(pedidoAjeno));

            CorreccionPedidoRequest request = new CorreccionPedidoRequest(
                List.of(new ItemCorreccion(itemId.getValue().toString(), 1)),
                List.of(new PagoCorreccion(MedioPago.EFECTIVO, new BigDecimal("1000")))
            );

            // When / Then — el use case bloquea acceso cross-tenant
            assertThatThrownBy(() ->
                useCase.ejecutar(localId, pedidoAjeno.getId(), request)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("no pertenece");

            // No se persistió ni se mutó el pedido ajeno
            then(pedidoRepository).should(never()).guardar(any());
        }
    }

    // ============================================================
    // 5. Recálculo del snapshot contable post-corrección
    // ============================================================

    @Nested
    class RecalculoSnapshot {

        @Test
        void deberia_recalcular_snapshot_contable() {
            // Given — pedido cerrado con 2 pizzas ($2000), snapshot inicial = $2000
            Pedido pedido = pedidoCerradoDos();
            assertThat(pedido.getMontoTotalFinal()).isEqualByComparingTo("2000");
            assertThat(pedido.getMontoSubtotalFinal()).isEqualByComparingTo("2000");

            PedidoId pedidoId = pedido.getId();
            ItemPedidoId itemId = pedido.getItems().get(0).getId();
            Mesa mesa = new Mesa(pedido.getMesaId(), localId, 5);
            given(pedidoRepository.buscarPorId(pedidoId)).willReturn(Optional.of(pedido));
            given(mesaRepository.buscarPorId(pedido.getMesaId())).willReturn(Optional.of(mesa));

            // Corrección: bajar de 2 a 1 pizza → nuevo total $1000
            CorreccionPedidoRequest request = new CorreccionPedidoRequest(
                List.of(new ItemCorreccion(itemId.getValue().toString(), 1)),
                List.of(new PagoCorreccion(MedioPago.EFECTIVO, new BigDecimal("1000")))
            );

            // When
            DetallePedidoCerradoResponse response = useCase.ejecutar(localId, pedidoId, request);

            // Then — el snapshot contable refleja el nuevo total tras la corrección
            assertThat(pedido.getMontoTotalFinal()).isEqualByComparingTo("1000");
            assertThat(pedido.getMontoSubtotalFinal()).isEqualByComparingTo("1000");
            assertThat(pedido.getMontoDescuentosFinal()).isEqualByComparingTo("0");
            // Σpagos == nuevo total → invariante contable preservado
            BigDecimal sumaPagos = pedido.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(sumaPagos).isEqualByComparingTo(pedido.getMontoTotalFinal());
            // Y el DTO de respuesta refleja el cambio
            assertThat(response.montoTotal()).isEqualByComparingTo("1000");
            then(pedidoRepository).should(times(1)).guardar(pedido);
        }
    }
}
