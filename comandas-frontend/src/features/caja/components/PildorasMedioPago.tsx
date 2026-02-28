import { DollarSign, CreditCard, QrCode, ArrowRightLeft } from 'lucide-react';
import type { MedioPago } from '../../salon/types';

// ─── Utilidad ─────────────────────────────────────────────────────────────────

function fmt(valor: number): string {
  return valor.toLocaleString('es-AR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });
}

// ─── Config de medios de pago ─────────────────────────────────────────────────

interface MedioPagoConfig {
  key: MedioPago;
  label: string;
  icono: React.ReactNode;
  color: string; // texto del monto
}

const MEDIOS: MedioPagoConfig[] = [
  {
    key: 'EFECTIVO',
    label: 'Efectivo',
    icono: <DollarSign size={16} />,
    color: 'text-emerald-400',
  },
  {
    key: 'TARJETA',
    label: 'Tarjeta',
    icono: <CreditCard size={16} />,
    color: 'text-blue-400',
  },
  {
    key: 'QR',
    label: 'QR',
    icono: <QrCode size={16} />,
    color: 'text-violet-400',
  },
  {
    key: 'TRANSFERENCIA',
    label: 'Transf.',
    icono: <ArrowRightLeft size={16} />,
    color: 'text-cyan-400',
  },
];

// ─── Skeleton ─────────────────────────────────────────────────────────────────

function PildoraSkeleton() {
  return (
    <div className="flex items-center gap-2.5 px-4 py-3 rounded-2xl bg-neutral-800/50 animate-pulse">
      <div className="w-8 h-8 rounded-lg bg-neutral-700" />
      <div className="space-y-1">
        <div className="h-2.5 w-12 rounded bg-neutral-700" />
        <div className="h-4 w-16 rounded bg-neutral-700" />
      </div>
    </div>
  );
}

// ─── Props ────────────────────────────────────────────────────────────────────

interface PildorasMedioPagoProps {
  desglose: Record<MedioPago, number> | undefined;
  isLoading: boolean;
}

/**
 * Fila horizontal de "píldoras" con los totales por medio de pago.
 *
 * Sin bordes pesados. Fondo sutil, ícono pequeño, monto en font-mono.
 * Se renderizan solo los medios con monto > 0 (o todos en loading).
 * A_CUENTA se excluye intencionalmente (vive en el panel izquierdo).
 */
export default function PildorasMedioPago({ desglose, isLoading }: PildorasMedioPagoProps) {
  if (isLoading) {
    return (
      <div className="flex gap-2 overflow-x-auto pb-1">
        {Array.from({ length: 3 }).map((_, i) => (
          <PildoraSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (!desglose) return null;

  // Filtrar medios con monto > 0, excluyendo A_CUENTA
  const activos = MEDIOS.filter((m) => (desglose[m.key] ?? 0) > 0);

  if (activos.length === 0) {
    return (
      <p className="text-sm text-gray-600 py-2">Sin ventas registradas</p>
    );
  }

  return (
    <div className="flex gap-2 overflow-x-auto pb-1">
      {activos.map(({ key, label, icono, color }) => (
        <div
          key={key}
          className={[
            'flex items-center gap-2.5 px-4 py-3',
            'rounded-2xl bg-neutral-800/50 border border-neutral-800',
            'shrink-0',
          ].join(' ')}
        >
          <div className="w-8 h-8 rounded-lg bg-neutral-700/40 flex items-center justify-center text-gray-400">
            {icono}
          </div>
          <div>
            <p className="text-[11px] text-gray-500 uppercase tracking-wide leading-none mb-0.5">
              {label}
            </p>
            <p className={`text-base font-semibold font-mono leading-none ${color}`}>
              ${fmt(desglose[key])}
            </p>
          </div>
        </div>
      ))}
    </div>
  );
}
