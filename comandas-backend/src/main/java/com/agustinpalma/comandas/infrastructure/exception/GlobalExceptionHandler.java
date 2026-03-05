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


import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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
        logger.warn("Validación de entrada fallida: {}", ex.getMessage());
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        
        // Extraer el primer error de validación
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Error de validación");
        
        body.put("message", errorMessage);
        
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
     * CRÍTICO: Devuelve el stack trace completo para debugging.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Excepción no controlada capturada", ex);
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", ex.getMessage());
        body.put("exceptionType", ex.getClass().getName());
        body.put("cause", ex.getCause() != null ? ex.getCause().toString() : null);
        
        // Stack trace para debugging (solo en desarrollo)
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            StringBuilder traceBuilder = new StringBuilder();
            for (int i = 0; i < Math.min(10, stackTrace.length); i++) {
                traceBuilder.append(stackTrace[i].toString()).append("\n");
            }
            body.put("stackTrace", traceBuilder.toString());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
