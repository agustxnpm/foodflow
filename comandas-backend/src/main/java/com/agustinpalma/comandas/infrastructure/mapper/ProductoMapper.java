package com.agustinpalma.comandas.infrastructure.mapper;

import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.infrastructure.persistence.entity.ProductoEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre la entidad de dominio Producto y la entidad JPA ProductoEntity.
 * Convierte entre objetos de negocio y objetos de persistencia.
 */
@Component
public class ProductoMapper {

    /**
     * Convierte de entidad JPA a objeto de dominio.
     *
     * @param entity entidad JPA
     * @return objeto de dominio
     */
    public Producto toDomain(ProductoEntity entity) {
        return new Producto(
            new ProductoId(entity.getId()),
            new LocalId(entity.getLocalId()),
            entity.getNombre(),
            entity.getPrecio(),
            entity.isActivo(),
            entity.getColorHex(),
            entity.getGrupoVarianteId() != null ? new ProductoId(entity.getGrupoVarianteId()) : null,
            entity.isEsExtra(),
            entity.isEsModificadorEstructural(),
            entity.getCantidadDiscosCarne(),
            entity.getCategoriaId() != null ? new CategoriaId(entity.getCategoriaId()) : null,
            entity.isPermiteExtras(),
            entity.isRequiereConfiguracion(),
            entity.getStockActual(),
            entity.isControlaStock()
        );
    }

    /**
     * Convierte de objeto de dominio a entidad JPA.
     *
     * @param domain objeto de dominio
     * @return entidad JPA
     */
    public ProductoEntity toEntity(Producto domain) {
        return new ProductoEntity(
            domain.getId().getValue(),
            domain.getLocalId().getValue(),
            domain.getNombre(),
            domain.getPrecio(),
            domain.isActivo(),
            domain.getColorHex(),
            domain.getGrupoVarianteId() != null ? domain.getGrupoVarianteId().getValue() : null,
            domain.isEsExtra(),
            domain.isEsModificadorEstructural(),
            domain.getCantidadDiscosCarne(),
            domain.getCategoriaId() != null ? domain.getCategoriaId().getValue() : null,
            domain.isPermiteExtras(),
            domain.isRequiereConfiguracion(),
            domain.getStockActual(),
            domain.isControlaStock()
        );
    }
}
