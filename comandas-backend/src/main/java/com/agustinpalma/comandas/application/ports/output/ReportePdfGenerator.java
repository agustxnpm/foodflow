package com.agustinpalma.comandas.application.ports.output;

import com.agustinpalma.comandas.application.dto.ReporteCierreData;

/**
 * Puerto de salida para la generación del reporte PDF de cierre de jornada.
 *
 * Abstrae la tecnología de generación de PDF (FlyingSaucer, iText, etc.)
 * de la lógica de orquestación del caso de uso.
 *
 * La implementación concreta reside en la capa de infraestructura.
 */
public interface ReportePdfGenerator {

    /**
     * Genera un PDF con el reporte de cierre de jornada.
     *
     * @param data datos consolidados del cierre (totales, desglose, movimientos)
     * @return bytes del documento PDF generado
     */
    byte[] generarReporteCierre(ReporteCierreData data);
}
