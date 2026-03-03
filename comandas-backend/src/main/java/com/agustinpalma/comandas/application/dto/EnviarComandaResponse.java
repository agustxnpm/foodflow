package com.agustinpalma.comandas.application.dto;

import java.time.LocalDateTime;

/**
 * HU-29: Respuesta al enviar una comanda a cocina.
 *
 * Contiene:
 * - El buffer ESC/POS codificado en Base64, listo para enviar a la impresora
 * - El timestamp del envío (para que el frontend sepa cuándo fue el último envío)
 * - Si había ítems nuevos para imprimir
 *
 * El frontend recibe el Base64, lo decodifica y lo envía vía Tauri al dispositivo USB/LAN.
 * En modo mock (sin Tauri), puede mostrarlo como descarga o log de consola.
 */
public record EnviarComandaResponse(
    String escPosBase64,
    LocalDateTime timestampEnvio,
    int cantidadItemsNuevos,
    int cantidadItemsTotal
) {
    public EnviarComandaResponse {
        if (escPosBase64 == null || escPosBase64.isBlank()) {
            throw new IllegalArgumentException("El buffer ESC/POS no puede ser nulo o vacío");
        }
        if (timestampEnvio == null) {
            throw new IllegalArgumentException("El timestamp de envío no puede ser nulo");
        }
        if (cantidadItemsNuevos < 0) {
            throw new IllegalArgumentException("La cantidad de ítems nuevos no puede ser negativa");
        }
        if (cantidadItemsTotal <= 0) {
            throw new IllegalArgumentException("La cantidad total de ítems debe ser mayor a cero");
        }
    }
}
