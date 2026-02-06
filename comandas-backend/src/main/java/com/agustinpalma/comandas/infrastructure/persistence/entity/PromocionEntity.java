package com.agustinpalma.comandas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad JPA para la tabla 'promociones'.
 * 
 * Decisión de persistencia refactorizada:
 * - Los triggers (criterios de activación) se persisten en JSONB (triggers_json)
 *   porque son polimórficos, extensibles y de tamaño variable.
 * - La estrategia sigue aplanada porque hay solo 3 tipos conocidos y fijos.
 * 
 * Esto balancea flexibilidad (triggers) con simplicidad (estrategia).
 */
@Entity
@Table(name = "promociones")
public class PromocionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "local_id", nullable = false)
    private UUID localId;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "prioridad", nullable = false)
    private int prioridad;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    // ============================================
    // Estrategia (aplanada)
    // ============================================

    @Column(name = "tipo_estrategia", nullable = false, length = 30)
    private String tipoEstrategia;

    /** Solo para DESCUENTO_DIRECTO: PORCENTAJE o MONTO_FIJO */
    @Column(name = "modo_descuento", length = 20)
    private String modoDescuento;

    /** Solo para DESCUENTO_DIRECTO: el valor del descuento */
    @Column(name = "valor_descuento", precision = 10, scale = 2)
    private BigDecimal valorDescuento;

    /** Solo para CANTIDAD_FIJA: cuántos se lleva */
    @Column(name = "cantidad_llevas")
    private Integer cantidadLlevas;

    /** Solo para CANTIDAD_FIJA: cuántos paga */
    @Column(name = "cantidad_pagas")
    private Integer cantidadPagas;

    /** Solo para COMBO_CONDICIONAL: cantidad mínima del trigger */
    @Column(name = "cantidad_minima_trigger")
    private Integer cantidadMinimaTrigger;

    /** Solo para COMBO_CONDICIONAL: porcentaje de beneficio sobre el target */
    @Column(name = "porcentaje_beneficio", precision = 5, scale = 2)
    private BigDecimal porcentajeBeneficio;

    // ============================================
    // Triggers (JSONB)
    // ============================================

    /**
     * Array de criterios de activación serializados en JSON.
     * 
     * Estructura: [{tipo, ...campos específicos}, {...}]
     * 
     * Ejemplo:
     * [
     *   {"tipo": "TEMPORAL", "fechaDesde": "2026-02-01", "fechaHasta": "2026-02-28", ...},
     *   {"tipo": "CONTENIDO", "productosRequeridos": ["uuid1", "uuid2"]}
     * ]
     */
    @Column(name = "triggers_json", nullable = false, columnDefinition = "TEXT")
    private String triggersJson;

    // ============================================
    // Constructores
    // ============================================

    public PromocionEntity() {
    }

    // ============================================
    // Getters y Setters
    // ============================================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getLocalId() { return localId; }
    public void setLocalId(UUID localId) { this.localId = localId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTipoEstrategia() { return tipoEstrategia; }
    public void setTipoEstrategia(String tipoEstrategia) { this.tipoEstrategia = tipoEstrategia; }

    public String getModoDescuento() { return modoDescuento; }
    public void setModoDescuento(String modoDescuento) { this.modoDescuento = modoDescuento; }

    public BigDecimal getValorDescuento() { return valorDescuento; }
    public void setValorDescuento(BigDecimal valorDescuento) { this.valorDescuento = valorDescuento; }

    public Integer getCantidadLlevas() { return cantidadLlevas; }
    public void setCantidadLlevas(Integer cantidadLlevas) { this.cantidadLlevas = cantidadLlevas; }

    public Integer getCantidadPagas() { return cantidadPagas; }
    public void setCantidadPagas(Integer cantidadPagas) { this.cantidadPagas = cantidadPagas; }

    public Integer getCantidadMinimaTrigger() { return cantidadMinimaTrigger; }
    public void setCantidadMinimaTrigger(Integer cantidadMinimaTrigger) { this.cantidadMinimaTrigger = cantidadMinimaTrigger; }

    public BigDecimal getPorcentajeBeneficio() { return porcentajeBeneficio; }
    public void setPorcentajeBeneficio(BigDecimal porcentajeBeneficio) { this.porcentajeBeneficio = porcentajeBeneficio; }

    public String getTriggersJson() { return triggersJson; }
    public void setTriggersJson(String triggersJson) { this.triggersJson = triggersJson; }
}
