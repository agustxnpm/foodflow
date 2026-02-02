package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.AbrirMesaRequest;
import com.agustinpalma.comandas.application.dto.AbrirMesaResponse;
import com.agustinpalma.comandas.application.dto.MesaResponse;
import com.agustinpalma.comandas.application.usecase.AbrirMesaUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarMesasUseCase;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para operaciones sobre mesas.
 * Expone endpoints HTTP y transforma requests/responses.
 * No contiene lógica de negocio, solo coordina entre HTTP y casos de uso.
 */
@RestController
@RequestMapping("/api/mesas")
public class MesaController {

    private final ConsultarMesasUseCase consultarMesasUseCase;
    private final AbrirMesaUseCase abrirMesaUseCase;

    public MesaController(
        ConsultarMesasUseCase consultarMesasUseCase,
        AbrirMesaUseCase abrirMesaUseCase
    ) {
        this.consultarMesasUseCase = consultarMesasUseCase;
        this.abrirMesaUseCase = abrirMesaUseCase;
    }

    /**
     * Obtiene todas las mesas del local actual.
     *
     * GET /api/mesas
     *
     * TODO: Implementar autenticación/autorización para obtener el localId del usuario logueado.
     *       Por ahora se usa un localId hardcodeado para permitir testing del endpoint.
     *
     * @return lista de mesas con su estado actual
     */
    @GetMapping
    public ResponseEntity<List<MesaResponse>> listarMesas() {
        // TODO: Reemplazar por extracción del localId desde el contexto de seguridad
        // Ejemplo futuro: LocalId localId = securityContext.getAuthenticatedUser().getLocalId();
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        List<MesaResponse> mesas = consultarMesasUseCase.ejecutar(localIdSimulado);

        return ResponseEntity.ok(mesas);
    }

    /**
     * Abre una mesa libre y crea su pedido inicial.
     *
     * POST /api/mesas/{mesaId}/abrir
     *
     * TODO: Implementar autenticación/autorización para obtener el localId del usuario logueado.
     *       Por ahora se usa un localId hardcodeado para permitir testing del endpoint.
     *
     * @param mesaId ID de la mesa a abrir
     * @return información de la mesa abierta y el pedido creado
     */
    @PostMapping("/{mesaId}/abrir")
    public ResponseEntity<AbrirMesaResponse> abrirMesa(@PathVariable String mesaId) {
        // TODO: Reemplazar por extracción del localId desde el contexto de seguridad
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        AbrirMesaRequest request = new AbrirMesaRequest(mesaId);
        AbrirMesaResponse response = abrirMesaUseCase.ejecutar(localIdSimulado, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
