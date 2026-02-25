package com.agustinpalma.comandas.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad JPA para productos.
 * Representa la tabla de productos en la base de datos.
 */
@Entity
@Table(name = "productos")
public class ProductoEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "local_id", nullable = false)
    private UUID localId;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(name = "grupo_variante_id")
    private UUID grupoVarianteId;

    @Column(name = "es_extra", nullable = false)
    private boolean esExtra = false;

    @Column(name = "es_modificador_estructural", nullable = false)
    private boolean esModificadorEstructural = false;

    @Column(name = "cantidad_discos_carne")
    private Integer cantidadDiscosCarne;

    // Clasificación de catálogo
    @Column(name = "categoria_id")
    private UUID categoriaId;

    @Column(name = "permite_extras", nullable = false)
    private boolean permiteExtras = true;

    // Control de flujo POS
    @Column(name = "requiere_configuracion", nullable = false)
    private boolean requiereConfiguracion = true;

    // HU-22: Gestión de stock
    @Column(name = "stock_actual", nullable = false)
    private int stockActual = 0;

    @Column(name = "controla_stock", nullable = false)
    private boolean controlaStock = false;

    // Constructor vacío para JPA
    public ProductoEntity() {}

    // Constructor con parámetros
    public ProductoEntity(UUID id, UUID localId, String nombre, BigDecimal precio, boolean activo, String colorHex,
                         UUID grupoVarianteId, boolean esExtra, boolean esModificadorEstructural,
                         Integer cantidadDiscosCarne,
                         UUID categoriaId, boolean permiteExtras,
                         boolean requiereConfiguracion, int stockActual, boolean controlaStock) {
        this.id = id;
        this.localId = localId;
        this.nombre = nombre;
        this.precio = precio;
        this.activo = activo;
        this.colorHex = colorHex;
        this.grupoVarianteId = grupoVarianteId;
        this.esExtra = esExtra;
        this.esModificadorEstructural = esModificadorEstructural;
        this.cantidadDiscosCarne = cantidadDiscosCarne;
        this.categoriaId = categoriaId;
        this.permiteExtras = permiteExtras;
        this.requiereConfiguracion = requiereConfiguracion;
        this.stockActual = stockActual;
        this.controlaStock = controlaStock;
    }

    // Getters y setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLocalId() {
        return localId;
    }

    public void setLocalId(UUID localId) {
        this.localId = localId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public UUID getGrupoVarianteId() {
        return grupoVarianteId;
    }

    public void setGrupoVarianteId(UUID grupoVarianteId) {
        this.grupoVarianteId = grupoVarianteId;
    }

    public boolean isEsExtra() {
        return esExtra;
    }

    public void setEsExtra(boolean esExtra) {
        this.esExtra = esExtra;
    }

    public boolean isEsModificadorEstructural() {
        return esModificadorEstructural;
    }

    public void setEsModificadorEstructural(boolean esModificadorEstructural) {
        this.esModificadorEstructural = esModificadorEstructural;
    }

    public Integer getCantidadDiscosCarne() {
        return cantidadDiscosCarne;
    }

    public void setCantidadDiscosCarne(Integer cantidadDiscosCarne) {
        this.cantidadDiscosCarne = cantidadDiscosCarne;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(UUID categoriaId) {
        this.categoriaId = categoriaId;
    }

    public boolean isPermiteExtras() {
        return permiteExtras;
    }

    public void setPermiteExtras(boolean permiteExtras) {
        this.permiteExtras = permiteExtras;
    }

    public boolean isRequiereConfiguracion() {
        return requiereConfiguracion;
    }

    public void setRequiereConfiguracion(boolean requiereConfiguracion) {
        this.requiereConfiguracion = requiereConfiguracion;
    }

    public int getStockActual() {
        return stockActual;
    }

    public void setStockActual(int stockActual) {
        this.stockActual = stockActual;
    }

    public boolean isControlaStock() {
        return controlaStock;
    }

    public void setControlaStock(boolean controlaStock) {
        this.controlaStock = controlaStock;
    }
}
