package com.agustinpalma.comandas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para configuración por local.
 * Almacena preferencias operativas como la impresora seleccionada.
 */
@Entity
@Table(name = "configuracion_local")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionLocalEntity {

    @Id
    @Column(name = "local_id", nullable = false)
    private UUID localId;

    @Column(name = "impresora_predeterminada")
    private String impresoraPredeterminada;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public ConfiguracionLocalEntity(UUID localId, String impresoraPredeterminada) {
        this.localId = localId;
        this.impresoraPredeterminada = impresoraPredeterminada;
        this.fechaActualizacion = LocalDateTime.now();
    }
}
