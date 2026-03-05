package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AbrirCajaResponse;
import com.agustinpalma.comandas.domain.exception.JornadaYaAbiertaException;
import com.agustinpalma.comandas.domain.exception.JornadaYaCerradaException;
import com.agustinpalma.comandas.domain.exception.TrialExpiredException;
import com.agustinpalma.comandas.domain.model.DomainIds.JornadaCajaId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.repository.JornadaCajaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Caso de uso: abrir una nueva jornada de caja.
 *
 * Crea una jornada en estado ABIERTA con el fondo inicial declarado
 * por el operador (el dinero físico que hay en el cajón).
 *
 * Validaciones:
 * - No puede existir otra jornada ABIERTA para el local
 * - El monto inicial debe ser ≥ 0
 *
 * La fecha operativa se calcula automáticamente según la hora de apertura
 * (regla de turno noche: antes de las 06:00 → día anterior).
 */
@Transactional
public class AbrirJornadaUseCase {

    private final JornadaCajaRepository jornadaCajaRepository;
    private final Clock clock;
    private final LocalDate fechaExpiracion;

    public AbrirJornadaUseCase(JornadaCajaRepository jornadaCajaRepository, Clock clock,
                               LocalDate fechaExpiracion) {
        this.jornadaCajaRepository = Objects.requireNonNull(jornadaCajaRepository,
            "jornadaCajaRepository es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
        this.fechaExpiracion = Objects.requireNonNull(fechaExpiracion,
            "fechaExpiracion es obligatorio");
    }

    /**
     * Abre una nueva jornada de caja.
     *
     * @param localId      identificador del local (tenant)
     * @param montoInicial efectivo físico declarado (≥ 0)
     * @return respuesta con los datos de la jornada creada
     * @throws JornadaYaAbiertaException si ya existe una jornada abierta
     * @throws IllegalArgumentException  si el monto es null o negativo
     */
    public AbrirCajaResponse ejecutar(LocalId localId, BigDecimal montoInicial) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(montoInicial, "El monto inicial es obligatorio");

        // 0. Validar período de prueba
        if (LocalDate.now(clock).isAfter(fechaExpiracion)) {
            throw new TrialExpiredException();
        }

        // 1. Validar que no exista jornada abierta
        if (jornadaCajaRepository.buscarAbierta(localId).isPresent()) {
            throw new JornadaYaAbiertaException();
        }

        // 2. Calcular fecha operativa para validar que no exista jornada previa
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate fechaOperativa = JornadaCaja.calcularFechaOperativa(ahora);

        if (jornadaCajaRepository.existePorFechaOperativa(localId, fechaOperativa)) {
            throw new JornadaYaCerradaException(fechaOperativa);
        }

        // 3. Crear jornada ABIERTA (la validación de monto ≥ 0 la hace el dominio)
        JornadaCajaId jornadaId = JornadaCajaId.generate();

        JornadaCaja jornada = new JornadaCaja(jornadaId, localId, montoInicial, ahora);
        jornadaCajaRepository.guardar(jornada);

        return new AbrirCajaResponse(
            jornadaId.getValue().toString(),
            jornada.getFondoInicial(),
            jornada.getFechaOperativa().toString()
        );
    }
}
