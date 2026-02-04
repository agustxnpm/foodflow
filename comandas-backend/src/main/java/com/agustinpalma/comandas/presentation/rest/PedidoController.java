package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.AgregarProductoRequest;
import com.agustinpalma.comandas.application.dto.AgregarProductoResponse;
import com.agustinpalma.comandas.application.usecase.AgregarProductoUseCase;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST para operaciones sobre pedidos.
 * Expone endpoints HTTP y transforma requests/responses.
 * No contiene lógica de negocio, solo coordina entre HTTP y casos de uso.
 * 
 * HU-05: Agregar productos a un pedido
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final AgregarProductoUseCase agregarProductoUseCase;

    public PedidoController(AgregarProductoUseCase agregarProductoUseCase) {
        this.agregarProductoUseCase = agregarProductoUseCase;
    }

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
        // Transformar de String HTTP a Value Objects del dominio
        PedidoId pedidoIdVO = new PedidoId(UUID.fromString(pedidoId));
        ProductoId productoIdVO = new ProductoId(UUID.fromString(body.productoId()));

        // Construir DTO de aplicación
        AgregarProductoRequest request = new AgregarProductoRequest(
            pedidoIdVO,
            productoIdVO,
            body.cantidad(),
            body.observaciones()
        );

        // Ejecutar caso de uso
        AgregarProductoResponse response = agregarProductoUseCase.ejecutar(request);

        return ResponseEntity.ok(response);
    }

    /**
     * DTO de entrada HTTP (solo para deserialización JSON).
     * Contiene tipos primitivos/String que vienen del cliente REST.
     */
    public record AgregarProductoRequestBody(
        String productoId,
        int cantidad,
        String observaciones
    ) {}
}
