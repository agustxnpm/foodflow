package com.agustinpalma.comandas.domain.service;

import com.agustinpalma.comandas.domain.model.ExtraPedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Domain Service para normalización automática de variantes de hamburguesas.
 * 
 * HU-05.1 + HU-22: Implementa la lógica de negocio crítica de conversión automática.
 * 
 * Regla de negocio fundamental:
 * El disco de carne SOLO puede agregarse como extra a la variante máxima.
 * Si se intenta agregar a una variante menor, el sistema convierte automáticamente
 * al siguiente nivel (ej: Simple + Disco → Doble).
 * 
 * Responsabilidades:
 * 1. Detectar si un extra es "disco de carne"
 * 2. Buscar variantes hermanas del mismo grupo
 * 3. Determinar la variante máxima por cantidad de discos
 * 4. Convertir automáticamente si es necesario
 * 5. Filtrar extras de disco que fueron convertidos en variante
 * 
 * Este servicio NO tiene estado. Es puro (dado un input, siempre retorna el mismo output).
 */
public class NormalizadorVariantesService {

    /**
     * Normaliza el producto seleccionado y los extras según la regla de discos de carne.
     * 
     * Lógica:
     * 1. Cuenta cuántos discos de carne hay en los extras solicitados
     * 2. Si el producto NO es hamburguesa → devuelve todo sin cambios
     * 3. Si es hamburguesa:
     *    a. Determina cuántos discos TOTALES hay (producto + extras)
     *    b. Busca la variante adecuada para esa cantidad
     *    c. Si existe una variante con exactamente esa cantidad → la usa
     *    d. Si no existe → usa la variante máxima + los discos sobrantes como extras
     * 4. Filtra los discos que fueron absorbidos por la conversión
     * 
     * @param productoSeleccionado producto inicial seleccionado por el usuario
     * @param extrasOriginal lista original de extras solicitados
     * @param variantesHermanas lista de todas las variantes del mismo grupo (incluye productoSeleccionado)
     * @param discoDeCarne producto catalogado como "disco de carne" (extra especial)
     * @return ResultadoNormalizacion con el producto final y extras finales
     */
    public ResultadoNormalizacion normalizarVariante(
            Producto productoSeleccionado,
            List<ExtraPedido> extrasOriginal,
            List<Producto> variantesHermanas,
            Producto discoDeCarne
    ) {
        Objects.requireNonNull(productoSeleccionado, "El producto seleccionado no puede ser null");
        Objects.requireNonNull(extrasOriginal, "La lista de extras no puede ser null");
        Objects.requireNonNull(discoDeCarne, "El producto disco de carne no puede ser null");
        
        // Si el producto NO es hamburguesa, no hay nada que normalizar
        if (!productoSeleccionado.esHamburguesa()) {
            return new ResultadoNormalizacion(productoSeleccionado, extrasOriginal, false);
        }
        
        // Contar cuántos discos de carne hay en los extras
        long discosEnExtras = extrasOriginal.stream()
            .filter(extra -> extra.getProductoId().equals(discoDeCarne.getId()))
            .count();
        
        // Si no hay discos en extras, no hay conversión necesaria
        if (discosEnExtras == 0) {
            return new ResultadoNormalizacion(productoSeleccionado, extrasOriginal, false);
        }
        
        // Cantidad total de discos = discos base del producto + discos en extras
        int discosTotales = productoSeleccionado.getCantidadDiscosCarne() + (int) discosEnExtras;
        
        // Buscar la variante adecuada para esa cantidad de discos
        Producto varianteFinal = buscarVarianteParaCantidad(
            discosTotales, 
            variantesHermanas, 
            productoSeleccionado
        );
        
        // Determinar cuántos discos fueron absorbidos por la variante final
        int discosAbsorbidos = varianteFinal.getCantidadDiscosCarne() - productoSeleccionado.getCantidadDiscosCarne();
        
        // Filtrar los extras: eliminar los discos absorbidos, mantener el resto
        List<ExtraPedido> extrasFiltrados = filtrarDiscosAbsorbidos(
            extrasOriginal, 
            discoDeCarne.getId(), 
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
        
        // Filtrar solo hamburguesas válidas
        List<Producto> hamburguesasValidas = variantes.stream()
            .filter(Producto::esHamburguesa)
            .collect(Collectors.toList());
        
        if (hamburguesasValidas.isEmpty()) {
            return productoOriginal;
        }
        
        // Intentar match exacto
        Producto varianteExacta = hamburguesasValidas.stream()
            .filter(v -> v.getCantidadDiscosCarne() == discosSolicitados)
            .findFirst()
            .orElse(null);
        
        if (varianteExacta != null) {
            return varianteExacta;
        }
        
        // No hay match exacto: buscar la variante máxima
        return hamburguesasValidas.stream()
            .max(Comparator.comparingInt(Producto::getCantidadDiscosCarne))
            .orElse(productoOriginal);
    }

    /**
     * Filtra los discos de carne que fueron absorbidos por la conversión de variante.
     * 
     * Ejemplo:
     * - Extras originales: [Disco, Disco, Huevo, Queso]
     * - Discos absorbidos: 2
     * - Resultado: [Huevo, Queso]
     * 
     * @param extrasOriginales lista original de extras
     * @param discoDeCarneId ID del producto "disco de carne"
     * @param cantidadAEliminar cuántos discos fueron absorbidos
     * @return nueva lista con los discos absorbidos eliminados
     */
    private List<ExtraPedido> filtrarDiscosAbsorbidos(
            List<ExtraPedido> extrasOriginales,
            ProductoId discoDeCarneId,
            int cantidadAEliminar
    ) {
        if (cantidadAEliminar == 0) {
            return new ArrayList<>(extrasOriginales);
        }
        
        List<ExtraPedido> resultado = new ArrayList<>();
        int discosEliminados = 0;
        
        for (ExtraPedido extra : extrasOriginales) {
            boolean esDisco = extra.getProductoId().equals(discoDeCarneId);
            
            if (esDisco && discosEliminados < cantidadAEliminar) {
                // Este disco fue absorbido por la conversión → NO agregarlo
                discosEliminados++;
            } else {
                // Extra normal o disco excedente → agregarlo
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
