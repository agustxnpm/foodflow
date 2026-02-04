package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AgregarProductoRequest;
import com.agustinpalma.comandas.application.dto.AgregarProductoResponse;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;

import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para agregar productos a un pedido abierto.
 * Orquesta la lógica de aplicación y delega validaciones de negocio al dominio.
 */
@Transactional
public class AgregarProductoUseCase {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    /**
     * Constructor con inyección de dependencias.
     * El framework de infraestructura (Spring) se encargará de proveer las implementaciones.
     *
     * @param pedidoRepository repositorio de pedidos
     * @param productoRepository repositorio de productos
     */
    public AgregarProductoUseCase(PedidoRepository pedidoRepository, ProductoRepository productoRepository) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
        this.productoRepository = Objects.requireNonNull(productoRepository, "El productoRepository es obligatorio");
    }

    /**
     * Ejecuta el caso de uso: agregar un producto a un pedido.
     * 
     * Flujo:
     * 1. Recuperar Pedido por ID (falla si no existe)
     * 2. Recuperar Producto por ID (falla si no existe)
     * 3. Ejecutar pedido.agregarProducto() - las validaciones ocurren en el dominio
     * 4. Persistir Pedido con el nuevo ítem
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

        // 3. Agregar producto al pedido (las validaciones de negocio ocurren aquí)
        // - AC4: Validación de estado ABIERTO
        // - AC5: Validación multi-tenancy (mismo local)
        // - AC3: Snapshot del precio
        pedido.agregarProducto(producto, request.cantidad(), request.observaciones());

        // 4. Persistir cambios
        Pedido pedidoActualizado = pedidoRepository.guardar(pedido);

        return AgregarProductoResponse.fromDomain(pedidoActualizado);
    }
}
