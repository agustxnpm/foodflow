package com.agustinpalma.comandas.domain.service;

import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.MovimientoStock;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimientoStock;
import com.agustinpalma.comandas.domain.model.DomainIds.MovimientoStockId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Domain Service para la gestión de stock.
 * 
 * HU-22: Contiene lógica de dominio pura para operaciones de inventario.
 * 
 * Responsabilidades:
 * - Registrar descuento de stock por venta (cierre de mesa)
 * - Revertir stock por reapertura de pedido
 * - Registrar ajustes manuales
 * 
 * Este servicio NO tiene dependencias de framework.
 * La persistencia y la búsqueda de productos se manejan desde la capa de aplicación.
 * 
 * Decisión de diseño: El servicio recibe los productos ya resueltos como Map<ProductoId, Producto>
 * para evitar acoplar el dominio a interfaces de repositorio. La orquestación de carga
 * es responsabilidad de la capa de aplicación.
 */
public class GestorStockService {

    /**
     * Resultado de una operación de stock.
     * Contiene los productos modificados y los movimientos generados para persistir.
     */
    public record ResultadoStock(
        List<Producto> productosModificados,
        List<MovimientoStock> movimientos
    ) {
        public ResultadoStock {
            productosModificados = List.copyOf(productosModificados);
            movimientos = List.copyOf(movimientos);
        }
    }

    /**
     * Descuenta stock de cada producto vendido en el pedido.
     * Itera los ítems del pedido y descuenta la cantidad correspondiente.
     * 
     * Solo afecta productos con controlaStock == true.
     * Genera un MovimientoStock de tipo VENTA por cada producto afectado.
     * 
     * @param pedido el pedido cerrado
     * @param productosDelPedido mapa de ProductoId → Producto para los productos del pedido
     * @param fecha fecha del movimiento
     * @return resultado con productos modificados y movimientos generados
     */
    public ResultadoStock registrarVenta(Pedido pedido, Map<ProductoId, Producto> productosDelPedido, LocalDateTime fecha) {
        Objects.requireNonNull(pedido, "El pedido no puede ser null");
        Objects.requireNonNull(productosDelPedido, "Los productos del pedido no pueden ser null");
        Objects.requireNonNull(fecha, "La fecha no puede ser null");

        List<Producto> productosModificados = new ArrayList<>();
        List<MovimientoStock> movimientos = new ArrayList<>();

        for (ItemPedido item : pedido.getItems()) {
            Producto producto = productosDelPedido.get(item.getProductoId());
            if (producto == null) {
                continue; // Producto ya no existe, no se puede descontar
            }

            if (!producto.isControlaStock()) {
                continue;
            }

            producto.descontarStock(item.getCantidad());

            MovimientoStock movimiento = new MovimientoStock(
                MovimientoStockId.generate(),
                producto.getId(),
                pedido.getLocalId(),
                -item.getCantidad(),
                TipoMovimientoStock.VENTA,
                fecha,
                String.format("Venta - Pedido #%d - %s x%d",
                    pedido.getNumero(), item.getNombreProducto(), item.getCantidad())
            );

            productosModificados.add(producto);
            movimientos.add(movimiento);
        }

        return new ResultadoStock(productosModificados, movimientos);
    }

    /**
     * Revierte el stock descontado por un pedido al reabrirlo.
     * Repone la cantidad de cada ítem al inventario.
     * 
     * Solo afecta productos con controlaStock == true.
     * Genera un MovimientoStock de tipo REAPERTURA_PEDIDO por cada producto afectado.
     * 
     * @param pedido el pedido reabierto
     * @param productosDelPedido mapa de ProductoId → Producto para los productos del pedido
     * @param fecha fecha del movimiento
     * @return resultado con productos modificados y movimientos generados
     */
    public ResultadoStock revertirVenta(Pedido pedido, Map<ProductoId, Producto> productosDelPedido, LocalDateTime fecha) {
        Objects.requireNonNull(pedido, "El pedido no puede ser null");
        Objects.requireNonNull(productosDelPedido, "Los productos del pedido no pueden ser null");
        Objects.requireNonNull(fecha, "La fecha no puede ser null");

        List<Producto> productosModificados = new ArrayList<>();
        List<MovimientoStock> movimientos = new ArrayList<>();

        for (ItemPedido item : pedido.getItems()) {
            Producto producto = productosDelPedido.get(item.getProductoId());
            if (producto == null) {
                continue;
            }

            if (!producto.isControlaStock()) {
                continue;
            }

            producto.reponerStock(item.getCantidad());

            MovimientoStock movimiento = new MovimientoStock(
                MovimientoStockId.generate(),
                producto.getId(),
                pedido.getLocalId(),
                item.getCantidad(),
                TipoMovimientoStock.REAPERTURA_PEDIDO,
                fecha,
                String.format("Reapertura - Pedido #%d - %s x%d",
                    pedido.getNumero(), item.getNombreProducto(), item.getCantidad())
            );

            productosModificados.add(producto);
            movimientos.add(movimiento);
        }

        return new ResultadoStock(productosModificados, movimientos);
    }

    /**
     * Ajusta manualmente el stock de un producto.
     * 
     * REGLA DE NEGOCIO: Los ajustes manuales activan automáticamente el control de stock.
     * Cuando se realiza un ajuste de tipo INGRESO_MERCADERIA o AJUSTE_MANUAL,
     * el producto comienza a controlar inventario automáticamente (controlaStock = true).
     * 
     * @param producto el producto a ajustar
     * @param cantidad cantidad del ajuste (positiva para ingreso, negativa para egreso)
     * @param tipo tipo de movimiento (AJUSTE_MANUAL o INGRESO_MERCADERIA)
     * @param motivo descripción del ajuste
     * @param fecha fecha del movimiento
     * @return el movimiento generado
     * @throws IllegalArgumentException si el tipo no es AJUSTE_MANUAL ni INGRESO_MERCADERIA
     */
    public MovimientoStock ajustarStock(
            Producto producto,
            int cantidad,
            TipoMovimientoStock tipo,
            String motivo,
            LocalDateTime fecha
    ) {
        Objects.requireNonNull(producto, "El producto no puede ser null");
        Objects.requireNonNull(tipo, "El tipo de movimiento no puede ser null");
        Objects.requireNonNull(fecha, "La fecha no puede ser null");

        if (tipo != TipoMovimientoStock.AJUSTE_MANUAL && tipo != TipoMovimientoStock.INGRESO_MERCADERIA) {
            throw new IllegalArgumentException(
                String.format("Solo se permiten ajustes de tipo AJUSTE_MANUAL o INGRESO_MERCADERIA. Recibido: %s", tipo)
            );
        }

        if (cantidad == 0) {
            throw new IllegalArgumentException("La cantidad del ajuste no puede ser cero");
        }

        // INVARIANTE DE DOMINIO: Ajustes manuales activan el control de stock automáticamente
        if (!producto.isControlaStock()) {
            producto.activarControlStock();
        }

        // Aplicar el ajuste al producto
        if (cantidad > 0) {
            producto.reponerStock(cantidad);
        } else {
            producto.descontarStock(Math.abs(cantidad));
        }

        return new MovimientoStock(
            MovimientoStockId.generate(),
            producto.getId(),
            producto.getLocalId(),
            cantidad,
            tipo,
            fecha,
            motivo
        );
    }
}
