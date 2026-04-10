import { AlertTriangle } from 'lucide-react';

interface IndicadorLicenciaProps {
  diasRestantes: number | null;
  estado: 'ACTIVO' | 'POR_VENCER' | 'BLOQUEADO';
}

/**
 * Indicador sutil de vencimiento de licencia.
 *
 * Se muestra en la navbar solo cuando faltan ≤ 5 días para el vencimiento.
 * Si la licencia está activa con más de 5 días, no renderiza nada.
 */
export default function IndicadorLicencia({ diasRestantes, estado }: IndicadorLicenciaProps) {
  if (estado === 'ACTIVO' || diasRestantes === null) return null;

  const esHoy = diasRestantes === 0;
  const colorTexto = esHoy ? 'text-red-400' : 'text-amber-400';
  const colorFondo = esHoy ? 'bg-red-950/60' : 'bg-amber-950/40';

  const mensaje = esHoy
    ? 'Tu licencia vence HOY'
    : `Tu licencia vence en ${diasRestantes} día${diasRestantes > 1 ? 's' : ''}`;

  return (
    <div className={`flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-medium ${colorTexto} ${colorFondo}`}>
      <AlertTriangle size={14} strokeWidth={2} />
      <span>{mensaje}</span>
    </div>
  );
}
