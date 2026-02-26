package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ProductoRequest;
import com.agustinpalma.comandas.application.dto.ProductoResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import java.util.Objects;

/**
 * Caso de uso para editar un producto existente del catálogo.
 * 
 * Reglas de negocio implementadas:
 * - El producto debe existir y pertenecer al local (validación multi-tenancy)
 * - El nombre debe seguir siendo único dentro del local
 * - La edición del precio NO afecta ítems en pedidos ya abiertos (garantizado por Snapshot)
 * - Se pueden actualizar: nombre, precio, estado activo/inactivo, color y clasificación como extra
 */
public class EditarProductoUseCase {

    private final ProductoRepository productoRepository;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param productoRepository repositorio de productos
     */
    public EditarProductoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = Objects.requireNonNull(productoRepository, "El productoRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: actualiza los datos de un producto existente.
     * 
     * @param productoId identificador del producto a editar
     * @param localId identificador del local (para validación de seguridad)
     * @param request nuevos datos del producto
     * @return DTO con la información del producto actualizado
     * @throws IllegalArgumentException si el producto no existe (404 Not Found)
     * @throws IllegalArgumentException si el producto no pertenece al local (403 Forbidden)
     * @throws IllegalArgumentException si el nuevo nombre ya está en uso (409 Conflict)
     */
    public ProductoResponse ejecutar(ProductoId productoId, LocalId localId, ProductoRequest request) {
        Objects.requireNonNull(productoId, "El productoId es obligatorio");
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(request, "El request es obligatorio");
        
        // Buscar el producto existente
        Producto producto = productoRepository.buscarPorId(productoId)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        // Validación de seguridad multi-tenancy: el producto debe pertenecer al local
        if (!producto.getLocalId().equals(localId)) {
            throw new IllegalArgumentException("No tiene permisos para editar este producto");
        }

        // Si cambió el nombre, validar unicidad (excluyendo el producto actual)
        if (!producto.getNombre().equalsIgnoreCase(request.nombre())) {
            if (productoRepository.existePorNombreYLocalExcluyendo(request.nombre(), localId, productoId)) {
                throw new IllegalArgumentException(
                    "Ya existe otro producto con el nombre '" + request.nombre() + "' en este local"
                );
            }
        }

        // Actualizar campos usando métodos de dominio
        // Estos métodos contienen las validaciones de negocio
        producto.actualizarNombre(request.nombre());
        producto.actualizarPrecio(request.precio());
        producto.cambiarEstado(request.activo() != null ? request.activo() : producto.isActivo());

        // Color: solo se modifica si el request lo incluye explícitamente
        // Esto evita que un toggle de activo/inactivo borre el color existente
        if (request.colorHex() != null) {
            producto.actualizarColor(request.colorHex());
        }

        // Reclasificación como extra: solo se modifica si el request lo incluye explícitamente
        if (request.esExtra() != null) {
            producto.reclasificarExtra(request.esExtra());
        }

        // Modificador estructural: solo se modifica si el request lo incluye explícitamente
        if (request.esModificadorEstructural() != null) {
            producto.cambiarModificadorEstructural(request.esModificadorEstructural());
        }

        // Categoría: se actualiza si viene en el request (null = desclasificar)
        if (request.categoriaId() != null) {
            producto.actualizarCategoria(CategoriaId.from(request.categoriaId()));
        }

        // Permite extras: solo se modifica si el request lo incluye explícitamente
        if (request.permiteExtras() != null) {
            producto.cambiarPermiteExtras(request.permiteExtras());
        }

        // Configuración POS: solo se modifica si el request lo incluye explícitamente
        if (request.requiereConfiguracion() != null) {
            producto.cambiarRequiereConfiguracion(request.requiereConfiguracion());
        }

        // Control de stock: solo se modifica si el request lo incluye explícitamente
        if (request.controlaStock() != null) {
            if (request.controlaStock()) {
                producto.activarControlStock();
            } else {
                producto.desactivarControlStock();
            }
        }

        // Persistir cambios
        Producto productoActualizado = productoRepository.guardar(producto);

        // Retornar DTO
        return ProductoResponse.fromDomain(productoActualizado);
    }
}
