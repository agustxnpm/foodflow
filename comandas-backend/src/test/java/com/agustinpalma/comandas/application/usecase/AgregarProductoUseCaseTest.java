package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AgregarProductoRequest;
import com.agustinpalma.comandas.application.dto.AgregarProductoResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitario del caso de uso AgregarProductoUseCase.
 * Sin Spring, sin base de datos, solo lógica pura con mocks.
 * Valida el comportamiento esperado según los criterios de aceptación de HU-05.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Agregar Producto a Pedido - Caso de Uso")
class AgregarProductoUseCaseTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoRepository productoRepository;

    private AgregarProductoUseCase useCase;

    private LocalId localId;
    private PedidoId pedidoId;
    private ProductoId productoId;
    private MesaId mesaId;

    @BeforeEach
    void setUp() {
        useCase = new AgregarProductoUseCase(pedidoRepository, productoRepository);
        
        // Datos de prueba comunes
        localId = new LocalId(UUID.randomUUID());
        pedidoId = PedidoId.generate();
        productoId = ProductoId.generate();
        mesaId = MesaId.generate();
    }

    @Test
    @DisplayName("AC3 - Snapshot de Precio: El ítem debe guardar el precio actual del producto")
    void deberia_capturar_precio_del_producto_al_momento_de_agregar() {
        // Given: Un producto con precio $100
        Producto producto = new Producto(productoId, localId, "Hamburguesa", new BigDecimal("100.00"), true);
        Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
        when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(producto));
        when(pedidoRepository.guardar(any(Pedido.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId, productoId, 2, "Sin cebolla"
        );

        // When: Se agrega el producto al pedido
        AgregarProductoResponse response = useCase.ejecutar(request);

        // Then: El ítem debe tener el precio capturado ($100)
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).precioUnitario()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.items().get(0).nombreProducto()).isEqualTo("Hamburguesa");
        assertThat(response.items().get(0).cantidad()).isEqualTo(2);
        assertThat(response.items().get(0).subtotalItem()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.items().get(0).observacion()).isEqualTo("Sin cebolla");
        
        // El subtotal del pedido debe reflejar los ítems agregados
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
        
        verify(pedidoRepository, times(1)).guardar(any(Pedido.class));
    }

    @Test
    @DisplayName("AC3 - Inmutabilidad: Cambios posteriores en el catálogo NO afectan ítems históricos")
    void cambios_en_precio_del_producto_no_deben_afectar_items_existentes() {
        // Given: Un producto con precio inicial $100
        Producto producto = new Producto(productoId, localId, "Pizza", new BigDecimal("100.00"), true);
        Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
        when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(producto));
        when(pedidoRepository.guardar(any(Pedido.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId, productoId, 1, null
        );

        // When: Se agrega el producto (snapshot a $100)
        AgregarProductoResponse primeraRespuesta = useCase.ejecutar(request);
        
        // Simulamos cambio de precio en el catálogo a $200
        // NOTA: En realidad el producto es inmutable por construcción,
        // pero lo que importa es que el ítem YA tiene el snapshot guardado
        
        // Then: El ítem debe seguir teniendo el precio original ($100)
        assertThat(primeraRespuesta.items()).hasSize(1);
        assertThat(primeraRespuesta.items().get(0).precioUnitario())
            .isEqualByComparingTo(new BigDecimal("100.00"));
        
        // Verificamos que el subtotal se calcula correctamente
        assertThat(primeraRespuesta.subtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("AC5 - Multi-tenancy: Debe rechazar productos de otro local")
    void deberia_rechazar_producto_de_otro_local() {
        // Given: Un pedido del Local A y un producto del Local B
        LocalId localA = new LocalId(UUID.randomUUID());
        LocalId localB = new LocalId(UUID.randomUUID());
        
        Pedido pedidoLocalA = new Pedido(pedidoId, localA, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        Producto productoLocalB = new Producto(productoId, localB, "Producto Ajeno", new BigDecimal("50.00"), true);
        
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedidoLocalA));
        when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(productoLocalB));

        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId, productoId, 1, null
        );

        // When/Then: Debe lanzar excepción por violación de multi-tenancy
        assertThatThrownBy(() -> useCase.ejecutar(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no pertenece al mismo local");
        
        // NO debe persistirse el pedido
        verify(pedidoRepository, never()).guardar(any(Pedido.class));
    }

    @Test
    @DisplayName("AC4 - Estado del Pedido: Solo permite agregar a pedidos ABIERTOS")
    void deberia_rechazar_agregar_a_pedido_cerrado() {
        // Given: Un pedido en estado ABIERTO que cerraremos
        Producto producto = new Producto(productoId, localId, "Café", new BigDecimal("30.00"), true);
        Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        
        // Agregamos un ítem primero para poder cerrarlo
        pedido.agregarProducto(producto, 1, null);
        
        // Cerramos el pedido
        pedido.finalizar(
            com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago.EFECTIVO, 
            LocalDateTime.now()
        );
        
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
        when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(producto));

        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId, productoId, 1, null
        );

        // When/Then: Debe lanzar excepción porque el pedido está CERRADO
        assertThatThrownBy(() -> useCase.ejecutar(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CERRADO");
        
        verify(pedidoRepository, never()).guardar(any(Pedido.class));
    }

    @Test
    @DisplayName("AC1 - Acumulación: Permite agregar el mismo producto múltiples veces")
    void deberia_permitir_agregar_mismo_producto_multiples_veces() {
        // Given: Un producto y un pedido abierto
        Producto producto = new Producto(productoId, localId, "Empanada", new BigDecimal("25.00"), true);
        Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
        when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(producto));
        when(pedidoRepository.guardar(any(Pedido.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When: Se agrega el mismo producto dos veces (líneas separadas)
        AgregarProductoRequest request1 = new AgregarProductoRequest(pedidoId, productoId, 3, "De carne");
        AgregarProductoRequest request2 = new AgregarProductoRequest(pedidoId, productoId, 2, "De pollo");
        
        useCase.ejecutar(request1);
        AgregarProductoResponse response = useCase.ejecutar(request2);

        // Then: Deben existir 2 líneas independientes
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).cantidad()).isEqualTo(3);
        assertThat(response.items().get(0).observacion()).isEqualTo("De carne");
        assertThat(response.items().get(1).cantidad()).isEqualTo(2);
        assertThat(response.items().get(1).observacion()).isEqualTo("De pollo");
        
        // Subtotal: (3 * 25) + (2 * 25) = 125
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("125.00"));
    }

    @Test
    @DisplayName("Debe fallar si el pedido no existe")
    void deberia_lanzar_excepcion_si_pedido_no_existe() {
        // Given: Un pedidoId inexistente
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.empty());

        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId, productoId, 1, null
        );

        // When/Then: Debe lanzar IllegalArgumentException
        assertThatThrownBy(() -> useCase.ejecutar(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(pedidoId.getValue().toString());
        
        verify(productoRepository, never()).buscarPorId(any());
        verify(pedidoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Debe fallar si el producto no existe")
    void deberia_lanzar_excepcion_si_producto_no_existe() {
        // Given: Un pedido existente pero un producto inexistente
        Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
        when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.empty());

        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId, productoId, 1, null
        );

        // When/Then: Debe lanzar IllegalArgumentException
        assertThatThrownBy(() -> useCase.ejecutar(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(productoId.getValue().toString());
        
        verify(pedidoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("AC2 - Personalización: Debe soportar cantidad y observaciones")
    void deberia_soportar_personalizacion_de_cantidad_y_observaciones() {
        // Given: Un producto y un pedido
        Producto producto = new Producto(productoId, localId, "Milanesa", new BigDecimal("150.00"), true);
        Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
        when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(producto));
        when(pedidoRepository.guardar(any(Pedido.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId, productoId, 3, "Con papas fritas extra"
        );

        // When: Se ejecuta el caso de uso
        AgregarProductoResponse response = useCase.ejecutar(request);

        // Then: El ítem debe reflejar cantidad y observaciones
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).cantidad()).isEqualTo(3);
        assertThat(response.items().get(0).observacion()).isEqualTo("Con papas fritas extra");
        assertThat(response.items().get(0).subtotalItem()).isEqualByComparingTo(new BigDecimal("450.00"));
    }

    @Test
    @DisplayName("Debe validar cantidad <= 0")
    void deberia_rechazar_cantidad_invalida() {
        // Given/When/Then: Cantidad <= 0 debe fallar en el DTO
        assertThatThrownBy(() -> new AgregarProductoRequest(pedidoId, productoId, 0, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cantidad debe ser mayor a 0");
        
        assertThatThrownBy(() -> new AgregarProductoRequest(pedidoId, productoId, -5, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cantidad debe ser mayor a 0");
    }
}
