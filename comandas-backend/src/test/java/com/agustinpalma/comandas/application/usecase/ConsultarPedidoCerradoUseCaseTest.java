package com.agustinpalma.comandas.application.usecase;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests del caso de uso ConsultarPedidoCerradoUseCase (Fase 6 — solo lectura).
 *
 * Escenarios:
 * - Happy path: pedido CERRADO del local → retorna DTO completo.
 * - Multi-tenancy: pedido de otro local → IllegalArgumentException.
 * - Guard estado: pedido ABIERTO → IllegalStateException (solo CERRADO se consulta acá).
 * - Pedido inexistente → IllegalStateException.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ConsultarPedidoCerradoUseCaseTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private MesaRepository   mesaRepository;

    private ConsultarPedidoCerradoUseCase useCase;
    private LocalId localId;
    private Producto pizza;

    private static final LocalDateTime FECHA_APERTURA = LocalDateTime.of(2026, 4, 21,  8, 0);
    private static final LocalDateTime FECHA_CIERRE   = LocalDateTime.of(2026, 4, 21, 14, 0);

    @BeforeEach
    void setUp() {
        useCase = new ConsultarPedidoCerradoUseCase(pedidoRepository, mesaRepository);
        localId = LocalId.generate();
        pizza = new Producto(
            ProductoId.generate(), localId, "Pizza",
            new BigDecimal("1000"), true, "#FF0000"
        );
    }

    /** Crea un pedido cerrado de 1 pizza ($1000) en el localId del test. */
    private Pedido pedidoCerrado() {
        Pedido pedido = new Pedido(
            PedidoId.generate(), localId, MesaId.generate(),
            1, EstadoPedido.ABIERTO, FECHA_APERTURA
        );
        pedido.agregarProducto(pizza, 1, null);
        pedido.cerrar(
            List.of(new Pago(MedioPago.EFECTIVO, new BigDecimal("1000"), FECHA_CIERRE)),
            FECHA_CIERRE
        );
        return pedido;
    }

    @Test
    void deberia_retornar_detalle_de_pedido_cerrado() {
        // Given
        Pedido pedido = pedidoCerrado();
        Mesa mesa = new Mesa(pedido.getMesaId(), localId, 5);
        given(pedidoRepository.buscarPorId(pedido.getId())).willReturn(Optional.of(pedido));
        given(mesaRepository.buscarPorId(pedido.getMesaId())).willReturn(Optional.of(mesa));

        // When
        DetallePedidoCerradoResponse response = useCase.ejecutar(localId, pedido.getId());

        // Then — el DTO refleja el snapshot contable del pedido cerrado
        assertThat(response).isNotNull();
        assertThat(response.montoTotal()).isEqualByComparingTo("1000");
    }

    @Test
    void deberia_rechazar_si_pedido_no_pertenece_al_local() {
        // Given — pedido cerrado de OTRO local
        LocalId otroLocalId = LocalId.generate();
        Producto pizzaAjena = new Producto(
            ProductoId.generate(), otroLocalId, "Pizza",
            new BigDecimal("1000"), true, "#FF0000"
        );
        Pedido pedidoAjeno = new Pedido(
            PedidoId.generate(), otroLocalId, MesaId.generate(),
            1, EstadoPedido.ABIERTO, FECHA_APERTURA
        );
        pedidoAjeno.agregarProducto(pizzaAjena, 1, null);
        pedidoAjeno.cerrar(
            List.of(new Pago(MedioPago.EFECTIVO, new BigDecimal("1000"), FECHA_CIERRE)),
            FECHA_CIERRE
        );

        given(pedidoRepository.buscarPorId(pedidoAjeno.getId()))
            .willReturn(Optional.of(pedidoAjeno));

        // When / Then — el use case bloquea acceso cross-tenant
        assertThatThrownBy(() -> useCase.ejecutar(localId, pedidoAjeno.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pertenece");

        then(mesaRepository).shouldHaveNoInteractions();
    }

    @Test
    void deberia_rechazar_si_pedido_no_esta_cerrado() {
        // Given — pedido ABIERTO (no cerrado todavía)
        Pedido pedidoAbierto = new Pedido(
            PedidoId.generate(), localId, MesaId.generate(),
            1, EstadoPedido.ABIERTO, FECHA_APERTURA
        );
        pedidoAbierto.agregarProducto(pizza, 1, null);

        given(pedidoRepository.buscarPorId(pedidoAbierto.getId()))
            .willReturn(Optional.of(pedidoAbierto));

        // When / Then — solo se consulta detalle de pedidos cerrados
        assertThatThrownBy(() -> useCase.ejecutar(localId, pedidoAbierto.getId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cerrados")
            .hasMessageContaining("ABIERTO");
    }

    @Test
    void deberia_rechazar_si_pedido_no_existe() {
        // Given — el repositorio no encuentra el pedido
        PedidoId inexistente = PedidoId.generate();
        given(pedidoRepository.buscarPorId(inexistente)).willReturn(Optional.empty());

        // When / Then — el use case propaga la inexistencia
        assertThatThrownBy(() -> useCase.ejecutar(localId, inexistente))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no existe");
    }
}
