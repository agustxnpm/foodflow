package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ReabrirPedidoResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimientoStock;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.MovimientoStock;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.MovimientoStockRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.service.GestorStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests del caso de uso ReabrirPedidoUseCase (HU-14).
 *
 * Cubre la Regla R14 ("Reapertura revierte stock") con un test unitario puro,
 * complementando la cobertura existente en ReabrirPedidoIntegrationTest.
 *
 * Escenarios:
 *  1. Happy path con reposición de stock (controlaStock = true)
 *  2. Multi-tenancy: pedido de otro local → IllegalArgumentException, sin efectos
 *  3. Guard de estado: pedido ABIERTO → IllegalStateException (delegado a Pedido.reabrir())
 *  4. Producto sin control de stock no genera movimiento ni se persiste
 *
 * El GestorStockService se usa real (es un Domain Service puro sin dependencias externas).
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 * Sin Spring. Sin base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ReabrirPedidoUseCaseTest {

    @Mock private PedidoRepository           pedidoRepository;
    @Mock private MesaRepository             mesaRepository;
    @Mock private ProductoRepository         productoRepository;
    @Mock private MovimientoStockRepository  movimientoStockRepository;

    private ReabrirPedidoUseCase useCase;
    private LocalId localId;

    private static final LocalDateTime FECHA_APERTURA   = LocalDateTime.of(2026, 4, 21,  8,  0);
    private static final LocalDateTime FECHA_CIERRE     = LocalDateTime.of(2026, 4, 21, 14,  0);
    private static final LocalDateTime FECHA_REAPERTURA = LocalDateTime.of(2026, 4, 21, 15,  0);

    @BeforeEach
    void setUp() {
        localId = LocalId.generate();
        Clock clock = Clock.fixed(
            FECHA_REAPERTURA.toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
        );
        // GestorStockService es puro: sin dependencias, se instancia directamente
        useCase = new ReabrirPedidoUseCase(
            pedidoRepository, mesaRepository, productoRepository,
            movimientoStockRepository, new GestorStockService(), clock
        );
    }

    // ============================================================
    // Helpers
    // ============================================================

    /**
     * Crea un Producto con control de stock activado y stock inicial dado.
     * Constructor completo: 14 parámetros.
     */
    private Producto productoConStock(String nombre, String precio, int stockInicial) {
        return new Producto(
            ProductoId.generate(), localId, nombre,
            new BigDecimal(precio), true, "#FF0000",
            null, false, false, null, null, true, true,
            stockInicial, true   // stockActual=stockInicial, controlaStock=true
        );
    }

    /**
     * Crea un Pedido CERRADO con 1 producto y un único pago en efectivo.
     * Mesa asociada queda en estado LIBRE (precondición de reapertura).
     */
    private Pedido pedidoCerradoCon(Producto producto, int cantidad) {
        Pedido pedido = new Pedido(
            PedidoId.generate(), localId, MesaId.generate(),
            1, EstadoPedido.ABIERTO, FECHA_APERTURA
        );
        pedido.agregarProducto(producto, cantidad, null);
        BigDecimal total = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
        pedido.cerrar(
            List.of(new Pago(MedioPago.EFECTIVO, total, FECHA_CIERRE)),
            FECHA_CIERRE
        );
        return pedido;
    }

    /** Mesa en estado LIBRE — apta para reocupar. */
    private Mesa mesaLibre(MesaId id) {
        return new Mesa(id, localId, 7);   // constructor por defecto crea LIBRE
    }

    // ============================================================
    // 1. R14: reapertura revierte stock (happy path)
    // ============================================================

    @Nested
    class ReaperturaConStock {

        @Test
        void deberia_reabrir_pedido_y_revertir_stock_de_productos_controlados() {
            // Given — producto con stock inicial 10, vendieron 3 → stock real = 7
            Producto pizza = productoConStock("Pizza", "1000", 10);
            pizza.descontarStock(3);  // simular descuento previo de la venta original
            assertThat(pizza.getStockActual()).isEqualTo(7);

            Pedido pedido = pedidoCerradoCon(pizza, 3);
            Mesa mesa = mesaLibre(pedido.getMesaId());

            given(pedidoRepository.buscarPorId(pedido.getId())).willReturn(Optional.of(pedido));
            given(mesaRepository.buscarPorId(pedido.getMesaId())).willReturn(Optional.of(mesa));
            given(productoRepository.buscarPorIdYLocal(pizza.getId(), localId))
                .willReturn(Optional.of(pizza));

            // When
            ReabrirPedidoResponse response = useCase.ejecutar(localId, pedido.getId());

            // Then — pedido y mesa volvieron a estado activo
            assertThat(response).isNotNull();
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ABIERTO);
            assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.ABIERTA);

            // R14: el stock fue repuesto (7 + 3 = 10)
            assertThat(pizza.getStockActual()).isEqualTo(10);

            // Persistencias: pedido + mesa + producto + movimiento de stock REAPERTURA_PEDIDO
            then(pedidoRepository).should(times(1)).guardar(pedido);
            then(mesaRepository).should(times(1)).guardar(mesa);
            then(productoRepository).should(times(1)).guardar(pizza);

            ArgumentCaptor<MovimientoStock> movCaptor = ArgumentCaptor.forClass(MovimientoStock.class);
            then(movimientoStockRepository).should(times(1)).guardar(movCaptor.capture());
            MovimientoStock movimiento = movCaptor.getValue();
            assertThat(movimiento.getCantidad()).isEqualTo(3);   // positivo: reposición
            assertThat(movimiento.getTipo()).isEqualTo(TipoMovimientoStock.REAPERTURA_PEDIDO);
            assertThat(movimiento.getProductoId()).isEqualTo(pizza.getId());
        }
    }

    // ============================================================
    // 2. Guard: multi-tenancy
    // ============================================================

    @Nested
    class GuardMultiTenancy {

        @Test
        void deberia_rechazar_si_pedido_no_pertenece_al_local() {
            // Given — pedido de otro local
            LocalId otroLocalId = LocalId.generate();
            Producto productoAjeno = new Producto(
                ProductoId.generate(), otroLocalId, "Pizza",
                new BigDecimal("1000"), true, "#FF0000"
            );
            Pedido pedidoAjeno = new Pedido(
                PedidoId.generate(), otroLocalId, MesaId.generate(),
                1, EstadoPedido.ABIERTO, FECHA_APERTURA
            );
            pedidoAjeno.agregarProducto(productoAjeno, 1, null);
            pedidoAjeno.cerrar(
                List.of(new Pago(MedioPago.EFECTIVO, new BigDecimal("1000"), FECHA_CIERRE)),
                FECHA_CIERRE
            );

            given(pedidoRepository.buscarPorId(pedidoAjeno.getId()))
                .willReturn(Optional.of(pedidoAjeno));

            // When / Then — el use case bloquea el cross-tenant
            assertThatThrownBy(() -> useCase.ejecutar(localId, pedidoAjeno.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");

            // No se tocó nada más
            then(mesaRepository).should(never()).buscarPorId(any());
            then(pedidoRepository).should(never()).guardar(any());
            then(mesaRepository).should(never()).guardar(any());
            then(productoRepository).should(never()).guardar(any());
            then(movimientoStockRepository).should(never()).guardar(any());
        }
    }

    // ============================================================
    // 3. Guard: pedido no cerrado
    // ============================================================

    @Nested
    class GuardEstado {

        @Test
        void deberia_rechazar_si_pedido_no_esta_cerrado() {
            // Given — pedido ABIERTO (sin cerrar)
            Producto pizza = productoConStock("Pizza", "1000", 10);
            Pedido pedidoAbierto = new Pedido(
                PedidoId.generate(), localId, MesaId.generate(),
                1, EstadoPedido.ABIERTO, FECHA_APERTURA
            );
            pedidoAbierto.agregarProducto(pizza, 1, null);
            // NO se llama a cerrar()

            Mesa mesa = mesaLibre(pedidoAbierto.getMesaId());
            given(pedidoRepository.buscarPorId(pedidoAbierto.getId()))
                .willReturn(Optional.of(pedidoAbierto));
            given(mesaRepository.buscarPorId(pedidoAbierto.getMesaId()))
                .willReturn(Optional.of(mesa));
            given(productoRepository.buscarPorIdYLocal(pizza.getId(), localId))
                .willReturn(Optional.of(pizza));

            // When / Then — Pedido.reabrir() exige estado CERRADO
            assertThatThrownBy(() -> useCase.ejecutar(localId, pedidoAbierto.getId()))
                .isInstanceOf(IllegalStateException.class);

            // No se persistieron cambios
            then(pedidoRepository).should(never()).guardar(any());
            then(mesaRepository).should(never()).guardar(any());
        }
    }

    // ============================================================
    // 4. Producto sin control de stock: no genera movimiento
    // ============================================================

    @Nested
    class ProductoSinControlStock {

        @Test
        void deberia_reabrir_sin_generar_movimientos_de_stock_para_producto_no_controlado() {
            // Given — producto sin control de stock (constructor de retrocompatibilidad)
            Producto bebida = new Producto(
                ProductoId.generate(), localId, "Agua mineral",
                new BigDecimal("500"), true, "#0000FF"
            );  // controlaStock=false por defecto

            Pedido pedido = pedidoCerradoCon(bebida, 2);
            Mesa mesa = mesaLibre(pedido.getMesaId());

            given(pedidoRepository.buscarPorId(pedido.getId())).willReturn(Optional.of(pedido));
            given(mesaRepository.buscarPorId(pedido.getMesaId())).willReturn(Optional.of(mesa));
            given(productoRepository.buscarPorIdYLocal(bebida.getId(), localId))
                .willReturn(Optional.of(bebida));

            // When
            useCase.ejecutar(localId, pedido.getId());

            // Then — pedido y mesa reabren, pero el GestorStockService omite el producto
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ABIERTO);
            assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.ABIERTA);

            // Sin control de stock → ningún producto guardado, ningún movimiento generado
            then(productoRepository).should(never()).guardar(any());
            then(movimientoStockRepository).should(never()).guardar(any());

            // Pero pedido y mesa sí se persisten
            then(pedidoRepository).should(times(1)).guardar(pedido);
            then(mesaRepository).should(times(1)).guardar(mesa);
        }
    }
}
