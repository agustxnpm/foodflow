package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ProductoResponse;
import com.agustinpalma.comandas.application.dto.VarianteProductoRequest;
import com.agustinpalma.comandas.application.dto.VarianteProductoResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;

import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para crear una variante de un producto existente.
 * 
 * Una variante es un producto que pertenece al mismo grupo (grupoVarianteId)
 * y se diferencia por su jerarquía (cantidadDiscosCarne).
 * 
 * Flujo:
 * 1. Buscar el producto base por ID + localId (validación multi-tenancy)
 * 2. Determinar el grupoVarianteId:
 *    - Si el base ya tiene grupo → usar ese grupo
 *    - Si el base NO tiene grupo → asignar el ID del base como grupoVarianteId
 *      (el base se convierte en el líder del grupo con cantidadDiscosCarne = 1)
 * 3. Validar unicidad del nombre dentro del local
 * 4. Validar unicidad de cantidadDiscosCarne dentro del grupo
 * 5. Crear el nuevo Producto como variante
 * 6. Persistir (base actualizado si corresponde + nueva variante)
 * 7. Retornar la variante creada + todas las hermanas del grupo
 * 
 * Reglas de negocio:
 * - Un producto extra (esExtra=true) no puede tener variantes
 * - La cantidadDiscosCarne debe ser única dentro del grupo
 * - El nombre debe ser único dentro del local (como cualquier producto)
 * - La variante hereda colorHex, categoriaId, permiteExtras y requiereConfiguracion
 *   del producto base si no se especifican explícitamente
 */
@Transactional
public class CrearVarianteUseCase {

    private final ProductoRepository productoRepository;

    public CrearVarianteUseCase(ProductoRepository productoRepository) {
        this.productoRepository = Objects.requireNonNull(productoRepository, "El productoRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: crear una variante de un producto base.
     * 
     * @param localId identificador del local (tenant)
     * @param productoBaseId ID del producto base del cual se crea la variante
     * @param request datos de la nueva variante
     * @return DTO con la variante creada y todas las hermanas del grupo
     * @throws IllegalArgumentException si el producto base no existe o no pertenece al local
     * @throws IllegalArgumentException si el nombre ya existe en el local
     * @throws IllegalArgumentException si la cantidadDiscosCarne ya existe en el grupo
     * @throws IllegalStateException si el producto base es un extra
     */
    public VarianteProductoResponse ejecutar(LocalId localId, ProductoId productoBaseId, VarianteProductoRequest request) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(productoBaseId, "El productoBaseId es obligatorio");
        Objects.requireNonNull(request, "El request es obligatorio");

        // 1. Buscar producto base con validación multi-tenancy
        Producto productoBase = productoRepository.buscarPorIdYLocal(productoBaseId, localId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el producto base con ID: " + productoBaseId.getValue() + " en el local actual"
            ));

        // Validación: un extra no puede ser base de variantes
        if (productoBase.isEsExtra()) {
            throw new IllegalArgumentException(
                "El producto '" + productoBase.getNombre() + "' es un extra y no puede tener variantes"
            );
        }

        // 2. Determinar grupoVarianteId
        ProductoId grupoId;
        if (productoBase.tieneVariantesEstructurales()) {
            // El base ya pertenece a un grupo → usar ese grupo
            grupoId = productoBase.getGrupoVarianteId();
        } else {
            // Primera variante: el base se convierte en líder del grupo
            grupoId = productoBase.getId();
            productoBase.asignarGrupoVariante(grupoId, 1);
            productoRepository.guardar(productoBase);
        }

        // 3. Validar unicidad del nombre dentro del local
        if (productoRepository.existePorNombreYLocal(request.nombre(), localId)) {
            throw new IllegalArgumentException(
                "Ya existe un producto con el nombre '" + request.nombre() + "' en este local"
            );
        }

        // 4. Validar unicidad de cantidadDiscosCarne dentro del grupo
        List<Producto> hermanasExistentes = productoRepository.buscarPorGrupoVariante(localId, grupoId);
        boolean discosOcupados = hermanasExistentes.stream()
            .anyMatch(p -> request.cantidadDiscosCarne().equals(p.getCantidadDiscosCarne()));
        if (discosOcupados) {
            throw new IllegalArgumentException(
                "Ya existe una variante con cantidadDiscosCarne=" + request.cantidadDiscosCarne()
                + " en el grupo de variantes"
            );
        }

        // 5. Crear el nuevo Producto como variante (hereda campos del base si no se especifican)
        ProductoId nuevoId = ProductoId.generate();
        boolean activo = request.activo() != null ? request.activo() : true;
        String colorHex = request.colorHex() != null ? request.colorHex() : productoBase.getColorHex();
        boolean controlaStock = request.controlaStock() != null ? request.controlaStock() : false;
        boolean permiteExtras = request.permiteExtras() != null ? request.permiteExtras() : productoBase.isPermiteExtras();
        boolean requiereConfig = request.requiereConfiguracion() != null ? request.requiereConfiguracion() : productoBase.isRequiereConfiguracion();
        CategoriaId categoriaId = request.categoriaId() != null
            ? CategoriaId.from(request.categoriaId())
            : productoBase.getCategoriaId();

        Producto nuevaVariante = new Producto(
            nuevoId,
            localId,
            request.nombre(),
            request.precio(),
            activo,
            colorHex,
            grupoId,
            false,  // esExtra: una variante nunca es un extra
            false,  // esModificadorEstructural: no aplica a variantes
            request.cantidadDiscosCarne(),
            categoriaId,
            permiteExtras,
            requiereConfig,
            0,      // stockActual inicial
            controlaStock
        );

        // 6. Persistir la nueva variante
        Producto varianteGuardada = productoRepository.guardar(nuevaVariante);

        // 7. Retornar respuesta con la variante creada y todas las hermanas
        List<Producto> todasLasVariantes = productoRepository.buscarPorGrupoVariante(localId, grupoId);
        List<ProductoResponse> variantesResponse = todasLasVariantes.stream()
            .map(ProductoResponse::fromDomain)
            .toList();

        return new VarianteProductoResponse(
            ProductoResponse.fromDomain(varianteGuardada),
            variantesResponse
        );
    }
}
