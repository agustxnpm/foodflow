package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.MesaResponse;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

/**
 * Test unitario del caso de uso ConsultarMesasUseCase.
 * Sin Spring, sin base de datos, solo lógica pura con mocks.
 * Valida el comportamiento esperado del caso de uso.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Consultar Mesas - Caso de Uso")
class ConsultarMesasUseCaseTest {

    @Mock
    private MesaRepository mesaRepository;

    private ConsultarMesasUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarMesasUseCase(mesaRepository);
    }

    @Test
    @DisplayName("Debería retornar mesas mapeadas correctamente filtrando por local")
    void deberia_retornar_mesas_mapeadas_correctamente_filtrando_por_local() {
        // Given: Un local con dos mesas (una LIBRE, una ABIERTA)
        LocalId localId = new LocalId(UUID.randomUUID());

        Mesa mesaLibre = new Mesa(
            new MesaId(UUID.randomUUID()),
            localId,
            1
        );

        Mesa mesaAbierta = new Mesa(
            new MesaId(UUID.randomUUID()),
            localId,
            2
        );
        mesaAbierta.abrir();

        when(mesaRepository.buscarPorLocal(localId))
            .thenReturn(List.of(mesaLibre, mesaAbierta));

        // When: Se ejecuta el caso de uso
        List<MesaResponse> resultado = useCase.ejecutar(localId);

        // Then: Se retornan 2 DTOs con estados correctos (sin importar el orden)
        assertThat(resultado).hasSize(2);
        assertThat(resultado)
            .extracting(MesaResponse::numero, MesaResponse::estado)
            .containsExactlyInAnyOrder(
                tuple(1, "LIBRE"),
                tuple(2, "ABIERTA")
            );

        // Verifica que se llamó al repositorio con el localId correcto
        verify(mesaRepository, times(1)).buscarPorLocal(localId);
    }

    @Test
    @DisplayName("Debería retornar lista vacía cuando no existen mesas para el local")
    void deberia_retornar_lista_vacia_cuando_no_existen_mesas() {
        // Given: Un local sin mesas
        LocalId localId = new LocalId(UUID.randomUUID());
        when(mesaRepository.buscarPorLocal(localId)).thenReturn(List.of());

        // When: Se ejecuta el caso de uso
        List<MesaResponse> resultado = useCase.ejecutar(localId);

        // Then: Se retorna lista vacía
        assertThat(resultado).isEmpty();
        verify(mesaRepository, times(1)).buscarPorLocal(localId);
    }

    @Test
    @DisplayName("Debería fallar cuando el localId es null")
    void deberia_fallar_cuando_local_id_es_null() {
        // Given/When/Then: Se lanza excepción al pasar null
        assertThatThrownBy(() -> useCase.ejecutar(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("localId");

        // No debe llamar al repositorio
        verifyNoInteractions(mesaRepository);
    }

    @Test
    @DisplayName("Debería fallar cuando el repositorio es null en construcción")
    void deberia_fallar_cuando_repositorio_es_null() {
        // Given/When/Then: Se lanza excepción al construir con null
        assertThatThrownBy(() -> new ConsultarMesasUseCase(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mesaRepository");
    }


    @Test
    @DisplayName("Debería mapear correctamente los IDs de las mesas")
    void deberia_mapear_correctamente_los_ids() {
        // Given: Una mesa con un UUID específico
        LocalId localId = new LocalId(UUID.randomUUID());
        MesaId mesaId = new MesaId(UUID.randomUUID());
        Mesa mesa = new Mesa(mesaId, localId, 10);
        
        when(mesaRepository.buscarPorLocal(localId)).thenReturn(List.of(mesa));

        // When: Se ejecuta el caso de uso
        List<MesaResponse> resultado = useCase.ejecutar(localId);

        // Then: El ID se convirtió correctamente a String sin pérdida de información
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id())
            .isEqualTo(mesaId.getValue().toString());
        assertThat(resultado.get(0).numero()).isEqualTo(10);
        
        verify(mesaRepository, times(1)).buscarPorLocal(localId);
    }
}
