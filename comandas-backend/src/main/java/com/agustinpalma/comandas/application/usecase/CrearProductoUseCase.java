package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ProductoRequest;
import com.agustinpalma.comandas.application.dto.ProductoResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import java.util.Objects;

/**
 * Caso de uso para crear un nuevo producto en el catálogo del local.
 * 
 * Reglas de negocio implementadas:
 * - El nombre debe ser único dentro del local (case insensitive)
 * - El precio debe ser mayor a cero
 * - El color se normaliza automáticamente a mayúsculas
 * - Si no se provee color, se asigna #FFFFFF por defecto
 * - El producto se vincula de forma inmutable al LocalId
 */
public class CrearProductoUseCase {

    private final ProductoRepository productoRepository;

    /**
     * Constructor con inyección de dependencias.
     * El framework de infraestructura (Spring) se encargará de proveer la implementación.
     *
     * @param productoRepository repositorio de productos
     */
    public CrearProductoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = Objects.requireNonNull(productoRepository, "El productoRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: crear un producto nuevo con los datos especificados.
     * 
     * @param localId identificador del local (tenant) al que pertenece el producto
     * @param request datos del producto a crear
     * @return DTO con la información del producto creado
     * @throws IllegalArgumentException si el nombre ya existe en el local (409 Conflict)
     * @throws IllegalArgumentException si los datos son inválidos (400 Bad Request)
     */
    public ProductoResponse ejecutar(LocalId localId, ProductoRequest request) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(request, "El request es obligatorio");
        
        // Validación de negocio: unicidad del nombre dentro del local (case insensitive)
        if (productoRepository.existePorNombreYLocal(request.nombre(), localId)) {
            throw new IllegalArgumentException(
                "Ya existe un producto con el nombre '" + request.nombre() + "' en este local"
            );
        }

        // Crear la entidad de dominio
        // El dominio se encarga de validar precio, nombre y normalizar el color
        ProductoId nuevoId = ProductoId.generate();
        boolean activarStock = request.controlaStock() != null ? request.controlaStock() : false;
        boolean esExtra = request.esExtra() != null ? request.esExtra() : false;
        boolean permiteExtras = request.permiteExtras() != null ? request.permiteExtras() : true;
        boolean requiereConfig = request.requiereConfiguracion() != null ? request.requiereConfiguracion() : true;
        Producto nuevoProducto = new Producto(
            nuevoId,
            localId,
            request.nombre(),
            request.precio(),
            request.activo() != null ? request.activo() : true, // Por defecto activo
            request.colorHex(), // Puede ser null, el dominio asigna default
            null,   // grupoVarianteId
            esExtra,
            null,   // cantidadDiscosCarne
            request.categoria(),  // Puede ser null
            permiteExtras,
            requiereConfig,
            0,      // stockActual inicial
            activarStock
        );

        // Persistir
        Producto productoGuardado = productoRepository.guardar(nuevoProducto);

        // Retornar DTO
        return ProductoResponse.fromDomain(productoGuardado);
    }
}
