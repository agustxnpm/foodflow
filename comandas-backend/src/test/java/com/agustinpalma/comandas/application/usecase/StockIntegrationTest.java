package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.*;
import com.agustinpalma.comandas.domain.model.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.MovimientoStockRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * HU-22: Suite de integración para Gestión de Stock.
 * 
 * Valida los escenarios completos de stock integrados con los flujos existentes:
 * - Venta (cierre de mesa) descuenta stock
 * - Reapertura de pedido repone stock
 * - Ajuste manual modifica stock con auditoría
 * - Producto sin control de stock no se ve afectado
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestClockConfig.class)
@DisplayName("HU-22: Gestión de Stock - Suite de Integración")
class StockIntegrationTest {

    @Autowired
    private CerrarMesaUseCase cerrarMesaUseCase;

    @Autowired
    private ReabrirPedidoUseCase reabrirPedidoUseCase;

    @Autowired
    private AjustarStockUseCase ajustarStockUseCase;

    @Autowired
    private AgregarProductoUseCase agregarProductoUseCase;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    private LocalId localId;
    private Mesa mesa;
    private Pedido pedido;

    /**
     * Producto con control de stock activado: stock inicial = 10.
     */
    private Producto hamburguesaConStock;

    /**
     * Producto SIN control de stock: comportamiento legacy.
     */
    private Producto cervezaSinStock;

    @BeforeEach
    void setUp() {
        localId = LocalId.generate();

        // Crear mesa y abrirla
        mesa = new Mesa(MesaId.generate(), localId, 1);
        mesaRepository.guardar(mesa);
        mesa.abrir();
        mesaRepository.guardar(mesa);

        // Crear pedido abierto
        pedido = new Pedido(
            PedidoId.generate(),
            localId,
            mesa.getId(),
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );
        pedidoRepository.guardar(pedido);

        // Producto CON control de stock: stock=10, controlaStock=true
        hamburguesaConStock = new Producto(
            ProductoId.generate(), localId,
            "Hamburguesa Completa", new BigDecimal("2500"), true, "#FF0000",
            null, false, null,
            10, true  // stock=10, controlaStock=true
        );
        productoRepository.guardar(hamburguesaConStock);

        // Producto SIN control de stock: controlaStock=false
        cervezaSinStock = new Producto(
            ProductoId.generate(), localId,
            "Cerveza Artesanal", new BigDecimal("1500"), true, "#FFCC00"
            // Usa constructor retrocompatible: stock=0, controlaStock=false
        );
        productoRepository.guardar(cervezaSinStock);
    }

    /**
     * Agrega un producto al pedido a través del use case real.
     */
    private void agregarProductoAlPedido(Producto producto, int cantidad) {
        agregarProductoUseCase.ejecutar(
            new AgregarProductoRequest(pedido.getId(), producto.getId(), cantidad, null)
        );
    }

    /**
     * Cierra la mesa con pago único en efectivo por el total exacto.
     */
    private CerrarMesaResponse cerrarMesaConPagoTotal(BigDecimal total) {
        List<PagoRequest> pagos = List.of(
            new PagoRequest(MedioPago.EFECTIVO, total)
        );
        return cerrarMesaUseCase.ejecutar(localId, mesa.getId(), pagos);
    }

    // ============================================
    // Escenario 1: Venta descuenta stock
    // ============================================

    @Nested
    @DisplayName("Escenario 1: Venta con cierre de mesa descuenta stock")
    class VentaDescontarStock {

        @Test
        @DisplayName("Debe descontar stock al cerrar mesa con producto que controla stock")
        void deberia_descontar_stock_al_cerrar_mesa() {
            // Given: producto con stock=10, vendo 2 unidades
            agregarProductoAlPedido(hamburguesaConStock, 2);

            BigDecimal total = new BigDecimal("5000"); // 2 x $2500

            // When: cierro la mesa
            CerrarMesaResponse response = cerrarMesaConPagoTotal(total);

            // Then: el pedido se cerró exitosamente
            assertThat(response.pedidoEstado()).isEqualTo(EstadoPedido.CERRADO);
            assertThat(response.mesaEstado()).isEqualTo(EstadoMesa.LIBRE);

            // Then: el stock se descontó correctamente
            Producto productoActualizado = productoRepository.buscarPorId(hamburguesaConStock.getId())
                .orElseThrow();
            assertThat(productoActualizado.getStockActual()).isEqualTo(8); // 10 - 2

            // Then: se creó movimiento de auditoría tipo VENTA
            List<MovimientoStock> movimientos = movimientoStockRepository
                .buscarPorProducto(hamburguesaConStock.getId(), localId);
            assertThat(movimientos).hasSize(1);
            assertThat(movimientos.get(0).getTipo()).isEqualTo(TipoMovimientoStock.VENTA);
            assertThat(movimientos.get(0).getCantidad()).isEqualTo(-2);
        }
    }

    // ============================================
    // Escenario 2: Reapertura repone stock
    // ============================================

    @Nested
    @DisplayName("Escenario 2: Reapertura de pedido repone stock")
    class ReaperturaReponeStock {

        @Test
        @DisplayName("Debe reponer stock al reabrir pedido previamente cerrado")
        void deberia_reponer_stock_al_reabrir_pedido() {
            // Given: producto con stock=10, vendo 2 y cierro
            agregarProductoAlPedido(hamburguesaConStock, 2);
            BigDecimal total = new BigDecimal("5000");
            cerrarMesaConPagoTotal(total);

            // Verificar que el stock bajó a 8
            Producto despuesCierre = productoRepository.buscarPorId(hamburguesaConStock.getId())
                .orElseThrow();
            assertThat(despuesCierre.getStockActual()).isEqualTo(8);

            // When: reabro el pedido
            reabrirPedidoUseCase.ejecutar(localId, pedido.getId());

            // Then: el stock volvió a 10
            Producto despuesReapertura = productoRepository.buscarPorId(hamburguesaConStock.getId())
                .orElseThrow();
            assertThat(despuesReapertura.getStockActual()).isEqualTo(10);

            // Then: se creó movimiento de auditoría tipo REAPERTURA_PEDIDO
            List<MovimientoStock> movimientos = movimientoStockRepository
                .buscarPorProducto(hamburguesaConStock.getId(), localId);
            assertThat(movimientos).hasSize(2); // VENTA + REAPERTURA

            MovimientoStock movimientoReapertura = movimientos.stream()
                .filter(m -> m.getTipo() == TipoMovimientoStock.REAPERTURA_PEDIDO)
                .findFirst()
                .orElseThrow();
            assertThat(movimientoReapertura.getCantidad()).isEqualTo(2); // positivo = reposición
        }
    }

    // ============================================
    // Escenario 3: Ajuste manual de stock
    // ============================================

    @Nested
    @DisplayName("Escenario 3: Ajuste manual de stock")
    class AjusteManualStock {

        @Test
        @DisplayName("Debe aumentar stock con ingreso de mercadería")
        void deberia_aumentar_stock_con_ingreso_mercaderia() {
            // Given: producto con stock=10

            // When: ajusto +50 unidades por ingreso de mercadería
            AjustarStockRequest request = new AjustarStockRequest(
                hamburguesaConStock.getId(),
                50,
                TipoMovimientoStock.INGRESO_MERCADERIA,
                "Compra proveedor semanal"
            );
            AjustarStockResponse response = ajustarStockUseCase.ejecutar(localId, request);

            // Then: stock = 60 (10 + 50)
            assertThat(response.stockActual()).isEqualTo(60);
            assertThat(response.cantidadAjustada()).isEqualTo(50);
            assertThat(response.tipo()).isEqualTo(TipoMovimientoStock.INGRESO_MERCADERIA);

            // Then: se creó movimiento de auditoría
            List<MovimientoStock> movimientos = movimientoStockRepository
                .buscarPorProducto(hamburguesaConStock.getId(), localId);
            assertThat(movimientos).hasSize(1);
            assertThat(movimientos.get(0).getTipo()).isEqualTo(TipoMovimientoStock.INGRESO_MERCADERIA);
            assertThat(movimientos.get(0).getMotivo()).isEqualTo("Compra proveedor semanal");
        }

        @Test
        @DisplayName("Debe permitir ajuste negativo por merma")
        void deberia_permitir_ajuste_negativo_por_merma() {
            // Given: producto con stock=10

            // When: ajusto -3 por merma/rotura
            AjustarStockRequest request = new AjustarStockRequest(
                hamburguesaConStock.getId(),
                -3,
                TipoMovimientoStock.AJUSTE_MANUAL,
                "Merma por rotura en cocina"
            );
            AjustarStockResponse response = ajustarStockUseCase.ejecutar(localId, request);

            // Then: stock = 7 (10 - 3)
            assertThat(response.stockActual()).isEqualTo(7);
            assertThat(response.cantidadAjustada()).isEqualTo(-3);
        }
    }

    // ============================================
    // Escenario 4: Producto sin control de stock
    // ============================================

    @Nested
    @DisplayName("Escenario 4: Producto sin control de stock no se ve afectado")
    class ProductoSinControlStock {

        @Test
        @DisplayName("No debe modificar stock en producto con controlaStock=false")
        void no_deberia_modificar_stock_sin_control() {
            // Given: cerveza con controlaStock=false, stock=0
            agregarProductoAlPedido(cervezaSinStock, 3);

            BigDecimal total = new BigDecimal("4500"); // 3 x $1500

            // When: cierro la mesa
            cerrarMesaConPagoTotal(total);

            // Then: el stock NO cambió
            Producto productoActualizado = productoRepository.buscarPorId(cervezaSinStock.getId())
                .orElseThrow();
            assertThat(productoActualizado.getStockActual()).isEqualTo(0); // Sin cambio

            // Then: NO se crearon movimientos de stock
            List<MovimientoStock> movimientos = movimientoStockRepository
                .buscarPorProducto(cervezaSinStock.getId(), localId);
            assertThat(movimientos).isEmpty();
        }
    }

    // ============================================
    // Escenario extra: Atomicidad stock + cierre
    // ============================================

    @Nested
    @DisplayName("Escenario extra: Atomicidad y consistencia")
    class AtomicidadYConsistencia {

        @Test
        @DisplayName("Pedido con mix de productos con y sin stock")
        void deberia_manejar_mix_de_productos_con_y_sin_stock() {
            // Given: hamburguesa (controlaStock=true, stock=10) + cerveza (controlaStock=false)
            agregarProductoAlPedido(hamburguesaConStock, 1);
            agregarProductoAlPedido(cervezaSinStock, 2);

            BigDecimal total = new BigDecimal("5500"); // $2500 + 2*$1500

            // When: cierro la mesa
            cerrarMesaConPagoTotal(total);

            // Then: solo hamburguesa tuvo descuento de stock
            Producto hamburguesaActualizada = productoRepository.buscarPorId(hamburguesaConStock.getId())
                .orElseThrow();
            assertThat(hamburguesaActualizada.getStockActual()).isEqualTo(9); // 10 - 1

            Producto cervezaActualizada = productoRepository.buscarPorId(cervezaSinStock.getId())
                .orElseThrow();
            assertThat(cervezaActualizada.getStockActual()).isEqualTo(0); // Sin cambio

            // Then: solo 1 movimiento de stock (hamburguesa)
            List<MovimientoStock> movHamburguesa = movimientoStockRepository
                .buscarPorProducto(hamburguesaConStock.getId(), localId);
            assertThat(movHamburguesa).hasSize(1);

            List<MovimientoStock> movCerveza = movimientoStockRepository
                .buscarPorProducto(cervezaSinStock.getId(), localId);
            assertThat(movCerveza).isEmpty();
        }

        @Test
        @DisplayName("Stock permite valores negativos por flexibilidad operativa")
        void deberia_permitir_stock_negativo() {
            // Given: producto con stock=1, vendo 3 unidades
            Producto productoPocoStock = new Producto(
                ProductoId.generate(), localId,
                "Empanada Escasa", new BigDecimal("500"), true, "#FF5500",
                null, false, null,
                1, true  // stock=1, controlaStock=true
            );
            productoRepository.guardar(productoPocoStock);

            // Crear nueva mesa y pedido para este test
            Mesa mesa2 = new Mesa(MesaId.generate(), localId, 2);
            mesaRepository.guardar(mesa2);
            mesa2.abrir();
            mesaRepository.guardar(mesa2);

            Pedido pedido2 = new Pedido(
                PedidoId.generate(), localId, mesa2.getId(),
                2, EstadoPedido.ABIERTO, LocalDateTime.now()
            );
            pedidoRepository.guardar(pedido2);

            agregarProductoUseCase.ejecutar(
                new AgregarProductoRequest(pedido2.getId(), productoPocoStock.getId(), 3, null)
            );

            BigDecimal total = new BigDecimal("1500"); // 3 x $500

            List<PagoRequest> pagos = List.of(new PagoRequest(MedioPago.EFECTIVO, total));
            cerrarMesaUseCase.ejecutar(localId, mesa2.getId(), pagos);

            // Then: stock negativo es permitido
            Producto actualizado = productoRepository.buscarPorId(productoPocoStock.getId())
                .orElseThrow();
            assertThat(actualizado.getStockActual()).isEqualTo(-2); // 1 - 3 = -2
        }
    }
}
