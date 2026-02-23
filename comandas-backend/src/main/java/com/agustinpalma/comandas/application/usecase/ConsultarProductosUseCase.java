package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ProductoResponse;
import com.agustinpalma.comandas.application.dto.ProductoResponse.PromocionActivaInfo;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
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
     * Ejecuta el caso de uso: consulta productos del local.
     * Si se provee un filtro de color, solo retorna productos con ese color.
     * Si no, retorna todos los productos del local.
     * 
     * Enriquece cada producto con las promociones activas cuyo alcance incluya
     * al producto como TRIGGER o TARGET.
     * 
     * @param localId identificador del local
     * @param colorHexFiltro color hexadecimal para filtrar (opcional, puede ser null)
     * @return lista de productos que cumplen el criterio (puede estar vacía)
     */
    public List<ProductoResponse> ejecutar(LocalId localId, String colorHexFiltro) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        
        List<Producto> productos;
        
        if (colorHexFiltro != null && !colorHexFiltro.isBlank()) {
            String colorNormalizado = colorHexFiltro.trim().toUpperCase();
            productos = productoRepository.buscarPorLocalYColor(localId, colorNormalizado);
        } else {
            productos = productoRepository.buscarPorLocal(localId);
        }

        // Cruce en capa de aplicación: traer promos activas y mapear por productoId
        Map<UUID, List<PromocionActivaInfo>> promosPorProducto = buildPromosPorProducto(localId);

        // Transformar a DTOs enriquecidos
        return productos.stream()
            .map(producto -> {
                UUID productoUuid = producto.getId().getValue();
                List<PromocionActivaInfo> promos = promosPorProducto.getOrDefault(productoUuid, List.of());
                return ProductoResponse.fromDomain(producto, promos);
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
}
