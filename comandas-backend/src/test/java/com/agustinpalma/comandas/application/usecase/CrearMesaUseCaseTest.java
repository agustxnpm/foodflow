package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.MesaResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test unitario del caso de uso CrearMesaUseCase.
 * Sin Spring, sin base de datos, solo lógica pura con mocks.
 * Valida el comportamiento esperado según los criterios de aceptación de la HU-15.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Crear Mesa - Caso de Uso")
class CrearMesaUseCaseTest {

    @Mock
    private MesaRepository mesaRepository;

    private CrearMesaUseCase useCase;

    private LocalId localId;

    @BeforeEach
    void setUp() {
        useCase = new CrearMesaUseCase(mesaRepository);
        localId = new LocalId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Debería crear una mesa exitosamente con número válido y único")
    void deberia_crear_mesa_exitosamente() {
        // Given: Un número válido que no existe en el local
        int numeroMesa = 10;
        
        when(mesaRepository.existePorNumeroYLocal(numeroMesa, localId))
            .thenReturn(false);
        when(mesaRepository.guardar(any(Mesa.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When: Se ejecuta el caso de uso
        MesaResponse response = useCase.ejecutar(localId, numeroMesa);

        // Then: Se retorna la respuesta con la mesa creada en estado LIBRE
        assertThat(response).isNotNull();
        assertThat(response.numero()).isEqualTo(10);
        assertThat(response.estado()).isEqualTo("LIBRE");
        assertThat(response.id()).isNotBlank();

        // Verifica que se validó la unicidad
        verify(mesaRepository, times(1)).existePorNumeroYLocal(numeroMesa, localId);

        // Verifica que se guardó la mesa con los datos correctos
        ArgumentCaptor<Mesa> mesaCaptor = ArgumentCaptor.forClass(Mesa.class);
        verify(mesaRepository, times(1)).guardar(mesaCaptor.capture());
        
        Mesa mesaGuardada = mesaCaptor.getValue();
        assertThat(mesaGuardada.getNumero()).isEqualTo(10);
        assertThat(mesaGuardada.getLocalId()).isEqualTo(localId);
        assertThat(mesaGuardada.getEstado()).isEqualTo(EstadoMesa.LIBRE);
    }

    @Test
    @DisplayName("Debería fallar si el número de mesa ya existe en el local")
    void deberia_fallar_si_numero_duplicado() {
        // Given: Un número que ya existe en el local
        int numeroExistente = 5;
        when(mesaRepository.existePorNumeroYLocal(numeroExistente, localId))
            .thenReturn(true);

        // When/Then: Lanza excepción indicando duplicación
        assertThatThrownBy(() -> useCase.ejecutar(localId, numeroExistente))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ya existe una mesa con el número " + numeroExistente);

        // Verifica que NO se intentó guardar
        verify(mesaRepository, never()).guardar(any(Mesa.class));
    }

    @Test
    @DisplayName("Debería fallar si el número de mesa es 0")
    void deberia_fallar_si_numero_es_cero() {
        // Given: Un número inválido (0)
        int numeroInvalido = 0;

        // When/Then: Lanza excepción antes de validar unicidad
        assertThatThrownBy(() -> useCase.ejecutar(localId, numeroInvalido))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("debe ser mayor a 0");

        // Verifica que NO se validó unicidad ni se guardó
        verify(mesaRepository, never()).existePorNumeroYLocal(anyInt(), any(LocalId.class));
        verify(mesaRepository, never()).guardar(any(Mesa.class));
    }

    @Test
    @DisplayName("Debería fallar si el número de mesa es negativo")
    void deberia_fallar_si_numero_es_negativo() {
        // Given: Un número inválido (negativo)
        int numeroInvalido = -5;

        // When/Then: Lanza excepción antes de validar unicidad
        assertThatThrownBy(() -> useCase.ejecutar(localId, numeroInvalido))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("debe ser mayor a 0");

        // Verifica que NO se validó unicidad ni se guardó
        verify(mesaRepository, never()).existePorNumeroYLocal(anyInt(), any(LocalId.class));
        verify(mesaRepository, never()).guardar(any(Mesa.class));
    }

    @Test
    @DisplayName("Debería crear múltiples mesas con números diferentes sin conflicto")
    void deberia_crear_multiples_mesas_diferentes() {
        // Given: Varios números válidos y únicos
        when(mesaRepository.existePorNumeroYLocal(anyInt(), eq(localId)))
            .thenReturn(false);
        when(mesaRepository.guardar(any(Mesa.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When: Se crean varias mesas
        MesaResponse mesa1 = useCase.ejecutar(localId, 1);
        MesaResponse mesa2 = useCase.ejecutar(localId, 2);
        MesaResponse mesa3 = useCase.ejecutar(localId, 100);

        // Then: Todas se crean exitosamente
        assertThat(mesa1.numero()).isEqualTo(1);
        assertThat(mesa2.numero()).isEqualTo(2);
        assertThat(mesa3.numero()).isEqualTo(100);

        // Verifica que se guardaron las 3 mesas
        verify(mesaRepository, times(3)).guardar(any(Mesa.class));
    }

    @Test
    @DisplayName("Debería permitir crear mesa con número alto (ej: 999)")
    void deberia_permitir_numero_alto() {
        // Given: Un número muy alto pero válido
        int numeroAlto = 999;
        when(mesaRepository.existePorNumeroYLocal(numeroAlto, localId))
            .thenReturn(false);
        when(mesaRepository.guardar(any(Mesa.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When: Se crea la mesa
        MesaResponse response = useCase.ejecutar(localId, numeroAlto);

        // Then: Se crea exitosamente
        assertThat(response.numero()).isEqualTo(999);
        assertThat(response.estado()).isEqualTo("LIBRE");
        verify(mesaRepository, times(1)).guardar(any(Mesa.class));
    }

    @Test
    @DisplayName("Debería fallar si el localId es null")
    void deberia_fallar_si_localId_es_null() {
        // Given: Un localId null
        LocalId localIdNull = null;

        // When/Then: Lanza excepción de validación
        assertThatThrownBy(() -> useCase.ejecutar(localIdNull, 10))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("localId");

        // Verifica que no se ejecutó ninguna operación
        verify(mesaRepository, never()).existePorNumeroYLocal(anyInt(), any());
        verify(mesaRepository, never()).guardar(any(Mesa.class));
    }
}
