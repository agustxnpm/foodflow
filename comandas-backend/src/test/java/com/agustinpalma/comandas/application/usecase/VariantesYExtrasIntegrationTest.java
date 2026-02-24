package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AgregarProductoRequest;
import com.agustinpalma.comandas.application.dto.AgregarProductoResponse;
import com.agustinpalma.comandas.domain.model.*;
import com.agustinpalma.comandas.domain.model.CriterioActivacion.CriterioTemporal;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.CantidadFija;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.infrastructure.config.TestClockConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración E2E para el flujo completo de variantes, extras y promociones.
 * 
 * Valida el pipeline real:
 * AgregarProductoUseCase → NormalizadorVariantesService → MotorReglasService
 * → PedidoRepository → Database → Rehydration → Cálculo de precios
 * 
 * Sin mocks. Contexto real de Spring. Base de datos H2 en memoria.
 * Cada test es transaccional y se revierte automáticamente.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestClockConfig.class)
@DisplayName("Variantes y Extras - Test de Integración E2E")
class VariantesYExtrasIntegrationTest {

    @Autowired
    private AgregarProductoUseCase agregarProductoUseCase;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PromocionRepository promocionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    // IDs compartidos por toda la familia de hamburguesas
    private LocalId localId;
    private MesaId mesaId;
    private PedidoId pedidoId;

    // Productos base (variantes)
    private Producto hamburguesaSimple;
    private Producto hamburguesaDoble;
    private Producto hamburguesaTriple;

    // Extras
    private Producto discoCarne;
    private Producto huevo;

    @BeforeEach
    void setUp() {
        localId = LocalId.generate();
        mesaId = MesaId.generate();
        pedidoId = PedidoId.generate();

        // Persistir entidades base necesarias para FK (local, mesa)
        persistirLocal(localId);
        persistirMesa(mesaId, localId);

        // Crear y persistir la familia de hamburguesas
        configurarFamiliaHamburguesas();

        // Crear y persistir el pedido abierto
        persistirPedidoAbierto();
    }

    // ==========================================================================
    // TEST A — Normalización Automática: Simple + Disco = Doble
    // ==========================================================================

    @Test
    @DisplayName("TEST A: Simple + Disco de Carne → Normaliza a Doble (extras vacíos, precio 2500)")
    void simple_mas_disco_debe_normalizar_a_doble() {
        // Given: Hamburguesa Simple + 1 Disco de Carne como extra
        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId,
            hamburguesaSimple.getId(),
            1,
            null,
            List.of(discoCarne.getId())
        );

        // When: Se ejecuta el caso de uso real (UseCase → Normalizador → Motor → Repo)
        AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

        // Then: El ítem resultante debe ser Hamburguesa Doble (normalización automática)
        assertThat(response.items()).hasSize(1);
        var item = response.items().get(0);

        // El producto fue normalizado de Simple → Doble
        assertThat(item.nombreProducto()).isEqualTo("Hamburguesa Doble");

        // El precio es el de la Doble (2500), NO Simple + Disco (2000 + 500 = 2500 coincide, pero validamos el concepto)
        assertThat(item.precioUnitarioBase()).isEqualByComparingTo(new BigDecimal("2500"));

        // Los extras deben estar VACÍOS (el disco fue absorbido por la normalización)
        // Verificamos via persistencia rehidratada
        entityManager.flush();
        entityManager.clear();

        Pedido pedidoRehidratado = pedidoRepository.buscarPorId(pedidoId).orElseThrow();
        ItemPedido itemPersistido = pedidoRehidratado.getItems().get(0);

        assertThat(itemPersistido.getNombreProducto()).isEqualTo("Hamburguesa Doble");
        assertThat(itemPersistido.getPrecioUnitario()).isEqualByComparingTo(new BigDecimal("2500"));
        assertThat(itemPersistido.getExtras()).isEmpty();
        assertThat(itemPersistido.calcularPrecioFinal()).isEqualByComparingTo(new BigDecimal("2500"));
    }

    // ==========================================================================
    // TEST B — Límite de Variante Máxima: Triple + Disco = Triple + Extra
    // ==========================================================================

    @Test
    @DisplayName("TEST B: Triple + Disco de Carne → Permanece Triple con disco como extra (precio 3500)")
    void triple_mas_disco_permanece_triple_con_extra() {
        // Given: Hamburguesa Triple + 1 Disco de Carne como extra
        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId,
            hamburguesaTriple.getId(),
            1,
            null,
            List.of(discoCarne.getId())
        );

        // When
        AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

        // Then: El ítem sigue siendo Triple (ya es la variante máxima)
        assertThat(response.items()).hasSize(1);
        var item = response.items().get(0);

        assertThat(item.nombreProducto()).isEqualTo("Hamburguesa Triple");
        assertThat(item.precioUnitarioBase()).isEqualByComparingTo(new BigDecimal("3000"));

        // El total debe ser 3000 (base) + 500 (extra) = 3500
        assertThat(response.total()).isEqualByComparingTo(new BigDecimal("3500"));

        // Verificar persistencia: el extra debe estar guardado en items_pedido_extras
        entityManager.flush();
        entityManager.clear();

        Pedido pedidoRehidratado = pedidoRepository.buscarPorId(pedidoId).orElseThrow();
        ItemPedido itemPersistido = pedidoRehidratado.getItems().get(0);

        assertThat(itemPersistido.getNombreProducto()).isEqualTo("Hamburguesa Triple");
        assertThat(itemPersistido.getExtras()).hasSize(1);
        assertThat(itemPersistido.getExtras().get(0).getNombre()).isEqualTo("Disco de Carne");
        assertThat(itemPersistido.getExtras().get(0).getPrecioSnapshot()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(itemPersistido.calcularPrecioFinal()).isEqualByComparingTo(new BigDecimal("3500"));
    }

    // ==========================================================================
    // TEST C — Extras Normales: Simple + Huevo
    // ==========================================================================

    @Test
    @DisplayName("TEST C: Simple + Huevo → Permanece Simple con huevo como extra (precio 2200)")
    void simple_mas_huevo_permanece_simple_con_extra() {
        // Given: Hamburguesa Simple + 1 Huevo como extra
        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId,
            hamburguesaSimple.getId(),
            1,
            null,
            List.of(huevo.getId())
        );

        // When
        AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

        // Then: El ítem sigue siendo Simple (el huevo no activa normalización)
        assertThat(response.items()).hasSize(1);
        var item = response.items().get(0);

        assertThat(item.nombreProducto()).isEqualTo("Hamburguesa Simple");
        assertThat(item.precioUnitarioBase()).isEqualByComparingTo(new BigDecimal("2000"));

        // Total: 2000 (base) + 200 (huevo) = 2200
        assertThat(response.total()).isEqualByComparingTo(new BigDecimal("2200"));

        // Verificar persistencia
        entityManager.flush();
        entityManager.clear();

        Pedido pedidoRehidratado = pedidoRepository.buscarPorId(pedidoId).orElseThrow();
        ItemPedido itemPersistido = pedidoRehidratado.getItems().get(0);

        assertThat(itemPersistido.getExtras()).hasSize(1);
        assertThat(itemPersistido.getExtras().get(0).getNombre()).isEqualTo("Huevo");
        assertThat(itemPersistido.calcularPrecioFinal()).isEqualByComparingTo(new BigDecimal("2200"));
    }

    // ==========================================================================
    // TEST D — Aislamiento de Promociones: 2x1 NO descuenta extras (CRÍTICO)
    // ==========================================================================

    @Test
    @DisplayName("TEST D: 2x1 NO aplica a producto con extras — extras personalizan el producto")
    void promocion_2x1_no_debe_descontar_extras() {
        // Given: Promoción 2x1 para Hamburguesa Doble
        Promocion promo2x1 = crearPromocion2x1ParaDoble();
        promocionRepository.guardar(promo2x1);

        // Agregar 2 unidades de Hamburguesa Doble, cada una con 1 Huevo (extra)
        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoId,
            hamburguesaDoble.getId(),
            2,
            null,
            List.of(huevo.getId())
        );

        // When: Se agrega el producto con la promoción activa
        AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

        // Then: Regla de negocio: producto con extras NO califica para promo.
        // Un producto personalizado con extras no es el "producto base" que la promo busca.
        //
        // Cálculo esperado SIN promo:
        // Precio base: 2 × 2500 = 5000
        // Extras: 2 × 200 = 400 (cada unidad lleva 1 huevo)
        // Total: 5000 + 400 = 5400 (sin descuento)

        assertThat(response.items()).hasSize(1);
        var item = response.items().get(0);

        assertThat(item.precioUnitarioBase()).isEqualByComparingTo(new BigDecimal("2500"));
        assertThat(item.cantidad()).isEqualTo(2);

        // Subtotal ítems: base (5000) + extras (400) = 5400
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("5400"));

        // SIN descuento — producto con extras excluido de promo
        assertThat(response.totalDescuentos()).isEqualByComparingTo(BigDecimal.ZERO);

        // Total a pagar: 5400 completo
        assertThat(response.total()).isEqualByComparingTo(new BigDecimal("5400"));

        // Verificar persistencia y rehidratación
        entityManager.flush();
        entityManager.clear();

        Pedido pedidoRehidratado = pedidoRepository.buscarPorId(pedidoId).orElseThrow();
        ItemPedido itemPersistido = pedidoRehidratado.getItems().get(0);

        // Los extras están presentes
        assertThat(itemPersistido.getExtras()).hasSize(1);
        assertThat(itemPersistido.getExtras().get(0).getPrecioSnapshot()).isEqualByComparingTo(new BigDecimal("200"));

        // Sin promo aplicada
        assertThat(itemPersistido.getMontoDescuento()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(itemPersistido.tienePromocion()).isFalse();

        // Precio final: base + extras, sin descuento
        assertThat(itemPersistido.calcularPrecioFinal()).isEqualByComparingTo(new BigDecimal("5400"));
    }

    // ==========================================================================
    // HELPERS: Configuración de datos de prueba persistidos en BD real
    // ==========================================================================

    /**
     * Configura y persiste la familia completa de hamburguesas en la base de datos.
     * 
     * Crea un grupo de variantes compartido y persiste:
     * - 3 variantes base (Simple, Doble, Triple) con cantidadDiscosCarne creciente
     * - 3 extras (Disco de Carne, Huevo, Bacon)
     * 
     * Todos los productos pertenecen al mismo local (multi-tenancy).
     */
    private void configurarFamiliaHamburguesas() {
        // UUID compartido para el grupo de variantes de hamburguesas
        ProductoId grupoVarianteId = ProductoId.generate();

        // --- Variantes base ---

        hamburguesaSimple = productoRepository.guardar(new Producto(
            ProductoId.generate(), localId,
            "Hamburguesa Simple", new BigDecimal("2000"), true, "#8B4513",
            grupoVarianteId, false, 1
        ));

        hamburguesaDoble = productoRepository.guardar(new Producto(
            ProductoId.generate(), localId,
            "Hamburguesa Doble", new BigDecimal("2500"), true, "#8B4513",
            grupoVarianteId, false, 2
        ));

        hamburguesaTriple = productoRepository.guardar(new Producto(
            ProductoId.generate(), localId,
            "Hamburguesa Triple", new BigDecimal("3000"), true, "#8B4513",
            grupoVarianteId, false, 3
        ));

        // --- Extras ---

        discoCarne = productoRepository.guardar(new Producto(
            ProductoId.generate(), localId,
            "Disco de Carne", new BigDecimal("500"), true, "#A0522D",
            null, true, null
        ));

        huevo = productoRepository.guardar(new Producto(
            ProductoId.generate(), localId,
            "Huevo", new BigDecimal("200"), true, "#FFD700",
            null, true, null
        ));

        productoRepository.guardar(new Producto(
            ProductoId.generate(), localId,
            "Bacon", new BigDecimal("300"), true, "#DC143C",
            null, true, null
        ));
    }

    /**
     * Persiste un pedido abierto vinculado al local y mesa de prueba.
     */
    private void persistirPedidoAbierto() {
        Pedido pedido = new Pedido(
            pedidoId, localId, mesaId, 1,
            EstadoPedido.ABIERTO, LocalDateTime.now()
        );
        pedidoRepository.guardar(pedido);
    }

    /**
     * Persiste un local directamente via EntityManager (sin pasar por domain layer).
     * Necesario porque la tabla locales es referenciada implícitamente por las entidades.
     */
    private void persistirLocal(LocalId id) {
        entityManager.createNativeQuery(
            "INSERT INTO locales (id, nombre) VALUES (:id, :nombre)"
        )
        .setParameter("id", id.getValue())
        .setParameter("nombre", "Local de Test")
        .executeUpdate();
    }

    /**
     * Persiste una mesa directamente via EntityManager.
     */
    private void persistirMesa(MesaId id, LocalId localId) {
        entityManager.createNativeQuery(
            "INSERT INTO mesas (id, local_id, numero, estado) VALUES (:id, :localId, :numero, :estado)"
        )
        .setParameter("id", id.getValue())
        .setParameter("localId", localId.getValue())
        .setParameter("numero", 1)
        .setParameter("estado", "LIBRE")
        .executeUpdate();
    }

    /**
     * Crea y retorna una promoción 2x1 para Hamburguesa Doble.
     * Vigente en la fecha del TestClockConfig, con prioridad alta.
     */
    private Promocion crearPromocion2x1ParaDoble() {
        EstrategiaPromocion estrategia = new CantidadFija(2, 1);

        LocalDate fechaTest = LocalDate.ofInstant(clock.instant(), clock.getZone());
        CriterioActivacion trigger = CriterioTemporal.soloFechas(
            fechaTest.minusDays(1),
            fechaTest.plusDays(30)
        );

        Promocion promo = new Promocion(
            PromocionId.generate(),
            localId,
            "2x1 Hamburguesa Doble",
            "Promoción 2x1 en Hamburguesa Doble",
            10,
            EstadoPromocion.ACTIVA,
            estrategia,
            List.of(trigger)
        );

        // Definir alcance: target = Hamburguesa Doble
        ItemPromocion target = ItemPromocion.productoTarget(hamburguesaDoble.getId().getValue());
        promo.definirAlcance(new AlcancePromocion(List.of(target)));

        return promo;
    }
}
