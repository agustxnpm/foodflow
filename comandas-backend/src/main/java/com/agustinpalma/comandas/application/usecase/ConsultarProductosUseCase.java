package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ProductoResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso para consultar productos del catálogo.
 * 
 * Funcionalidades:
 * - Listar todos los productos del local
 * - Filtrar productos por color hexadecimal (opcional)
 * 
 * El filtrado por color es útil para la interfaz de toma de pedidos,
 * permitiendo al mozo identificar visualmente los productos por categorías de color.
 */
public class ConsultarProductosUseCase {

    private final ProductoRepository productoRepository;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param productoRepository repositorio de productos
     */
    public ConsultarProductosUseCase(ProductoRepository productoRepository) {
        this.productoRepository = Objects.requireNonNull(productoRepository, "El productoRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: consulta productos del local.
     * Si se provee un filtro de color, solo retorna productos con ese color.
     * Si no, retorna todos los productos del local.
     * 
     * @param localId identificador del local
     * @param colorHexFiltro color hexadecimal para filtrar (opcional, puede ser null)
     * @return lista de productos que cumplen el criterio (puede estar vacía)
     */
    public List<ProductoResponse> ejecutar(LocalId localId, String colorHexFiltro) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        
        List<Producto> productos;
        
        if (colorHexFiltro != null && !colorHexFiltro.isBlank()) {
            // Filtrar por color
            // El color se debe normalizar antes de buscar (el dominio lo hace automáticamente)
            String colorNormalizado = colorHexFiltro.trim().toUpperCase();
            productos = productoRepository.buscarPorLocalYColor(localId, colorNormalizado);
        } else {
            // Listar todos
            productos = productoRepository.buscarPorLocal(localId);
        }

        // Transformar a DTOs
        return productos.stream()
            .map(ProductoResponse::fromDomain)
            .toList();
    }
}
