package com.agustinpalma.comandas.infrastructure.adapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generador de comandos ESC/POS para impresoras térmicas de 80mm.
 *
 * Construye un buffer de bytes crudos con los comandos del protocolo ESC/POS.
 * Compatible con impresoras genéricas ESC/POS (incluyendo DPos rt80b).
 *
 * Ancho de papel: 80mm → 48 caracteres por línea en fuente estándar (Font A, 12x24px).
 *
 * Esta clase es INFRAESTRUCTURA: el dominio no sabe de ESC/POS.
 * Recibe datos ya calculados por el dominio y los formatea para impresión.
 *
 * Diseño: Builder pattern fluido para construir el ticket secuencialmente.
 */
public class EscPosGenerator {

    // ─── Constantes del protocolo ESC/POS ────────────────────────────────────────

    /** Inicializar impresora (reset a estado por defecto) */
    private static final byte[] INIT = { 0x1B, 0x40 };

    /** Alimentar N líneas */
    private static final byte[] FEED_LINE = { 0x0A };

    /** Alinear a la izquierda */
    private static final byte[] ALIGN_LEFT = { 0x1B, 0x61, 0x00 };

    /** Alinear al centro */
    private static final byte[] ALIGN_CENTER = { 0x1B, 0x61, 0x01 };

    /** Alinear a la derecha */
    private static final byte[] ALIGN_RIGHT = { 0x1B, 0x61, 0x02 };

    /** Activar negrita */
    private static final byte[] BOLD_ON = { 0x1B, 0x45, 0x01 };

    /** Desactivar negrita */
    private static final byte[] BOLD_OFF = { 0x1B, 0x45, 0x00 };

    /** Activar doble ancho + doble alto (texto grande) */
    private static final byte[] DOUBLE_SIZE_ON = { 0x1D, 0x21, 0x11 };

    /** Desactivar doble ancho + doble alto (volver a tamaño normal) */
    private static final byte[] DOUBLE_SIZE_OFF = { 0x1D, 0x21, 0x00 };

    /** Activar subrayado */
    private static final byte[] UNDERLINE_ON = { 0x1B, 0x2D, 0x01 };

    /** Desactivar subrayado */
    private static final byte[] UNDERLINE_OFF = { 0x1B, 0x2D, 0x00 };

    /** Corte parcial de papel (deja pestaña para arrancar) */
    private static final byte[] CUT_PAPER = { 0x1D, 0x56, 0x41, 0x03 };

    /** Abrir cajón de dinero (kick pulse pin 2) */
    private static final byte[] OPEN_DRAWER = { 0x1B, 0x70, 0x00, 0x19, (byte) 0xFA };

    // ─── Configuración ───────────────────────────────────────────────────────────

    /** Caracteres por línea para papel de 80mm (Font A estándar) */
    private static final int LINE_WIDTH = 48;

    /** Caracteres por línea en modo doble tamaño (cada char ocupa 2 columnas) */
    private static final int DOUBLE_LINE_WIDTH = LINE_WIDTH / 2;

    /** Separador de sección (línea de guiones) */
    private static final String SEPARATOR = "-".repeat(LINE_WIDTH);

    /** Separador doble para totales */
    private static final String DOUBLE_SEPARATOR = "=".repeat(LINE_WIDTH);

    /** Formato de fecha para tickets */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Formato de hora para tickets */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Charset para codificación de texto (Latin-1 para ESC/POS) */
    private static final Charset CHARSET = Charset.forName("ISO-8859-1");

    // ─── Buffer interno ──────────────────────────────────────────────────────────

    private final ByteArrayOutputStream buffer;

    public EscPosGenerator() {
        this.buffer = new ByteArrayOutputStream(1024);
        escribir(INIT);
    }

    // ─── API fluida ──────────────────────────────────────────────────────────────

    public EscPosGenerator centrado() {
        escribir(ALIGN_CENTER);
        return this;
    }

    public EscPosGenerator izquierda() {
        escribir(ALIGN_LEFT);
        return this;
    }

    public EscPosGenerator negrita(boolean activar) {
        escribir(activar ? BOLD_ON : BOLD_OFF);
        return this;
    }

    public EscPosGenerator tamanoDoble(boolean activar) {
        escribir(activar ? DOUBLE_SIZE_ON : DOUBLE_SIZE_OFF);
        return this;
    }

    public EscPosGenerator linea(String texto) {
        escribirTexto(texto);
        escribir(FEED_LINE);
        return this;
    }

    public EscPosGenerator lineaVacia() {
        escribir(FEED_LINE);
        return this;
    }

    /**
     * Imprime texto con word-wrap respetando un ancho máximo.
     * Divide por palabras, sin cortar a mitad de palabra.
     * Útil para tamaño doble donde el ancho efectivo es LINE_WIDTH/2.
     *
     * @param texto el texto a imprimir
     * @param anchoMax caracteres máximos por línea
     */
    public EscPosGenerator lineaConWrap(String texto, int anchoMax) {
        if (texto == null || texto.isBlank()) {
            return lineaVacia();
        }
        if (texto.length() <= anchoMax) {
            return linea(texto);
        }
        // Word-wrap: dividir por palabras
        String[] palabras = texto.split(" ");
        StringBuilder lineaActual = new StringBuilder();
        for (String palabra : palabras) {
            if (lineaActual.isEmpty()) {
                lineaActual.append(palabra);
            } else if (lineaActual.length() + 1 + palabra.length() <= anchoMax) {
                lineaActual.append(' ').append(palabra);
            } else {
                // Emitir línea actual y empezar nueva
                linea(lineaActual.toString());
                lineaActual.setLength(0);
                lineaActual.append(palabra);
            }
        }
        // Emitir última línea
        if (!lineaActual.isEmpty()) {
            linea(lineaActual.toString());
        }
        return this;
    }

    public EscPosGenerator separador() {
        escribirTexto(SEPARATOR);
        escribir(FEED_LINE);
        return this;
    }

    public EscPosGenerator separadorDoble() {
        escribirTexto(DOUBLE_SEPARATOR);
        escribir(FEED_LINE);
        return this;
    }

    /**
     * Imprime dos textos en la misma línea: uno a la izquierda, otro a la derecha.
     * Rellena con espacios para completar LINE_WIDTH caracteres.
     */
    public EscPosGenerator lineaDosColumnas(String izq, String der) {
        izquierda();
        int espacios = LINE_WIDTH - izq.length() - der.length();
        if (espacios < 1) espacios = 1;
        String lineaCompleta = izq + " ".repeat(espacios) + der;
        return linea(lineaCompleta);
    }

    /**
     * Línea con 4 columnas: Cant | Descripción | P.Unit | Importe
     * Distribución: 4 + 24 + 10 + 10 = 48
     */
    public EscPosGenerator lineaItem(int cantidad, String descripcion, BigDecimal precioUnit, BigDecimal importe) {
        String cant = String.format("%3d", cantidad);
        String precio = formatMoney(precioUnit);
        String total = formatMoney(importe);

        // Truncar descripción si es muy larga
        int maxDesc = LINE_WIDTH - cant.length() - precio.length() - total.length() - 3;
        if (descripcion.length() > maxDesc) {
            descripcion = descripcion.substring(0, maxDesc - 1) + ".";
        }

        int espaciosDesc = maxDesc - descripcion.length();
        if (espaciosDesc < 0) espaciosDesc = 0;

        String linea = cant + " " + descripcion + " ".repeat(espaciosDesc) + " " + precio + " " + total;
        return linea(linea);
    }

    public EscPosGenerator cortePapel() {
        lineaVacia();
        lineaVacia();
        lineaVacia();
        escribir(CUT_PAPER);
        return this;
    }

    public EscPosGenerator abrirCajon() {
        escribir(OPEN_DRAWER);
        return this;
    }

    /**
     * Retorna el buffer ESC/POS como array de bytes listo para enviar a la impresora.
     */
    public byte[] build() {
        return buffer.toByteArray();
    }

    // ─── Métodos de generación de tickets predefinidos ────────────────────────────

    /**
     * Genera el buffer ESC/POS completo para un ticket de VENTA (cliente).
     *
     * Replica el diseño visual del TicketPreview del frontend:
     * - Header: nombre local, dirección, teléfono, CUIT
     * - Datos: Mesa, Pedido, Fecha, Hora
     * - Ítems: Cant | Descripción | P.Unit | Importe (con extras)
     * - Totales: Subtotal, Descuentos (promo/manual), TOTAL
     * - Footer: mensaje de bienvenida
     */
    public static byte[] generarTicketVenta(TicketVentaData data) {
        EscPosGenerator gen = new EscPosGenerator();

        // ── Header del local ──
        gen.centrado()
           .negrita(true)
           .tamanoDoble(true)
           .linea(data.nombreLocal)
           .tamanoDoble(false)
           .negrita(false);

        if (data.direccion != null && !data.direccion.isBlank()) {
            gen.linea(data.direccion);
        }
        if (data.telefono != null && !data.telefono.isBlank()) {
            gen.linea("Tel: " + data.telefono);
        }
        if (data.cuit != null && !data.cuit.isBlank()) {
            gen.linea("CUIT: " + data.cuit);
        }

        gen.separador();

        // ── Datos del ticket ──
        gen.izquierda()
           .lineaDosColumnas("Mesa " + data.numeroMesa, data.fechaHora.format(DATE_FMT))
           .lineaDosColumnas("Pedido #" + data.numeroPedido, data.fechaHora.format(TIME_FMT));

        gen.separador();

        // ── Cabecera de columnas ──
        gen.negrita(true)
           .linea(formatColumnaHeader())
           .negrita(false);

        // ── Ítems ──
        for (TicketItemData item : data.items) {
            gen.lineaItem(item.cantidad, item.descripcion, item.precioUnitario, item.importe);

            // Extras como sub-ítems
            if (item.extras != null) {
                for (TicketExtraData extra : item.extras) {
                    String extraDesc = "  + " + (extra.cantidad > 1 ? extra.cantidad + "x " : "") + extra.nombre;
                    gen.lineaDosColumnas(extraDesc, "$" + formatMoney(extra.subtotal));
                }
            }
        }

        gen.separador();

        // ── Totales ──
        gen.lineaDosColumnas("Subtotal", "$" + formatMoney(data.subtotal));

        if (data.montoDescuentoPromos.compareTo(BigDecimal.ZERO) > 0) {
            gen.lineaDosColumnas("Desc. Promos", "-$" + formatMoney(data.montoDescuentoPromos));
        }
        if (data.montoDescuentoManual.compareTo(BigDecimal.ZERO) > 0) {
            gen.lineaDosColumnas("Desc. Manual", "-$" + formatMoney(data.montoDescuentoManual));
        }

        gen.separadorDoble();

        gen.negrita(true)
           .tamanoDoble(true)
           .centrado()
           .linea("TOTAL  $" + formatMoney(data.totalFinal))
           .tamanoDoble(false)
           .negrita(false);

        gen.separador();

        // ── Footer ──
        gen.centrado();
        if (data.mensajeBienvenida != null && !data.mensajeBienvenida.isBlank()) {
            gen.linea(data.mensajeBienvenida);
        } else {
            gen.linea("*** Gracias por su visita ***");
        }

        gen.cortePapel();

        return gen.build();
    }

    /**
     * Genera el buffer ESC/POS para una COMANDA de cocina.
     *
     * Formato operativo: solo cantidades, nombres y observaciones.
     * SIN precios. Los ítems "nuevos" se marcan con *** NUEVO ***.
     *
     * @param data datos de la comanda
     * @param soloNuevos si true, solo imprime ítems marcados como nuevos
     */
    public static byte[] generarComandaCocina(ComandaCocinaData data, boolean soloNuevos) {
        EscPosGenerator gen = new EscPosGenerator();

        // ── Header ──
        gen.centrado()
           .negrita(true)
           .tamanoDoble(true)
           .linea("COCINA")
           .tamanoDoble(false)
           .negrita(false);

        gen.izquierda()
           .lineaDosColumnas("Mesa " + data.numeroMesa, data.fechaHora.format(TIME_FMT))
           .linea("Pedido #" + data.numeroPedido);

        gen.separador();

        // ── Ítems ──
        List<ComandaItemData> itemsAImprimir = soloNuevos
                ? data.items.stream().filter(i -> i.esNuevo).toList()
                : data.items;

        for (ComandaItemData item : itemsAImprimir) {
            gen.negrita(true)
               .tamanoDoble(true);

            String cantNombre = item.cantidad + "x " + item.nombreProducto;
            // Solo agregar badge *NUEVO* cuando se imprimen TODOS los ítems
            // (soloNuevos=false). Si soloNuevos=true, todos los ítems impresos
            // son nuevos por definición → el badge es redundante.
            if (!soloNuevos && item.esNuevo) {
                cantNombre += "  *NUEVO*";
            }
            // Word-wrap: en tamaño doble el ancho efectivo es 24 chars
            gen.lineaConWrap(cantNombre, DOUBLE_LINE_WIDTH);

            gen.tamanoDoble(false)
               .negrita(false);

            if (item.observaciones != null && !item.observaciones.isBlank()) {
                gen.linea("   >> " + item.observaciones);
            }

            // Extras de la comanda
            if (item.extras != null) {
                for (String extra : item.extras) {
                    gen.linea("   + " + extra);
                }
            }
        }

        gen.separador();
        gen.centrado()
           .linea(data.fechaHora.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));

        gen.cortePapel();

        return gen.build();
    }

    // ─── Helpers privados ────────────────────────────────────────────────────────

    private void escribir(byte[] bytes) {
        try {
            buffer.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Error escribiendo buffer ESC/POS", e);
        }
    }

    private void escribirTexto(String texto) {
        escribir(texto.getBytes(CHARSET));
    }

    private static String formatMoney(BigDecimal amount) {
        return String.format("%,.2f", amount);
    }

    private static String formatColumnaHeader() {
        // Cnt | Descripción                | P.Unit    | Importe
        return String.format("%3s %-" + (LINE_WIDTH - 24) + "s %9s %9s", "Cnt", "Descripcion", "P.Unit", "Importe");
    }

    // ─── Data classes (records inmutables) ────────────────────────────────────────

    /** Datos para generar un ticket de venta completo */
    public record TicketVentaData(
        String nombreLocal,
        String direccion,
        String telefono,
        String cuit,
        int numeroMesa,
        int numeroPedido,
        LocalDateTime fechaHora,
        List<TicketItemData> items,
        BigDecimal subtotal,
        BigDecimal montoDescuentoPromos,
        BigDecimal montoDescuentoManual,
        BigDecimal totalFinal,
        String mensajeBienvenida
    ) {}

    /** Ítem dentro de un ticket de venta */
    public record TicketItemData(
        int cantidad,
        String descripcion,
        BigDecimal precioUnitario,
        BigDecimal importe,
        List<TicketExtraData> extras
    ) {}

    /** Extra agrupado dentro de un ítem de ticket */
    public record TicketExtraData(
        String nombre,
        int cantidad,
        BigDecimal subtotal
    ) {}

    /** Datos para generar una comanda de cocina */
    public record ComandaCocinaData(
        int numeroMesa,
        int numeroPedido,
        LocalDateTime fechaHora,
        List<ComandaItemData> items
    ) {}

    /** Ítem dentro de una comanda de cocina */
    public record ComandaItemData(
        int cantidad,
        String nombreProducto,
        String observaciones,
        boolean esNuevo,
        List<String> extras
    ) {}
}
