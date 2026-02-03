package com.agustinpalma.comandas.infrastructure.config;

import com.agustinpalma.comandas.application.usecase.AbrirMesaUseCase;
import com.agustinpalma.comandas.application.usecase.CerrarMesaUseCase;
import com.agustinpalma.comandas.application.usecase.ConsultarMesasUseCase;
import com.agustinpalma.comandas.domain.repository.MesaRepository;
import com.agustinpalma.comandas.domain.repository.PedidoRepository;
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
}

