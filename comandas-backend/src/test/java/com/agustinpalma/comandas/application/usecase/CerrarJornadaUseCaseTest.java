package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.domain.exception.MesasAbiertasException;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoJornada;
import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoPedido;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import com.agustinpalma.comandas.domain.model.DomainEnums.TipoMovimiento;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.JornadaCaja;
import com.agustinpalma.comandas.domain.model.Mesa;
import com.agustinpalma.comandas.domain.model.MovimientoCaja;
import com.agustinpalma.comandas.domain.model.Pago;
import com.agustinpalma.comandas.domain.model.Pedido;
import com.agustinpalma.comandas.domain.model.Producto;
import com.agustinpalma.comandas.domain.repository.JornadaCajaRepository;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.MovimientoCajaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests del caso de uso CerrarJornadaUseCase.
 *
 * Escenarios validados:
 *  1. Balance contable correcto: fondo + efectivo en ventas + ingresos manuales − egresos.
 *  2. Guard: mesas ABIERTA presentes → lanza MesasAbiertasException.
 *  3. Separación de ventas reales vs. consumo interno (A_CUENTA no suma a ventas).
 *
 * Fórmula del arqueo (Regla de Negocio central):
 *   balanceEfectivo = fondoInicial + Σ(pagos EFECTIVO) + Σ(ingresos) − Σ(egresos)
 *
 * Los pedidos cerrados son mockeados: solo importa lo que pagan y con qué medio.
 *
 * Stack: JUnit 5 + AssertJ + Mockito.
 * Sin Spring. Sin base de datos.
 */
@ExtendWith(MockitoExtension.class)
class CerrarJornadaUseCaseTest {

    @Mock private MesaRepository             mesaRepository;
    @Mock private PedidoRepository           pedidoRepository;
    @Mock private MovimientoCajaRepository   movimientoCajaRepository;
    @Mock private JornadaCajaRepository      jornadaCajaRepository;

    private CerrarJornadaUseCase useCase;
    private LocalId localId;

    // Clock fijo a las 14:00 del día de la jornada
    private static final LocalDateTime FECHA_APERTURA_JORNADA =
        LocalDateTime.of(2026, 4, 21, 8, 0);
    private static final LocalDateTime FECHA_CIERRE =
        LocalDateTime.of(2026, 4, 21, 14, 0);

    @BeforeEach
    void setUp() {
        localId = LocalId.generate();
        Clock clock = Clock.fixed(
            FECHA_CIERRE.toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC
        );
        useCase = new CerrarJornadaUseCase(
            mesaRepository, pedidoRepository, movimientoCajaRepository,
            jornadaCajaRepository, clock
        );
    }

    // ============================================================
    // Helpers de fixtures
    // ============================================================

    /** Crea una jornada ABIERTA con el fondo indicado. */
    private JornadaCaja jornadaAbierta(String fondo) {
        return new JornadaCaja(
            JornadaCajaId.generate(),
            localId,
            new BigDecimal(fondo),
            FECHA_APERTURA_JORNADA
        );
    }

    /**
     * Crea un Pedido CERRADO real con 1 producto al precio indicado y un único pago
     * del medio indicado por ese mismo monto.
     *
     * Nota: Pedido.getPagos() es final → no se puede mockear. Se usan objetos reales.
     */
    private Pedido crearPedidoCerrado(MedioPago medio, String precio) {
        Producto prod = new Producto(
            ProductoId.generate(), localId,
            "Prod-" + medio, new BigDecimal(precio), true, "#FFFFFF"
        );
        Pedido pedido = new Pedido(
            PedidoId.generate(), localId, MesaId.generate(),
            1, EstadoPedido.ABIERTO, FECHA_APERTURA_JORNADA
        );
        pedido.agregarProducto(prod, 1, null);
        pedido.cerrar(
            List.of(new Pago(medio, new BigDecimal(precio), FECHA_CIERRE)),
            FECHA_CIERRE
        );
        return pedido;
    }

    private MovimientoCaja egreso(String monto) {
        return new MovimientoCaja(
            MovimientoCajaId.generate(), localId,
            new BigDecimal(monto), "Egreso test", FECHA_CIERRE,
            TipoMovimiento.EGRESO
        );
    }

    private MovimientoCaja ingreso(String monto) {
        return new MovimientoCaja(
            MovimientoCajaId.generate(), localId,
            new BigDecimal(monto), "Ingreso test", FECHA_CIERRE,
            TipoMovimiento.INGRESO
        );
    }

    // ============================================================
    // 1. Balance contable
    // ============================================================

    @Nested
    class BalanceContable {

        @Test
        void deberia_cerrar_jornada_con_balance_correcto() {
            // Given
            // fondoInicial = $1000
            // 1 pedido: EFECTIVO $500     → entradasEfectivo += $500, totalVentas += $500
            // 1 egreso:             $100  → totalEgresos += $100
            // 1 ingreso manual:     $200  → totalIngresos += $200
            // balanceEfectivo = $1000 + $500 + $200 - $100 = $1600
            JornadaCaja jornada = jornadaAbierta("1000");
            given(jornadaCajaRepository.buscarAbierta(localId))
                .willReturn(Optional.of(jornada));
            given(mesaRepository.buscarPorLocal(localId))
                .willReturn(List.of());  // sin mesas abiertas
            given(pedidoRepository.buscarCerradosPorFecha(eq(localId), any(), any()))
                .willReturn(List.of(
                    crearPedidoCerrado(MedioPago.EFECTIVO, "500")
                ));
            given(movimientoCajaRepository.buscarPorFecha(eq(localId), any(), any()))
                .willReturn(List.of(egreso("100"), ingreso("200")));

            // When
            JornadaCajaId idRetornado = useCase.ejecutar(localId);

            // Then — la jornada fue mutada y persistida
            assertThat(idRetornado).isNotNull();
            assertThat(jornada.getEstado()).isEqualTo(EstadoJornada.CERRADA);
            assertThat(jornada.getTotalVentasReales()).isEqualByComparingTo("500");
            assertThat(jornada.getTotalConsumoInterno()).isEqualByComparingTo("0");
            assertThat(jornada.getTotalEgresos()).isEqualByComparingTo("100");
            assertThat(jornada.getBalanceEfectivo()).isEqualByComparingTo("1600");
            assertThat(jornada.getPedidosCerradosCount()).isEqualTo(1);
            then(jornadaCajaRepository).should(times(1)).guardar(jornada);
        }
    }

    // ============================================================
    // 2. Guard: mesas abiertas
    // ============================================================

    @Nested
    class GuardMesasAbiertas {

        @Test
        void deberia_rechazar_si_existen_mesas_abiertas() {
            // Given — 1 mesa con estado ABIERTA (tiene pedido activo)
            JornadaCaja jornada = jornadaAbierta("500");
            given(jornadaCajaRepository.buscarAbierta(localId))
                .willReturn(Optional.of(jornada));

            Mesa mesaConPedido = new Mesa(MesaId.generate(), localId, 3);
            mesaConPedido.abrir();  // estado → ABIERTA
            given(mesaRepository.buscarPorLocal(localId))
                .willReturn(List.of(mesaConPedido));

            // When / Then — la caja no puede cerrarse con mesas activas
            assertThatThrownBy(() -> useCase.ejecutar(localId))
                .isInstanceOf(MesasAbiertasException.class)
                .hasMessageContaining("1");

            // La jornada no fue persistida
            then(jornadaCajaRepository).should(never()).guardar(any());
        }
    }

    // ============================================================
    // 3. Separación contable: ventas reales vs. consumo interno
    // ============================================================

    @Nested
    class SeparacionContable {

        @Test
        void deberia_separar_ventas_reales_de_consumo_interno() {
            // Given
            // Pedido 1: pago EFECTIVO $300  → ventas reales  + entrada de efectivo
            // Pedido 2: pago A_CUENTA  $200 → consumo interno (no suma a ventas, no entra a caja)
            // Sin movimientos de caja
            // balanceEfectivo = $1000 + $300 (solo EFECTIVO) + $0 - $0 = $1300
            JornadaCaja jornada = jornadaAbierta("1000");
            given(jornadaCajaRepository.buscarAbierta(localId))
                .willReturn(Optional.of(jornada));
            given(mesaRepository.buscarPorLocal(localId))
                .willReturn(List.of());
            given(pedidoRepository.buscarCerradosPorFecha(eq(localId), any(), any()))
                .willReturn(List.of(
                    crearPedidoCerrado(MedioPago.EFECTIVO, "300"),
                    crearPedidoCerrado(MedioPago.A_CUENTA,  "200")
                ));
            given(movimientoCajaRepository.buscarPorFecha(eq(localId), any(), any()))
                .willReturn(List.of());

            // When
            useCase.ejecutar(localId);

            // Then — la separación contable es correcta
            assertThat(jornada.getTotalVentasReales()).isEqualByComparingTo("300");
            assertThat(jornada.getTotalConsumoInterno()).isEqualByComparingTo("200");
            assertThat(jornada.getBalanceEfectivo()).isEqualByComparingTo("1300");
            assertThat(jornada.getPedidosCerradosCount()).isEqualTo(2);
        }
    }

    // ============================================================
    // 4. Guard: precondición — debe existir jornada ABIERTA
    // ============================================================

    @Nested
    class GuardJornadaAbiertaInexistente {

        @Test
        void deberia_rechazar_si_no_hay_jornada_abierta() {
            // Given — el repositorio no tiene jornada ABIERTA para el local
            given(jornadaCajaRepository.buscarAbierta(localId)).willReturn(Optional.empty());

            // When / Then — no se puede cerrar lo que nunca se abrió
            assertThatThrownBy(() -> useCase.ejecutar(localId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no hay una jornada abierta");

            // No se inspeccionaron mesas, pedidos ni movimientos: corto-circuito en el guard
            then(mesaRepository).should(never()).buscarPorLocal(any());
            then(pedidoRepository).should(never()).buscarCerradosPorFecha(any(), any(), any());
            then(movimientoCajaRepository).should(never()).buscarPorFecha(any(), any(), any());
            then(jornadaCajaRepository).should(never()).guardar(any());
        }
    }
}
