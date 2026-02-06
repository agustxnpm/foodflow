package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.AsociarScopeCommand;
import com.agustinpalma.comandas.application.dto.CrearPromocionCommand;
import com.agustinpalma.comandas.application.dto.EditarPromocionCommand;
import com.agustinpalma.comandas.application.dto.PromocionResponse;
import com.agustinpalma.comandas.application.usecase.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPromocion;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PromocionId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el dominio de Promociones.
 * 
 * Endpoints:
 * - GET /api/promociones: Listar todas las promociones del local
 * - GET /api/promociones/{id}: Obtener detalle de una promoción
 * - POST /api/promociones: Crear una nueva promoción con triggers configurables
 * - PUT /api/promociones/{id}: Actualizar una promoción existente
 * - DELETE /api/promociones/{id}: Eliminar (desactivar) una promoción
 * - PUT /api/promociones/{id}/alcance: Asociar productos a la promoción (HU-09)
 * 
 */
@RestController
@RequestMapping("/api/promociones")
public class PromocionController {

    private final CrearPromocionUseCase crearPromocionUseCase;
    private final ConsultarPromocionesUseCase consultarPromocionesUseCase;
    private final ConsultarPromocionUseCase consultarPromocionUseCase;
    private final EditarPromocionUseCase editarPromocionUseCase;
    private final EliminarPromocionUseCase eliminarPromocionUseCase;
    private final AsociarProductoAPromocionUseCase asociarProductoAPromocionUseCase;

    public PromocionController(
            CrearPromocionUseCase crearPromocionUseCase,
            ConsultarPromocionesUseCase consultarPromocionesUseCase,
            ConsultarPromocionUseCase consultarPromocionUseCase,
            EditarPromocionUseCase editarPromocionUseCase,
            EliminarPromocionUseCase eliminarPromocionUseCase,
            AsociarProductoAPromocionUseCase asociarProductoAPromocionUseCase
    ) {
        this.crearPromocionUseCase = crearPromocionUseCase;
        this.consultarPromocionesUseCase = consultarPromocionesUseCase;
        this.consultarPromocionUseCase = consultarPromocionUseCase;
        this.editarPromocionUseCase = editarPromocionUseCase;
        this.eliminarPromocionUseCase = eliminarPromocionUseCase;
        this.asociarProductoAPromocionUseCase = asociarProductoAPromocionUseCase;
    }

    /**
     * Listar todas las promociones del local.
     * 
     * Query params opcionales:
     * - estado: Filtra por ACTIVA o INACTIVA
     * 
     * @return Lista de promociones del local autenticado
     */
    @GetMapping
    public ResponseEntity<List<PromocionResponse>> listarPromociones(
            @RequestParam(required = false) String estado
    ) {
        // TODO: Reemplazar por localId del contexto de autenticación cuando se implemente JWT
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        
        List<PromocionResponse> promociones;
        if (estado != null) {
            EstadoPromocion estadoEnum = EstadoPromocion.valueOf(estado);
            promociones = consultarPromocionesUseCase.ejecutarPorEstado(localIdSimulado, estadoEnum);
        } else {
            promociones = consultarPromocionesUseCase.ejecutar(localIdSimulado);
        }
        
        return ResponseEntity.ok(promociones);
    }

    /**
     * Obtener el detalle de una promoción específica.
     * 
     * Seguridad: Valida que la promoción pertenezca al local autenticado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PromocionResponse> obtenerPromocion(@PathVariable String id) {
        // TODO: Reemplazar por localId del contexto de autenticación cuando se implemente JWT
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        PromocionId promocionId = new PromocionId(UUID.fromString(id));
        
        PromocionResponse response = consultarPromocionUseCase.ejecutar(localIdSimulado, promocionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Crea una nueva promoción para el local del usuario autenticado.
     * 
     * Valida:
     * - Unicidad del nombre en el local
     * - Parámetros de la estrategia según el tipo
     * - Al menos un trigger configurado
     * - Campos obligatorios según tipo de trigger
     */
    @PostMapping
    public ResponseEntity<PromocionResponse> crearPromocion(@Valid @RequestBody CrearPromocionCommand command) {
        // TODO: Reemplazar por localId del contexto de autenticación cuando se implemente JWT
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        PromocionResponse response = crearPromocionUseCase.ejecutar(localIdSimulado, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza una promoción existente.
     * 
     * Permite actualización parcial de nombre, descripción, prioridad.
     * Los triggers se reemplazan completamente si se especifican.
     * 
     * Seguridad: Valida que la promoción pertenezca al local autenticado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PromocionResponse> editarPromocion(
            @PathVariable String id,
            @Valid @RequestBody EditarPromocionCommand command
    ) {
                
        // TODO: Reemplazar por localId del contexto de autenticación cuando se implemente JWT
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        PromocionId promocionId = new PromocionId(UUID.fromString(id));
        
        PromocionResponse response = editarPromocionUseCase.ejecutar(localIdSimulado, promocionId, command);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina (desactiva) una promoción.
     * 
     * Implementa soft delete: marca la promoción como INACTIVA sin borrarla
     * físicamente, preservando el histórico de pedidos que la aplicaron.
     * 
     * Seguridad: Valida que la promoción pertenezca al local autenticado.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPromocion(@PathVariable String id) {
        // TODO: Reemplazar por localId del contexto de autenticación cuando se implemente JWT        
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        PromocionId promocionId = new PromocionId(UUID.fromString(id));
        
        eliminarPromocionUseCase.ejecutar(localIdSimulado, promocionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Asocia productos/categorías a una promoción, definiendo su alcance (scope).
     * HU-09: Asociar productos a promociones.
     * 
     * Define:
     * - Qué productos/categorías ACTIVAN la promoción (rol TRIGGER)
     * - Qué productos/categorías RECIBEN el beneficio (rol TARGET)
     * 
     * Ejemplo de request (Caso Torta + Licuado):
     * {
     *   "items": [
     *     { "referenciaId": "uuid-torta-chocolate", "tipo": "PRODUCTO", "rol": "TRIGGER" },
     *     { "referenciaId": "uuid-cat-licuados", "tipo": "CATEGORIA", "rol": "TARGET" },
     *     { "referenciaId": "uuid-agua-mineral", "tipo": "PRODUCTO", "rol": "TARGET" }
     *   ]
     * }
     * 
     * Seguridad:
     * - Valida que la promoción pertenezca al local autenticado
     * - Valida que todos los productos referenciados existan y pertenezcan al local
     */
    @PutMapping("/{id}/alcance")
    public ResponseEntity<PromocionResponse> asociarProductos(
            @PathVariable String id,
            @Valid @RequestBody AsociarScopeCommand command
    ) {
        // TODO: Reemplazar por localId del contexto de autenticación cuando se implemente JWT
        LocalId localIdSimulado = new LocalId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        PromocionId promocionId = new PromocionId(UUID.fromString(id));

        PromocionResponse response = asociarProductoAPromocionUseCase.ejecutar(
                localIdSimulado,
                promocionId,
                command
        );

        return ResponseEntity.ok(response);
    }
}
