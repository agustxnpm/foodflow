package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.EnviarComandaResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.infrastructure.adapter.EscPosGenerator;
import com.agustinpalma.comandas.infrastructure.adapter.EscPosGenerator.ComandaCocinaData;
import com.agustinpalma.comandas.infrastructure.adapter.EscPosGenerator.ComandaItemData;
import com.agustinpalma.comandas.infrastructure.config.MeisenProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * HU-29: Caso de uso para enviar comanda a cocina.
 *
 * Flujo:
 * 1. Validar mesa (existe, pertenece al local, está abierta)
 * 2. Obtener pedido abierto
 * 3. Determinar ítems nuevos (desde dominio)
 * 4. Marcar pedido como enviado a cocina (actualiza timestamp)
 * 5. Generar buffer ESC/POS con la comanda
 * 6. Persistir el pedido actualizado
 * 7. Retornar el Base64 del buffer para que el frontend lo envíe a la impresora
 *
 * Decisión de diseño: este use case genera bytes ESC/POS.
 * Técnicamente EscPosGenerator es infraestructura, pero el use case necesita
 * orquestar el "qué imprimir" con el "marcar como enviado" en la misma transacción.
 * La generación de bytes no tiene efecto secundario — es una función pura sobre datos.
 *
 * Alternativa evaluada: mover la generación a un servicio de dominio. Se descartó
 * porque ESC/POS es un protocolo de hardware — ni dominio ni aplicación deberían
 * conocer bytes. El use case recibe un "generador" como dependencia inyectada.
 */
@Transactional
public class EnviarComandaCocinaUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnviarComandaCocinaUseCase.class);

    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;
    private final MeisenProperties properties;

    public EnviarComandaCocinaUseCase(
        MesaRepository mesaRepository,
        PedidoRepository pedidoRepository,
        MeisenProperties properties
    ) {
        this.mesaRepository = Objects.requireNonNull(mesaRepository, "El mesaRepository es obligatorio");
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository, "El pedidoRepository es obligatorio");
        this.properties = Objects.requireNonNull(properties, "Las properties son obligatorias");
    }

    /**
     * Ejecuta el envío de comanda a cocina.
     *
     * Genera el buffer ESC/POS solo con los ítems NUEVOS (agregados después del
     * último envío). Si nunca se envió, todos los ítems se consideran nuevos.
     *
     * Luego actualiza el timestamp de envío en el pedido para que en la próxima
     * consulta, los ítems actuales ya no se marquen como "nuevos".
     *
     * Permite reimpresiones ilimitadas: cada llamada genera un nuevo buffer
     * y actualiza el timestamp.
     *
     * @param localId ID del local (tenant)
     * @param mesaId ID de la mesa
     * @return respuesta con el buffer ESC/POS en Base64 y metadata
     */
    /**
     * Sobrecarga por defecto: solo ítems nuevos + actualiza timestamp.
     */
    public EnviarComandaResponse ejecutar(LocalId localId, MesaId mesaId) {
        return ejecutar(localId, mesaId, true);
    }

    /**
     * Ejecuta el envío/reimpresión de comanda a cocina.
     *
     * @param localId ID del local (tenant)
     * @param mesaId ID de la mesa
     * @param soloNuevos true = solo ítems nuevos + actualizar timestamp (envío operativo).
     *                   false = todos los ítems sin actualizar timestamp (reimpresión).
     */
    public EnviarComandaResponse ejecutar(LocalId localId, MesaId mesaId, boolean soloNuevos) {
        Objects.requireNonNull(localId, "El localId es obligatorio");
        Objects.requireNonNull(mesaId, "El mesaId es obligatorio");

        // 1. Validar mesa
        Mesa mesa = mesaRepository.buscarPorId(mesaId)
            .orElseThrow(() -> new IllegalStateException("La mesa no existe"));

        if (!mesa.getLocalId().equals(localId)) {
            throw new IllegalArgumentException("La mesa no pertenece a este local");
        }

        if (mesa.getEstado() == EstadoMesa.LIBRE) {
            throw new IllegalStateException("La mesa no tiene un pedido activo");
        }

        // 2. Obtener pedido abierto
        Pedido pedido = pedidoRepository.buscarAbiertoPorMesa(mesaId, localId)
            .orElseThrow(() -> new IllegalStateException(
                "La mesa está abierta pero no tiene un pedido activo. Inconsistencia de datos."
            ));

        if (pedido.getItems().isEmpty()) {
            throw new IllegalStateException("No se puede enviar una comanda sin ítems");
        }

        // 3. Identificar ítems nuevos ANTES de actualizar el timestamp
        List<ItemPedido> itemsNuevos = pedido.obtenerItemsNuevos();
        int cantidadNuevos = itemsNuevos.size();

        log.info("{} comanda: Mesa {}, Pedido #{}, {} ítems totales, {} nuevos",
            soloNuevos ? "Enviando" : "Reimprimiendo",
            mesa.getNumero(), pedido.getNumero(), pedido.getItems().size(), cantidadNuevos);

        // 4. Generar buffer ESC/POS
        LocalDateTime ahora = LocalDateTime.now();
        ComandaCocinaData data = construirDatosComanda(pedido, mesa.getNumero(), ahora, soloNuevos);
        byte[] escPosBuffer = EscPosGenerator.generarComandaCocina(data, soloNuevos);

        // 5. Solo actualizar timestamp si es envío operativo (no reimpresión)
        if (soloNuevos) {
            pedido.marcarComoEnviadoACocina(ahora);
            pedidoRepository.guardar(pedido);
        }

        // 6. Retornar Base64
        String base64 = Base64.getEncoder().encodeToString(escPosBuffer);

        log.info("Comanda generada: {} bytes ESC/POS, {} chars Base64",
            escPosBuffer.length, base64.length());

        return new EnviarComandaResponse(
            base64,
            ahora,
            soloNuevos ? cantidadNuevos : pedido.getItems().size(),
            pedido.getItems().size()
        );
    }

    /**
     * Construye el DTO de datos para el generador ESC/POS.
     *
     * Transforma el modelo de dominio en datos planos para impresión.
     * Los extras se representan como strings simples (solo nombre, para cocina).
     *
     * HU-29 fix: Cuando soloNuevos=true, la cantidad de cada ítem es el DELTA
     * (unidades nuevas = cantidad - cantidadEnviadaCocina), no el total acumulado.
     * Cuando soloNuevos=false (reimpresión), se usa la cantidad total.
     *
     * @param soloNuevos true = usar delta como cantidad; false = usar cantidad total
     */
    private ComandaCocinaData construirDatosComanda(Pedido pedido, int numeroMesa, LocalDateTime ahora, boolean soloNuevos) {
        List<ComandaItemData> items = pedido.getItems().stream()
            .map(item -> {
                boolean esNuevo = item.tieneCantidadNueva();
                // Si es envío operativo y el ítem tiene cantidad nueva → imprimir solo el delta
                // Si es reimpresión → siempre imprimir la cantidad total
                int cantidad = (soloNuevos && esNuevo)
                    ? item.obtenerCantidadNueva()
                    : item.getCantidad();
                return new ComandaItemData(
                    cantidad,
                    item.getNombreProducto(),
                    item.getObservacion(),
                    esNuevo,
                    item.getExtras().stream()
                        .map(extra -> extra.getNombre())
                        .toList()
                );
            })
            .toList();

        return new ComandaCocinaData(
            numeroMesa,
            pedido.getNumero(),
            ahora,
            items
        );
    }
}
