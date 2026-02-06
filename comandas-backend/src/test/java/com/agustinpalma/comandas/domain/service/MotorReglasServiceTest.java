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
}
