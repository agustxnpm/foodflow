package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.AbrirMesaRequest;
import com.agustinpalma.comandas.application.dto.AbrirMesaResponse;
import com.agustinpalma.comandas.application.dto.CerrarMesaRequest;
import com.agustinpalma.comandas.application.dto.CerrarMesaResponse;
import com.agustinpalma.comandas.application.dto.ComandaImpresionResponse;
import com.agustinpalma.comandas.application.dto.CrearMesaRequest;
import com.agustinpalma.comandas.application.dto.DetallePedidoResponse;
import com.agustinpalma.comandas.application.dto.MesaResponse;
import com.agustinpalma.comandas.application.dto.TicketImpresionResponse;
import com.agustinpalma.comandas.application.usecase.AbrirMesaUseCase;
import com.agustinpalma.comandas.application.usecase.CerrarMesaUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarDetallePedidoUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarMesasUseCase;
import com.agustinpalma.comandas.application.usecase.CrearMesaUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarMesaUseCase;
import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.infrastructure.mapper.TicketImpresionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para operaciones sobre mesas.
 * Expone endpoints HTTP y transforma requests/responses.
 * No contiene lógica de negocio, solo coordina entre HTTP y casos de uso.
 */
@RestController
@RequestMapping("/api/mesas")
public class MesaController {

    private final LocalContextProvider localContextProvider;
    private final ConsultarMesasUseCase consultarMesasUseCase;
    private final AbrirMesaUseCase abrirMesaUseCase;
    private final CerrarMesaUseCase cerrarMesaUseCase;
    private final CrearMesaUseCase crearMesaUseCase;
    private final EliminarMesaUseCase eliminarMesaUseCase;
    private final ConsultarDetallePedidoUseCase consultarDetallePedidoUseCase;
    private final TicketImpresionMapper ticketImpresionMapper;

    public MesaController(
        LocalContextProvider localContextProvider,
        ConsultarMesasUseCase consultarMesasUseCase,
        AbrirMesaUseCase abrirMesaUseCase,
        CerrarMesaUseCase cerrarMesaUseCase,
        CrearMesaUseCase crearMesaUseCase,
        EliminarMesaUseCase eliminarMesaUseCase,
        ConsultarDetallePedidoUseCase consultarDetallePedidoUseCase,
        TicketImpresionMapper ticketImpresionMapper
    ) {
        this.localContextProvider = localContextProvider;
        this.consultarMesasUseCase = consultarMesasUseCase;
        this.abrirMesaUseCase = abrirMesaUseCase;
        this.cerrarMesaUseCase = cerrarMesaUseCase;
        this.crearMesaUseCase = crearMesaUseCase;
        this.eliminarMesaUseCase = eliminarMesaUseCase;
        this.consultarDetallePedidoUseCase = consultarDetallePedidoUseCase;
        this.ticketImpresionMapper = ticketImpresionMapper;
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
        LocalId localId = localContextProvider.getCurrentLocalId();

        List<MesaResponse> mesas = consultarMesasUseCase.ejecutar(localId);

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
        LocalId localId = localContextProvider.getCurrentLocalId();

        AbrirMesaRequest request = new AbrirMesaRequest(mesaId);
        AbrirMesaResponse response = abrirMesaUseCase.ejecutar(localId, request);

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
        LocalId localId = localContextProvider.getCurrentLocalId();

        MesaResponse response = crearMesaUseCase.ejecutar(localId, request.numero());

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
        LocalId localId = localContextProvider.getCurrentLocalId();

        MesaId id = MesaId.from(mesaId);
        eliminarMesaUseCase.ejecutar(localId, id);

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
        LocalId localId = localContextProvider.getCurrentLocalId();

        MesaId id = MesaId.from(mesaId);
        DetallePedidoResponse detalle = consultarDetallePedidoUseCase.ejecutar(localId, id);

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
        LocalId localId = localContextProvider.getCurrentLocalId();

        MesaId id = MesaId.from(mesaId);
        CerrarMesaResponse response = cerrarMesaUseCase.ejecutar(localId, id, request.pagos());

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene los datos estructurados para imprimir el ticket de venta (cliente).
     *
     * GET /api/mesas/{mesaId}/ticket
     *
     * HU-29: Ticket de venta para impresora térmica.
     * Endpoint de solo lectura — no cierra mesa, no modifica estado, no crea snapshots.
     *
     * Flujo:
     * 1. Obtener tenant vía LocalContextProvider
     * 2. Delegar a ConsultarDetallePedidoUseCase (datos ya calculados por dominio)
     * 3. Transformar vía TicketImpresionMapper (datos del local desde MeisenProperties)
     * 4. Retornar DTO de impresión
     *
     * @param mesaId ID de la mesa cuyo ticket se desea obtener
     * @return datos estructurados del ticket para renderizado en frontend
     */
    @GetMapping("/{mesaId}/ticket")
    public ResponseEntity<TicketImpresionResponse> obtenerTicket(@PathVariable String mesaId) {
        LocalId localId = localContextProvider.getCurrentLocalId();
        MesaId id = MesaId.from(mesaId);

        DetallePedidoResponse detalle = consultarDetallePedidoUseCase.ejecutar(localId, id);
        TicketImpresionResponse ticket = ticketImpresionMapper.toTicket(detalle);

        return ResponseEntity.ok(ticket);
    }

    /**
     * Obtiene los datos estructurados para imprimir la comanda operativa (cocina/barra).
     *
     * GET /api/mesas/{mesaId}/comanda
     *
     * HU-05: Comanda operativa para cocina.
     * Endpoint de solo lectura — no modifica pedido ni estado de mesa.
     * No incluye valores monetarios.
     *
     * Flujo:
     * 1. Obtener tenant vía LocalContextProvider
     * 2. Delegar a ConsultarDetallePedidoUseCase
     * 3. Transformar vía TicketImpresionMapper (solo datos operativos, sin precios)
     * 4. Retornar DTO de comanda
     *
     * @param mesaId ID de la mesa cuya comanda se desea obtener
     * @return datos estructurados de la comanda para renderizado en frontend
     */
    @GetMapping("/{mesaId}/comanda")
    public ResponseEntity<ComandaImpresionResponse> obtenerComanda(@PathVariable String mesaId) {
        LocalId localId = localContextProvider.getCurrentLocalId();
        MesaId id = MesaId.from(mesaId);

        DetallePedidoResponse detalle = consultarDetallePedidoUseCase.ejecutar(localId, id);
        ComandaImpresionResponse comanda = ticketImpresionMapper.toComanda(detalle);

        return ResponseEntity.ok(comanda);
    }
}
