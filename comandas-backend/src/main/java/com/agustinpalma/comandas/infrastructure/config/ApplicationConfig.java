package com.agustinpalma.comandas.infrastructure.config;

import com.agustinpalma.comandas.application.usecase.AbrirMesaUseCase;
import com.agustinpalma.comandas.application.usecase.AgregarProductoUseCase;
import com.agustinpalma.comandas.application.usecase.CerrarMesaUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarDetallePedidoUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarMesasUseCase;
import com.agustinpalma.comandas.application.usecase.CrearMesaUseCase;
import com.agustinpalma.comandas.application.usecase.EliminarMesaUseCase;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
import com.agustinpalma.comandas.domain.repository.ProductoRepository;
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
}

