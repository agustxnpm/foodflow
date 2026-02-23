package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.*;
import com.agustinpalma.comandas.application.usecase.AgregarProductoUseCase;
import com.agustinpalma.comandas.application.usecase.AplicarDescuentoManualUseCase;
import com.agustinpalma.comandas.application.usecase.GestionarItemsPedidoUseCase;
import com.agustinpalma.comandas.application.usecase.ReabrirPedidoUseCase;
import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.domain.model.DomainIds.ItemPedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.UUID;

/**
 * Controller REST para operaciones sobre pedidos.
 * Expone endpoints HTTP y transforma requests/responses.
 * No contiene lógica de negocio, solo coordina entre HTTP y casos de uso.
 * 
 * HU-05: Agregar productos a un pedido
 * HU-14: Aplicar descuentos manuales y reapertura de pedidos cerrados
 * HU-20: Eliminar producto de un pedido abierto
 * HU-21: Modificar cantidad de un producto en pedido abierto
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final LocalContextProvider localContextProvider;
    private final AgregarProductoUseCase agregarProductoUseCase;
    private final AplicarDescuentoManualUseCase aplicarDescuentoManualUseCase;
    private final GestionarItemsPedidoUseCase gestionarItemsPedidoUseCase;
    private final ReabrirPedidoUseCase reabrirPedidoUseCase;

    public PedidoController(
            LocalContextProvider localContextProvider,
            AgregarProductoUseCase agregarProductoUseCase,
            AplicarDescuentoManualUseCase aplicarDescuentoManualUseCase,
            GestionarItemsPedidoUseCase gestionarItemsPedidoUseCase,
            ReabrirPedidoUseCase reabrirPedidoUseCase
    ) {
        this.localContextProvider = localContextProvider;
        this.agregarProductoUseCase = agregarProductoUseCase;
        this.aplicarDescuentoManualUseCase = aplicarDescuentoManualUseCase;
        this.gestionarItemsPedidoUseCase = gestionarItemsPedidoUseCase;
        this.reabrirPedidoUseCase = reabrirPedidoUseCase;
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

        // Convertir extrasIds de String a ProductoId (puede ser null/vacío)
        List<ProductoId> extrasIdsVO = null;
        if (body.extrasIds() != null && !body.extrasIds().isEmpty()) {
            extrasIdsVO = body.extrasIds().stream()
                .map(id -> new ProductoId(UUID.fromString(id)))
                .toList();
        }

        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoIdVO,
            productoIdVO,
            body.cantidad(),
            body.observaciones(),
            extrasIdsVO
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

    // =================================================
    // ENDPOINTS - HU-20: Eliminar producto de pedido
    //           - HU-21: Modificar cantidad de producto
    // =================================================

    /**
     * Modifica la cantidad de un ítem en un pedido abierto.
     * 
     * PATCH /api/pedidos/{pedidoId}/items/{itemId}
     * Body: { "cantidad": 4 }
     * 
     * HU-21: La cantidad define la operación:
     * - cantidad > 0: actualiza la cantidad
     * - cantidad == 0: elimina el ítem
     * - cantidad == actual: operación idempotente
     * 
     * Cualquier cambio dispara recálculo completo de promociones (HU-10)
     * y el descuento global se ajusta dinámicamente (HU-14).
     * 
     * @param pedidoId ID del pedido (UUID en path)
     * @param itemId ID del ítem (UUID en path)
     * @param body JSON con la nueva cantidad
     * @return 200 OK con el pedido actualizado (mismo DTO que AgregarProducto)
     */
    @PatchMapping("/{pedidoId}/items/{itemId}")
    public ResponseEntity<AgregarProductoResponse> modificarCantidadItem(
            @PathVariable UUID pedidoId,
            @PathVariable UUID itemId,
            @Valid @RequestBody ModificarCantidadItemRequestBody body
    ) {
        ModificarCantidadItemRequest request = new ModificarCantidadItemRequest(
            new PedidoId(pedidoId),
            new ItemPedidoId(itemId),
            body.cantidad()
        );

        AgregarProductoResponse response = gestionarItemsPedidoUseCase.modificarCantidad(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina un ítem de un pedido abierto.
     * 
     * DELETE /api/pedidos/{pedidoId}/items/{itemId}
     * 
     * HU-20: Tras eliminar, se recalculan todas las promociones del pedido.
     * Si el ítem eliminado era trigger de un combo, el target pierde su descuento.
     * 
     * @param pedidoId ID del pedido (UUID en path)
     * @param itemId ID del ítem (UUID en path)
     * @return 200 OK con el pedido actualizado (mismo DTO que AgregarProducto)
     */
    @DeleteMapping("/{pedidoId}/items/{itemId}")
    public ResponseEntity<AgregarProductoResponse> eliminarItemPedido(
            @PathVariable UUID pedidoId,
            @PathVariable UUID itemId
    ) {
        EliminarItemPedidoRequest request = new EliminarItemPedidoRequest(
            new PedidoId(pedidoId),
            new ItemPedidoId(itemId)
        );

        AgregarProductoResponse response = gestionarItemsPedidoUseCase.eliminarItem(request);
        return ResponseEntity.ok(response);
    }

    // =================================================
    // ENDPOINTS - HU-14: Reapertura de Pedido
    // =================================================

    /**
     * Reabre un pedido previamente cerrado.
     * 
     * POST /api/pedidos/{pedidoId}/reapertura
     * 
     * HU-14: Válvula de escape para corregir errores operativos antes del cierre de caja.
     * 
     * Ejemplos de uso:
     * - Se cobró en efectivo cuando era tarjeta
     * - Se cerró la mesa equivocada
     * - Se olvidó agregar un ítem antes del cierre
     * 
     * ADVERTENCIA: Esta operación es destructiva.
     * - Elimina el snapshot contable
     * - Elimina todos los pagos registrados
     * - Revierte pedido a ABIERTO y mesa a ABIERTA
     * 
     * Validaciones:
     * - El pedido debe estar en estado CERRADO
     * - La mesa debe estar en estado LIBRE
     * - El pedido debe pertenecer al local (multi-tenancy)
     * 
     * Query Parameter:
     * - localId: UUID del local (obligatorio para validación multi-tenant)
     * 
     * @param pedidoId ID del pedido a reabrir (UUID en path)
     * @param localId ID del local para validación multi-tenant (query param)
     * @return 200 OK con información del pedido reabierto y mesa reocupada
     * @throws IllegalStateException si el pedido no está CERRADO o la mesa no está LIBRE (409 Conflict)
     * @throws IllegalArgumentException si el pedido no pertenece al local (403 Forbidden)
     */
    @PostMapping("/{pedidoId}/reapertura")
    public ResponseEntity<ReabrirPedidoResponse> reabrirPedido(
            @PathVariable UUID pedidoId
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        ReabrirPedidoResponse response = reabrirPedidoUseCase.ejecutar(
            localId,
            new PedidoId(pedidoId)
        );

        return ResponseEntity.ok(response);
    }
}
