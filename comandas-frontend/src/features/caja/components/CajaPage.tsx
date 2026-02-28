import { useState, useMemo, useSyncExternalStore } from 'react';
import { DollarSign, Calendar, History } from 'lucide-react';
import { Link } from 'react-router-dom';

import {
  useReporteCaja,
  useRegistrarEgreso,
  useCerrarJornada,
} from '../hooks/useCaja';
import { MesasAbiertasError, JornadaYaCerradaError } from '../types';
import type { PagoDetalle } from '../types';
import useToast from '../../../hooks/useToast';
import { getDevDateOverride, subscribeDevDate } from '../../../lib/devClock';

import PanelResumenCaja from './PanelResumenCaja';
import DesglosePagos from './DesglosePagos';
import DetallePagoPedidoModal from './DetallePagoPedidoModal';
import ActividadPorHoraChart from './ActividadPorHoraChart';
import HistorialVentas from './HistorialVentas';
import ListaMovimientosDia from './ListaMovimientosDia';
import EgresoModal from './EgresoModal';
import AlertaMesasAbiertas from './AlertaMesasAbiertas';
import CorregirPedidoModal from './CorregirPedidoModal';
import ConfirmarCierreModal from './ConfirmarCierreModal';

// ─── Utilidad ─────────────────────────────────────────────────────────────────

/**
 * Hora de corte para determinar la fecha operativa.
 * Espejo exacto de JornadaCaja.HORA_CORTE_JORNADA del backend.
 *
 * Si la hora actual es ANTES de las 06:00, la fecha operativa
 * es el día anterior (turno noche: el local sigue operando).
 */
const HORA_CORTE = 6;

function fechaOperativaDesde(date: Date): string {
  const ajustada = date.getHours() < HORA_CORTE
    ? new Date(date.getFullYear(), date.getMonth(), date.getDate() - 1)
    : date;
  const yyyy = ajustada.getFullYear();
  const mm = String(ajustada.getMonth() + 1).padStart(2, '0');
  const dd = String(ajustada.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

/**
 * Hook que retorna la fecha operativa actual.
 * En dev con time-travel activo, usa la fecha del backend override.
 * En producción, aplica la lógica de hora de corte (06:00)
 * para que a las 01:00 AM siga mostrando los datos del día anterior.
 */
function useFechaOperativa(): string {
  const devDate = useSyncExternalStore(subscribeDevDate, getDevDateOverride);
  const realDate = useMemo(() => fechaOperativaDesde(new Date()), []);
  return devDate ?? realDate;
}

function formatFechaLegible(fecha: string): string {
  const [yyyy, mm, dd] = fecha.split('-').map(Number);
  const d = new Date(yyyy, mm - 1, dd);
  return d.toLocaleDateString('es-AR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}

// ─── CajaPage ─────────────────────────────────────────────────────────────────

/**
 * Dashboard principal del módulo Caja.
 *
 * Layout a 3 columnas en desktop (xl:grid-cols-12) para aprovechar
 * todo el ancho de pantalla:
 *
 *   • Col 1 (col-span-3): Panel resumen + acciones + actividad por hora
 *   • Col 2 (col-span-5): Desglose interactivo por medio de pago
 *   • Col 3 (col-span-4): Historial ventas + egresos
 *
 * En pantallas medianas (lg) usa 2 columnas.
 * En mobile se apila verticalmente.
 */
export default function CajaPage() {
  const toast = useToast();
  const hoy = useFechaOperativa();

  // ── Hooks de datos ──
  const { data: reporte, isLoading: cargandoReporte } = useReporteCaja(hoy);
  const registrarEgreso = useRegistrarEgreso();
  const cerrarJornada = useCerrarJornada();

  // ── Estado local de UI ──
  const [egresoModalAbierto, setEgresoModalAbierto] = useState(false);
  const [confirmarCierreAbierto, setConfirmarCierreAbierto] = useState(false);
  const jornadaCerrada = reporte?.jornadaCerrada ?? false;
  const [alertaMesas, setAlertaMesas] = useState<{
    mensaje: string;
    mesasAbiertas?: number;
  } | null>(null);
  const [pagoDetalle, setPagoDetalle] = useState<PagoDetalle | null>(null);
  const [pedidoACorregir, setPedidoACorregir] = useState<string | null>(null);

  // ── Handlers ──

  const handleRegistrarEgreso = (data: { monto: number; descripcion: string }) => {
    registrarEgreso.mutate(data, {
      onSuccess: () => {
        toast.success('Egreso registrado correctamente');
        setEgresoModalAbierto(false);
      },
      onError: () => {
        toast.error('Error al registrar el egreso');
      },
    });
  };

  const handleCerrarJornada = () => {
    setConfirmarCierreAbierto(true);
  };

  const handleConfirmarCierre = () => {
    cerrarJornada.mutate(undefined, {
      onSuccess: () => {
        setConfirmarCierreAbierto(false);
        setPagoDetalle(null);
        setPedidoACorregir(null);
        toast.success('Jornada cerrada exitosamente. Los datos quedan registrados.');
      },
      onError: (error) => {
        setConfirmarCierreAbierto(false);
        if (error instanceof MesasAbiertasError) {
          setAlertaMesas({
            mensaje: error.message,
            mesasAbiertas: error.mesasAbiertas,
          });
        } else if (error instanceof JornadaYaCerradaError) {
          toast.info(error.message);
        } else {
          toast.error('Error inesperado al cerrar la jornada');
        }
      },
    });
  };

  const handleCorregirPedido = (pedidoId: string) => {
    setPedidoACorregir(pedidoId);
  };

  const handleCorreccionExitosa = () => {
    setPedidoACorregir(null);
    toast.success('Pedido corregido correctamente');
  };

  const handleVerDetallePago = (pago: PagoDetalle) => {
    setPagoDetalle(pago);
  };

  // ── Datos derivados para modal de detalle ──
  const ventaDelPagoDetalle = pagoDetalle
    ? reporte?.ventas?.find((v) => v.pedidoId === pagoDetalle.pedidoId)
    : undefined;

  const pagosDelMismoPedido = pagoDetalle
    ? (reporte?.pagosDetalle ?? []).filter((p) => p.pedidoId === pagoDetalle.pedidoId)
    : [];

  return (
    <section className="h-[calc(100vh-4rem)] bg-neutral-900 overflow-hidden">
      <div className="h-full w-full px-4 sm:px-6 xl:px-8 py-5 flex flex-col">
        {/* ── Header ── */}
        <header className="flex items-center gap-3 mb-5 shrink-0">
          <div className="w-10 h-10 rounded-xl bg-red-600/20 flex items-center justify-center">
            <DollarSign size={22} className="text-red-500" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-100 tracking-tight">Caja</h1>
            <div className="flex items-center gap-1.5 text-sm text-gray-500">
              <Calendar size={14} />
              <span>{formatFechaLegible(hoy)}</span>
            </div>
          </div>

          {/* Acceso rápido a historial de jornadas */}
          <Link
            to="/caja/historial"
            className="ml-auto flex items-center gap-2 px-3 py-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-gray-400 hover:text-gray-200 text-xs font-medium transition-all"
          >
            <History size={14} />
            <span className="hidden sm:inline">Historial</span>
          </Link>
        </header>

        {/* ── Grid principal — 3 columnas en XL ── */}
        <div className="flex-1 grid grid-cols-1 lg:grid-cols-12 gap-5 min-h-0">

          {/* ── Columna 1: Resumen + Actividad ── */}
          <div className="lg:col-span-4 xl:col-span-3 overflow-y-auto pr-1 space-y-5 scrollbar-thin">
            <PanelResumenCaja
              reporte={reporte}
              isLoading={cargandoReporte}
              onRegistrarEgreso={() => setEgresoModalAbierto(true)}
              onCerrarJornada={handleCerrarJornada}
              cerrandoJornada={cerrarJornada.isPending}
              cierreDeshabilitado={jornadaCerrada}
            />

            <ActividadPorHoraChart
              ventas={reporte?.ventas}
              isLoading={cargandoReporte}
            />
          </div>

          {/* ── Columna 2: Desglose interactivo por medio de pago ── */}
          <div className="lg:col-span-4 xl:col-span-5 flex flex-col min-h-0 rounded-2xl border border-neutral-800/60 bg-neutral-900/50 p-4">
            <div className="flex items-center justify-between shrink-0 mb-3">
              <h2 className="text-xs text-gray-500 uppercase tracking-wider font-medium">
                Agrupado por medio de pago
              </h2>
              {reporte?.pagosDetalle && reporte.pagosDetalle.length > 0 && (
                <span className="text-xs text-gray-600 font-mono">
                  {reporte.pagosDetalle.length} {reporte.pagosDetalle.length === 1 ? 'pago' : 'pagos'}
                </span>
              )}
            </div>
            <div className="flex-1 overflow-y-auto scrollbar-thin">
              <DesglosePagos
                pagosDetalle={reporte?.pagosDetalle ?? []}
                isLoading={cargandoReporte}
                onVerDetallePedido={handleVerDetallePago}
              />
            </div>
          </div>

          {/* ── Columna 3: Ventas + Egresos ── */}
          <div className="lg:col-span-4 xl:col-span-4 flex flex-col min-h-0 gap-4">
            {/* Ventas del día */}
            <div className="flex flex-col min-h-0 flex-1 rounded-2xl border border-neutral-800/60 bg-neutral-900/50 p-4">
              <div className="flex items-center justify-between shrink-0 mb-2">
                <h2 className="text-xs text-gray-500 uppercase tracking-wider font-medium">
                  Ventas del día
                </h2>
                {reporte?.ventas && reporte.ventas.length > 0 && (
                  <span className="text-xs text-gray-600 font-mono">
                    {reporte.ventas.length} {reporte.ventas.length === 1 ? 'pedido' : 'pedidos'}
                  </span>
                )}
              </div>
              <div className="flex-1 overflow-y-auto scrollbar-thin">
                <HistorialVentas
                  ventas={reporte?.ventas ?? []}
                  isLoading={cargandoReporte}
                  onCorregirPedido={handleCorregirPedido}
                />
              </div>
            </div>

            {/* Egresos del día */}
            <div className="flex flex-col min-h-0 flex-1 rounded-2xl border border-neutral-800/60 bg-neutral-900/50 p-4">
              <div className="flex items-center justify-between shrink-0 mb-2">
                <h2 className="text-xs text-gray-500 uppercase tracking-wider font-medium">
                  Egresos del día
                </h2>
                {reporte?.movimientos && reporte.movimientos.length > 0 && (
                  <span className="text-xs text-gray-600 font-mono">
                    {reporte.movimientos.length} {reporte.movimientos.length === 1 ? 'registro' : 'registros'}
                  </span>
                )}
              </div>
              <div className="flex-1 overflow-y-auto scrollbar-thin">
                <ListaMovimientosDia
                  movimientos={reporte?.movimientos ?? []}
                  isLoading={cargandoReporte}
                  onReabrirPedido={handleCorregirPedido}
                  reabriendo={false}
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ── Modales ── */}
      {egresoModalAbierto && (
        <EgresoModal
          onClose={() => setEgresoModalAbierto(false)}
          onConfirmar={handleRegistrarEgreso}
          isPending={registrarEgreso.isPending}
        />
      )}

      {alertaMesas && (
        <AlertaMesasAbiertas
          mensaje={alertaMesas.mensaje}
          mesasAbiertas={alertaMesas.mesasAbiertas}
          onDismiss={() => setAlertaMesas(null)}
        />
      )}

      {pagoDetalle && (
        <DetallePagoPedidoModal
          pago={pagoDetalle}
          venta={ventaDelPagoDetalle}
          pagosDelPedido={pagosDelMismoPedido}
          onClose={() => setPagoDetalle(null)}
        />
      )}

      {pedidoACorregir && (
        <CorregirPedidoModal
          pedidoId={pedidoACorregir}
          onClose={() => setPedidoACorregir(null)}
          onCorreccionExitosa={handleCorreccionExitosa}
        />
      )}

      {confirmarCierreAbierto && (
        <ConfirmarCierreModal
          onConfirmar={handleConfirmarCierre}
          onCancelar={() => setConfirmarCierreAbierto(false)}
          isPending={cerrarJornada.isPending}
        />
      )}
    </section>
  );
}
