package com.agustinpalma.comandas.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad (TEMPORAL - SOLO DESARROLLO).
 * Deshabilita autenticación para permitir testing del endpoint.
 * 
 * CORS habilitado para los orígenes de Tauri (producción) y Vite (desarrollo).
 * El filtro de CORS se integra dentro del SecurityFilterChain para que los
 * preflight OPTIONS se procesen antes del filtro de autorización.
 * 
 * TODO: Implementar autenticación JWT/OAuth2 en producción.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        
        return http.build();
    }

    /**
     * Configuración CORS para todos los endpoints /api/**.
     *
     * Orígenes permitidos:
     * - http://tauri.localhost → WebView2 en Windows (producción NSIS)
     * - tauri://localhost      → WebView en macOS/Linux (producción)
     * - http://localhost:3000  → Vite dev server (desarrollo)
     * - http://localhost:5173  → Vite dev server (puerto default alternativo)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://tauri.localhost",
            "tauri://localhost",
            "http://localhost:3000",
            "http://localhost:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
