package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.AjustarStockRequest;
import com.agustinpalma.comandas.application.dto.AjustarStockResponse;
import com.agustinpalma.comandas.application.dto.StockAjusteRequestBody;
import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.application.usecase.AjustarStockUseCase;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for stock management operations.
 * HU-22: Stock management.
 */
@RestController
@RequestMapping("/api/productos")
public class StockController {

    private final LocalContextProvider localContextProvider;
    private final AjustarStockUseCase ajustarStockUseCase;

    public StockController(
            LocalContextProvider localContextProvider,
            AjustarStockUseCase ajustarStockUseCase
    ) {
        this.localContextProvider = localContextProvider;
        this.ajustarStockUseCase = ajustarStockUseCase;
    }

    /**
     * PATCH /api/productos/{id}/stock
     * Adjusts stock for a product (manual adjustment or merchandise entry).
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<AjustarStockResponse> ajustarStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockAjusteRequestBody body
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();
        ProductoId productoId = new ProductoId(id);

        AjustarStockRequest request = new AjustarStockRequest(
            productoId,
            body.cantidad(),
            body.tipo(),
            body.motivo()
        );

        AjustarStockResponse response = ajustarStockUseCase.ejecutar(localId, request);
        return ResponseEntity.ok(response);
    }
}
