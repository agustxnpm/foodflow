package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.*;
import com.agustinpalma.comandas.application.usecase.AgregarProductoUseCase;
import com.agustinpalma.comandas.application.usecase.AplicarDescuentoManualUseCase;
import com.agustinpalma.comandas.domain.model.DomainIds.ItemPedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST para operaciones sobre pedidos.
 * Expone endpoints HTTP y transforma requests/responses.
 * No contiene lógica de negocio, solo coordina entre HTTP y casos de uso.
 * 
 * HU-05: Agregar productos a un pedido
 * HU-14: Aplicar descuentos manuales
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final AgregarProductoUseCase agregarProductoUseCase;
    private final AplicarDescuentoManualUseCase aplicarDescuentoManualUseCase;

    public PedidoController(
            AgregarProductoUseCase agregarProductoUseCase,
            AplicarDescuentoManualUseCase aplicarDescuentoManualUseCase
    ) {
        this.agregarProductoUseCase = agregarProductoUseCase;
        this.aplicarDescuentoManualUseCase = aplicarDescuentoManualUseCase;
    }

    // =================================================
    // ENDPOINTS - HU-05: Agregar Productos
    // =================================================

    /**
     * Agrega un producto a un pedido existente.
     * 
     * POST /api/pedidos/{pedidoId}/items
     * 
     * Criterios de aceptación:
     * - AC1: Permite agregar el mismo producto múltiples veces
     * - AC2: Soporta cantidad y observaciones personalizadas
     * - AC3: Captura el precio del producto al momento de la adición (Snapshot)
     * - AC4: Solo funciona con pedidos ABIERTOS (409 si está cerrado)
     * - AC5: Valida que el producto pertenezca al mismo local (403 si no coincide)
     * 
     * TODO: Cuando se implemente ControllerAdvice:
     *   - IllegalStateException → 409 Conflict (pedido cerrado)
     *   - IllegalArgumentException → 400 Bad Request / 404 Not Found según mensaje
     * 
     * @param pedidoId ID del pedido (path variable)
     * @param body JSON con productoId, cantidad y observaciones
     * @return 200 OK con el pedido actualizado
     */
    @PostMapping("/{pedidoId}/items")
    public ResponseEntity<AgregarProductoResponse> agregarProducto(
        @PathVariable String pedidoId,
        @RequestBody AgregarProductoRequestBody body
    ) {
        PedidoId pedidoIdVO = new PedidoId(UUID.fromString(pedidoId));
        ProductoId productoIdVO = new ProductoId(UUID.fromString(body.productoId()));

        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoIdVO,
            productoIdVO,
            body.cantidad(),
            body.observaciones()
        );

        AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);
        return ResponseEntity.ok(response);
    }

    // =================================================
    // ENDPOINTS - HU-14: Descuentos Manuales
    // =================================================

    /**
     * Aplica descuento manual global al pedido completo.
     * 
     * POST /api/pedidos/{pedidoId}/descuento-manual
     * 
     * HU-14: El descuento es dinámico. Se recalcula cada vez que cambia el total del pedido.
     * 
     * Ejemplo de request:
     * {
     *   "porcentaje": 10.5,
     *   "razon": "Cliente frecuente",
     *   "usuarioId": "550e8400-e29b-41d4-a716-446655440000"
     * }
     * 
     * @param pedidoId ID del pedido (UUID en path)
     * @param requestBody DTO con porcentaje, razón y usuarioId
     * @return Respuesta con desglose completo de descuentos
     */
    @PostMapping("/{pedidoId}/descuento-manual")
    public ResponseEntity<AplicarDescuentoManualResponse> aplicarDescuentoGlobal(
            @PathVariable UUID pedidoId,
            @Valid @RequestBody DescuentoManualRequestBody requestBody
    ) {
        AplicarDescuentoManualRequest request = new AplicarDescuentoManualRequest(
            new PedidoId(pedidoId),
            null,  // null = descuento global
            requestBody.porcentaje(),
            requestBody.razon(),
            requestBody.usuarioId()
        );

        AplicarDescuentoManualResponse response = aplicarDescuentoManualUseCase.ejecutar(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Aplica descuento manual a un ítem específico del pedido.
     * 
     * POST /api/pedidos/{pedidoId}/items/{itemId}/descuento-manual
     * 
     * HU-14: El descuento es dinámico. Se recalcula sobre el remanente después de promociones automáticas.
     * 
     * Ejemplo de request:
     * {
     *   "porcentaje": 15,
     *   "razon": "Compensación por demora",
     *   "usuarioId": "550e8400-e29b-41d4-a716-446655440000"
     * }
     * 
     * @param pedidoId ID del pedido (UUID en path)
     * @param itemId ID del ítem (UUID en path)
     * @param requestBody DTO con porcentaje, razón y usuarioId
     * @return Respuesta con desglose completo de descuentos
     */
    @PostMapping("/{pedidoId}/items/{itemId}/descuento-manual")
    public ResponseEntity<AplicarDescuentoManualResponse> aplicarDescuentoPorItem(
            @PathVariable UUID pedidoId,
            @PathVariable UUID itemId,
            @Valid @RequestBody DescuentoManualRequestBody requestBody
    ) {
        AplicarDescuentoManualRequest request = new AplicarDescuentoManualRequest(
            new PedidoId(pedidoId),
            new ItemPedidoId(itemId),  // Descuento por ítem
            requestBody.porcentaje(),
            requestBody.razon(),
            requestBody.usuarioId()
        );

        AplicarDescuentoManualResponse response = aplicarDescuentoManualUseCase.ejecutar(request);
        return ResponseEntity.ok(response);
    }
}
