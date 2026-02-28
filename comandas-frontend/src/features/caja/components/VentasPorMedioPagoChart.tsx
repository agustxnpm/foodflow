import type { MedioPago } from '../../salon/types';

// ─── Utilidad ─────────────────────────────────────────────────────────────────

function fmt(valor: number): string {
  return valor.toLocaleString('es-AR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });
}

// ─── Config ───────────────────────────────────────────────────────────────────

interface MedioPagoBar {
  key: MedioPago;
  label: string;
  color: string; // bg de la barra
}

const MEDIOS: MedioPagoBar[] = [
  { key: 'EFECTIVO', label: 'Efectivo', color: 'bg-emerald-500' },
  { key: 'TARJETA', label: 'Tarjeta', color: 'bg-blue-500' },
  { key: 'QR', label: 'QR', color: 'bg-violet-500' },
  { key: 'TRANSFERENCIA', label: 'Transf.', color: 'bg-cyan-500' },
];

// ─── Props ────────────────────────────────────────────────────────────────────

interface VentasPorMedioPagoChartProps {
  desglose: Record<MedioPago, number> | undefined;
  isLoading: boolean;
}

/**
 * Gráfico de barras horizontales (puro Tailwind) — distribución por medio de pago.
 *
 * Cada barra ocupa un porcentaje proporcional al total.
 * TODO: Reemplazar por Recharts o similar cuando se justifique.
 */
export default function VentasPorMedioPagoChart({
  desglose,
  isLoading,
}: VentasPorMedioPagoChartProps) {
  // ── Skeleton ────────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="rounded-2xl bg-neutral-800/40 p-5 space-y-3 animate-pulse">
        <div className="h-3 w-36 rounded bg-neutral-700" />
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="space-y-1">
            <div className="h-2 w-16 rounded bg-neutral-700" />
            <div className="h-5 rounded bg-neutral-700" style={{ width: `${60 - i * 15}%` }} />
          </div>
        ))}
      </div>
    );
  }

  if (!desglose) return null;

  // Calcular total para porcentajes
  const items = MEDIOS.map((m) => ({
    ...m,
    monto: desglose[m.key] ?? 0,
  })).filter((m) => m.monto > 0);

  const totalMedios = items.reduce((acc, i) => acc + i.monto, 0);

  if (items.length === 0) return null;

  return (
    <div className="rounded-2xl bg-neutral-800/40 p-5 space-y-3">
      <h3 className="text-xs text-gray-500 uppercase tracking-wider font-medium">
        Distribución por medio de pago
      </h3>

      <div className="space-y-2.5">
        {items.map(({ key, label, color, monto }) => {
          const pct = totalMedios > 0 ? (monto / totalMedios) * 100 : 0;
          return (
            <div key={key} className="space-y-1">
              <div className="flex items-center justify-between text-xs">
                <span className="text-gray-400">{label}</span>
                <span className="font-mono text-gray-300">${fmt(monto)}</span>
              </div>
              <div className="h-5 w-full rounded-lg bg-neutral-700/40 overflow-hidden">
                <div
                  className={`h-full rounded-lg ${color} transition-all duration-500 ease-out`}
                  style={{ width: `${Math.max(pct, 2)}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
