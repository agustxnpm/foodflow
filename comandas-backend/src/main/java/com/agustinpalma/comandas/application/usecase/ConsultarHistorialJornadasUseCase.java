package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.JornadaResumenResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.repository.JornadaCajaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Caso de uso para consultar el historial de jornadas cerradas.
 *
 * Permite al operador revisar los cierres de caja pasados dentro de
 * un rango de fechas operativas. Usado por:
 *   - Lista expandible del historial
 *   - Gráfico interactivo de evolución financiera
 *
 * No contiene lógica de negocio; solo orquesta la consulta al repositorio
 * y transforma al DTO de presentación.
 *
 * Validaciones:
 *   - El rango no puede exceder 365 días (prevenir consultas pesadas)
 *   - 'desde' no puede ser posterior a 'hasta'
 */
public class ConsultarHistorialJornadasUseCase {

    private static final int MAX_DIAS_RANGO = 365;

    private final JornadaCajaRepository jornadaCajaRepository;

    public ConsultarHistorialJornadasUseCase(JornadaCajaRepository jornadaCajaRepository) {
        this.jornadaCajaRepository = Objects.requireNonNull(jornadaCajaRepository,
            "jornadaCajaRepository es obligatorio");
    }

    /**
     * Ejecuta la consulta de historial de jornadas cerradas.
     *
     * @param localId identificador del local (tenant)
     * @param desde   fecha operativa inicial (inclusive)
     * @param hasta   fecha operativa final (inclusive)
     * @return lista de resúmenes de jornada, ordenada desc por fecha operativa
     * @throws IllegalArgumentException si el rango es inválido o excede el máximo
     */
    public List<JornadaResumenResponse> ejecutar(LocalId localId, LocalDate desde, LocalDate hasta) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(desde, "La fecha 'desde' es obligatoria");
        Objects.requireNonNull(hasta, "La fecha 'hasta' es obligatoria");

        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                "La fecha 'desde' (%s) no puede ser posterior a 'hasta' (%s)".formatted(desde, hasta)
            );
        }

        long diasEnRango = desde.until(hasta).getDays();
        if (diasEnRango > MAX_DIAS_RANGO) {
            throw new IllegalArgumentException(
                "El rango no puede exceder %d días (solicitado: %d)".formatted(MAX_DIAS_RANGO, diasEnRango)
            );
        }

        List<JornadaCaja> jornadas = jornadaCajaRepository.buscarPorRangoFecha(localId, desde, hasta);

        return jornadas.stream()
            .map(JornadaResumenResponse::fromDomain)
            .toList();
    }
}
