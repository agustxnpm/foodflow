package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainEnums.ModoDescuento;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios de dominio puro para ItemPedido.
 *
 * Cubren la "Regla de Oro" del dominio:
 *   El total se calcula desde los ítems base + descuentos acumulables, nunca al revés.
 *
 * Escenarios validados:
 * - Cálculo de subtotal (solo base, sin extras)
 * - Cálculo de subtotal de línea (base + extras)
 * - Cálculo de precio final sin descuentos
 * - Cálculo con promoción automática (snapshot)
 * - Cálculo con descuento manual PORCENTAJE
 * - Cálculo con descuento manual MONTO_FIJO
 * - Cálculo con ambos descuentos (promoción + manual) — el orden importa
 * - Los extras NUNCA participan en descuentos (invariante crítico)
 * - Restricciones de construcción (cantidad, precio)
 * - Delta de envío a cocina (HU-29)
 *
 * Sin Spring. Sin base de datos. Dominio puro.
 */
class ItemPedidoTest {

    // ============================================================
    // Fixtures
    // ============================================================

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 4, 21, 10, 0);

    private PedidoId pedidoId;
    private Producto productoPizza;       // $1000 c/u
    private Producto productoExtra;       // $200 c/u, esExtra = true

    @BeforeEach
    void setUp() {
        pedidoId = PedidoId.generate();

        productoPizza = new Producto(
            ProductoId.generate(),
            LocalId.generate(),
            "Pizza Muzza",
            new BigDecimal("1000"),
            true,
            "#FF0000"
        );

        // Construimos el extra usando el constructor completo para marcar esExtra = true
        productoExtra = new Producto(
            ProductoId.generate(),
            LocalId.generate(),
            "Huevo",
            new BigDecimal("200"),
            true,
            "#FFFFFF",
            null,      // grupoVarianteId
            true,      // esExtra
            false,     // esModificadorEstructural
            null,      // cantidadDiscosCarne
            null,      // categoriaId
            false,     // permiteExtras
            false,     // requiereConfiguracion
            100,       // stockActual
            false      // controlaStock
        );
    }

    // ============================================================
    // Helpers de construcción
    // ============================================================

    /**
     * Crea un ítem limpio (sin descuentos, sin extras) usando el factory method.
     */
    private ItemPedido itemSimple(int cantidad) {
        return ItemPedido.crearDesdeProducto(
            ItemPedidoId.generate(), pedidoId, productoPizza, cantidad, null
        );
    }

    private DescuentoManual descuentoPorcentaje(String valor) {
        return new DescuentoManual(ModoDescuento.PORCENTAJE, new BigDecimal(valor), "Test", USUARIO_ID, FECHA);
    }

    private DescuentoManual descuentoMontoFijo(String valor) {
        return new DescuentoManual(ModoDescuento.MONTO_FIJO, new BigDecimal(valor), "Test", USUARIO_ID, FECHA);
    }

    // ============================================================
    // calcularSubtotal — precio base × cantidad (sin extras)
    // ============================================================

    @Test
    void deberia_calcular_subtotal_base_sin_descuento_ni_extras() {
        // Given
        ItemPedido item = itemSimple(3);

        // When
        BigDecimal subtotal = item.calcularSubtotal();

        // Then — 3 × $1000 = $3000
        assertEquals(new BigDecimal("3000"), subtotal);
    }

    @Test
    void deberia_calcular_subtotal_base_para_cantidad_uno() {
        // Given
        ItemPedido item = itemSimple(1);

        // When
        BigDecimal subtotal = item.calcularSubtotal();

        // Then — 1 × $1000 = $1000
        assertEquals(new BigDecimal("1000"), subtotal);
    }

    // ============================================================
    // calcularSubtotalLinea — (base + extras) × cantidad
    // ============================================================

    @Test
    void deberia_calcular_subtotal_linea_incluyendo_extras() {
        // Given — 2 Pizzas + Huevo ($200)
        ExtraPedido extra = ExtraPedido.crearDesdeProducto(productoExtra);
        ItemPedido item = ItemPedido.crearConExtras(
            ItemPedidoId.generate(), pedidoId, productoPizza, 2, null, List.of(extra)
        );

        // When
        BigDecimal subtotalLinea = item.calcularSubtotalLinea();

        // Then — 2 × ($1000 + $200) = $2400
        assertEquals(new BigDecimal("2400"), subtotalLinea);
    }

    @Test
    void deberia_calcular_subtotal_linea_igual_a_subtotal_cuando_no_hay_extras() {
        // Given
        ItemPedido item = itemSimple(2);

        // When / Then — Sin extras, ambos métodos deben devolver el mismo valor
        assertEquals(item.calcularSubtotal(), item.calcularSubtotalLinea());
    }

    // ============================================================
    // calcularPrecioFinal — sin descuentos
    // ============================================================

    @Test
    void deberia_calcular_precio_final_igual_al_subtotal_cuando_no_hay_descuentos() {
        // Given
        ItemPedido item = itemSimple(2);

        // When
        BigDecimal precioFinal = item.calcularPrecioFinal();

        // Then — 2 × $1000 = $2000, sin descuentos
        assertEquals(new BigDecimal("2000"), precioFinal);
    }

    // ============================================================
    // calcularPrecioFinal — con promoción automática (HU-10)
    // ============================================================

    @Test
    void deberia_calcular_precio_final_con_promocion_automatica_snapshot() {
        // Given — 2 Pizzas con $500 de descuento por promoción (ej: 2x1)
        ItemPedido item = ItemPedido.crearConPromocion(
            ItemPedidoId.generate(), pedidoId, productoPizza, 2, null,
            new BigDecimal("500"), "Promo 2x1", UUID.randomUUID()
        );

        // When
        BigDecimal precioFinal = item.calcularPrecioFinal();

        // Then — (2 × $1000) - $500 = $1500
        assertEquals(new BigDecimal("1500"), precioFinal);
    }

    // ============================================================
    // calcularPrecioFinal — con descuento manual PORCENTAJE (HU-14)
    // ============================================================

    @Test
    void deberia_calcular_precio_final_con_descuento_manual_porcentaje() {
        // Given — 2 Pizzas, 10% de descuento manual
        ItemPedido item = itemSimple(2);
        item.aplicarDescuentoManual(descuentoPorcentaje("10"));

        // When
        BigDecimal precioFinal = item.calcularPrecioFinal();

        // Then — (2 × $1000) - 10% de $2000 = $2000 - $200 = $1800
        assertEquals(new BigDecimal("1800.00"), precioFinal);
    }

    @Test
    void deberia_calcular_precio_final_con_descuento_manual_monto_fijo() {
        // Given — 2 Pizzas, descuento fijo de $300
        ItemPedido item = itemSimple(2);
        item.aplicarDescuentoManual(descuentoMontoFijo("300"));

        // When
        BigDecimal precioFinal = item.calcularPrecioFinal();

        // Then — $2000 - $300 = $1700
        assertEquals(new BigDecimal("1700"), precioFinal);
    }

    // ============================================================
    // calcularPrecioFinal — cascada de descuentos (Regla de Oro)
    // El descuento manual se calcula SOBRE EL REMANENTE después de la promoción
    // ============================================================

    @Test
    void deberia_calcular_precio_final_aplicando_manual_sobre_remanente_despues_de_promo() {
        // Given — 2 Pizzas, $500 promo automática + 10% manual sobre remanente
        ItemPedido item = ItemPedido.crearConPromocion(
            ItemPedidoId.generate(), pedidoId, productoPizza, 2, null,
            new BigDecimal("500"), "Promo verano", UUID.randomUUID()
        );
        item.aplicarDescuentoManual(descuentoPorcentaje("10"));

        // When
        BigDecimal precioFinal = item.calcularPrecioFinal();

        // Then:
        // precioBase = 2 × $1000 = $2000
        // remanenteDespuesPromo = $2000 - $500 = $1500
        // descuentoManual = 10% de $1500 = $150
        // precioFinal = $1500 - $150 = $1350
        assertEquals(new BigDecimal("1350.00"), precioFinal);
    }

    @Test
    void deberia_calcular_precio_final_con_promo_y_descuento_manual_monto_fijo() {
        // Given — 1 Pizza, $200 promo + $100 monto fijo manual
        ItemPedido item = ItemPedido.crearConPromocion(
            ItemPedidoId.generate(), pedidoId, productoPizza, 1, null,
            new BigDecimal("200"), "Promo martes", UUID.randomUUID()
        );
        item.aplicarDescuentoManual(descuentoMontoFijo("100"));

        // When
        BigDecimal precioFinal = item.calcularPrecioFinal();

        // Then:
        // base = $1000, remanenteDespuesPromo = $800
        // descuentoManual = $100 (cabe dentro del remanente)
        // precioFinal = $800 - $100 = $700
        assertEquals(new BigDecimal("700"), precioFinal);
    }

    // ============================================================
    // Invariante crítico: los extras NUNCA se descuentan (HU-05.1)
    // ============================================================

    @Test
    void deberia_no_descontar_extras_al_aplicar_promocion_automatica() {
        // Given — 2 Pizzas + Huevo ($200 extra), con $500 de promo sobre la base
        ExtraPedido extra = ExtraPedido.crearDesdeProducto(productoExtra);
        ItemPedido item = ItemPedido.crearCompleto(
            ItemPedidoId.generate(), pedidoId, productoPizza, 2, null,
            new BigDecimal("500"), "2x1 Pizzas", UUID.randomUUID(), List.of(extra)
        );

        // When
        BigDecimal precioFinal = item.calcularPrecioFinal();

        // Then:
        // precioBaseTotal = 2 × $1000 = $2000
        // precioExtrasTotal = $200 × 2 = $400 (extras × cantidad)
        // remanenteDespuesPromo = $2000 - $500 = $1500
        // precioFinal = $1500 + $400 (extras sin descuento) = $1900
        assertEquals(new BigDecimal("1900"), precioFinal);
    }

    @Test
    void deberia_no_descontar_extras_al_aplicar_descuento_manual_porcentaje() {
        // Given — 1 Pizza + Huevo ($200 extra), 10% descuento manual
        ExtraPedido extra = ExtraPedido.crearDesdeProducto(productoExtra);
        ItemPedido item = ItemPedido.crearConExtras(
            ItemPedidoId.generate(), pedidoId, productoPizza, 1, null, List.of(extra)
        );
        item.aplicarDescuentoManual(descuentoPorcentaje("10"));

        // When
        BigDecimal precioFinal = item.calcularPrecioFinal();

        // Then:
        // precioBase = $1000, descuentoManual = 10% de $1000 = $100
        // extras = $200 (no se descuentan)
        // precioFinal = ($1000 - $100) + $200 = $1100
        assertEquals(new BigDecimal("1100.00"), precioFinal);
    }

    // ============================================================
    // calcularMontoDescuentoManual
    // ============================================================

    @Test
    void deberia_retornar_cero_como_monto_descuento_manual_cuando_no_hay_descuento() {
        // Given
        ItemPedido item = itemSimple(1);

        // When
        BigDecimal monto = item.calcularMontoDescuentoManual();

        // Then
        assertEquals(0, monto.compareTo(BigDecimal.ZERO));
    }

    @Test
    void deberia_calcular_monto_descuento_manual_sobre_remanente_despues_de_promo() {
        // Given — base $2000, promo $500, remanente $1500. Manual: 20%
        ItemPedido item = ItemPedido.crearConPromocion(
            ItemPedidoId.generate(), pedidoId, productoPizza, 2, null,
            new BigDecimal("500"), "Promo test", UUID.randomUUID()
        );
        item.aplicarDescuentoManual(descuentoPorcentaje("20"));

        // When
        BigDecimal montoDescManual = item.calcularMontoDescuentoManual();

        // Then — 20% de $1500 = $300
        assertEquals(new BigDecimal("300.00"), montoDescManual);
    }

    // ============================================================
    // aplicarDescuentoManual — validaciones
    // ============================================================

    @Test
    void deberia_rechazar_monto_fijo_que_supera_el_remanente_despues_de_promo() {
        // Given — base $1000, promo $500, remanente $500. Intentar descuento fijo $600
        ItemPedido item = ItemPedido.crearConPromocion(
            ItemPedidoId.generate(), pedidoId, productoPizza, 1, null,
            new BigDecimal("500"), "Promo test", UUID.randomUUID()
        );
        DescuentoManual descuentoExcesivo = descuentoMontoFijo("600");

        // When / Then — $600 > $500 remanente: debe rechazarse
        assertThrows(IllegalArgumentException.class, () ->
            item.aplicarDescuentoManual(descuentoExcesivo)
        );
    }

    @Test
    void deberia_permitir_monto_fijo_exactamente_igual_al_remanente() {
        // Given — base $1000, promo $400, remanente $600. Descuento fijo $600 (precio final = $0)
        ItemPedido item = ItemPedido.crearConPromocion(
            ItemPedidoId.generate(), pedidoId, productoPizza, 1, null,
            new BigDecimal("400"), "Promo test", UUID.randomUUID()
        );
        DescuentoManual descuentoExacto = descuentoMontoFijo("600");

        // When / Then — $600 = $600 remanente: debe aceptarse (precio final = $0)
        assertDoesNotThrow(() -> item.aplicarDescuentoManual(descuentoExacto));
        assertEquals(0, item.calcularPrecioFinal().compareTo(BigDecimal.ZERO));
    }

    @Test
    void deberia_remover_descuento_manual_al_aplicar_null() {
        // Given — ítem con descuento manual activo
        ItemPedido item = itemSimple(1);
        item.aplicarDescuentoManual(descuentoPorcentaje("10"));
        assertTrue(item.tieneDescuentoManual());

        // When — se remueve el descuento pasando null
        item.aplicarDescuentoManual(null);

        // Then
        assertFalse(item.tieneDescuentoManual());
        assertEquals(new BigDecimal("1000"), item.calcularPrecioFinal());
    }

    // ============================================================
    // Snapshot de precio — inmutabilidad histórica
    // ============================================================

    @Test
    void deberia_mantener_precio_snapshot_aunque_cambie_el_catalogo() {
        // Given — ítem creado con precio $1000
        ItemPedido item = itemSimple(1);
        BigDecimal precioOriginal = item.getPrecioUnitario();

        // When — simular cambio de precio en el catálogo (el producto es mutable, el ítem no)
        productoPizza.actualizarPrecio(new BigDecimal("2000"));

        // Then — el ítem conserva el precio del momento de la venta
        assertEquals(precioOriginal, item.getPrecioUnitario());
        assertEquals(new BigDecimal("1000"), item.getPrecioUnitario());
        assertEquals(new BigDecimal("1000"), item.calcularSubtotal());
    }

    // ============================================================
    // Construcción inválida — fail fast
    // ============================================================

    @Test
    void deberia_rechazar_cantidad_cero() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class, () ->
            ItemPedido.crearDesdeProducto(ItemPedidoId.generate(), pedidoId, productoPizza, 0, null)
        );
    }

    @Test
    void deberia_rechazar_cantidad_negativa() {
        // Given / When / Then
        assertThrows(IllegalArgumentException.class, () ->
            ItemPedido.crearDesdeProducto(ItemPedidoId.generate(), pedidoId, productoPizza, -1, null)
        );
    }

    @Test
    void deberia_rechazar_producto_null_en_factory_method() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
            ItemPedido.crearDesdeProducto(ItemPedidoId.generate(), pedidoId, null, 1, null)
        );
    }

    @Test
    void deberia_rechazar_precio_unitario_negativo_en_constructor_directo() {
        // Given / When / Then — precio negativo viola la invariante
        assertThrows(IllegalArgumentException.class, () ->
            new ItemPedido(
                ItemPedidoId.generate(), pedidoId,
                productoPizza.getId(), "Pizza",
                1, new BigDecimal("-100"), null
            )
        );
    }

    // ============================================================
    // HU-29: Delta de envío a cocina
    // ============================================================

    @Test
    void deberia_reportar_cantidad_total_como_nueva_cuando_nunca_se_envio_a_cocina() {
        // Given
        ItemPedido item = itemSimple(3);

        // When / Then — las 3 unidades son nuevas
        assertEquals(3, item.obtenerCantidadNueva());
        assertTrue(item.tieneCantidadNueva());
    }

    @Test
    void deberia_reportar_delta_correcto_despues_de_envio_parcial() {
        // Given — 3 unidades, se envían 2
        ItemPedido item = itemSimple(3);
        item.marcarCantidadEnviada(); // Marca las 3 como enviadas

        // Simulamos que luego se aumentó la cantidad a 5 (merge)
        // Para eso usamos heredarEnvioCocina con el valor anterior
        ItemPedido itemMergeado = ItemPedido.crearDesdeProducto(
            ItemPedidoId.generate(), pedidoId, productoPizza, 5, null
        );
        itemMergeado.heredarEnvioCocina(3);

        // When
        int cantidadNueva = itemMergeado.obtenerCantidadNueva();

        // Then — 5 total - 3 ya enviadas = 2 nuevas
        assertEquals(2, cantidadNueva);
        assertTrue(itemMergeado.tieneCantidadNueva());
    }

    @Test
    void deberia_reportar_cero_nuevas_despues_de_marcar_todo_como_enviado() {
        // Given
        ItemPedido item = itemSimple(2);

        // When
        item.marcarCantidadEnviada();

        // Then
        assertEquals(0, item.obtenerCantidadNueva());
        assertFalse(item.tieneCantidadNueva());
    }

    @Test
    void deberia_rechazar_cantidad_ya_enviada_negativa_en_heredar() {
        // Given
        ItemPedido item = itemSimple(1);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> item.heredarEnvioCocina(-1));
    }

    // ============================================================
    // Snapshot de precio — captura inicial al crear el ítem
    // ============================================================

    @Test
    void deberia_crear_item_con_snapshot_de_precio() {
        // Given — producto con precio conocido en catálogo
        BigDecimal precioEnCatalogo = productoPizza.getPrecio(); // $1000

        // When — se crea el ítem mediante factory method
        ItemPedido item = ItemPedido.crearDesdeProducto(
            ItemPedidoId.generate(), pedidoId, productoPizza, 1, null
        );

        // Then — el precio unitario del ítem es igual al del catálogo en el momento de creación
        assertEquals(precioEnCatalogo, item.getPrecioUnitario());
        assertEquals(new BigDecimal("1000"), item.getPrecioUnitario());
    }

    // ============================================================
    // Validaciones de construcción — nombres exactos del plan
    // ============================================================

    @Test
    void deberia_rechazar_cantidad_cero_o_negativa() {
        // Given / When / Then — cualquier valor <= 0 es inválido
        assertThrows(IllegalArgumentException.class, () ->
            ItemPedido.crearDesdeProducto(ItemPedidoId.generate(), pedidoId, productoPizza, 0, null)
        );
        assertThrows(IllegalArgumentException.class, () ->
            ItemPedido.crearDesdeProducto(ItemPedidoId.generate(), pedidoId, productoPizza, -3, null)
        );
    }

    @Test
    void deberia_rechazar_precio_negativo() {
        // Given / When / Then — precio unitario negativo viola la invariante del ítem
        assertThrows(IllegalArgumentException.class, () ->
            new ItemPedido(
                ItemPedidoId.generate(), pedidoId,
                productoPizza.getId(), "Pizza",
                1, new BigDecimal("-500"), null
            )
        );
    }

    // ============================================================
    // calcularSubtotalLinea — nombres exactos del plan
    // ============================================================

    @Test
    void deberia_calcular_subtotal_linea_sin_extras() {
        // Given — ítem sin extras
        ItemPedido item = itemSimple(2);

        // When
        BigDecimal subtotalLinea = item.calcularSubtotalLinea();

        // Then — sin extras: 2 × $1000 = $2000 (idéntico a calcularSubtotal)
        assertEquals(new BigDecimal("2000"), subtotalLinea);
    }

    @Test
    void deberia_calcular_subtotal_linea_con_extras() {
        // Given — 3 Pizzas + Huevo extra ($200)
        ExtraPedido extra = ExtraPedido.crearDesdeProducto(productoExtra);
        ItemPedido item = ItemPedido.crearConExtras(
            ItemPedidoId.generate(), pedidoId, productoPizza, 3, null, List.of(extra)
        );

        // When
        BigDecimal subtotalLinea = item.calcularSubtotalLinea();

        // Then — 3 × ($1000 base + $200 extra) = $3600
        assertEquals(new BigDecimal("3600"), subtotalLinea);
    }

    // ============================================================
    // calcularPrecioFinal — nombres exactos del plan
    // ============================================================

    @Test
    void deberia_calcular_precio_final_con_promo_snapshot() {
        // Given — 1 Pizza con $300 de descuento por promoción (snapshot fijo)
        ItemPedido item = ItemPedido.crearConPromocion(
            ItemPedidoId.generate(), pedidoId, productoPizza, 1, null,
            new BigDecimal("300"), "Promo Especial", UUID.randomUUID()
        );

        // When
        BigDecimal precioFinal = item.calcularPrecioFinal();

        // Then — $1000 - $300 (promo snapshot) = $700
        assertEquals(new BigDecimal("700"), precioFinal);
    }

    @Test
    void deberia_calcular_precio_final_con_descuento_manual() {
        // Given — 1 Pizza, descuento manual de $150 (monto fijo, sin promo)
        ItemPedido item = itemSimple(1);
        item.aplicarDescuentoManual(descuentoMontoFijo("150"));

        // When
        BigDecimal precioFinal = item.calcularPrecioFinal();

        // Then — base $1000 - $150 (desc manual) = $850
        assertEquals(new BigDecimal("850"), precioFinal);
    }

    @Test
    void deberia_calcular_precio_final_completo() {
        // Given — 2 Pizzas, promo automática $300 + descuento manual 10%
        // Cubre la "Regla de Oro": total = items_base + descuentos_acumulables
        ItemPedido item = ItemPedido.crearConPromocion(
            ItemPedidoId.generate(), pedidoId, productoPizza, 2, null,
            new BigDecimal("300"), "Promo Cumpleaños", UUID.randomUUID()
        );
        item.aplicarDescuentoManual(descuentoPorcentaje("10"));

        // When
        BigDecimal precioFinal = item.calcularPrecioFinal();

        // Then:
        // base           = 2 × $1000 = $2000
        // remanente      = $2000 - $300 (promo) = $1700
        // desc manual    = 10% de $1700 = $170
        // precioFinal    = $1700 - $170 = $1530
        assertEquals(new BigDecimal("1530.00"), precioFinal);
    }

    // ============================================================
    // coincideConfiguracion — sin tests previos en el proyecto
    // ============================================================

    @Test
    void deberia_coincidir_configuracion_misma_obs_mismos_extras() {
        // Given — ítem con observación específica y un extra
        ExtraPedido extra = ExtraPedido.crearDesdeProducto(productoExtra);
        ItemPedido item = ItemPedido.crearConExtras(
            ItemPedidoId.generate(), pedidoId, productoPizza, 1, "sin sal", List.of(extra)
        );

        // When — consultamos con exactamente la misma configuración
        boolean coincide = item.coincideConfiguracion(
            productoPizza.getId(), "sin sal", List.of(extra)
        );

        // Then — misma obs + mismos extras → fusionable
        assertTrue(coincide);
    }

    @Test
    void deberia_no_coincidir_configuracion_con_obs_diferente() {
        // Given — ítem con observación "sin cebolla"
        ItemPedido item = ItemPedido.crearDesdeProducto(
            ItemPedidoId.generate(), pedidoId, productoPizza, 1, "sin cebolla"
        );

        // When — se compara contra mismo producto SIN observación
        boolean coincide = item.coincideConfiguracion(
            productoPizza.getId(), null, List.of()
        );

        // Then — observación distinta → líneas separadas, no fusionable
        assertFalse(coincide);
    }

    // ============================================================
    // HU-29: Delta de cocina — nombres exactos del plan
    // ============================================================

    @Test
    void deberia_calcular_cantidad_nueva_para_cocina() {
        // Given — ítem con 4 unidades, 2 ya enviadas previamente
        ItemPedido item = itemSimple(4);
        item.heredarEnvioCocina(2);

        // When
        int cantidadNueva = item.obtenerCantidadNueva();

        // Then — 4 total - 2 enviadas = 2 nuevas (delta)
        assertEquals(2, cantidadNueva);
    }

    @Test
    void deberia_heredar_envio_cocina_en_merge() {
        // Given — ítem original con 3 unidades enviadas a cocina
        ItemPedido itemOriginal = itemSimple(3);
        itemOriginal.marcarCantidadEnviada(); // las 3 quedan como enviadas

        // When — se crea un nuevo ítem (merge) con 5 unidades y hereda el estado
        ItemPedido itemMergeado = ItemPedido.crearDesdeProducto(
            ItemPedidoId.generate(), pedidoId, productoPizza, 5, null
        );
        itemMergeado.heredarEnvioCocina(itemOriginal.getCantidadEnviadaCocina()); // hereda 3

        // Then — 5 total - 3 heredadas = 2 nuevas para la próxima comanda
        assertEquals(2, itemMergeado.obtenerCantidadNueva());
        assertTrue(itemMergeado.tieneCantidadNueva());
        assertEquals(3, itemMergeado.getCantidadEnviadaCocina());
    }
}
