package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.AjustarStockRequest;
import com.agustinpalma.comandas.application.dto.AjustarStockResponse;
import com.agustinpalma.comandas.application.dto.ProductoRequest;
import com.agustinpalma.comandas.application.dto.ProductoResponse;
import com.agustinpalma.comandas.application.dto.StockAjusteRequestBody;
import com.agustinpalma.comandas.application.usecase.AjustarStockUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarProductosUseCase;
import com.agustinpalma.comandas.application.usecase.CrearProductoUseCase;
import com.agustinpalma.comandas.application.usecase.EditarProductoUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarProductoUseCase;
import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
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
    private final ProductoRepository productoRepository;
    private final ConsultarProductosUseCase consultarProductosUseCase;
    private final CrearProductoUseCase crearProductoUseCase;
    private final EditarProductoUseCase editarProductoUseCase;
    private final EliminarProductoUseCase eliminarProductoUseCase;
    private final AjustarStockUseCase ajustarStockUseCase;

    public ProductoController(
        LocalContextProvider localContextProvider,
        ProductoRepository productoRepository,
        ConsultarProductosUseCase consultarProductosUseCase,
        CrearProductoUseCase crearProductoUseCase,
        EditarProductoUseCase editarProductoUseCase,
        EliminarProductoUseCase eliminarProductoUseCase,
        AjustarStockUseCase ajustarStockUseCase
    ) {
        this.localContextProvider = localContextProvider;
        this.productoRepository = productoRepository;
        this.consultarProductosUseCase = consultarProductosUseCase;
        this.crearProductoUseCase = crearProductoUseCase;
        this.editarProductoUseCase = editarProductoUseCase;
        this.eliminarProductoUseCase = eliminarProductoUseCase;
        this.ajustarStockUseCase = ajustarStockUseCase;
    }

    /**
     * Obtiene todos los productos del local actual.
     * Soporta filtrado opcional por color hexadecimal y/o categoría.
     *
     * GET /api/productos
     * GET /api/productos?color=%23FF0000
     * GET /api/productos?categoria=bebida
     *
     * TODO: Implementar autenticación/autorización para obtener el localId del usuario logueado.
     *       Por ahora se usa un localId hardcodeado para permitir testing del endpoint.
     *
     * @param color código hexadecimal de color para filtrar (opcional)
     * @param categoria etiqueta de categoría para filtrar (opcional)
     * @return lista de productos (puede estar vacía)
     */
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listarProductos(
        @RequestParam(required = false) String color,
        @RequestParam(required = false) String categoria
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();

        List<ProductoResponse> productos = consultarProductosUseCase.ejecutar(localId, color, categoria);

        return ResponseEntity.ok(productos);
    }

    /**
     * Consulta un producto específico por ID.
     *
     * GET /api/productos/{id}
     *
     * @param id UUID del producto a consultar
     * @return producto encontrado con status 200 OK
     * @throws IllegalArgumentException si el producto no existe (se traduce a 400 por GlobalExceptionHandler)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> consultarProducto(@PathVariable UUID id) {
        LocalId localId = localContextProvider.getCurrentLocalId();
        ProductoId productoId = new ProductoId(id);

        Producto producto = productoRepository.buscarPorIdYLocal(productoId, localId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Producto no encontrado con ID: " + id + " en el local actual"
            ));

        return ResponseEntity.ok(ProductoResponse.fromDomain(producto));
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

    /**
     * Ajusta el stock de un producto (ajuste manual o ingreso de mercadería).
     *
     * PATCH /api/productos/{id}/stock
     * Body: {
     *   "cantidad": 10,
     *   "tipo": "AJUSTE_MANUAL" | "INGRESO_MERCADERIA",
     *   "motivo": "Reposición desde depósito"
     * }
     *
     * HU-22: Stock Management
     *
     * @param id UUID del producto
     * @param body datos del ajuste de stock
     * @return respuesta con stock actualizado
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<AjustarStockResponse> ajustarStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockAjusteRequestBody body
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();
        ProductoId productoId = new ProductoId(id);

        AjustarStockRequest request = new AjustarStockRequest(
            productoId,
            body.cantidad(),
            body.tipo(),
            body.motivo()
        );

        AjustarStockResponse response = ajustarStockUseCase.ejecutar(localId, request);
        return ResponseEntity.ok(response);
    }
}
