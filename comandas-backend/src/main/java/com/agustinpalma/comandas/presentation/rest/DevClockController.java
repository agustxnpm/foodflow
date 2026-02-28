package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.domain.model.JornadaCaja;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controller de desarrollo para manipular el reloj del sistema.
 * 
 * Permite "viajar en el tiempo" durante pruebas de usabilidad sin
 * modificar el reloj del sistema operativo.
 * 
 * ⚠️ SOLO DISPONIBLE con profile "dev". No se registra en producción.
 * 
 * Endpoints:
 * 
 *   GET  /api/dev/clock              → Consultar hora actual del sistema
 *   POST /api/dev/clock              → Fijar hora específica
 *   POST /api/dev/clock/advance      → Avanzar N horas/minutos
 *   DELETE /api/dev/clock             → Volver a tiempo real
 * 
 * Ejemplo de flujo de testing:
 * 
 *   1. POST /api/dev/clock { "dateTime": "2026-02-28T22:00:00" }
 *      → Sistema cree que son las 22:00 del 28/02
 *   
 *   2. Operar normalmente (crear pedidos, cobrar, cerrar jornada)
 *   
 *   3. POST /api/dev/clock/advance { "hours": 10 }
 *      → Ahora son las 08:00 del 01/03 (día siguiente)
 *   
 *   4. Operar de nuevo → verificar que es un nuevo día operativo
 *   
 *   5. DELETE /api/dev/clock
 *      → Vuelve a la hora real
 */
@RestController
@RequestMapping("/api/dev/clock")
@Profile("dev")
public class DevClockController {

    private static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AtomicReference<Instant> devClockOverride;
    private final Clock clock;

    public DevClockController(AtomicReference<Instant> devClockOverride, Clock clock) {
        this.devClockOverride = devClockOverride;
        this.clock = clock;
    }

    /**
     * Consultar la hora actual que ve el sistema.
     * 
     * Retorna tanto la hora del override (si existe) como la hora real,
     * para que el operador tenga contexto claro.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> obtenerHoraActual() {
        Instant override = devClockOverride.get();
        LocalDateTime horaActual = LocalDateTime.now(clock);
        LocalDateTime horaReal = LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires"));

        return ResponseEntity.ok(Map.of(
            "horaDelSistema", horaActual.format(FORMATTER),
            "horaReal", horaReal.format(FORMATTER),
            "overrideActivo", override != null,
            "fechaOperativa", JornadaCaja.calcularFechaOperativa(horaActual).toString()
        ));
    }

    /**
     * Fijar el reloj en una hora específica.
     * 
     * Body: { "dateTime": "2026-02-28T23:30:00" }
     * 
     * El formato es ISO local (sin zona) — se interpreta como Argentina.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> fijarHora(@RequestBody Map<String, String> body) {
        String dateTimeStr = body.get("dateTime");
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Se requiere 'dateTime' en formato ISO"));
        }

        LocalDateTime target = LocalDateTime.parse(dateTimeStr);
        Instant instant = target.atZone(ARGENTINA_ZONE).toInstant();
        devClockOverride.set(instant);

        return ResponseEntity.ok(Map.of(
            "mensaje", "Reloj fijado",
            "horaDelSistema", target.format(FORMATTER),
            "overrideActivo", true
        ));
    }

    /**
     * Avanzar el reloj N horas y/o minutos desde la hora actual del sistema.
     * 
     * Body: { "hours": 8, "minutes": 30 }
     * 
     * Si no hay override activo, avanza desde la hora real.
     * Si hay override, avanza desde la hora fijada.
     */
    @PostMapping("/advance")
    public ResponseEntity<Map<String, Object>> avanzarTiempo(@RequestBody Map<String, Integer> body) {
        int hours = body.getOrDefault("hours", 0);
        int minutes = body.getOrDefault("minutes", 0);

        if (hours == 0 && minutes == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Se requiere 'hours' y/o 'minutes'"));
        }

        // Partir de la hora actual del sistema (override o real)
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime nueva = ahora.plusHours(hours).plusMinutes(minutes);
        Instant instant = nueva.atZone(ARGENTINA_ZONE).toInstant();
        devClockOverride.set(instant);

        return ResponseEntity.ok(Map.of(
            "mensaje", String.format("Reloj avanzado +%dh %dmin", hours, minutes),
            "horaAnterior", ahora.format(FORMATTER),
            "horaNueva", nueva.format(FORMATTER),
            "fechaOperativa", JornadaCaja.calcularFechaOperativa(nueva).toString(),
            "overrideActivo", true
        ));
    }

    /**
     * Volver al tiempo real (desactivar override).
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> resetearReloj() {
        devClockOverride.set(null);
        LocalDateTime horaReal = LocalDateTime.now(ARGENTINA_ZONE);

        return ResponseEntity.ok(Map.of(
            "mensaje", "Reloj reseteado a tiempo real",
            "horaDelSistema", horaReal.format(FORMATTER),
            "overrideActivo", false
        ));
    }
}
