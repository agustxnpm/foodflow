package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.EgresoRequestBody;
import com.agustinpalma.comandas.application.dto.EgresoResponse;
import com.agustinpalma.comandas.application.dto.ReporteCajaResponse;
import com.agustinpalma.comandas.application.usecase.GenerarReporteCajaUseCase;
import com.agustinpalma.comandas.application.usecase.RegistrarEgresoUseCase;
import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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

    public CajaController(
            LocalContextProvider localContextProvider,
            RegistrarEgresoUseCase registrarEgresoUseCase,
            GenerarReporteCajaUseCase generarReporteCajaUseCase
    ) {
        this.localContextProvider = localContextProvider;
        this.registrarEgresoUseCase = registrarEgresoUseCase;
        this.generarReporteCajaUseCase = generarReporteCajaUseCase;
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
}
