package com.agustinpalma.comandas.infrastructure.persistence.jpa;

import com.agustinpalma.comandas.infrastructure.persistence.entity.PromocionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataPromocionRepository extends JpaRepository<PromocionEntity, UUID> {

    boolean existsByLocalIdAndNombreIgnoreCase(UUID localId, String nombre);

    List<PromocionEntity> findByLocalId(UUID localId);

    /**
     * HU-10: Consulta optimizada para traer solo promociones ACTIVAS de un local.
     * 
     * Evita cargar promociones inactivas/históricas a memoria.
     * El estado se compara como String ya que así está persistido en la entidad.
     * 
     * @param localId UUID del local
     * @param estado estado a filtrar (se espera "ACTIVA")
     * @return lista de promociones activas
     */
    @Query("SELECT p FROM PromocionEntity p WHERE p.localId = :localId AND p.estado = :estado")
    List<PromocionEntity> findByLocalIdAndEstado(
            @Param("localId") UUID localId, 
            @Param("estado") String estado
    );
}
