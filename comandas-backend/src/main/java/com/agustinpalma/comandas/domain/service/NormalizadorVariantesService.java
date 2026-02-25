package com.agustinpalma.comandas.domain.service;

import com.agustinpalma.comandas.domain.model.ExtraPedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain Service para normalización automática de variantes.
 * 
 * Regla de negocio fundamental:
 * Un modificador estructural (ej: disco de carne) SOLO puede agregarse como extra 
 * a la variante máxima de un grupo. Si se intenta agregar a una variante menor, 
 * el sistema convierte automáticamente al siguiente nivel (ej: Simple + Disco → Doble).
 * 
 * Los modificadores estructurales se identifican por el flag esModificadorEstructural = true,
 * NO por nombre literal.
 * 
 * Responsabilidades:
 * 1. Detectar si algún extra es un modificador estructural
 * 2. Buscar variantes hermanas del mismo grupo
 * 3. Determinar la variante adecuada por cantidad de discos
 * 4. Convertir automáticamente si es necesario
 * 5. Filtrar extras de modificadores que fueron absorbidos por la conversión
 * 
 * Este servicio NO tiene estado. Es puro (dado un input, siempre retorna el mismo output).
 */
public class NormalizadorVariantesService {

    /**
     * Normaliza el producto seleccionado y los extras según la regla de modificadores estructurales.
     * 
     * Lógica:
     * 1. Cuenta cuántos modificadores estructurales hay en los extras solicitados
     * 2. Si el producto NO es variante estructural → devuelve todo sin cambios
     * 3. Si es variante estructural:
     *    a. Determina cuántos discos TOTALES hay (producto + extras modificadores)
     *    b. Busca la variante adecuada para esa cantidad
     *    c. Si existe una variante con exactamente esa cantidad → la usa
     *    d. Si no existe → usa la variante máxima + los modificadores sobrantes como extras
     * 4. Filtra los modificadores que fueron absorbidos por la conversión
     * 
     * @param productoSeleccionado producto inicial seleccionado por el usuario
     * @param extrasOriginal lista original de extras solicitados
     * @param variantesHermanas lista de todas las variantes del mismo grupo (incluye productoSeleccionado)
     * @param idsModificadoresEstructurales IDs de productos que son modificadores estructurales
     * @return ResultadoNormalizacion con el producto final y extras finales
     */
    public ResultadoNormalizacion normalizarVariante(
            Producto productoSeleccionado,
            List<ExtraPedido> extrasOriginal,
            List<Producto> variantesHermanas,
            Set<ProductoId> idsModificadoresEstructurales
    ) {
        Objects.requireNonNull(productoSeleccionado, "El producto seleccionado no puede ser null");
        Objects.requireNonNull(extrasOriginal, "La lista de extras no puede ser null");
        Objects.requireNonNull(idsModificadoresEstructurales, "Los IDs de modificadores estructurales no pueden ser null");
        
        // Si el producto NO tiene variantes estructurales, no hay nada que normalizar
        if (!productoSeleccionado.tieneVariantesEstructurales()) {
            return new ResultadoNormalizacion(productoSeleccionado, extrasOriginal, false);
        }

        // Si no hay modificadores estructurales catalogados, no hay normalización posible
        if (idsModificadoresEstructurales.isEmpty()) {
            return new ResultadoNormalizacion(productoSeleccionado, extrasOriginal, false);
        }
        
        // Contar cuántos modificadores estructurales hay en los extras
        long modificadoresEnExtras = extrasOriginal.stream()
            .filter(extra -> idsModificadoresEstructurales.contains(extra.getProductoId()))
            .count();
        
        // Si no hay modificadores en extras, no hay conversión necesaria
        if (modificadoresEnExtras == 0) {
            return new ResultadoNormalizacion(productoSeleccionado, extrasOriginal, false);
        }
        
        // Cantidad total de discos = discos base del producto + modificadores en extras
        int discosTotales = productoSeleccionado.getCantidadDiscosCarne() + (int) modificadoresEnExtras;
        
        // Buscar la variante adecuada para esa cantidad de discos
        Producto varianteFinal = buscarVarianteParaCantidad(
            discosTotales, 
            variantesHermanas, 
            productoSeleccionado
        );
        
        // Determinar cuántos discos fueron absorbidos por la variante final
        int discosAbsorbidos = varianteFinal.getCantidadDiscosCarne() - productoSeleccionado.getCantidadDiscosCarne();
        
        // Filtrar los extras: eliminar los modificadores absorbidos, mantener el resto
        List<ExtraPedido> extrasFiltrados = filtrarModificadoresAbsorbidos(
            extrasOriginal, 
            idsModificadoresEstructurales, 
            discosAbsorbidos
        );
        
        // Detectar si hubo conversión
        boolean huboConversion = !varianteFinal.getId().equals(productoSeleccionado.getId());
        
        return new ResultadoNormalizacion(varianteFinal, extrasFiltrados, huboConversion);
    }

    /**
     * Busca la variante adecuada para la cantidad de discos solicitada.
     * 
     * Estrategia:
     * 1. Buscar variante exacta (cantidadDiscos == discosSolicitados)
     * 2. Si no existe, buscar la variante MÁXIMA del grupo
     * 3. Si no hay variantes, retornar el producto original
     * 
     * @param discosSolicitados cantidad total de discos requeridos
     * @param variantes lista de variantes hermanas
     * @param productoOriginal producto de fallback si no hay variantes
     * @return el producto adecuado para la cantidad de discos
     */
    private Producto buscarVarianteParaCantidad(
            int discosSolicitados, 
            List<Producto> variantes,
            Producto productoOriginal
    ) {
        if (variantes == null || variantes.isEmpty()) {
            return productoOriginal;
        }
        
        // Filtrar solo variantes estructurales válidas
        List<Producto> variantesValidas = variantes.stream()
            .filter(Producto::tieneVariantesEstructurales)
            .collect(Collectors.toList());
        
        if (variantesValidas.isEmpty()) {
            return productoOriginal;
        }
        
        // Intentar match exacto
        Producto varianteExacta = variantesValidas.stream()
            .filter(v -> v.getCantidadDiscosCarne() == discosSolicitados)
            .findFirst()
            .orElse(null);
        
        if (varianteExacta != null) {
            return varianteExacta;
        }
        
        // No hay match exacto: buscar la variante máxima
        return variantesValidas.stream()
            .max(Comparator.comparingInt(Producto::getCantidadDiscosCarne))
            .orElse(productoOriginal);
    }

    /**
     * Filtra los modificadores estructurales que fueron absorbidos por la conversión de variante.
     * 
     * Ejemplo:
     * - Extras originales: [Disco, Disco, Huevo, Queso]
     * - Modificadores absorbidos: 2
     * - Resultado: [Huevo, Queso]
     * 
     * @param extrasOriginales lista original de extras
     * @param idsModificadores IDs de productos que son modificadores estructurales
     * @param cantidadAEliminar cuántos modificadores fueron absorbidos
     * @return nueva lista con los modificadores absorbidos eliminados
     */
    private List<ExtraPedido> filtrarModificadoresAbsorbidos(
            List<ExtraPedido> extrasOriginales,
            Set<ProductoId> idsModificadores,
            int cantidadAEliminar
    ) {
        if (cantidadAEliminar == 0) {
            return new ArrayList<>(extrasOriginales);
        }
        
        List<ExtraPedido> resultado = new ArrayList<>();
        int modificadoresEliminados = 0;
        
        for (ExtraPedido extra : extrasOriginales) {
            boolean esModificador = idsModificadores.contains(extra.getProductoId());
            
            if (esModificador && modificadoresEliminados < cantidadAEliminar) {
                // Este modificador fue absorbido por la conversión → NO agregarlo
                modificadoresEliminados++;
            } else {
                // Extra normal o modificador excedente → agregarlo
                resultado.add(extra);
            }
        }
        
        return resultado;
    }

    /**
     * Value Object que encapsula el resultado de la normalización.
     * 
     * Contiene:
     * - productoFinal: producto a usar (puede ser diferente al seleccionado)
     * - extrasFiltrados: extras finales (sin discos absorbidos)
     * - huboConversion: indica si se realizó conversión automática
     */
    public static class ResultadoNormalizacion {
        private final Producto productoFinal;
        private final List<ExtraPedido> extrasFiltrados;
        private final boolean huboConversion;

        public ResultadoNormalizacion(
                Producto productoFinal, 
                List<ExtraPedido> extrasFiltrados,
                boolean huboConversion
        ) {
            this.productoFinal = Objects.requireNonNull(productoFinal);
            this.extrasFiltrados = new ArrayList<>(extrasFiltrados != null ? extrasFiltrados : List.of());
            this.huboConversion = huboConversion;
        }

        public Producto getProductoFinal() {
            return productoFinal;
        }

        public List<ExtraPedido> getExtrasFiltrados() {
            return new ArrayList<>(extrasFiltrados);
        }

        public boolean isHuboConversion() {
            return huboConversion;
        }
    }
}
