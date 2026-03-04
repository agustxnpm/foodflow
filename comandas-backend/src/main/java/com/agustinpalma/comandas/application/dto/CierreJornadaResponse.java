package com.agustinpalma.comandas.application.dto;

import java.util.UUID;

/**
 * Respuesta del cierre de jornada.
 *
 * Devuelve el ID de la jornada recién creada para que el frontend
 * pueda solicitar inmediatamente la descarga del reporte PDF.
 */
public record CierreJornadaResponse(UUID jornadaId) {}
