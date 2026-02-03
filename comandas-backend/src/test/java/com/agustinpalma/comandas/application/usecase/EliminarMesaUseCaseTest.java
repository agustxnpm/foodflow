package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitario del caso de uso EliminarMesaUseCase.
 * Sin Spring, sin base de datos, solo lógica pura con mocks.
 * Valida el comportamiento esperado según los criterios de aceptación de la HU-16.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Eliminar Mesa - Caso de Uso")
class EliminarMesaUseCaseTest {

    @Mock
    private MesaRepository mesaRepository;

    private EliminarMesaUseCase useCase;

    private LocalId localId;
    private MesaId mesaId;

    @BeforeEach
    void setUp() {
        useCase = new EliminarMesaUseCase(mesaRepository);
        localId = new LocalId(UUID.randomUUID());
        mesaId = new MesaId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Debería eliminar una mesa LIBRE exitosamente cuando hay múltiples mesas")
    void deberia_eliminar_mesa_libre_exitosamente() {
        // Given: Una mesa LIBRE del local y hay al menos 2 mesas en total
        Mesa mesaLibre = new Mesa(mesaId, localId, 5);
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaLibre));
        when(mesaRepository.contarPorLocal(localId)).thenReturn(3); // Hay 3 mesas, se puede eliminar 1

        // When: Se ejecuta el caso de uso
        assertThatCode(() -> useCase.ejecutar(localId, mesaId))
            .doesNotThrowAnyException();

        // Then: Se eliminó la mesa
        verify(mesaRepository, times(1)).eliminar(mesaId);
    }

    @Test
    @DisplayName("Debería fallar si la mesa no existe")
    void deberia_fallar_si_mesa_no_existe() {
        // Given: Un mesaId inexistente
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.empty());

        // When/Then: Lanza excepción indicando que no existe
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesaId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no existe");

        // Verifica que NO se intentó eliminar
        verify(mesaRepository, never()).eliminar(any(MesaId.class));
    }

    @Test
    @DisplayName("Debería fallar si la mesa pertenece a otro local")
    void deberia_fallar_si_mesa_de_otro_local() {
        // Given: Una mesa que pertenece a otro local
        LocalId otroLocalId = new LocalId(UUID.randomUUID());
        Mesa mesaDeOtroLocal = new Mesa(mesaId, otroLocalId, 5);
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaDeOtroLocal));

        // When/Then: Lanza excepción de seguridad multi-tenant
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesaId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("permisos")
            .hasMessageContaining("otro local");

        // Verifica que NO se intentó eliminar
        verify(mesaRepository, never()).eliminar(any(MesaId.class));
    }

    @Test
    @DisplayName("Debería fallar si la mesa está ABIERTA (tiene pedido activo)")
    void deberia_fallar_si_mesa_abierta() {
        // Given: Una mesa ABIERTA del local
        Mesa mesaAbierta = new Mesa(mesaId, localId, 5);
        mesaAbierta.abrir(); // Transiciona a ABIERTA
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaAbierta));

        // When/Then: Lanza excepción indicando que tiene pedido abierto
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesaId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("pedido abierto");

        // Verifica que NO se intentó eliminar ni se contaron mesas
        verify(mesaRepository, never()).contarPorLocal(any(LocalId.class));
        verify(mesaRepository, never()).eliminar(any(MesaId.class));
    }

    @Test
    @DisplayName("Debería fallar si es la última mesa del local")
    void deberia_fallar_si_es_ultima_mesa() {
        // Given: Una mesa LIBRE pero es la única del local
        Mesa mesaLibre = new Mesa(mesaId, localId, 1);
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaLibre));
        when(mesaRepository.contarPorLocal(localId)).thenReturn(1); // Solo hay 1 mesa

        // When/Then: Lanza excepción protegiendo la regla de negocio
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesaId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("última mesa")
            .hasMessageContaining("al menos una mesa");

        // Verifica que NO se intentó eliminar
        verify(mesaRepository, never()).eliminar(any(MesaId.class));
    }

    @Test
    @DisplayName("Debería permitir eliminar mesa cuando hay exactamente 2 mesas")
    void deberia_permitir_eliminar_cuando_hay_dos_mesas() {
        // Given: Una mesa LIBRE y hay exactamente 2 mesas (después quedará 1, que es el mínimo)
        Mesa mesaLibre = new Mesa(mesaId, localId, 10);
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaLibre));
        when(mesaRepository.contarPorLocal(localId)).thenReturn(2); // Hay 2 mesas

        // When: Se ejecuta el caso de uso
        assertThatCode(() -> useCase.ejecutar(localId, mesaId))
            .doesNotThrowAnyException();

        // Then: Se eliminó la mesa (quedará 1, que es el mínimo permitido)
        verify(mesaRepository, times(1)).eliminar(mesaId);
    }

    @Test
    @DisplayName("Debería validar en orden: existencia -> permisos -> estado -> regla última mesa")
    void deberia_validar_en_orden_correcto() {
        // Given: Una mesa de otro local que está abierta y es la última
        LocalId otroLocalId = new LocalId(UUID.randomUUID());
        Mesa mesa = new Mesa(mesaId, otroLocalId, 1);
        mesa.abrir();
        
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesa));

        // When/Then: Debe fallar primero por permisos (validación multi-tenant)
        // No debe llegar a validar estado ni cantidad
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesaId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("permisos");

        // Verifica que NO se llegó a contar mesas ni eliminar
        verify(mesaRepository, never()).contarPorLocal(any(LocalId.class));
        verify(mesaRepository, never()).eliminar(any(MesaId.class));
    }

    @Test
    @DisplayName("Debería fallar si el localId es null")
    void deberia_fallar_si_localId_es_null() {
        // Given: Un localId null
        LocalId localIdNull = null;

        // When/Then: Lanza excepción de validación
        assertThatThrownBy(() -> useCase.ejecutar(localIdNull, mesaId))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("localId");

        // Verifica que no se ejecutó ninguna operación
        verify(mesaRepository, never()).buscarPorId(any(MesaId.class));
        verify(mesaRepository, never()).eliminar(any(MesaId.class));
    }

    @Test
    @DisplayName("Debería fallar si el mesaId es null")
    void deberia_fallar_si_mesaId_es_null() {
        // Given: Un mesaId null
        MesaId mesaIdNull = null;

        // When/Then: Lanza excepción de validación
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesaIdNull))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mesaId");

        // Verifica que no se ejecutó ninguna operación
        verify(mesaRepository, never()).buscarPorId(any());
        verify(mesaRepository, never()).eliminar(any());
    }

    @Test
    @DisplayName("Debería permitir eliminar mesas hasta dejar solo 1")
    void deberia_permitir_eliminar_hasta_dejar_una() {
        // Given: Varias mesas del local, se irán eliminando hasta dejar 1
        Mesa mesa1 = new Mesa(new MesaId(UUID.randomUUID()), localId, 1);
        Mesa mesa2 = new Mesa(new MesaId(UUID.randomUUID()), localId, 2);
        Mesa mesa3 = new Mesa(new MesaId(UUID.randomUUID()), localId, 3);

        // Eliminar la primera mesa (quedan 3 mesas)
        when(mesaRepository.buscarPorId(mesa1.getId())).thenReturn(Optional.of(mesa1));
        when(mesaRepository.contarPorLocal(localId)).thenReturn(3);
        
        assertThatCode(() -> useCase.ejecutar(localId, mesa1.getId()))
            .doesNotThrowAnyException();
        verify(mesaRepository, times(1)).eliminar(mesa1.getId());

        // Eliminar la segunda mesa (quedan 2 mesas)
        when(mesaRepository.buscarPorId(mesa2.getId())).thenReturn(Optional.of(mesa2));
        when(mesaRepository.contarPorLocal(localId)).thenReturn(2);
        
        assertThatCode(() -> useCase.ejecutar(localId, mesa2.getId()))
            .doesNotThrowAnyException();
        verify(mesaRepository, times(1)).eliminar(mesa2.getId());

        // Intentar eliminar la tercera (última) debería fallar
        when(mesaRepository.buscarPorId(mesa3.getId())).thenReturn(Optional.of(mesa3));
        when(mesaRepository.contarPorLocal(localId)).thenReturn(1);
        
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesa3.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("última mesa");
    }
}
