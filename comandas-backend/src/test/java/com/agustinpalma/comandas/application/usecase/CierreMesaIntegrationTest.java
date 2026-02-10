package com.agustinpalma.comandas.application.usecase;

import com.agustinpalma.comandas.application.dto.AgregarProductoRequest;
import com.agustinpalma.comandas.application.dto.CerrarMesaResponse;
import com.agustinpalma.comandas.application.dto.PagoRequest;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Suite de Tests de Integración para el flujo de Cierre de Mesa.
 * 
 * Valida el flujo completo desde CerrarMesaUseCase hasta persistencia:
 * - Snapshot contable (inmutabilidad financiera)
 * - Pagos parciales (split)
 * - Validaciones de monto
 * - Transiciones de estado
 * 
 * Principios:
 * - Tests contra BD real (H2)
 * - Transaccional (rollback automático)
 * - Clock fijo para determinismo
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestClockConfig.class)
@DisplayName("Cierre de Mesa - Suite de Integración")
class CierreMesaIntegrationTest {

    @Autowired
    private CerrarMesaUseCase cerrarMesaUseCase;

    @Autowired
    private AgregarProductoUseCase agregarProductoUseCase;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    // Datos base del test
    private LocalId localId;
    private Mesa mesa;
    private Pedido pedido;

    // Productos base
    private Producto hamburguesa;  // $2500
    private Producto cerveza;      // $1500

    @BeforeEach
    void setUp() {
        // Configurar Local y Mesa
        localId = LocalId.generate();
        mesa = new Mesa(MesaId.generate(), localId, 1);
        mesaRepository.guardar(mesa);

        // Abrir mesa y crear pedido
        mesa.abrir();
        mesaRepository.guardar(mesa);

        pedido = new Pedido(
            PedidoId.generate(),
            localId,
            mesa.getId(),
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );
        pedidoRepository.guardar(pedido);

        // Crear productos de catálogo
        hamburguesa = new Producto(
            ProductoId.generate(), localId,
            "Hamburguesa Completa", new BigDecimal("2500"), true, "#FF0000"
        );
        cerveza = new Producto(
            ProductoId.generate(), localId,
            "Cerveza Artesanal", new BigDecimal("1500"), true, "#FFCC00"
        );
        productoRepository.guardar(hamburguesa);
        productoRepository.guardar(cerveza);
    }

    // ============================================
    // Helpers
    // ============================================

    /**
     * Agrega un producto al pedido activo de la mesa usando el caso de uso real.
     */
    private void agregarProductoAlPedido(Producto producto, int cantidad) {
        agregarProductoUseCase.ejecutar(
            new AgregarProductoRequest(pedido.getId(), producto.getId(), cantidad, null)
        );
    }

    // ============================================
    // Escenarios de éxito
    // ============================================

    @Nested
    @DisplayName("Cierre exitoso con pago único")
    class CierreExitosoPagoUnico {

        @Test
        @DisplayName("Debe cerrar mesa y pedido con pago único en EFECTIVO")
        void deberia_cerrar_con_pago_unico_efectivo() {
            // Given: Pedido con 1 hamburguesa ($2500)
            agregarProductoAlPedido(hamburguesa, 1);

            BigDecimal totalEsperado = new BigDecimal("2500");

            List<PagoRequest> pagos = List.of(
                new PagoRequest(MedioPago.EFECTIVO, totalEsperado)
            );

            // When: Se cierra la mesa
            CerrarMesaResponse response = cerrarMesaUseCase.ejecutar(localId, mesa.getId(), pagos);

            // Then: La mesa queda LIBRE
            assertThat(response.mesaEstado()).isEqualTo(EstadoMesa.LIBRE);

            // Then: El pedido queda CERRADO
            assertThat(response.pedidoEstado()).isEqualTo(EstadoPedido.CERRADO);

            // Then: El snapshot contable es correcto
            assertThat(response.montoSubtotal()).isEqualByComparingTo(totalEsperado);
            assertThat(response.montoTotal()).isEqualByComparingTo(totalEsperado);
            assertThat(response.montoDescuentos()).isEqualByComparingTo(BigDecimal.ZERO);

            // Then: Los pagos están registrados
            assertThat(response.pagos()).hasSize(1);
            assertThat(response.pagos().get(0).medio()).isEqualTo(MedioPago.EFECTIVO);
            assertThat(response.pagos().get(0).monto()).isEqualByComparingTo(totalEsperado);

            // Then: La fecha de cierre está registrada
            assertThat(response.fechaCierre()).isNotNull();

            // Then: Verificar persistencia real
            Mesa mesaRecuperada = mesaRepository.buscarPorId(mesa.getId()).orElseThrow();
            assertThat(mesaRecuperada.getEstado()).isEqualTo(EstadoMesa.LIBRE);

            Pedido pedidoRecuperado = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            assertThat(pedidoRecuperado.getEstado()).isEqualTo(EstadoPedido.CERRADO);
            assertThat(pedidoRecuperado.getMontoTotalFinal()).isEqualByComparingTo(totalEsperado);
            assertThat(pedidoRecuperado.getPagos()).hasSize(1);
        }

        @Test
        @DisplayName("Debe cerrar con múltiples ítems y pago único")
        void deberia_cerrar_con_multiples_items_pago_unico() {
            // Given: 2 hamburguesas ($5000) + 1 cerveza ($1500) = $6500
            agregarProductoAlPedido(hamburguesa, 2);
            agregarProductoAlPedido(cerveza, 1);

            BigDecimal totalEsperado = new BigDecimal("6500");

            List<PagoRequest> pagos = List.of(
                new PagoRequest(MedioPago.TARJETA, totalEsperado)
            );

            // When
            CerrarMesaResponse response = cerrarMesaUseCase.ejecutar(localId, mesa.getId(), pagos);

            // Then
            assertThat(response.pedidoEstado()).isEqualTo(EstadoPedido.CERRADO);
            assertThat(response.mesaEstado()).isEqualTo(EstadoMesa.LIBRE);
            assertThat(response.montoTotal()).isEqualByComparingTo(totalEsperado);
            assertThat(response.montoSubtotal()).isEqualByComparingTo(new BigDecimal("6500"));
        }
    }

    @Nested
    @DisplayName("Cierre exitoso con pagos múltiples (split)")
    class CierreExitosoPagosMultiples {

        @Test
        @DisplayName("Debe cerrar con mitad EFECTIVO + mitad TARJETA")
        void deberia_cerrar_con_split_efectivo_tarjeta() {
            // Given: 2 hamburguesas ($5000) + 2 cervezas ($3000) = $8000
            agregarProductoAlPedido(hamburguesa, 2);
            agregarProductoAlPedido(cerveza, 2);

            BigDecimal totalEsperado = new BigDecimal("8000");
            BigDecimal mitad = new BigDecimal("4000");

            List<PagoRequest> pagos = List.of(
                new PagoRequest(MedioPago.EFECTIVO, mitad),
                new PagoRequest(MedioPago.TARJETA, mitad)
            );

            // When
            CerrarMesaResponse response = cerrarMesaUseCase.ejecutar(localId, mesa.getId(), pagos);

            // Then: Pedido cerrado con total correcto
            assertThat(response.pedidoEstado()).isEqualTo(EstadoPedido.CERRADO);
            assertThat(response.mesaEstado()).isEqualTo(EstadoMesa.LIBRE);
            assertThat(response.montoTotal()).isEqualByComparingTo(totalEsperado);

            // Then: Los 2 pagos están registrados con medios y montos correctos
            assertThat(response.pagos()).hasSize(2);

            var pagoEfectivo = response.pagos().stream()
                .filter(p -> p.medio() == MedioPago.EFECTIVO)
                .findFirst().orElseThrow();
            assertThat(pagoEfectivo.monto()).isEqualByComparingTo(mitad);

            var pagoTarjeta = response.pagos().stream()
                .filter(p -> p.medio() == MedioPago.TARJETA)
                .findFirst().orElseThrow();
            assertThat(pagoTarjeta.monto()).isEqualByComparingTo(mitad);

            // Then: Verificar persistencia de pagos
            Pedido pedidoRecuperado = pedidoRepository.buscarPorId(pedido.getId()).orElseThrow();
            assertThat(pedidoRecuperado.getPagos()).hasSize(2);
        }

        @Test
        @DisplayName("Debe cerrar con 3 medios de pago distintos")
        void deberia_cerrar_con_tres_medios_de_pago() {
            // Given: 1 hamburguesa ($2500) + 1 cerveza ($1500) = $4000
            agregarProductoAlPedido(hamburguesa, 1);
            agregarProductoAlPedido(cerveza, 1);

            List<PagoRequest> pagos = List.of(
                new PagoRequest(MedioPago.EFECTIVO, new BigDecimal("1500")),
                new PagoRequest(MedioPago.TARJETA, new BigDecimal("1500")),
                new PagoRequest(MedioPago.QR, new BigDecimal("1000"))
            );

            // When
            CerrarMesaResponse response = cerrarMesaUseCase.ejecutar(localId, mesa.getId(), pagos);

            // Then
            assertThat(response.pedidoEstado()).isEqualTo(EstadoPedido.CERRADO);
            assertThat(response.montoTotal()).isEqualByComparingTo(new BigDecimal("4000"));
            assertThat(response.pagos()).hasSize(3);
        }
    }

    // ============================================
    // Escenarios de error
    // ============================================

    @Nested
    @DisplayName("Error: monto de pago incorrecto")
    class ErrorMontoPagoIncorrecto {

        @Test
        @DisplayName("Debe rechazar cierre si el pago es menor al total")
        void deberia_rechazar_pago_menor_al_total() {
            // Given: Pedido con total $2500
            agregarProductoAlPedido(hamburguesa, 1);

            List<PagoRequest> pagos = List.of(
                new PagoRequest(MedioPago.EFECTIVO, new BigDecimal("2000")) // Falta $500
            );

            // When/Then: Debe rechazar porque no cubre el total
            assertThatThrownBy(() -> cerrarMesaUseCase.ejecutar(localId, mesa.getId(), pagos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no coincide con el total");

            // Then: La mesa sigue ABIERTA
            Mesa mesaRecuperada = mesaRepository.buscarPorId(mesa.getId()).orElseThrow();
            assertThat(mesaRecuperada.getEstado()).isEqualTo(EstadoMesa.ABIERTA);
        }

        @Test
        @DisplayName("Debe rechazar cierre si el pago excede el total")
        void deberia_rechazar_pago_mayor_al_total() {
            // Given: Pedido con total $2500
            agregarProductoAlPedido(hamburguesa, 1);

            List<PagoRequest> pagos = List.of(
                new PagoRequest(MedioPago.EFECTIVO, new BigDecimal("3000")) // Excede en $500
            );

            // When/Then
            assertThatThrownBy(() -> cerrarMesaUseCase.ejecutar(localId, mesa.getId(), pagos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no coincide con el total");

            // Then: La mesa sigue ABIERTA
            Mesa mesaRecuperada = mesaRepository.buscarPorId(mesa.getId()).orElseThrow();
            assertThat(mesaRecuperada.getEstado()).isEqualTo(EstadoMesa.ABIERTA);
        }

        @Test
        @DisplayName("Debe rechazar cierre con split donde la suma no coincide")
        void deberia_rechazar_split_con_suma_incorrecta() {
            // Given: Pedido con total $4000 (1 hamburguesa + 1 cerveza)
            agregarProductoAlPedido(hamburguesa, 1);
            agregarProductoAlPedido(cerveza, 1);

            List<PagoRequest> pagos = List.of(
                new PagoRequest(MedioPago.EFECTIVO, new BigDecimal("2000")),
                new PagoRequest(MedioPago.TARJETA, new BigDecimal("1500")) // Total $3500 ≠ $4000
            );

            // When/Then
            assertThatThrownBy(() -> cerrarMesaUseCase.ejecutar(localId, mesa.getId(), pagos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no coincide con el total");
        }
    }

    @Nested
    @DisplayName("Error: pedido sin ítems")
    class ErrorPedidoSinItems {

        @Test
        @DisplayName("Debe rechazar cierre de pedido sin ítems cargados")
        void deberia_rechazar_cierre_pedido_vacio() {
            // Given: Pedido sin ítems (no se agregó ningún producto)
            List<PagoRequest> pagos = List.of(
                new PagoRequest(MedioPago.EFECTIVO, new BigDecimal("1000"))
            );

            // When/Then: El dominio debe rechazar
            assertThatThrownBy(() -> cerrarMesaUseCase.ejecutar(localId, mesa.getId(), pagos))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sin ítems");

            // Then: Mesa sigue ABIERTA
            Mesa mesaRecuperada = mesaRepository.buscarPorId(mesa.getId()).orElseThrow();
            assertThat(mesaRecuperada.getEstado()).isEqualTo(EstadoMesa.ABIERTA);
        }
    }
}
