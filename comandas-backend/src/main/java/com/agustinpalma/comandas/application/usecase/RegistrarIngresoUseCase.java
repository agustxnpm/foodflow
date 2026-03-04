package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.IngresoResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimiento;
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
 * Caso de uso para registrar un ingreso manual de caja.
 * 
 * Un ingreso representa una entrada de efectivo al local que NO proviene
 * de un pedido/mesa convencional. Ejemplo: cobro en efectivo de plataformas
 * externas (PedidosYa, Rappi), ajustes manuales, etc.
 * 
 * Flujo:
 * 1. Validar datos de entrada
 * 2. Crear MovimientoCaja con tipo INGRESO (genera comprobante ING-...)
 * 3. Persistir el movimiento
 * 4. Retornar respuesta con comprobante generado
 */
@Transactional
public class RegistrarIngresoUseCase {

    private final MovimientoCajaRepository movimientoCajaRepository;
    private final Clock clock;

    public RegistrarIngresoUseCase(MovimientoCajaRepository movimientoCajaRepository, Clock clock) {
        this.movimientoCajaRepository = Objects.requireNonNull(movimientoCajaRepository, 
            "El movimientoCajaRepository es obligatorio");
        this.clock = Objects.requireNonNull(clock, "El clock es obligatorio");
    }

    /**
     * Ejecuta el registro de un ingreso manual de caja.
     *
     * @param localId identificador del local (tenant)
     * @param monto monto del ingreso (debe ser > 0)
     * @param descripcion descripción del ingreso
     * @return DTO con los datos del movimiento registrado y su comprobante
     * @throws IllegalArgumentException si el monto es <= 0 o la descripción está vacía
     */
    public IngresoResponse ejecutar(LocalId localId, BigDecimal monto, String descripcion) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(monto, "El monto es obligatorio");

        LocalDateTime ahora = LocalDateTime.now(clock);

        MovimientoCaja movimiento = new MovimientoCaja(
            MovimientoCajaId.generate(),
            localId,
            monto,
            descripcion,
            ahora,
            TipoMovimiento.INGRESO
        );

        MovimientoCaja guardado = movimientoCajaRepository.guardar(movimiento);

        return IngresoResponse.fromDomain(guardado);
    }
}
