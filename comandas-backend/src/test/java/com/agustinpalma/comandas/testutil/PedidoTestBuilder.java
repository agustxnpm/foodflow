package com.agustinpalma.comandas.testutil;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.Pedido;

import java.time.LocalDateTime;

/**
 * Builder fluido para construir un {@link Pedido} válido en tests.
 *
 * Genera por defecto un Pedido en estado ABIERTO con fechas y IDs sintéticos,
 * para evitar ruido de construcción en los tests del dominio y de aplicación.
 *
 * Uso típico:
 * <pre>
 *     Pedido pedido = PedidoTestBuilder.abierto()
 *         .enLocal(localId)
 *         .conNumero(7)
 *         .build();
 * </pre>
 */
public final class PedidoTestBuilder {

    /** Fecha de apertura por defecto (compartida con la fixture estándar de los tests). */
    public static final LocalDateTime FECHA_APERTURA_DEFAULT =
        LocalDateTime.of(2026, 4, 21, 10, 0);

    private LocalId         localId        = LocalId.generate();
    private MesaId          mesaId         = MesaId.generate();
    private int             numero         = 1;
    private EstadoPedido    estado         = EstadoPedido.ABIERTO;
    private LocalDateTime   fechaApertura  = FECHA_APERTURA_DEFAULT;

    private PedidoTestBuilder() { }

    /** Builder para un pedido en estado ABIERTO (caso por defecto). */
    public static PedidoTestBuilder abierto() {
        return new PedidoTestBuilder();
    }

    public PedidoTestBuilder enLocal(LocalId localId) {
        this.localId = localId;
        return this;
    }

    public PedidoTestBuilder enMesa(MesaId mesaId) {
        this.mesaId = mesaId;
        return this;
    }

    public PedidoTestBuilder conNumero(int numero) {
        this.numero = numero;
        return this;
    }

    public PedidoTestBuilder conFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
        return this;
    }

    public Pedido build() {
        return new Pedido(
            PedidoId.generate(),
            localId,
            mesaId,
            numero,
            estado,
            fechaApertura
        );
    }
}
