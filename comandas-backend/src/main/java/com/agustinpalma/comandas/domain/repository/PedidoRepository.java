package com.agustinpalma.comandas.domain.repository;

import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;

import java.time.LocalDateTime;
import java.util.List;
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
     * Busca un pedido por su identificador.
     *
     * @param id identificador del pedido
     * @return Optional con el pedido si existe, vacío en caso contrario
     */
    Optional<Pedido> buscarPorId(PedidoId id);

    /**
     * Obtiene el siguiente número de pedido disponible para un local.
     * Calcula el máximo número existente + 1 para ese local.
     *
     * @param localId identificador del local
     * @return el siguiente número secuencial disponible
     */
    int obtenerSiguienteNumero(LocalId localId);

    /**
     * Busca un pedido en un estado específico para una mesa dada.
     * Utilizado para validar que no exista otro pedido abierto antes de crear uno nuevo.
     *
     * @param mesaId identificador de la mesa
     * @param estado estado del pedido a buscar
     * @return Optional con el pedido si existe, vacío si no
     */
    Optional<Pedido> buscarPorMesaYEstado(MesaId mesaId, EstadoPedido estado);

    /**
     * Busca el pedido actualmente abierto para una mesa específica dentro de un local.
     * Este método es clave para mantener pedidos abiertos: permite recuperar un pedido
     * existente sin necesidad de que el frontend envíe el PedidoId.
     * 
     * El aislamiento multi-tenant se garantiza filtrando también por LocalId.
     *
     * @param mesaId identificador de la mesa
     * @param localId identificador del local (para aislamiento multi-tenant)
     * @return Optional con el pedido abierto si existe, vacío si no
     */
    Optional<Pedido> buscarAbiertoPorMesa(MesaId mesaId, LocalId localId);

    /**
     * Busca todos los pedidos cerrados de un local dentro de un rango de fechas.
     * Utilizado por el reporte de caja diario para calcular ventas reales.
     * 
     * IMPORTANTE: La implementación DEBE usar JOIN FETCH para cargar los pagos
     * eager y evitar el problema N+1.
     *
     * @param localId identificador del local (tenant)
     * @param inicio inicio del rango temporal (inclusive)
     * @param fin fin del rango temporal (inclusive)
     * @return lista de pedidos cerrados con sus pagos cargados
     */
    List<Pedido> buscarCerradosPorFecha(LocalId localId, LocalDateTime inicio, LocalDateTime fin);
}
