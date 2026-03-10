package com.agustinpalma.comandas.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Filtro de logging para todas las peticiones HTTP entrantes.
 *
 * Registra en stdout (capturado por Tauri) y en el logger:
 * - URL y método HTTP
 * - Body de la petición (JSON recibido)
 * - Código de estado de la respuesta
 * - Tiempo de procesamiento en ms
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Leer y cachear el body para poder loguearlo sin consumir el stream
        byte[] bodyBytes = request.getInputStream().readAllBytes();
        CachedBodyRequestWrapper wrappedRequest = new CachedBodyRequestWrapper(request, bodyBytes);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String method = wrappedRequest.getMethod();
            String uri = wrappedRequest.getRequestURI();
            String queryString = wrappedRequest.getQueryString();
            String fullUrl = queryString != null ? uri + "?" + queryString : uri;

            String requestBody = bodyBytes.length > 0
                    ? new String(bodyBytes, StandardCharsets.UTF_8)
                    : "(sin body)";

            String logLine = String.format("[HTTP] %s %s | Body: %s | Status: %d | %dms",
                    method, fullUrl, requestBody, status, duration);

            if (status >= 500) {
                System.out.println("⚠️ " + logLine);
                logger.error(logLine);
            } else if (status >= 400) {
                System.out.println("⚠ " + logLine);
                logger.warn(logLine);
            } else {
                logger.info(logLine);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/favicon") || path.startsWith("/actuator");
    }

    /**
     * Wrapper que permite releer el body de la request después de haberlo consumido para logging.
     */
    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        CachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.cachedBody = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() { return bais.available() == 0; }

                @Override
                public boolean isReady() { return true; }

                @Override
                public void setReadListener(ReadListener listener) {
                    // No-op para implementaciones síncronas
                }

                @Override
                public int read() { return bais.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
