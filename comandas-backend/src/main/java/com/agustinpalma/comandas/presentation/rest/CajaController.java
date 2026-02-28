package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.CorreccionPedidoRequest;
import com.agustinpalma.comandas.application.dto.DetallePedidoCerradoResponse;
import com.agustinpalma.comandas.application.dto.EgresoRequestBody;
import com.agustinpalma.comandas.application.dto.EgresoResponse;
import com.agustinpalma.comandas.application.dto.JornadaResumenResponse;
import com.agustinpalma.comandas.application.dto.ReporteCajaResponse;
import com.agustinpalma.comandas.application.usecase.CerrarJornadaUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarHistorialJornadasUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarPedidoCerradoUseCase;
import com.agustinpalma.comandas.application.usecase.CorregirPedidoCerradoUseCase;
import com.agustinpalma.comandas.application.usecase.GenerarReporteCajaUseCase;
import com.agustinpalma.comandas.application.usecase.RegistrarEgresoUseCase;
import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para operaciones de caja.
 * Expone endpoints para registrar egresos y generar reportes diarios.
 * No contiene lógica de negocio, solo coordina entre HTTP y casos de uso.
 */
@RestController
@RequestMapping("/api/caja")
public class CajaController {

    private final LocalContextProvider localContextProvider;
    private final RegistrarEgresoUseCase registrarEgresoUseCase;
    private final GenerarReporteCajaUseCase generarReporteCajaUseCase;
    private final CerrarJornadaUseCase cerrarJornadaUseCase;
    private final ConsultarPedidoCerradoUseCase consultarPedidoCerradoUseCase;
    private final CorregirPedidoCerradoUseCase corregirPedidoCerradoUseCase;
    private final ConsultarHistorialJornadasUseCase consultarHistorialJornadasUseCase;

    public CajaController(
            LocalContextProvider localContextProvider,
            RegistrarEgresoUseCase registrarEgresoUseCase,
            GenerarReporteCajaUseCase generarReporteCajaUseCase,
            CerrarJornadaUseCase cerrarJornadaUseCase,
            ConsultarPedidoCerradoUseCase consultarPedidoCerradoUseCase,
            CorregirPedidoCerradoUseCase corregirPedidoCerradoUseCase,
            ConsultarHistorialJornadasUseCase consultarHistorialJornadasUseCase
    ) {
        this.localContextProvider = localContextProvider;
        this.registrarEgresoUseCase = registrarEgresoUseCase;
        this.generarReporteCajaUseCase = generarReporteCajaUseCase;
        this.cerrarJornadaUseCase = cerrarJornadaUseCase;
        this.consultarPedidoCerradoUseCase = consultarPedidoCerradoUseCase;
        this.corregirPedidoCerradoUseCase = corregirPedidoCerradoUseCase;
        this.consultarHistorialJornadasUseCase = consultarHistorialJornadasUseCase;
    }

    /**
     * Registra un egreso de caja.
     * 
     * POST /api/caja/egresos
     * 
     * @param body JSON con monto y descripción del egreso
     * @return 201 CREATED con los datos del movimiento registrado
     */
    @PostMapping("/egresos")
    public ResponseEntity<EgresoResponse> registrarEgreso(@RequestBody EgresoRequestBody body) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        EgresoResponse response = registrarEgresoUseCase.ejecutar(localId, body.monto(), body.descripcion());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Genera el reporte de caja diario (arqueo).
     * 
     * GET /api/caja/reporte?fecha=YYYY-MM-DD
     * 
     * @param fecha fecha del reporte en formato ISO
     * @return 200 OK con el reporte completo de caja
     */
    @GetMapping("/reporte")
    public ResponseEntity<ReporteCajaResponse> obtenerReporteDiario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        ReporteCajaResponse response = generarReporteCajaUseCase.ejecutar(localId, fecha);
        return ResponseEntity.ok(response);
    }

    /**
     * Cierre de jornada diaria (arqueo final).
     * 
     * POST /api/caja/cierre-jornada
     * 
     * Valida que no existan mesas abiertas, genera un snapshot contable
     * del día operativo y lo persiste como registro de auditoría.
     * 
     * La fecha operativa se calcula automáticamente:
     * - Cierre entre 00:00 y 05:59 → jornada del día anterior (turno noche)
     * - Cierre a partir de 06:00 → jornada del día actual
     * 
     * @return 200 OK si el cierre fue exitoso
     * @throws MesasAbiertasException → 400 Bad Request si hay mesas abiertas
     * @throws JornadaYaCerradaException → 409 Conflict si la jornada ya fue cerrada
     */
    @PostMapping("/cierre-jornada")
    public ResponseEntity<Void> cerrarJornada() {
        LocalId localId = localContextProvider.getCurrentLocalId();

        cerrarJornadaUseCase.ejecutar(localId);
        return ResponseEntity.ok().build();
    }

    /**
     * Obtiene el detalle completo de un pedido cerrado (ítems + pagos).
     * 
     * GET /api/caja/pedidos/{pedidoId}
     * 
     * Usado por el modal de corrección para mostrar los datos editables.
     * 
     * @param pedidoId UUID del pedido a consultar
     * @return 200 OK con el detalle completo del pedido cerrado
     */
    @GetMapping("/pedidos/{pedidoId}")
    public ResponseEntity<DetallePedidoCerradoResponse> consultarPedidoCerrado(
            @PathVariable UUID pedidoId
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        DetallePedidoCerradoResponse response = consultarPedidoCerradoUseCase.ejecutar(
            localId, new PedidoId(pedidoId)
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Corrige un pedido cerrado sin reabrir la mesa.
     * 
     * PUT /api/caja/pedidos/{pedidoId}/correccion
     * 
     * Permite ajustar cantidades de ítems y reemplazar pagos directamente
     * sobre el pedido cerrado. La mesa NO se modifica.
     * 
     * @param pedidoId UUID del pedido a corregir
     * @param request correcciones de ítems y pagos
     * @return 200 OK con el detalle actualizado del pedido
     */
    @PutMapping("/pedidos/{pedidoId}/correccion")
    public ResponseEntity<DetallePedidoCerradoResponse> corregirPedido(
            @PathVariable UUID pedidoId,
            @RequestBody CorreccionPedidoRequest request
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        DetallePedidoCerradoResponse response = corregirPedidoCerradoUseCase.ejecutar(
            localId, new PedidoId(pedidoId), request
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta el historial de jornadas cerradas por rango de fechas operativas.
     * 
     * GET /api/caja/jornadas?desde=YYYY-MM-DD&hasta=YYYY-MM-DD
     * 
     * Usado por la pantalla de consulta histórica (lista expandible + gráfico).
     * Retorna las jornadas ordenadas por fecha operativa descendente.
     * 
     * @param desde fecha operativa inicial (inclusive)
     * @param hasta fecha operativa final (inclusive)
     * @return 200 OK con lista de resúmenes de jornada
     */
    @GetMapping("/jornadas")
    public ResponseEntity<List<JornadaResumenResponse>> consultarHistorialJornadas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();
        List<JornadaResumenResponse> jornadas = consultarHistorialJornadasUseCase.ejecutar(localId, desde, hasta);
        return ResponseEntity.ok(jornadas);
    }
}
