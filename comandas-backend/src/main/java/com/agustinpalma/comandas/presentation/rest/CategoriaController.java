package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.application.dto.CategoriaRequest;
import com.agustinpalma.comandas.application.dto.CategoriaResponse;
import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.application.usecase.ConsultarCategoriasUseCase;
import com.agustinpalma.comandas.application.usecase.CrearCategoriaUseCase;
import com.agustinpalma.comandas.application.usecase.EditarCategoriaUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarCategoriaUseCase;
import com.agustinpalma.comandas.domain.model.DomainIds.CategoriaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gestión de categorías del catálogo.
 * CRUD completo: crear, listar, editar, eliminar.
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final LocalContextProvider localContextProvider;
    private final ConsultarCategoriasUseCase consultarCategoriasUseCase;
    private final CrearCategoriaUseCase crearCategoriaUseCase;
    private final EditarCategoriaUseCase editarCategoriaUseCase;
    private final EliminarCategoriaUseCase eliminarCategoriaUseCase;

    public CategoriaController(
        LocalContextProvider localContextProvider,
        ConsultarCategoriasUseCase consultarCategoriasUseCase,
        CrearCategoriaUseCase crearCategoriaUseCase,
        EditarCategoriaUseCase editarCategoriaUseCase,
        EliminarCategoriaUseCase eliminarCategoriaUseCase
    ) {
        this.localContextProvider = localContextProvider;
        this.consultarCategoriasUseCase = consultarCategoriasUseCase;
        this.crearCategoriaUseCase = crearCategoriaUseCase;
        this.editarCategoriaUseCase = editarCategoriaUseCase;
        this.eliminarCategoriaUseCase = eliminarCategoriaUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        LocalId localId = localContextProvider.getCurrentLocalId();
        List<CategoriaResponse> categorias = consultarCategoriasUseCase.ejecutar(localId);
        return ResponseEntity.ok(categorias);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crearCategoria(@Valid @RequestBody CategoriaRequest request) {
        LocalId localId = localContextProvider.getCurrentLocalId();
        CategoriaResponse categoria = crearCategoriaUseCase.ejecutar(localId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoria);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> editarCategoria(
        @PathVariable UUID id, @Valid @RequestBody CategoriaRequest request
    ) {
        LocalId localId = localContextProvider.getCurrentLocalId();
        CategoriaId categoriaId = new CategoriaId(id);
        CategoriaResponse categoria = editarCategoriaUseCase.ejecutar(categoriaId, localId, request);
        return ResponseEntity.ok(categoria);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable UUID id) {
        LocalId localId = localContextProvider.getCurrentLocalId();
        CategoriaId categoriaId = new CategoriaId(id);
        eliminarCategoriaUseCase.ejecutar(categoriaId, localId);
        return ResponseEntity.noContent().build();
    }
}
