package com.agustinpalma.comandas.domain.service;

import com.agustinpalma.comandas.domain.model.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.CriterioActivacion.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Suite de tests del Motor de Reglas de Promociones.
 * 
 * HU-10: Aplicar promociones automáticamente.
 * 
 * Escenarios cubiertos:
 * 1. Descuento Directo (Happy Hour)
 * 2. Combo Condicional (Torta + Licuado)
 * 3. Resolución de conflictos por Prioridad
 * 4. Validación de Vigencia temporal
 * 5. Cantidad Fija (2x1)
 * 
 * Principios:
 * - Tests de dominio puros: sin Spring, sin base de datos
 * - Nombres descriptivos en español
 * - Estructura Given/When/Then implícita
 */
@DisplayName("MotorReglasService - Motor de Promociones")
class MotorReglasServiceTest {

    private MotorReglasService motorReglas;
    private LocalId localId;
    private Pedido pedido;
    
    // Productos de prueba
    private Producto cafe;
    private Producto medialunas;
    private Producto torta;
    private Producto licuado;
    private Producto cerveza;

    @BeforeEach
    void setUp() {
        motorReglas = new MotorReglasService();
        localId = LocalId.generate();
        
        // Crear pedido base
        pedido = crearPedidoAbierto();
        
        // Crear productos de prueba
        cafe = crearProducto("Café con leche", new BigDecimal("1500"));
        medialunas = crearProducto("Medialunas x3", new BigDecimal("1200"));
        torta = crearProducto("Torta de chocolate", new BigDecimal("2000"));
        licuado = crearProducto("Licuado de frutas", new BigDecimal("1800"));
        cerveza = crearProducto("Cerveza artesanal", new BigDecimal("2500"));
    }

    // =================================================
    // Escenario 1: DESCUENTO DIRECTO (Happy Hour)
    // =================================================

    @Nested
    @DisplayName("Descuento Directo")
    class DescuentoDirectoTests {

        @Test
        @DisplayName("debería aplicar descuento porcentual cuando el producto es TARGET de una promo activa")
        void deberia_aplicar_descuento_porcentual_cuando_producto_es_target() {
            // Given: Promo Happy Hour con 20% en cervezas, vigente hoy
            Promocion happyHour = crearPromocionDescuentoPorcentaje(
                "Happy Hour", 
                new BigDecimal("20"), 
                cerveza.getId().getValue(),
                10 // prioridad
            );
            List<Promocion> promociones = List.of(happyHour);
            LocalDateTime ahora = LocalDateTime.now();

            // When: Agrego una cerveza al pedido
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, cerveza, 1, null, promociones, ahora
            );

            // Then: Debería tener el descuento del 20% aplicado
            assertThat(item.tienePromocion()).isTrue();
            assertThat(item.getNombrePromocion()).isEqualTo("Happy Hour");
            assertThat(item.getMontoDescuento())
                .isEqualByComparingTo(new BigDecimal("500")); // 2500 * 20% = 500
            assertThat(item.calcularPrecioFinal())
                .isEqualByComparingTo(new BigDecimal("2000")); // 2500 - 500
        }

        @Test
        @DisplayName("debería aplicar descuento de monto fijo correctamente")
        void deberia_aplicar_descuento_monto_fijo() {
            // Given: Promo con $300 de descuento fijo en café
            Promocion promoFija = crearPromocionDescuentoFijo(
                "Promo Café", 
                new BigDecimal("300"), 
                cafe.getId().getValue(),
                5
            );
            List<Promocion> promociones = List.of(promoFija);
            LocalDateTime ahora = LocalDateTime.now();

            // When: Agrego 2 cafés
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, cafe, 2, null, promociones, ahora
            );

            // Then: Descuento = $300 * 2 = $600
            assertThat(item.tienePromocion()).isTrue();
            assertThat(item.getMontoDescuento())
                .isEqualByComparingTo(new BigDecimal("600")); // 300 * 2 unidades
            assertThat(item.calcularPrecioFinal())
                .isEqualByComparingTo(new BigDecimal("2400")); // (1500 * 2) - 600
        }

        @Test
        @DisplayName("no debería aplicar descuento si el producto NO es TARGET")
        void no_deberia_aplicar_descuento_si_producto_no_es_target() {
            // Given: Promo solo aplica a cerveza
            Promocion happyHour = crearPromocionDescuentoPorcentaje(
                "Happy Hour", 
                new BigDecimal("20"), 
                cerveza.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(happyHour);
            LocalDateTime ahora = LocalDateTime.now();

            // When: Agrego un CAFÉ (no es target)
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, cafe, 1, null, promociones, ahora
            );

            // Then: No debería tener promoción
            assertThat(item.tienePromocion()).isFalse();
            assertThat(item.getMontoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.calcularPrecioFinal())
                .isEqualByComparingTo(cafe.getPrecio());
        }
    }

    // =================================================
    // Escenario 2: COMBO CONDICIONAL (Torta + Licuado)
    // =================================================

    @Nested
    @DisplayName("Combo Condicional (Trigger/Target)")
    class ComboCondicionalTests {

        @Test
        @DisplayName("debería aplicar descuento al target cuando el trigger ya está en el pedido")
        void deberia_aplicar_descuento_cuando_trigger_presente() {
            // Given: Promo "Merienda" - si comprás torta (trigger), licuado tiene 50% off
            Promocion promoMerienda = crearPromocionCombo(
                "Promo Merienda",
                torta.getId().getValue(),   // trigger
                licuado.getId().getValue(), // target
                new BigDecimal("50"),       // 50% off en target
                1,                          // cantidad mínima trigger
                15                          // prioridad
            );
            
            // Agregar la torta (trigger) al pedido primero
            ItemPedido itemTorta = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(), torta, 1, null
            );
            pedido.agregarItem(itemTorta);
            
            List<Promocion> promociones = List.of(promoMerienda);
            LocalDateTime ahora = LocalDateTime.now();

            // When: Agrego el licuado (target)
            ItemPedido itemLicuado = motorReglas.aplicarReglas(
                pedido, licuado, 1, null, promociones, ahora
            );

            // Then: Licuado debería tener 50% de descuento
            assertThat(itemLicuado.tienePromocion()).isTrue();
            assertThat(itemLicuado.getNombrePromocion()).isEqualTo("Promo Merienda");
            assertThat(itemLicuado.getMontoDescuento())
                .isEqualByComparingTo(new BigDecimal("900")); // 1800 * 50% = 900
            assertThat(itemLicuado.calcularPrecioFinal())
                .isEqualByComparingTo(new BigDecimal("900")); // 1800 - 900
        }

        @Test
        @DisplayName("NO debería aplicar descuento al target si el trigger NO está en el pedido")
        void no_deberia_aplicar_descuento_sin_trigger() {
            // Given: Promo "Merienda" requiere torta como trigger
            Promocion promoMerienda = crearPromocionCombo(
                "Promo Merienda",
                torta.getId().getValue(),
                licuado.getId().getValue(),
                new BigDecimal("50"),
                1,
                15
            );
            
            // El pedido está VACÍO (sin torta)
            List<Promocion> promociones = List.of(promoMerienda);
            LocalDateTime ahora = LocalDateTime.now();

            // When: Agrego el licuado SIN haber agregado la torta
            ItemPedido itemLicuado = motorReglas.aplicarReglas(
                pedido, licuado, 1, null, promociones, ahora
            );

            // Then: Licuado debería estar a precio FULL
            assertThat(itemLicuado.tienePromocion()).isFalse();
            assertThat(itemLicuado.getMontoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(itemLicuado.calcularPrecioFinal())
                .isEqualByComparingTo(new BigDecimal("1800"));
        }

        @Test
        @DisplayName("debería requerir cantidad mínima de trigger para activar combo")
        void deberia_requerir_cantidad_minima_de_trigger() {
            // Given: Promo requiere 2 tortas como mínimo
            Promocion promoMerienda = crearPromocionCombo(
                "Promo Merienda x2",
                torta.getId().getValue(),
                licuado.getId().getValue(),
                new BigDecimal("50"),
                2, // requiere 2 tortas
                15
            );
            
            // Solo agregar UNA torta
            ItemPedido itemTorta = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(), torta, 1, null
            );
            pedido.agregarItem(itemTorta);
            
            List<Promocion> promociones = List.of(promoMerienda);
            LocalDateTime ahora = LocalDateTime.now();

            // When: Agrego licuado con solo 1 torta en el pedido
            ItemPedido itemLicuado = motorReglas.aplicarReglas(
                pedido, licuado, 1, null, promociones, ahora
            );

            // Then: NO aplica porque falta 1 torta
            assertThat(itemLicuado.tienePromocion()).isFalse();
        }
    }

    // =================================================
    // Escenario 3: RESOLUCIÓN DE CONFLICTOS POR PRIORIDAD
    // =================================================

    @Nested
    @DisplayName("Resolución de Conflictos por Prioridad")
    class PrioridadTests {

        @Test
        @DisplayName("debería aplicar la promoción con MAYOR prioridad cuando hay conflicto")
        void deberia_aplicar_promo_con_mayor_prioridad() {
            // Given: Dos promos que aplican al mismo producto
            // Promo A: 10% off, prioridad 1
            Promocion promoA = crearPromocionDescuentoPorcentaje(
                "Promo Básica", 
                new BigDecimal("10"), 
                cerveza.getId().getValue(),
                1 // prioridad baja
            );
            
            // Promo B: 30% off, prioridad 10
            Promocion promoB = crearPromocionDescuentoPorcentaje(
                "Super Promo", 
                new BigDecimal("30"), 
                cerveza.getId().getValue(),
                10 // prioridad alta
            );
            
            List<Promocion> promociones = List.of(promoA, promoB);
            LocalDateTime ahora = LocalDateTime.now();

            // When: Agrego cerveza
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, cerveza, 1, null, promociones, ahora
            );

            // Then: Debería ganar "Super Promo" (prioridad 10)
            assertThat(item.tienePromocion()).isTrue();
            assertThat(item.getNombrePromocion()).isEqualTo("Super Promo");
            assertThat(item.getMontoDescuento())
                .isEqualByComparingTo(new BigDecimal("750")); // 2500 * 30%
        }

        @Test
        @DisplayName("debería aplicar la primera encontrada si tienen igual prioridad")
        void deberia_aplicar_primera_si_igual_prioridad() {
            // Given: Dos promos con misma prioridad
            Promocion promoA = crearPromocionDescuentoPorcentaje(
                "Promo A", new BigDecimal("15"), cerveza.getId().getValue(), 5
            );
            Promocion promoB = crearPromocionDescuentoPorcentaje(
                "Promo B", new BigDecimal("20"), cerveza.getId().getValue(), 5
            );
            
            // Orden en lista: primero B (mayor descuento)
            List<Promocion> promociones = List.of(promoB, promoA);
            LocalDateTime ahora = LocalDateTime.now();

            // When
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, cerveza, 1, null, promociones, ahora
            );

            // Then: Alguna de las dos debería aplicar (ambas válidas)
            assertThat(item.tienePromocion()).isTrue();
        }
    }

    // =================================================
    // Escenario 4: VALIDACIÓN DE VIGENCIA TEMPORAL
    // =================================================

    @Nested
    @DisplayName("Vigencia Temporal")
    class VigenciaTemporalTests {

        @Test
        @DisplayName("NO debería aplicar promo de Martes si el pedido es de Miércoles")
        void no_deberia_aplicar_promo_si_dia_no_coincide() {
            // Given: Promo solo válida los martes
            Promocion promoMartes = crearPromocionDiaSemana(
                "Martes de Descuento",
                new BigDecimal("25"),
                cerveza.getId().getValue(),
                Set.of(DayOfWeek.TUESDAY),
                10
            );
            
            List<Promocion> promociones = List.of(promoMartes);
            
            // Simular que hoy es miércoles
            LocalDateTime miercoles = LocalDateTime.of(2026, 2, 4, 18, 0); // Miércoles

            // When: Agrego cerveza un miércoles
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, cerveza, 1, null, promociones, miercoles
            );

            // Then: No debería aplicar la promo
            assertThat(item.tienePromocion()).isFalse();
        }

        @Test
        @DisplayName("DEBERÍA aplicar promo de Martes si el pedido es de Martes")
        void deberia_aplicar_promo_si_dia_coincide() {
            // Given: Promo solo válida los martes
            Promocion promoMartes = crearPromocionDiaSemana(
                "Martes de Descuento",
                new BigDecimal("25"),
                cerveza.getId().getValue(),
                Set.of(DayOfWeek.TUESDAY),
                10
            );
            
            List<Promocion> promociones = List.of(promoMartes);
            
            // Simular que hoy es martes
            LocalDateTime martes = LocalDateTime.of(2026, 2, 3, 18, 0); // Martes

            // When: Agrego cerveza un martes
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, cerveza, 1, null, promociones, martes
            );

            // Then: Debería aplicar la promo
            assertThat(item.tienePromocion()).isTrue();
            assertThat(item.getNombrePromocion()).isEqualTo("Martes de Descuento");
        }

        @Test
        @DisplayName("NO debería aplicar promo fuera del rango de fechas")
        void no_deberia_aplicar_promo_fuera_de_fechas() {
            // Given: Promo válida del 1 al 15 de febrero
            Promocion promoFebrero = crearPromocionConFechas(
                "Promo Febrero",
                new BigDecimal("15"),
                cerveza.getId().getValue(),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 15),
                10
            );
            
            List<Promocion> promociones = List.of(promoFebrero);
            
            // Simular que estamos el 20 de febrero (fuera de rango)
            LocalDateTime fueraDeRango = LocalDateTime.of(2026, 2, 20, 14, 0);

            // When
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, cerveza, 1, null, promociones, fueraDeRango
            );

            // Then
            assertThat(item.tienePromocion()).isFalse();
        }
    }

    // =================================================
    // Escenario 5: CANTIDAD FIJA (2x1)
    // =================================================

    @Nested
    @DisplayName("Cantidad Fija (NxM)")
    class CantidadFijaTests {

        @Test
        @DisplayName("debería aplicar 2x1 correctamente")
        void deberia_aplicar_dos_por_uno() {
            // Given: Promo 2x1 en medialunas
            Promocion promo2x1 = crearPromocionCantidadFija(
                "2x1 Medialunas",
                2, 1, // lleva 2, paga 1
                medialunas.getId().getValue(),
                10
            );
            
            List<Promocion> promociones = List.of(promo2x1);
            LocalDateTime ahora = LocalDateTime.now();

            // When: Agrego 2 medialunas
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, medialunas, 2, null, promociones, ahora
            );

            // Then: Debería pagar solo 1 (descuento = precio de 1)
            assertThat(item.tienePromocion()).isTrue();
            assertThat(item.getNombrePromocion()).isEqualTo("2x1 Medialunas");
            assertThat(item.getMontoDescuento())
                .isEqualByComparingTo(new BigDecimal("1200")); // 1 unidad gratis
            assertThat(item.calcularPrecioFinal())
                .isEqualByComparingTo(new BigDecimal("1200")); // 2400 - 1200 = 1200
        }

        @Test
        @DisplayName("debería aplicar 3x2 con múltiples ciclos")
        void deberia_aplicar_tres_por_dos_multiples_ciclos() {
            // Given: Promo 3x2 en café
            Promocion promo3x2 = crearPromocionCantidadFija(
                "3x2 Café",
                3, 2, // lleva 3, paga 2
                cafe.getId().getValue(),
                10
            );
            
            List<Promocion> promociones = List.of(promo3x2);
            LocalDateTime ahora = LocalDateTime.now();

            // When: Agrego 6 cafés (2 ciclos de 3x2)
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, cafe, 6, null, promociones, ahora
            );

            // Then: 2 ciclos × 1 gratis = 2 cafés gratis
            // Descuento = 2 × $1500 = $3000
            assertThat(item.tienePromocion()).isTrue();
            assertThat(item.getMontoDescuento())
                .isEqualByComparingTo(new BigDecimal("3000"));
            assertThat(item.calcularPrecioFinal())
                .isEqualByComparingTo(new BigDecimal("6000")); // 9000 - 3000
        }

        @Test
        @DisplayName("NO debería aplicar 2x1 si solo hay 1 unidad")
        void no_deberia_aplicar_si_cantidad_insuficiente() {
            // Given: Promo 2x1
            Promocion promo2x1 = crearPromocionCantidadFija(
                "2x1 Medialunas",
                2, 1,
                medialunas.getId().getValue(),
                10
            );
            
            List<Promocion> promociones = List.of(promo2x1);
            LocalDateTime ahora = LocalDateTime.now();

            // When: Solo agrego 1 medialuna
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, medialunas, 1, null, promociones, ahora
            );

            // Then: No aplica el 2x1, pero SÍ debería detectar la promo (con descuento 0)
            // El ciclo completo requiere 2 unidades
            assertThat(item.getMontoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // =================================================
    // Escenario 6: SIN PROMOCIONES
    // =================================================

    @Nested
    @DisplayName("Sin Promociones Disponibles")
    class SinPromocionesTests {

        @Test
        @DisplayName("debería crear ítem sin descuento cuando no hay promociones")
        void deberia_crear_item_sin_descuento_si_no_hay_promos() {
            // Given: Lista vacía de promociones
            List<Promocion> promociones = Collections.emptyList();
            LocalDateTime ahora = LocalDateTime.now();

            // When
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, cafe, 1, "sin azúcar", promociones, ahora
            );

            // Then
            assertThat(item.tienePromocion()).isFalse();
            assertThat(item.getNombrePromocion()).isNull();
            assertThat(item.getMontoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.calcularSubtotal()).isEqualByComparingTo(item.calcularPrecioFinal());
            assertThat(item.getObservacion()).isEqualTo("sin azúcar");
        }

        @Test
        @DisplayName("debería crear ítem sin descuento cuando la promoción está INACTIVA")
        void deberia_ignorar_promociones_inactivas() {
            // Given: Promo que existe pero está INACTIVA
            Promocion promoInactiva = crearPromocionDescuentoPorcentaje(
                "Promo Desactivada",
                new BigDecimal("50"),
                cerveza.getId().getValue(),
                100
            );
            promoInactiva.desactivar(); // La desactivamos
            
            List<Promocion> promociones = List.of(promoInactiva);
            LocalDateTime ahora = LocalDateTime.now();

            // When
            ItemPedido item = motorReglas.aplicarReglas(
                pedido, cerveza, 1, null, promociones, ahora
            );

            // Then
            assertThat(item.tienePromocion()).isFalse();
        }
    }

    // =================================================
    // Escenario 7: EXTRAS EXCLUYEN PROMOCIONES
    // =================================================

    @Nested
    @DisplayName("Exclusión de promociones por extras")
    class ExtrasExcluyenPromocionesTests {

        @Test
        @DisplayName("producto base sin extras DEBE recibir promo normalmente")
        void producto_base_sin_extras_recibe_promo() {
            // Given: Promo 2x1 en cervezas
            Promocion promo2x1 = crearPromocionCantidadFija(
                "2x1 Cervezas",
                2, 1,
                cerveza.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(promo2x1);
            LocalDateTime ahora = LocalDateTime.now();

            // When: 2 cervezas SIN extras
            ItemPedido item = motorReglas.aplicarReglasConExtras(
                pedido, cerveza, 2, null,
                Collections.emptyList(), // sin extras
                promociones, ahora
            );

            // Then: Promo debe aplicar (1 cerveza gratis)
            assertThat(item.tienePromocion()).isTrue();
            assertThat(item.getNombrePromocion()).isEqualTo("2x1 Cervezas");
            assertThat(item.getMontoDescuento())
                .isEqualByComparingTo(cerveza.getPrecio()); // 1 unidad gratis
        }

        @Test
        @DisplayName("producto CON extras NO debe recibir promo — regla de producto base puro")
        void producto_con_extras_no_recibe_promo() {
            // Given: Promo 2x1 en hamburguesas
            Producto hamburguesa = crearProducto("Hamburguesa", new BigDecimal("5000"));
            Promocion promo2x1 = crearPromocionCantidadFija(
                "2x1 Hamburguesas",
                2, 1,
                hamburguesa.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(promo2x1);
            LocalDateTime ahora = LocalDateTime.now();

            // Extra: Cheddar
            Producto cheddarProducto = new Producto(
                ProductoId.generate(), localId, "Queso Cheddar",
                new BigDecimal("500"), true, "#FFD700",
                null, true, null // esExtra = true
            );
            ExtraPedido cheddar = ExtraPedido.crearDesdeProducto(cheddarProducto);

            // When: 2 hamburguesas CON cheddar
            ItemPedido item = motorReglas.aplicarReglasConExtras(
                pedido, hamburguesa, 2, null,
                List.of(cheddar, cheddar), // 2 extras
                promociones, ahora
            );

            // Then: promo NO debe aplicar (producto personalizado con extras)
            assertThat(item.tienePromocion()).isFalse();
            assertThat(item.getMontoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("descuento porcentual NO aplica a producto con extras")
        void descuento_porcentual_no_aplica_con_extras() {
            // Given: Happy Hour 20% en café
            Promocion happyHour = crearPromocionDescuentoPorcentaje(
                "Happy Hour",
                new BigDecimal("20"),
                cafe.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(happyHour);
            LocalDateTime ahora = LocalDateTime.now();

            // Extra: un shot de crema
            ExtraPedido cremaExtra = new ExtraPedido(
                ProductoId.generate(), "Crema", new BigDecimal("200")
            );

            // When: 1 café CON crema
            ItemPedido item = motorReglas.aplicarReglasConExtras(
                pedido, cafe, 1, null,
                List.of(cremaExtra),
                promociones, ahora
            );

            // Then: sin promo — producto con extras no califica
            assertThat(item.tienePromocion()).isFalse();
            assertThat(item.getMontoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);
            // Pero el extra sí debe estar presente
            assertThat(item.getExtras()).hasSize(1);
            assertThat(item.getExtras().get(0).getNombre()).isEqualTo("Crema");
        }

        @Test
        @DisplayName("observaciones diferentes SIN extras SÍ califican para promo — solo el producto base importa")
        void observaciones_diferentes_sin_extras_si_califican_para_promo() {
            // Given: Promo 20% en cerveza
            Promocion happyHour = crearPromocionDescuentoPorcentaje(
                "Happy Hour",
                new BigDecimal("20"),
                cerveza.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(happyHour);
            LocalDateTime ahora = LocalDateTime.now();

            // When: Cerveza con observación pero SIN extras
            ItemPedido item = motorReglas.aplicarReglasConExtras(
                pedido, cerveza, 1, "Bien fría por favor",
                Collections.emptyList(),
                promociones, ahora
            );

            // Then: Promo DEBE aplicar (la observación no afecta elegibilidad)
            assertThat(item.tienePromocion()).isTrue();
            assertThat(item.getNombrePromocion()).isEqualTo("Happy Hour");
            assertThat(item.getMontoDescuento())
                .isEqualByComparingTo(new BigDecimal("500")); // 2500 * 20%
        }

        @Test
        @DisplayName("recálculo de promos (HU-20/21) debe excluir ítems con extras")
        void recalculo_promos_excluye_items_con_extras() {
            // Given: Promo 20% en hamburguesa
            Producto hamburguesa = crearProducto("Hamburguesa", new BigDecimal("5000"));
            Promocion promo = crearPromocionDescuentoPorcentaje(
                "Promo Hamburguesa",
                new BigDecimal("20"),
                hamburguesa.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(promo);
            LocalDateTime ahora = LocalDateTime.now();

            // Crear ítem CON extras (ya en el pedido)
            ExtraPedido cheddar = new ExtraPedido(
                ProductoId.generate(), "Cheddar", new BigDecimal("500")
            );
            ItemPedido itemConExtras = ItemPedido.crearConExtras(
                ItemPedidoId.generate(), pedido.getId(),
                hamburguesa, 1, "sin cebolla",
                List.of(cheddar)
            );
            pedido.agregarItem(itemConExtras);

            // Crear ítem SIN extras (también en el pedido)
            ItemPedido itemSinExtras = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(),
                hamburguesa, 1, null
            );
            pedido.agregarItem(itemSinExtras);

            // Limpiar promos existentes (precondición del recálculo)
            pedido.limpiarPromocionesItems();

            // When: Recalcular promos de todo el pedido
            motorReglas.aplicarPromociones(pedido, promociones, ahora);

            // Then:
            // - Ítem CON extras: NO debe tener promo
            assertThat(itemConExtras.tienePromocion()).isFalse();
            assertThat(itemConExtras.getMontoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);

            // - Ítem SIN extras: SÍ debe tener promo (20%)
            assertThat(itemSinExtras.tienePromocion()).isTrue();
            assertThat(itemSinExtras.getNombrePromocion()).isEqualTo("Promo Hamburguesa");
            assertThat(itemSinExtras.getMontoDescuento())
                .isEqualByComparingTo(new BigDecimal("1000")); // 5000 * 20%
        }
    }

    // =================================================
    // Escenario 8: AGREGACIÓN CROSS-LÍNEA DE PROMOS
    // =================================================

    @Nested
    @DisplayName("Agregación cross-línea: promos consideran cantidad total del producto")
    class AgregacionCrossLineaTests {

        @Test
        @DisplayName("PrecioFijo: 3+1 cheeseburgers en líneas separadas → 2 ciclos de promo")
        void precio_fijo_agrega_cantidades_cross_linea() {
            // Given: Promo "2 Cheeseburgers por $24.000" (cada una $13.500)
            Producto cheeseburger = crearProducto("Cheeseburger", new BigDecimal("13500"));
            Promocion promo = crearPromocionPrecioFijoCantidad(
                "2x Cheeseburger $24.000",
                2,
                new BigDecimal("24000"),
                cheeseburger.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(promo);
            LocalDateTime ahora = LocalDateTime.now();

            // Crear 2 líneas: 3x cheeseburger + 1x cheeseburger "sin cebolla"
            ItemPedido linea1 = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(),
                cheeseburger, 3, null
            );
            ItemPedido linea2 = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(),
                cheeseburger, 1, "sin cebolla"
            );
            pedido.agregarItem(linea1);
            pedido.agregarItem(linea2);
            pedido.limpiarPromocionesItems();

            // When: Recalcular promos
            motorReglas.aplicarPromociones(pedido, promociones, ahora);

            // Then: 4 unidades total → 2 ciclos de "2 por $24.000"
            // cantidadEnCiclos = 4 (todos participan)
            // Descuento total: 2 × (2×13500 - 24000) = 2 × 3000 = $6.000
            // Línea 1 aporta 3 de 4 unidades → $6.000 × 3/4 = $4.500
            // Línea 2 aporta 1 de 4 unidades → $6.000 × 1/4 = $1.500
            assertThat(linea1.tienePromocion()).isTrue();
            assertThat(linea1.getMontoDescuento()).isEqualByComparingTo(new BigDecimal("4500"));

            assertThat(linea2.tienePromocion()).isTrue();
            assertThat(linea2.getNombrePromocion()).isEqualTo("2x Cheeseburger $24.000");
            assertThat(linea2.getMontoDescuento()).isEqualByComparingTo(new BigDecimal("1500"));

            // Total: (3×13500 - 4500) + (1×13500 - 1500) = 36000 + 12000 = $48.000 = 2 × $24.000
            BigDecimal totalEsperado = new BigDecimal("48000");
            BigDecimal totalReal = linea1.calcularPrecioFinal().add(linea2.calcularPrecioFinal());
            assertThat(totalReal).isEqualByComparingTo(totalEsperado);
        }

        @Test
        @DisplayName("PrecioFijo: 2+1 cheeseburgers → solo 1 ciclo, sobrante SIN promo (bug visual fix)")
        void precio_fijo_sobrante_no_muestra_promo() {
            // Given: Promo "2 Cheeseburgers por $24.000" (cada una $13.500)
            Producto cheeseburger = crearProducto("Cheeseburger", new BigDecimal("13500"));
            Promocion promo = crearPromocionPrecioFijoCantidad(
                "2x Cheeseburger $24.000",
                2,
                new BigDecimal("24000"),
                cheeseburger.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(promo);
            LocalDateTime ahora = LocalDateTime.now();

            // 2 líneas: 2x cheeseburger + 1x cheeseburger "sin cebolla" (sobrante)
            ItemPedido linea1 = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(),
                cheeseburger, 2, null
            );
            ItemPedido linea2 = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(),
                cheeseburger, 1, "sin cebolla"
            );
            pedido.agregarItem(linea1);
            pedido.agregarItem(linea2);
            pedido.limpiarPromocionesItems();

            // When
            motorReglas.aplicarPromociones(pedido, promociones, ahora);

            // Then: 3 total → 1 ciclo (2 unidades), 1 sobrante
            // cantidadEnCiclos = 2
            // Línea 1 (2 unidades) llena el ciclo → descuento $3.000 completo
            assertThat(linea1.tienePromocion()).isTrue();
            assertThat(linea1.getNombrePromocion()).isEqualTo("2x Cheeseburger $24.000");
            assertThat(linea1.getMontoDescuento()).isEqualByComparingTo(new BigDecimal("3000"));

            // Línea 2 (1 unidad) es sobrante → SIN promo, SIN descuento, SIN label
            assertThat(linea2.tienePromocion()).isFalse();
            assertThat(linea2.getMontoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);

            // Total: (2×13500 - 3000) + (1×13500) = 24000 + 13500 = $37.500
            BigDecimal totalEsperado = new BigDecimal("37500");
            BigDecimal totalReal = linea1.calcularPrecioFinal().add(linea2.calcularPrecioFinal());
            assertThat(totalReal).isEqualByComparingTo(totalEsperado);
        }

        @Test
        @DisplayName("CantidadFija (2x1): ítems del mismo producto en líneas separadas suman para promo")
        void cantidad_fija_agrega_cantidades_cross_linea() {
            // Given: Promo 2x1 en cerveza
            Promocion promo2x1 = crearPromocionCantidadFija(
                "2x1 Cerveza",
                2, 1,
                cerveza.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(promo2x1);
            LocalDateTime ahora = LocalDateTime.now();

            // 2 líneas: 1x cerveza + 1x cerveza "bien fría"
            ItemPedido linea1 = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(),
                cerveza, 1, null
            );
            ItemPedido linea2 = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(),
                cerveza, 1, "bien fría"
            );
            pedido.agregarItem(linea1);
            pedido.agregarItem(linea2);
            pedido.limpiarPromocionesItems();

            // When
            motorReglas.aplicarPromociones(pedido, promociones, ahora);

            // Then: 1+1=2 total → 1 ciclo 2x1 → 1 gratis → descuento $2.500
            // Distribuido 50/50 (1/2 cada uno)
            BigDecimal descuentoTotal = linea1.getMontoDescuento().add(linea2.getMontoDescuento());
            assertThat(descuentoTotal).isEqualByComparingTo(cerveza.getPrecio()); // $2.500

            assertThat(linea1.tienePromocion()).isTrue();
            assertThat(linea2.tienePromocion()).isTrue();
        }

        @Test
        @DisplayName("Línea única: sin cambio de comportamiento (regresión)")
        void linea_unica_sin_cambio_de_comportamiento() {
            // Given: Promo "2 Cheeseburgers por $24.000"
            Producto cheeseburger = crearProducto("Cheeseburger", new BigDecimal("13500"));
            Promocion promo = crearPromocionPrecioFijoCantidad(
                "2x Cheeseburger $24.000",
                2,
                new BigDecimal("24000"),
                cheeseburger.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(promo);
            LocalDateTime ahora = LocalDateTime.now();

            // Una sola línea con 3 unidades
            ItemPedido item = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(),
                cheeseburger, 3, null
            );
            pedido.agregarItem(item);
            pedido.limpiarPromocionesItems();

            // When
            motorReglas.aplicarPromociones(pedido, promociones, ahora);

            // Then: 3 unidades → 1 ciclo → descuento $3.000 (idéntico al comportamiento previo)
            assertThat(item.tienePromocion()).isTrue();
            assertThat(item.getMontoDescuento()).isEqualByComparingTo(new BigDecimal("3000"));
        }

        @Test
        @DisplayName("Ítem con extras NO suma al grupo cross-línea")
        void items_con_extras_no_suman_al_grupo() {
            // Given: Promo 2x1 en hamburguesa
            Producto hamburguesa = crearProducto("Hamburguesa", new BigDecimal("5000"));
            Promocion promo2x1 = crearPromocionCantidadFija(
                "2x1 Hamburguesa",
                2, 1,
                hamburguesa.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(promo2x1);
            LocalDateTime ahora = LocalDateTime.now();

            // Línea 1: 1x hamburguesa normal
            ItemPedido lineaBase = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(),
                hamburguesa, 1, null
            );
            // Línea 2: 1x hamburguesa CON cheddar (extra)
            ExtraPedido cheddar = new ExtraPedido(
                ProductoId.generate(), "Cheddar", new BigDecimal("500")
            );
            ItemPedido lineaConExtras = ItemPedido.crearConExtras(
                ItemPedidoId.generate(), pedido.getId(),
                hamburguesa, 1, null,
                List.of(cheddar)
            );
            pedido.agregarItem(lineaBase);
            pedido.agregarItem(lineaConExtras);
            pedido.limpiarPromocionesItems();

            // When
            motorReglas.aplicarPromociones(pedido, promociones, ahora);

            // Then: Solo 1 unidad base (la con extras NO cuenta) → no alcanza para 2x1
            assertThat(lineaBase.tienePromocion()).isFalse();
            assertThat(lineaConExtras.tienePromocion()).isFalse();
        }

        @Test
        @DisplayName("Descuento porcentual funciona independiente de líneas (sin cambio de resultado)")
        void descuento_porcentual_cross_linea_mismo_resultado() {
            // Given: Happy Hour 20% en café
            Promocion happyHour = crearPromocionDescuentoPorcentaje(
                "Happy Hour",
                new BigDecimal("20"),
                cafe.getId().getValue(),
                10
            );
            List<Promocion> promociones = List.of(happyHour);
            LocalDateTime ahora = LocalDateTime.now();

            // 2 líneas: 2x café + 1x café "con leche"
            ItemPedido linea1 = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(),
                cafe, 2, null
            );
            ItemPedido linea2 = ItemPedido.crearDesdeProducto(
                ItemPedidoId.generate(), pedido.getId(),
                cafe, 1, "con leche"
            );
            pedido.agregarItem(linea1);
            pedido.agregarItem(linea2);
            pedido.limpiarPromocionesItems();

            // When
            motorReglas.aplicarPromociones(pedido, promociones, ahora);

            // Then: 3 cafés total × $1.500 × 20% = $900 de descuento total
            // Línea 1 (2/3): $900 × 2/3 = $600
            // Línea 2 (1/3): $900 × 1/3 = $300
            assertThat(linea1.tienePromocion()).isTrue();
            assertThat(linea2.tienePromocion()).isTrue();

            BigDecimal descuentoTotal = linea1.getMontoDescuento().add(linea2.getMontoDescuento());
            assertThat(descuentoTotal).isEqualByComparingTo(new BigDecimal("900"));
        }
    }

    // =================================================
    // HELPERS: Métodos de construcción
    // =================================================

    private Pedido crearPedidoAbierto() {
        return new Pedido(
            PedidoId.generate(),
            localId,
            MesaId.generate(),
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );
    }

    private Producto crearProducto(String nombre, BigDecimal precio) {
        return new Producto(
            ProductoId.generate(),
            localId,
            nombre,
            precio,
            true,
            "#FFFFFF"
        );
    }

    private Promocion crearPromocionDescuentoPorcentaje(
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
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(30)
        );
        
        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Promo de prueba",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );
        
        // Definir alcance: el producto es TARGET
        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoTargetId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        
        return promo;
    }

    private Promocion crearPromocionDescuentoFijo(
            String nombre, 
            BigDecimal monto, 
            UUID productoTargetId,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new DescuentoDirecto(
            ModoDescuento.MONTO_FIJO, 
            monto
        );
        
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(30)
        );
        
        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Promo de prueba",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );
        
        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoTargetId);
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
        EstrategiaPromocion estrategia = new ComboCondicional(
            cantidadMinimaTrigger,
            porcentajeBeneficio
        );
        
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(30)
        );
        
        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Combo de prueba",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );
        
        // Alcance: producto trigger y producto target
        ItemPromocion itemTrigger = ItemPromocion.productoTrigger(productoTriggerId);
        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoTargetId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTrigger, itemTarget)));
        
        return promo;
    }

    private Promocion crearPromocionDiaSemana(
            String nombre,
            BigDecimal porcentaje,
            UUID productoTargetId,
            Set<DayOfWeek> diasValidos,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new DescuentoDirecto(
            ModoDescuento.PORCENTAJE,
            porcentaje
        );
        
        // Trigger temporal con días específicos
        CriterioActivacion trigger = new CriterioTemporal(
            LocalDate.now().minusDays(30),
            LocalDate.now().plusDays(30),
            diasValidos,
            null,
            null
        );
        
        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Promo días específicos",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );
        
        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoTargetId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        
        return promo;
    }

    private Promocion crearPromocionConFechas(
            String nombre,
            BigDecimal porcentaje,
            UUID productoTargetId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new DescuentoDirecto(
            ModoDescuento.PORCENTAJE,
            porcentaje
        );
        
        CriterioActivacion trigger = CriterioTemporal.soloFechas(fechaDesde, fechaHasta);
        
        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Promo con fechas",
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
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(30)
        );
        
        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Promo NxM",
            prioridad,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );
        
        ItemPromocion itemTarget = ItemPromocion.productoTarget(productoTargetId);
        promo.definirAlcance(new AlcancePromocion(List.of(itemTarget)));
        
        return promo;
    }

    private Promocion crearPromocionPrecioFijoCantidad(
            String nombre,
            int cantidadActivacion,
            BigDecimal precioPaquete,
            UUID productoTargetId,
            int prioridad
    ) {
        EstrategiaPromocion estrategia = new PrecioFijoPorCantidad(cantidadActivacion, precioPaquete);
        
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(30)
        );
        
        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            nombre,
            "Promo PrecioFijo",
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
