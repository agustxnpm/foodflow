package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.EnviarComandaResponse;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainIds.ItemPedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.domain.model.DomainIds.MesaId;
import com.agustinpalma.comandas.domain.model.DomainIds.PedidoId;
import com.agustinpalma.comandas.domain.model.DomainIds.ProductoId;
import com.agustinpalma.comandas.domain.model.ItemPedido;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del caso de uso EnviarComandaCocinaUseCase.
 *
 * Valida el flujo HU-29: envío operativo (soloNuevos=true) vs reimpresión
 * (soloNuevos=false), actualización del timestamp de envío, y los guards
 * que protegen contra mesas libres o pedidos vacíos.
 *
 * Nota: EscPosGenerator.generarComandaCocina es una llamada estática de
 * infraestructura (pura manipulación de bytes). Se ejecuta realmente en los
 * tests — no tiene efectos secundarios ni requiere mock.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class EnviarComandaCocinaUseCaseTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    private EnviarComandaCocinaUseCase useCase;

    private LocalId localId;
    private MesaId mesaId;

    @BeforeEach
    void setUp() {
        useCase = new EnviarComandaCocinaUseCase(mesaRepository, pedidoRepository);
        localId = LocalId.generate();
        mesaId = MesaId.generate();
    }

    // ─── Helpers / Fixtures ──────────────────────────────────────────────────────

    private Mesa crearMesaAbierta() {
        Mesa mesa = new Mesa(mesaId, localId, 5);
        mesa.abrir();
        return mesa;
    }

    /**
     * Crea un ítem con cantidadEnviadaCocina=0 → tieneCantidadNueva()=true.
     * Simula un producto recién agregado que aún no fue comunicado a cocina.
     */
    private ItemPedido crearItemNuevo(PedidoId pedidoId) {
        return new ItemPedido(
            ItemPedidoId.generate(), pedidoId, ProductoId.generate(),
            "Empanada", 2, BigDecimal.valueOf(500), null,
            BigDecimal.ZERO, null, null, null, Collections.emptyList(),
            null, null, null, LocalDateTime.now(), 0
        );
    }

    /**
     * Crea un ítem donde cantidadEnviadaCocina == cantidad → tieneCantidadNueva()=false.
     * Simula un producto ya comunicado en un envío previo.
     */
    private ItemPedido crearItemYaEnviado(PedidoId pedidoId) {
        return new ItemPedido(
            ItemPedidoId.generate(), pedidoId, ProductoId.generate(),
            "Cerveza", 1, BigDecimal.valueOf(300), null,
            BigDecimal.ZERO, null, null, null, Collections.emptyList(),
            null, null, null, LocalDateTime.now(), 1
        );
    }

    private Pedido crearPedidoAbierto(PedidoId pedidoId, List<ItemPedido> items) {
        return Pedido.reconstruirDesdePersistencia(
            pedidoId, localId, mesaId, 1,
            EstadoPedido.ABIERTO, LocalDateTime.now(), null,
            items, Collections.emptyList(),
            null, null, null, null, null
        );
    }

    // ─── Tests ───────────────────────────────────────────────────────────────────

    @Test
    void deberia_enviar_solo_items_nuevos() {
        // Given — pedido con 1 ítem nuevo y 1 ya enviado a cocina
        PedidoId pedidoId = PedidoId.generate();
        Pedido pedido = crearPedidoAbierto(pedidoId, List.of(
            crearItemNuevo(pedidoId),
            crearItemYaEnviado(pedidoId)
        ));

        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(crearMesaAbierta()));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaId, localId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When — soloNuevos=true: modo operativo, solo comunica el delta
        EnviarComandaResponse response = useCase.ejecutar(localId, mesaId, true);

        // Then — cantidadItemsNuevos refleja solo el ítem pendiente; el pedido se persiste
        assertThat(response.cantidadItemsNuevos()).isEqualTo(1);
        assertThat(response.cantidadItemsTotal()).isEqualTo(2);
        verify(pedidoRepository, times(1)).guardar(any());
    }

    @Test
    void deberia_enviar_todos_en_reimpresion() {
        // Given — mismo pedido: 1 nuevo + 1 ya enviado
        PedidoId pedidoId = PedidoId.generate();
        Pedido pedido = crearPedidoAbierto(pedidoId, List.of(
            crearItemNuevo(pedidoId),
            crearItemYaEnviado(pedidoId)
        ));

        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(crearMesaAbierta()));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaId, localId)).thenReturn(Optional.of(pedido));

        // When — soloNuevos=false: reimpresión, todos los ítems
        EnviarComandaResponse response = useCase.ejecutar(localId, mesaId, false);

        // Then — todos los ítems reflejados; el timestamp NO se actualiza (no guardar)
        assertThat(response.cantidadItemsNuevos()).isEqualTo(2);
        assertThat(response.cantidadItemsTotal()).isEqualTo(2);
        verify(pedidoRepository, never()).guardar(any());
    }

    @Test
    void deberia_actualizar_timestamp_envio() {
        // Given — pedido con un ítem nuevo
        PedidoId pedidoId = PedidoId.generate();
        Pedido pedido = crearPedidoAbierto(pedidoId, List.of(crearItemNuevo(pedidoId)));

        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(crearMesaAbierta()));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaId, localId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        useCase.ejecutar(localId, mesaId, true);

        // Then — marcarComoEnviadoACocina produce la persistencia del pedido actualizado
        verify(pedidoRepository, times(1)).guardar(any());
    }

    @Test
    void deberia_rechazar_mesa_libre() {
        // Given — mesa en estado LIBRE (sin pedido activo asignado)
        Mesa mesaLibre = new Mesa(mesaId, localId, 3);
        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(mesaLibre));

        // When / Then
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesaId, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no tiene un pedido activo");
    }

    @Test
    void deberia_rechazar_pedido_sin_items() {
        // Given — mesa ABIERTA con un pedido vacío (sin ítems agregados)
        Pedido pedidoVacio = crearPedidoAbierto(PedidoId.generate(), Collections.emptyList());

        when(mesaRepository.buscarPorId(mesaId)).thenReturn(Optional.of(crearMesaAbierta()));
        when(pedidoRepository.buscarAbiertoPorMesa(mesaId, localId)).thenReturn(Optional.of(pedidoVacio));

        // When / Then
        assertThatThrownBy(() -> useCase.ejecutar(localId, mesaId, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("sin ítems");
    }
}
