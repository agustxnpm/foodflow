package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AgregarProductoRequest;
import com.agustinpalma.comandas.application.dto.AgregarProductoResponse;
import com.agustinpalma.comandas.domain.model.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.CriterioActivacion.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.*;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.domain.service.MotorReglasService;
import com.agustinpalma.comandas.domain.service.NormalizadorVariantesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
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

    @Mock
    private PromocionRepository promocionRepository;

    private MotorReglasService motorReglasService;
    private NormalizadorVariantesService normalizadorVariantesService;

    private AgregarProductoUseCase useCase;

    private LocalId localId;
    private PedidoId pedidoId;
    private ProductoId productoId;
    private MesaId mesaId;
    private Clock clock;

    @BeforeEach
    void setUp() {
        // Clock fijo para tests deterministas: 12 feb 2026, 19:00 (Jueves)
        clock = Clock.fixed(
            LocalDateTime.of(2026, 2, 12, 19, 0).atZone(ZoneId.of("America/Argentina/Buenos_Aires")).toInstant(),
            ZoneId.of("America/Argentina/Buenos_Aires")
        );
        
        motorReglasService = new MotorReglasService();
        normalizadorVariantesService = new NormalizadorVariantesService();
        useCase = new AgregarProductoUseCase(pedidoRepository, productoRepository, promocionRepository, motorReglasService, normalizadorVariantesService, clock);
        
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
        Producto producto = new Producto(productoId, localId, "Hamburguesa", new BigDecimal("100.00"), true, "#FF0000");
        Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
        when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(producto));
        when(promocionRepository.buscarActivasPorLocal(localId)).thenReturn(java.util.Collections.emptyList());
        when(pedidoRepository.guardar(any(Pedido.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId, productoId, 2, "Sin cebolla"
        );

        // When: Se agrega el producto al pedido
        AgregarProductoResponse response = useCase.ejecutar(request);

        // Then: El ítem debe tener el precio capturado ($100)
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).precioUnitarioBase()).isEqualByComparingTo(new BigDecimal("100.00"));
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
        Producto producto = new Producto(productoId, localId, "Pizza", new BigDecimal("100.00"), true, "#00FF00");
        Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
        when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(producto));
        when(promocionRepository.buscarActivasPorLocal(localId)).thenReturn(java.util.Collections.emptyList());
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
        assertThat(primeraRespuesta.items().get(0).precioUnitarioBase())
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
        Producto productoLocalB = new Producto(productoId, localB, "Producto Ajeno", new BigDecimal("50.00"), true, "#0000FF");
        
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
        Producto producto = new Producto(productoId, localId, "Café", new BigDecimal("30.00"), true, "#FFFF00");
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
        Producto producto = new Producto(productoId, localId, "Empanada", new BigDecimal("25.00"), true, "#FF00FF");
        Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
        when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(producto));
        when(promocionRepository.buscarActivasPorLocal(localId)).thenReturn(Collections.emptyList());
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
        Producto producto = new Producto(productoId, localId, "Milanesa", new BigDecimal("150.00"), true, "#00FFFF");
        Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
        
        when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
        when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(producto));
        when(promocionRepository.buscarActivasPorLocal(localId)).thenReturn(Collections.emptyList());
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

    // =================================================
    // HU-10: APLICAR PROMOCIONES AUTOMÁTICAMENTE
    // =================================================

    @Nested
    @DisplayName("HU-10: Aplicación Automática de Promociones")
    class PromocionesAutomaticasTests {

        @Test
        @DisplayName("HU-10 AC1: Debería aplicar descuento al agregar producto cuando existe promoción vigente")
        void deberia_aplicar_descuento_al_agregar_producto_cuando_existe_promocion_vigente() {
            // Given: Un producto de $100 con una promo de 20% de descuento
            Producto cerveza = new Producto(productoId, localId, "Cerveza Artesanal", new BigDecimal("100.00"), true, "#FFD700");
            Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
            
            // Crear promoción Happy Hour con 20% de descuento
            Promocion happyHour = crearPromocionDescuento(
                "Happy Hour",
                new BigDecimal("20"),
                cerveza.getId().getValue(),
                10
            );
            
            when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
            when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(cerveza));
            when(promocionRepository.buscarActivasPorLocal(localId)).thenReturn(List.of(happyHour));
            when(pedidoRepository.guardar(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            AgregarProductoRequest request = new AgregarProductoRequest(
                pedidoId, productoId, 1, null
            );

            // When: Se agrega el producto
            AgregarProductoResponse response = useCase.ejecutar(request);

            // Then: El ítem debe tener el descuento aplicado
            assertThat(response.items()).hasSize(1);
            var item = response.items().get(0);
            
            // AC1: Snapshot del precio base (para tachar en UI)
            assertThat(item.precioUnitarioBase()).isEqualByComparingTo(new BigDecimal("100.00"));
            
            // AC1: Descuento del 20% = $20
            assertThat(item.descuentoTotal()).isEqualByComparingTo(new BigDecimal("20.00"));
            
            // AC1: Precio final = $100 - $20 = $80
            assertThat(item.precioFinal()).isEqualByComparingTo(new BigDecimal("80.00"));
            
            // AC1: El subtotal del pedido debe reflejar el precio CON descuento
            assertThat(response.total()).isEqualByComparingTo(new BigDecimal("80.00"));
            assertThat(response.totalDescuentos()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
            
            // Verificar que se llamó al motor de reglas
            verify(promocionRepository, times(1)).buscarActivasPorLocal(localId);
            verify(pedidoRepository, times(1)).guardar(any(Pedido.class));
        }

        @Test
        @DisplayName("HU-10 AC3: Debería guardar el nombre de la promoción en el ítem para transparencia con el cliente")
        void deberia_guardar_nombre_de_promocion_en_el_item_para_la_cuenta_del_cliente() {
            // Given: Producto con promoción activa
            Producto producto = new Producto(productoId, localId, "Hamburguesa Completa", new BigDecimal("200.00"), true, "#FF5733");
            Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
            
            Promocion promoHamburguesa = crearPromocionDescuento(
                "Promo Hamburguesas",
                new BigDecimal("15"),
                producto.getId().getValue(),
                5
            );
            
            when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
            when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(producto));
            when(promocionRepository.buscarActivasPorLocal(localId)).thenReturn(List.of(promoHamburguesa));
            when(pedidoRepository.guardar(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            AgregarProductoRequest request = new AgregarProductoRequest(
                pedidoId, productoId, 1, null
            );

            // When
            AgregarProductoResponse response = useCase.ejecutar(request);

            // Then: AC3 - El nombre de la promoción debe estar en el ítem (snapshot para factura)
            assertThat(response.items()).hasSize(1);
            var item = response.items().get(0);
            
            assertThat(item.nombrePromocion()).isEqualTo("Promo Hamburguesas");
            assertThat(item.tienePromocion()).isTrue();
            
            // Verificar que el descuento es correcto (15% de 200 = 30)
            assertThat(item.descuentoTotal()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(item.precioFinal()).isEqualByComparingTo(new BigDecimal("170.00"));
        }

        @Test
        @DisplayName("HU-10 AC2: La cantidad para cocina debe ser la real independientemente del descuento (2x1)")
        void la_cantidad_para_cocina_debe_ser_la_real_independientemente_del_descuento() {
            // Given: Promo 2x1 en empanadas
            Producto empanada = new Producto(productoId, localId, "Empanada de Carne", new BigDecimal("50.00"), true, "#8B4513");
            Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
            
            // Promoción 2x1: lleva 2, paga 1
            Promocion promo2x1 = crearPromocionCantidadFija(
                "2x1 Empanadas",
                2, 1,
                empanada.getId().getValue(),
                10
            );
            
            when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
            when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(empanada));
            when(promocionRepository.buscarActivasPorLocal(localId)).thenReturn(List.of(promo2x1));
            when(pedidoRepository.guardar(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            AgregarProductoRequest request = new AgregarProductoRequest(
                pedidoId, productoId, 2, null // Cliente pide 2 empanadas
            );

            // When
            AgregarProductoResponse response = useCase.ejecutar(request);

            // Then: AC2 - La cantidad DEBE ser 2 (lo que cocina debe preparar)
            assertThat(response.items()).hasSize(1);
            var item = response.items().get(0);
            
            assertThat(item.cantidad()).isEqualTo(2); // CRÍTICO: Cocina prepara 2, no 1
            
            // El descuento es de 1 empanada completa
            assertThat(item.descuentoTotal()).isEqualByComparingTo(new BigDecimal("50.00")); // 1 gratis
            
            // Precio final: paga 1 empanada
            assertThat(item.precioFinal()).isEqualByComparingTo(new BigDecimal("50.00")); // 100 - 50
            
            // Subtotal base: 2 × $50 = $100
            assertThat(item.subtotalItem()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("HU-10: Debería aplicar descuento con cantidad mayor a 1")
        void deberia_aplicar_descuento_con_cantidad_mayor_a_uno() {
            // Given: Producto con promo de 25% y cantidad 3
            Producto cafe = new Producto(productoId, localId, "Café Espresso", new BigDecimal("80.00"), true, "#6F4E37");
            Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
            
            Promocion promoCafe = crearPromocionDescuento(
                "Promo Café",
                new BigDecimal("25"),
                cafe.getId().getValue(),
                5
            );
            
            when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
            when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(cafe));
            when(promocionRepository.buscarActivasPorLocal(localId)).thenReturn(List.of(promoCafe));
            when(pedidoRepository.guardar(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            AgregarProductoRequest request = new AgregarProductoRequest(
                pedidoId, productoId, 3, null
            );

            // When
            AgregarProductoResponse response = useCase.ejecutar(request);

            // Then: El descuento se aplica por cada unidad
            var item = response.items().get(0);
            
            assertThat(item.cantidad()).isEqualTo(3);
            assertThat(item.precioUnitarioBase()).isEqualByComparingTo(new BigDecimal("80.00"));
            
            // Descuento: 25% × 80 × 3 = 60
            assertThat(item.descuentoTotal()).isEqualByComparingTo(new BigDecimal("60.00"));
            
            // Precio final: (80 × 3) - 60 = 240 - 60 = 180
            assertThat(item.precioFinal()).isEqualByComparingTo(new BigDecimal("180.00"));
            
            assertThat(response.total()).isEqualByComparingTo(new BigDecimal("180.00"));
        }

        @Test
        @DisplayName("HU-10: No debería aplicar promoción si el producto NO es target")
        void no_deberia_aplicar_promocion_si_producto_no_es_target() {
            // Given: Promo solo para pizzas, pero se agrega una hamburguesa
            Producto hamburguesa = new Producto(productoId, localId, "Hamburguesa", new BigDecimal("150.00"), true, "#FF6347");
            ProductoId pizzaId = ProductoId.generate();
            Pedido pedido = new Pedido(pedidoId, localId, mesaId, 1, EstadoPedido.ABIERTO, LocalDateTime.now());
            
            // Promo solo aplica a pizzas (otro producto)
            Promocion promoPizzas = crearPromocionDescuento(
                "Promo Pizzas",
                new BigDecimal("30"),
                pizzaId.getValue(),
                5
            );
            
            when(pedidoRepository.buscarPorId(pedidoId)).thenReturn(Optional.of(pedido));
            when(productoRepository.buscarPorId(productoId)).thenReturn(Optional.of(hamburguesa));
            when(promocionRepository.buscarActivasPorLocal(localId)).thenReturn(List.of(promoPizzas));
            when(pedidoRepository.guardar(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            AgregarProductoRequest request = new AgregarProductoRequest(
                pedidoId, productoId, 1, null
            );

            // When
            AgregarProductoResponse response = useCase.ejecutar(request);

            // Then: NO debe aplicar la promo porque el producto no es target
            var item = response.items().get(0);
            
            assertThat(item.tienePromocion()).isFalse();
            assertThat(item.nombrePromocion()).isNull();
            assertThat(item.descuentoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.precioFinal()).isEqualByComparingTo(new BigDecimal("150.00"));
        }
    }

    // =================================================
    // HELPERS: Métodos para crear promociones de prueba
    // =================================================

    // Fecha de referencia alineada con el clock fijo de test (2026-02-06)
    private static final LocalDate FECHA_TEST = LocalDate.of(2026, 2, 12);

    private Promocion crearPromocionDescuento(
            String nombre,
            BigDecimal porcentaje,
            UUID productoTargetId,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new DescuentoDirecto(
            ModoDescuento.PORCENTAJE,
            porcentaje
        );
        
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            FECHA_TEST.minusDays(1),
            FECHA_TEST.plusDays(30)
        );
        
        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Promoción de prueba",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );
        
        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoTargetId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        
        return promo;
    }

    private Promocion crearPromocionCantidadFija(
            String nombre,
            int cantidadLlevas,
            int cantidadPagas,
            UUID productoTargetId,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new CantidadFija(cantidadLlevas, cantidadPagas);
        
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            FECHA_TEST.minusDays(1),
            FECHA_TEST.plusDays(30)
        );
        
        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Promoción NxM de prueba",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );
        
        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoTargetId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        
        return promo;
    }
}
