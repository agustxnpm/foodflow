package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AsociarScopeCommand;
import com.agustinpalma.comandas.application.dto.AsociarScopeCommand.ItemScopeParams;
import com.agustinpalma.comandas.application.dto.PromocionResponse;
import com.agustinpalma.comandas.domain.model.AlcancePromocion;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.ItemPromocion;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso: Asociar productos/categorías a una promoción (HU-09).
 * 
 * Responsabilidades:
 * 1. Validar que la promoción exista y pertenezca al local (multi-tenancy)
 * 2. Validar que todos los productos referenciados existan y pertenezcan al local
 * 3. Construir el alcance (AlcancePromocion) con los items y roles especificados
 * 4. Actualizar la promoción
 * 5. Persistir
 * 
 * Nota: La validación de categorías se implementará cuando exista CategoriaRepository.
 * Por ahora, solo validamos productos.
 */
public class AsociarProductoAPromocionUseCase {

    private final PromocionRepository promocionRepository;
    private final ProductoRepository productoRepository;

    public AsociarProductoAPromocionUseCase(
            PromocionRepository promocionRepository,
            ProductoRepository productoRepository
    ) {
        this.promocionRepository = Objects.requireNonNull(
                promocionRepository,
                "El promocionRepository es obligatorio"
        );
        this.productoRepository = Objects.requireNonNull(
                productoRepository,
                "El productoRepository es obligatorio"
        );
    }

    public PromocionResponse ejecutar(LocalId localId, PromocionId promocionId, AsociarScopeCommand command) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(promocionId, "El promocionId es obligatorio");
        Objects.requireNonNull(command, "El command es obligatorio");

        // 1. Recuperar la promoción validando multi-tenancy
        Promocion promocion = promocionRepository.buscarPorIdYLocal(promocionId, localId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la promoción con id " + promocionId.getValue() +
                        " para el local " + localId.getValue()
                ));

        // 2. Validar que todas las referencias de tipo PRODUCTO existen y pertenecen al local
        validarProductos(command.items(), localId);

        // 3. Construir el alcance
        List<ItemPromocion> items = command.items().stream()
                .map(this::construirItem)
                .collect(Collectors.toList());

        AlcancePromocion nuevoAlcance = new AlcancePromocion(items);

        // 4. Actualizar la promoción
        promocion.definirAlcance(nuevoAlcance);

        // 5. Persistir
        Promocion promocionActualizada = promocionRepository.guardar(promocion);

        return PromocionResponse.fromDomain(promocionActualizada);
    }

    /**
     * Valida que todos los productos referenciados en el comando existan y pertenezcan al local.
     */
    private void validarProductos(List<ItemScopeParams> items, LocalId localId) {
        items.stream()
                .filter(item -> item.tipo() == com.agustinpalma.comandas.domain.model.DomainEnums.TipoAlcance.PRODUCTO)
                .forEach(item -> {
                    ProductoId productoId = new ProductoId(UUID.fromString(item.referenciaId()));
                    
                    if (productoRepository.buscarPorIdYLocal(productoId, localId).isEmpty()) {
                        throw new IllegalArgumentException(
                                "El producto con id " + item.referenciaId() + 
                                " no existe o no pertenece al local " + localId.getValue()
                        );
                    }
                });
    }

    /**
     * Construye un ItemPromocion a partir de los parámetros del comando.
     */
    private ItemPromocion construirItem(ItemScopeParams params) {
        return new ItemPromocion(
                ItemPromocionId.generate(),
                UUID.fromString(params.referenciaId()),
                params.tipo(),
                params.rol()
        );
    }
}
