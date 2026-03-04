package com.agustinpalma.comandas.infrastructure.adapter;

import com.agustinpalma.comandas.application.dto.ReporteCierreData;
import com.agustinpalma.comandas.application.ports.output.ReportePdfGenerator;
import com.agustinpalma.comandas.domain.model.DomainEnums.MedioPago;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

/**
 * Implementación de {@link ReportePdfGenerator} usando FlyingSaucer (XHTML → PDF).
 *
 * Genera un PDF A4 profesional con:
 * - Encabezado con logo y datos del local
 * - Resumen de arqueo (totales + pedidos cerrados)
 * - Desglose por medio de pago
 * - Lista de movimientos manuales (egresos + ingresos)
 * - Pie con fecha/hora de generación
 *
 * El HTML se construye programáticamente con CSS embebido.
 * FlyingSaucer lo convierte a PDF vía OpenPDF.
 */
public class FlyingSaucerReportePdfAdapter implements ReportePdfGenerator {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String logoBase64;

    public FlyingSaucerReportePdfAdapter() {
        this.logoBase64 = cargarLogoBase64();
    }

    @Override
    public byte[] generarReporteCierre(ReporteCierreData data) {
        String xhtml = construirXhtml(data);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF del reporte de cierre", e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Construcción del XHTML
    // ════════════════════════════════════════════════════════════════════════════

    private String construirXhtml(ReporteCierreData data) {
        StringBuilder html = new StringBuilder();

        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ");
        html.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        html.append("<head>\n");
        html.append("<style type=\"text/css\">\n");
        html.append(css());
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // ── Encabezado ─────────────────────────────────────────────────────
        html.append("<div class=\"header\">\n");
        if (logoBase64 != null && !logoBase64.isEmpty()) {
            html.append("  <img class=\"logo\" src=\"data:image/png;base64,")
                .append(logoBase64).append("\" />\n");
        }
        html.append("  <h1>").append(esc(data.nombreLocal())).append("</h1>\n");
        if (!data.direccion().isEmpty()) {
            html.append("  <p class=\"info\">").append(esc(data.direccion())).append("</p>\n");
        }
        if (!data.telefono().isEmpty()) {
            html.append("  <p class=\"info\">Tel: ").append(esc(data.telefono())).append("</p>\n");
        }
        if (!data.cuit().isEmpty()) {
            html.append("  <p class=\"info\">CUIT: ").append(esc(data.cuit())).append("</p>\n");
        }
        html.append("</div>\n");

        // ── Título ─────────────────────────────────────────────────────────
        html.append("<div class=\"titulo-reporte\">\n");
        html.append("  <h2>REPORTE DE CIERRE DE JORNADA</h2>\n");
        html.append("  <p>Fecha operativa: <strong>")
            .append(data.fechaOperativa().format(FORMATO_FECHA))
            .append("</strong></p>\n");
        html.append("  <p>Cierre realizado: ")
            .append(data.fechaCierre().format(FORMATO_FECHA_HORA))
            .append("</p>\n");
        html.append("</div>\n");

        // ── Resumen de Arqueo ──────────────────────────────────────────────
        html.append("<div class=\"seccion\">\n");
        html.append("  <h3>Resumen de Arqueo</h3>\n");
        html.append("  <table class=\"tabla-resumen\">\n");
        html.append("    <tbody>\n");
        filaResumen(html, "Pedidos cerrados", String.valueOf(data.pedidosCerradosCount()));
        filaResumenMonto(html, "Total ventas reales", data.totalVentasReales());
        filaResumenMonto(html, "Total consumo interno", data.totalConsumoInterno());
        filaResumenMonto(html, "Total ingresos manuales", data.totalIngresos());
        filaResumenMonto(html, "Total egresos", data.totalEgresos());
        html.append("    </tbody>\n");
        html.append("    <tfoot>\n");
        html.append("      <tr class=\"total\">\n");
        html.append("        <td>Balance de Efectivo</td>\n");
        html.append("        <td class=\"monto\">").append(formatMonto(data.balanceEfectivo())).append("</td>\n");
        html.append("      </tr>\n");
        html.append("    </tfoot>\n");
        html.append("  </table>\n");
        html.append("</div>\n");

        // ── Desglose por Medio de Pago ─────────────────────────────────────
        if (!data.desglosePorMedioPago().isEmpty()) {
            html.append("<div class=\"seccion\">\n");
            html.append("  <h3>Desglose por Medio de Pago</h3>\n");
            html.append("  <table class=\"tabla-desglose\">\n");
            html.append("    <thead>\n");
            html.append("      <tr><th>Medio de Pago</th><th class=\"monto\">Monto</th></tr>\n");
            html.append("    </thead>\n");
            html.append("    <tbody>\n");

            BigDecimal totalDesglose = BigDecimal.ZERO;
            for (Map.Entry<MedioPago, BigDecimal> entry : data.desglosePorMedioPago().entrySet()) {
                html.append("      <tr>\n");
                html.append("        <td>").append(formatMedioPago(entry.getKey())).append("</td>\n");
                html.append("        <td class=\"monto\">").append(formatMonto(entry.getValue())).append("</td>\n");
                html.append("      </tr>\n");
                totalDesglose = totalDesglose.add(entry.getValue());
            }

            html.append("    </tbody>\n");
            html.append("    <tfoot>\n");
            html.append("      <tr class=\"total\">\n");
            html.append("        <td>TOTAL</td>\n");
            html.append("        <td class=\"monto\">").append(formatMonto(totalDesglose)).append("</td>\n");
            html.append("      </tr>\n");
            html.append("    </tfoot>\n");
            html.append("  </table>\n");
            html.append("</div>\n");
        }

        // ── Movimientos Manuales ───────────────────────────────────────────
        if (!data.movimientos().isEmpty()) {
            html.append("<div class=\"seccion\">\n");
            html.append("  <h3>Movimientos Manuales</h3>\n");
            html.append("  <table class=\"tabla-movimientos\">\n");
            html.append("    <thead>\n");
            html.append("      <tr>\n");
            html.append("        <th>Tipo</th>\n");
            html.append("        <th>Comprobante</th>\n");
            html.append("        <th>Descripción</th>\n");
            html.append("        <th class=\"monto\">Monto</th>\n");
            html.append("        <th>Hora</th>\n");
            html.append("      </tr>\n");
            html.append("    </thead>\n");
            html.append("    <tbody>\n");

            DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");
            for (ReporteCierreData.MovimientoDetalle mov : data.movimientos()) {
                html.append("      <tr>\n");
                html.append("        <td class=\"tipo-").append(mov.tipo().toLowerCase()).append("\">")
                    .append(mov.tipo()).append("</td>\n");
                html.append("        <td class=\"comprobante\">").append(esc(mov.numeroComprobante())).append("</td>\n");
                html.append("        <td>").append(esc(mov.descripcion())).append("</td>\n");
                html.append("        <td class=\"monto\">").append(formatMonto(mov.monto())).append("</td>\n");
                html.append("        <td>").append(mov.fecha().format(horaFmt)).append("</td>\n");
                html.append("      </tr>\n");
            }

            html.append("    </tbody>\n");
            html.append("  </table>\n");
            html.append("</div>\n");
        }

        // ── Pie ────────────────────────────────────────────────────────────
        html.append("<div class=\"footer\">\n");
        html.append("  <p>Documento generado automáticamente por FoodFlow</p>\n");
        html.append("  <p>Este reporte es un comprobante interno — no válido como factura fiscal</p>\n");
        html.append("</div>\n");

        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CSS embebido
    // ════════════════════════════════════════════════════════════════════════════

    private String css() {
        return """
            @page {
                size: A4;
                margin: 20mm 15mm 20mm 15mm;
            }
            body {
                font-family: Helvetica, Arial, sans-serif;
                font-size: 11pt;
                color: #1a1a1a;
                line-height: 1.4;
            }
            .header {
                text-align: center;
                margin-bottom: 10px;
                padding-bottom: 10px;
                border-bottom: 2px solid #c0392b;
            }
            .header .logo {
                width: 80px;
                height: auto;
                margin-bottom: 5px;
            }
            .header h1 {
                font-size: 18pt;
                margin: 5px 0 2px 0;
                color: #c0392b;
            }
            .header .info {
                font-size: 9pt;
                color: #555;
                margin: 1px 0;
            }
            .titulo-reporte {
                text-align: center;
                margin: 15px 0;
            }
            .titulo-reporte h2 {
                font-size: 14pt;
                color: #2c3e50;
                margin: 0 0 5px 0;
                letter-spacing: 1px;
            }
            .titulo-reporte p {
                font-size: 10pt;
                color: #555;
                margin: 2px 0;
            }
            .seccion {
                margin: 18px 0;
            }
            .seccion h3 {
                font-size: 12pt;
                color: #2c3e50;
                border-bottom: 1px solid #ddd;
                padding-bottom: 4px;
                margin-bottom: 8px;
            }
            table {
                width: 100%;
                border-collapse: collapse;
            }
            .tabla-resumen td {
                padding: 6px 8px;
                border-bottom: 1px solid #eee;
            }
            .tabla-resumen .monto {
                text-align: right;
                font-weight: bold;
                font-family: monospace;
                font-size: 11pt;
            }
            .tabla-resumen tfoot .total td {
                border-top: 2px solid #2c3e50;
                font-size: 13pt;
                font-weight: bold;
                padding-top: 8px;
                color: #c0392b;
            }
            .tabla-desglose th,
            .tabla-movimientos th {
                background-color: #2c3e50;
                color: white;
                padding: 6px 8px;
                text-align: left;
                font-size: 10pt;
            }
            .tabla-desglose td,
            .tabla-movimientos td {
                padding: 5px 8px;
                border-bottom: 1px solid #eee;
                font-size: 10pt;
            }
            .tabla-desglose tbody tr:nth-child(even),
            .tabla-movimientos tbody tr:nth-child(even) {
                background-color: #f9f9f9;
            }
            .tabla-desglose tfoot .total td {
                border-top: 2px solid #2c3e50;
                font-weight: bold;
                padding-top: 6px;
            }
            .monto {
                text-align: right;
                font-family: monospace;
            }
            .comprobante {
                font-family: monospace;
                font-size: 9pt;
            }
            .tipo-egreso { color: #c0392b; font-weight: bold; }
            .tipo-ingreso { color: #27ae60; font-weight: bold; }
            .footer {
                margin-top: 30px;
                padding-top: 10px;
                border-top: 1px solid #ddd;
                text-align: center;
                font-size: 8pt;
                color: #999;
            }
            .footer p {
                margin: 2px 0;
            }
            """;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════════

    private void filaResumen(StringBuilder html, String etiqueta, String valor) {
        html.append("      <tr>\n");
        html.append("        <td>").append(etiqueta).append("</td>\n");
        html.append("        <td class=\"monto\">").append(valor).append("</td>\n");
        html.append("      </tr>\n");
    }

    private void filaResumenMonto(StringBuilder html, String etiqueta, BigDecimal monto) {
        filaResumen(html, etiqueta, formatMonto(monto));
    }

    private String formatMonto(BigDecimal monto) {
        if (monto == null) return "$0.00";
        return String.format("$%,.2f", monto);
    }

    private String formatMedioPago(MedioPago medio) {
        return switch (medio) {
            case EFECTIVO -> "Efectivo";
            case TARJETA -> "Tarjeta";
            case TRANSFERENCIA -> "Transferencia";
            case QR -> "QR";
            case A_CUENTA -> "A Cuenta (Consumo Interno)";
        };
    }

    /**
     * Escapa caracteres XML especiales para evitar XHTML roto.
     */
    private String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    /**
     * Carga el logo del local desde resources y lo codifica en Base64.
     * Si no se encuentra, retorna cadena vacía (el PDF se genera sin logo).
     */
    private static String cargarLogoBase64() {
        try (InputStream is = FlyingSaucerReportePdfAdapter.class
                .getResourceAsStream("/templates/logo-meiser.png")) {
            if (is == null) {
                return "";
            }
            byte[] bytes = is.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return "";
        }
    }
}
