package com.agustinpalma.comandas.application.dto;

import com.agustinpalma.comandas.domain.model.Producto;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de salida para productos.
 * Representa la información de un producto que se expone en la API REST.
 * Incluye el colorHex para permitir al frontend renderizar botones con colores.
 * Incluye información de stock para transparencia del inventario.
 * Incluye las promociones activas asociadas al producto (cruce en capa de aplicación).
 */
public record ProductoResponse(
    String id,              // UUID como String para JSON/REST
    String nombre,
    BigDecimal precio,
    boolean activo,
    String colorHex,        // Siempre normalizado a mayúsculas (ej: #FF0000)
    Integer stockActual,    // Cantidad actual en inventario
    Boolean controlaStock,  // Si el producto tiene control de inventario activo
    boolean esExtra,        // true si es un extra (huevo, queso, disco de carne, etc.)
    boolean esModificadorEstructural, // true si este extra activa normalización de variantes
    String categoriaId,     // UUID de la categoría del catálogo — puede ser null
    boolean permiteExtras,  // Si el producto acepta extras/agregados
    boolean requiereConfiguracion, // Si true, el POS abre modal de configuración antes de agregar
    String grupoVarianteId, // UUID del grupo de variantes — puede ser null
    Integer cantidadDiscosCarne, // Cantidad de discos de carne (variantes) — puede ser null
    List<PromocionActivaInfo> promocionesActivas // Promociones vigentes que aplican a este producto
) {

    /**
     * Información resumida de una promoción activa asociada a un producto.
     * Se usa exclusivamente como DTO de lectura para el frontend.
     */
    public record PromocionActivaInfo(
        String nombre,
        String tipoEstrategia // ej: "DESCUENTO_DIRECTO", "CANTIDAD_FIJA", etc.
    ) {}

    /**
     * Factory method para construir el DTO desde la entidad de dominio.
     * Sin enriquecimiento de promociones (lista vacía).
     */
    public static ProductoResponse fromDomain(Producto producto) {
        return fromDomain(producto, List.of());
    }

    /**
     * Factory method para construir el DTO desde la entidad de dominio
     * con las promociones activas asociadas.
     *
     * @param producto entidad de dominio
     * @param promociones lista de promociones activas que aplican al producto
     */
    public static ProductoResponse fromDomain(Producto producto, List<PromocionActivaInfo> promociones) {
        return new ProductoResponse(
            producto.getId().getValue().toString(),
            producto.getNombre(),
            producto.getPrecio(),
            producto.isActivo(),
            producto.getColorHex(),
            producto.getStockActual(),
            producto.isControlaStock(),
            producto.isEsExtra(),
            producto.isEsModificadorEstructural(),
            producto.getCategoriaId() != null ? producto.getCategoriaId().getValue().toString() : null,
            producto.isPermiteExtras(),
            producto.isRequiereConfiguracion(),
            producto.getGrupoVarianteId() != null ? producto.getGrupoVarianteId().getValue().toString() : null,
            producto.getCantidadDiscosCarne(),
            promociones != null ? List.copyOf(promociones) : List.of()
        );
    }
}
