package com.agustinpalma.comandas.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Configuración de Clock mutable para desarrollo y pruebas de usabilidad.
 * 
 * Reemplaza el Clock fijo de producción con uno que permite "viajar en el tiempo"
 * vía un endpoint REST. Esto habilita simular escenarios operativos completos:
 * 
 * - Cerrar jornada a las 23:00, avanzar al día siguiente, operar de nuevo
 * - Probar turno noche (cierre a la 1am → fecha operativa del día anterior)
 * - Simular doble turno en el mismo día
 * - Verificar comportamiento del Happy Hour en distintos horarios
 * 
 * ⚠️ SOLO ACTIVO con profile "dev". En producción se usa ClockConfig estándar.
 * 
 * Uso desde el frontend:
 *   POST /api/dev/clock { "dateTime": "2026-02-28T23:30:00" }  → fijar hora
 *   POST /api/dev/clock/advance { "hours": 8 }                  → avanzar 8hs
 *   DELETE /api/dev/clock                                        → volver a tiempo real
 *   GET /api/dev/clock                                           → consultar hora actual
 */
@Configuration
@Profile("dev")
public class DevClockConfig {

    private static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    /**
     * Clock mutable compartido entre el bean y el controller.
     * 
     * - null → usa Clock.system (tiempo real)
     * - Instant presente → Clock.fixed en ese instante
     * 
     * AtomicReference para thread-safety (aunque en dev es un solo usuario).
     */
    @Bean
    public AtomicReference<Instant> devClockOverride() {
        return new AtomicReference<>(null);
    }

    /**
     * Clock que delega al override si está seteado, o al sistema si no.
     * 
     * @Primary asegura que este bean gane sobre el de ClockConfig.
     */
    @Bean
    @Primary
    public Clock clock(AtomicReference<Instant> devClockOverride) {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return ARGENTINA_ZONE;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                // Los use cases no cambian la zona, pero cumplimos el contrato
                return this;
            }

            @Override
            public Instant instant() {
                Instant override = devClockOverride.get();
                return override != null ? override : Instant.now();
            }
        };
    }
}
