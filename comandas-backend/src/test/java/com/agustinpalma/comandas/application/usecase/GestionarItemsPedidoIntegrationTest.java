package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.*;
import com.agustinpalma.comandas.domain.model.*;
import com.agustinpalma.comandas.domain.model.CriterioActivacion.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.*;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.infrastructure.config.TestClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Suite de Tests de Integración para HU-20 y HU-21:
 * Gestión Dinámica de Ítems en Pedido.
 * 
 * Valida la interacción con:
 * - HU-10: Promociones automáticas (recálculo tras cambios)
 * - HU-14: Descuento manual global (ajuste dinámico)
 * 
 * Escenarios cubiertos:
 * 1. Ciclos NxM: 2x1 con 2, 4 y 3 unidades
 * 2. Modificar cantidad y recálculo de promociones
 * 3. Rotura de combo: eliminar trigger
 * 4. Eliminar trigger de promoción
 * 5. Dinamismo global HU-14: eliminar producto recalcula descuento
 * 6. Idempotencia: misma cantidad no recalcula
 * 7. Cantidad 0 elimina ítem
 * 8. Validaciones de estado y parámetros
 * 
 * Principios:
 * - Tests contra BD real (H2)
 * - Transaccional (rollback automático)
 * - Clock fijo para determinismo
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestClockConfig.class)
@DisplayName("HU-20/HU-21: Gestión Dinámica de Ítems - Suite de Integración")
class GestionarItemsPedidoIntegrationTest {

    @Autowired
    private GestionarItemsPedidoUseCase gestionarItemsPedidoUseCase;

    @Autowired
    private AgregarProductoUseCase agregarProductoUseCase;

    @Autowired
    private AplicarDescuentoManualUseCase aplicarDescuentoManualUseCase;

    @Autowired
    private PromocionRepository promocionRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    // Datos base del test
    private LocalId localId;
    private Mesa mesa;
    private Pedido pedido;

    // Productos base
    private Producto hamburguesa;  // $2500
    private Producto papas;        // $800
    private Producto cerveza;      // $1500
    private Producto bebida;       // $600
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        // Configurar Local y Mesa
        localId = LocalId.generate();
        mesa = new Mesa(MesaId.generate(), localId, 5);
        mesaRepository.guardar(mesa);

        // Abrir mesa y crear pedido
        mesa.abrir();
        pedido = new Pedido(
            PedidoId.generate(),
            localId,
            mesa.getId(),
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );
        pedidoRepository.guardar(pedido);

        // Crear productos base
        hamburguesa = crearYGuardarProducto("Hamburguesa Completa", "2500.00");
        papas = crearYGuardarProducto("Papas Fritas", "800.00");
        cerveza = crearYGuardarProducto("Cerveza Artesanal", "1500.00");
        bebida = crearYGuardarProducto("Coca Cola 500ml", "600.00");

        // Usuario de auditoría
        usuarioId = UUID.randomUUID();
    }

    // =================================================
    // TEST 1: CICLOS NxM - 2x1 con variaciones de cantidad
    // =================================================

    @Nested
    @DisplayName("Test 1: Ciclos NxM - Recálculo de promoción 2x1 al modificar cantidad")
    class CiclosNxMTests {

        @Test
        @DisplayName("2 cervezas → paga 1 | Subir a 4 → paga 2 | Bajar a 3 → paga 2 y una de regalo")
        void ciclo_2x1_debe_recalcular_en_multiples_modificaciones() {
            // Crear promoción 2x1 en cervezas
            Promocion promo2x1 = crearPromocionNxM(
                "2x1 Cervezas", 2, 1, cerveza.getId().getValue(), 10
            );
            promocionRepository.guardar(promo2x1);

            // === PASO 1: Agregar 2 cervezas → paga 1 ===
            AgregarProductoResponse response1 = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), cerveza.getId(), 2, null)
            );

            var item1 = response1.items().get(0);
            assertThat(item1.cantidad()).isEqualTo(2);
            assertThat(item1.subtotalItem()).isEqualByComparingTo("3000.00");     // 2 × $1500
            assertThat(item1.descuentoTotal()).isEqualByComparingTo("1500.00");   // 1 gratis
            assertThat(item1.precioFinal()).isEqualByComparingTo("1500.00");      // paga 1
            assertThat(item1.tienePromocion()).isTrue();

            ItemPedidoId itemId = new ItemPedidoId(UUID.fromString(item1.itemId()));

            // === PASO 2: Subir a 4 → paga 2 (2 ciclos de 2x1) ===
            AgregarProductoResponse response2 = gestionarItemsPedidoUseCase.modificarCantidad(
                new ModificarCantidadItemRequest(pedido.getId(), itemId, 4)
            );

            var item2 = response2.items().get(0);
            assertThat(item2.cantidad()).isEqualTo(4);
            assertThat(item2.subtotalItem()).isEqualByComparingTo("6000.00");     // 4 × $1500
            assertThat(item2.descuentoTotal()).isEqualByComparingTo("3000.00");   // 2 gratis
            assertThat(item2.precioFinal()).isEqualByComparingTo("3000.00");      // paga 2
            assertThat(item2.tienePromocion()).isTrue();

            // === PASO 3: Bajar a 3 → 1 ciclo completo (paga 1) + 1 extra a precio full ===
            AgregarProductoResponse response3 = gestionarItemsPedidoUseCase.modificarCantidad(
                new ModificarCantidadItemRequest(pedido.getId(), itemId, 3)
            );

            var item3 = response3.items().get(0);
            assertThat(item3.cantidad()).isEqualTo(3);
            assertThat(item3.subtotalItem()).isEqualByComparingTo("4500.00");     // 3 × $1500
            assertThat(item3.descuentoTotal()).isEqualByComparingTo("1500.00");   // 1 gratis (1 ciclo de 2x1)
            assertThat(item3.precioFinal()).isEqualByComparingTo("3000.00");      // paga 2
            assertThat(item3.tienePromocion()).isTrue();
        }

        @Test
        @DisplayName("Bajar cantidad por debajo del mínimo NxM → pierde promoción")
        void bajar_cantidad_debajo_minimo_nxm_pierde_promocion() {
            // Crear promoción 3x2 en papas
            Promocion promo3x2 = crearPromocionNxM(
                "3x2 Papas", 3, 2, papas.getId().getValue(), 10
            );
            promocionRepository.guardar(promo3x2);

            // Agregar 3 papas → 3x2 activo
            AgregarProductoResponse response1 = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), papas.getId(), 3, null)
            );

            var item1 = response1.items().get(0);
            assertThat(item1.descuentoTotal()).isEqualByComparingTo("800.00");  // 1 gratis
            assertThat(item1.tienePromocion()).isTrue();

            ItemPedidoId itemId = new ItemPedidoId(UUID.fromString(item1.itemId()));

            // Bajar a 2 → ya no cumple 3x2
            AgregarProductoResponse response2 = gestionarItemsPedidoUseCase.modificarCantidad(
                new ModificarCantidadItemRequest(pedido.getId(), itemId, 2)
            );

            var item2 = response2.items().get(0);
            assertThat(item2.cantidad()).isEqualTo(2);
            assertThat(item2.descuentoTotal()).isEqualByComparingTo("0.00");
            assertThat(item2.tienePromocion()).isFalse();
            assertThat(item2.precioFinal()).isEqualByComparingTo("1600.00");  // 2 × $800 sin descuento
        }
    }

    // =================================================
    // TEST 2: MODIFICAR CANTIDAD & HU-10
    // =================================================

    @Nested
    @DisplayName("Test 2: Modificar cantidad dispara recálculo de promociones")
    class ModificarCantidadYPromociones {

        @Test
        @DisplayName("Pedido con 1 cerveza (sin promo) → modificar a 2 → aplica 2x1")
        void modificar_cantidad_activa_promocion() {
            // Crear promoción 2x1 en cervezas
            Promocion promo2x1 = crearPromocionNxM(
                "2x1 Cervezas", 2, 1, cerveza.getId().getValue(), 10
            );
            promocionRepository.guardar(promo2x1);

            // Agregar 1 cerveza (no alcanza para 2x1)
            AgregarProductoResponse response1 = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), cerveza.getId(), 1, null)
            );

            var item1 = response1.items().get(0);
            assertThat(item1.tienePromocion()).isFalse();
            assertThat(item1.precioFinal()).isEqualByComparingTo("1500.00");

            ItemPedidoId itemId = new ItemPedidoId(UUID.fromString(item1.itemId()));

            // Modificar a 2 → ahora califica para 2x1
            AgregarProductoResponse response2 = gestionarItemsPedidoUseCase.modificarCantidad(
                new ModificarCantidadItemRequest(pedido.getId(), itemId, 2)
            );

            var item2 = response2.items().get(0);
            assertThat(item2.cantidad()).isEqualTo(2);
            assertThat(item2.descuentoTotal()).isEqualByComparingTo("1500.00");  // 1 gratis
            assertThat(item2.precioFinal()).isEqualByComparingTo("1500.00");
            assertThat(item2.tienePromocion()).isTrue();
            assertThat(item2.nombrePromocion()).isEqualTo("2x1 Cervezas");
        }
    }

    // =================================================
    // TEST 3: ROTURA DE COMBO
    // =================================================

    @Nested
    @DisplayName("Test 3: Rotura de Combo - Eliminar trigger rompe descuento del target")
    class RoturaComboTests {

        @Test
        @DisplayName("Combo Hamburguesa+Papas → Eliminar hamburguesa → Papas vuelven a precio normal")
        void eliminar_trigger_rompe_combo() {
            // Crear combo: Hamburguesa (trigger) + Papas (target) con 50% en papas
            Promocion combo = crearPromocionCombo(
                "Combo Hamburguesa + Papas",
                hamburguesa.getId().getValue(),
                papas.getId().getValue(),
                new BigDecimal("50"),
                1,
                10
            );
            promocionRepository.guardar(combo);

            // Agregar hamburguesa (trigger)
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );
            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();

            // Agregar papas → deben tener descuento de combo
            AgregarProductoResponse response1 = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), papas.getId(), 1, null)
            );

            var itemPapas1 = response1.items().stream()
                .filter(i -> i.nombreProducto().equals("Papas Fritas"))
                .findFirst().orElseThrow();

            assertThat(itemPapas1.tienePromocion()).isTrue();
            assertThat(itemPapas1.descuentoTotal()).isEqualByComparingTo("400.00");  // 50% de $800
            assertThat(itemPapas1.precioFinal()).isEqualByComparingTo("400.00");

            // Encontrar el ID del ítem hamburguesa
            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            ItemPedidoId hamburguesaItemId = pedido.getItems().stream()
                .filter(i -> i.getNombreProducto().equals("Hamburguesa Completa"))
                .findFirst()
                .map(ItemPedido::getId)
                .orElseThrow();

            // Eliminar hamburguesa (trigger del combo)
            AgregarProductoResponse response2 = gestionarItemsPedidoUseCase.eliminarItem(
                new EliminarItemPedidoRequest(pedido.getId(), hamburguesaItemId)
            );

            // Papas deben volver a precio normal (sin combo)
            assertThat(response2.items()).hasSize(1);  // Solo papas
            var itemPapas2 = response2.items().get(0);
            assertThat(itemPapas2.nombreProducto()).isEqualTo("Papas Fritas");
            assertThat(itemPapas2.tienePromocion()).isFalse();
            assertThat(itemPapas2.descuentoTotal()).isEqualByComparingTo("0.00");
            assertThat(itemPapas2.precioFinal()).isEqualByComparingTo("800.00");
        }
    }

    // =================================================
    // TEST 4: ELIMINAR TRIGGER DE PROMO
    // =================================================

    @Nested
    @DisplayName("Test 4: Eliminar Trigger de Promoción - Target pierde descuento")
    class EliminarTriggerPromoTests {

        @Test
        @DisplayName("Pedido con combo Hamburguesa+Bebida → Eliminar hamburguesa → Bebida pierde descuento")
        void eliminar_trigger_hace_que_target_pierda_descuento() {
            // Crear combo: Hamburguesa (trigger) + Bebida (target) con 100% (regalo)
            Promocion comboRegalo = crearPromocionCombo(
                "Bebida de Regalo con Hamburguesa",
                hamburguesa.getId().getValue(),
                bebida.getId().getValue(),
                new BigDecimal("100"),
                1,
                10
            );
            promocionRepository.guardar(comboRegalo);

            // Agregar hamburguesa (trigger)
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );
            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();

            // Agregar bebida → regalo (100% descuento)
            AgregarProductoResponse response1 = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), bebida.getId(), 1, null)
            );

            var itemBebida1 = response1.items().stream()
                .filter(i -> i.nombreProducto().equals("Coca Cola 500ml"))
                .findFirst().orElseThrow();

            assertThat(itemBebida1.precioFinal()).isEqualByComparingTo("0.00");  // ¡Regalo!
            assertThat(itemBebida1.descuentoTotal()).isEqualByComparingTo("600.00");

            // Encontrar ID de hamburguesa
            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            ItemPedidoId hamburguesaItemId = pedido.getItems().stream()
                .filter(i -> i.getNombreProducto().equals("Hamburguesa Completa"))
                .findFirst()
                .map(ItemPedido::getId)
                .orElseThrow();

            // Eliminar hamburguesa → bebida pierde el regalo
            AgregarProductoResponse response2 = gestionarItemsPedidoUseCase.eliminarItem(
                new EliminarItemPedidoRequest(pedido.getId(), hamburguesaItemId)
            );

            assertThat(response2.items()).hasSize(1);
            var itemBebida2 = response2.items().get(0);
            assertThat(itemBebida2.nombreProducto()).isEqualTo("Coca Cola 500ml");
            assertThat(itemBebida2.tienePromocion()).isFalse();
            assertThat(itemBebida2.precioFinal()).isEqualByComparingTo("600.00");  // Precio full
            assertThat(itemBebida2.descuentoTotal()).isEqualByComparingTo("0.00");
        }
    }

    // =================================================
    // TEST 5: DINAMISMO GLOBAL HU-14
    // =================================================

    @Nested
    @DisplayName("Test 5: Dinamismo Global HU-14 - Eliminar producto recalcula descuento global")
    class DinamismoGlobalTests {

        @Test
        @DisplayName("Pedido $3300 con 10% desc → Eliminar $800 → Subtotal $2500 → Descuento $250")
        void eliminar_producto_recalcula_descuento_global() {
            // Agregar hamburguesa ($2500) + papas ($800) = $3300
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), papas.getId(), 1, null)
            );

            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            assertThat(pedido.calcularSubtotalItems()).isEqualByComparingTo("3300.00");

            // Aplicar 10% descuento global
            aplicarDescuentoManualUseCase.ejecutar(
                new AplicarDescuentoManualRequest(
                    pedido.getId(),
                    null,  // Global
                    new BigDecimal("10"),
                    "Descuento test",
                    usuarioId
                )
            );

            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            assertThat(pedido.calcularTotal()).isEqualByComparingTo("2970.00");  // $3300 - 10%
            assertThat(pedido.calcularMontoDescuentoGlobal()).isEqualByComparingTo("330.00");

            // Encontrar el ID del ítem papas
            ItemPedidoId papasItemId = pedido.getItems().stream()
                .filter(i -> i.getNombreProducto().equals("Papas Fritas"))
                .findFirst()
                .map(ItemPedido::getId)
                .orElseThrow();

            // Eliminar papas ($800)
            AgregarProductoResponse response = gestionarItemsPedidoUseCase.eliminarItem(
                new EliminarItemPedidoRequest(pedido.getId(), papasItemId)
            );

            // Nuevo subtotal: $2500
            // Nuevo descuento global: 10% de $2500 = $250
            // Nuevo total: $2500 - $250 = $2250
            assertThat(response.subtotal()).isEqualByComparingTo("2500.00");
            assertThat(response.totalDescuentos()).isEqualByComparingTo("250.00");
            assertThat(response.total()).isEqualByComparingTo("2250.00");
        }

        @Test
        @DisplayName("Modificar cantidad con descuento global → recalcula dinámicamente")
        void modificar_cantidad_recalcula_descuento_global() {
            // Agregar 2 hamburguesas ($5000)
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 2, null)
            );

            // Aplicar 10% descuento global
            aplicarDescuentoManualUseCase.ejecutar(
                new AplicarDescuentoManualRequest(
                    pedido.getId(),
                    null,
                    new BigDecimal("10"),
                    "Descuento test",
                    usuarioId
                )
            );

            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            assertThat(pedido.calcularTotal()).isEqualByComparingTo("4500.00");  // $5000 - 10%

            // Encontrar ID del ítem
            ItemPedidoId itemId = pedido.getItems().get(0).getId();

            // Modificar a 1 hamburguesa ($2500)
            AgregarProductoResponse response = gestionarItemsPedidoUseCase.modificarCantidad(
                new ModificarCantidadItemRequest(pedido.getId(), itemId, 1)
            );

            // Nuevo subtotal: $2500
            // Descuento global: 10% de $2500 = $250
            // Total: $2500 - $250 = $2250
            assertThat(response.subtotal()).isEqualByComparingTo("2500.00");
            assertThat(response.totalDescuentos()).isEqualByComparingTo("250.00");
            assertThat(response.total()).isEqualByComparingTo("2250.00");
        }
    }

    // =================================================
    // TEST 6: IDEMPOTENCIA
    // =================================================

    @Nested
    @DisplayName("Test 6: Idempotencia - Misma cantidad no recalcula")
    class IdempotenciaTests {

        @Test
        @DisplayName("Modificar con misma cantidad → operación idempotente")
        void misma_cantidad_no_modifica_pedido() {
            // Agregar 2 cervezas
            AgregarProductoResponse response1 = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), cerveza.getId(), 2, null)
            );

            var item1 = response1.items().get(0);
            ItemPedidoId itemId = new ItemPedidoId(UUID.fromString(item1.itemId()));

            // Modificar con misma cantidad (2)
            AgregarProductoResponse response2 = gestionarItemsPedidoUseCase.modificarCantidad(
                new ModificarCantidadItemRequest(pedido.getId(), itemId, 2)
            );

            // Debe ser idempotente: mismos valores
            var item2 = response2.items().get(0);
            assertThat(item2.cantidad()).isEqualTo(2);
            assertThat(item2.precioFinal()).isEqualByComparingTo(item1.precioFinal());
        }
    }

    // =================================================
    // TEST 7: CANTIDAD 0 ELIMINA ÍTEM
    // =================================================

    @Nested
    @DisplayName("Test 7: Cantidad 0 elimina el ítem")
    class CantidadCeroTests {

        @Test
        @DisplayName("Modificar cantidad a 0 → elimina el ítem del pedido")
        void cantidad_cero_elimina_item() {
            // Agregar hamburguesa y papas
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), papas.getId(), 1, null)
            );

            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            assertThat(pedido.getItems()).hasSize(2);

            // Encontrar ID de hamburguesa
            ItemPedidoId hamburguesaItemId = pedido.getItems().stream()
                .filter(i -> i.getNombreProducto().equals("Hamburguesa Completa"))
                .findFirst()
                .map(ItemPedido::getId)
                .orElseThrow();

            // Modificar cantidad a 0 → debe eliminar
            AgregarProductoResponse response = gestionarItemsPedidoUseCase.modificarCantidad(
                new ModificarCantidadItemRequest(pedido.getId(), hamburguesaItemId, 0)
            );

            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).nombreProducto()).isEqualTo("Papas Fritas");
            assertThat(response.subtotal()).isEqualByComparingTo("800.00");
        }
    }

    // =================================================
    // TEST 8: VALIDACIONES
    // =================================================

    @Nested
    @DisplayName("Test 8: Validaciones de estado y parámetros")
    class ValidacionesTests {

        @Test
        @DisplayName("Modificar ítem en pedido CERRADO → IllegalStateException")
        void modificar_item_pedido_cerrado_lanza_excepcion() {
            // Agregar ítem y cerrar pedido
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );

            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            ItemPedidoId itemId = pedido.getItems().get(0).getId();

            // Cerrar pedido
            pedido.finalizar(MedioPago.EFECTIVO, LocalDateTime.now());
            pedidoRepository.guardar(pedido);

            // Intentar modificar → debe fallar
            assertThatThrownBy(() ->
                gestionarItemsPedidoUseCase.modificarCantidad(
                    new ModificarCantidadItemRequest(pedido.getId(), itemId, 3)
                )
            ).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Eliminar ítem en pedido CERRADO → IllegalStateException")
        void eliminar_item_pedido_cerrado_lanza_excepcion() {
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );

            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            ItemPedidoId itemId = pedido.getItems().get(0).getId();

            pedido.finalizar(MedioPago.EFECTIVO, LocalDateTime.now());
            pedidoRepository.guardar(pedido);

            assertThatThrownBy(() ->
                gestionarItemsPedidoUseCase.eliminarItem(
                    new EliminarItemPedidoRequest(pedido.getId(), itemId)
                )
            ).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Cantidad negativa → IllegalArgumentException")
        void cantidad_negativa_lanza_excepcion() {
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );

            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            ItemPedidoId itemId = pedido.getItems().get(0).getId();

            assertThatThrownBy(() ->
                gestionarItemsPedidoUseCase.modificarCantidad(
                    new ModificarCantidadItemRequest(pedido.getId(), itemId, -1)
                )
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Ítem inexistente → IllegalArgumentException")
        void item_inexistente_lanza_excepcion() {
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );

            ItemPedidoId itemInexistente = ItemPedidoId.generate();

            assertThatThrownBy(() ->
                gestionarItemsPedidoUseCase.modificarCantidad(
                    new ModificarCantidadItemRequest(pedido.getId(), itemInexistente, 3)
                )
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Pedido inexistente → IllegalArgumentException")
        void pedido_inexistente_lanza_excepcion() {
            PedidoId pedidoInexistente = PedidoId.generate();
            ItemPedidoId itemId = ItemPedidoId.generate();

            assertThatThrownBy(() ->
                gestionarItemsPedidoUseCase.eliminarItem(
                    new EliminarItemPedidoRequest(pedidoInexistente, itemId)
                )
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =================================================
    // TEST 9: ELIMINAR ÚLTIMO ÍTEM
    // =================================================

    @Nested
    @DisplayName("Test 9: Eliminar último ítem deja pedido vacío")
    class EliminarUltimoItemTests {

        @Test
        @DisplayName("Eliminar el único ítem → pedido vacío con total $0")
        void eliminar_ultimo_item_deja_pedido_vacio() {
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );

            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            ItemPedidoId itemId = pedido.getItems().get(0).getId();

            AgregarProductoResponse response = gestionarItemsPedidoUseCase.eliminarItem(
                new EliminarItemPedidoRequest(pedido.getId(), itemId)
            );

            assertThat(response.items()).isEmpty();
            assertThat(response.subtotal()).isEqualByComparingTo("0.00");
            assertThat(response.total()).isEqualByComparingTo("0.00");
            assertThat(response.totalDescuentos()).isEqualByComparingTo("0.00");
        }
    }

    // =================================================
    // HELPERS: Métodos de construcción
    // =================================================

    private Producto crearYGuardarProducto(String nombre, String precio) {
        Producto producto = new Producto(
            ProductoId.generate(),
            localId,
            nombre,
            new BigDecimal(precio),
            true,
            "#FF0000"
        );
        productoRepository.guardar(producto);
        return producto;
    }

    // Fecha de referencia alineada con el clock fijo de test (2026-02-06)
    private static final LocalDate FECHA_TEST = LocalDate.of(2026, 2, 6);

    private Promocion crearPromocionNxM(String nombre, int llevas, int pagas, UUID productoId, int prioridad) {
        EstrategiaPromocion estrategia = new CantidadFija(llevas, pagas);
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            FECHA_TEST.minusDays(1),
            FECHA_TEST.plusDays(30)
        );

        Promocion promo = new Promocion(
            PromocionId.generate(), localId, nombre, "Descripción NxM",
            prioridad, EstadoPromocion.ACTIVA, estrategia, List.of(trigger)
        );

        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        return promo;
    }

    private Promocion crearPromocionCombo(
            String nombre, UUID productoTriggerId, UUID productoTargetId,
            BigDecimal porcentajeBeneficio, int cantidadMinimaTrigger, int prioridad
    ) {
        EstrategiaPromocion estrategia = new ComboCondicional(cantidadMinimaTrigger, porcentajeBeneficio);
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            FECHA_TEST.minusDays(1),
            FECHA_TEST.plusDays(30)
        );

        Promocion promo = new Promocion(
            PromocionId.generate(), localId, nombre, "Descripción combo",
            prioridad, EstadoPromocion.ACTIVA, estrategia, List.of(trigger)
        );

        ItemPromocion itemTrigger = ItemPromocion.productoTrigger(productoTriggerId);
        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoTargetId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTrigger, itemTarget)));
        return promo;
    }
}
