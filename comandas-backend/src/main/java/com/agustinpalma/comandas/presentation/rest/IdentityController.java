package com.agustinpalma.comandas.presentation.rest;

import com.agustinpalma.comandas.infrastructure.config.LocalConfigService;
import com.agustinpalma.comandas.infrastructure.config.MeisenSeedService;
import com.agustinpalma.comandas.infrastructure.persistence.entity.LocalConfigEntity;
import com.agustinpalma.comandas.infrastructure.persistence.jpa.SpringDataLocalConfigRepository;
import com.agustinpalma.comandas.presentation.dto.ActivationRequestDTO;
import com.agustinpalma.comandas.presentation.dto.IdentityResponseDTO;
import com.agustinpalma.comandas.presentation.dto.UpdateLocalDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller REST para identidad y activación del local.
 *
 * Gestiona el flujo de "primer arranque":
 * - GET  /api/config/identity → consulta si el sistema está activado
 * - POST /api/config/identity → activa el sistema con UUID + datos del local
 *
 * NOTA: Estos endpoints NO requieren el header X-Local-Id,
 * ya que se usan antes de que la identidad exista.
 */
@RestController
@RequestMapping("/api/config")
public class IdentityController {

    private static final Logger log = LoggerFactory.getLogger(IdentityController.class);

    private static final String KEY_LOCAL_ID = "local-id";
    private static final String KEY_NOMBRE_LOCAL = "nombre-local";
    private static final String KEY_DIRECCION = "direccion";
    private static final String KEY_TELEFONO = "telefono";
    private static final String KEY_CUIT = "cuit";
    private static final String KEY_MENSAJE_BIENVENIDA = "mensaje-bienvenida";

    private final SpringDataLocalConfigRepository configRepository;
    private final MeisenSeedService seedService;
    private final LocalConfigService localConfigService;

    public IdentityController(
            SpringDataLocalConfigRepository configRepository,
            MeisenSeedService seedService,
            LocalConfigService localConfigService
    ) {
        this.configRepository = configRepository;
        this.seedService = seedService;
        this.localConfigService = localConfigService;
    }

    /**
     * Consulta si el sistema tiene identidad configurada.
     *
     * GET /api/config/identity
     *
     * @return configured=true con datos del local, o configured=false
     */
    @GetMapping("/identity")
    public ResponseEntity<IdentityResponseDTO> obtenerIdentidad() {
        var optLocalId = configRepository.findById(KEY_LOCAL_ID);

        if (optLocalId.isEmpty()) {
            return ResponseEntity.ok(IdentityResponseDTO.noConfigurado());
        }

        Map<String, String> config = configRepository.findAll().stream()
                .collect(Collectors.toMap(LocalConfigEntity::getKey, LocalConfigEntity::getValue));

        return ResponseEntity.ok(IdentityResponseDTO.configurado(
                config.get(KEY_LOCAL_ID),
                config.getOrDefault(KEY_NOMBRE_LOCAL, ""),
                config.getOrDefault(KEY_DIRECCION, ""),
                config.getOrDefault(KEY_TELEFONO, ""),
                config.getOrDefault(KEY_CUIT, ""),
                config.getOrDefault(KEY_MENSAJE_BIENVENIDA, "")
        ));
    }

    /**
     * Activa el sistema con un código de activación (UUID) y los datos del local.
     *
     * POST /api/config/identity
     *
     * Idempotencia: si el mismo localId ya existe, retorna 200 OK sin duplicar.
     * Si existe otro localId diferente, rechaza con 409 Conflict.
     *
     * @param request DTO con localId, nombreLocal y datos opcionales
     * @return 201 Created con el localId confirmado, o 200 si ya existía
     */
    @PostMapping("/identity")
    @Transactional
    public ResponseEntity<?> activarIdentidad(@RequestBody ActivationRequestDTO request) {
        UUID localUuid;
        try {
            localUuid = UUID.fromString(request.localId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El código de activación no es un UUID válido"));
        }

        // Idempotencia: si ya existe con el mismo UUID, retornar OK
        var existente = configRepository.findById(KEY_LOCAL_ID);
        if (existente.isPresent()) {
            if (existente.get().getValue().equals(localUuid.toString())) {
                log.info("Activación idempotente: el local {} ya está configurado", localUuid);
                return ResponseEntity.ok(Map.of("localId", localUuid.toString()));
            }
            log.warn("Intento de reactivación con UUID diferente. Existente: {}, Nuevo: {}",
                    existente.get().getValue(), localUuid);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El sistema ya fue activado con otro código"));
        }

        // Persistir configuración como filas key-value
        guardarConfig(KEY_LOCAL_ID, localUuid.toString());
        guardarConfig(KEY_NOMBRE_LOCAL, request.nombreLocal().trim());
        guardarConfig(KEY_DIRECCION, valorODefault(request.direccion()));
        guardarConfig(KEY_TELEFONO, valorODefault(request.telefono()));
        guardarConfig(KEY_CUIT, valorODefault(request.cuit()));
        guardarConfig(KEY_MENSAJE_BIENVENIDA,
                request.mensajeBienvenida() != null && !request.mensajeBienvenida().isBlank()
                        ? request.mensajeBienvenida().trim()
                        : "¡Gracias por su visita!");

        // Seed: crear LocalEntity + MesaEntity mesa 1
        seedService.inicializarLocal(localUuid, request.nombreLocal().trim());

        // Recargar caché para que el resto de la app vea los nuevos datos
        localConfigService.recargar();

        log.info("Sistema activado exitosamente para Local ID: {} ({})", localUuid, request.nombreLocal());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("localId", localUuid.toString()));
    }

    private void guardarConfig(String key, String value) {
        configRepository.save(new LocalConfigEntity(key, value));
    }

    private String valorODefault(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : "";
    }

    /**
     * Actualiza los datos del local (post-activación).
     *
     * PUT /api/config/identity
     *
     * No permite cambiar el localId — solo los datos descriptivos.
     * Requiere que el sistema esté previamente activado (409 si no lo está).
     *
     * @param request DTO con nombreLocal (obligatorio) y datos opcionales
     * @return 200 OK con los datos actualizados
     */
    @PutMapping("/identity")
    @Transactional
    public ResponseEntity<?> actualizarDatosLocal(@RequestBody UpdateLocalDataDTO request) {
        var existente = configRepository.findById(KEY_LOCAL_ID);
        if (existente.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El sistema no está activado. Activá primero con POST /api/config/identity"));
        }

        guardarConfig(KEY_NOMBRE_LOCAL, request.nombreLocal().trim());
        guardarConfig(KEY_DIRECCION, valorODefault(request.direccion()));
        guardarConfig(KEY_TELEFONO, valorODefault(request.telefono()));
        guardarConfig(KEY_CUIT, valorODefault(request.cuit()));
        guardarConfig(KEY_MENSAJE_BIENVENIDA,
                request.mensajeBienvenida() != null && !request.mensajeBienvenida().isBlank()
                        ? request.mensajeBienvenida().trim()
                        : "¡Gracias por su visita!");

        localConfigService.recargar();

        log.info("Datos del local actualizados: {}", request.nombreLocal().trim());

        return ResponseEntity.ok(Map.of("mensaje", "Datos del local actualizados correctamente"));
    }
}
