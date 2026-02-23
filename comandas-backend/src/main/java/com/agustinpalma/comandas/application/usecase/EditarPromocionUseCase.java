package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CrearPromocionCommand.TriggerParams;
import com.agustinpalma.comandas.application.dto.EditarPromocionCommand;
import com.agustinpalma.comandas.application.dto.PromocionResponse;
import com.agustinpalma.comandas.domain.model.CriterioActivacion;
import com.agustinpalma.comandas.domain.model.CriterioActivacion.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.ModoDescuento;
import com.agustinpalma.comandas.domain.model.DomainEnums.TipoCriterio;
import com.agustinpalma.comandas.domain.model.DomainEnums.TipoEstrategia;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.PromocionId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.*;
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

        // 4. Actualizar estrategia si se especificó
        EstrategiaPromocion estrategia = promocion.getEstrategia();
        if (command.tipoEstrategia() != null) {
            estrategia = construirEstrategia(command);
        }

        // 5. Actualizar triggers si se especificaron
        List<CriterioActivacion> triggers = promocion.getTriggers();
        if (command.triggers() != null && !command.triggers().isEmpty()) {
            triggers = construirTriggers(command.triggers());
        }

        // 6. Reconstruir instancia si cambiaron triggers o estrategia
        //    (ambos campos son final en Promocion)
        if (command.tipoEstrategia() != null || (command.triggers() != null && !command.triggers().isEmpty())) {
            promocion = new Promocion(
                    promocion.getId(),
                    promocion.getLocalId(),
                    promocion.getNombre(),
                    promocion.getDescripcion(),
                    promocion.getPrioridad(),
                    promocion.getEstado(),
                    estrategia,
                    triggers
            );
        }

        // 7. Persistir
        Promocion promocionActualizada = promocionRepository.guardar(promocion);

        return PromocionResponse.fromDomain(promocionActualizada);
    }

    /**
     * Construye la estrategia de dominio a partir del command de edición.
     * Reutiliza los sub-records de CrearPromocionCommand para los parámetros.
     */
    private EstrategiaPromocion construirEstrategia(EditarPromocionCommand command) {
        TipoEstrategia tipo;
        try {
            tipo = TipoEstrategia.valueOf(command.tipoEstrategia());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Tipo de estrategia no válido: '" + command.tipoEstrategia() +
                            "'. Valores permitidos: DESCUENTO_DIRECTO, CANTIDAD_FIJA, COMBO_CONDICIONAL, PRECIO_FIJO_CANTIDAD"
            );
        }

        return switch (tipo) {
            case DESCUENTO_DIRECTO -> {
                var params = command.descuentoDirecto();
                if (params == null) {
                    throw new IllegalArgumentException(
                            "Los parámetros de descuento directo son obligatorios para tipo DESCUENTO_DIRECTO"
                    );
                }
                ModoDescuento modo;
                try {
                    modo = ModoDescuento.valueOf(params.modo());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Modo de descuento no válido: '" + params.modo() +
                                    "'. Valores permitidos: PORCENTAJE, MONTO_FIJO"
                    );
                }
                yield new DescuentoDirecto(modo, params.valor());
            }
            case CANTIDAD_FIJA -> {
                var params = command.cantidadFija();
                if (params == null) {
                    throw new IllegalArgumentException(
                            "Los parámetros de cantidad fija son obligatorios para tipo CANTIDAD_FIJA"
                    );
                }
                yield new CantidadFija(params.cantidadLlevas(), params.cantidadPagas());
            }
            case COMBO_CONDICIONAL -> {
                var params = command.comboCondicional();
                if (params == null) {
                    throw new IllegalArgumentException(
                            "Los parámetros de combo condicional son obligatorios para tipo COMBO_CONDICIONAL"
                    );
                }
                yield new ComboCondicional(params.cantidadMinimaTrigger(), params.porcentajeBeneficio());
            }
            case PRECIO_FIJO_CANTIDAD -> {
                var params = command.precioFijoPorCantidad();
                if (params == null) {
                    throw new IllegalArgumentException(
                            "Los parámetros de precio fijo por cantidad son obligatorios para tipo PRECIO_FIJO_CANTIDAD"
                    );
                }
                yield new PrecioFijoPorCantidad(params.cantidadActivacion(), params.precioPaquete());
            }
        };
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
