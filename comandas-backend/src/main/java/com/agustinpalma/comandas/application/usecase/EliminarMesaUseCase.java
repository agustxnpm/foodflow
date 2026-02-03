package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import java.util.Objects;

/**
 * Caso de uso para eliminar una mesa del local.
 * Implementa las validaciones de negocio necesarias:
 * - Solo se puede eliminar mesas en estado LIBRE
 * - No se puede eliminar la última mesa del local
 * - La mesa debe pertenecer al local del usuario autenticado
 *  */
public class EliminarMesaUseCase {

    private final MesaRepository mesaRepository;

    /**
     * Constructor con inyección de dependencias.
     * El framework de infraestructura (Spring) se encargará de proveer la implementación.
     *
     * @param mesaRepository repositorio de mesas
     */
    public EliminarMesaUseCase(MesaRepository mesaRepository) {
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "El mesaRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: eliminar físicamente una mesa del sistema.
     * 
     * Validaciones aplicadas:
     * 1. La mesa debe existir
     * 2. La mesa debe pertenecer al local del usuario (validación multi-tenant)
     * 3. La mesa debe estar en estado LIBRE (no puede tener pedido abierto)
     * 4. No se puede eliminar la última mesa del local (debe haber al menos 2)
     * 
     * @param localId identificador del local (tenant) del usuario autenticado
     * @param mesaId identificador de la mesa a eliminar
     * @throws IllegalArgumentException si la mesa no existe (404 Not Found)
     * @throws IllegalArgumentException si la mesa no pertenece al local (403 Forbidden)
     * @throws IllegalArgumentException si la mesa está ABIERTA (409 Conflict)
     * @throws IllegalArgumentException si es la última mesa del local (409 Conflict)
     */
    public void ejecutar(LocalId localId, MesaId mesaId) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(mesaId, "El mesaId es obligatorio");

        // 1. Validar que la mesa existe
        Mesa mesa = mesaRepository.buscarPorId(mesaId)
            .orElseThrow(() -> {
                // TODO: Reemplazar por excepción de dominio personalizada (MesaNoEncontradaException)
                return new IllegalArgumentException("La mesa no existe");
            });

        // 2. Validación multi-tenant: la mesa debe pertenecer al local del usuario
        if (!mesa.getLocalId().equals(localId)) {
            // TODO: Reemplazar por excepción de dominio personalizada (AccesoDenegadoException)
            throw new IllegalArgumentException(
                "No tiene permisos para eliminar esta mesa (pertenece a otro local)"
            );
        }

        // 3. Validar que la mesa está LIBRE (no tiene pedido abierto)
        if (mesa.getEstado() == EstadoMesa.ABIERTA) {
            // TODO: Reemplazar por excepción de dominio personalizada (MesaNoEliminableException)
            throw new IllegalArgumentException(
                "No se puede eliminar la mesa " + mesa.getNumero() + " porque tiene un pedido abierto"
            );
        }

        // 4. Regla crítica: No permitir eliminar la última mesa del local
        int cantidadMesas = mesaRepository.contarPorLocal(localId);
        if (cantidadMesas <= 1) {
            // TODO: Reemplazar por excepción de dominio personalizada (UltimaMesaException)
            throw new IllegalArgumentException(
                "No se puede eliminar la última mesa. El local debe tener al menos una mesa disponible"
            );
        }

        // Si todas las validaciones pasaron, eliminar físicamente
        mesaRepository.eliminar(mesaId);
    }
}
