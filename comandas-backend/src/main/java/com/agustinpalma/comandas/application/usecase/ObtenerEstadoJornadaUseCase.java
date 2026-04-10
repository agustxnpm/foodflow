package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.EstadoCajaResponse;
import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.repository.JornadaCajaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * Caso de uso: consultar el estado actual de la caja.
 *
 * Determina si hay una jornada ABIERTA para el local.
 * Si no la hay, busca la última jornada CERRADA y retorna su
 * balanceEfectivo como sugerencia de fondo inicial.
 *
 * Este es el "gatekeeper" del sistema: el frontend lo consulta
 * para decidir si mostrar la UI operativa o la pantalla de apertura.
 */
@Transactional(readOnly = true)
public class ObtenerEstadoJornadaUseCase {

    private final JornadaCajaRepository jornadaCajaRepository;

    public ObtenerEstadoJornadaUseCase(JornadaCajaRepository jornadaCajaRepository) {
        this.jornadaCajaRepository = Objects.requireNonNull(jornadaCajaRepository,
            "jornadaCajaRepository es obligatorio");
    }

    /**
     * Consulta el estado de la caja para un local.
     *
     * @param localId identificador del local (tenant)
     * @return estado de la caja con datos relevantes según el caso
     */
    public EstadoCajaResponse ejecutar(LocalId localId) {
        Objects.requireNonNull(localId, "El localId es obligatorio");

        // 1. Buscar jornada ABIERTA
        Optional<JornadaCaja> jornadaAbierta = jornadaCajaRepository.buscarAbierta(localId);

        if (jornadaAbierta.isPresent()) {
            JornadaCaja jornada = jornadaAbierta.get();
            return EstadoCajaResponse.abierta(
                jornada.getId().getValue().toString(),
                jornada.getFondoInicial(),
                jornada.getFechaApertura() != null
                    ? jornada.getFechaApertura().toString()
                    : null
            );
        }

        // 2. No hay jornada abierta → buscar última cerrada para sugerencia
        Optional<JornadaCaja> ultimaCerrada = jornadaCajaRepository.buscarUltimaCerrada(localId);

        return EstadoCajaResponse.cerrada(
            ultimaCerrada.map(JornadaCaja::getBalanceEfectivo).orElse(null)
        );
    }
}
