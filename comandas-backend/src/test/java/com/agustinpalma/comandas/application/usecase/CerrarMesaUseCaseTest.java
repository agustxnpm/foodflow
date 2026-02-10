package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CerrarMesaResponse;
import com.agustinpalma.comandas.application.dto.PagoRequest;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.DomainIds.ItemPedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.domain.service.MotorReglasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CerrarMesaUseCase.
 * Verifican las reglas de negocio del proceso de cierre de mesa.
 * 
 * Actualizados para la nueva API con soporte de pagos múltiples (split)
 * y snapshot contable.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CerrarMesaUseCase - Tests de comportamiento")
class CerrarMesaUseCaseTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PromocionRepository promocionRepository;

    @Mock
    private MotorReglasService motorReglasService;

    private Clock clock;

    private CerrarMesaUseCase useCase;

    private LocalId localIdValido;
    private MesaId mesaIdValida;
    private PedidoId pedidoIdValido;

    @BeforeEach
    void setUp() {
        // Clock fijo: 2026-02-06 19:00 Argentina
        clock = Clock.fixed(
            Instant.parse("2026-02-06T22:00:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires")
        );
        useCase = new CerrarMesaUseCase(mesaRepository, pedidoRepository, promocionRepository, motorReglasService, clock);
        
        localIdValido = new LocalId(UUID.randomUUID());
        mesaIdValida = new MesaId(UUID.randomUUID());
        pedidoIdValido = new PedidoId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Debe cerrar exitosamente una mesa con pedido abierto y con ítems")
    void deberia_cerrar_mesa_exitosamente() {
        // Given: Mesa ABIERTA con pedido ABIERTO que tiene ítems
        Mesa mesa = new Mesa(mesaIdValida, localIdValido, 5);
        mesa.abrir();

        Pedido pedido = crearPedidoConItems(pedidoIdValido, localIdValido, mesaIdValida);
        BigDecimal totalPedido = pedido.calcularTotal();
        
        List<PagoRequest> pagos = List.of(
            new PagoRequest(MedioPago.EFECTIVO, totalPedido)
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida, localIdValido)).thenReturn(Optional.of(pedido));
        when(promocionRepository.buscarActivasPorLocal(localIdValido)).thenReturn(Collections.emptyList());
        when(pedidoRepository.guardar(any(Pedido.class))).thenReturn(pedido);
        when(mesaRepository.guardar(any(Mesa.class))).thenReturn(mesa);

        // When: Se ejecuta el caso de uso
        CerrarMesaResponse response = useCase.ejecutar(localIdValido, mesaIdValida, pagos);

        // Then: La mesa y el pedido se cierran correctamente
        assertThat(response).isNotNull();
        assertThat(response.mesaEstado()).isEqualTo(EstadoMesa.LIBRE);
        assertThat(response.pedidoEstado()).isEqualTo(EstadoPedido.CERRADO);
        assertThat(response.pagos()).hasSize(1);
        assertThat(response.pagos().get(0).medio()).isEqualTo(MedioPago.EFECTIVO);
        assertThat(response.fechaCierre()).isNotNull();

        // Verificar que se llamaron los métodos de guardado
        verify(pedidoRepository).guardar(any(Pedido.class));
        verify(mesaRepository).guardar(any(Mesa.class));
    }

    @Test
    @DisplayName("Debe rechazar el cierre si la mesa no existe")
    void deberia_rechazar_cierre_si_mesa_no_existe() {
        // Given: La mesa no existe en el repositorio
        List<PagoRequest> pagos = List.of(
            new PagoRequest(MedioPago.TARJETA, new BigDecimal("1000"))
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.empty());

        // When/Then: Debe lanzar excepción
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, mesaIdValida, pagos))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("La mesa no existe");

        // Verificar que no se intentó guardar nada
        verify(pedidoRepository, never()).guardar(any());
        verify(mesaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe rechazar el cierre si la mesa no pertenece al local (multi-tenancy)")
    void deberia_rechazar_cierre_si_mesa_de_otro_local() {
        // Given: Mesa que pertenece a otro local
        LocalId otroLocalId = new LocalId(UUID.randomUUID());
        Mesa mesa = new Mesa(mesaIdValida, otroLocalId, 5);
        mesa.abrir();

        List<PagoRequest> pagos = List.of(
            new PagoRequest(MedioPago.EFECTIVO, new BigDecimal("1000"))
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));

        // When/Then: Debe lanzar excepción de validación de tenant
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, mesaIdValida, pagos))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("La mesa no pertenece a este local");

        verify(pedidoRepository, never()).guardar(any());
        verify(mesaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe rechazar el cierre si la mesa no tiene pedido abierto")
    void deberia_rechazar_cierre_si_no_hay_pedido_abierto() {
        // Given: Mesa ABIERTA pero sin pedido abierto asociado
        Mesa mesa = new Mesa(mesaIdValida, localIdValido, 5);
        mesa.abrir();

        List<PagoRequest> pagos = List.of(
            new PagoRequest(MedioPago.EFECTIVO, new BigDecimal("1000"))
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida, localIdValido)).thenReturn(Optional.empty());

        // When/Then: Debe lanzar excepción
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, mesaIdValida, pagos))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("La mesa no tiene un pedido abierto");

        verify(pedidoRepository, never()).guardar(any());
        verify(mesaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe rechazar el cierre si el pedido no tiene ítems")
    void deberia_rechazar_cierre_si_pedido_sin_items() {
        // Given: Mesa ABIERTA con pedido ABIERTO pero SIN ítems
        Mesa mesa = new Mesa(mesaIdValida, localIdValido, 5);
        mesa.abrir();

        Pedido pedidoSinItems = new Pedido(
            pedidoIdValido,
            localIdValido,
            mesaIdValida,
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );

        List<PagoRequest> pagos = List.of(
            new PagoRequest(MedioPago.EFECTIVO, new BigDecimal("1000"))
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida, localIdValido)).thenReturn(Optional.of(pedidoSinItems));
        when(promocionRepository.buscarActivasPorLocal(localIdValido)).thenReturn(Collections.emptyList());

        // When/Then: El dominio debe rechazar la finalización del pedido
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, mesaIdValida, pagos))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No se puede cerrar un pedido sin ítems");

        verify(pedidoRepository, never()).guardar(any());
        verify(mesaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe rechazar el cierre si la mesa ya está LIBRE")
    void deberia_rechazar_cierre_si_mesa_ya_libre() {
        // Given: Mesa LIBRE (no abierta)
        Mesa mesa = new Mesa(mesaIdValida, localIdValido, 5);
        // No llamamos a mesa.abrir(), queda en estado LIBRE

        Pedido pedido = crearPedidoConItems(pedidoIdValido, localIdValido, mesaIdValida);
        BigDecimal totalPedido = pedido.calcularTotal();

        List<PagoRequest> pagos = List.of(
            new PagoRequest(MedioPago.EFECTIVO, totalPedido)
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida, localIdValido)).thenReturn(Optional.of(pedido));
        when(promocionRepository.buscarActivasPorLocal(localIdValido)).thenReturn(Collections.emptyList());

        // When/Then: La entidad Mesa debe rechazar el cierre
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, mesaIdValida, pagos))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya está libre");

        verify(pedidoRepository, never()).guardar(any());
        verify(mesaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe rechazar el cierre si la lista de pagos es vacía")
    void deberia_rechazar_cierre_con_pagos_vacios() {
        // Given: Mesa y pedido válidos, pero lista de pagos vacía
        Mesa mesa = new Mesa(mesaIdValida, localIdValido, 5);
        mesa.abrir();

        Pedido pedido = crearPedidoConItems(pedidoIdValido, localIdValido, mesaIdValida);

        List<PagoRequest> pagos = List.of();

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida, localIdValido)).thenReturn(Optional.of(pedido));
        when(promocionRepository.buscarActivasPorLocal(localIdValido)).thenReturn(Collections.emptyList());

        // When/Then: El dominio rechaza pagos vacíos
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, mesaIdValida, pagos))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("al menos un pago");

        verify(pedidoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe registrar el timestamp de cierre con el clock inyectado")
    void deberia_registrar_timestamp_cierre_con_clock() {
        // Given: Mesa y pedido válidos
        Mesa mesa = new Mesa(mesaIdValida, localIdValido, 5);
        mesa.abrir();

        Pedido pedido = crearPedidoConItems(pedidoIdValido, localIdValido, mesaIdValida);
        BigDecimal totalPedido = pedido.calcularTotal();

        List<PagoRequest> pagos = List.of(
            new PagoRequest(MedioPago.TRANSFERENCIA, totalPedido)
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida, localIdValido)).thenReturn(Optional.of(pedido));
        when(promocionRepository.buscarActivasPorLocal(localIdValido)).thenReturn(Collections.emptyList());
        when(pedidoRepository.guardar(any(Pedido.class))).thenReturn(pedido);
        when(mesaRepository.guardar(any(Mesa.class))).thenReturn(mesa);

        // When: Se cierra la mesa
        CerrarMesaResponse response = useCase.ejecutar(localIdValido, mesaIdValida, pagos);

        // Then: La fecha de cierre usa el clock fijo
        assertThat(response.fechaCierre()).isNotNull();
        // El clock está fijo en 2026-02-06 19:00 Argentina
        assertThat(response.fechaCierre().toLocalDate())
            .isEqualTo(java.time.LocalDate.of(2026, 2, 6));
    }

    // --- Helpers ---

    /**
     * Crea un pedido válido con ítems para testing.
     * Usa reflexión para agregar un ItemPedido real a la lista privada del pedido.
     * El ítem tiene precio unitario $1000 x cantidad 1 = total $1000.
     */
    private Pedido crearPedidoConItems(PedidoId id, LocalId localId, MesaId mesaId) {
        Pedido pedido = new Pedido(id, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        
        // Crear un ItemPedido real con precio $1000
        ItemPedido item = new ItemPedido(
            ItemPedidoId.generate(),
            id,
            ProductoId.generate(),
            "Producto de prueba",
            1,
            new BigDecimal("1000"),
            null
        );
        
        try {
            var itemsField = Pedido.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var items = (java.util.List<ItemPedido>) itemsField.get(pedido);
            items.add(item);
        } catch (Exception e) {
            throw new RuntimeException("Error preparando pedido de prueba", e);
        }
        
        return pedido;
    }
}
