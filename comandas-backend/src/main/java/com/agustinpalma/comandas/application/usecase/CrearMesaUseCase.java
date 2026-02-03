package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.MesaResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import java.util.Objects;

/**
 * Caso de uso para crear una nueva mesa en el local.
 * Implementa las validaciones de negocio necesarias:
 * - Número único por local
 * - Número positivo mayor a 0
 * - Vinculación inmutable al LocalId
 *  */
public class CrearMesaUseCase {

    private final MesaRepository mesaRepository;

    /**
     * Constructor con inyección de dependencias.
     * El framework de infraestructura (Spring) se encargará de proveer la implementación.
     *
     * @param mesaRepository repositorio de mesas
     */
    public CrearMesaUseCase(MesaRepository mesaRepository) {
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "El mesaRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: crear una mesa nueva con el número especificado.
     * 
     * La mesa se crea en estado LIBRE por defecto (definido en el constructor de Mesa).
     * 
     * @param localId identificador del local (tenant) al que pertenece la mesa
     * @param numero número de la mesa (debe ser único dentro del local y > 0)
     * @return DTO con la información de la mesa creada
     * @throws IllegalArgumentException si el número ya existe en el local (409 Conflict)
     * @throws IllegalArgumentException si el número es inválido (≤ 0) (400 Bad Request)
     */
    public MesaResponse ejecutar(LocalId localId, int numero) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        
        // Validación de negocio: el número debe ser positivo mayor a 0
        // Esta validación también está en el constructor de Mesa, pero la hacemos explícita
        // para lanzar una excepción más clara a nivel de caso de uso
        if (numero <= 0) {
            // TODO: Reemplazar por excepción de dominio personalizada (NumeroMesaInvalidoException)
            throw new IllegalArgumentException("El número de mesa debe ser mayor a 0");
        }

        // Validación de negocio: unicidad del número dentro del local
        if (mesaRepository.existePorNumeroYLocal(numero, localId)) {
            // TODO: Reemplazar por excepción de dominio personalizada (MesaDuplicadaException)
            throw new IllegalArgumentException(
                "Ya existe una mesa con el número " + numero + " en este local"
            );
        }

        // Crear la entidad de dominio
        // El estado LIBRE se asigna automáticamente en el constructor
        MesaId nuevoId = MesaId.generate();
        Mesa nuevaMesa = new Mesa(nuevoId, localId, numero);

        // Persistir
        Mesa mesaGuardada = mesaRepository.guardar(nuevaMesa);

        // Retornar DTO
        return MesaResponse.fromDomain(mesaGuardada);
    }
}
