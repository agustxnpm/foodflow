package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.Categoria;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.infrastructure.persistence.entity.CategoriaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre la entidad de dominio Categoria y la entidad JPA CategoriaEntity.
 * Convierte entre objetos de negocio y objetos de persistencia.
 */
@Component
public class CategoriaMapper {

    /**
     * Convierte de entidad JPA a objeto de dominio.
     *
     * @param entity entidad JPA
     * @return objeto de dominio
     */
    public Categoria toDomain(CategoriaEntity entity) {
        return new Categoria(
            new CategoriaId(entity.getId()),
            new LocalId(entity.getLocalId()),
            entity.getNombre(),
            entity.getColorHex(),
            entity.isAdmiteVariantes(),
            entity.isEsCategoriaExtra(),
            entity.getOrden()
        );
    }

    /**
     * Convierte de objeto de dominio a entidad JPA.
     *
     * @param domain objeto de dominio
     * @return entidad JPA
     */
    public CategoriaEntity toEntity(Categoria domain) {
        return new CategoriaEntity(
            domain.getId().getValue(),
            domain.getLocalId().getValue(),
            domain.getNombre(),
            domain.getColorHex(),
            domain.isAdmiteVariantes(),
            domain.isEsCategoriaExtra(),
            domain.getOrden()
        );
    }
}
