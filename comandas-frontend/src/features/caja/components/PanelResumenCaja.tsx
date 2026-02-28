import { Wallet, TrendingUp, TrendingDown, ArrowDownCircle, Lock, Loader2, Users } from 'lucide-react';
import type { ReporteCajaDerivado } from '../types';

// ─── Utilidad ─────────────────────────────────────────────────────────────────

function fmt(valor: number): string {
  return valor.toLocaleString('es-AR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

// ─── Skeleton ─────────────────────────────────────────────────────────────────

function PanelSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      <div className="rounded-2xl bg-neutral-800/50 border border-neutral-800 p-6 space-y-4">
        <div className="flex items-center gap-3">
          <div className="w-11 h-11 rounded-xl bg-neutral-700" />
          <div className="space-y-1.5">
            <div className="h-3 w-32 rounded bg-neutral-700" />
            <div className="h-3 w-20 rounded bg-neutral-700" />
          </div>
        </div>
        <div className="h-12 w-48 rounded bg-neutral-700" />
        <div className="h-px bg-neutral-800" />
        <div className="flex justify-between">
          <div className="h-4 w-28 rounded bg-neutral-700" />
          <div className="h-4 w-20 rounded bg-neutral-700" />
        </div>
      </div>
      <div className="space-y-3">
        <div className="h-14 rounded-xl bg-neutral-700" />
        <div className="h-14 rounded-xl bg-neutral-700" />
      </div>
    </div>
  );
}

// ─── Props ────────────────────────────────────────────────────────────────────

interface PanelResumenCajaProps {
  reporte: ReporteCajaDerivado | undefined;
  isLoading: boolean;
  onRegistrarEgreso: () => void;
  onCerrarJornada: () => void;
  cerrandoJornada: boolean;
  cierreDeshabilitado?: boolean;
}

/**
 * Panel izquierdo del dashboard de Caja.
 *
 * Composición vertical:
 * 1. Tarjeta masiva — Saldo de caja (arqueo) con ingreso neto
 * 2. Línea de consumos internos (A Cuenta), tono apagado
 * 3. Botones de acción (Egreso + Cierre) integrados debajo
 *
 * Sin border-l, sin colores chillones. Jerarquía por tipografía y opacidad.
 */
export default function PanelResumenCaja({
  reporte,
  isLoading,
  onRegistrarEgreso,
  onCerrarJornada,
  cerrandoJornada,
  cierreDeshabilitado = false,
}: PanelResumenCajaProps) {
  if (isLoading) return <PanelSkeleton />;

  if (!reporte) {
    return (
      <div className="flex items-center justify-center py-20 text-gray-600">
        <Loader2 size={20} className="animate-spin mr-2" />
        Esperando datos…
      </div>
    );
  }

  const positivo = reporte.balanceFisicoEsperado >= 0;

  return (
    <div className="space-y-5">
      {/* ── Tarjeta principal: Saldo de caja ── */}
      <div className="rounded-2xl bg-gradient-to-b from-neutral-800 to-neutral-900 border border-neutral-800 p-6">
        {/* Label + ícono */}
        <div className="flex items-center gap-3 mb-5">
          <div className="w-11 h-11 rounded-xl bg-neutral-700/50 flex items-center justify-center">
            <Wallet size={22} className="text-gray-400" />
          </div>
          <div>
            <p className="text-xs font-semibold text-gray-500 uppercase tracking-widest">
              Saldo de caja
            </p>
          </div>
        </div>

        {/* Gran total */}
        <p
          className={[
            'text-5xl font-bold font-mono tracking-tighter leading-none',
            positivo ? 'text-gray-100' : 'text-red-400',
          ].join(' ')}
        >
          <span className="text-3xl text-gray-500 mr-0.5">$</span>
          {fmt(reporte.balanceFisicoEsperado)}
        </p>

        {/* Separador sutil */}
        <div className="my-5 border-t border-neutral-800" />

        {/* Ingreso neto */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <TrendingUp size={16} className="text-emerald-500/70" />
            <span className="text-sm text-gray-400">Ingreso neto</span>
          </div>
          <span className="text-lg font-semibold font-mono text-gray-300">
            ${fmt(reporte.ingresoNeto)}
          </span>
        </div>

        {/* Egresos */}
        <div className="flex items-center justify-between mt-2.5">
          <div className="flex items-center gap-2">
            <TrendingDown size={16} className="text-amber-500/70" />
            <span className="text-sm text-gray-400">Egresos</span>
          </div>
          <span className="text-lg font-semibold font-mono text-gray-300">
            −${fmt(reporte.totalEgresos)}
          </span>
        </div>

        {/* Consumos internos (A Cuenta) — apagado */}
        {reporte.totalConsumoInterno > 0 && (
          <div className="flex items-center justify-between mt-4 pt-3 border-t border-neutral-800/60">
            <div className="flex items-center gap-2">
              <Users size={14} className="text-gray-600" />
              <span className="text-xs text-gray-600 uppercase tracking-wide">
                A cuenta
              </span>
            </div>
            <span className="text-sm font-mono text-gray-600">
              ${fmt(reporte.totalConsumoInterno)}
            </span>
          </div>
        )}
      </div>

      {/* ── Botones de acción ── */}
      <div className="flex gap-2.5">
        <button
          type="button"
          onClick={onRegistrarEgreso}
          className={[
            'flex-1 h-12 rounded-xl font-medium text-sm',
            'flex items-center justify-center gap-2',
            'bg-neutral-800 border border-neutral-700/80 text-gray-300',
            'hover:bg-neutral-700/80 hover:border-neutral-600 hover:text-gray-100',
            'transition-all duration-150 active:scale-[0.97]',
            'focus:outline-none focus-visible:ring-2 focus-visible:ring-neutral-500',
          ].join(' ')}
        >
          <ArrowDownCircle size={17} className="text-amber-400" />
          Registrar egreso
        </button>

        <button
          type="button"
          onClick={onCerrarJornada}
          disabled={cerrandoJornada || cierreDeshabilitado}
          className={[
            'flex-1 h-12 rounded-xl font-medium text-sm',
            'flex items-center justify-center gap-2',
            'transition-all duration-150',
            'focus:outline-none focus-visible:ring-2 focus-visible:ring-red-500/50',
            cerrandoJornada
              ? 'bg-neutral-800 text-gray-500 cursor-wait'
              : cierreDeshabilitado
                ? 'bg-neutral-800/60 text-gray-600 cursor-not-allowed'
                : 'bg-red-600/90 hover:bg-red-500 text-white active:scale-[0.97]',
          ].join(' ')}
        >
          {cerrandoJornada ? (
            <>
              <Loader2 size={17} className="animate-spin" />
              Cerrando…
            </>
          ) : cierreDeshabilitado ? (
            <>
              <Lock size={16} className="text-gray-600" />
              Jornada cerrada
            </>
          ) : (
            <>
              <Lock size={16} />
              Cerrar jornada
            </>
          )}
        </button>
      </div>
    </div>
  );
}
