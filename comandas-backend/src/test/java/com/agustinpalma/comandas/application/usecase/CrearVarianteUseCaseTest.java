package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.VarianteProductoRequest;
import com.agustinpalma.comandas.application.dto.VarianteProductoResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del caso de uso CrearVarianteUseCase.
 * 
 * Escenarios cubiertos:
 * - Creación exitosa de primera variante (el base se convierte en líder del grupo)
 * - Creación de variante adicional a un grupo existente
 * - Rechazo por nombre duplicado
 * - Rechazo por cantidadDiscosCarne duplicada en el grupo
 * - Rechazo si el producto base es un extra
 * - Herencia de propiedades del producto base
 */
@ExtendWith(MockitoExtension.class)
class CrearVarianteUseCaseTest {

    @Mock
    private ProductoRepository productoRepository;

    private CrearVarianteUseCase useCase;
    private LocalId localId;

    @BeforeEach
    void setUp() {
        useCase = new CrearVarianteUseCase(productoRepository);
        localId = LocalId.generate();
    }

    @Nested
    class CreacionDePrimeraVariante {

        @Test
        void deberia_crear_variante_y_asignar_grupo_al_producto_base() {
            // Given: Un producto base SIN grupo de variantes
            ProductoId baseId = ProductoId.generate();
            Producto productoBase = new Producto(
                baseId, localId, "Hamburguesa Completa", new BigDecimal("1500.00"),
                true, "#FF0000"
            );
            assertNull(productoBase.getGrupoVarianteId(), "Pre-condición: base no tiene grupo");

            VarianteProductoRequest request = new VarianteProductoRequest(
                "Hamburguesa Completa Doble", new BigDecimal("2200.00"),
                2, null, null, null, null, null, null
            );

            when(productoRepository.buscarPorIdYLocal(baseId, localId))
                .thenReturn(Optional.of(productoBase));
            when(productoRepository.existePorNombreYLocal("Hamburguesa Completa Doble", localId))
                .thenReturn(false);
            when(productoRepository.buscarPorGrupoVariante(localId, baseId))
                .thenReturn(List.of())  // Primera vez: para validar discos
                .thenReturn(List.of(productoBase)); // Segunda vez: para el response (post-persistencia)
            when(productoRepository.guardar(any(Producto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            VarianteProductoResponse response = useCase.ejecutar(localId, baseId, request);

            // Then
            assertNotNull(response);
            assertEquals("Hamburguesa Completa Doble", response.varianteCreada().nombre());
            assertEquals(new BigDecimal("2200.00"), response.varianteCreada().precio());
            assertEquals(2, response.varianteCreada().cantidadDiscosCarne());
            assertEquals(baseId.getValue().toString(), response.varianteCreada().grupoVarianteId());

            // El base fue actualizado: grupoVarianteId = su propio ID, cantidadDiscosCarne = 1
            assertEquals(baseId, productoBase.getGrupoVarianteId());
            assertEquals(1, productoBase.getCantidadDiscosCarne());

            // Se guardaron 2 productos: el base actualizado y la nueva variante
            verify(productoRepository, times(2)).guardar(any(Producto.class));
        }

        @Test
        void deberia_heredar_color_del_base_cuando_no_se_especifica() {
            // Given
            ProductoId baseId = ProductoId.generate();
            Producto productoBase = new Producto(
                baseId, localId, "Pizza Muzzarella", new BigDecimal("3000.00"),
                true, "#FF5500"
            );

            VarianteProductoRequest request = new VarianteProductoRequest(
                "Pizza Muzzarella Grande", new BigDecimal("4500.00"),
                2, null, null, null, null, null, null  // colorHex null → hereda #FF5500
            );

            when(productoRepository.buscarPorIdYLocal(baseId, localId))
                .thenReturn(Optional.of(productoBase));
            when(productoRepository.existePorNombreYLocal(any(), eq(localId)))
                .thenReturn(false);
            when(productoRepository.buscarPorGrupoVariante(eq(localId), any()))
                .thenReturn(List.of());
            when(productoRepository.guardar(any(Producto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            VarianteProductoResponse response = useCase.ejecutar(localId, baseId, request);

            // Then: la variante hereda el color del base
            assertEquals("#FF5500", response.varianteCreada().colorHex());
        }
    }

    @Nested
    class CreacionDeVarianteAdicional {

        @Test
        void deberia_crear_variante_en_grupo_existente_sin_modificar_base() {
            // Given: Un producto base que ya tiene grupo
            ProductoId baseId = ProductoId.generate();
            ProductoId grupoId = baseId; // El base es el líder
            Producto productoBase = new Producto(
                baseId, localId, "Hamburguesa Completa", new BigDecimal("1500.00"),
                true, "#FF0000", grupoId, false, 1
            );

            // Ya existe una variante "Doble" con cantidadDiscos=2
            Producto varianteExistente = new Producto(
                ProductoId.generate(), localId, "Hamburguesa Completa Doble", new BigDecimal("2200.00"),
                true, "#FF0000", grupoId, false, 2
            );

            VarianteProductoRequest request = new VarianteProductoRequest(
                "Hamburguesa Completa Triple", new BigDecimal("2800.00"),
                3, null, null, null, null, null, null
            );

            when(productoRepository.buscarPorIdYLocal(baseId, localId))
                .thenReturn(Optional.of(productoBase));
            when(productoRepository.existePorNombreYLocal("Hamburguesa Completa Triple", localId))
                .thenReturn(false);
            when(productoRepository.buscarPorGrupoVariante(localId, grupoId))
                .thenReturn(List.of(productoBase, varianteExistente));
            when(productoRepository.guardar(any(Producto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            VarianteProductoResponse response = useCase.ejecutar(localId, baseId, request);

            // Then
            assertEquals("Hamburguesa Completa Triple", response.varianteCreada().nombre());
            assertEquals(3, response.varianteCreada().cantidadDiscosCarne());

            // Solo se guardó la nueva variante (el base ya tenía grupo)
            verify(productoRepository, times(1)).guardar(any(Producto.class));
        }
    }

    @Nested
    class Validaciones {

        @Test
        void deberia_rechazar_variante_con_nombre_duplicado() {
            // Given
            ProductoId baseId = ProductoId.generate();
            Producto productoBase = new Producto(
                baseId, localId, "Hamburguesa Completa", new BigDecimal("1500.00"),
                true, "#FF0000"
            );

            VarianteProductoRequest request = new VarianteProductoRequest(
                "Hamburguesa Completa", new BigDecimal("2200.00"),
                2, null, null, null, null, null, null
            );

            when(productoRepository.buscarPorIdYLocal(baseId, localId))
                .thenReturn(Optional.of(productoBase));
            when(productoRepository.existePorNombreYLocal("Hamburguesa Completa", localId))
                .thenReturn(true);

            // When/Then
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(localId, baseId, request)
            );
            assertTrue(ex.getMessage().contains("Ya existe un producto con el nombre"));
        }

        @Test
        void deberia_rechazar_variante_con_cantidadDiscos_duplicada_en_grupo() {
            // Given
            ProductoId baseId = ProductoId.generate();
            ProductoId grupoId = baseId;
            Producto productoBase = new Producto(
                baseId, localId, "Hamburguesa Completa", new BigDecimal("1500.00"),
                true, "#FF0000", grupoId, false, 1
            );

            VarianteProductoRequest request = new VarianteProductoRequest(
                "Hamburguesa Completa Otra Simple", new BigDecimal("1600.00"),
                1, null, null, null, null, null, null  // cantidadDiscos=1, ya ocupado por el base
            );

            when(productoRepository.buscarPorIdYLocal(baseId, localId))
                .thenReturn(Optional.of(productoBase));
            when(productoRepository.existePorNombreYLocal(any(), eq(localId)))
                .thenReturn(false);
            when(productoRepository.buscarPorGrupoVariante(localId, grupoId))
                .thenReturn(List.of(productoBase));

            // When/Then
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(localId, baseId, request)
            );
            assertTrue(ex.getMessage().contains("cantidadDiscosCarne=1"));
        }

        @Test
        void deberia_rechazar_variante_si_base_es_extra() {
            // Given
            ProductoId baseId = ProductoId.generate();
            Producto productoExtra = new Producto(
                baseId, localId, "Disco de Carne", new BigDecimal("500.00"),
                true, "#FF0000",
                null, true, false, null, null, true, true, 0, false
            );

            VarianteProductoRequest request = new VarianteProductoRequest(
                "Disco de Carne Doble", new BigDecimal("900.00"),
                2, null, null, null, null, null, null
            );

            when(productoRepository.buscarPorIdYLocal(baseId, localId))
                .thenReturn(Optional.of(productoExtra));

            // When/Then
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(localId, baseId, request)
            );
            assertTrue(ex.getMessage().contains("es un extra y no puede tener variantes"));
        }

        @Test
        void deberia_rechazar_si_producto_base_no_existe() {
            // Given
            ProductoId baseId = ProductoId.generate();

            VarianteProductoRequest request = new VarianteProductoRequest(
                "Variante X", new BigDecimal("1000.00"),
                2, null, null, null, null, null, null
            );

            when(productoRepository.buscarPorIdYLocal(baseId, localId))
                .thenReturn(Optional.empty());

            // When/Then
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(localId, baseId, request)
            );
            assertTrue(ex.getMessage().contains("No se encontró el producto base"));
        }
    }
}
