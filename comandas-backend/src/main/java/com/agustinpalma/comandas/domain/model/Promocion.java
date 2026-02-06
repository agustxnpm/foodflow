package com.agustinpalma.comandas.domain.model;

import com.agustinpalma.comandas.domain.model.DomainIds.*;
import com.agustinpalma.comandas.domain.model.DomainEnums.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate Root del contexto de Promociones.
 * 
 * Implementa el patrón Specification/Composite Trigger.
 * 
 * Componentes:
 * - **Triggers (Criterios de Activación):** Lista de condiciones que deben cumplirse
 *   simultáneamente (lógica AND) para que la promoción pueda activarse.
 * - **Estrategia (Beneficio):** Define QUÉ beneficio se otorga una vez activada.
 * - **Alcance (Scope):** Define QUÉ productos/categorías participan y CON QUÉ ROL (HU-09).
 * 
 * Separación de responsabilidades:
 * - Los triggers responden: "¿CUÁNDO y BAJO QUÉ CONDICIONES aplico?"
 * - La estrategia responde: "¿QUÉ BENEFICIO otorgo?" (calculado en HU-10)
 * - El alcance responde: "¿A QUÉ productos/categorías aplico?" (HU-09)
 * 
 * HU-09: AlcancePromocion define los productos trigger y target.
 * La aplicación automática a pedidos se realizará en HU-10.
 * 
 * Invariantes:
 * - Nombre no vacío
 * - Prioridad >= 0
 * - Estrategia obligatoria
 * - Al menos un trigger (criterio de activación)
 * - Pertenece a un Local (multi-tenancy)
 * - Unicidad del nombre por local (validada en el use case, no en la entidad)
 */
public class Promocion {

    private final PromocionId id;
    private final LocalId localId;
    private String nombre;
    private String descripcion;
    private int prioridad;
    private EstadoPromocion estado;
    private final EstrategiaPromocion estrategia;
    private final List<CriterioActivacion> triggers;
    private AlcancePromocion alcance; // HU-09

    public Promocion(
            PromocionId id,
            LocalId localId,
            String nombre,
            String descripcion,
            int prioridad,
            EstadoPromocion estado,
            EstrategiaPromocion estrategia,
            List<CriterioActivacion> triggers
    ) {
        this.id = Objects.requireNonNull(id, "El id de la promoción no puede ser null");
        this.localId = Objects.requireNonNull(localId, "El localId no puede ser null");
        this.nombre = validarNombre(nombre);
        this.descripcion = descripcion != null ? descripcion.trim() : null;
        this.prioridad = validarPrioridad(prioridad);
        this.estado = Objects.requireNonNull(estado, "El estado de la promoción no puede ser null");
        this.estrategia = Objects.requireNonNull(estrategia, "La estrategia de la promoción es obligatoria");
        this.triggers = validarTriggers(triggers);
        this.alcance = AlcancePromocion.vacio(); // Inicialmente vacío
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la promoción no puede estar vacío");
        }
        return nombre.trim();
    }

    private int validarPrioridad(int prioridad) {
        if (prioridad < 0) {
            throw new IllegalArgumentException(
                    "La prioridad no puede ser negativa. Recibido: " + prioridad
            );
        }
        return prioridad;
    }

    private List<CriterioActivacion> validarTriggers(List<CriterioActivacion> triggers) {
        if (triggers == null || triggers.isEmpty()) {
            throw new IllegalArgumentException(
                    "La promoción debe tener al menos un criterio de activación (trigger)"
            );
        }
        return new ArrayList<>(triggers);
    }

    /**
     * Evalúa si esta promoción puede activarse dado el contexto actual.
     * 
     * Implementa lógica AND: TODOS los triggers deben satisfacerse simultáneamente.
     * 
     * Patrón Specification: cada trigger encapsula su propia lógica de evaluación.
     * La promoción no conoce los detalles internos de cada criterio, solo pregunta
     * si se satisface.
     * 
     * @param contexto información sobre el pedido (fecha, hora, productos, total, etc.)
     * @return true si TODOS los triggers se cumplen, false si al menos uno falla
     */
    public boolean puedeActivarse(ContextoValidacion contexto) {
        Objects.requireNonNull(contexto, "El contexto de validación no puede ser null");
        
        if (this.estado != EstadoPromocion.ACTIVA) {
            return false;
        }

        // Lógica AND: todos los triggers deben satisfacerse
        return triggers.stream()
                .allMatch(trigger -> trigger.esSatisfechoPor(contexto));
    }

    /**
     * Activa la promoción.
     */
    public void activar() {
        this.estado = EstadoPromocion.ACTIVA;
    }

    /**
     * Desactiva la promoción.
     */
    public void desactivar() {
        this.estado = EstadoPromocion.INACTIVA;
    }

    /**
     * Actualiza el nombre de la promoción.
     */
    public void actualizarNombre(String nuevoNombre) {
        this.nombre = validarNombre(nuevoNombre);
    }

    /**
     * Actualiza la descripción de la promoción.
     */
    public void actualizarDescripcion(String nuevaDescripcion) {
        this.descripcion = nuevaDescripcion != null ? nuevaDescripcion.trim() : null;
    }

    /**
     * Actualiza la prioridad de la promoción.
     */
    public void actualizarPrioridad(int nuevaPrioridad) {
        this.prioridad = validarPrioridad(nuevaPrioridad);
    }

    /**
     * Define el alcance de la promoción (qué productos/categorías participan y con qué rol).
     * HU-09: Asociar productos a promociones.
     */
    public void definirAlcance(AlcancePromocion nuevoAlcance) {
        this.alcance = Objects.requireNonNull(nuevoAlcance, "El alcance no puede ser null");
    }

    // ============================================
    // Getters (sin setters públicos)
    // ============================================

    public PromocionId getId() {
        return id;
    }

    public LocalId getLocalId() {
        return localId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public EstadoPromocion getEstado() {
        return estado;
    }

    public EstrategiaPromocion getEstrategia() {
        return estrategia;
    }

    public List<CriterioActivacion> getTriggers() {
        return Collections.unmodifiableList(triggers);
    }

    public AlcancePromocion getAlcance() {
        return alcance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Promocion promocion = (Promocion) o;
        return Objects.equals(id, promocion.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Promocion{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", estado=" + estado +
                ", estrategia=" + estrategia.getTipo() +
                ", prioridad=" + prioridad +
                ", triggersCount=" + triggers.size() +
                '}';
    }
}
