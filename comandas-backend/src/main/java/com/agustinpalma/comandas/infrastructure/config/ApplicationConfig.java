package com.agustinpalma.comandas.infrastructure.config;

import com.agustinpalma.comandas.application.usecase.AbrirMesaUseCase;
import com.agustinpalma.comandas.application.usecase.AgregarProductoUseCase;
import com.agustinpalma.comandas.application.usecase.AsociarProductoAPromocionUseCase;
import com.agustinpalma.comandas.application.usecase.CerrarMesaUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarDetallePedidoUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarMesasUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarProductosUseCase;
import com.agustinpalma.comandas.application.usecase.CrearMesaUseCase;
import com.agustinpalma.comandas.application.usecase.CrearProductoUseCase;
import com.agustinpalma.comandas.application.usecase.EditarProductoUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarMesaUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarProductoUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarPromocionesUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarPromocionUseCase;
import com.agustinpalma.comandas.application.usecase.CrearPromocionUseCase;
import com.agustinpalma.comandas.application.usecase.EditarPromocionUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarPromocionUseCase;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
import com.agustinpalma.comandas.domain.repository.PromocionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
     * Spring inyectará automáticamente las implementaciones JPA de los repositorios.
     *
     * @param mesaRepository implementación del repositorio de mesas
     * @param pedidoRepository implementación del repositorio de pedidos
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public CerrarMesaUseCase cerrarMesaUseCase(MesaRepository mesaRepository, PedidoRepository pedidoRepository) {
        return new CerrarMesaUseCase(mesaRepository, pedidoRepository);
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
     * Spring inyectará automáticamente las implementaciones JPA de los repositorios.
     *
     * @param pedidoRepository implementación del repositorio de pedidos
     * @param productoRepository implementación del repositorio de productos
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public AgregarProductoUseCase agregarProductoUseCase(PedidoRepository pedidoRepository, ProductoRepository productoRepository) {
        return new AgregarProductoUseCase(pedidoRepository, productoRepository);
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
     * Spring inyectará automáticamente la implementación JPA del repositorio.
     *
     * @param productoRepository implementación del repositorio de productos
     * @return instancia del caso de uso lista para usar
     */
    @Bean
    public ConsultarProductosUseCase consultarProductosUseCase(ProductoRepository productoRepository) {
        return new ConsultarProductosUseCase(productoRepository);
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
     * Bean del caso de uso para asociar productos a una promoción (HU-09).
     */
    @Bean
    public AsociarProductoAPromocionUseCase asociarProductoAPromocionUseCase(
            PromocionRepository promocionRepository,
            ProductoRepository productoRepository
    ) {
        return new AsociarProductoAPromocionUseCase(promocionRepository, productoRepository);
    }
}

