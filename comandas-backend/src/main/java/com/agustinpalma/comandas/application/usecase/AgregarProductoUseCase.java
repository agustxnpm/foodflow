package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AgregarProductoRequest;
import com.agustinpalma.comandas.application.dto.AgregarProductoResponse;
import com.agustinpalma.comandas.domain.model.ExtraPedido;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.domain.service.MotorReglasService;
import com.agustinpalma.comandas.domain.service.NormalizadorVariantesService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para agregar productos a un pedido abierto.
 * Orquesta la lógica de aplicación y delega validaciones de negocio al dominio.
 * 
 * HU-10: Integra el MotorReglasService para aplicar promociones automáticamente.
 * HU-05.1 + HU-22: Integra normalización de variantes y extras controlados.
 * 
 * Flujo actualizado:
 * 1. Recuperar Pedido y Producto
 * 2. Procesar extras solicitados
 * 3. Normalizar variantes (conversión automática de discos de carne)
 * 4. Recuperar promociones activas del local
 * 5. Invocar MotorReglasService para evaluar y aplicar promociones
 * 6. Agregar el ítem resultante (con extras y descuentos) al pedido
 * 7. Persistir cambios
 */
@Transactional
public class AgregarProductoUseCase {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;
    private final MotorReglasService motorReglasService;
    private final NormalizadorVariantesService normalizadorVariantesService;
    private final Clock clock;

    /**
     * Constructor con inyección de dependencias.
     * El framework de infraestructura (Spring) se encargará de proveer las implementaciones.
     *
     * @param pedidoRepository repositorio de pedidos
     * @param productoRepository repositorio de productos
     * @param promocionRepository repositorio de promociones (HU-10)
     * @param motorReglasService servicio de dominio para evaluar promociones (HU-10)
     * @param normalizadorVariantesService servicio de dominio para normalizar variantes (HU-22)
     * @param clock reloj del sistema configurado para zona horaria de Argentina
     */
    public AgregarProductoUseCase(
            PedidoRepository pedidoRepository, 
            ProductoRepository productoRepository,
            PromocionRepository promocionRepository,
            MotorReglasService motorReglasService,
            NormalizadorVariantesService normalizadorVariantesService,
            Clock clock
    ) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
        this.productoRepository = Objects.requireNonNull(productoRepository, "El productoRepository es obligatorio");
        this.promocionRepository = Objects.requireNonNull(promocionRepository, "El promocionRepository es obligatorio");
        this.motorReglasService = Objects.requireNonNull(motorReglasService, "El motorReglasService es obligatorio");
        this.normalizadorVariantesService = Objects.requireNonNull(normalizadorVariantesService, "El normalizadorVariantesService es obligatorio");
        this.clock = Objects.requireNonNull(clock, "El clock es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: agregar un producto a un pedido con extras y aplicación automática de promociones.
     * 
     * HU-10 - Flujo con Motor de Reglas
     * HU-05.1 + HU-22 - Normalización de variantes y extras controlados
     * 
     * Flujo completo:
     * 1. Recuperar Pedido por ID (falla si no existe)
     * 2. Recuperar Producto por ID (falla si no existe)
     * 3. Procesar extras solicitados (convertir IDs a ExtraPedido)
     * 4. Normalizar variantes (conversión automática si hay discos de carne)
     * 5. Recuperar promociones activas del local
     * 6. Invocar MotorReglasService para evaluar promociones (SOLO sobre precio base)
     * 7. Agregar el ítem resultante al pedido (con extras y descuentos)
     * 8. Persistir cambios
     * 
     * CRÍTICO: Las promociones SOLO aplican sobre el precio base del producto,
     * NUNCA sobre los extras (aislamiento de descuentos).
     *
     * @param request DTO con pedidoId, productoId, cantidad, observaciones y extrasIds
     * @return DTO con el pedido actualizado
     * @throws IllegalArgumentException si el pedido no existe, si el producto no existe, o si el producto pertenece a otro local
     * @throws IllegalStateException si el pedido no está ABIERTO (delegado al dominio)
     */
    public AgregarProductoResponse ejecutar(AgregarProductoRequest request) {
        Objects.requireNonNull(request, "El request no puede ser null");

        // 1. Recuperar Pedido
        Pedido pedido = pedidoRepository.buscarPorId(request.pedidoId())
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el pedido con ID: " + request.pedidoId().getValue()
            ));

        // 2. Recuperar Producto seleccionado
        Producto productoSeleccionado = productoRepository.buscarPorId(request.productoId())
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el producto con ID: " + request.productoId().getValue()
            ));

        // 2.1 Selección explícita de variante: si varianteId viene informado,
        // el cliente ya eligió la variante concreta → se usa directamente.
        // Esto cortocircuita la auto-normalización posterior.
        boolean varianteExplicita = false;
        if (request.varianteId() != null) {
            Producto variante = productoRepository.buscarPorId(request.varianteId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "No se encontró la variante con ID: " + request.varianteId().getValue()
                ));

            // Validar que la variante pertenece al mismo grupo que el producto base
            if (variante.getGrupoVarianteId() == null) {
                throw new IllegalArgumentException(
                    "El producto '" + variante.getNombre() + "' no es una variante (no tiene grupoVarianteId)"
                );
            }
            ProductoId grupoEsperado = productoSeleccionado.getGrupoVarianteId() != null
                ? productoSeleccionado.getGrupoVarianteId()
                : productoSeleccionado.getId();
            if (!variante.getGrupoVarianteId().equals(grupoEsperado)) {
                throw new IllegalArgumentException(
                    String.format("La variante '%s' no pertenece al grupo del producto '%s'",
                        variante.getNombre(), productoSeleccionado.getNombre())
                );
            }

            productoSeleccionado = variante;
            varianteExplicita = true;
        }

        // Validación: un producto marcado como extra NO puede agregarse como línea independiente
        if (productoSeleccionado.isEsExtra()) {
            throw new IllegalArgumentException(
                String.format("El producto '%s' es un extra y no puede agregarse como línea independiente. " +
                    "Selecciónelo desde la configuración del ítem.", productoSeleccionado.getNombre())
            );
        }

        // Validación multi-tenancy temprana
        if (!pedido.getLocalId().equals(productoSeleccionado.getLocalId())) {
            throw new IllegalArgumentException(
                String.format("El producto (local: %s) no pertenece al mismo local que el pedido (local: %s)", 
                    productoSeleccionado.getLocalId().getValue(), 
                    pedido.getLocalId().getValue())
            );
        }

        // 3. Procesar extras solicitados (convertir ProductoId → ExtraPedido con snapshot)
        List<ExtraPedido> extrasOriginales = procesarExtras(request.extrasIds(), pedido.getLocalId());

        // 4. HU-22: Normalizar variantes (conversión automática de discos de carne)
        // Si el cliente ya seleccionó una variante explícita, se salta la normalización.
        NormalizadorVariantesService.ResultadoNormalizacion normalizacion;
        if (varianteExplicita) {
            normalizacion = new NormalizadorVariantesService.ResultadoNormalizacion(
                productoSeleccionado,
                extrasOriginales,
                false
            );
        } else {
            normalizacion = normalizarVariantes(
                productoSeleccionado,
                extrasOriginales,
                pedido.getLocalId()
            );
        }

        Producto productoFinal = normalizacion.getProductoFinal();
        List<ExtraPedido> extrasFiltrados = normalizacion.getExtrasFiltrados();

        // 4.2 Validación de modificadores estructurales como extras.
        //
        // Regla dinámica: Si después de la normalización todavía quedan extras
        // que son modificadores estructurales, validar que el producto final
        // esté en la variante máxima de su grupo.
        //
        // Si cantidadDiscosActual < maxEstructural → la variante debería escalarse,
        // no agregar el modificador como extra suelto.
        validarModificadoresEstructuralesComoExtras(productoFinal, extrasFiltrados, pedido.getLocalId());

        // 4.3 MERGE INTELIGENTE: Solo fusionar si la CONFIGURACIÓN es idéntica.
        //
        // Regla de negocio: Cada plato personalizado es una unidad independiente.
        // Dos ítems son fusionables SOLO si comparten:
        //   - Mismo productoId (post-normalización)
        //   - Misma observación
        //   - Mismos extras
        //
        // Si no coinciden → crear nuevo ItemPedido (línea independiente).
        // Esto evita el bug donde "Hamburguesa" + "Hamburguesa sin cebolla"
        // se fusionaban incorrectamente en "2x Hamburguesa sin cebolla".
        int cantidadFinal = request.cantidad();
        String observacionesFinal = request.observaciones();
        List<ExtraPedido> extrasCombinados = new ArrayList<>(extrasFiltrados);

        Optional<ItemPedido> itemExistente = pedido.buscarItemConMismaConfiguracion(
            productoFinal.getId(), observacionesFinal, extrasFiltrados
        );

        if (itemExistente.isPresent()) {
            ItemPedido existente = itemExistente.get();
            // Configuración idéntica → solo acumular cantidad
            cantidadFinal += existente.getCantidad();
            // Observaciones y extras ya son iguales, no hace falta merge
            
            // Remover ítem existente (será reemplazado con cantidad acumulada)
            pedido.eliminarItem(existente.getId());
        }

        // 5. Recuperar promociones activas del local
        List<Promocion> promocionesActivas = promocionRepository.buscarActivasPorLocal(pedido.getLocalId());

        // 6. HU-10: Invocar motor de reglas para evaluar promociones
        // CRÍTICO: El motor SOLO descuenta sobre precio base, NO sobre extras
        // Usa cantidadFinal y extrasCombinados para que las promos calculen sobre el acumulado
        ItemPedido itemConPromocion = motorReglasService.aplicarReglasConExtras(
            pedido,
            productoFinal,
            cantidadFinal,
            observacionesFinal,
            extrasCombinados,
            promocionesActivas,
            LocalDateTime.now(clock)
        );

        // 7. Agregar ítem al pedido (con extras y descuentos aplicados)
        pedido.agregarItem(itemConPromocion);

        // 8. Recalcular promociones de TODO el pedido (agregación cross-línea)
        // Necesario porque ítems del mismo producto en diferentes líneas
        // (ej: "hamburguesa" y "hamburguesa sin cebolla") deben sumar cantidades
        // para evaluar promos basadas en cantidad (2x1, PrecioFijo).
        pedido.limpiarPromocionesItems();
        motorReglasService.aplicarPromociones(pedido, promocionesActivas, LocalDateTime.now(clock));

        // 9. Persistir cambios
        Pedido pedidoActualizado = pedidoRepository.guardar(pedido);

        return AgregarProductoResponse.fromDomain(pedidoActualizado);
    }

    /**
     * Procesa los IDs de extras solicitados y los convierte a ExtraPedido con snapshot de precio.
     * 
     * Validaciones:
     * - Cada extra debe existir en el catálogo
     * - Cada extra debe estar marcado como esExtra = true
     * - Cada extra debe pertenecer al mismo local
     * 
     * @param extrasIds lista de ProductoId de extras solicitados (puede ser null)
     * @param localId ID del local para validación multi-tenancy
     * @return lista de ExtraPedido con snapshot de precio
     * @throws IllegalArgumentException si algún extra no existe, no es extra, o pertenece a otro local
     */
    private List<ExtraPedido> procesarExtras(List<ProductoId> extrasIds, com.agustinpalma.comandas.domain.model.DomainIds.LocalId localId) {
        if (extrasIds == null || extrasIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExtraPedido> extras = new ArrayList<>();

        for (ProductoId extraId : extrasIds) {
            Producto productoExtra = productoRepository.buscarPorId(extraId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "No se encontró el extra con ID: " + extraId.getValue()
                ));

            // Validar multi-tenancy
            if (!productoExtra.getLocalId().equals(localId)) {
                throw new IllegalArgumentException(
                    String.format("El extra '%s' no pertenece al local del pedido", productoExtra.getNombre())
                );
            }

            // Validar que esté marcado como extra
            if (!productoExtra.isEsExtra()) {
                throw new IllegalArgumentException(
                    String.format("El producto '%s' no está marcado como extra", productoExtra.getNombre())
                );
            }

            // Crear ExtraPedido con snapshot de precio actual
            extras.add(ExtraPedido.crearDesdeProducto(productoExtra));
        }

        return extras;
    }

    /**
     * Normaliza las variantes según la regla de modificadores estructurales.
     * 
     * Si el producto tiene grupo de variantes y algún extra es un modificador estructural,
     * el NormalizadorVariantesService convierte automáticamente a la variante adecuada.
     * 
     * Los modificadores estructurales se identifican por su flag, NO por nombre literal.
     * 
     * @param productoSeleccionado producto inicial seleccionado
     * @param extrasOriginales extras solicitados
     * @param localId ID del local
     * @return ResultadoNormalizacion con producto final y extras filtrados
     */
    private NormalizadorVariantesService.ResultadoNormalizacion normalizarVariantes(
            Producto productoSeleccionado,
            List<ExtraPedido> extrasOriginales,
            com.agustinpalma.comandas.domain.model.DomainIds.LocalId localId
    ) {
        // Si el producto NO tiene grupo de variantes, no hay nada que normalizar
        if (productoSeleccionado.getGrupoVarianteId() == null) {
            return new NormalizadorVariantesService.ResultadoNormalizacion(
                productoSeleccionado, 
                extrasOriginales, 
                false
            );
        }

        // Buscar todas las variantes hermanas del mismo grupo
        List<Producto> variantesHermanas = productoRepository.buscarPorGrupoVariante(
            localId,
            productoSeleccionado.getGrupoVarianteId()
        );

        // Buscar modificadores estructurales del local (ej: disco de carne)
        // Se identifican por flag esModificadorEstructural = true, NO por nombre.
        List<Producto> modificadores = productoRepository.buscarModificadoresEstructurales(localId);

        // Si no hay modificadores estructurales catalogados, no hay normalización posible
        if (modificadores.isEmpty()) {
            return new NormalizadorVariantesService.ResultadoNormalizacion(
                productoSeleccionado, 
                extrasOriginales, 
                false
            );
        }

        // Extraer IDs para delegar al Domain Service
        java.util.Set<ProductoId> idsModificadores = modificadores.stream()
            .map(Producto::getId)
            .collect(Collectors.toSet());

        // Delegar normalización al Domain Service
        return normalizadorVariantesService.normalizarVariante(
            productoSeleccionado,
            extrasOriginales,
            variantesHermanas,
            idsModificadores
        );
    }


    /**
     * Valida que no haya modificadores estructurales como extras cuando
     * el producto todavía puede escalarse a una variante superior.
     * 
     * Regla dinámica:
     *   Sea maxEstructural = obtenerMaximaCantidadDiscos(localId, grupoVarianteId)
     *   puedeAgregarDiscoExtra = (cantidadDiscosActual == maxEstructural)
     * 
     * Si cantidadDiscosActual < maxEstructural y hay un modificador estructural
     * entre los extras (que no fue absorbido por la normalización),
     * se lanza excepción de dominio.
     * 
     * @param productoFinal producto después de normalización
     * @param extrasFiltrados extras después de normalización
     * @param localId ID del local para consultar maxEstructural
     * @throws DiscoExtraNoPermitidoException si hay modificadores y se puede escalar
     */
    private void validarModificadoresEstructuralesComoExtras(
            Producto productoFinal,
            List<ExtraPedido> extrasFiltrados,
            com.agustinpalma.comandas.domain.model.DomainIds.LocalId localId
    ) {
        // Si no tiene grupo de variantes, no aplica la restricción
        if (productoFinal.getGrupoVarianteId() == null) return;

        // Si no hay extras, nada que validar
        if (extrasFiltrados == null || extrasFiltrados.isEmpty()) return;

        // Buscar modificadores estructurales del local
        List<Producto> modificadores = productoRepository.buscarModificadoresEstructurales(localId);
        if (modificadores.isEmpty()) return;

        java.util.Set<ProductoId> idsModificadores = modificadores.stream()
            .map(Producto::getId)
            .collect(Collectors.toSet());

        // Verificar si hay modificadores entre los extras filtrados
        boolean tieneModificadorEnExtras = extrasFiltrados.stream()
            .anyMatch(extra -> idsModificadores.contains(extra.getProductoId()));

        if (!tieneModificadorEnExtras) return;

        // Hay modificadores como extras → validar que el producto esté en su máximo
        int maxEstructural = productoRepository.obtenerMaximaCantidadDiscos(
            localId, productoFinal.getGrupoVarianteId()
        );
        int discosActuales = productoFinal.getCantidadDiscosCarne() != null
            ? productoFinal.getCantidadDiscosCarne() : 0;

        if (discosActuales < maxEstructural) {
            throw new IllegalArgumentException(
                String.format(
                    "No se puede agregar un modificador estructural como extra al producto '%s' " +
                    "porque no es la variante máxima del grupo (discos: %d/%d). " +
                    "Seleccione la variante máxima para agregar este extra.",
                    productoFinal.getNombre(), discosActuales, maxEstructural)
            );
        }
        // discosActuales == maxEstructural → OK
    }
}
