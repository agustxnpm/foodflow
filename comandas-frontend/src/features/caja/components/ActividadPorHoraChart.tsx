import { useMemo } from 'react';
import { Activity } from 'lucide-react';
import type { VentaResumen } from '../types';

// ─── Props ────────────────────────────────────────────────────────────────────

interface ActividadPorHoraChartProps {
  ventas: VentaResumen[] | undefined;
  isLoading: boolean;
}

/**
 * Gráfico de barras verticales — actividad operativa por franja horaria.
 *
 * Agrupa las ventas (pedidos cerrados) por hora de cierre y dibuja barras
 * proporcionales con gradiente y efecto glow. Muestra conteo y monto acumulado
 * en tooltip hover.
 *
 * Puro Tailwind + CSS inline, sin dependencias de charting.
 */
export default function ActividadPorHoraChart({ ventas, isLoading }: ActividadPorHoraChartProps) {
  // ── Skeleton ────────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="rounded-2xl bg-neutral-800/40 p-5 space-y-3 animate-pulse">
        <div className="h-3 w-28 rounded bg-neutral-700" />
        <div className="flex items-end gap-1.5 h-28">
          {Array.from({ length: 14 }).map((_, i) => (
            <div
              key={i}
              className="flex-1 rounded-t bg-neutral-700"
              style={{ height: `${15 + Math.random() * 70}%` }}
            />
          ))}
        </div>
      </div>
    );
  }

  if (!ventas || ventas.length === 0) return null;

  // ── Agrupar por hora ────────────────────────────────────────────────────

  const datos = useMemo(() => {
    const conteo: Record<number, { cantidad: number; monto: number }> = {};

    ventas.forEach((v) => {
      const hora = new Date(v.fechaCierre).getHours();
      if (!conteo[hora]) conteo[hora] = { cantidad: 0, monto: 0 };
      conteo[hora].cantidad += 1;
      conteo[hora].monto += v.total;
    });

    const horas = Object.keys(conteo).map(Number).sort((a, b) => a - b);
    if (horas.length === 0) return null;

    const min = horas[0];
    const max = horas[horas.length - 1];
    const rango = Array.from({ length: max - min + 1 }, (_, i) => min + i);
    const maxCant = Math.max(...Object.values(conteo).map((d) => d.cantidad));

    return { conteo, rango, maxCant };
  }, [ventas]);

  if (!datos) return null;

  const { conteo, rango, maxCant } = datos;

  // ── Hora pico ──
  const horaPico = rango.reduce((best, h) =>
    (conteo[h]?.cantidad ?? 0) > (conteo[best]?.cantidad ?? 0) ? h : best
  , rango[0]);

  return (
    <div className="rounded-2xl bg-neutral-800/40 p-5 space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Activity size={14} className="text-red-400" />
          <h3 className="text-xs text-gray-500 uppercase tracking-wider font-medium">
            Actividad por hora
          </h3>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="text-[10px] text-gray-600">Pico:</span>
          <span className="text-xs font-mono font-semibold text-red-400">
            {String(horaPico).padStart(2, '0')}hs
          </span>
        </div>
      </div>

      {/* Gráfico */}
      <div className="flex items-end gap-[3px] h-28">
        {rango.map((hora) => {
          const dato = conteo[hora];
          const cant = dato?.cantidad ?? 0;
          const monto = dato?.monto ?? 0;
          const pct = maxCant > 0 ? (cant / maxCant) * 100 : 0;
          const esPico = hora === horaPico && cant > 0;

          return (
            <div key={hora} className="flex-1 flex flex-col items-center gap-1 group relative">
              {/* Tooltip */}
              {cant > 0 && (
                <div className={[
                  'absolute -top-14 left-1/2 -translate-x-1/2 z-10',
                  'bg-neutral-800 border border-neutral-700 rounded-lg',
                  'px-2.5 py-1.5 shadow-lg shadow-black/40',
                  'opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none',
                  'whitespace-nowrap',
                ].join(' ')}>
                  <p className="text-[10px] font-mono text-gray-300">
                    {cant} {cant === 1 ? 'pedido' : 'pedidos'}
                  </p>
                  <p className="text-[10px] font-mono text-emerald-400">
                    ${monto.toLocaleString('es-AR', { maximumFractionDigits: 0 })}
                  </p>
                </div>
              )}

              {/* Barra */}
              <div className="w-full relative flex items-end h-24">
                <div
                  className={[
                    'w-full rounded-t-md transition-all duration-500 ease-out',
                    cant > 0
                      ? esPico
                        ? 'bg-gradient-to-t from-red-600 to-red-400 shadow-[0_0_12px_rgba(239,68,68,0.3)]'
                        : 'bg-gradient-to-t from-red-800/80 to-red-500/60 group-hover:from-red-700 group-hover:to-red-400'
                      : 'bg-neutral-800/30',
                  ].join(' ')}
                  style={{ height: cant > 0 ? `${Math.max(pct, 10)}%` : '3px' }}
                />
              </div>

              {/* Conteo sobre la barra */}
              {cant > 0 && (
                <span className={[
                  'text-[9px] font-mono leading-none tabular-nums',
                  esPico ? 'text-red-400 font-bold' : 'text-gray-500',
                ].join(' ')}>
                  {cant}
                </span>
              )}

              {/* Label hora */}
              <span className={[
                'text-[9px] font-mono leading-none',
                esPico ? 'text-red-400 font-semibold' : 'text-gray-600',
              ].join(' ')}>
                {hora.toString().padStart(2, '0')}
              </span>
            </div>
          );
        })}
      </div>

      {/* Resumen inferior */}
      <div className="flex items-center justify-between pt-2 border-t border-neutral-800/50">
        <span className="text-[11px] text-gray-600">
          {ventas.length} {ventas.length === 1 ? 'venta' : 'ventas'} totales
        </span>
        <span className="text-[11px] text-gray-500 font-mono">
          {String(rango[0]).padStart(2, '0')}:00 — {String(rango[rango.length - 1]).padStart(2, '0')}:59
        </span>
      </div>
    </div>
  );
}
