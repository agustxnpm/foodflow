package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import java.util.Objects;

/**
 * Caso de uso para eliminar un producto del catálogo.
 * 
 * Reglas de negocio implementadas:
 * - El producto debe existir y pertenecer al local (validación multi-tenancy)
 * - La eliminación es física (se borra de la base de datos)
 * - Si el producto está vinculado a pedidos activos, la base de datos rechazará la operación
 *   por integridad referencial
 * 
 * Nota: En un escenario productivo avanzado, se podría implementar validación previa
 * consultando si hay pedidos activos, o bien cambiar a eliminación lógica (estado="eliminado").
 * Para el MVP actual, confiamos en la restricción de FK de la base de datos.
 */
public class EliminarProductoUseCase {

    private final ProductoRepository productoRepository;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param productoRepository repositorio de productos
     */
    public EliminarProductoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = Objects.requireNonNull(productoRepository, "El productoRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: elimina un producto del catálogo.
     * 
     * @param productoId identificador del producto a eliminar
     * @param localId identificador del local (para validación de seguridad)
     * @throws IllegalArgumentException si el producto no existe (404 Not Found)
     * @throws IllegalArgumentException si el producto no pertenece al local (403 Forbidden)
     * @throws RuntimeException si hay un error de integridad referencial (409 Conflict)
     */
    public void ejecutar(ProductoId productoId, LocalId localId) {
        Objects.requireNonNull(productoId, "El productoId es obligatorio");
        Objects.requireNonNull(localId, "El localId es obligatorio");
        
        // Buscar el producto existente
        Producto producto = productoRepository.buscarPorId(productoId)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        // Validación de seguridad multi-tenancy: el producto debe pertenecer al local
        if (!producto.getLocalId().equals(localId)) {
            throw new IllegalArgumentException("No tiene permisos para eliminar este producto");
        }

        // Eliminar
        // Si hay restricción de FK (producto vinculado a pedidos), la BD lanzará excepción
        // que será capturada por el controller para devolver 409 Conflict
        productoRepository.eliminar(productoId);
    }
}
