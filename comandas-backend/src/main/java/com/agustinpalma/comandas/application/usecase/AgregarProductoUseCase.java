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
        NormalizadorVariantesService.ResultadoNormalizacion normalizacion = normalizarVariantes(
            productoSeleccionado,
            extrasOriginales,
            pedido.getLocalId()
        );

        Producto productoFinal = normalizacion.getProductoFinal();
        List<ExtraPedido> extrasFiltrados = normalizacion.getExtrasFiltrados();

        // 5. Recuperar promociones activas del local
        List<Promocion> promocionesActivas = promocionRepository.buscarActivasPorLocal(pedido.getLocalId());

        // 6. HU-10: Invocar motor de reglas para evaluar promociones
        // CRÍTICO: El motor SOLO descuenta sobre precio base, NO sobre extras
        ItemPedido itemConPromocion = motorReglasService.aplicarReglasConExtras(
            pedido,
            productoFinal,
            request.cantidad(),
            request.observaciones(),
            extrasFiltrados,
            promocionesActivas,
            LocalDateTime.now(clock)
        );

        // 7. Agregar ítem al pedido (con extras y descuentos aplicados)
        pedido.agregarItem(itemConPromocion);

        // 8. Persistir cambios
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
     * Normaliza las variantes según la regla de discos de carne.
     * 
     * HU-22: Si el producto es hamburguesa y hay discos de carne en los extras,
     * convierte automáticamente a la variante adecuada.
     * 
     * Lógica delegada al NormalizadorVariantesService (Domain Service).
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

        // Buscar el producto "disco de carne" del local
        // NOTA: Esto asume que existe un producto catalogado como "disco de carne extra"
        // con esExtra = true. Si no existe, la normalización no se aplicará.
        Producto discoDeCarne = productoRepository.buscarExtraDiscoDeCarne(localId)
            .orElse(null);

        // Si no hay disco de carne catalogado, no hay normalización posible
        if (discoDeCarne == null) {
            return new NormalizadorVariantesService.ResultadoNormalizacion(
                productoSeleccionado, 
                extrasOriginales, 
                false
            );
        }

        // Delegar normalización al Domain Service
        return normalizadorVariantesService.normalizarVariante(
            productoSeleccionado,
            extrasOriginales,
            variantesHermanas,
            discoDeCarne
        );
    }
}
