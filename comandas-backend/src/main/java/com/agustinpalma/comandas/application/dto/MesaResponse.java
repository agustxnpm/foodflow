package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.Mesa;

public record MesaResponse(
    String id,       // El ID como String es más seguro para JSON/REST
    int numero, 
    String estado    // Usamos String para que el frontend no dependa del Enum de Java
) {
    public static MesaResponse fromDomain(Mesa mesa) {
        return new MesaResponse(
            mesa.getId().getValue().toString(), // Mantenemos la fidelidad del UUID
            mesa.getNumero(),
            mesa.getEstado().name()
        );
    }
}