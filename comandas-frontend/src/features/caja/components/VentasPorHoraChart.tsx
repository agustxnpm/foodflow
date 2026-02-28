import type { MovimientoResumen } from '../types';

// ─── Props ────────────────────────────────────────────────────────────────────

interface VentasPorHoraChartProps {
  movimientos: MovimientoResumen[] | undefined;
  isLoading: boolean;
}

/**
 * Micro-gráfico de barras verticales — actividad por franja horaria.
 *
 * Agrupa los movimientos por hora y dibuja barras proporcionales.
 * Puro Tailwind, sin dependencias de charting.
 *
 * TODO: Reemplazar por Recharts cuando se justifique la dependencia.
 */
export default function VentasPorHoraChart({ movimientos, isLoading }: VentasPorHoraChartProps) {
  // ── Skeleton ────────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="rounded-2xl bg-neutral-800/40 p-5 space-y-3 animate-pulse">
        <div className="h-3 w-28 rounded bg-neutral-700" />
        <div className="flex items-end gap-1.5 h-20">
          {Array.from({ length: 12 }).map((_, i) => (
            <div
              key={i}
              className="flex-1 rounded-t bg-neutral-700"
              style={{ height: `${20 + Math.random() * 60}%` }}
            />
          ))}
        </div>
      </div>
    );
  }

  if (!movimientos || movimientos.length === 0) return null;

  // ── Agrupar por hora ────────────────────────────────────────────────────

  const conteo: Record<number, number> = {};
  movimientos.forEach((m) => {
    const hora = new Date(m.fecha).getHours();
    conteo[hora] = (conteo[hora] ?? 0) + 1;
  });

  // Rango de horas: desde la mínima hasta la máxima detectada
  const horas = Object.keys(conteo).map(Number).sort((a, b) => a - b);
  if (horas.length === 0) return null;

  const min = horas[0];
  const max = horas[horas.length - 1];
  const rangoHoras = Array.from({ length: max - min + 1 }, (_, i) => min + i);
  const maxConteo = Math.max(...Object.values(conteo));

  return (
    <div className="rounded-2xl bg-neutral-800/40 p-5 space-y-3">
      <h3 className="text-xs text-gray-500 uppercase tracking-wider font-medium">
        Actividad por hora
      </h3>

      <div className="flex items-end gap-1 h-20">
        {rangoHoras.map((hora) => {
          const cant = conteo[hora] ?? 0;
          const pct = maxConteo > 0 ? (cant / maxConteo) * 100 : 0;
          return (
            <div key={hora} className="flex-1 flex flex-col items-center gap-1 group">
              {/* Barra */}
              <div className="w-full relative flex items-end h-16">
                <div
                  className={[
                    'w-full rounded-t transition-all duration-500 ease-out',
                    cant > 0 ? 'bg-red-500/70 group-hover:bg-red-500' : 'bg-neutral-700/30',
                  ].join(' ')}
                  style={{ height: cant > 0 ? `${Math.max(pct, 8)}%` : '4px' }}
                />
              </div>
              {/* Label hora */}
              <span className="text-[9px] text-gray-600 font-mono leading-none">
                {hora.toString().padStart(2, '0')}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
