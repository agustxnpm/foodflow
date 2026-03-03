package com.agustinpalma.comandas.application.dto;

/**
 * HU-29: Respuesta al generar un ticket de venta ESC/POS.
 *
 * Contiene el buffer ESC/POS codificado en Base64, listo para enviar a la impresora.
 * El frontend decodifica el Base64 y lo envía vía Tauri (producción) o lo mockea en consola (dev).
 *
 * Operación de solo lectura — no incluye timestamps de mutación como EnviarComandaResponse.
 */
public record TicketVentaEscPosResponse(
    String escPosBase64
) {
    public TicketVentaEscPosResponse {
        if (escPosBase64 == null || escPosBase64.isBlank()) {
            throw new IllegalArgumentException("El buffer ESC/POS no puede ser nulo o vacío");
        }
    }
}
