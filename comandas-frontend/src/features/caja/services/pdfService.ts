/**
 * Servicio de descarga de PDF para el módulo Caja.
 *
 * Descarga el reporte PDF de cierre de jornada desde el backend
 * y lo guarda como archivo en el dispositivo del usuario.
 *
 * Funciona tanto en Tauri (WebView) como en navegador web:
 * usa Blob URL + anchor click, que es nativo en ambos entornos.
 *
 * @see cajaApi.descargarReportePdf — obtiene el blob del backend
 */

/**
 * Descarga un blob como archivo PDF usando un anchor temporal.
 *
 * Crea un enlace invisible, asigna el blob URL, simula click y limpia.
 * Funciona en Chrome, Firefox, Safari, y Tauri WebView.
 *
 * @param blob    - Blob del PDF recibido del backend
 * @param filename - Nombre del archivo a descargar (incluir .pdf)
 */
export function descargarPdf(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.style.display = 'none';

  document.body.appendChild(link);
  link.click();

  // Limpieza: revocar URL y eliminar el anchor
  setTimeout(() => {
    URL.revokeObjectURL(url);
    document.body.removeChild(link);
  }, 200);
}

/**
 * Genera un nombre de archivo descriptivo para el reporte de cierre.
 *
 * Formato: cierre-jornada-YYYY-MM-DD.pdf
 * Si no se provee fecha, usa la fecha actual.
 *
 * @param fechaOperativa - Fecha operativa de la jornada (ISO: YYYY-MM-DD)
 */
export function nombreArchivoPdf(fechaOperativa?: string): string {
  const fecha = fechaOperativa ?? new Date().toISOString().slice(0, 10);
  return `cierre-jornada-${fecha}.pdf`;
}
