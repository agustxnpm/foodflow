package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.ReporteCajaResponse;
import com.agustinpalma.comandas.domain.model.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.infrastructure.config.TestClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Suite de Tests de Integración para el módulo de Control de Caja.
 * 
 * Valida el flujo completo del reporte de caja diario (arqueo):
 * - Clasificación de ventas reales vs consumo interno
 * - Cálculo de egresos
 * - Balance de efectivo
 * - Desglose por medio de pago
 * 
 * Principios:
 * - Tests contra BD real (H2)
 * - Transaccional (rollback automático)
 * - Clock fijo para determinismo
 * - Sin mocks: validación end-to-end
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestClockConfig.class)
@DisplayName("Reporte de Caja - Suite de Integración")
class ReporteCajaIntegrationTest {

    @Autowired
    private GenerarReporteCajaUseCase generarReporteCajaUseCase;

    @Autowired
    private RegistrarEgresoUseCase registrarEgresoUseCase;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private Clock clock;

    // Datos base del test
    private LocalId localId;
    private Producto producto5000;
    private Producto producto3000;
    private Producto producto2000;

    @BeforeEach
    void setUp() {
        localId = LocalId.generate();

        // Crear productos del catálogo
        producto5000 = new Producto(
            ProductoId.generate(), localId,
            "Hamburguesa Completa", new BigDecimal("5000"), true, "#FF0000"
        );
        producto3000 = new Producto(
            ProductoId.generate(), localId,
            "Pizza Grande", new BigDecimal("3000"), true, "#00FF00"
        );
        producto2000 = new Producto(
            ProductoId.generate(), localId,
            "Cerveza Artesanal", new BigDecimal("2000"), true, "#FFFF00"
        );

        productoRepository.guardar(producto5000);
        productoRepository.guardar(producto3000);
        productoRepository.guardar(producto2000);
    }

    // ============================================
    // Helpers
    // ============================================

    /**
     * Crea un pedido, agrega un producto, lo cierra con el pago indicado y lo persiste.
     */
    private Pedido crearYCerrarPedido(Producto producto, MedioPago medioPago, int numeroMesa) {
        // Crear mesa
        Mesa mesa = new Mesa(MesaId.generate(), localId, numeroMesa);
        mesa.abrir();
        mesaRepository.guardar(mesa);

        // Crear pedido abierto
        Pedido pedido = new Pedido(
            PedidoId.generate(),
            localId,
            mesa.getId(),
            pedidoRepository.obtenerSiguienteNumero(localId),
            EstadoPedido.ABIERTO,
            LocalDateTime.now(clock)
        );
        pedido.agregarProducto(producto, 1, null);
        pedidoRepository.guardar(pedido);

        // Cerrar con pago
        LocalDateTime fechaCierre = LocalDateTime.now(clock);
        BigDecimal total = pedido.calcularTotal();
        Pago pago = new Pago(medioPago, total, fechaCierre);
        pedido.cerrar(List.of(pago), fechaCierre);
        pedidoRepository.guardar(pedido);

        // Liberar mesa
        mesa.liberar();
        mesaRepository.guardar(mesa);

        return pedido;
    }

    // ============================================
    // Tests
    // ============================================

    @Nested
    @DisplayName("Escenario principal: ventas mixtas con consumo interno y egreso")
    class EscenarioPrincipal {

        /**
         * Escenario completo:
         * 
         * Pedido 1 (Cliente): $5000 EFECTIVO
         * Pedido 2 (Cliente): $3000 TARJETA
         * Pedido 3 (Dueño):   $2000 A_CUENTA
         * Egreso (Limpieza):  $1000
         * 
         * Esperado:
         * totalVentasReales   = 8000 (5000 + 3000)
         * totalConsumoInterno = 2000
         * totalEgresos        = 1000
         * balanceEfectivo     = 4000 (5000 - 1000)
         * 
         * Desglose:
         * EFECTIVO = 5000
         * TARJETA  = 3000
         * A_CUENTA = 2000
         */
        @Test
        @DisplayName("debería calcular correctamente el reporte con ventas reales, consumo interno y egresos")
        void debería_calcular_reporte_completo() {
            // Given — Pedido 1: Cliente paga $5000 en EFECTIVO
            crearYCerrarPedido(producto5000, MedioPago.EFECTIVO, 1);

            // Given — Pedido 2: Cliente paga $3000 con TARJETA
            crearYCerrarPedido(producto3000, MedioPago.TARJETA, 2);

            // Given — Pedido 3: Dueño consume $2000, paga A_CUENTA
            crearYCerrarPedido(producto2000, MedioPago.A_CUENTA, 3);

            // Given — Egreso de caja: $1000 limpieza
            registrarEgresoUseCase.ejecutar(localId, new BigDecimal("1000"), "Productos de limpieza");

            // When — Generar reporte del día
            LocalDate hoy = LocalDate.now(clock);
            ReporteCajaResponse reporte = generarReporteCajaUseCase.ejecutar(localId, hoy);

            // Then — Verificar totales
            assertThat(reporte.totalVentasReales())
                .as("Total ventas reales (EFECTIVO + TARJETA)")
                .isEqualByComparingTo(new BigDecimal("8000"));

            assertThat(reporte.totalConsumoInterno())
                .as("Total consumo interno (A_CUENTA)")
                .isEqualByComparingTo(new BigDecimal("2000"));

            assertThat(reporte.totalEgresos())
                .as("Total egresos de caja")
                .isEqualByComparingTo(new BigDecimal("1000"));

            assertThat(reporte.balanceEfectivo())
                .as("Balance efectivo (5000 EFECTIVO - 1000 egreso)")
                .isEqualByComparingTo(new BigDecimal("4000"));

            // Then — Verificar desglose por medio de pago
            assertThat(reporte.desglosePorMedioPago())
                .containsEntry(MedioPago.EFECTIVO, new BigDecimal("5000"))
                .containsEntry(MedioPago.TARJETA, new BigDecimal("3000"))
                .containsEntry(MedioPago.A_CUENTA, new BigDecimal("2000"));

            // Then — Verificar lista de movimientos
            assertThat(reporte.movimientos())
                .hasSize(1)
                .first()
                .satisfies(m -> {
                    assertThat(m.monto()).isEqualByComparingTo(new BigDecimal("1000"));
                    assertThat(m.descripcion()).isEqualTo("Productos de limpieza");
                    assertThat(m.numeroComprobante()).startsWith("EGR-");
                });
        }
    }

    @Nested
    @DisplayName("Escenarios de borde")
    class EscenariosBorde {

        @Test
        @DisplayName("debería retornar reporte vacío cuando no hay operaciones en el día")
        void debería_retornar_reporte_vacio() {
            // When
            LocalDate hoy = LocalDate.now(clock);
            ReporteCajaResponse reporte = generarReporteCajaUseCase.ejecutar(localId, hoy);

            // Then
            assertThat(reporte.totalVentasReales()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(reporte.totalConsumoInterno()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(reporte.totalEgresos()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(reporte.balanceEfectivo()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(reporte.desglosePorMedioPago()).isEmpty();
            assertThat(reporte.movimientos()).isEmpty();
        }

        @Test
        @DisplayName("debería aislar datos entre locales (multi-tenancy)")
        void debería_aislar_datos_por_local() {
            // Given — Pedido en local principal
            crearYCerrarPedido(producto5000, MedioPago.EFECTIVO, 1);

            // Given — Otro local con su propio producto y pedido
            LocalId otroLocalId = LocalId.generate();
            Producto productoOtroLocal = new Producto(
                ProductoId.generate(), otroLocalId,
                "Empanada", new BigDecimal("500"), true, "#0000FF"
            );
            productoRepository.guardar(productoOtroLocal);

            Mesa mesaOtro = new Mesa(MesaId.generate(), otroLocalId, 1);
            mesaOtro.abrir();
            mesaRepository.guardar(mesaOtro);

            Pedido pedidoOtro = new Pedido(
                PedidoId.generate(), otroLocalId, mesaOtro.getId(),
                1, EstadoPedido.ABIERTO, LocalDateTime.now(clock)
            );
            pedidoOtro.agregarProducto(productoOtroLocal, 1, null);
            pedidoRepository.guardar(pedidoOtro);

            LocalDateTime fechaCierre = LocalDateTime.now(clock);
            pedidoOtro.cerrar(List.of(new Pago(MedioPago.EFECTIVO, new BigDecimal("500"), fechaCierre)), fechaCierre);
            pedidoRepository.guardar(pedidoOtro);

            // When — Reporte del local principal
            LocalDate hoy = LocalDate.now(clock);
            ReporteCajaResponse reporte = generarReporteCajaUseCase.ejecutar(localId, hoy);

            // Then — Solo incluye datos del local principal
            assertThat(reporte.totalVentasReales())
                .as("Solo debe incluir ventas del local principal")
                .isEqualByComparingTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("debería generar comprobante único al registrar egreso")
        void debería_generar_comprobante_unico() {
            // When
            var response = registrarEgresoUseCase.ejecutar(
                localId, new BigDecimal("500"), "Compra de servilletas"
            );

            // Then
            assertThat(response.numeroComprobante())
                .isNotNull()
                .startsWith("EGR-");
            assertThat(response.monto()).isEqualByComparingTo(new BigDecimal("500"));
            assertThat(response.tipo()).isEqualTo("EGRESO");
        }

        @Test
        @DisplayName("debería calcular balance negativo si egresos superan efectivo")
        void debería_calcular_balance_negativo() {
            // Given — Solo $1000 en efectivo
            Producto cafe = new Producto(
                ProductoId.generate(), localId,
                "Café", new BigDecimal("1000"), true, "#AA0000"
            );
            productoRepository.guardar(cafe);
            crearYCerrarPedido(cafe, MedioPago.EFECTIVO, 1);

            // Given — Egreso de $3000
            registrarEgresoUseCase.ejecutar(localId, new BigDecimal("3000"), "Reparación urgente");

            // When
            ReporteCajaResponse reporte = generarReporteCajaUseCase.ejecutar(localId, LocalDate.now(clock));

            // Then — Balance negativo
            assertThat(reporte.balanceEfectivo())
                .as("Balance debe ser negativo cuando egresos superan efectivo")
                .isEqualByComparingTo(new BigDecimal("-2000"));
        }
    }
}
