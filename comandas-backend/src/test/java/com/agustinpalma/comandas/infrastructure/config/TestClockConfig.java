package com.agustinpalma.comandas.infrastructure.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Configuración de Clock para tests.
 * 
 * Provee un Clock fijo para hacer los tests deterministas.
 * El @Primary garantiza que este bean sobrescriba el Clock de producción en contexto de tests.
 * 
 * Por defecto, el clock está fijado a:
 * - Fecha: 6 de febrero de 2026 (Jueves)
 * - Hora: 19:00 (dentro del rango de Happy Hour 18:00-20:00)
 * - Zona: America/Argentina/Buenos_Aires
 * 
 * Esto permite validar promociones con triggers temporales de forma determinista.
 */
@TestConfiguration
public class TestClockConfig {

    /**
     * Zona horaria de prueba: Argentina/Buenos Aires (UTC-3).
     */
    private static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    /**
     * Fecha y hora fija para tests: 6 de febrero de 2026, 19:00 (Jueves).
     * 
     * Esta fecha cumple:
     * - Jueves: valida promociones por día de la semana
     * - 19:00: dentro de Happy Hour (18:00-20:00)
     * - Febrero 2026: fecha coherente con el contexto del proyecto
     * 
     * @return Clock fijo para testing determinista
     */
    @Bean
    @Primary
    public Clock testClock() {
        LocalDateTime fixedDateTime = LocalDateTime.of(2026, 2, 6, 19, 0, 0);
        return Clock.fixed(
            fixedDateTime.atZone(ARGENTINA_ZONE).toInstant(),
            ARGENTINA_ZONE
        );
    }
}
