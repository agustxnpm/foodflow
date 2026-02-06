package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.AlcancePromocion;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPromocion;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PromocionId;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.infrastructure.mapper.PromocionMapper;
import com.agustinpalma.comandas.infrastructure.persistence.entity.ItemPromocionEntity;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataItemPromocionRepository;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataPromocionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class PromocionRepositoryImpl implements PromocionRepository {

    private final SpringDataPromocionRepository springDataRepository;
    private final SpringDataItemPromocionRepository itemPromocionRepository;
    private final PromocionMapper mapper;

    public PromocionRepositoryImpl(
            SpringDataPromocionRepository springDataRepository,
            SpringDataItemPromocionRepository itemPromocionRepository,
            PromocionMapper mapper
    ) {
        this.springDataRepository = springDataRepository;
        this.itemPromocionRepository = itemPromocionRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Promocion guardar(Promocion promocion) {
        // 1. Guardar la entidad principal
        var entity = mapper.toEntity(promocion);
        var guardada = springDataRepository.save(entity);

        // 2. Eliminar items de alcance existentes
        itemPromocionRepository.deleteByPromocionId(guardada.getId());

        // 3. Guardar nuevos items de alcance (HU-09)
        if (promocion.getAlcance() != null && promocion.getAlcance().tieneItems()) {
            List<ItemPromocionEntity> itemEntities = mapper.toEntityAlcance(
                    promocion.getAlcance(),
                    guardada.getId()
            );
            itemPromocionRepository.saveAll(itemEntities);
        }

        // 4. Retornar la promoción completa con alcance
        return buscarPorId(new PromocionId(guardada.getId())).orElseThrow();
    }

    @Override
    public Optional<Promocion> buscarPorId(PromocionId id) {
        return springDataRepository
                .findById(id.getValue())
                .map(entity -> {
                    Promocion promocion = mapper.toDomain(entity);
                    cargarAlcance(promocion, id);
                    return promocion;
                });
    }

    @Override
    public Optional<Promocion> buscarPorIdYLocal(PromocionId id, LocalId localId) {
        return springDataRepository
                .findById(id.getValue())
                .filter(entity -> entity.getLocalId().equals(localId.getValue()))
                .map(entity -> {
                    Promocion promocion = mapper.toDomain(entity);
                    cargarAlcance(promocion, id);
                    return promocion;
                });
    }

    @Override
    public boolean existePorNombreYLocal(String nombre, LocalId localId) {
        return springDataRepository.existsByLocalIdAndNombreIgnoreCase(localId.getValue(), nombre);
    }

    @Override
    public List<Promocion> buscarPorLocal(LocalId localId) {
        return springDataRepository.findByLocalId(localId.getValue())
                .stream()
                .map(entity -> {
                    Promocion promocion = mapper.toDomain(entity);
                    cargarAlcance(promocion, new PromocionId(entity.getId()));
                    return promocion;
                })
                .toList();
    }

    /**
     * HU-10: Busca todas las promociones activas de un local.
     * 
     * Utiliza consulta optimizada para filtrar por estado directamente en BD.
     */
    @Override
    public List<Promocion> buscarActivasPorLocal(LocalId localId) {
        return springDataRepository
                .findByLocalIdAndEstado(localId.getValue(), EstadoPromocion.ACTIVA.name())
                .stream()
                .map(entity -> {
                    Promocion promocion = mapper.toDomain(entity);
                    cargarAlcance(promocion, new PromocionId(entity.getId()));
                    return promocion;
                })
                .toList();
    }

    /**
     * Carga el alcance (scope) de la promoción desde la tabla intermedia.
     * HU-09: Asociar productos a promociones.
     */
    private void cargarAlcance(Promocion promocion, PromocionId promocionId) {
        List<ItemPromocionEntity> itemEntities = itemPromocionRepository
                .findByPromocionId(promocionId.getValue());

        AlcancePromocion alcance = mapper.toDomainAlcance(itemEntities);
        promocion.definirAlcance(alcance);
    }
}
