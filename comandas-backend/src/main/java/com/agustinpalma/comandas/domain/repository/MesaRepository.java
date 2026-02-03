package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import java.util.List;
import java.util.Optional;

/**
 * Contrato del repositorio de mesas.
 * Define las operaciones de persistencia sin acoplarse a tecnologías específicas.
 * La implementación concreta reside en la capa de infraestructura.
 */
public interface MesaRepository {

    /**
     * Busca todas las mesas pertenecientes a un local específico.
     *
     * @param localId identificador del local (tenant)
     * @return lista de mesas del local, ordenadas por número
     */
    List<Mesa> buscarPorLocal(LocalId localId);

    /**
     * Busca una mesa específica por su ID.
     *
     * @param mesaId identificador de la mesa
     * @return Optional con la mesa si existe, vacío si no
     */
    Optional<Mesa> buscarPorId(MesaId mesaId);

    /**
     * Persiste una mesa nueva o actualiza una existente.
     *
     * @param mesa la mesa a guardar
     * @return la mesa guardada
     */
    Mesa guardar(Mesa mesa);

    /**
     * Verifica si ya existe una mesa con el número dado en un local específico.
     * Necesario para validar unicidad del número de mesa dentro del local.
     *
     * @param numero número de la mesa a verificar
     * @param localId identificador del local (tenant)
     * @return true si ya existe una mesa con ese número en el local, false si no
     */
    boolean existePorNumeroYLocal(int numero, LocalId localId);

    /**
     * Cuenta cuántas mesas tiene un local específico.
     * Necesario para validar que no se elimine la última mesa del local.
     *
     * @param localId identificador del local (tenant)
     * @return cantidad de mesas que tiene el local
     */
    int contarPorLocal(LocalId localId);

    /**
     * Elimina físicamente una mesa del sistema.
     * Solo debe invocarse tras validar las reglas de negocio correspondientes.
     *
     * @param mesaId identificador de la mesa a eliminar
     */
    void eliminar(MesaId mesaId);
}
