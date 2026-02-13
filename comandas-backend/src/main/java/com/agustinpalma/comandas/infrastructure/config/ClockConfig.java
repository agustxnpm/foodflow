package com.agustinpalma.comandas.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Configuración del reloj del sistema.
 * 
 * Garantiza que todas las operaciones temporales del sistema usen la zona horaria de Argentina.
 * Esto es crítico para:
 * - Promociones con franjas horarias (Happy Hour)
 * - Promociones por día de la semana
 * - Auditoría temporal consistente
 * 
 * En tests, este bean puede ser sobrescrito con Clock.fixed() para determinismo.
 */
@Configuration
@Profile("!test")
public class ClockConfig {

    /**
     * Zona horaria oficial del sistema: Argentina/Buenos Aires (UTC-3).
     */
    private static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    /**
     * Provee un Clock configurado para la zona horaria de Argentina.
     * 
     * Este bean es inyectado en todos los Use Cases que requieren timestamp actual.
     * Permite tener control centralizado sobre la zona horaria del sistema.
     * 
     * @return Clock del sistema en zona horaria de Argentina
     */
    @Bean
    public Clock clock() {
        return Clock.system(ARGENTINA_ZONE);
    }
}
