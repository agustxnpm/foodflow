package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AjustarStockRequest;
import com.agustinpalma.comandas.application.dto.AjustarStockResponse;
import com.agustinpalma.comandas.domain.model.MovimientoStock;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.repository.MovimientoStockRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.service.GestorStockService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * HU-22: Caso de uso para ajustar manualmente el stock de un producto.
 * 
 * Permite registrar ingresos de mercadería o ajustes manuales
 * con trazabilidad completa en los movimientos de stock.
 * 
 * La operación es atómica: actualiza el stock del producto
 * y persiste el movimiento de auditoría en la misma transacción.
 */
@Transactional
public class AjustarStockUseCase {

    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final GestorStockService gestorStockService;
    private final Clock clock;

    public AjustarStockUseCase(
            ProductoRepository productoRepository,
            MovimientoStockRepository movimientoStockRepository,
            GestorStockService gestorStockService,
            Clock clock
    ) {
        this.productoRepository = Objects.requireNonNull(productoRepository, "El productoRepository es obligatorio");
        this.movimientoStockRepository = Objects.requireNonNull(movimientoStockRepository, "El movimientoStockRepository es obligatorio");
        this.gestorStockService = Objects.requireNonNull(gestorStockService, "El gestorStockService es obligatorio");
        this.clock = Objects.requireNonNull(clock, "El clock es obligatorio");
    }

    /**
     * Ejecuta el ajuste manual de stock.
     * 
     * @param localId identificador del local (tenant)
     * @param request datos del ajuste
     * @return respuesta con el estado actualizado del producto
     * @throws IllegalArgumentException si el producto no existe o no pertenece al local
     */
    public AjustarStockResponse ejecutar(LocalId localId, AjustarStockRequest request) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(request, "El request es obligatorio");

        // 1. Buscar el producto validando multi-tenancy
        Producto producto = productoRepository.buscarPorIdYLocal(request.productoId(), localId)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("No existe un producto con ID %s en este local", request.productoId().getValue())
            ));

        // 2. Delegar al domain service
        LocalDateTime ahora = LocalDateTime.now(clock);
        MovimientoStock movimiento = gestorStockService.ajustarStock(
            producto,
            request.cantidad(),
            request.tipo(),
            request.motivo(),
            ahora
        );

        // 3. Persistir cambios
        productoRepository.guardar(producto);
        movimientoStockRepository.guardar(movimiento);

        // 4. Retornar respuesta
        return AjustarStockResponse.fromDomain(producto, movimiento);
    }
}
