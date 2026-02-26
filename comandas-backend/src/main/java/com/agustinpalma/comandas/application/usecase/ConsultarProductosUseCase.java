package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ProductoResponse;
import com.agustinpalma.comandas.application.dto.ProductoResponse.PromocionActivaInfo;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import java.util.*;

/**
 * Caso de uso para consultar productos del catálogo.
 * 
 * Funcionalidades:
 * - Listar todos los productos del local
 * - Filtrar productos por color hexadecimal (opcional)
 * - Enriquecer cada producto con las promociones activas que le aplican
 * 
 * El cruce entre productos y promociones se realiza en esta capa de aplicación
 * para mantener el dominio desacoplado: Producto no conoce a Promocion.
 * 
 * El filtrado por color es útil para la interfaz de toma de pedidos,
 * permitiendo al mozo identificar visualmente los productos por categorías de color.
 */
public class ConsultarProductosUseCase {

    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param productoRepository repositorio de productos
     * @param promocionRepository repositorio de promociones (para enriquecer con promos activas)
     */
    public ConsultarProductosUseCase(
            ProductoRepository productoRepository,
            PromocionRepository promocionRepository
    ) {
        this.productoRepository = Objects.requireNonNull(productoRepository, "El productoRepository es obligatorio");
        this.promocionRepository = Objects.requireNonNull(promocionRepository, "El promocionRepository es obligatorio");
    }

    /**
     * Sobrecarga de retrocompatibilidad: consulta sin filtro de categoría ni activo.
     */
    public List<ProductoResponse> ejecutar(LocalId localId, String colorHexFiltro) {
        return ejecutar(localId, colorHexFiltro, null, null);
    }

    /**
     * Sobrecarga de retrocompatibilidad: consulta sin filtro de activo.
     */
    public List<ProductoResponse> ejecutar(LocalId localId, String colorHexFiltro, String categoriaIdFiltro) {
        return ejecutar(localId, colorHexFiltro, categoriaIdFiltro, null);
    }

    /**
     * Ejecuta el caso de uso: consulta productos del local.
     * Soporta filtros opcionales por color, categoría y estado activo.
     * Si no se proveen filtros, retorna todos los productos del local.
     * 
     * Enriquece cada producto con las promociones activas cuyo alcance incluya
     * al producto como TRIGGER o TARGET.
     * 
     * @param localId identificador del local
     * @param colorHexFiltro color hexadecimal para filtrar (opcional, puede ser null)
     * @param categoriaIdFiltro UUID de categoría para filtrar (opcional, puede ser null)
     * @param soloActivos si true, filtra solo productos activos; si null/false, retorna todos
     * @return lista de productos que cumplen el criterio (puede estar vacía)
     */
    public List<ProductoResponse> ejecutar(LocalId localId, String colorHexFiltro, String categoriaIdFiltro, Boolean soloActivos) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        
        List<Producto> productos;
        
        if (colorHexFiltro != null && !colorHexFiltro.isBlank()) {
            String colorNormalizado = colorHexFiltro.trim().toUpperCase();
            productos = productoRepository.buscarPorLocalYColor(localId, colorNormalizado);
        } else if (categoriaIdFiltro != null && !categoriaIdFiltro.isBlank()) {
            CategoriaId categoriaId = CategoriaId.from(categoriaIdFiltro.trim());
            productos = productoRepository.buscarPorCategoriaId(localId, categoriaId);
        } else {
            productos = productoRepository.buscarPorLocal(localId);
        }

        // Filtrar por estado activo si se solicita (POS solo muestra activos)
        if (Boolean.TRUE.equals(soloActivos)) {
            productos = productos.stream().filter(Producto::isActivo).toList();
        }

        // Cruce en capa de aplicación: traer promos activas y mapear por productoId
        Map<UUID, List<PromocionActivaInfo>> promosPorProducto = buildPromosPorProducto(localId);

        // Cache de maxEstructural por grupoVarianteId para evitar N+1 queries
        Map<ProductoId, Integer> cacheMaxDiscos = new HashMap<>();

        // Transformar a DTOs enriquecidos con promos y flag de disco extra
        return productos.stream()
            .map(producto -> {
                UUID productoUuid = producto.getId().getValue();
                List<PromocionActivaInfo> promos = promosPorProducto.getOrDefault(productoUuid, List.of());
                boolean puedeAgregarDiscoExtra = calcularPuedeAgregarDiscoExtra(producto, localId, cacheMaxDiscos);
                return ProductoResponse.fromDomain(producto, promos, puedeAgregarDiscoExtra);
            })
            .toList();
    }

    /**
     * Construye un mapa productoId → lista de PromocionActivaInfo.
     * 
     * Recorre las promociones activas del local y, por cada una, inspecciona
     * su AlcancePromocion para saber qué productos participan (trigger o target).
     * 
     * Este cruce vive en la capa de aplicación para no acoplar Producto con Promocion
     * en el dominio.
     */
    private Map<UUID, List<PromocionActivaInfo>> buildPromosPorProducto(LocalId localId) {
        List<Promocion> promocionesActivas = promocionRepository.buscarActivasPorLocal(localId);
        Map<UUID, List<PromocionActivaInfo>> mapa = new HashMap<>();

        for (Promocion promo : promocionesActivas) {
            PromocionActivaInfo info = new PromocionActivaInfo(
                promo.getNombre(),
                promo.getDescripcion(),
                promo.getEstrategia().getTipo().name()
            );

            // Productos que participan como TRIGGER o TARGET en esta promo
            Set<UUID> productosInvolucrados = new HashSet<>();
            productosInvolucrados.addAll(promo.getAlcance().getProductosTriggerIds());
            productosInvolucrados.addAll(promo.getAlcance().getProductosTargetIds());

            for (UUID productoId : productosInvolucrados) {
                mapa.computeIfAbsent(productoId, k -> new ArrayList<>()).add(info);
            }
        }

        return mapa;
    }

    /**
     * Calcula si un producto puede recibir un modificador estructural como extra.
     * 
     * Regla única:
     *   - Si el producto NO tiene grupoVarianteId → true (sin restricción)
     *   - Si tiene grupo → true solo si cantidadDiscosCarne == maxEstructural del grupo
     * 
     * Usa cache para evitar consultas repetidas al repositorio por cada producto del mismo grupo.
     *
     * @param producto producto de dominio a evaluar
     * @param localId ID del local (para consultar maxEstructural)
     * @param cacheMaxDiscos cache mutable de maxDiscos por grupo
     * @return true si el producto puede recibir extras estructurales
     */
    private boolean calcularPuedeAgregarDiscoExtra(
            Producto producto,
            LocalId localId,
            Map<ProductoId, Integer> cacheMaxDiscos
    ) {
        // Sin grupo de variantes → sin restricción
        if (producto.getGrupoVarianteId() == null) {
            return true;
        }

        Integer discos = producto.getCantidadDiscosCarne();
        if (discos == null) {
            return true;
        }

        // Obtener maxEstructural del grupo (con cache para evitar N+1)
        int maxEstructural = cacheMaxDiscos.computeIfAbsent(
            producto.getGrupoVarianteId(),
            grupoId -> productoRepository.obtenerMaximaCantidadDiscos(localId, grupoId)
        );

        return discos >= maxEstructural;
    }
}
