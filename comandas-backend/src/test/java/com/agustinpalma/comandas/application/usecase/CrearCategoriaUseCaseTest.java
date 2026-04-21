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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del caso de uso CrearCategoriaUseCase.
 *
 * Escenarios:
 * - Happy path: creación exitosa con nombre y color válidos.
 * - Unicidad case-insensitive: "BEBIDAS" y "Bebidas" se consideran duplicados.
 * - Color default: si colorHex es null, el dominio asigna #FFFFFF.
 * - Nombre vacío: fail fast antes de llegar al repositorio.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class CrearCategoriaUseCaseTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    private CrearCategoriaUseCase useCase;

    private LocalId localId;

    @BeforeEach
    void setUp() {
        useCase = new CrearCategoriaUseCase(categoriaRepository);
        localId = LocalId.generate();
    }

    // ============================================================
    // Happy path
    // ============================================================

    @Test
    void deberia_crear_categoria_exitosamente() {
        // Given — el nombre no existe en el local
        CategoriaRequest request = new CategoriaRequest("Bebidas", "#FF0000", false, false, 1, null);
        when(categoriaRepository.existePorNombreYLocal("Bebidas", localId)).thenReturn(false);
        when(categoriaRepository.guardar(any(Categoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        CategoriaResponse response = useCase.ejecutar(localId, request);

        // Then — la respuesta refleja los datos del request
        assertThat(response.nombre()).isEqualTo("Bebidas");
        assertThat(response.colorHex()).isEqualToIgnoringCase("#FF0000");
        assertThat(response.id()).isNotNull();

        // Verificar que se persistió exactamente una entidad
        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).guardar(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Bebidas");
        assertThat(captor.getValue().getLocalId()).isEqualTo(localId);
    }

    // ============================================================
    // Unicidad case-insensitive
    // ============================================================

    @Test
    void deberia_rechazar_nombre_duplicado_case_insensitive() {
        // Given — "BEBIDAS" colisiona con la categoría existente "Bebidas"
        CategoriaRequest request = new CategoriaRequest("BEBIDAS", "#00FF00", false, false, 0, null);
        when(categoriaRepository.existePorNombreYLocal("BEBIDAS", localId)).thenReturn(true);

        // When / Then — el use case lanza excepción de conflicto (409 semántico)
        assertThatThrownBy(() -> useCase.ejecutar(localId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BEBIDAS");

        // El repositorio no debe haber intentado persistir nada
        verify(categoriaRepository, never()).guardar(any());
    }

    // ============================================================
    // Color default cuando colorHex es null
    // ============================================================

    @Test
    void deberia_asignar_color_default_si_null() {
        // Given — colorHex enviado como null
        CategoriaRequest request = new CategoriaRequest("Postres", null, false, false, 0, null);
        when(categoriaRepository.existePorNombreYLocal("Postres", localId)).thenReturn(false);
        when(categoriaRepository.guardar(any(Categoria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        CategoriaResponse response = useCase.ejecutar(localId, request);

        // Then — el dominio normaliza a #FFFFFF cuando no se provee color
        assertThat(response.colorHex()).isEqualTo("#FFFFFF");

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).guardar(captor.capture());
        assertThat(captor.getValue().getColorHex()).isEqualTo("#FFFFFF");
    }

    // ============================================================
    // Guard: nombre vacío — fail fast en dominio
    // ============================================================

    @Test
    void deberia_rechazar_nombre_vacio() {
        // Given — nombre en blanco
        CategoriaRequest request = new CategoriaRequest("   ", "#FFFFFF", false, false, 0, null);
        when(categoriaRepository.existePorNombreYLocal(any(), eq(localId))).thenReturn(false);

        // When / Then — el dominio rechaza en construcción (Categoria.validarNombre)
        assertThatThrownBy(() -> useCase.ejecutar(localId, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(categoriaRepository, never()).guardar(any());
    }
}
