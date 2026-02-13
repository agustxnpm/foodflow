package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.EgresoResponse;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MovimientoCajaId;
import com.agustinpalma.comandas.domain.model.MovimientoCaja;
import com.agustinpalma.comandas.domain.repository.MovimientoCajaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Caso de uso para registrar un egreso de caja.
 * 
 * Un egreso representa una salida de efectivo del local,
 * por ejemplo: compra de insumos, reparaciones, propinas, etc.
 * 
 * Flujo:
 * 1. Validar datos de entrada
 * 2. Crear MovimientoCaja (genera comprobante automáticamente)
 * 3. Persistir el movimiento
 * 4. Retornar respuesta con comprobante generado
 */
@Transactional
public class RegistrarEgresoUseCase {

    private final MovimientoCajaRepository movimientoCajaRepository;
    private final Clock clock;

    public RegistrarEgresoUseCase(MovimientoCajaRepository movimientoCajaRepository, Clock clock) {
        this.movimientoCajaRepository = Objects.requireNonNull(movimientoCajaRepository, 
            "El movimientoCajaRepository es obligatorio");
        this.clock = Objects.requireNonNull(clock, "El clock es obligatorio");
    }

    /**
     * Ejecuta el registro de un egreso de caja.
     *
     * @param localId identificador del local (tenant)
     * @param monto monto del egreso (debe ser > 0)
     * @param descripcion descripción del egreso
     * @return DTO con los datos del movimiento registrado y su comprobante
     * @throws IllegalArgumentException si el monto es <= 0 o la descripción está vacía
     */
    public EgresoResponse ejecutar(LocalId localId, BigDecimal monto, String descripcion) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(monto, "El monto es obligatorio");

        LocalDateTime ahora = LocalDateTime.now(clock);

        MovimientoCaja movimiento = new MovimientoCaja(
            MovimientoCajaId.generate(),
            localId,
            monto,
            descripcion,
            ahora
        );

        MovimientoCaja guardado = movimientoCajaRepository.guardar(movimiento);

        return EgresoResponse.fromDomain(guardado);
    }
}
