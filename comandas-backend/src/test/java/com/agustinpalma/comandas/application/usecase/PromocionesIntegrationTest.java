package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AgregarProductoRequest;
import com.agustinpalma.comandas.application.dto.AgregarProductoResponse;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Suite de Tests de Integración Completa para HU-10: Aplicar Promociones Automáticamente.
 * 
 * Valida 22+ escenarios de la vida real contra base de datos H2 en memoria.
 * 
 * Cobertura:
 * - Estrategias: NxM, Descuento Directo, Combos Condicionales
 * - Triggers: Temporal (fechas, días, horarios), Contenido (productos/categorías), Monto Mínimo
 * - Triggers Compuestos: Lógica AND múltiple
 * - Resolución de conflictos por Prioridad
 * - Ciclo de vida y Snapshot (inmutabilidad)
 * - Zona horaria: America/Argentina/Buenos_Aires
 * 
 * Principios:
 * - Tests contra BD real (H2)
 * - Transaccional (rollback automático)
 * - Datos aislados por test
 * - Helpers para legibilidad
 * - Clock fijo: 6 feb 2026, 19:00 (Jueves) para determinismo
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestClockConfig.class)
@DisplayName("HU-10: Promociones - Suite de Integración Completa")
class PromocionesIntegrationTest {

    @Autowired
    private AgregarProductoUseCase agregarProductoUseCase;

    @Autowired
    private PromocionRepository promocionRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private java.time.Clock clock;

    // Datos base del test
    private LocalId localId;
    private Mesa mesa;
    private Pedido pedido;

    // Productos base
    private Producto hamburguesa;
    private Producto papas;
    private Producto bebida;
    private Producto postre;
    private Producto cerveza;
    private Producto plato;

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
        bebida = crearYGuardarProducto("Coca Cola 500ml", "600.00");
        postre = crearYGuardarProducto("Flan con Dulce de Leche", "1200.00");
        cerveza = crearYGuardarProducto("Cerveza Artesanal", "1500.00");
        plato = crearYGuardarProducto("Plato del Día", "3000.00");
    }

    // =================================================
    // GRUPO 1: ESTRATEGIAS DE CANTIDAD (NxM)
    // =================================================

    @Nested
    @DisplayName("Grupo 1: Estrategias de Cantidad (NxM)")
    class EstrategiasNxMTests {

        @Test
        @DisplayName("Escenario 1: 2x1 Simple - Agregar 2 unidades -> Cobrar 1")
        void promo_2x1_simple_debe_cobrar_una_unidad() {
            // Given: Promo 2x1 en hamburguesas
            Promocion promo2x1 = crearPromocionNxM(
                "2x1 Hamburguesas",
                2, 1,
                hamburguesa.getId().getValue(),
                10
            );
            promocionRepository.guardar(promo2x1);

            // When: Agregar 2 hamburguesas
            AgregarProductoRequest request = new AgregarProductoRequest(
                pedido.getId(), hamburguesa.getId(), 2, null
            );
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

            // Then: Cantidad cocina = 2, pero precio = 1 unidad
            assertThat(response.items()).hasSize(1);
            var item = response.items().get(0);

            assertThat(item.cantidad()).isEqualTo(2); // Cocina prepara 2
            assertThat(item.precioUnitarioBase()).isEqualByComparingTo("2500.00");
            assertThat(item.subtotalItem()).isEqualByComparingTo("5000.00"); // 2 × 2500
            assertThat(item.descuentoTotal()).isEqualByComparingTo("2500.00"); // 1 gratis
            assertThat(item.precioFinal()).isEqualByComparingTo("2500.00"); // Paga 1
            assertThat(item.nombrePromocion()).isEqualTo("2x1 Hamburguesas");
            assertThat(item.tienePromocion()).isTrue();

            assertThat(response.total()).isEqualByComparingTo("2500.00");
        }

        @Test
        @DisplayName("Escenario 2: 3x2 - Agregar 3 unidades -> Cobrar 2")
        void promo_3x2_debe_cobrar_dos_unidades() {
            // Given: Promo 3x2 en papas
            Promocion promo3x2 = crearPromocionNxM(
                "3x2 Papas",
                3, 2,
                papas.getId().getValue(),
                10
            );
            promocionRepository.guardar(promo3x2);

            // When: Agregar 3 papas
            AgregarProductoRequest request = new AgregarProductoRequest(
                pedido.getId(), papas.getId(), 3, null
            );
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

            // Then: Cantidad = 3, descuento = 1 unidad
            var item = response.items().get(0);

            assertThat(item.cantidad()).isEqualTo(3);
            assertThat(item.subtotalItem()).isEqualByComparingTo("2400.00"); // 3 × 800
            assertThat(item.descuentoTotal()).isEqualByComparingTo("800.00"); // 1 gratis
            assertThat(item.precioFinal()).isEqualByComparingTo("1600.00"); // Paga 2
            assertThat(item.nombrePromocion()).isEqualTo("3x2 Papas");
        }

        @Test
        @DisplayName("Escenario 2b: 3x2 con cantidad insuficiente (2) - No aplica descuento")
        void promo_3x2_con_cantidad_insuficiente_no_aplica() {
            // Given: Promo 3x2
            Promocion promo3x2 = crearPromocionNxM("3x2 Papas", 3, 2, papas.getId().getValue(), 10);
            promocionRepository.guardar(promo3x2);

            // When: Solo agregar 2 papas (insuficiente)
            AgregarProductoRequest request = new AgregarProductoRequest(
                pedido.getId(), papas.getId(), 2, null
            );
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

            // Then: No hay descuento
            var item = response.items().get(0);
            assertThat(item.descuentoTotal()).isEqualByComparingTo("0.00");
            assertThat(item.precioFinal()).isEqualByComparingTo("1600.00"); // 2 × 800
            assertThat(item.tienePromocion()).isFalse();
        }
    }

    // =================================================
    // GRUPO 2: COMBOS CONDICIONALES (TRIGGER -> TARGET)
    // =================================================

    @Nested
    @DisplayName("Grupo 2: Combos Condicionales (Trigger → Target)")
    class CombosCondicionalesTests {

        @Test
        @DisplayName("Escenario 3: Combo Fijo - Hamburguesa + Papas -> Papas con precio reducido")
        void combo_hamburguesa_con_papas_aplica_descuento_en_papas() {
            // Given: Combo con 50% en papas si comprás hamburguesa
            Promocion comboHamburguesa = crearPromocionCombo(
                "Combo Hamburguesa + Papas",
                hamburguesa.getId().getValue(), // trigger
                papas.getId().getValue(),        // target
                new BigDecimal("50"),            // 50% off en papas
                1,                               // cantidad mínima trigger
                10
            );
            promocionRepository.guardar(comboHamburguesa);

            // When: Agregar hamburguesa primero
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );

            // Refrescar pedido (simulando segunda llamada)
            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();

            // Luego agregar papas
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), papas.getId(), 1, null)
            );

            // Then: Las papas tienen 50% de descuento
            var itemPapas = response.items().stream()
                .filter(i -> i.nombreProducto().equals("Papas Fritas"))
                .findFirst()
                .orElseThrow();

            assertThat(itemPapas.precioUnitarioBase()).isEqualByComparingTo("800.00");
            assertThat(itemPapas.descuentoTotal()).isEqualByComparingTo("400.00"); // 50%
            assertThat(itemPapas.precioFinal()).isEqualByComparingTo("400.00");
            assertThat(itemPapas.nombrePromocion()).isEqualTo("Combo Hamburguesa + Papas");
        }

        @Test
        @DisplayName("Escenario 4: Regalo - Plato Principal → Postre con 100% descuento")
        void combo_plato_con_regalo_de_postre() {
            // Given: Promo "Postre Gratis" si comprás plato
            Promocion promoPostre = crearPromocionCombo(
                "Postre de Regalo",
                plato.getId().getValue(),
                postre.getId().getValue(),
                new BigDecimal("100"), // 100% off
                1,
                10
            );
            promocionRepository.guardar(promoPostre);

            // When: Agregar plato y luego postre
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), plato.getId(), 1, null)
            );
            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();

            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), postre.getId(), 1, null)
            );

            // Then: Postre gratis (100% descuento)
            var itemPostre = response.items().stream()
                .filter(i -> i.nombreProducto().equals("Flan con Dulce de Leche"))
                .findFirst()
                .orElseThrow();

            assertThat(itemPostre.descuentoTotal()).isEqualByComparingTo("1200.00");
            assertThat(itemPostre.precioFinal()).isEqualByComparingTo("0.00");
            assertThat(itemPostre.nombrePromocion()).isEqualTo("Postre de Regalo");
        }

        @Test
        @DisplayName("Escenario 5: Combo sin trigger presente - No aplica")
        void combo_sin_trigger_presente_no_aplica_descuento() {
            // Given: Combo hamburguesa + papas
            Promocion combo = crearPromocionCombo(
                "Combo H+P",
                hamburguesa.getId().getValue(),
                papas.getId().getValue(),
                new BigDecimal("50"),
                1,
                10
            );
            promocionRepository.guardar(combo);

            // When: Agregar solo papas SIN hamburguesa
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), papas.getId(), 1, null)
            );

            // Then: No hay descuento
            var item = response.items().get(0);
            assertThat(item.tienePromocion()).isFalse();
            assertThat(item.descuentoTotal()).isEqualByComparingTo("0.00");
        }
    }

    // =================================================
    // GRUPO 2A: PRECIO FIJO POR CANTIDAD (PACKS)
    // =================================================

    @Nested
    @DisplayName("Grupo 2A: Precio Fijo por Cantidad (Packs)")
    class PrecioFijoPorCantidadTests {

        @Test
        @DisplayName("Escenario 6A: Pack 2×$22.000 - 1 unidad → Sin descuento")
        void pack_precio_fijo_una_unidad_sin_descuento() {
            // Given: Hamburguesa Premium $13.000, pack "2×$22.000"
            Producto hamburguesaPremium = crearYGuardarProducto("Hamburguesa Premium", "13000.00");
            
            Promocion packPareja = crearPromocionPrecioFijo(
                "Pack Pareja",
                hamburguesaPremium.getId().getValue(),
                2,                              // cantidadActivacion
                new BigDecimal("22000.00"),     // precioPaquete
                10
            );
            promocionRepository.guardar(packPareja);

            // When: Agregar 1 hamburguesa
            AgregarProductoRequest request = new AgregarProductoRequest(
                pedido.getId(), hamburguesaPremium.getId(), 1, null
            );
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

            // Then: Sin ciclos completos → Sin descuento
            var item = response.items().get(0);
            assertThat(item.cantidad()).isEqualTo(1);
            assertThat(item.precioUnitarioBase()).isEqualByComparingTo("13000.00");
            assertThat(item.descuentoTotal()).isEqualByComparingTo("0.00");
            assertThat(item.precioFinal()).isEqualByComparingTo("13000.00");
            assertThat(item.tienePromocion()).isFalse();
        }

        @Test
        @DisplayName("Escenario 6B: Pack 2×$22.000 - 2 unidades → Descuento $4.000")
        void pack_precio_fijo_dos_unidades_aplica_descuento() {
            // Given: Hamburguesa Premium $13.000, pack "2×$22.000"
            Producto hamburguesaPremium = crearYGuardarProducto("Hamburguesa Premium", "13000.00");
            
            Promocion packPareja = crearPromocionPrecioFijo(
                "Pack Pareja",
                hamburguesaPremium.getId().getValue(),
                2,
                new BigDecimal("22000.00"),
                10
            );
            promocionRepository.guardar(packPareja);

            // When: Agregar 2 hamburguesas
            AgregarProductoRequest request = new AgregarProductoRequest(
                pedido.getId(), hamburguesaPremium.getId(), 2, null
            );
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

            // Then: 1 ciclo completo → Descuento = (2×$13.000) - $22.000 = $4.000
            var item = response.items().get(0);
            assertThat(item.cantidad()).isEqualTo(2);
            assertThat(item.precioUnitarioBase()).isEqualByComparingTo("13000.00");
            assertThat(item.descuentoTotal()).isEqualByComparingTo("4000.00");
            assertThat(item.precioFinal()).isEqualByComparingTo("22000.00");
            assertThat(item.nombrePromocion()).isEqualTo("Pack Pareja");
            assertThat(item.tienePromocion()).isTrue();
        }

        @Test
        @DisplayName("Escenario 6C: Pack 2×$22.000 - 3 unidades → Descuento $4.000 + 1 a precio completo")
        void pack_precio_fijo_tres_unidades_un_ciclo_mas_una_suelta() {
            // Given: Hamburguesa Premium $13.000, pack "2×$22.000"
            Producto hamburguesaPremium = crearYGuardarProducto("Hamburguesa Premium", "13000.00");
            
            Promocion packPareja = crearPromocionPrecioFijo(
                "Pack Pareja",
                hamburguesaPremium.getId().getValue(),
                2,
                new BigDecimal("22000.00"),
                10
            );
            promocionRepository.guardar(packPareja);

            // When: Agregar 3 hamburguesas
            AgregarProductoRequest request = new AgregarProductoRequest(
                pedido.getId(), hamburguesaPremium.getId(), 3, null
            );
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

            // Then: 1 ciclo ($4.000 descuento) + 1 unidad suelta a $13.000
            // Precio final = $22.000 + $13.000 = $35.000
            var item = response.items().get(0);
            assertThat(item.cantidad()).isEqualTo(3);
            assertThat(item.precioUnitarioBase()).isEqualByComparingTo("13000.00");
            assertThat(item.descuentoTotal()).isEqualByComparingTo("4000.00");
            assertThat(item.precioFinal()).isEqualByComparingTo("35000.00");
            assertThat(item.nombrePromocion()).isEqualTo("Pack Pareja");
        }

        @Test
        @DisplayName("Escenario 6D: Pack 2×$22.000 - 4 unidades → 2 ciclos, Descuento $8.000")
        void pack_precio_fijo_cuatro_unidades_dos_ciclos() {
            // Given: Hamburguesa Premium $13.000, pack "2×$22.000"
            Producto hamburguesaPremium = crearYGuardarProducto("Hamburguesa Premium", "13000.00");
            
            Promocion packPareja = crearPromocionPrecioFijo(
                "Pack Pareja",
                hamburguesaPremium.getId().getValue(),
                2,
                new BigDecimal("22000.00"),
                10
            );
            promocionRepository.guardar(packPareja);

            // When: Agregar 4 hamburguesas
            AgregarProductoRequest request = new AgregarProductoRequest(
                pedido.getId(), hamburguesaPremium.getId(), 4, null
            );
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

            // Then: 2 ciclos → Descuento = (4×$13.000) - (2×$22.000) = $52.000 - $44.000 = $8.000
            var item = response.items().get(0);
            assertThat(item.cantidad()).isEqualTo(4);
            assertThat(item.precioUnitarioBase()).isEqualByComparingTo("13000.00");
            assertThat(item.descuentoTotal()).isEqualByComparingTo("8000.00");
            assertThat(item.precioFinal()).isEqualByComparingTo("44000.00");
            assertThat(item.nombrePromocion()).isEqualTo("Pack Pareja");
        }

        @Test
        @DisplayName("Escenario 6E: HU-21 - Cambio dinámico de cantidad debe recalcular descuento")
        void pack_cambio_dinamico_cantidad_recalcula_descuento() {
            // Given: Hamburguesa Premium $13.000, pack "2×$22.000"
            Producto hamburguesaPremium = crearYGuardarProducto("Hamburguesa Premium", "13000.00");
            
            Promocion packPareja = crearPromocionPrecioFijo(
                "Pack Pareja",
                hamburguesaPremium.getId().getValue(),
                2,
                new BigDecimal("22000.00"),
                10
            );
            promocionRepository.guardar(packPareja);

            // When: Agregar 1 hamburguesa (sin promo)
            AgregarProductoRequest request1 = new AgregarProductoRequest(
                pedido.getId(), hamburguesaPremium.getId(), 1, null
            );
            AgregarProductoResponse response1 = agregarProductoUseCase.ejecutar(request1);

            // Then: Sin descuento
            var item1 = response1.items().get(0);
            assertThat(item1.descuentoTotal()).isEqualByComparingTo("0.00");
            assertThat(item1.precioFinal()).isEqualByComparingTo("13000.00");

            // NOTE: El recálculo automático al cambiar cantidad (PATCH /items/{id})
            // requiere el use case GestionarItemsPedidoUseCase que ya está implementado.
            // Este test valida el comportamiento en agregar producto.
            // Para validar HU-21 completo, ver GestionarItemsPedidoIntegrationTest.
        }
    }

    // =================================================
    // GRUPO 3: TRIGGERS CONTEXTUALES (TIEMPO Y MONTO)
    // =================================================

    @Nested
    @DisplayName("Grupo 3: Triggers Contextuales (Tiempo y Monto)")
    class TriggersContextualesTests {

        @Test
        @DisplayName("Escenario 7: Happy Hour (Franja Horaria) - Dentro del rango")
        void happy_hour_dentro_del_rango_horario_aplica_descuento() {
            // Given: Happy Hour de 18:00 a 20:00 con 30% en cervezas
            // TestClockConfig fija la hora a las 19:00 (DENTRO del rango)
            Promocion happyHour = crearPromocionHappyHour(
                "Happy Hour Cervezas",
                cerveza.getId().getValue(),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                new BigDecimal("30"),
                10
            );
            promocionRepository.guardar(happyHour);

            // When: Agregar cerveza (clock fijo a las 19:00)
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), cerveza.getId(), 1, null)
            );

            // Then: La promoción debe aplicarse porque estamos a las 19:00 (18:00-20:00)
            var item = response.items().get(0);
            
            assertThat(item.cantidad()).isEqualTo(1);
            assertThat(item.precioUnitarioBase()).isEqualByComparingTo("1500.00");
            assertThat(item.subtotalItem()).isEqualByComparingTo("1500.00");
            assertThat(item.descuentoTotal()).isEqualByComparingTo("450.00"); // 30% de 1500
            assertThat(item.precioFinal()).isEqualByComparingTo("1050.00");
            assertThat(item.tienePromocion()).isTrue();
            assertThat(item.nombrePromocion()).isEqualTo("Happy Hour Cervezas");
        }

        @Test
        @DisplayName("Escenario 8: Día de la Semana - Promo válida solo los Jueves")
        void promo_dia_especifico_valida_solo_en_dia_correcto() {
            // Given: Promo de Jueves con 25% en hamburguesas
            Promocion promoJueves = crearPromocionDiaSemana(
                "Jueves de Hamburguesas",
                hamburguesa.getId().getValue(),
                Set.of(DayOfWeek.THURSDAY),
                new BigDecimal("25"),
                10
            );
            promocionRepository.guardar(promoJueves);

            // When: Agregar hamburguesa
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );

            // Then: Solo aplica si hoy es jueves
            var item = response.items().get(0);
            DayOfWeek hoy = LocalDate.now(clock).getDayOfWeek();
            
            if (hoy == DayOfWeek.THURSDAY) {
                assertThat(item.descuentoTotal()).isEqualByComparingTo("625.00"); // 25% de 2500
                assertThat(item.nombrePromocion()).isEqualTo("Jueves de Hamburguesas");
            } else {
                assertThat(item.tienePromocion()).isFalse();
            }
        }

        @Test
        @DisplayName("Escenario 9: Monto Mínimo - Descuento solo si pedido supera $X")
        void descuento_solo_aplica_si_pedido_supera_monto_minimo() {
            // Given: Promo con monto mínimo de $5000 que da 15% en bebidas
            Promocion promoMontoMinimo = crearPromocionMontoMinimo(
                "Descuento Compra Mayor",
                bebida.getId().getValue(),
                new BigDecimal("5000"),
                new BigDecimal("15"),
                10
            );
            promocionRepository.guardar(promoMontoMinimo);

            // When: Primero agregar productos hasta superar $5000
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 2, null) // 2 × 2500 = 5000
            );
            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();

            // Ahora agregar bebida (el pedido ya tiene $5000)
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), bebida.getId(), 1, null)
            );

            // Then: La bebida debería tener descuento
            var itemBebida = response.items().stream()
                .filter(i -> i.nombreProducto().equals("Coca Cola 500ml"))
                .findFirst()
                .orElseThrow();

            assertThat(itemBebida.descuentoTotal()).isEqualByComparingTo("90.00"); // 15% de 600
            assertThat(itemBebida.nombrePromocion()).isEqualTo("Descuento Compra Mayor");
        }
    }

    // =================================================
    // GRUPO 5: TRIGGERS COMPUESTOS (LÓGICA AND)
    // =================================================

    @Nested
    @DisplayName("Grupo 5: Triggers Compuestos (Lógica AND)")
    class TriggersCompuestosTests {

        @Test
        @DisplayName("Escenario 12: Trigger Completo - Producto + Día + Horario (TODO debe coincidir)")
        void trigger_compuesto_aplica_solo_si_todo_coincide() {
            // Given: Promo que requiere: Cerveza + Jueves + 18-20hs
            Promocion promoCompleja = crearPromocionTriggerCompuesto(
                "Promo Cerveza Jueves Noche",
                cerveza.getId().getValue(),
                Set.of(DayOfWeek.THURSDAY),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                new BigDecimal("40"),
                10
            );
            promocionRepository.guardar(promoCompleja);

            // When: Agregar cerveza
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), cerveza.getId(), 1, null)
            );

            // Then: Solo aplica si es Jueves Y está entre 18-20hs
            var item = response.items().get(0);
            DayOfWeek hoy = LocalDate.now(clock).getDayOfWeek();
            LocalTime ahora = LocalTime.now(clock);
            boolean estaEnHorario = ahora.isAfter(LocalTime.of(18, 0)) && ahora.isBefore(LocalTime.of(20, 0));

            if (hoy == DayOfWeek.THURSDAY && estaEnHorario) {
                assertThat(item.descuentoTotal()).isEqualByComparingTo("600.00"); // 40% de 1500
                assertThat(item.nombrePromocion()).isEqualTo("Promo Cerveza Jueves Noche");
            } else {
                assertThat(item.tienePromocion()).isFalse();
            }
        }
    }

    // =================================================
    // GRUPO 6: RESOLUCIÓN DE CONFLICTOS
    // =================================================

    @Nested
    @DisplayName("Grupo 6: Resolución de Conflictos por Prioridad")
    class ResolucionConflictosTests {

        @Test
        @DisplayName("Escenario 14: Prioridad Simple - Mayor prioridad gana")
        void cuando_dos_promos_aplican_gana_la_de_mayor_prioridad() {
            // Given: Dos promos que aplican al mismo producto
            // Promo A: 10% descuento, prioridad 1
            Promocion promoA = crearPromocionDescuento(
                "Promo Básica",
                hamburguesa.getId().getValue(),
                new BigDecimal("10"),
                1 // Prioridad baja
            );

            // Promo B: 25% descuento, prioridad 10
            Promocion promoB = crearPromocionDescuento(
                "Super Promo",
                hamburguesa.getId().getValue(),
                new BigDecimal("25"),
                10 // Prioridad alta
            );

            promocionRepository.guardar(promoA);
            promocionRepository.guardar(promoB);

            // When: Agregar hamburguesa
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );

            // Then: Debe ganar "Super Promo" (prioridad 10)
            var item = response.items().get(0);
            assertThat(item.nombrePromocion()).isEqualTo("Super Promo");
            assertThat(item.descuentoTotal()).isEqualByComparingTo("625.00"); // 25% de 2500
        }

        @Test
        @DisplayName("Escenario 15: Acumulabilidad - No suma descuentos (Winner takes all)")
        void no_acumula_descuentos_solo_aplica_el_mejor() {
            // Given: Dos promos aplicables con igual prioridad
            Promocion promo1 = crearPromocionDescuento("Promo 1", cerveza.getId().getValue(), new BigDecimal("20"), 5);
            Promocion promo2 = crearPromocionDescuento("Promo 2", cerveza.getId().getValue(), new BigDecimal("15"), 5);

            promocionRepository.guardar(promo1);
            promocionRepository.guardar(promo2);

            // When
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), cerveza.getId(), 1, null)
            );

            // Then: Solo debe aplicar UNA promo (no sumar 20% + 15%)
            var item = response.items().get(0);
            
            // Debería aplicar una de las dos (primera encontrada con igual prioridad)
            // El descuento debe ser O 300 (20%) O 225 (15%), nunca 525 (35%)
            assertThat(item.descuentoTotal()).isIn(
                new BigDecimal("300.00"), // 20%
                new BigDecimal("225.00")  // 15%
            );
        }
    }

    // =================================================
    // GRUPO 7: CICLO DE VIDA Y SNAPSHOT
    // =================================================

    @Nested
    @DisplayName("Grupo 7: Ciclo de Vida y Snapshot (Inmutabilidad)")
    class CicloVidaSnapshotTests {

        @Test
        @DisplayName("Escenario 16: Aplicación Automática - Response ya trae precioFinal calculado")
        void response_debe_traer_precio_final_calculado_automaticamente() {
            // Given: Promo 2x1
            Promocion promo2x1 = crearPromocionNxM("2x1 Test", 2, 1, hamburguesa.getId().getValue(), 10);
            promocionRepository.guardar(promo2x1);

            // When: Agregar producto
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 2, null)
            );

            // Then: El response debe tener TODO calculado
            assertThat(response.items()).hasSize(1);
            var item = response.items().get(0);

            assertThat(item.precioUnitarioBase()).isNotNull();
            assertThat(item.descuentoTotal()).isNotNull();
            assertThat(item.precioFinal()).isNotNull();
            assertThat(item.nombrePromocion()).isNotNull();
            
            // Validación matemática
            BigDecimal subtotal = item.subtotalItem();
            BigDecimal descuento = item.descuentoTotal();
            BigDecimal precioFinal = item.precioFinal();
            
            assertThat(subtotal.subtract(descuento)).isEqualByComparingTo(precioFinal);
        }

        @Test
        @DisplayName("Escenario 17: Snapshot de Cierre - Promo vencida no afecta ítems anteriores")
        void snapshot_preserva_precio_aunque_promo_venza() {
            // Given: Promo simple con descuento del 30% (sin restricción horaria)
            Promocion promoTemporal = crearPromocionDescuento(
                "Promo Temporal",
                hamburguesa.getId().getValue(),
                new BigDecimal("30"),
                10
            );
            promocionRepository.guardar(promoTemporal);

            // When: Paso 1 - Agregar producto con promo vigente
            AgregarProductoResponse response1 = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );

            var itemOriginal = response1.items().get(0);
            BigDecimal descuentoSnapshot = itemOriginal.descuentoTotal();
            BigDecimal precioFinalSnapshot = itemOriginal.precioFinal();
            String nombrePromoSnapshot = itemOriginal.nombrePromocion();

            // Validar que la promo se aplicó
            assertThat(descuentoSnapshot).isGreaterThan(BigDecimal.ZERO);
            assertThat(nombrePromoSnapshot).isEqualTo("Promo Temporal");

            // Paso 2: Desactivar la promoción (simula que expiró)
            promoTemporal.desactivar();
            promocionRepository.guardar(promoTemporal);

            // Paso 3: Recuperar el pedido y verificar que el ítem mantiene los datos
            Pedido pedidoRecuperado = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();

            // Then: El ítem debe mantener el snapshot inmutable
            assertThat(pedidoRecuperado.getItems()).hasSize(1);
            ItemPedido itemRecuperado = pedidoRecuperado.getItems().get(0);

            assertThat(itemRecuperado.getMontoDescuento()).isEqualByComparingTo(descuentoSnapshot);
            assertThat(itemRecuperado.calcularPrecioFinal()).isEqualByComparingTo(precioFinalSnapshot);
            assertThat(itemRecuperado.getNombrePromocion()).isEqualTo(nombrePromoSnapshot);
            
            // CRÍTICO: El descuento sigue siendo > 0, por lo tanto tienePromocion() debe ser true
            assertThat(itemRecuperado.getMontoDescuento().compareTo(BigDecimal.ZERO)).isGreaterThan(0);
            assertThat(itemRecuperado.tienePromocion())
                .withFailMessage(
                    "El ítem debería mantener tienePromocion=true porque montoDescuento=%s > 0, " +
                    "aunque la promo esté desactivada. El snapshot es inmutable.",
                    itemRecuperado.getMontoDescuento()
                )
                .isTrue();
        }

        @Test
        @DisplayName("Escenario 18: Sin promociones activas - Ítem sin descuento")
        void sin_promociones_activas_item_tiene_descuento_cero() {
            // Given: No hay promociones

            // When: Agregar producto
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );

            // Then: Sin descuento
            var item = response.items().get(0);
            assertThat(item.tienePromocion()).isFalse();
            assertThat(item.descuentoTotal()).isEqualByComparingTo("0.00");
            assertThat(item.precioFinal()).isEqualByComparingTo(item.subtotalItem());
            assertThat(item.nombrePromocion()).isNull();
        }

        @Test
        @DisplayName("Escenario 19: Múltiples ítems con diferentes promociones")
        void multiples_items_cada_uno_con_su_promo() {
            // Given: Promo para hamburguesa y otra para cerveza
            Promocion promoHamburguesa = crearPromocionDescuento(
                "Promo Hamburguesa",
                hamburguesa.getId().getValue(),
                new BigDecimal("20"),
                10
            );
            Promocion promoCerveza = crearPromocionDescuento(
                "Promo Cerveza",
                cerveza.getId().getValue(),
                new BigDecimal("15"),
                10
            );
            promocionRepository.guardar(promoHamburguesa);
            promocionRepository.guardar(promoCerveza);

            // When: Agregar hamburguesa y cerveza
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), hamburguesa.getId(), 1, null)
            );
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), cerveza.getId(), 1, null)
            );

            // Then: Cada ítem tiene su propia promo
            assertThat(response.items()).hasSize(2);

            var itemH = response.items().stream()
                .filter(i -> i.nombreProducto().equals("Hamburguesa Completa"))
                .findFirst().orElseThrow();
            assertThat(itemH.nombrePromocion()).isEqualTo("Promo Hamburguesa");
            assertThat(itemH.descuentoTotal()).isEqualByComparingTo("500.00"); // 20% de 2500

            var itemC = response.items().stream()
                .filter(i -> i.nombreProducto().equals("Cerveza Artesanal"))
                .findFirst().orElseThrow();
            assertThat(itemC.nombrePromocion()).isEqualTo("Promo Cerveza");
            assertThat(itemC.descuentoTotal()).isEqualByComparingTo("225.00"); // 15% de 1500
        }
    }

    // =================================================
    // GRUPO 8: EDGE CASES Y VALIDACIONES
    // =================================================

    @Nested
    @DisplayName("Grupo 8: Edge Cases y Validaciones")
    class EdgeCasesTests {

        @Test
        @DisplayName("Escenario 20: Producto sin promoción aplicable mantiene precio full")
        void producto_sin_promo_aplicable_mantiene_precio_completo() {
            // Given: Promo solo para hamburguesas
            Promocion promoHamburguesa = crearPromocionDescuento(
                "Solo Hamburguesas",
                hamburguesa.getId().getValue(),
                new BigDecimal("30"),
                10
            );
            promocionRepository.guardar(promoHamburguesa);

            // When: Agregar cerveza (sin promo)
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), cerveza.getId(), 1, null)
            );

            // Then: Precio completo
            var item = response.items().get(0);
            assertThat(item.tienePromocion()).isFalse();
            assertThat(item.precioFinal()).isEqualByComparingTo("1500.00");
        }

        @Test
        @DisplayName("Escenario 21: Cantidad mayor en NxM aplica múltiples ciclos")
        void nxm_con_cantidad_mayor_aplica_multiples_ciclos() {
            // Given: 2x1
            Promocion promo2x1 = crearPromocionNxM("2x1", 2, 1, bebida.getId().getValue(), 10);
            promocionRepository.guardar(promo2x1);

            // When: Agregar 6 bebidas (3 ciclos de 2x1)
            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), bebida.getId(), 6, null)
            );

            // Then: 3 bebidas gratis
            var item = response.items().get(0);
            assertThat(item.cantidad()).isEqualTo(6);
            assertThat(item.subtotalItem()).isEqualByComparingTo("3600.00"); // 6 × 600
            assertThat(item.descuentoTotal()).isEqualByComparingTo("1800.00"); // 3 × 600
            assertThat(item.precioFinal()).isEqualByComparingTo("1800.00"); // Paga 3
        }

        @Test
        @DisplayName("Escenario 22: Combo con cantidad mínima de trigger = 2")
        void combo_requiere_cantidad_minima_de_trigger() {
            // Given: Combo que requiere 2 platos para activar descuento en bebida
            Promocion comboDoble = crearPromocionCombo(
                "Combo 2 Platos + Bebida",
                plato.getId().getValue(),
                bebida.getId().getValue(),
                new BigDecimal("100"), // Bebida gratis
                2, // Requiere 2 platos
                10
            );
            promocionRepository.guardar(comboDoble);

            // When: Agregar solo 1 plato y una bebida
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), plato.getId(), 1, null)
            );
            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();

            AgregarProductoResponse response = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), bebida.getId(), 1, null)
            );

            // Then: NO aplica (falta 1 plato más)
            var itemBebida = response.items().stream()
                .filter(i -> i.nombreProducto().equals("Coca Cola 500ml"))
                .findFirst().orElseThrow();

            assertThat(itemBebida.tienePromocion()).isFalse();

            // Paso 2: Agregar el segundo plato
            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), plato.getId(), 1, null)
            );
            pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();

            // Agregar otra bebida ahora que hay 2 platos
            AgregarProductoResponse response2 = agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido.getId(), bebida.getId(), 1, null)
            );

            // Then: AHORA sí aplica
            var itemBebida2 = response2.items().stream()
                .filter(i -> i.nombreProducto().equals("Coca Cola 500ml"))
                .skip(1) // La segunda bebida
                .findFirst().orElseThrow();

            assertThat(itemBebida2.tienePromocion()).isTrue();
            assertThat(itemBebida2.descuentoTotal()).isEqualByComparingTo("600.00"); // 100% gratis
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

    private Promocion crearPromocionNxM(String nombre, int llevas, int pagas, UUID productoId, int prioridad) {
        EstrategiaPromocion estrategia = new CantidadFija(llevas, pagas);
        LocalDate fechaTest = LocalDate.ofInstant(clock.instant(), clock.getZone());
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            fechaTest.minusDays(1),
            fechaTest.plusDays(30)
        );

        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Descripción NxM",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );

        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        return promo;
    }

    private Promocion crearPromocionDescuento(String nombre, UUID productoId, BigDecimal porcentaje, int prioridad) {
        EstrategiaPromocion estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, porcentaje);
        LocalDate fechaTest = LocalDate.ofInstant(clock.instant(), clock.getZone());
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            fechaTest.minusDays(1),
            fechaTest.plusDays(30)
        );

        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Descripción descuento",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );

        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        return promo;
    }

    private Promocion crearPromocionCombo(
            String nombre,
            UUID productoTriggerId,
            UUID productoTargetId,
            BigDecimal porcentajeBeneficio,
            int cantidadMinimaTrigger,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new ComboCondicional(cantidadMinimaTrigger, porcentajeBeneficio);
        LocalDate fechaTest = LocalDate.ofInstant(clock.instant(), clock.getZone());
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            fechaTest.minusDays(1),
            fechaTest.plusDays(30)
        );

        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Descripción combo",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );

        ItemPromocion itemTrigger = ItemPromocion.productoTrigger(productoTriggerId);
        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoTargetId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTrigger, itemTarget)));
        return promo;
    }

    private Promocion crearPromocionHappyHour(
            String nombre,
            UUID productoId,
            LocalTime horaInicio,
            LocalTime horaFin,
            BigDecimal porcentaje,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, porcentaje);
        LocalDate fechaTest = LocalDate.ofInstant(clock.instant(), clock.getZone());
        CriterioActivacion trigger = new CriterioTemporal(
            fechaTest.minusDays(1),
            fechaTest.plusDays(30),
            null,
            horaInicio,
            horaFin
        );

        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Descripción Happy Hour",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );

        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        return promo;
    }

    private Promocion crearPromocionDiaSemana(
            String nombre,
            UUID productoId,
            Set<DayOfWeek> dias,
            BigDecimal porcentaje,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, porcentaje);
        LocalDate fechaTest = LocalDate.ofInstant(clock.instant(), clock.getZone());
        CriterioActivacion trigger = new CriterioTemporal(
            fechaTest.minusDays(1),
            fechaTest.plusDays(30),
            dias,
            null,
            null
        );

        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Descripción día semana",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );

        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        return promo;
    }

    private Promocion crearPromocionMontoMinimo(
            String nombre,
            UUID productoId,
            BigDecimal montoMinimo,
            BigDecimal porcentaje,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, porcentaje);
        
        LocalDate fechaTest = LocalDate.ofInstant(clock.instant(), clock.getZone());
        // Trigger temporal
        CriterioActivacion triggerTemporal = CriterioTemporal.soloFechas(
            fechaTest.minusDays(1),
            fechaTest.plusDays(30)
        );
        
        // Trigger de monto mínimo
        CriterioActivacion triggerMonto = new CriterioMontoMinimo(montoMinimo);

        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Descripción monto mínimo",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(triggerTemporal, triggerMonto) // AND lógico
        );

        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        return promo;
    }

    private Promocion crearPromocionTriggerCompuesto(
            String nombre,
            UUID productoId,
            Set<DayOfWeek> dias,
            LocalTime horaInicio,
            LocalTime horaFin,
            BigDecimal porcentaje,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new DescuentoDirecto(ModoDescuento.PORCENTAJE, porcentaje);
        LocalDate fechaTest = LocalDate.ofInstant(clock.instant(), clock.getZone());
        CriterioActivacion trigger = new CriterioTemporal(
            fechaTest.minusDays(1),
            fechaTest.plusDays(30),
            dias,
            horaInicio,
            horaFin
        );

        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Descripción trigger compuesto",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );

        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        return promo;
    }

    private Promocion crearPromocionPrecioFijo(
            String nombre,
            UUID productoId,
            int cantidadActivacion,
            BigDecimal precioPaquete,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new PrecioFijoPorCantidad(cantidadActivacion, precioPaquete);
        LocalDate fechaTest = LocalDate.ofInstant(clock.instant(), clock.getZone());
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            fechaTest.minusDays(1),
            fechaTest.plusDays(30)
        );

        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Descripción pack precio fijo",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );

        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        return promo;
    }
}
