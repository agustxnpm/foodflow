package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AbrirMesaRequest;
import com.agustinpalma.comandas.application.dto.AbrirMesaResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
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
 * Test unitario del caso de uso AbrirMesaUseCase.
 * Sin Spring, sin base de datos, solo lógica pura con mocks.
 * Valida el comportamiento esperado según los criterios de aceptación de la HU.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Abrir Mesa - Caso de Uso")
class AbrirMesaUseCaseTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    private AbrirMesaUseCase useCase;

    private LocalId localId;
    private MesaId mesaId;
    private Mesa mesaLibre;

    @BeforeEach
    void setUp() {
        useCase = new AbrirMesaUseCase(mesaRepository, pedidoRepository);
        
        // Datos de prueba comunes
        localId = new LocalId(UUID.randomUUID());
        mesaId = new MesaId(UUID.randomUUID());
        mesaLibre = new Mesa(mesaId, localId, 5);
    }

    @Test
    @DisplayName("Debería abrir mesa libre y crear pedido exitosamente")
    void deberia_abrir_mesa_libre_y_crear_pedido_exitosamente() {
        // Given: Una mesa LIBRE del local
        AbrirMesaRequest request = new AbrirMesaRequest(mesaId.getValue().toString());
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaLibre));
        when(pedidoRepository.buscarPorMesaYEstado(mesaId, EstadoPedido.ABIERTO))
            .thenReturn(Optional.empty());
        when(pedidoRepository.guardar(any(Pedido.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(mesaRepository.guardar(any(Mesa.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When: Se ejecuta el caso de uso
        AbrirMesaResponse response = useCase.ejecutar(localId, request);

        // Then: Se retorna la respuesta con mesa ABIERTA y pedido ABIERTO
        assertThat(response).isNotNull();
        assertThat(response.mesaId()).isEqualTo(mesaId.getValue().toString());
        assertThat(response.numeroMesa()).isEqualTo(5);
        assertThat(response.estadoMesa()).isEqualTo("ABIERTA");
        assertThat(response.estadoPedido()).isEqualTo("ABIERTO");
        assertThat(response.pedidoId()).isNotBlank();
        assertThat(response.fechaApertura()).isNotBlank();

        // Verifica que se guardaron ambos cambios
        verify(pedidoRepository, times(1)).guardar(any(Pedido.class));
        verify(mesaRepository, times(1)).guardar(any(Mesa.class));
    }

    @Test
    @DisplayName("Debería fallar si la mesa no existe")
    void deberia_fallar_si_mesa_no_existe() {
        // Given: Un request con un mesaId inexistente
        AbrirMesaRequest request = new AbrirMesaRequest(mesaId.getValue().toString());
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.empty());

        // When/Then: Lanza excepción
        assertThatThrownBy(() -> useCase.ejecutar(localId, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("La mesa no existe");

        // No debe persistir nada
        verifyNoInteractions(pedidoRepository);
        verify(mesaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debería fallar si la mesa no pertenece al local del usuario")
    void deberia_fallar_si_mesa_no_pertenece_al_local() {
        // Given: Una mesa que pertenece a otro local
        LocalId otroLocalId = new LocalId(UUID.randomUUID());
        Mesa mesaOtroLocal = new Mesa(mesaId, otroLocalId, 5);
        AbrirMesaRequest request = new AbrirMesaRequest(mesaId.getValue().toString());
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaOtroLocal));

        // When/Then: Lanza excepción de validación multi-tenant
        assertThatThrownBy(() -> useCase.ejecutar(localId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pertenece a este local");

        // No debe persistir nada
        verifyNoInteractions(pedidoRepository);
        verify(mesaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debería fallar si la mesa ya tiene un pedido abierto")
    void deberia_fallar_si_ya_existe_pedido_abierto() {
        // Given: Una mesa con un pedido abierto existente
        Pedido pedidoExistente = new Pedido(
            new PedidoId(UUID.randomUUID()),
            localId,
            mesaId,
            EstadoPedido.ABIERTO,
            LocalDateTime.now().minusHours(1)
        );
        AbrirMesaRequest request = new AbrirMesaRequest(mesaId.getValue().toString());
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaLibre));
        when(pedidoRepository.buscarPorMesaYEstado(mesaId, EstadoPedido.ABIERTO))
            .thenReturn(Optional.of(pedidoExistente));

        // When/Then: Lanza excepción
        assertThatThrownBy(() -> useCase.ejecutar(localId, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ya tiene un pedido abierto");

        // No debe persistir nada
        verify(pedidoRepository, never()).guardar(any());
        verify(mesaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debería fallar si se intenta abrir una mesa que ya está abierta")
    void deberia_fallar_si_mesa_ya_esta_abierta() {
        // Given: Una mesa ya ABIERTA
        mesaLibre.abrir();
        AbrirMesaRequest request = new AbrirMesaRequest(mesaId.getValue().toString());
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaLibre));
        when(pedidoRepository.buscarPorMesaYEstado(mesaId, EstadoPedido.ABIERTO))
            .thenReturn(Optional.empty());

        // When/Then: Falla en la entidad de dominio
        assertThatThrownBy(() -> useCase.ejecutar(localId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya se encuentra abierta");
    }

    @Test
    @DisplayName("Debería crear el pedido con los datos correctos")
    void deberia_crear_pedido_con_datos_correctos() {
        // Given
        AbrirMesaRequest request = new AbrirMesaRequest(mesaId.getValue().toString());
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaLibre));
        when(pedidoRepository.buscarPorMesaYEstado(mesaId, EstadoPedido.ABIERTO))
            .thenReturn(Optional.empty());
        when(pedidoRepository.guardar(any(Pedido.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(mesaRepository.guardar(any(Mesa.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        useCase.ejecutar(localId, request);

        // Then: Captura el pedido guardado
        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).guardar(pedidoCaptor.capture());
        
        Pedido pedidoGuardado = pedidoCaptor.getValue();
        assertThat(pedidoGuardado.getLocalId()).isEqualTo(localId);
        assertThat(pedidoGuardado.getMesaId()).isEqualTo(mesaId);
        assertThat(pedidoGuardado.getEstado()).isEqualTo(EstadoPedido.ABIERTO);
        assertThat(pedidoGuardado.getItems()).isEmpty();
        assertThat(pedidoGuardado.getDescuentos()).isEmpty();
        assertThat(pedidoGuardado.getFechaApertura()).isNotNull();
    }

    @Test
    @DisplayName("Debería fallar cuando el localId es null")
    void deberia_fallar_cuando_local_id_es_null() {
        // Given/When/Then
        AbrirMesaRequest request = new AbrirMesaRequest(mesaId.getValue().toString());
        
        assertThatThrownBy(() -> useCase.ejecutar(null, request))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("localId");

        verifyNoInteractions(mesaRepository, pedidoRepository);
    }

    @Test
    @DisplayName("Debería fallar cuando el request es null")
    void deberia_fallar_cuando_request_es_null() {
        // Given/When/Then
        assertThatThrownBy(() -> useCase.ejecutar(localId, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("request");

        verifyNoInteractions(mesaRepository, pedidoRepository);
    }

    @Test
    @DisplayName("Debería fallar cuando se construye con repositorios null")
    void deberia_fallar_cuando_repositorios_son_null() {
        // Given/When/Then
        assertThatThrownBy(() -> new AbrirMesaUseCase(null, pedidoRepository))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mesaRepository");

        assertThatThrownBy(() -> new AbrirMesaUseCase(mesaRepository, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("pedidoRepository");
    }

    @Test
    @DisplayName("Debería vincular el pedido al localId inmutablemente (multi-tenancy)")
    void deberia_vincular_pedido_a_local_id_inmutablemente() {
        // Given
        AbrirMesaRequest request = new AbrirMesaRequest(mesaId.getValue().toString());
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaLibre));
        when(pedidoRepository.buscarPorMesaYEstado(mesaId, EstadoPedido.ABIERTO))
            .thenReturn(Optional.empty());
        when(pedidoRepository.guardar(any(Pedido.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(mesaRepository.guardar(any(Mesa.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        useCase.ejecutar(localId, request);

        // Then: El pedido debe tener el localId vinculado
        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).guardar(pedidoCaptor.capture());
        
        Pedido pedidoPersistido = pedidoCaptor.getValue();
        
        assertThat(pedidoPersistido.getLocalId())
            .as("El pedido debe estar vinculado al mismo local que la mesa")
            .isEqualTo(localId)
            .isEqualTo(mesaLibre.getLocalId());
    }
}