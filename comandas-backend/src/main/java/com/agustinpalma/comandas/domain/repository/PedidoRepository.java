package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import java.util.Optional;

/**
 * Contrato del repositorio de pedidos.
 * Define las operaciones de persistencia sin acoplarse a tecnologías específicas.
 * La implementación concreta reside en la capa de infraestructura.
 */
public interface PedidoRepository {

    /**
     * Persiste un pedido nuevo o actualiza uno existente.
     *
     * @param pedido el pedido a guardar
     * @return el pedido guardado
     */
    Pedido guardar(Pedido pedido);

    /**
     * Busca un pedido en un estado específico para una mesa dada.
     * Utilizado para validar que no exista otro pedido abierto antes de crear uno nuevo.
     *
     * @param mesaId identificador de la mesa
     * @param estado estado del pedido a buscar
     * @return Optional con el pedido si existe, vacío si no
     */
    Optional<Pedido> buscarPorMesaYEstado(MesaId mesaId, EstadoPedido estado);
}
