package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.DetallePedidoResponse;
import com.agustinpalma.comandas.application.dto.ItemDetalleDTO;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Test unitario del caso de uso ConsultarDetallePedidoUseCase.
 * Sin Spring, sin base de datos, solo lógica pura con mocks.
 * 
 * Valida el comportamiento esperado según los criterios de aceptación de la HU-06.
 * 
 * Escenarios probados:
 * - Consulta exitosa con cálculos correctos
 * - Mesa inexistente
 * - Mesa de otro local (seguridad)
 * - Mesa en estado LIBRE (sin pedido activo)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Consultar Detalle Pedido - Caso de Uso (HU-06)")
class ConsultarDetallePedidoUseCaseTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    private ConsultarDetallePedidoUseCase useCase;

    private LocalId localId;
    private MesaId mesaId;
    private Mesa mesaAbierta;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarDetallePedidoUseCase(mesaRepository, pedidoRepository);
        
        // Datos de prueba comunes
        localId = new LocalId(UUID.randomUUID());
        mesaId = new MesaId(UUID.randomUUID());
        
        // Mesa ABIERTA del local
        mesaAbierta = new Mesa(mesaId, localId, 10);
        mesaAbierta.abrir();
        
        // Pedido abierto con ítems
        pedido = new Pedido(
            new PedidoId(UUID.randomUUID()),
            localId,
            mesaId,
            42,
            EstadoPedido.ABIERTO,
            LocalDateTime.now().minusHours(1)
        );
    }

    @Test
    @DisplayName("Debería retornar detalle del pedido con cálculos correctos")
    void deberia_retornar_detalle_pedido_con_calculos_correctos() {
        // Given: Un pedido con 2 ítems
        ItemPedido item1 = new ItemPedido(
            new ItemPedidoId(UUID.randomUUID()),
            pedido.getId(),
            new ProductoId(UUID.randomUUID()),
            "Pizza Napolitana",
            2,  // cantidad
            new BigDecimal("350.00"),  // precio unitario
            "Sin aceitunas"
        );
        
        ItemPedido item2 = new ItemPedido(
            new ItemPedidoId(UUID.randomUUID()),
            pedido.getId(),
            new ProductoId(UUID.randomUUID()),
            "Coca Cola 1.5L",
            3,  // cantidad
            new BigDecimal("120.50"),  // precio unitario
            null
        );
        
        pedido.agregarItem(item1);
        pedido.agregarItem(item2);
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaAbierta));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaId, localId)).thenReturn(Optional.of(pedido));

        // When: Se consulta el detalle
        DetallePedidoResponse response = useCase.ejecutar(localId, mesaId);

        // Then: AC1 - Se retornan todos los ítems con su información completa
        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(2);
        
        // Verifica primer ítem
        ItemDetalleDTO itemDTO1 = response.items().get(0);
        assertThat(itemDTO1.nombreProducto()).isEqualTo("Pizza Napolitana");
        assertThat(itemDTO1.cantidad()).isEqualTo(2);
        assertThat(itemDTO1.precioUnitarioBase()).isEqualByComparingTo("350.00");
        assertThat(itemDTO1.subtotal()).isEqualByComparingTo("700.00");  // 2 * 350
        assertThat(itemDTO1.observacion()).isEqualTo("Sin aceitunas");
        
        // Verifica segundo ítem
        ItemDetalleDTO itemDTO2 = response.items().get(1);
        assertThat(itemDTO2.nombreProducto()).isEqualTo("Coca Cola 1.5L");
        assertThat(itemDTO2.cantidad()).isEqualTo(3);
        assertThat(itemDTO2.precioUnitarioBase()).isEqualByComparingTo("120.50");
        assertThat(itemDTO2.subtotal()).isEqualByComparingTo("361.50");  // 3 * 120.50
        assertThat(itemDTO2.observacion()).isNull();
        
        // Then: AC2 - Cálculo del total parcial correcto
        // Total esperado: 700 + 361.50 = 1061.50
        assertThat(response.totalParcial()).isEqualByComparingTo("1061.50");
        
        // Then: AC3 - Información de contexto presente
        assertThat(response.pedidoId()).isEqualTo(pedido.getId().getValue().toString());
        assertThat(response.numeroPedido()).isEqualTo(42);
        assertThat(response.numeroMesa()).isEqualTo(10);
        assertThat(response.estado()).isEqualTo("ABIERTO");
        assertThat(response.fechaApertura()).isNotNull();

        // Verifica interacciones
        verify(mesaRepository, times(1)).buscarPorId(mesaId);
        verify(pedidoRepository, times(1)).buscarAbiertoPorMesa(mesaId, localId);
    }

    @Test
    @DisplayName("Debería fallar si la mesa no existe")
    void deberia_fallar_si_mesa_no_existe() {
        // Given: Mesa inexistente
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.empty());

        // When/Then: Lanza excepción
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesaId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("La mesa no existe");

        // No debe consultar el pedido
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    @DisplayName("Debería fallar si la mesa no pertenece al local del usuario (AC5 - Multi-tenancy)")
    void deberia_fallar_si_mesa_no_pertenece_al_local() {
        // Given: Mesa que pertenece a otro local
        LocalId otroLocalId = new LocalId(UUID.randomUUID());
        Mesa mesaOtroLocal = new Mesa(mesaId, otroLocalId, 5);
        mesaOtroLocal.abrir();
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaOtroLocal));

        // When/Then: Lanza excepción de seguridad
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesaId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pertenece a este local");

        // No debe consultar el pedido
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    @DisplayName("Debería fallar si la mesa está LIBRE (AC4 - Sin pedido activo)")
    void deberia_fallar_si_mesa_esta_libre() {
        // Given: Mesa en estado LIBRE
        Mesa mesaLibre = new Mesa(mesaId, localId, 8);
        // La mesa se crea en estado LIBRE por defecto
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaLibre));

        // When/Then: Lanza excepción informativa
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesaId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no tiene un pedido activo");

        // No debe consultar el pedido
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    @DisplayName("Debería retornar lista vacía si el pedido no tiene ítems (edge case)")
    void deberia_retornar_lista_vacia_si_pedido_sin_items() {
        // Given: Pedido sin ítems (recién abierto)
        // El pedido ya está creado en setUp() sin ítems
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaAbierta));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaId, localId)).thenReturn(Optional.of(pedido));

        // When: Se consulta el detalle
        DetallePedidoResponse response = useCase.ejecutar(localId, mesaId);

        // Then: Retorna pedido con lista vacía y total en 0
        assertThat(response).isNotNull();
        assertThat(response.items()).isEmpty();
        assertThat(response.totalParcial()).isEqualByComparingTo("0.00");
        assertThat(response.numeroPedido()).isEqualTo(42);
    }

    @Test
    @DisplayName("Debería manejar correctamente observaciones nulas")
    void deberia_manejar_observaciones_nulas() {
        // Given: Pedido con ítem sin observaciones
        ItemPedido itemSinObservaciones = new ItemPedido(
            new ItemPedidoId(UUID.randomUUID()),
            pedido.getId(),
            new ProductoId(UUID.randomUUID()),
            "Empanadas de Carne",
            6,
            new BigDecimal("45.00"),
            null  // Sin observaciones
        );
        
        pedido.agregarItem(itemSinObservaciones);
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaAbierta));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaId, localId)).thenReturn(Optional.of(pedido));

        // When: Se consulta el detalle
        DetallePedidoResponse response = useCase.ejecutar(localId, mesaId);

        // Then: La observación del ítem es null
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).observacion()).isNull();
    }
}
