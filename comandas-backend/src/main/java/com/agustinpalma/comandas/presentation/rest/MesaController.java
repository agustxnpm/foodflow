package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.AbrirMesaRequest;
import com.agustinpalma.comandas.application.dto.AbrirMesaResponse;
import com.agustinpalma.comandas.application.dto.CerrarMesaRequest;
import com.agustinpalma.comandas.application.dto.CerrarMesaResponse;
import com.agustinpalma.comandas.application.dto.CrearMesaRequest;
import com.agustinpalma.comandas.application.dto.DetallePedidoResponse;
import com.agustinpalma.comandas.application.dto.MesaResponse;
import com.agustinpalma.comandas.application.usecase.AbrirMesaUseCase;
import com.agustinpalma.comandas.application.usecase.CerrarMesaUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarDetallePedidoUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarMesasUseCase;
import com.agustinpalma.comandas.application.usecase.CrearMesaUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarMesaUseCase;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
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
    private final CerrarMesaUseCase cerrarMesaUseCase;
    private final CrearMesaUseCase crearMesaUseCase;
    private final EliminarMesaUseCase eliminarMesaUseCase;
    private final ConsultarDetallePedidoUseCase consultarDetallePedidoUseCase;

    public MesaController(
        ConsultarMesasUseCase consultarMesasUseCase,
        AbrirMesaUseCase abrirMesaUseCase,
        CerrarMesaUseCase cerrarMesaUseCase,
        CrearMesaUseCase crearMesaUseCase,
        EliminarMesaUseCase eliminarMesaUseCase,
        ConsultarDetallePedidoUseCase consultarDetallePedidoUseCase
    ) {
        this.consultarMesasUseCase = consultarMesasUseCase;
        this.abrirMesaUseCase = abrirMesaUseCase;
        this.cerrarMesaUseCase = cerrarMesaUseCase;
        this.crearMesaUseCase = crearMesaUseCase;
        this.eliminarMesaUseCase = eliminarMesaUseCase;
        this.consultarDetallePedidoUseCase = consultarDetallePedidoUseCase;
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

    /**
     * Crea una nueva mesa en el local.
     *
     * POST /api/mesas
     *
     * Request body: { "numero": 15 }
     *
     * TODO: Implementar autenticación/autorización para obtener el localId del usuario logueado.
     *       Por ahora se usa un localId hardcodeado para permitir testing del endpoint.
     * TODO: Implementar manejo de excepciones con @ControllerAdvice para capturar:
     *       - MesaDuplicadaException -> 409 Conflict
     *       - NumeroMesaInvalidoException -> 400 Bad Request
     *
     * @param request DTO con el número de la nueva mesa
     * @return información de la mesa creada (en estado LIBRE)
     */
    @PostMapping
    public ResponseEntity<MesaResponse> crearMesa(@RequestBody CrearMesaRequest request) {
        // TODO: Reemplazar por extracción del localId desde el contexto de seguridad
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        MesaResponse response = crearMesaUseCase.ejecutar(localIdSimulado, request.numero());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Elimina una mesa del local (solo si está LIBRE).
     *
     * DELETE /api/mesas/{mesaId}
     *
     * TODO: Implementar autenticación/autorización para obtener el localId del usuario logueado.
     *       Por ahora se usa un localId hardcodeado para permitir testing del endpoint.
     * TODO: Implementar manejo de excepciones con @ControllerAdvice para capturar:
     *       - MesaNoEncontradaException -> 404 Not Found
     *       - AccesoDenegadoException -> 403 Forbidden
     *       - MesaNoEliminableException -> 409 Conflict
     *       - UltimaMesaException -> 409 Conflict
     *
     * @param mesaId ID de la mesa a eliminar
     * @return 204 No Content si la eliminación fue exitosa
     */
    @DeleteMapping("/{mesaId}")
    public ResponseEntity<Void> eliminarMesa(@PathVariable String mesaId) {
        // TODO: Reemplazar por extracción del localId desde el contexto de seguridad
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        MesaId id = MesaId.from(mesaId);
        eliminarMesaUseCase.ejecutar(localIdSimulado, id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Consulta el detalle del pedido activo de una mesa específica.
     *
     * GET /api/mesas/{mesaId}/pedido-actual
     *
     * HU-06: Ver pedido de una mesa
     *
     * TODO: Implementar autenticación/autorización para obtener el localId del usuario logueado.
     *       Por ahora se usa un localId hardcodeado para permitir testing del endpoint.
     * TODO: Implementar manejo de excepciones con @ControllerAdvice para capturar:
     *       - IllegalStateException (mesa no existe o está libre) -> 404 Not Found
     *       - IllegalArgumentException (mesa de otro local) -> 403 Forbidden
     *
     * @param mesaId ID de la mesa cuyo pedido se desea consultar
     * @return detalle completo del pedido activo (ítems, totales, contexto)
     */
    @GetMapping("/{mesaId}/pedido-actual")
    public ResponseEntity<DetallePedidoResponse> consultarPedidoActual(@PathVariable String mesaId) {
        // TODO: Reemplazar por extracción del localId desde el contexto de seguridad
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        MesaId id = MesaId.from(mesaId);
        DetallePedidoResponse detalle = consultarDetallePedidoUseCase.ejecutar(localIdSimulado, id);

        return ResponseEntity.ok(detalle);
    }

    /**
     * Cierra una mesa y finaliza su pedido activo.
     *
     * POST /api/mesas/{mesaId}/cierre
     *
     * Evento de negocio crítico que consolida:
     * - Cierre del pedido (inmutabilidad financiera)
     * - Registro de pagos (soporte pagos parciales)
     * - Liberación de la mesa (recurso físico)
     *
     * Request body:
     * {
     *   "pagos": [
     *     {"medio": "EFECTIVO", "monto": 5000},
     *     {"medio": "TARJETA", "monto": 2500}
     *   ]
     * }
     *
     * TODO: Implementar autenticación/autorización para obtener el localId del usuario logueado.
     *
     * @param mesaId ID de la mesa a cerrar
     * @param request DTO con la lista de pagos
     * @return información de la mesa liberada y el pedido cerrado con snapshot contable
     */
    @PostMapping("/{mesaId}/cierre")
    public ResponseEntity<CerrarMesaResponse> cerrarMesa(
            @PathVariable String mesaId,
            @RequestBody CerrarMesaRequest request
    ) {
        // TODO: Reemplazar por extracción del localId desde el contexto de seguridad
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        MesaId id = MesaId.from(mesaId);
        CerrarMesaResponse response = cerrarMesaUseCase.ejecutar(localIdSimulado, id, request.pagos());

        return ResponseEntity.ok(response);
    }
}
