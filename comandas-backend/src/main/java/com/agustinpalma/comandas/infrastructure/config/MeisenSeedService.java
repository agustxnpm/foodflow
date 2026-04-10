package com.agustinpalma.comandas.infrastructure.config;

import com.agustinpalma.comandas.infrastructure.persistence.entity.LocalEntity;
import com.agustinpalma.comandas.infrastructure.persistence.entity.MesaEntity;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataLocalRepository;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataMesaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de inicialización del local y mesa inicial.
 *
 * Invocado exclusivamente desde IdentityController durante la activación.
 * Es idempotente: ejecutar múltiples veces no duplica datos.
 */
@Service
public class MeisenSeedService {

    private static final Logger log = LoggerFactory.getLogger(MeisenSeedService.class);

    private final SpringDataLocalRepository localRepository;
    private final SpringDataMesaRepository mesaRepository;

    public MeisenSeedService(
            SpringDataLocalRepository localRepository,
            SpringDataMesaRepository mesaRepository
    ) {
        this.localRepository = localRepository;
        this.mesaRepository = mesaRepository;
    }

    /**
     * Inicializa el local y la primera mesa si no existen.
     * Idempotente: si el local ya existe, no hace nada.
     *
     * @param localId    UUID del tenant
     * @param nombreLocal nombre del local para la entidad
     */
    @Transactional
    public void inicializarLocal(UUID localId, String nombreLocal) {
        if (localRepository.existsById(localId)) {
            log.info("Sistema ya inicializado para Local ID: {}", localId);
            return;
        }

        log.info("Primera ejecución detectada. Inicializando datos para Local ID: {}", localId);

        LocalEntity local = new LocalEntity();
        local.setId(localId);
        local.setNombre(nombreLocal);
        localRepository.save(local);

        MesaEntity mesa = new MesaEntity();
        mesa.setId(UUID.randomUUID());
        mesa.setLocalId(localId);
        mesa.setNumero(1);
        mesa.setEstado(com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa.LIBRE);
        mesaRepository.save(mesa);

        log.info("Sistema inicializado para Local ID: {}", localId);
    }
}
