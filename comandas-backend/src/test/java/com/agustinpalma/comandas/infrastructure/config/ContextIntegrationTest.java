package com.agustinpalma.comandas.infrastructure.config;

import com.agustinpalma.comandas.application.ports.output.LocalContextProvider;
import com.agustinpalma.comandas.domain.model.DomainIds.LocalId;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataLocalRepository;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataMesaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para verificar la inicialización del contexto del local.
 *
 * Valida que:
 * - LocalContextProvider se inyecta correctamente desde la configuración
 * - El LocalId configurado coincide con el esperado
 * - MeisenSeedService se ejecuta al arrancar e inicializa la base de datos
 * - La base queda con datos del local (al menos 1 mesa)
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class ContextIntegrationTest {

    private static final UUID EXPECTED_LOCAL_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Autowired
    private LocalContextProvider localContextProvider;

    @Autowired
    private SpringDataLocalRepository localRepository;

    @Autowired
    private SpringDataMesaRepository mesaRepository;

    @Test
    void localContextProvider_debe_estar_inyectado() {
        assertNotNull(localContextProvider, "LocalContextProvider debe estar disponible en el contexto de Spring");
    }

    @Test
    void localContextProvider_debe_retornar_el_localId_configurado() {
        LocalId localId = localContextProvider.getCurrentLocalId();

        assertNotNull(localId);
        assertEquals(EXPECTED_LOCAL_UUID, localId.getValue());
    }

    @Test
    void seed_debe_crear_el_local_en_base_de_datos() {
        assertTrue(
            localRepository.existsById(EXPECTED_LOCAL_UUID),
            "El local configurado debe existir en la base de datos después del seed"
        );
    }

    @Test
    void seed_debe_crear_al_menos_una_mesa() {
        long cantidadMesas = mesaRepository.findByLocalIdOrderByNumeroAsc(EXPECTED_LOCAL_UUID).size();

        assertTrue(
            cantidadMesas > 0,
            "Debe existir al menos una mesa después de la inicialización. Encontradas: " + cantidadMesas
        );
    }

    @Test
    void seed_debe_ser_idempotente() {
        long mesasAntes = mesaRepository.findByLocalIdOrderByNumeroAsc(EXPECTED_LOCAL_UUID).size();

        // Simular segunda ejecución del seed
        MeisenSeedService seedService = new MeisenSeedService(
            new MeisenProperties() {{
                setLocalId(EXPECTED_LOCAL_UUID);
            }},
            localRepository,
            mesaRepository
        );
        seedService.run();

        long mesasDespues = mesaRepository.findByLocalIdOrderByNumeroAsc(EXPECTED_LOCAL_UUID).size();

        assertEquals(
            mesasAntes,
            mesasDespues,
            "Ejecutar el seed dos veces no debe duplicar datos"
        );
    }
}
