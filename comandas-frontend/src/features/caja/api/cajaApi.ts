/**
 * Cliente API para el módulo Caja (Control Financiero Diario).
 *
 * Mapea los endpoints del backend:
 *   GET  /api/caja/reporte?fecha=YYYY-MM-DD  → Reporte / arqueo diario
 *   POST /api/caja/egresos                   → Registrar egreso de caja
 *   POST /api/caja/cierre-jornada            → Cierre de jornada diaria
 *
 * Patrón: devuelve `response.data` directamente (sin wrapper AxiosResponse)
 * para que TanStack Query trabaje con el tipo de dominio puro.
 *
 * @see mesasApi.ts y pedidosApi.ts por el patrón de referencia.
 * @see backend: CajaController
 */

import apiClient from '../../../lib/apiClient';
import type { AbrirCajaRequest, AbrirCajaResponse, EgresoRequest, EgresoResponse, EstadoCajaResponse, IngresoRequest, IngresoResponse, ReporteCajaResponse, DetallePedidoCerrado, CorreccionPedidoRequest, JornadaResumen, CierreJornadaResponse } from '../types';

export const cajaApi = {
  // ─── Queries ──────────────────────────────────────────────────────────────────

  /**
   * Consulta el estado actual de la caja (ABIERTA / CERRADA).
   *
   * GET /api/caja/estado
   *
   * Si hay una jornada abierta retorna sus datos (fondoInicial, fechaApertura).
   * Si no, retorna estado CERRADA con el saldo sugerido de la última jornada.
   */
  obtenerEstadoCaja: async (): Promise<EstadoCajaResponse> => {
    const response = await apiClient.get<EstadoCajaResponse>('/caja/estado');
    return response.data;
  },

  /**
   * HU-13: Obtener reporte de caja diario (arqueo).
   *
   * Devuelve totales por medio de pago, egresos y balance de efectivo.
   * El backend calcula todo; el frontend solo agrega datos derivados vía `select`.
   *
   * @param fecha - Fecha del reporte en formato ISO (YYYY-MM-DD)
   */
  obtenerReporte: async (fecha: string): Promise<ReporteCajaResponse> => {
    const response = await apiClient.get<ReporteCajaResponse>('/caja/reporte', {
      params: { fecha },
    });
    return response.data;
  },

  // ─── Mutations ────────────────────────────────────────────────────────────────

  /**
   * HU-13: Registrar un egreso de caja (salida de efectivo).
   *
   * El backend genera automáticamente el número de comprobante
   * con formato EGR-yyyyMMdd-HHmmss-XXXX.
   *
   * @param data - Monto y descripción del egreso
   */
  registrarEgreso: async (data: EgresoRequest): Promise<EgresoResponse> => {
    const response = await apiClient.post<EgresoResponse>('/caja/egresos', data);
    return response.data;
  },

  /**
   * Registrar un ingreso manual de caja (entrada de efectivo).
   *
   * El backend genera automáticamente el número de comprobante
   * con formato ING-yyyyMMdd-HHmmss-XXXX.
   *
   * Usado para registrar efectivo de plataformas externas (PedidosYa/Rappi)
   * u otros ingresos que no generan ticket de mesa.
   *
   * @param data - Monto y descripción del ingreso
   */
  registrarIngreso: async (data: IngresoRequest): Promise<IngresoResponse> => {
    const response = await apiClient.post<IngresoResponse>('/caja/ingresos', data);
    return response.data;
  },

  /**
   * Cierre de jornada diaria.
   *
   * POST /api/caja/cierre-jornada
   *
   * Respuestas del backend:
   *   - HTTP 200: Jornada cerrada exitosamente con { jornadaId: UUID }
   *   - HTTP 400: Mesas abiertas → { mensaje: string, mesasAbiertas: number }
   *   - HTTP 409: Jornada ya cerrada para la fecha operativa
   *
   * La fecha operativa se calcula en backend (turno noche: antes de 06:00 → día anterior).
   *
   * El hook `useCerrarJornada` transforma errores en tipos discriminables
   * (`MesasAbiertasError`, `JornadaYaCerradaError`) para la UI.
   */
  cerrarJornada: async (): Promise<CierreJornadaResponse> => {
    const response = await apiClient.post<CierreJornadaResponse>('/caja/cierre-jornada');
    return response.data;
  },

  /**
   * Abre una nueva jornada de caja con un fondo inicial declarado.
   *
   * POST /api/caja/abrir
   *
   * Respuestas del backend:
   *   - HTTP 201: Jornada abierta exitosamente
   *   - HTTP 409: Ya existe una jornada abierta en curso
   *
   * @param data - Monto inicial de efectivo en caja
   */
  abrirCaja: async (data: AbrirCajaRequest): Promise<AbrirCajaResponse> => {
    const response = await apiClient.post<AbrirCajaResponse>('/caja/abrir', data);
    return response.data;
  },

  /**
   * Corrige un pedido cerrado sin reabrir la mesa.
   *
   * PUT /api/caja/pedidos/{pedidoId}/correccion
   *
   * @param pedidoId UUID del pedido a corregir
   * @param data correcciones de ítems y pagos
   * @returns detalle actualizado del pedido
   */
  /**
   * Obtener detalle completo de un pedido cerrado (ítems + pagos).
   *
   * GET /api/caja/pedidos/{pedidoId}
   *
   * @param pedidoId UUID del pedido cerrado a consultar
   */
  obtenerDetallePedido: async (pedidoId: string): Promise<DetallePedidoCerrado> => {
    const response = await apiClient.get<DetallePedidoCerrado>(
      `/caja/pedidos/${pedidoId}`,
    );
    return response.data;
  },

  corregirPedido: async (
    pedidoId: string,
    data: CorreccionPedidoRequest,
  ): Promise<DetallePedidoCerrado> => {
    const response = await apiClient.put<DetallePedidoCerrado>(
      `/caja/pedidos/${pedidoId}/correccion`,
      data,
    );
    return response.data;
  },

  // ─── Historial de Jornadas ──────────────────────────────────────────────────

  /**
   * Obtener historial de jornadas cerradas por rango de fechas.
   *
   * GET /api/caja/jornadas?desde=YYYY-MM-DD&hasta=YYYY-MM-DD
   *
   * @param desde - Fecha operativa inicial (inclusive)
   * @param hasta - Fecha operativa final (inclusive)
   */
  obtenerHistorialJornadas: async (desde: string, hasta: string): Promise<JornadaResumen[]> => {
    const response = await apiClient.get<JornadaResumen[]>('/caja/jornadas', {
      params: { desde, hasta },
    });
    return response.data;
  },

  // ─── Reporte PDF ────────────────────────────────────────────────────────────

  /**
   * Descarga el reporte PDF de cierre de una jornada.
   *
   * GET /api/caja/jornadas/{jornadaId}/reporte-pdf
   *
   * Retorna un Blob que puede descargarse como archivo.
   *
   * @param jornadaId - UUID de la jornada cerrada
   */
  descargarReportePdf: async (jornadaId: string): Promise<Blob> => {
    const response = await apiClient.get(`/caja/jornadas/${jornadaId}/reporte-pdf`, {
      responseType: 'blob',
    });
    return response.data as Blob;
  },
};
