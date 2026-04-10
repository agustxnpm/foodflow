package com.agustinpalma.comandas.presentation.dto;

/**
 * DTO de respuesta para el endpoint GET /api/config/identity.
 *
 * Dos estados posibles:
 * - configured=false → el sistema no fue activado (primer arranque)
 * - configured=true  → incluye localId y datos del local
 */
public record IdentityResponseDTO(
    boolean configured,
    String localId,
    String nombreLocal,
    String direccion,
    String telefono,
    String cuit,
    String mensajeBienvenida
) {
    /**
     * Fábrica para el estado "no configurado".
     */
    public static IdentityResponseDTO noConfigurado() {
        return new IdentityResponseDTO(false, null, null, null, null, null, null);
    }

    /**
     * Fábrica para el estado "configurado" con todos los datos del local.
     */
    public static IdentityResponseDTO configurado(
            String localId,
            String nombreLocal,
            String direccion,
            String telefono,
            String cuit,
            String mensajeBienvenida
    ) {
        return new IdentityResponseDTO(true, localId, nombreLocal, direccion, telefono, cuit, mensajeBienvenida);
    }
}
