package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.CrearPromocionCommand;
import com.agustinpalma.comandas.application.dto.CrearPromocionCommand.*;
import com.agustinpalma.comandas.application.dto.PromocionResponse;
import com.agustinpalma.comandas.domain.model.CriterioActivacion;
import com.agustinpalma.comandas.domain.model.CriterioActivacion.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion;
import com.agustinpalma.comandas.domain.model.EstrategiaPromocion.*;
import com.agustinpalma.comandas.domain.model.Promocion;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Caso de uso: Crear una nueva promoción para un local con triggers configurables.
 * 
 * Responsabilidades:
 * 1. Validar que el LocalId sea válido
 * 2. Verificar unicidad del nombre en el local
 * 3. Construir la estrategia de dominio a partir del command
 * 4. Construir la lista de triggers (criterios de activación)
 * 5. Persistir la promoción
 * 
 * No contiene lógica de negocio — eso vive en el dominio
 * (validaciones de la entidad Promocion, VOs de estrategia y criterios).
 */
public class CrearPromocionUseCase {

    private final PromocionRepository promocionRepository;

    public CrearPromocionUseCase(PromocionRepository promocionRepository) {
        this.promocionRepository = Objects.requireNonNull(promocionRepository, "El promocionRepository es obligatorio");
    }

    public PromocionResponse ejecutar(LocalId localId, CrearPromocionCommand command) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(command, "El command es obligatorio");

        // 1. Verificar unicidad del nombre en el local
        if (promocionRepository.existePorNombreYLocal(command.nombre(), localId)) {
            throw new IllegalArgumentException(
                    "Ya existe una promoción con el nombre '" + command.nombre() + "' en este local"
            );
        }

        // 2. Construir estrategia de dominio a partir del command
        EstrategiaPromocion estrategia = construirEstrategia(command);

        // 3. Construir lista de triggers (criterios de activación)
        List<CriterioActivacion> triggers = construirTriggers(command.triggers());

        // 4. Crear la entidad de dominio (las validaciones corren en el constructor)
        Promocion nuevaPromocion = new Promocion(
                PromocionId.generate(),
                localId,
                command.nombre(),
                command.descripcion(),
                command.prioridad(),
                EstadoPromocion.ACTIVA,
                estrategia,
                triggers
        );

        // 5. Persistir
        Promocion promocionGuardada = promocionRepository.guardar(nuevaPromocion);

        return PromocionResponse.fromDomain(promocionGuardada);
    }

    /**
     * Construye la estrategia de dominio correspondiente según el tipo indicado en el command.
     * Delega la validación de parámetros a los constructores de los Value Objects.
     */
    private EstrategiaPromocion construirEstrategia(CrearPromocionCommand command) {
        TipoEstrategia tipo;
        try {
            tipo = TipoEstrategia.valueOf(command.tipoEstrategia());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Tipo de estrategia no válido: '" + command.tipoEstrategia() +
                            "'. Valores permitidos: DESCUENTO_DIRECTO, CANTIDAD_FIJA, COMBO_CONDICIONAL"
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
        };
    }

    /**
     * Construye la lista de criterios de activación a partir de los parámetros del command.
     */
    private List<CriterioActivacion> construirTriggers(List<TriggerParams> triggersParams) {
        if (triggersParams == null || triggersParams.isEmpty()) {
            throw new IllegalArgumentException("Debe especificar al menos un trigger");
        }

        return triggersParams.stream()
                .map(this::construirTrigger)
                .collect(Collectors.toList());
    }

    /**
     * Construye un criterio de activación individual según su tipo.
     */
    private CriterioActivacion construirTrigger(TriggerParams params) {
        TipoCriterio tipo;
        try {
            tipo = TipoCriterio.valueOf(params.tipo());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Tipo de trigger no válido: '" + params.tipo() +
                            "'. Valores permitidos: TEMPORAL, CONTENIDO, MONTO_MINIMO"
            );
        }

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
