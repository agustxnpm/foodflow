package com.agustinpalma.comandas.infrastructure.persistence;

import com.agustinpalma.comandas.domain.model.*;
import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.infrastructure.config.TestClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Test de integración para HU-07: Mantener pedido abierto.
 * 
 * Valida el ciclo de vida completo de un pedido con persistencia real:
 * 1. Crear pedido y guardarlo
 * 2. Recuperarlo desde la BD
 * 3. Agregar productos en múltiples momentos
 * 4. Verificar atomicidad de guardado (pedido + ítems)
 * 5. Validar aislamiento multi-tenant
 * 
 * Este test verifica que la implementación JPA con @OneToMany y cascade funciona correctamente.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@Transactional
@DisplayName("HU-07: Mantener pedido abierto - Test de Integración")
class PedidoRepositoryIntegrationTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    private LocalId localId;
    private MesaId mesa5Id;
    private MesaId mesa10Id;
    private Producto productoMilanesa;
    private Producto productoCerveza;

    @BeforeEach
    void setUp() {
        // Configuración de datos de prueba
        localId = LocalId.generate();
        mesa5Id = MesaId.generate();
        mesa10Id = MesaId.generate();

        // Crear productos de prueba
        productoMilanesa = new Producto(
            ProductoId.generate(),
            localId,
            "Milanesa con papas",
            new BigDecimal("3500.00"),
            true,
            "#FF5500"
        );

        productoCerveza = new Producto(
            ProductoId.generate(),
            localId,
            "Cerveza artesanal",
            new BigDecimal("1200.00"),
            true,
            "#FFD700"
        );

        // Guardar productos en la BD
        productoRepository.guardar(productoMilanesa);
        productoRepository.guardar(productoCerveza);
    }

    @Test
    @DisplayName("Paso 1 y 2: Crear pedido y recuperarlo desde BD manteniendo estado ABIERTO")
    void deberia_crear_pedido_y_recuperarlo_con_estado_abierto() {
        // GIVEN - Paso 1: Crear pedido para Mesa 5
        Pedido pedido = new Pedido(
            PedidoId.generate(),
            localId,
            mesa5Id,
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );

        // WHEN - Guardar en repositorio
        Pedido pedidoGuardado = pedidoRepository.guardar(pedido);

        // THEN - Paso 2: Recuperar desde BD usando MesaId
        Optional<Pedido> pedidoRecuperado = pedidoRepository.buscarAbiertoPorMesa(mesa5Id, localId);

        assertThat(pedidoRecuperado).isPresent();
        assertThat(pedidoRecuperado.get().getId()).isEqualTo(pedidoGuardado.getId());
        assertThat(pedidoRecuperado.get().getEstado()).isEqualTo(EstadoPedido.ABIERTO);
        assertThat(pedidoRecuperado.get().getMesaId()).isEqualTo(mesa5Id);
        assertThat(pedidoRecuperado.get().getLocalId()).isEqualTo(localId);
    }

    @Test
    @DisplayName("Paso 3 y 4: Agregar producto, guardar y verificar persistencia atómica")
    void deberia_agregar_producto_y_persistir_atomicamente() {
        // GIVEN - Crear pedido inicial
        Pedido pedido = new Pedido(
            PedidoId.generate(),
            localId,
            mesa5Id,
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );
        pedidoRepository.guardar(pedido);

        // WHEN - Paso 3: Recuperar, agregar producto y guardar
        Optional<Pedido> pedidoRecuperado = pedidoRepository.buscarAbiertoPorMesa(mesa5Id, localId);
        assertThat(pedidoRecuperado).isPresent();

        Pedido pedidoExistente = pedidoRecuperado.get();
        pedidoExistente.agregarProducto(productoMilanesa, 2, "Sin ajo");
        
        pedidoRepository.guardar(pedidoExistente);

        // THEN - Paso 4: Recuperar nuevamente y verificar que tiene el ítem
        Optional<Pedido> pedidoFinal = pedidoRepository.buscarAbiertoPorMesa(mesa5Id, localId);
        
        assertThat(pedidoFinal).isPresent();
        assertThat(pedidoFinal.get().getItems()).hasSize(1);
        
        ItemPedido item = pedidoFinal.get().getItems().get(0);
        assertThat(item.getNombreProducto()).isEqualTo("Milanesa con papas");
        assertThat(item.getCantidad()).isEqualTo(2);
        assertThat(item.getPrecioUnitario()).isEqualByComparingTo(new BigDecimal("3500.00"));
        assertThat(item.getObservacion()).isEqualTo("Sin ajo");
        
        // Verificar que el total es correcto
        BigDecimal totalEsperado = new BigDecimal("7000.00"); // 3500 * 2
        assertThat(pedidoFinal.get().calcularTotal()).isEqualByComparingTo(totalEsperado);
    }

    @Test
    @DisplayName("Ciclo completo: Agregar múltiples productos en diferentes momentos")
    void deberia_permitir_agregar_productos_en_multiples_sesiones() {
        // GIVEN - Crear pedido inicial
        Pedido pedidoInicial = new Pedido(
            PedidoId.generate(),
            localId,
            mesa5Id,
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );
        pedidoRepository.guardar(pedidoInicial);

        // WHEN - Primera sesión: Agregar milanesa
        Optional<Pedido> pedidoSesion1 = pedidoRepository.buscarAbiertoPorMesa(mesa5Id, localId);
        assertThat(pedidoSesion1).isPresent();
        
        pedidoSesion1.get().agregarProducto(productoMilanesa, 1, null);
        pedidoRepository.guardar(pedidoSesion1.get());

        // WHEN - Segunda sesión: Agregar cerveza (simulando que pasa tiempo)
        Optional<Pedido> pedidoSesion2 = pedidoRepository.buscarAbiertoPorMesa(mesa5Id, localId);
        assertThat(pedidoSesion2).isPresent();
        
        pedidoSesion2.get().agregarProducto(productoCerveza, 3, null);
        pedidoRepository.guardar(pedidoSesion2.get());

        // THEN - Tercera sesión: Verificar que ambos productos están presentes
        Optional<Pedido> pedidoFinal = pedidoRepository.buscarAbiertoPorMesa(mesa5Id, localId);
        
        assertThat(pedidoFinal).isPresent();
        assertThat(pedidoFinal.get().getItems()).hasSize(2);
        
        // Verificar total: (3500 * 1) + (1200 * 3) = 3500 + 3600 = 7100
        BigDecimal totalEsperado = new BigDecimal("7100.00");
        assertThat(pedidoFinal.get().calcularTotal()).isEqualByComparingTo(totalEsperado);
    }

    @Test
    @DisplayName("Paso 5: Validar aislamiento multi-tenant - Pedidos de diferentes mesas no se mezclan")
    void deberia_mantener_aislamiento_entre_mesas() {
        // GIVEN - Crear pedidos para dos mesas diferentes
        Pedido pedidoMesa5 = new Pedido(
            PedidoId.generate(),
            localId,
            mesa5Id,
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );
        
        Pedido pedidoMesa10 = new Pedido(
            PedidoId.generate(),
            localId,
            mesa10Id,
            2,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );

        pedidoRepository.guardar(pedidoMesa5);
        pedidoRepository.guardar(pedidoMesa10);

        // WHEN - Agregar productos a cada mesa
        Optional<Pedido> recuperadoMesa5 = pedidoRepository.buscarAbiertoPorMesa(mesa5Id, localId);
        assertThat(recuperadoMesa5).isPresent();
        recuperadoMesa5.get().agregarProducto(productoMilanesa, 1, null);
        pedidoRepository.guardar(recuperadoMesa5.get());

        Optional<Pedido> recuperadoMesa10 = pedidoRepository.buscarAbiertoPorMesa(mesa10Id, localId);
        assertThat(recuperadoMesa10).isPresent();
        recuperadoMesa10.get().agregarProducto(productoCerveza, 2, null);
        pedidoRepository.guardar(recuperadoMesa10.get());

        // THEN - Verificar que cada mesa tiene su pedido correcto
        Optional<Pedido> finalMesa5 = pedidoRepository.buscarAbiertoPorMesa(mesa5Id, localId);
        Optional<Pedido> finalMesa10 = pedidoRepository.buscarAbiertoPorMesa(mesa10Id, localId);

        assertThat(finalMesa5).isPresent();
        assertThat(finalMesa5.get().getItems()).hasSize(1);
        assertThat(finalMesa5.get().getItems().get(0).getNombreProducto()).isEqualTo("Milanesa con papas");

        assertThat(finalMesa10).isPresent();
        assertThat(finalMesa10.get().getItems()).hasSize(1);
        assertThat(finalMesa10.get().getItems().get(0).getNombreProducto()).isEqualTo("Cerveza artesanal");
    }

    @Test
    @DisplayName("Validar que validarPermiteModificacion() impide modificar pedidos cerrados")
    void deberia_impedir_agregar_productos_a_pedido_cerrado() {
        // GIVEN - Crear y cerrar un pedido
        Pedido pedido = new Pedido(
            PedidoId.generate(),
            localId,
            mesa5Id,
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );
        
        pedido.agregarProducto(productoMilanesa, 1, null);
        pedidoRepository.guardar(pedido);

        // Recuperar y cerrar el pedido
        Optional<Pedido> pedidoRecuperado = pedidoRepository.buscarAbiertoPorMesa(mesa5Id, localId);
        assertThat(pedidoRecuperado).isPresent();
        
        pedidoRecuperado.get().finalizar(MedioPago.EFECTIVO, LocalDateTime.now());
        pedidoRepository.guardar(pedidoRecuperado.get());

        // WHEN/THEN - Intentar agregar producto a pedido cerrado debe fallar
        Optional<Pedido> pedidoCerrado = pedidoRepository.buscarPorId(pedido.getId());
        assertThat(pedidoCerrado).isPresent();
        
        assertThatThrownBy(() -> 
            pedidoCerrado.get().agregarProducto(productoCerveza, 1, null)
        )
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No se puede modificar un pedido en estado CERRADO");
    }

    @Test
    @DisplayName("Validar que buscarAbiertoPorMesa no devuelve pedidos de otros locales")
    void deberia_respetar_aislamiento_multi_tenant_por_local() {
        // GIVEN - Crear dos locales diferentes
        LocalId local1 = LocalId.generate();
        LocalId local2 = LocalId.generate();
        MesaId mesaCompartida = MesaId.generate();

        // Crear productos para cada local
        Producto productoLocal1 = new Producto(
            ProductoId.generate(),
            local1,
            "Producto Local 1",
            new BigDecimal("1000.00"),
            true,
            "#AA0000"
        );
        
        Producto productoLocal2 = new Producto(
            ProductoId.generate(),
            local2,
            "Producto Local 2",
            new BigDecimal("2000.00"),
            true,
            "#00AA00"
        );
        
        productoRepository.guardar(productoLocal1);
        productoRepository.guardar(productoLocal2);

        // Crear pedidos para la misma mesa pero en locales diferentes
        Pedido pedidoLocal1 = new Pedido(
            PedidoId.generate(),
            local1,
            mesaCompartida,
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );
        
        Pedido pedidoLocal2 = new Pedido(
            PedidoId.generate(),
            local2,
            mesaCompartida,
            1,
            EstadoPedido.ABIERTO,
            LocalDateTime.now()
        );

        pedidoRepository.guardar(pedidoLocal1);
        pedidoRepository.guardar(pedidoLocal2);

        // WHEN - Buscar pedido abierto para cada local
        Optional<Pedido> recuperadoLocal1 = pedidoRepository.buscarAbiertoPorMesa(mesaCompartida, local1);
        Optional<Pedido> recuperadoLocal2 = pedidoRepository.buscarAbiertoPorMesa(mesaCompartida, local2);

        // THEN - Cada local ve solo su propio pedido
        assertThat(recuperadoLocal1).isPresent();
        assertThat(recuperadoLocal1.get().getLocalId()).isEqualTo(local1);
        assertThat(recuperadoLocal1.get().getId()).isEqualTo(pedidoLocal1.getId());

        assertThat(recuperadoLocal2).isPresent();
        assertThat(recuperadoLocal2.get().getLocalId()).isEqualTo(local2);
        assertThat(recuperadoLocal2.get().getId()).isEqualTo(pedidoLocal2.getId());

        // Verificar que son pedidos diferentes
        assertThat(recuperadoLocal1.get().getId()).isNotEqualTo(recuperadoLocal2.get().getId());
    }
}
