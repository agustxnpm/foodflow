package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.AbrirCajaRequest;
import com.agustinpalma.comandas.application.dto.AbrirCajaResponse;
import com.agustinpalma.comandas.application.dto.CierreJornadaResponse;
import com.agustinpalma.comandas.application.dto.CorreccionPedidoRequest;
import com.agustinpalma.comandas.application.dto.DetallePedidoCerradoResponse;
import com.agustinpalma.comandas.application.dto.EgresoRequestBody;
import com.agustinpalma.comandas.application.dto.EgresoResponse;
import com.agustinpalma.comandas.application.dto.EstadoCajaResponse;
import com.agustinpalma.comandas.application.dto.IngresoRequestBody;
import com.agustinpalma.comandas.application.dto.IngresoResponse;
import com.agustinpalma.comandas.application.dto.JornadaResumenResponse;
import com.agustinpalma.comandas.application.dto.ProductoVendidoReporte;
import com.agustinpalma.comandas.application.dto.ReporteCajaResponse;
import com.agustinpalma.comandas.application.usecase.AbrirJornadaUseCase;
import com.agustinpalma.comandas.application.usecase.CerrarJornadaUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarHistorialJornadasUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarPedidoCerradoUseCase;
import com.agustinpalma.comandas.application.usecase.CorregirPedidoCerradoUseCase;
import com.agustinpalma.comandas.application.usecase.GenerarReporteCajaUseCase;
import com.agustinpalma.comandas.application.usecase.GenerarReportePdfJornadaUseCase;
import com.agustinpalma.comandas.application.usecase.ObtenerEstadoJornadaUseCase;
import com.agustinpalma.comandas.application.usecase.ObtenerReporteVentasUseCase;
import com.agustinpalma.comandas.application.usecase.RegistrarEgresoUseCase;
import com.agustinpalma.comandas.application.usecase.RegistrarIngresoUseCase;
import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final ObtenerEstadoJornadaUseCase obtenerEstadoJornadaUseCase;
    private final AbrirJornadaUseCase abrirJornadaUseCase;
    private final RegistrarEgresoUseCase registrarEgresoUseCase;
    private final RegistrarIngresoUseCase registrarIngresoUseCase;
    private final GenerarReporteCajaUseCase generarReporteCajaUseCase;
    private final CerrarJornadaUseCase cerrarJornadaUseCase;
    private final GenerarReportePdfJornadaUseCase generarReportePdfJornadaUseCase;
    private final ConsultarPedidoCerradoUseCase consultarPedidoCerradoUseCase;
    private final CorregirPedidoCerradoUseCase corregirPedidoCerradoUseCase;
    private final ConsultarHistorialJornadasUseCase consultarHistorialJornadasUseCase;
    private final ObtenerReporteVentasUseCase obtenerReporteVentasUseCase;

    public CajaController(
            LocalContextProvider localContextProvider,
            ObtenerEstadoJornadaUseCase obtenerEstadoJornadaUseCase,
            AbrirJornadaUseCase abrirJornadaUseCase,
            RegistrarEgresoUseCase registrarEgresoUseCase,
            RegistrarIngresoUseCase registrarIngresoUseCase,
            GenerarReporteCajaUseCase generarReporteCajaUseCase,
            CerrarJornadaUseCase cerrarJornadaUseCase,
            GenerarReportePdfJornadaUseCase generarReportePdfJornadaUseCase,
            ConsultarPedidoCerradoUseCase consultarPedidoCerradoUseCase,
            CorregirPedidoCerradoUseCase corregirPedidoCerradoUseCase,
            ConsultarHistorialJornadasUseCase consultarHistorialJornadasUseCase,
            ObtenerReporteVentasUseCase obtenerReporteVentasUseCase
    ) {
        this.localContextProvider = localContextProvider;
        this.obtenerEstadoJornadaUseCase = obtenerEstadoJornadaUseCase;
        this.abrirJornadaUseCase = abrirJornadaUseCase;
        this.registrarEgresoUseCase = registrarEgresoUseCase;
        this.registrarIngresoUseCase = registrarIngresoUseCase;
        this.generarReporteCajaUseCase = generarReporteCajaUseCase;
        this.cerrarJornadaUseCase = cerrarJornadaUseCase;
        this.generarReportePdfJornadaUseCase = generarReportePdfJornadaUseCase;
        this.consultarPedidoCerradoUseCase = consultarPedidoCerradoUseCase;
        this.corregirPedidoCerradoUseCase = corregirPedidoCerradoUseCase;
        this.consultarHistorialJornadasUseCase = consultarHistorialJornadasUseCase;
        this.obtenerReporteVentasUseCase = obtenerReporteVentasUseCase;
    }

    /**
     * Consulta el estado actual de la caja (ABIERTA / CERRADA).
     *
     * GET /api/caja/estado
     *
     * Si hay una jornada abierta, retorna sus datos.
     * Si no, retorna estado CERRADA con el saldo sugerido de la última jornada histórica.
     *
     * @return 200 OK con el estado de la caja
     */
    @GetMapping("/estado")
    public ResponseEntity<EstadoCajaResponse> obtenerEstadoCaja() {
        LocalId localId = localContextProvider.getCurrentLocalId();

        EstadoCajaResponse response = obtenerEstadoJornadaUseCase.ejecutar(localId);
        return ResponseEntity.ok(response);
    }

    /**
     * Abre una nueva jornada de caja con un fondo inicial.
     *
     * POST /api/caja/abrir
     *
     * Valida que no exista una jornada abierta en curso.
     * Crea una jornada con estado ABIERTA y fondo inicial declarado.
     *
     * @param body JSON con el monto inicial
     * @return 201 CREATED con los datos de la jornada abierta
     * @throws JornadaYaAbiertaException → 409 Conflict si ya hay una jornada abierta
     */
    @PostMapping("/abrir")
    public ResponseEntity<AbrirCajaResponse> abrirCaja(@RequestBody AbrirCajaRequest body) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        AbrirCajaResponse response = abrirJornadaUseCase.ejecutar(localId, body.montoInicial());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
     * Registra un ingreso manual de caja.
     * 
     * POST /api/caja/ingresos
     * 
     * Un ingreso representa efectivo que entra al local sin provenir de un
     * pedido/mesa convencional (ej: cobro de PedidosYa/Rappi en efectivo).
     * 
     * @param body JSON con monto y descripción del ingreso
     * @return 201 CREATED con los datos del movimiento registrado
     */
    @PostMapping("/ingresos")
    public ResponseEntity<IngresoResponse> registrarIngreso(@RequestBody IngresoRequestBody body) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        IngresoResponse response = registrarIngresoUseCase.ejecutar(localId, body.monto(), body.descripcion());
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
     * @return 200 OK con el ID de la jornada cerrada
     * @throws MesasAbiertasException → 400 Bad Request si hay mesas abiertas
     * @throws JornadaYaCerradaException → 409 Conflict si la jornada ya fue cerrada
     */
    @PostMapping("/cierre-jornada")
    public ResponseEntity<CierreJornadaResponse> cerrarJornada() {
        LocalId localId = localContextProvider.getCurrentLocalId();

        JornadaCajaId jornadaId = cerrarJornadaUseCase.ejecutar(localId);
        return ResponseEntity.ok(new CierreJornadaResponse(jornadaId.getValue()));
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

    /**
     * Genera y descarga el reporte PDF de cierre de una jornada específica.
     *
     * GET /api/caja/jornadas/{jornadaId}/reporte-pdf
     *
     * El PDF se genera bajo demanda a partir de la jornada cerrada y los datos
     * detallados del día (pedidos, movimientos, desglose por medio de pago).
     *
     * @param jornadaId UUID de la jornada cerrada
     * @return 200 OK con el PDF como application/pdf
     * @throws JornadaNoEncontradaException → 404 si la jornada no existe
     */
    @GetMapping("/jornadas/{jornadaId}/reporte-pdf")
    public ResponseEntity<byte[]> descargarReportePdf(@PathVariable UUID jornadaId) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        byte[] pdf = generarReportePdfJornadaUseCase.ejecutar(
            new JornadaCajaId(jornadaId), localId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
            String.format("cierre-jornada-%s.pdf", jornadaId));

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    // ─── Analytics ────────────────────────────────────────────────────────────

    /**
     * Reporte de ventas por producto para una fecha operativa.
     *
     * GET /api/caja/reportes/productos?fecha=YYYY-MM-DD
     *
     * Retorna el desglose de productos vendidos agrupando cantidades
     * y sumando totales, basándose únicamente en pedidos CERRADOS.
     *
     * Es una consulta de analytics de solo lectura — no modifica estado.
     *
     * @param fecha fecha operativa del reporte en formato ISO
     * @return 200 OK con la lista de productos vendidos
     */
    @GetMapping("/reportes/productos")
    public ResponseEntity<List<ProductoVendidoReporte>> obtenerReporteVentasProductos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        List<ProductoVendidoReporte> reporte = obtenerReporteVentasUseCase.ejecutar(localId, fecha);
        return ResponseEntity.ok(reporte);
    }
}
