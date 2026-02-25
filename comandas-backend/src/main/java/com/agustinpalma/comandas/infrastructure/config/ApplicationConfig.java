package com.agustinpalma.comandas.infrastructure.config;

import com.agustinpalma.comandas.application.usecase.AbrirMesaUseCase;
import com.agustinpalma.comandas.application.usecase.AgregarProductoUseCase;
import com.agustinpalma.comandas.application.usecase.AjustarStockUseCase;
import com.agustinpalma.comandas.application.usecase.AplicarDescuentoManualUseCase;
import com.agustinpalma.comandas.application.usecase.AsociarProductoAPromocionUseCase;
import com.agustinpalma.comandas.application.usecase.CerrarMesaUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarCategoriasUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarDetallePedidoUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarMesasUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarProductosUseCase;
import com.agustinpalma.comandas.application.usecase.CrearCategoriaUseCase;
import com.agustinpalma.comandas.application.usecase.CrearMesaUseCase;
import com.agustinpalma.comandas.application.usecase.CrearProductoUseCase;
import com.agustinpalma.comandas.application.usecase.CrearVarianteUseCase;
import com.agustinpalma.comandas.application.usecase.EditarCategoriaUseCase;
import com.agustinpalma.comandas.application.usecase.EditarProductoUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarCategoriaUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarMesaUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarProductoUseCase;
import com.agustinpalma.comandas.application.usecase.GenerarReporteCajaUseCase;
import com.agustinpalma.comandas.application.usecase.GestionarItemsPedidoUseCase;
import com.agustinpalma.comandas.application.usecase.ReabrirPedidoUseCase;
import com.agustinpalma.comandas.application.usecase.RegistrarEgresoUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarPromocionesUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarPromocionUseCase;
import com.agustinpalma.comandas.application.usecase.CambiarEstadoPromocionUseCase;
import com.agustinpalma.comandas.application.usecase.CrearPromocionUseCase;
import com.agustinpalma.comandas.application.usecase.EditarPromocionUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarPromocionUseCase;
import com.agustinpalma.comandas.domain.repository.CategoriaRepository;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.MovimientoCajaRepository;
import com.agustinpalma.comandas.domain.repository.MovimientoStockRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import com.agustinpalma.comandas.domain.service.GestorStockService;
import com.agustinpalma.comandas.domain.service.MotorReglasService;
import com.agustinpalma.comandas.domain.service.NormalizadorVariantesService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuración de Spring para inyección de dependencias.
 * Aquí se cablean los casos de uso con sus dependencias.
 * Los casos de uso NO tienen anotaciones de Spring - permanecen puros.
 */
@Configuration
public class ApplicationConfig {

    /**
     * Bean del caso de uso para consultar mesas.
     * Spring inyectará automáticamente la implementación JPA del repositorio.
     *
     * @param mesaRepository implementación del repositorio (MesaRepositoryImpl)
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public ConsultarMesasUseCase consultarMesasUseCase(MesaRepository mesaRepository) {
        return new ConsultarMesasUseCase(mesaRepository);
    }

    /**
     * Bean del caso de uso para abrir mesa.
     * Spring inyectará automáticamente las implementaciones JPA de los repositorios.
     *
     * @param mesaRepository implementación del repositorio de mesas
     * @param pedidoRepository implementación del repositorio de pedidos
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public AbrirMesaUseCase abrirMesaUseCase(MesaRepository mesaRepository, PedidoRepository pedidoRepository) {
        return new AbrirMesaUseCase(mesaRepository, pedidoRepository);
    }

    /**
     * Bean del caso de uso para cerrar mesa.
     * Incluye MotorReglasService para re-evaluar promociones antes del cierre.
     *
     * @param mesaRepository implementación del repositorio de mesas
     * @param pedidoRepository implementación del repositorio de pedidos
     * @param promocionRepository implementación del repositorio de promociones
     * @param motorReglasService servicio de dominio para evaluar promociones
     * @param clock reloj del sistema
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public CerrarMesaUseCase cerrarMesaUseCase(
            MesaRepository mesaRepository,
            PedidoRepository pedidoRepository,
            PromocionRepository promocionRepository,
            ProductoRepository productoRepository,
            MovimientoStockRepository movimientoStockRepository,
            MotorReglasService motorReglasService,
            GestorStockService gestorStockService,
            Clock clock
    ) {
        return new CerrarMesaUseCase(mesaRepository, pedidoRepository, promocionRepository,
                productoRepository, movimientoStockRepository, motorReglasService, gestorStockService, clock);
    }

    /**
     * Bean del caso de uso para crear mesa.
     * Spring inyectará automáticamente la implementación JPA del repositorio.
     *
     * @param mesaRepository implementación del repositorio de mesas
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public CrearMesaUseCase crearMesaUseCase(MesaRepository mesaRepository) {
        return new CrearMesaUseCase(mesaRepository);
    }

    /**
     * Bean del caso de uso para eliminar mesa.
     * Spring inyectará automáticamente la implementación JPA del repositorio.
     *
     * @param mesaRepository implementación del repositorio de mesas
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public EliminarMesaUseCase eliminarMesaUseCase(MesaRepository mesaRepository) {
        return new EliminarMesaUseCase(mesaRepository);
    }

    /**
     * Bean del caso de uso para agregar producto a un pedido.
     * HU-10: Ahora incluye motor de reglas para aplicar promociones automáticamente.
     * Spring inyectará automáticamente las implementaciones de los repositorios y servicios.
     *
     * @param pedidoRepository implementación del repositorio de pedidos
     * @param productoRepository implementación del repositorio de productos
     * @param promocionRepository implementación del repositorio de promociones
     * @param motorReglasService servicio de dominio para evaluar promociones
     * @param clock reloj del sistema configurado para zona horaria de Argentina
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public AgregarProductoUseCase agregarProductoUseCase(
            PedidoRepository pedidoRepository, 
            ProductoRepository productoRepository,
            PromocionRepository promocionRepository,
            MotorReglasService motorReglasService,
            NormalizadorVariantesService normalizadorVariantesService,
            Clock clock
    ) {
        return new AgregarProductoUseCase(
            pedidoRepository, 
            productoRepository, 
            promocionRepository, 
            motorReglasService,
            normalizadorVariantesService,
            clock
        );
    }

    /**
     * HU-10: Bean del servicio de dominio para el motor de reglas de promociones.
     * Es un servicio de dominio puro (sin dependencias de infraestructura).
     *
     * @return instancia del servicio de dominio
     */
    @Bean
    public MotorReglasService motorReglasService() {
        return new MotorReglasService();
    }

    /**
     * HU-22: Bean del servicio de dominio para normalización de variantes.
     * Es un servicio de dominio puro (sin dependencias de infraestructura).
     *
     * @return instancia del servicio de dominio
     */
    @Bean
    public NormalizadorVariantesService normalizadorVariantesService() {
        return new NormalizadorVariantesService();
    }

    /**
     * Bean del caso de uso para consultar detalle de pedido.
     * Spring inyectará automáticamente las implementaciones JPA de los repositorios.
     *
     * @param mesaRepository implementación del repositorio de mesas
     * @param pedidoRepository implementación del repositorio de pedidos
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public ConsultarDetallePedidoUseCase consultarDetallePedidoUseCase(
        MesaRepository mesaRepository, 
        PedidoRepository pedidoRepository
    ) {
        return new ConsultarDetallePedidoUseCase(mesaRepository, pedidoRepository);
    }

    /**
     * Bean del caso de uso para consultar productos.
     * Spring inyectará automáticamente las implementaciones JPA de los repositorios.
     * PromocionRepository se usa para enriquecer los productos con promos activas.
     *
     * @param productoRepository implementación del repositorio de productos
     * @param promocionRepository implementación del repositorio de promociones
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public ConsultarProductosUseCase consultarProductosUseCase(
            ProductoRepository productoRepository,
            PromocionRepository promocionRepository
    ) {
        return new ConsultarProductosUseCase(productoRepository, promocionRepository);
    }

    /**
     * Bean del caso de uso para crear producto.
     * Spring inyectará automáticamente la implementación JPA del repositorio.
     *
     * @param productoRepository implementación del repositorio de productos
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public CrearProductoUseCase crearProductoUseCase(ProductoRepository productoRepository) {
        return new CrearProductoUseCase(productoRepository);
    }

    /**
     * Bean del caso de uso para crear variantes de productos.
     * Permite crear variantes jerárquicas de un producto base (ej: Simple → Doble → Triple).
     *
     * @param productoRepository implementación del repositorio de productos
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public CrearVarianteUseCase crearVarianteUseCase(ProductoRepository productoRepository) {
        return new CrearVarianteUseCase(productoRepository);
    }

    /**
     * Bean del caso de uso para editar producto.
     * Spring inyectará automáticamente la implementación JPA del repositorio.
     *
     * @param productoRepository implementación del repositorio de productos
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public EditarProductoUseCase editarProductoUseCase(ProductoRepository productoRepository) {
        return new EditarProductoUseCase(productoRepository);
    }

    /**
     * Bean del caso de uso para eliminar producto.
     * Spring inyectará automáticamente la implementación JPA del repositorio.
     *
     * @param productoRepository implementación del repositorio de productos
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public EliminarProductoUseCase eliminarProductoUseCase(ProductoRepository productoRepository) {
        return new EliminarProductoUseCase(productoRepository);
    }

    /**
     * Bean del caso de uso para crear promoción.
     * Spring inyectará automáticamente la implementación JPA del repositorio.
     *
     * @param promocionRepository implementación del repositorio de promociones
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public CrearPromocionUseCase crearPromocionUseCase(PromocionRepository promocionRepository) {
        return new CrearPromocionUseCase(promocionRepository);
    }

    /**
     * Bean del caso de uso para consultar todas las promociones de un local.
     */
    @Bean
    public ConsultarPromocionesUseCase consultarPromocionesUseCase(PromocionRepository promocionRepository) {
        return new ConsultarPromocionesUseCase(promocionRepository);
    }

    /**
     * Bean del caso de uso para consultar el detalle de una promoción.
     */
    @Bean
    public ConsultarPromocionUseCase consultarPromocionUseCase(PromocionRepository promocionRepository) {
        return new ConsultarPromocionUseCase(promocionRepository);
    }

    /**
     * Bean del caso de uso para editar una promoción.
     */
    @Bean
    public EditarPromocionUseCase editarPromocionUseCase(PromocionRepository promocionRepository) {
        return new EditarPromocionUseCase(promocionRepository);
    }

    /**
     * Bean del caso de uso para eliminar (desactivar) una promoción.
     */
    @Bean
    public EliminarPromocionUseCase eliminarPromocionUseCase(PromocionRepository promocionRepository) {
        return new EliminarPromocionUseCase(promocionRepository);
    }

    /**
     * Bean del caso de uso para cambiar el estado (ACTIVA/INACTIVA) de una promoción.
     */
    @Bean
    public CambiarEstadoPromocionUseCase cambiarEstadoPromocionUseCase(PromocionRepository promocionRepository) {
        return new CambiarEstadoPromocionUseCase(promocionRepository);
    }

    /**
     * Bean del caso de uso para asociar productos a una promoción (HU-09).
     */
    @Bean
    public AsociarProductoAPromocionUseCase asociarProductoAPromocionUseCase(
            PromocionRepository promocionRepository,
            ProductoRepository productoRepository
    ) {
        return new AsociarProductoAPromocionUseCase(promocionRepository, productoRepository);
    }

    /**
     * Bean del caso de uso para aplicar descuento manual (HU-14).
     * Soporta descuentos globales y por ítem.
     * 
     * @param pedidoRepository implementación del repositorio de pedidos
     * @param clock reloj del sistema para timestamp de auditoría
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public AplicarDescuentoManualUseCase aplicarDescuentoManualUseCase(
            PedidoRepository pedidoRepository,
            Clock clock
    ) {
        return new AplicarDescuentoManualUseCase(pedidoRepository, clock);
    }

    /**
     * Bean del caso de uso para gestionar ítems del pedido (HU-20, HU-21).
     * Soporta eliminar ítems y modificar cantidades con recálculo de promociones.
     * 
     * @param pedidoRepository implementación del repositorio de pedidos
     * @param promocionRepository implementación del repositorio de promociones
     * @param motorReglasService servicio de dominio para evaluar promociones
     * @param clock reloj del sistema para evaluación de criterios temporales
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public GestionarItemsPedidoUseCase gestionarItemsPedidoUseCase(
            PedidoRepository pedidoRepository,
            PromocionRepository promocionRepository,
            MotorReglasService motorReglasService,
            Clock clock
    ) {
        return new GestionarItemsPedidoUseCase(
            pedidoRepository,
            promocionRepository,
            motorReglasService,
            clock
        );
    }

    /**
     * HU-14: Bean del caso de uso para reabrir un pedido cerrado.
     * Válvula de escape operativa para corregir errores antes del cierre de caja.
     * 
     * @param pedidoRepository implementación del repositorio de pedidos
     * @param mesaRepository implementación del repositorio de mesas
     * @param clock reloj del sistema para timestamp de reapertura
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public ReabrirPedidoUseCase reabrirPedidoUseCase(
            PedidoRepository pedidoRepository,
            MesaRepository mesaRepository,
            ProductoRepository productoRepository,
            MovimientoStockRepository movimientoStockRepository,
            GestorStockService gestorStockService,
            Clock clock
    ) {
        return new ReabrirPedidoUseCase(pedidoRepository, mesaRepository,
                productoRepository, movimientoStockRepository, gestorStockService, clock);
    }

    /**
     * Bean del caso de uso para registrar egresos de caja.
     * 
     * @param movimientoCajaRepository implementación del repositorio de movimientos
     * @param clock reloj del sistema para timestamp del movimiento
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public RegistrarEgresoUseCase registrarEgresoUseCase(
            MovimientoCajaRepository movimientoCajaRepository,
            Clock clock
    ) {
        return new RegistrarEgresoUseCase(movimientoCajaRepository, clock);
    }

    /**
     * Bean del caso de uso para generar reporte de caja diario (arqueo).
     * 
     * @param pedidoRepository implementación del repositorio de pedidos
     * @param movimientoCajaRepository implementación del repositorio de movimientos
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public GenerarReporteCajaUseCase generarReporteCajaUseCase(
            PedidoRepository pedidoRepository,
            MovimientoCajaRepository movimientoCajaRepository
    ) {
        return new GenerarReporteCajaUseCase(pedidoRepository, movimientoCajaRepository);
    }

    // ============================================
    // HU-22: Gestión de stock
    // ============================================

    /**
     * HU-22: Bean del servicio de dominio para gestión de stock.
     * Es un servicio de dominio puro (sin dependencias de infraestructura).
     */
    @Bean
    public GestorStockService gestorStockService() {
        return new GestorStockService();
    }

    /**
     * HU-22: Bean del caso de uso para ajuste manual de stock.
     */
    @Bean
    public AjustarStockUseCase ajustarStockUseCase(
            ProductoRepository productoRepository,
            MovimientoStockRepository movimientoStockRepository,
            GestorStockService gestorStockService,
            Clock clock
    ) {
        return new AjustarStockUseCase(productoRepository, movimientoStockRepository, gestorStockService, clock);
    }

    // ============================================
    // Categorías del catálogo
    // ============================================

    /**
     * Bean del caso de uso para crear categoría.
     */
    @Bean
    public CrearCategoriaUseCase crearCategoriaUseCase(CategoriaRepository categoriaRepository) {
        return new CrearCategoriaUseCase(categoriaRepository);
    }

    /**
     * Bean del caso de uso para editar categoría.
     */
    @Bean
    public EditarCategoriaUseCase editarCategoriaUseCase(CategoriaRepository categoriaRepository) {
        return new EditarCategoriaUseCase(categoriaRepository);
    }

    /**
     * Bean del caso de uso para consultar categorías de un local.
     */
    @Bean
    public ConsultarCategoriasUseCase consultarCategoriasUseCase(CategoriaRepository categoriaRepository) {
        return new ConsultarCategoriasUseCase(categoriaRepository);
    }

    /**
     * Bean del caso de uso para eliminar categoría.
     */
    @Bean
    public EliminarCategoriaUseCase eliminarCategoriaUseCase(CategoriaRepository categoriaRepository) {
        return new EliminarCategoriaUseCase(categoriaRepository);
    }
}
