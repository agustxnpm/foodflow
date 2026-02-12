package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AgregarProductoRequest;
import com.agustinpalma.comandas.application.dto.CerrarMesaResponse;
import com.agustinpalma.comandas.application.dto.PagoRequest;
import com.agustinpalma.comandas.application.dto.ReabrirPedidoResponse;
import com.agustinpalma.comandas.domain.model.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.infrastructure.config.TestClockConfig;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataPagoRepository;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * HU-14: Suite de Tests de Integración para Reapertura de Pedido.
 * 
 * Valida el flujo completo desde ReabrirPedidoUseCase hasta persistencia:
 * - Reversión de estado: CERRADO → ABIERTO (pedido), LIBRE → ABIERTA (mesa)
 * - Eliminación del snapshot contable (montos vuelven a null)
 * - Eliminación física de pagos (orphanRemoval en JPA)
 * - Validaciones de estado y multi-tenancy
 * - Atomicidad transaccional
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
@DisplayName("HU-14: Reapertura de Pedido - Suite de Integración")
class ReabrirPedidoIntegrationTest {

    @Autowired
    private ReabrirPedidoUseCase reabrirPedidoUseCase;

    @Autowired
    private CerrarMesaUseCase cerrarMesaUseCase;

    @Autowired
    private AgregarProductoUseCase agregarProductoUseCase;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private SpringDataPagoRepository springDataPagoRepository;

    // Datos base del test
    private LocalId localId;
    private Mesa mesa;
    private Pedido pedido;

    // Productos base
    private Producto hamburguesa;  // $2500
    private Producto cerveza;      // $1500

    @BeforeEach
    void setUp() {
        // Configurar Local y Mesa
        localId = LocalId.generate();
        mesa = new Mesa(MesaId.generate(), localId, 1);
        mesaRepository.guardar(mesa);

        // Abrir mesa y crear pedido
        mesa.abrir();
        mesaRepository.guardar(mesa);

        pedido = new Pedido(
            PedidoId.generate(),
            localId,
            mesa.getId(),
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );
        pedidoRepository.guardar(pedido);

        // Crear productos de catálogo
        hamburguesa = new Producto(
            ProductoId.generate(), localId,
            "Hamburguesa Completa", new BigDecimal("2500"), true, "#FF0000"
        );
        cerveza = new Producto(
            ProductoId.generate(), localId,
            "Cerveza Artesanal", new BigDecimal("1500"), true, "#FFCC00"
        );
        productoRepository.guardar(hamburguesa);
        productoRepository.guardar(cerveza);
    }

    // ============================================
    // Helpers
    // ============================================

    /**
     * Agrega un producto al pedido activo usando el caso de uso real.
     */
    private void agregarProductoAlPedido(Producto producto, int cantidad) {
        agregarProductoUseCase.ejecutar(
            new AgregarProductoRequest(pedido.getId(), producto.getId(), cantidad, null)
        );
    }

    /**
     * Cierra la mesa con el total calculado en efectivo.
     */
    private CerrarMesaResponse cerrarMesa() {
        // Refrescar el pedido para obtener el total actualizado
        pedido = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
        BigDecimal total = pedido.calcularTotal();

        List<PagoRequest> pagos = List.of(
            new PagoRequest(MedioPago.EFECTIVO, total)
        );

        return cerrarMesaUseCase.ejecutar(localId, mesa.getId(), pagos);
    }

    // ============================================
    // Escenario Exitoso: Reapertura Completa
    // ============================================

    @Nested
    @DisplayName("AC1-AC5: Reapertura exitosa con reversión completa")
    class ReaperturaExitosa {

        @Test
        @DisplayName("AC1: Debe permitir reabrir un pedido CERRADO con pagos y snapshot")
        void deberia_reabrir_pedido_cerrado_exitosamente() {
            // Given: Un pedido cerrado con snapshot y pagos
            agregarProductoAlPedido(hamburguesa, 2);  // $5000
            agregarProductoAlPedido(cerveza, 1);      // $1500
            // Total: $6500

            CerrarMesaResponse cierre = cerrarMesa();

            // Verificar que el cierre fue exitoso
            assertThat(cierre.pedidoEstado()).isEqualTo(EstadoPedido.CERRADO);
            assertThat(cierre.mesaEstado()).isEqualTo(EstadoMesa.LIBRE);
            assertThat(cierre.montoTotal()).isEqualByComparingTo(new BigDecimal("6500"));
            assertThat(cierre.pagos()).hasSize(1);

            // Verificar persistencia del cierre
            Pedido pedidoCerrado = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            assertThat(pedidoCerrado.getEstado()).isEqualTo(EstadoPedido.CERRADO);
            assertThat(pedidoCerrado.getMontoTotalFinal()).isNotNull();
            assertThat(pedidoCerrado.getPagos()).hasSize(1);

            long cantidadPagosAntes = springDataPagoRepository.count();
            assertThat(cantidadPagosAntes).isGreaterThan(0);

            // When: Se reabre el pedido
            ReabrirPedidoResponse response = reabrirPedidoUseCase.ejecutar(localId, pedido.getId());

            // Then AC2: El pedido vuelve a estado ABIERTO
            assertThat(response.pedidoEstado()).isEqualTo(EstadoPedido.ABIERTO);

            // Then AC4: La mesa vuelve a estado ABIERTA
            assertThat(response.mesaEstado()).isEqualTo(EstadoMesa.ABIERTA);

            // Then: Verificar que la reapertura se registró con timestamp
            assertThat(response.fechaReapertura()).isNotNull();

            // Then: Los ítems se conservan (2 líneas: hamburguesas y cerveza)
            assertThat(response.cantidadItems()).isEqualTo(2);

            // ============================================
            // Verificaciones de persistencia real
            // ============================================

            // Refrescar entidades desde BD
            Pedido pedidoReabierto = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            Mesa mesaReocupada = mesaRepository.buscarPorId(mesa.getId()).orElseThrow();

            // AC2: Pedido en estado ABIERTO
            assertThat(pedidoReabierto.getEstado())
                .as("El pedido debe volver a ABIERTO después de la reapertura")
                .isEqualTo(EstadoPedido.ABIERTO);

            // AC3: Snapshot contable limpiado (montos finales son null)
            assertThat(pedidoReabierto.getMontoSubtotalFinal())
                .as("El snapshot de subtotal debe limpiarse al reabrir")
                .isNull();

            assertThat(pedidoReabierto.getMontoDescuentosFinal())
                .as("El snapshot de descuentos debe limpiarse al reabrir")
                .isNull();

            assertThat(pedidoReabierto.getMontoTotalFinal())
                .as("El snapshot de total debe limpiarse al reabrir")
                .isNull();

            // AC3: Fecha de cierre limpiada
            assertThat(pedidoReabierto.getFechaCierre())
                .as("La fecha de cierre debe limpiarse al reabrir")
                .isNull();

            // AC3: Pagos eliminados físicamente
            assertThat(pedidoReabierto.getPagos())
                .as("La lista de pagos en el aggregate debe estar vacía")
                .isEmpty();

            // Verificar eliminación física en BD (orphanRemoval = true)
            long cantidadPagosDespues = springDataPagoRepository.count();
            assertThat(cantidadPagosDespues)
                .as("Los pagos deben haberse eliminado físicamente de la BD")
                .isLessThan(cantidadPagosAntes);

            // AC4: Mesa en estado ABIERTA
            assertThat(mesaReocupada.getEstado())
                .as("La mesa debe volver a ABIERTA después de la reapertura")
                .isEqualTo(EstadoMesa.ABIERTA);

            // Verificar que los ítems se conservaron (2 líneas de pedido)
            assertThat(pedidoReabierto.getItems())
                .as("Los ítems del pedido deben conservarse después de la reapertura")
                .hasSize(2);

            // Verificar que el pedido puede calcular total dinámicamente
            BigDecimal totalRecalculado = pedidoReabierto.calcularTotal();
            assertThat(totalRecalculado)
                .as("El pedido reabierto debe poder recalcular su total dinámicamente")
                .isEqualByComparingTo(new BigDecimal("6500"));
        }
    }

    // ============================================
    // Escenarios Fallidos: Validaciones
    // ============================================

    @Nested
    @DisplayName("Validaciones de estado y reglas de negocio")
    class ValidacionesDeEstado {

        @Test
        @DisplayName("AC1: Debe rechazar reapertura de pedido en estado ABIERTO")
        void deberia_rechazar_reapertura_de_pedido_abierto() {
            // Given: Un pedido que ya está ABIERTO
            agregarProductoAlPedido(hamburguesa, 1);

            Pedido pedidoAbierto = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            assertThat(pedidoAbierto.getEstado()).isEqualTo(EstadoPedido.ABIERTO);

            // When / Then: Intentar reabrir un pedido ya abierto debe fallar
            assertThatThrownBy(() -> reabrirPedidoUseCase.ejecutar(localId, pedido.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden reabrir pedidos en estado CERRADO")
                .hasMessageContaining("ABIERTO");
        }

        @Test
        @DisplayName("AC4: Debe rechazar reapertura si la mesa ya está ABIERTA")
        void deberia_rechazar_reapertura_si_mesa_esta_abierta() {
            // Given: Un pedido cerrado pero la mesa se manipuló manualmente a ABIERTA
            agregarProductoAlPedido(hamburguesa, 1);
            cerrarMesa();

            // Manipular la mesa a ABIERTA manualmente (escenario de error)
            Mesa mesaManipulada = mesaRepository.buscarPorId(mesa.getId()).orElseThrow();
            mesaManipulada.abrir();
            mesaRepository.guardar(mesaManipulada);

            // When / Then: La reapertura debe fallar porque la mesa no está LIBRE
            assertThatThrownBy(() -> reabrirPedidoUseCase.ejecutar(localId, pedido.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden reocupar mesas en estado LIBRE")
                .hasMessageContaining("ABIERTA");
        }

        @Test
        @DisplayName("AC5: Debe validar multi-tenancy (pedido no pertenece al local)")
        void deberia_validar_multi_tenancy() {
            // Given: Un pedido cerrado del local original
            agregarProductoAlPedido(hamburguesa, 1);
            cerrarMesa();

            // Given: Un local diferente
            LocalId otroLocalId = LocalId.generate();

            // When / Then: Intentar reabrir con otro localId debe fallar
            assertThatThrownBy(() -> reabrirPedidoUseCase.ejecutar(otroLocalId, pedido.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece al local");
        }

        @Test
        @DisplayName("Debe fallar si el pedido no existe")
        void deberia_fallar_si_pedido_no_existe() {
            // Given: Un ID de pedido que no existe
            PedidoId pedidoInexistente = PedidoId.generate();

            // When / Then
            assertThatThrownBy(() -> reabrirPedidoUseCase.ejecutar(localId, pedidoInexistente))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No existe un pedido con ID");
        }
    }

    // ============================================
    // Escenario de Atomicidad Transaccional
    // ============================================

    @Nested
    @DisplayName("Atomicidad: operación todo o nada")
    class AtomicidadTransaccional {

        @Test
        @DisplayName("Debe garantizar que pedido y mesa se reabren atómicamente")
        void deberia_garantizar_atomicidad_pedido_y_mesa() {
            // Given: Un pedido cerrado
            agregarProductoAlPedido(hamburguesa, 1);
            cerrarMesa();

            // When: Se reabre el pedido
            ReabrirPedidoResponse response = reabrirPedidoUseCase.ejecutar(localId, pedido.getId());

            // Then: Ambas entidades deben haber cambiado de estado
            Pedido pedidoReabierto = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            Mesa mesaReocupada = mesaRepository.buscarPorId(mesa.getId()).orElseThrow();

            assertThat(pedidoReabierto.getEstado()).isEqualTo(EstadoPedido.ABIERTO);
            assertThat(mesaReocupada.getEstado()).isEqualTo(EstadoMesa.ABIERTA);

            // Si falla la mesa, el pedido no debe reabrirse (rollback transaccional)
            // Este comportamiento está garantizado por @Transactional en el UseCase
        }
    }
}
