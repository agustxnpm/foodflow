package com.agustinpalma.comandas.infrastructure.config;

import com.agustinpalma.comandas.infrastructure.persistence.entity.LocalEntity;
import com.agustinpalma.comandas.infrastructure.persistence.entity.MesaEntity;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataLocalRepository;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataMesaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Servicio de inicialización que se ejecuta al arrancar la aplicación.
 *
 * Comportamiento:
 * - Lee el LocalId configurado en application.yml
 * - Verifica si la base de datos ya fue inicializada para ese local
 * - Si es la primera ejecución, crea el local y una mesa inicial
 * - Es idempotente: ejecutar múltiples veces no duplica datos
 */
@Component
public class MeisenSeedService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MeisenSeedService.class);

    private final MeisenProperties properties;
    private final SpringDataLocalRepository localRepository;
    private final SpringDataMesaRepository mesaRepository;

    public MeisenSeedService(
            MeisenProperties properties,
            SpringDataLocalRepository localRepository,
            SpringDataMesaRepository mesaRepository
    ) {
        this.properties = properties;
        this.localRepository = localRepository;
        this.mesaRepository = mesaRepository;
    }

    @Override
    public void run(String... args) {
        UUID localUuid = properties.getLocalId();

        if (localRepository.existsById(localUuid)) {
            log.info("Sistema ya inicializado para Local ID: {}", localUuid);
            log.info("NOTA: Productos creados manualmente sin control de stock inicial (controlaStock=false). " +
                    "El control se activará automáticamente al realizar el primer ajuste manual de inventario (INGRESO_MERCADERIA o AJUSTE_MANUAL).");
            return;
        }

        log.info("Primera ejecución detectada. Inicializando datos para Local ID: {}", localUuid);

        LocalEntity local = new LocalEntity();
        local.setId(localUuid);
        local.setNombre("Mi Local");
        localRepository.save(local);

        MesaEntity mesa = new MesaEntity();
        mesa.setId(UUID.randomUUID());
        mesa.setLocalId(localUuid);
        mesa.setNumero(1);
        mesa.setEstado(com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa.LIBRE);
        mesaRepository.save(mesa);

        log.info("Sistema inicializado para Local ID: {}", localUuid);
        log.info("IMPORTANTE: Los productos se crean sin control de stock por defecto (controlaStock=false, stockActual=0). " +
                "Para activar el control de inventario, realice un ajuste manual con INGRESO_MERCADERIA o AJUSTE_MANUAL.");
    }
}
