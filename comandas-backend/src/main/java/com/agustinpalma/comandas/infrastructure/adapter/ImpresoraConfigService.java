package com.agustinpalma.comandas.infrastructure.adapter;

import com.agustinpalma.comandas.infrastructure.persistence.entity.ConfiguracionLocalEntity;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataConfiguracionLocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de infraestructura para descubrimiento de impresoras del SO
 * y persistencia de la impresora predeterminada por local.
 *
 * Usa javax.print.PrintServiceLookup para listar impresoras instaladas
 * en el sistema operativo anfitrión (Windows Spooler / CUPS en Linux).
 */
@Service
public class ImpresoraConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ImpresoraConfigService.class);

    private final SpringDataConfiguracionLocalRepository configRepository;

    public ImpresoraConfigService(SpringDataConfiguracionLocalRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * Lista los nombres de todas las impresoras detectadas por el SO.
     * Si no hay impresoras instaladas (ej: entorno sin drivers), devuelve lista vacía.
     */
    public List<String> listarImpresoras() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

        if (services == null || services.length == 0) {
            logger.warn("[Impresoras] No se detectaron impresoras instaladas en el sistema");
            return List.of();
        }

        List<String> nombres = Arrays.stream(services)
                .map(PrintService::getName)
                .toList();

        logger.info("[Impresoras] Detectadas {} impresoras: {}", nombres.size(), nombres);
        return nombres;
    }

    /**
     * Guarda el nombre de la impresora predeterminada para un local.
     * Si ya existe configuración, la actualiza (upsert).
     */
    @Transactional
    public void guardarImpresoraPredeterminada(UUID localId, String nombreImpresora) {
        Optional<ConfiguracionLocalEntity> existente = configRepository.findById(localId);

        ConfiguracionLocalEntity config;
        if (existente.isPresent()) {
            config = existente.get();
            config.setImpresoraPredeterminada(nombreImpresora);
            config.setFechaActualizacion(LocalDateTime.now());
        } else {
            config = new ConfiguracionLocalEntity(localId, nombreImpresora);
        }

        configRepository.save(config);
        logger.info("[Impresoras] Impresora predeterminada guardada para local {}: '{}'",
                localId, nombreImpresora);
    }

    /**
     * Obtiene el nombre de la impresora predeterminada de un local.
     * Retorna null si no hay configuración.
     */
    @Transactional(readOnly = true)
    public String obtenerImpresoraPredeterminada(UUID localId) {
        return configRepository.findById(localId)
                .map(ConfiguracionLocalEntity::getImpresoraPredeterminada)
                .orElse(null);
    }

    /**
     * Busca la impresora del SO por nombre y envía bytes RAW (ESC/POS) al spooler.
     * Requiere que haya una impresora configurada previamente.
     *
     * @throws IllegalStateException si no hay impresora configurada o no se encuentra en el SO
     */
    public void imprimirViaSpooler(UUID localId, byte[] payload) {
        String nombreImpresora = obtenerImpresoraPredeterminada(localId);

        if (nombreImpresora == null || nombreImpresora.isBlank()) {
            throw new IllegalStateException("No hay una impresora configurada. " +
                    "Seleccioná una impresora desde Ajustes.");
        }

        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService target = null;

        if (services != null) {
            for (PrintService ps : services) {
                if (ps.getName().equals(nombreImpresora)) {
                    target = ps;
                    break;
                }
            }
        }

        if (target == null) {
            throw new IllegalStateException(
                    "La impresora '" + nombreImpresora + "' no fue encontrada en el sistema. " +
                    "Verificá que esté encendida y conectada, o seleccioná otra desde Ajustes.");
        }

        try {
            javax.print.Doc doc = new javax.print.SimpleDoc(
                    payload,
                    javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE,
                    null
            );
            javax.print.DocPrintJob job = target.createPrintJob();
            job.print(doc, null);
            logger.info("[Impresoras] Impresión enviada a '{}' ({} bytes)", nombreImpresora, payload.length);
        } catch (javax.print.PrintException e) {
            logger.error("[Impresoras] Error al imprimir en '{}': {}", nombreImpresora, e.getMessage());
            throw new IllegalStateException(
                    "Error al enviar impresión a '" + nombreImpresora + "': " + e.getMessage());
        }
    }
}
