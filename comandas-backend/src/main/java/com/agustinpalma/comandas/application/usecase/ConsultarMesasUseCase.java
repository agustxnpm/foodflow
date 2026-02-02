package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.MesaResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso para consultar las mesas de un local.
 * Orquesta la lógica de aplicación sin contener reglas de negocio.
 * Devuelve DTOs para evitar exponer entidades de dominio a capas externas.
 */
public class ConsultarMesasUseCase {

    private final MesaRepository mesaRepository;

    /**
     * Constructor con inyección de dependencias.
     * El framework de infraestructura (Spring) se encargará de proveer la implementación.
     *
     * @param mesaRepository repositorio de mesas
     */
    public ConsultarMesasUseCase(MesaRepository mesaRepository) {
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "El mesaRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: obtener todas las mesas de un local.
     *
     * @param localId identificador del local (tenant)
     * @return lista de DTOs con la información de las mesas
     */
  public List<MesaResponse> ejecutar(LocalId localId) {
    Objects.requireNonNull(localId, "El localId es obligatorio");

    return mesaRepository.buscarPorLocal(localId)
        .stream()
        .map(MesaResponse::fromDomain) // Delegamos el mapeo al DTO
        .toList();
}
}
