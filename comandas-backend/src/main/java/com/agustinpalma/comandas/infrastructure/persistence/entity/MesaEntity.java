package com.agustinpalma.comandas.infrastructure.persistence.entity;

import com.agustinpalma.comandas.domain.model.DomainEnums.EstadoMesa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidad JPA para persistencia de mesas.
 * Representa la estructura de la tabla en la base de datos.
 * NO es la entidad de dominio - vive exclusivamente en la capa de infraestructura.
 */
@Entity
@Table(name = "mesas",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_mesa_local_numero",
        columnNames = {"local_id", "numero"}
    ),
    indexes = {
        @Index(name = "idx_mesa_local_id", columnList = "local_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MesaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "local_id", nullable = false)
    private UUID localId;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoMesa estado;
}
