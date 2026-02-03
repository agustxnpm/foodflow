package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CerrarMesaRequest;
import com.agustinpalma.comandas.application.dto.CerrarMesaResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para CerrarMesaUseCase.
 * Verifican las reglas de negocio del proceso de cierre de mesa.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CerrarMesaUseCase - Tests de comportamiento")
class CerrarMesaUseCaseTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    private CerrarMesaUseCase useCase;

    private LocalId localIdValido;
    private MesaId mesaIdValida;
    private PedidoId pedidoIdValido;

    @BeforeEach
    void setUp() {
        useCase = new CerrarMesaUseCase(mesaRepository, pedidoRepository);
        
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
        
        CerrarMesaRequest request = new CerrarMesaRequest(
            mesaIdValida.getValue().toString(),
            MedioPago.EFECTIVO
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.guardar(any(Pedido.class))).thenReturn(pedido);
        when(mesaRepository.guardar(any(Mesa.class))).thenReturn(mesa);

        // When: Se ejecuta el caso de uso
        CerrarMesaResponse response = useCase.ejecutar(localIdValido, request);

        // Then: La mesa y el pedido se cierran correctamente
        assertThat(response).isNotNull();
        assertThat(response.mesaEstado()).isEqualTo(EstadoMesa.LIBRE);
        assertThat(response.pedidoEstado()).isEqualTo(EstadoPedido.CERRADO);
        assertThat(response.medioPago()).isEqualTo(MedioPago.EFECTIVO);
        assertThat(response.fechaCierre()).isNotNull();

        // Verificar que se llamaron los métodos de guardado
        verify(pedidoRepository).guardar(any(Pedido.class));
        verify(mesaRepository).guardar(any(Mesa.class));
    }

    @Test
    @DisplayName("Debe rechazar el cierre si la mesa no existe")
    void deberia_rechazar_cierre_si_mesa_no_existe() {
        // Given: La mesa no existe en el repositorio
        CerrarMesaRequest request = new CerrarMesaRequest(
            mesaIdValida.getValue().toString(),
            MedioPago.TARJETA
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.empty());

        // When/Then: Debe lanzar excepción
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, request))
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

        CerrarMesaRequest request = new CerrarMesaRequest(
            mesaIdValida.getValue().toString(),
            MedioPago.EFECTIVO
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));

        // When/Then: Debe lanzar excepción de validación de tenant
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, request))
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

        CerrarMesaRequest request = new CerrarMesaRequest(
            mesaIdValida.getValue().toString(),
            MedioPago.EFECTIVO
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida)).thenReturn(Optional.empty());

        // When/Then: Debe lanzar excepción
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, request))
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

        CerrarMesaRequest request = new CerrarMesaRequest(
            mesaIdValida.getValue().toString(),
            MedioPago.EFECTIVO
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida)).thenReturn(Optional.of(pedidoSinItems));

        // When/Then: El dominio debe rechazar la finalización del pedido
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, request))
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

        CerrarMesaRequest request = new CerrarMesaRequest(
            mesaIdValida.getValue().toString(),
            MedioPago.EFECTIVO
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida)).thenReturn(Optional.of(pedido));

        // When/Then: La entidad Mesa debe rechazar el cierre
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya está libre");

        verify(pedidoRepository, never()).guardar(any());
        verify(mesaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe rechazar el cierre si no se proporciona medio de pago")
    void deberia_rechazar_cierre_sin_medio_pago() {
        // Given: Request sin medio de pago
        CerrarMesaRequest request = new CerrarMesaRequest(
            mesaIdValida.getValue().toString(),
            null  // Sin medio de pago
        );

        // When/Then: Debe lanzar excepción de validación
        assertThatThrownBy(() -> useCase.ejecutar(localIdValido, request))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("El medio de pago es obligatorio");

        verify(mesaRepository, never()).buscarPorId(any());
        verify(pedidoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe registrar correctamente el medio de pago en el pedido")
    void deberia_registrar_medio_pago_en_pedido() {
        // Given: Mesa y pedido válidos
        Mesa mesa = new Mesa(mesaIdValida, localIdValido, 5);
        mesa.abrir();

        Pedido pedido = crearPedidoConItems(pedidoIdValido, localIdValido, mesaIdValida);

        CerrarMesaRequest request = new CerrarMesaRequest(
            mesaIdValida.getValue().toString(),
            MedioPago.TRANSFERENCIA
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.guardar(any(Pedido.class))).thenReturn(pedido);
        when(mesaRepository.guardar(any(Mesa.class))).thenReturn(mesa);

        // When: Se cierra la mesa
        CerrarMesaResponse response = useCase.ejecutar(localIdValido, request);

        // Then: El medio de pago debe ser el especificado
        assertThat(response.medioPago()).isEqualTo(MedioPago.TRANSFERENCIA);
        
        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).guardar(pedidoCaptor.capture());
        
        Pedido pedidoGuardado = pedidoCaptor.getValue();
        assertThat(pedidoGuardado.getMedioPago()).isEqualTo(MedioPago.TRANSFERENCIA);
        assertThat(pedidoGuardado.getFechaCierre()).isNotNull();
    }

    @Test
    @DisplayName("Debe registrar el timestamp de cierre automáticamente")
    void deberia_registrar_timestamp_cierre() {
        // Given: Mesa y pedido válidos
        Mesa mesa = new Mesa(mesaIdValida, localIdValido, 5);
        mesa.abrir();

        Pedido pedido = crearPedidoConItems(pedidoIdValido, localIdValido, mesaIdValida);

        CerrarMesaRequest request = new CerrarMesaRequest(
            mesaIdValida.getValue().toString(),
            MedioPago.EFECTIVO
        );

        when(mesaRepository.buscarPorId(mesaIdValida)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaIdValida)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.guardar(any(Pedido.class))).thenReturn(pedido);
        when(mesaRepository.guardar(any(Mesa.class))).thenReturn(mesa);

        LocalDateTime antes = LocalDateTime.now();

        // When: Se cierra la mesa
        CerrarMesaResponse response = useCase.ejecutar(localIdValido, request);

        LocalDateTime despues = LocalDateTime.now();

        // Then: La fecha de cierre debe estar entre antes y después
        assertThat(response.fechaCierre())
            .isNotNull()
            .isAfterOrEqualTo(antes)
            .isBeforeOrEqualTo(despues);
    }

    // --- Helpers ---

    /**
     * Crea un pedido válido con ítems para testing.
     * Usamos reflexión para agregar items a la lista privada del pedido,
     * ya que no existe un método público agregarItem() aún.
     */
    private Pedido crearPedidoConItems(PedidoId id, LocalId localId, MesaId mesaId) {
        Pedido pedido = new Pedido(id, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        

        // TODO: Agregar ítems reales cuando exista el método agregarItem() en Pedido

        // Simulamos que el pedido tiene ítems usando reflexión
        // En un escenario real, usarías el método agregarItem() del dominio
        try {
            var itemsField = Pedido.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var items = (java.util.List<Object>) itemsField.get(pedido);
            // Agregamos un objeto mock para simular que hay ítems
            items.add(new Object()); // Placeholder - en producción sería ItemPedido real
        } catch (Exception e) {
            throw new RuntimeException("Error preparando pedido de prueba", e);
        }
        
        return pedido;
    }
}
