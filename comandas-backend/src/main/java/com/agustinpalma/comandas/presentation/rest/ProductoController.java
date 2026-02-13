package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.ProductoRequest;
import com.agustinpalma.comandas.application.dto.ProductoResponse;
import com.agustinpalma.comandas.application.usecase.ConsultarProductosUseCase;
import com.agustinpalma.comandas.application.usecase.CrearProductoUseCase;
import com.agustinpalma.comandas.application.usecase.EditarProductoUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarProductoUseCase;
import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para operaciones sobre productos del catálogo.
 * Expone endpoints HTTP y transforma requests/responses.
 * No contiene lógica de negocio, solo coordina entre HTTP y casos de uso.
 * 
 * Endpoints:
 * - GET    /api/productos          -> Listar productos (con filtro opcional por color)
 * - POST   /api/productos          -> Crear producto
 * - PUT    /api/productos/{id}     -> Editar producto
 * - DELETE /api/productos/{id}     -> Eliminar producto
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final LocalContextProvider localContextProvider;
    private final ConsultarProductosUseCase consultarProductosUseCase;
    private final CrearProductoUseCase crearProductoUseCase;
    private final EditarProductoUseCase editarProductoUseCase;
    private final EliminarProductoUseCase eliminarProductoUseCase;

    public ProductoController(
        LocalContextProvider localContextProvider,
        ConsultarProductosUseCase consultarProductosUseCase,
        CrearProductoUseCase crearProductoUseCase,
        EditarProductoUseCase editarProductoUseCase,
        EliminarProductoUseCase eliminarProductoUseCase
    ) {
        this.localContextProvider = localContextProvider;
        this.consultarProductosUseCase = consultarProductosUseCase;
        this.crearProductoUseCase = crearProductoUseCase;
        this.editarProductoUseCase = editarProductoUseCase;
        this.eliminarProductoUseCase = eliminarProductoUseCase;
    }

    /**
     * Obtiene todos los productos del local actual.
     * Soporta filtrado opcional por color hexadecimal.
     *
     * GET /api/productos
     * GET /api/productos?color=%23FF0000
     *
     * TODO: Implementar autenticación/autorización para obtener el localId del usuario logueado.
     *       Por ahora se usa un localId hardcodeado para permitir testing del endpoint.
     *
     * @param color código hexadecimal de color para filtrar (opcional)
     * @return lista de productos (puede estar vacía)
     */
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listarProductos(
        @RequestParam(required = false) String color
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        List<ProductoResponse> productos = consultarProductosUseCase.ejecutar(localId, color);

        return ResponseEntity.ok(productos);
    }

    /**
     * Crea un nuevo producto en el catálogo.
     *
     * POST /api/productos
     * Body: { "nombre": "Hamburguesa", "precio": 1500.00, "activo": true, "colorHex": "#FF0000" }
     *
     * TODO: Implementar autenticación/autorización para obtener el localId del usuario logueado.
     * TODO: Implementar ControllerAdvice para manejo centralizado de excepciones.
     *
     * @param request datos del producto a crear
     * @return producto creado con status 201 CREATED
     */
    @PostMapping
    public ResponseEntity<ProductoResponse> crearProducto(@Valid @RequestBody ProductoRequest request) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        ProductoResponse producto = crearProductoUseCase.ejecutar(localId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(producto);
    }

    /**
     * Edita un producto existente.
     *
     * PUT /api/productos/{id}
     * Body: { "nombre": "Hamburguesa Completa", "precio": 1800.00, "activo": true, "colorHex": "#FF5500" }
     *
     * TODO: Implementar autenticación/autorización para obtener el localId del usuario logueado.
     * TODO: Implementar ControllerAdvice para manejo centralizado de excepciones.
     *
     * @param id UUID del producto a editar
     * @param request nuevos datos del producto
     * @return producto actualizado con status 200 OK
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> editarProducto(
        @PathVariable UUID id,
        @Valid @RequestBody ProductoRequest request
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();
        ProductoId productoId = new ProductoId(id);

        ProductoResponse producto = editarProductoUseCase.ejecutar(productoId, localId, request);
        return ResponseEntity.ok(producto);
    }

    /**
     * Elimina un producto del catálogo.
     *
     * DELETE /api/productos/{id}
     *
     * IMPORTANTE: Si el producto está vinculado a pedidos activos,
     * la base de datos rechazará la operación por integridad referencial.
     *
     * TODO: Implementar autenticación/autorización para obtener el localId del usuario logueado.
     * TODO: Implementar ControllerAdvice para manejo centralizado de excepciones.
     *
     * @param id UUID del producto a eliminar
     * @return 204 NO CONTENT si se eliminó correctamente
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable UUID id) {
        LocalId localId = localContextProvider.getCurrentLocalId();
        ProductoId productoId = new ProductoId(id);

        eliminarProductoUseCase.ejecutar(productoId, localId);
        return ResponseEntity.noContent().build();
    }
}
