package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CategoriaRequest;
import com.agustinpalma.comandas.application.dto.CategoriaResponse;
import com.agustinpalma.comandas.domain.model.Categoria;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del caso de uso EditarCategoriaUseCase.
 *
 * Escenarios:
 * - Happy path: edición exitosa con nuevo nombre.
 * - Unicidad excluyendo la categoría actual: otra categoría con el mismo nombre → excepción.
 * - Idempotencia: mantener el mismo nombre no falla (el use case excluye la categoría actual).
 * - Multi-tenancy: no se puede editar una categoría que pertenece a otro local.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class EditarCategoriaUseCaseTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    private EditarCategoriaUseCase useCase;

    private LocalId localId;
    private CategoriaId categoriaId;

    @BeforeEach
    void setUp() {
        useCase = new EditarCategoriaUseCase(categoriaRepository);
        localId = LocalId.generate();
        categoriaId = CategoriaId.generate();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Categoria categoriaExistente(String nombre) {
        return new Categoria(categoriaId, localId, nombre, "#AABBCC", false, false, 0);
    }

    // ============================================================
    // Happy path
    // ============================================================

    @Test
    void deberia_editar_categoria_exitosamente() {
        // Given — categoría existente que pertenece al local
        Categoria existente = categoriaExistente("Bebidas");
        CategoriaRequest request = new CategoriaRequest("Bebidas Calientes", "#FF0000", null, null, null, null);

        when(categoriaRepository.buscarPorId(categoriaId)).thenReturn(Optional.of(existente));
        // El nombre cambió → se verifica unicidad excluyendo la categoría actual
        when(categoriaRepository.existePorNombreYLocalExcluyendo("Bebidas Calientes", localId, categoriaId))
                .thenReturn(false);
        when(categoriaRepository.guardar(any(Categoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        CategoriaResponse response = useCase.ejecutar(categoriaId, localId, request);

        // Then — el nombre fue actualizado
        assertThat(response.nombre()).isEqualTo("Bebidas Calientes");

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).guardar(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Bebidas Calientes");
        assertThat(captor.getValue().getLocalId()).isEqualTo(localId);
    }

    // ============================================================
    // Unicidad excluyendo la categoría actual
    // ============================================================

    @Test
    void deberia_rechazar_nombre_duplicado_excluyendo_actual() {
        // Given — "Tragos" ya existe en otro registro del mismo local
        Categoria existente = categoriaExistente("Bebidas");
        CategoriaRequest request = new CategoriaRequest("Tragos", "#00FF00", null, null, null, null);

        when(categoriaRepository.buscarPorId(categoriaId)).thenReturn(Optional.of(existente));
        when(categoriaRepository.existePorNombreYLocalExcluyendo("Tragos", localId, categoriaId))
                .thenReturn(true);

        // When / Then — otra categoría ya tiene ese nombre → conflicto
        assertThatThrownBy(() -> useCase.ejecutar(categoriaId, localId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tragos");

        verify(categoriaRepository, never()).guardar(any());
    }

    // ============================================================
    // Idempotencia: mismo nombre → no debe fallar
    // ============================================================

    @Test
    void deberia_permitir_mantener_mismo_nombre() {
        // Given — se envía exactamente el mismo nombre que ya tiene la categoría
        Categoria existente = categoriaExistente("Bebidas");
        CategoriaRequest request = new CategoriaRequest("Bebidas", "#FF0000", null, null, null, null);

        when(categoriaRepository.buscarPorId(categoriaId)).thenReturn(Optional.of(existente));
        when(categoriaRepository.guardar(any(Categoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When — el use case detecta que el nombre no cambió y omite la validación de unicidad
        CategoriaResponse response = useCase.ejecutar(categoriaId, localId, request);

        // Then — persiste sin lanzar excepción; la verificación de duplicado nunca se llamó
        assertThat(response.nombre()).isEqualTo("Bebidas");
        verify(categoriaRepository, never()).existePorNombreYLocalExcluyendo(any(), any(), any());
        verify(categoriaRepository).guardar(any());
    }

    // ============================================================
    // Multi-tenancy: categoría de otro local
    // ============================================================

    @Test
    void deberia_rechazar_categoria_de_otro_local() {
        // Given — la categoría existe pero pertenece a un local diferente
        LocalId otroLocal = LocalId.generate();
        Categoria categoriaOtroLocal = new Categoria(categoriaId, otroLocal, "Postres", "#FFFFFF", false, false, 0);
        CategoriaRequest request = new CategoriaRequest("Postres Rediseñados", "#AAAAAA", null, null, null, null);

        when(categoriaRepository.buscarPorId(categoriaId)).thenReturn(Optional.of(categoriaOtroLocal));

        // When / Then — el localId del tenant actual no coincide → excepción de seguridad
        assertThatThrownBy(() -> useCase.ejecutar(categoriaId, localId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permisos");

        verify(categoriaRepository, never()).guardar(any());
    }
}
