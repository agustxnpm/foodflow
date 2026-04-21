package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.domain.model.Categoria;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del caso de uso EliminarCategoriaUseCase.
 *
 * Escenarios:
 * - Happy path: eliminación exitosa.
 * - Categoría inexistente → excepción 404 semántica.
 * - Multi-tenancy: categoría de otro local → acceso denegado.
 * - Integridad referencial: el repositorio detecta productos asociados
 *   y rechaza la eliminación con una excepción (simula FK constraint).
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class EliminarCategoriaUseCaseTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    private EliminarCategoriaUseCase useCase;

    private LocalId localId;
    private CategoriaId categoriaId;

    @BeforeEach
    void setUp() {
        useCase = new EliminarCategoriaUseCase(categoriaRepository);
        localId = LocalId.generate();
        categoriaId = CategoriaId.generate();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Categoria categoriaExistente() {
        return new Categoria(categoriaId, localId, "Bebidas", "#FFFFFF", false, false, 0);
    }

    // ============================================================
    // Happy path
    // ============================================================

    @Test
    void deberia_eliminar_categoria_exitosamente() {
        // Given — la categoría existe y pertenece al local
        when(categoriaRepository.buscarPorId(categoriaId))
                .thenReturn(Optional.of(categoriaExistente()));
        doNothing().when(categoriaRepository).eliminar(categoriaId);

        // When
        useCase.ejecutar(categoriaId, localId);

        // Then — el repositorio recibió exactamente la orden de eliminar el ID correcto
        verify(categoriaRepository).buscarPorId(categoriaId);
        verify(categoriaRepository).eliminar(categoriaId);
    }

    // ============================================================
    // Categoría inexistente (404 semántico)
    // ============================================================

    @Test
    void deberia_rechazar_categoria_inexistente() {
        // Given — el repositorio no encuentra nada para ese ID
        when(categoriaRepository.buscarPorId(categoriaId)).thenReturn(Optional.empty());

        // When / Then — se lanza excepción antes de intentar eliminar
        assertThatThrownBy(() -> useCase.ejecutar(categoriaId, localId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrada");

        verify(categoriaRepository, never()).eliminar(any());
    }

    // ============================================================
    // Multi-tenancy: categoría pertenece a otro local
    // ============================================================

    @Test
    void deberia_rechazar_categoria_de_otro_local() {
        // Given — la categoría existe pero fue creada por un tenant diferente
        LocalId otroLocal = LocalId.generate();
        Categoria categoriaOtroLocal = new Categoria(categoriaId, otroLocal, "Postres", "#FFFFFF", false, false, 0);
        when(categoriaRepository.buscarPorId(categoriaId))
                .thenReturn(Optional.of(categoriaOtroLocal));

        // When / Then — el localId del tenant actual no coincide con el de la entidad
        assertThatThrownBy(() -> useCase.ejecutar(categoriaId, localId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("permisos");

        verify(categoriaRepository, never()).eliminar(any());
    }

    // ============================================================
    // Integridad referencial: productos asociados
    // ============================================================

    @Test
    void deberia_rechazar_si_tiene_productos_asociados() {
        // Given — la categoría existe y pertenece al local,
        //         pero la capa de infraestructura detecta productos asociados (FK constraint)
        //         y lanza una excepción al intentar eliminar.
        when(categoriaRepository.buscarPorId(categoriaId))
                .thenReturn(Optional.of(categoriaExistente()));
        doThrow(new IllegalStateException("No se puede eliminar: existen productos asociados a esta categoría"))
                .when(categoriaRepository).eliminar(categoriaId);

        // When / Then — la excepción propaga la restricción referencial al llamador
        assertThatThrownBy(() -> useCase.ejecutar(categoriaId, localId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("productos asociados");

        verify(categoriaRepository).eliminar(categoriaId);
    }
}
