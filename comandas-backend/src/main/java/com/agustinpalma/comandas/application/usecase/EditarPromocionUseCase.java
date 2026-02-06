package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CrearPromocionCommand.TriggerParams;
import com.agustinpalma.comandas.application.dto.EditarPromocionCommand;
import com.agustinpalma.comandas.application.dto.PromocionResponse;
import com.agustinpalma.comandas.domain.model.CriterioActivacion;
import com.agustinpalma.comandas.domain.model.CriterioActivacion.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.TipoCriterio;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PromocionId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Caso de uso: Editar una promoción existente.
 * 
 * Valida:
 * 1. Que la promoción exista
 * 2. Que pertenezca al LocalId del usuario (seguridad multi-tenant)
 * 3. Que el nuevo nombre sea único si se cambió
 * 4. Que los triggers sean válidos
 * 
 * Solo actualiza los campos presentes en el command.
 */
public class EditarPromocionUseCase {

    private final PromocionRepository promocionRepository;

    public EditarPromocionUseCase(PromocionRepository promocionRepository) {
        this.promocionRepository = Objects.requireNonNull(
                promocionRepository,
                "El promocionRepository es obligatorio"
        );
    }

    public PromocionResponse ejecutar(LocalId localId, PromocionId promocionId, EditarPromocionCommand command) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(promocionId, "El promocionId es obligatorio");
        Objects.requireNonNull(command, "El command es obligatorio");

        // 1. Recuperar la promoción validando multi-tenancy
        Promocion promocion = promocionRepository.buscarPorIdYLocal(promocionId, localId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la promoción con id " + promocionId.getValue() +
                        " para el local " + localId.getValue()
                ));

        // 2. Validar unicidad del nombre si cambió
        if (!promocion.getNombre().equals(command.nombre())) {
            if (promocionRepository.existePorNombreYLocal(command.nombre(), localId)) {
                throw new IllegalArgumentException(
                        "Ya existe una promoción con el nombre '" + command.nombre() + "' en este local"
                );
            }
        }

        // 3. Aplicar cambios
        promocion.actualizarNombre(command.nombre());
        
        if (command.descripcion() != null) {
            promocion.actualizarDescripcion(command.descripcion());
        }
        
        if (command.prioridad() != null) {
            promocion.actualizarPrioridad(command.prioridad());
        }

        // 4. Actualizar triggers si se especificaron
        if (command.triggers() != null && !command.triggers().isEmpty()) {
            List<CriterioActivacion> nuevosTriggers = construirTriggers(command.triggers());
            // Como los triggers son final, necesitamos crear una nueva instancia
            promocion = new Promocion(
                    promocion.getId(),
                    promocion.getLocalId(),
                    promocion.getNombre(),
                    promocion.getDescripcion(),
                    promocion.getPrioridad(),
                    promocion.getEstado(),
                    promocion.getEstrategia(),
                    nuevosTriggers
            );
        }

        // 5. Persistir
        Promocion promocionActualizada = promocionRepository.guardar(promocion);

        return PromocionResponse.fromDomain(promocionActualizada);
    }

    private List<CriterioActivacion> construirTriggers(List<TriggerParams> triggersParams) {
        return triggersParams.stream()
                .map(this::construirTrigger)
                .collect(Collectors.toList());
    }

    private CriterioActivacion construirTrigger(TriggerParams params) {
        TipoCriterio tipo = TipoCriterio.valueOf(params.tipo());

        return switch (tipo) {
            case TEMPORAL -> {
                if (params.fechaDesde() == null || params.fechaHasta() == null) {
                    throw new IllegalArgumentException(
                            "Para trigger TEMPORAL, fechaDesde y fechaHasta son obligatorias"
                    );
                }
                yield new CriterioTemporal(
                        params.fechaDesde(),
                        params.fechaHasta(),
                        params.diasSemana(),
                        params.horaDesde(),
                        params.horaHasta()
                );
            }
            case CONTENIDO -> {
                if (params.productosRequeridos() == null || params.productosRequeridos().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Para trigger CONTENIDO, debe especificar al menos un producto requerido"
                    );
                }
                Set<ProductoId> productos = params.productosRequeridos().stream()
                        .map(ProductoId::from)
                        .collect(Collectors.toSet());
                yield new CriterioContenido(productos);
            }
            case MONTO_MINIMO -> {
                if (params.montoMinimo() == null) {
                    throw new IllegalArgumentException(
                            "Para trigger MONTO_MINIMO, montoMinimo es obligatorio"
                    );
                }
                yield new CriterioMontoMinimo(params.montoMinimo());
            }
        };
    }
}
