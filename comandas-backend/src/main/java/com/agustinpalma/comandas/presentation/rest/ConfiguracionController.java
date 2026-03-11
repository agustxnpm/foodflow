package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.infrastructure.adapter.ImpresoraConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para configuración de periféricos del local.
 *
 * Endpoints:
 * - GET  /api/config/impresoras               -> Listar impresoras del SO
 * - GET  /api/config/impresora-predeterminada  -> Obtener la impresora guardada
 * - POST /api/config/impresora-predeterminada  -> Guardar impresora seleccionada
 */
@RestController
@RequestMapping("/api/config")
public class ConfiguracionController {

    private final ImpresoraConfigService impresoraConfigService;
    private final LocalContextProvider localContextProvider;

    public ConfiguracionController(
            ImpresoraConfigService impresoraConfigService,
            LocalContextProvider localContextProvider
    ) {
        this.impresoraConfigService = impresoraConfigService;
        this.localContextProvider = localContextProvider;
    }

    /**
     * Lista todas las impresoras detectadas en el sistema operativo.
     * Usa javax.print.PrintServiceLookup para consultar el spooler del SO.
     *
     * GET /api/config/impresoras
     *
     * @return lista de nombres de impresoras (puede estar vacía)
     */
    @GetMapping("/impresoras")
    public ResponseEntity<List<String>> listarImpresoras() {
        List<String> impresoras = impresoraConfigService.listarImpresoras();
        return ResponseEntity.ok(impresoras);
    }

    /**
     * Obtiene el nombre de la impresora predeterminada del local actual.
     *
     * GET /api/config/impresora-predeterminada
     *
     * @return nombre de la impresora o null si no está configurada
     */
    @GetMapping("/impresora-predeterminada")
    public ResponseEntity<Map<String, String>> obtenerImpresoraPredeterminada() {
        var localId = localContextProvider.getCurrentLocalId();
        String nombre = impresoraConfigService.obtenerImpresoraPredeterminada(localId.getValue());
        return ResponseEntity.ok(Map.of("impresora", nombre != null ? nombre : ""));
    }

    /**
     * Guarda la impresora seleccionada como predeterminada para el local actual.
     *
     * POST /api/config/impresora-predeterminada
     * Body: { "nombre": "EPSON TM-T20III" }
     *
     * @param body mapa con clave "nombre" conteniendo el nombre de la impresora
     * @return 200 OK con mensaje de confirmación
     */
    @PostMapping("/impresora-predeterminada")
    public ResponseEntity<Map<String, String>> guardarImpresoraPredeterminada(
            @RequestBody Map<String, String> body
    ) {
        String nombre = body.get("nombre");
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la impresora es obligatorio");
        }

        var localId = localContextProvider.getCurrentLocalId();
        impresoraConfigService.guardarImpresoraPredeterminada(localId.getValue(), nombre.trim());

        return ResponseEntity.ok(Map.of("message", "Impresora predeterminada guardada: " + nombre.trim()));
    }
}
