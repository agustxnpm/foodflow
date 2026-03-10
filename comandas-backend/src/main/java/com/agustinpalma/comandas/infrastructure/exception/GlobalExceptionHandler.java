package com.agustinpalma.comandas.infrastructure.exception;

import com.agustinpalma.comandas.domain.exception.JornadaNoEncontradaException;
import com.agustinpalma.comandas.domain.exception.JornadaYaAbiertaException;
import com.agustinpalma.comandas.domain.exception.JornadaYaCerradaException;
import com.agustinpalma.comandas.domain.exception.MesasAbiertasException;
import com.agustinpalma.comandas.domain.exception.TrialExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler para capturar y exponer errores del backend.
 * 
 * Propósito: "Caja negra" de debugging que devuelve trazas completas
 * en el body del JSON para facilitar el debugging con CURL.
 * 
 * TODO: En producción, reemplazar por excepciones específicas
 * y mensajes user-friendly sin exponer stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Captura errores de validación de Bean Validation (@Valid, @Validated).
     * HTTP 400 Bad Request.
     * 
     * Ejemplos: @DecimalMax, @NotNull, @Size, etc.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        // Loguear TODOS los campos que fallaron validación
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    Map<String, String> detail = new LinkedHashMap<>();
                    detail.put("field", fe.getField());
                    detail.put("rejectedValue", String.valueOf(fe.getRejectedValue()));
                    detail.put("message", fe.getDefaultMessage());
                    return detail;
                })
                .collect(Collectors.toList());

        System.out.println("══════ VALIDATION ERROR ══════");
        fieldErrors.forEach(fe ->
                System.out.println("  Campo: " + fe.get("field")
                        + " | Valor rechazado: " + fe.get("rejectedValue")
                        + " | Motivo: " + fe.get("message"))
        );
        System.out.println("══════════════════════════════");

        logger.warn("Validación de entrada fallida — campos: {}", fieldErrors);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", fieldErrors.stream()
                .map(fe -> fe.get("field") + ": " + fe.get("message"))
                .collect(Collectors.joining("; ")));
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Captura MesasAbiertasException (intento de cerrar jornada con mesas abiertas).
     * HTTP 400 Bad Request.
     * 
     * El body es compatible con el frontend: { mensaje, mesasAbiertas }.
     * El hook useCerrarJornada transforma este 400 en MesasAbiertasError.
     */
    @ExceptionHandler(MesasAbiertasException.class)
    public ResponseEntity<Map<String, Object>> handleMesasAbiertas(MesasAbiertasException ex) {
        logger.warn("Intento de cerrar jornada con mesas abiertas: {}", ex.getMesasAbiertas());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mensaje", ex.getMessage());
        body.put("mesasAbiertas", ex.getMesasAbiertas());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Captura JornadaYaAbiertaException (intento de abrir caja con jornada ya abierta).
     * HTTP 409 Conflict.
     */
    @ExceptionHandler(JornadaYaAbiertaException.class)
    public ResponseEntity<Map<String, Object>> handleJornadaYaAbierta(JornadaYaAbiertaException ex) {
        logger.warn("Intento de abrir caja con jornada ya abierta");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Captura JornadaYaCerradaException (intento de cerrar una jornada ya cerrada).
     * HTTP 409 Conflict.
     */
    @ExceptionHandler(JornadaYaCerradaException.class)
    public ResponseEntity<Map<String, Object>> handleJornadaYaCerrada(JornadaYaCerradaException ex) {
        logger.warn("Intento de doble cierre de jornada: {}", ex.getFechaOperativa());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Captura JornadaNoEncontradaException (jornada no existe para el ID solicitado).
     * HTTP 404 Not Found.
     */
    @ExceptionHandler(JornadaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleJornadaNoEncontrada(JornadaNoEncontradaException ex) {
        logger.warn("Jornada no encontrada: {}", ex.getJornadaId().getValue());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Captura TrialExpiredException (período de prueba expirado).
     * HTTP 402 Payment Required.
     *
     * Indica al frontend que debe mostrar la pantalla de bloqueo de licencia.
     * Los datos del local permanecen intactos.
     */
    @ExceptionHandler(TrialExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleTrialExpired(TrialExpiredException ex) {
        logger.warn("Intento de operación con período de prueba expirado");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 402);
        body.put("error", "Payment Required");
        body.put("message", ex.getMessage());
        body.put("trialExpired", true);

        return ResponseEntity.status(402).body(body);
    }

    /**
     * Captura IllegalArgumentException (ej: pedido/item no encontrado, validaciones de negocio).
     * HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        System.out.println("══════ IllegalArgumentException ══════");
        ex.printStackTrace(System.out);
        System.out.println("══════════════════════════════════════");
        logger.error("IllegalArgumentException capturada", ex);
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("cause", ex.getCause() != null ? ex.getCause().toString() : null);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Captura IllegalStateException (ej: pedido cerrado, operación no permitida).
     * HTTP 409 Conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        System.out.println("══════ IllegalStateException ══════");
        ex.printStackTrace(System.out);
        System.out.println("═══════════════════════════════════");
        logger.error("IllegalStateException capturada", ex);
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        body.put("cause", ex.getCause() != null ? ex.getCause().toString() : null);
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }


    /**
     * Captura TODAS las demás excepciones no manejadas.
     * HTTP 500 Internal Server Error.
     * 
     * CRÍTICO: Imprime stack trace completo a System.out (capturado por Tauri)
     * y devuelve la traza en el body JSON para debugging desde el frontend.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Imprimir a stdout para que Tauri lo capture en sus logs de archivo
        System.out.println("══════ UNHANDLED EXCEPTION ══════");
        System.out.println("Type: " + ex.getClass().getName());
        System.out.println("Message: " + ex.getMessage());
        ex.printStackTrace(System.out);
        System.out.println("═════════════════════════════════");

        logger.error("Excepción no controlada capturada", ex);

        // Stack trace completo como String para el body JSON
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String fullStackTrace = sw.toString();
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());
        body.put("exceptionType", ex.getClass().getName());
        body.put("cause", ex.getCause() != null ? ex.getCause().toString() : null);
        body.put("stackTrace", fullStackTrace);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
