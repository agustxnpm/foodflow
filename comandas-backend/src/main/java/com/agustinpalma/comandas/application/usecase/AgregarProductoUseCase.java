package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AgregarProductoRequest;
import com.agustinpalma.comandas.application.dto.AgregarProductoResponse;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.domain.service.MotorReglasService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para agregar productos a un pedido abierto.
 * Orquesta la lógica de aplicación y delega validaciones de negocio al dominio.
 * 
 * HU-10: Integra el MotorReglasService para aplicar promociones automáticamente.
 * 
 * Flujo actualizado:
 * 1. Recuperar Pedido y Producto
 * 2. Recuperar promociones activas del local
 * 3. Invocar MotorReglasService para evaluar y aplicar promociones
 * 4. Agregar el ítem resultante (con o sin descuento) al pedido
 * 5. Persistir cambios
 */
@Transactional
public class AgregarProductoUseCase {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;
    private final MotorReglasService motorReglasService;
    private final Clock clock;

    /**
     * Constructor con inyección de dependencias.
     * El framework de infraestructura (Spring) se encargará de proveer las implementaciones.
     *
     * @param pedidoRepository repositorio de pedidos
     * @param productoRepository repositorio de productos
     * @param promocionRepository repositorio de promociones (HU-10)
     * @param motorReglasService servicio de dominio para evaluar promociones (HU-10)
     * @param clock reloj del sistema configurado para zona horaria de Argentina
     */
    public AgregarProductoUseCase(
            PedidoRepository pedidoRepository, 
            ProductoRepository productoRepository,
            PromocionRepository promocionRepository,
            MotorReglasService motorReglasService,
            Clock clock
    ) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
        this.productoRepository = Objects.requireNonNull(productoRepository, "El productoRepository es obligatorio");
        this.promocionRepository = Objects.requireNonNull(promocionRepository, "El promocionRepository es obligatorio");
        this.motorReglasService = Objects.requireNonNull(motorReglasService, "El motorReglasService es obligatorio");
        this.clock = Objects.requireNonNull(clock, "El clock es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: agregar un producto a un pedido con aplicación automática de promociones.
     * 
     * HU-10 - Flujo con Motor de Reglas:
     * 1. Recuperar Pedido por ID (falla si no existe)
     * 2. Recuperar Producto por ID (falla si no existe)
     * 3. Recuperar promociones activas del local
     * 4. Invocar MotorReglasService.aplicarReglas() para evaluar promociones
     * 5. Agregar el ítem resultante al pedido (ya tiene el descuento snapshot si aplica)
     * 6. Persistir cambios
     * 
     * NOTA: La validación de multi-tenancy (producto del mismo local) se realiza dentro del dominio.
     *
     * @param request DTO con pedidoId, productoId, cantidad y observaciones
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

        // 2. Recuperar Producto
        Producto producto = productoRepository.buscarPorId(request.productoId())
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el producto con ID: " + request.productoId().getValue()
            ));

        // Validación multi-tenancy temprana
        if (!pedido.getLocalId().equals(producto.getLocalId())) {
            throw new IllegalArgumentException(
                String.format("El producto (local: %s) no pertenece al mismo local que el pedido (local: %s)", 
                    producto.getLocalId().getValue(), 
                    pedido.getLocalId().getValue())
            );
        }

        // 3. Recuperar promociones activas del local
        List<Promocion> promocionesActivas = promocionRepository.buscarActivasPorLocal(pedido.getLocalId());

        // 4. HU-10: Invocar motor de reglas para evaluar promociones
        // El motor devuelve un ItemPedido ya construido con el descuento (si aplica)
        // Usa el clock inyectado (configurado para zona horaria de Argentina)
        ItemPedido itemConPromocion = motorReglasService.aplicarReglas(
            pedido,
            producto,
            request.cantidad(),
            request.observaciones(),
            promocionesActivas,
            LocalDateTime.now(clock)
        );

        // 5. Agregar ítem al pedido (el ítem ya tiene el snapshot de precio y promoción)
        pedido.agregarItem(itemConPromocion);

        // 6. Persistir cambios
        Pedido pedidoActualizado = pedidoRepository.guardar(pedido);

        return AgregarProductoResponse.fromDomain(pedidoActualizado);
    }
}
