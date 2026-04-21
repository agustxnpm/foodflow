package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CategoriaResponse;
import com.agustinpalma.comandas.domain.model.Categoria;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests del caso de uso ConsultarCategoriasUseCase (Fase 6 — solo lectura).
 *
 * Escenarios:
 * - Happy path: el use case retorna las categorías mapeadas a DTO.
 * - Multi-tenancy: solo se consultan las categorías del local solicitado.
 * - Lista vacía: el use case nunca falla; retorna una lista vacía.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 * Sin Spring. Sin base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ConsultarCategoriasUseCaseTest {

    @Mock private CategoriaRepository categoriaRepository;

    private ConsultarCategoriasUseCase useCase;
    private LocalId localId;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarCategoriasUseCase(categoriaRepository);
        localId = LocalId.generate();
    }

    @Test
    void deberia_listar_categorias_del_local() {
        // Given — 2 categorías persistidas para el local
        Categoria bebidas = new Categoria(
            CategoriaId.generate(), localId, "Bebidas", "#FF0000",
            false, false, 0
        );
        Categoria comidas = new Categoria(
            CategoriaId.generate(), localId, "Comidas", "#00FF00",
            true, false, 1
        );
        given(categoriaRepository.buscarPorLocal(localId))
            .willReturn(List.of(bebidas, comidas));

        // When
        List<CategoriaResponse> response = useCase.ejecutar(localId);

        // Then — el DTO refleja exactamente las categorías del local
        assertThat(response).hasSize(2);
        assertThat(response).extracting(CategoriaResponse::nombre)
            .containsExactly("Bebidas", "Comidas");
        assertThat(response).extracting(CategoriaResponse::admiteVariantes)
            .containsExactly(false, true);
    }

    @Test
    void deberia_consultar_solo_categorias_del_local_solicitado() {
        // Given — el use case se invoca para localId
        given(categoriaRepository.buscarPorLocal(localId)).willReturn(List.of());

        // When
        useCase.ejecutar(localId);

        // Then — el repositorio fue invocado con EXACTAMENTE el localId solicitado
        // (multi-tenancy: ningún otro local se ve afectado)
        then(categoriaRepository).should(times(1)).buscarPorLocal(localId);
        then(categoriaRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void deberia_retornar_lista_vacia_si_no_hay_categorias() {
        // Given — el local existe pero aún no creó categorías
        given(categoriaRepository.buscarPorLocal(localId)).willReturn(List.of());

        // When
        List<CategoriaResponse> response = useCase.ejecutar(localId);

        // Then — lista vacía, sin excepción
        assertThat(response).isNotNull().isEmpty();
    }
}
