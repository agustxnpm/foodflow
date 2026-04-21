package com.agustinpalma.comandas.domain.service;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimientoStock;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.MovimientoStock;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.service.GestorStockService.ResultadoStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios puros del Domain Service GestorStockService.
 *
 * GestorStockService es lógica de dominio sin dependencias de framework
 * ni de repositorio: recibe los productos resueltos como Map y devuelve
 * un ResultadoStock con productos modificados y movimientos generados.
 *
 * Escenarios:
 * - registrarVenta: descuenta stock + genera movimiento VENTA (cantidad negativa).
 * - registrarVenta: omite productos sin controlaStock (sin efecto, sin movimiento).
 * - revertirVenta: repone stock + genera movimiento REAPERTURA_PEDIDO (cantidad positiva).
 * - registrarVenta: omite ítems cuyos productos ya no existen (Map sin la clave).
 *
 * Sin Spring. Sin mocks. Dominio puro.
 */
class GestorStockServiceTest {

    private GestorStockService service;
    private LocalId localId;
    private Producto pizza;     // controla stock
    private Producto bebida;    // NO controla stock

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 4, 21, 14, 0);

    @BeforeEach
    void setUp() {
        service = new GestorStockService();
        localId = LocalId.generate();

        // pizza: stock inicial 10, controla stock = true
        pizza = new Producto(
            ProductoId.generate(), localId, "Pizza",
            new BigDecimal("1000"), true, "#FF0000",
            null, false, false, null, null, true, true,
            10, true   // stockActual=10, controlaStock=true
        );
        // bebida: constructor de retrocompatibilidad → controlaStock=false
        bebida = new Producto(
            ProductoId.generate(), localId, "Agua mineral",
            new BigDecimal("500"), true, "#0000FF"
        );
    }

    /** Crea un pedido ABIERTO con un producto y cantidad. */
    private Pedido pedidoCon(Producto producto, int cantidad) {
        Pedido pedido = new Pedido(
            PedidoId.generate(), localId, MesaId.generate(),
            1, EstadoPedido.ABIERTO, FECHA.minusHours(2)
        );
        pedido.agregarProducto(producto, cantidad, null);
        return pedido;
    }

    @Test
    void deberia_descontar_stock_y_generar_movimiento_venta_para_producto_controlado() {
        // Given — pedido con 3 pizzas (stock previo 10)
        Pedido pedido = pedidoCon(pizza, 3);
        Map<ProductoId, Producto> productos = Map.of(pizza.getId(), pizza);

        // When
        ResultadoStock resultado = service.registrarVenta(pedido, productos, FECHA);

        // Then — pizza descontó 3 (10 - 3 = 7) y se generó 1 movimiento VENTA
        assertThat(pizza.getStockActual()).isEqualTo(7);
        assertThat(resultado.productosModificados()).containsExactly(pizza);
        assertThat(resultado.movimientos()).hasSize(1);

        MovimientoStock mov = resultado.movimientos().get(0);
        assertThat(mov.getTipo()).isEqualTo(TipoMovimientoStock.VENTA);
        assertThat(mov.getCantidad()).isEqualTo(-3);   // negativo: descuento
        assertThat(mov.getProductoId()).isEqualTo(pizza.getId());
        assertThat(mov.getLocalId()).isEqualTo(localId);
    }

    @Test
    void deberia_omitir_productos_sin_control_de_stock() {
        // Given — pedido con 2 bebidas (no controla stock)
        Pedido pedido = pedidoCon(bebida, 2);
        Map<ProductoId, Producto> productos = Map.of(bebida.getId(), bebida);

        // When
        ResultadoStock resultado = service.registrarVenta(pedido, productos, FECHA);

        // Then — sin descuento, sin movimiento, sin productos modificados
        assertThat(resultado.productosModificados()).isEmpty();
        assertThat(resultado.movimientos()).isEmpty();
    }

    @Test
    void deberia_reponer_stock_y_generar_movimiento_reapertura_al_revertir_venta() {
        // Given — pizza con stock previamente descontado (3 unidades vendidas)
        pizza.descontarStock(3);
        assertThat(pizza.getStockActual()).isEqualTo(7);

        Pedido pedido = pedidoCon(pizza, 3);
        Map<ProductoId, Producto> productos = Map.of(pizza.getId(), pizza);

        // When — se revierte la venta
        ResultadoStock resultado = service.revertirVenta(pedido, productos, FECHA);

        // Then — el stock vuelve al valor original (7 + 3 = 10)
        assertThat(pizza.getStockActual()).isEqualTo(10);
        assertThat(resultado.productosModificados()).containsExactly(pizza);
        assertThat(resultado.movimientos()).hasSize(1);

        MovimientoStock mov = resultado.movimientos().get(0);
        assertThat(mov.getTipo()).isEqualTo(TipoMovimientoStock.REAPERTURA_PEDIDO);
        assertThat(mov.getCantidad()).isEqualTo(3);   // positivo: reposición
    }

    @Test
    void deberia_ignorar_items_cuyo_producto_ya_no_existe() {
        // Given — pedido con pizza, pero el Map de productos NO la incluye
        // (ej. el producto fue eliminado del catálogo entre la venta y la consulta)
        Pedido pedido = pedidoCon(pizza, 5);
        Map<ProductoId, Producto> productosVacio = Map.of();

        // When
        ResultadoStock resultado = service.registrarVenta(pedido, productosVacio, FECHA);

        // Then — no hay descuento ni movimiento (no se puede operar sobre lo inexistente)
        assertThat(pizza.getStockActual()).isEqualTo(10);   // stock intacto
        assertThat(resultado.productosModificados()).isEmpty();
        assertThat(resultado.movimientos()).isEmpty();
    }
}
