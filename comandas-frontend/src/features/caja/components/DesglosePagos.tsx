import { useState, useMemo } from 'react';
import {
  DollarSign,
  CreditCard,
  QrCode,
  ArrowRightLeft,
  Users,
  ChevronDown,
  ChevronUp,
  Eye,
  Printer,
  Receipt,
} from 'lucide-react';
import type { MedioPago } from '../../salon/types';
import type { PagoDetalle } from '../types';

// ─── Utilidades ───────────────────────────────────────────────────────────────

function fmt(valor: number): string {
  return valor.toLocaleString('es-AR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatHora(isoFecha: string): string {
  const d = new Date(isoFecha);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

// ─── Config visual por medio de pago ──────────────────────────────────────────

interface MedioPagoConfig {
  key: MedioPago;
  label: string;
  icon: React.ReactNode;
  color: string;     // text del monto
  bgBadge: string;   // fondo del badge
  bgBar: string;     // color de la barra porcentual
}

const MEDIOS_CONFIG: MedioPagoConfig[] = [
  {
    key: 'EFECTIVO',
    label: 'Efectivo',
    icon: <DollarSign size={18} />,
    color: 'text-emerald-400',
    bgBadge: 'bg-emerald-950/50 text-emerald-400 border-emerald-800/40',
    bgBar: 'bg-emerald-500',
  },
  {
    key: 'TARJETA',
    label: 'Tarjeta',
    icon: <CreditCard size={18} />,
    color: 'text-blue-400',
    bgBadge: 'bg-blue-950/50 text-blue-400 border-blue-800/40',
    bgBar: 'bg-blue-500',
  },
  {
    key: 'QR',
    label: 'QR',
    icon: <QrCode size={18} />,
    color: 'text-violet-400',
    bgBadge: 'bg-violet-950/50 text-violet-400 border-violet-800/40',
    bgBar: 'bg-violet-500',
  },
  {
    key: 'TRANSFERENCIA',
    label: 'Transferencia',
    icon: <ArrowRightLeft size={18} />,
    color: 'text-cyan-400',
    bgBadge: 'bg-cyan-950/50 text-cyan-400 border-cyan-800/40',
    bgBar: 'bg-cyan-500',
  },
  {
    key: 'A_CUENTA',
    label: 'A cuenta',
    icon: <Users size={18} />,
    color: 'text-gray-500',
    bgBadge: 'bg-neutral-800 text-gray-500 border-neutral-700',
    bgBar: 'bg-gray-600',
  },
];

// ─── Grupo agrupado ───────────────────────────────────────────────────────────

interface GrupoMedioPago {
  medio: MedioPago;
  config: MedioPagoConfig;
  pagos: PagoDetalle[];
  total: number;
  cantidad: number;
}

// ─── Stub ESC/POS ─────────────────────────────────────────────────────────────

function imprimirComprobantePago(pago: PagoDetalle): void {
  const buffer = [
    '================================',
    '     COMPROBANTE DE INGRESO     ',
    '================================',
    `Mesa:        ${pago.mesaNumero}`,
    `Pedido:      #${pago.numeroPedido}`,
    `Medio:       ${pago.medioPago}`,
    `Monto:       $${fmt(pago.monto)}`,
    `Fecha:       ${new Date(pago.fecha).toLocaleString('es-AR')}`,
    '================================',
    '',
  ].join('\n');

  // TODO: Reemplazar por invoke Tauri cuando el módulo de impresión esté listo
  // await window.__TAURI__.invoke('imprimir_comprobante_pago', { buffer });
  console.log('[ESC/POS Stub] imprimirComprobantePago →\n', buffer);
}

// ─── Skeleton ─────────────────────────────────────────────────────────────────

function DesgloseSkeleton() {
  return (
    <div className="space-y-3 animate-pulse">
      {Array.from({ length: 3 }).map((_, i) => (
        <div key={i} className="rounded-xl bg-neutral-800/40 p-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-neutral-700" />
            <div className="flex-1 space-y-1.5">
              <div className="h-3 w-20 rounded bg-neutral-700" />
              <div className="h-2 w-full rounded bg-neutral-700" />
            </div>
            <div className="h-5 w-20 rounded bg-neutral-700" />
          </div>
        </div>
      ))}
    </div>
  );
}

// ─── Fila individual de pago ──────────────────────────────────────────────────

interface FilaPagoProps {
  pago: PagoDetalle;
  config: MedioPagoConfig;
  onVerDetalle: (pago: PagoDetalle) => void;
}

function FilaPago({ pago, config, onVerDetalle }: FilaPagoProps) {
  return (
    <div className="flex items-center gap-3 px-4 py-2.5 hover:bg-neutral-800/60 transition-colors rounded-lg group">
      {/* Hora */}
      <span className="text-xs font-mono text-gray-600 w-11 shrink-0 tabular-nums">
        {formatHora(pago.fecha)}
      </span>

      {/* Mesa + Pedido */}
      <div className="flex-1 min-w-0">
        <p className="text-sm text-gray-300 truncate leading-tight">
          Mesa {pago.mesaNumero}
        </p>
        <p className="text-[11px] text-gray-600 font-mono truncate mt-0.5">
          Pedido #{pago.numeroPedido}
        </p>
      </div>

      {/* Monto */}
      <span className={`text-sm font-semibold font-mono tabular-nums shrink-0 ${config.color}`}>
        ${fmt(pago.monto)}
      </span>

      {/* Acciones */}
      <div className="flex items-center gap-1 shrink-0">
        <button
          type="button"
          onClick={() => onVerDetalle(pago)}
          className={[
            'p-1.5 rounded-lg text-gray-500',
            'hover:text-gray-200 hover:bg-neutral-700/60',
            'transition-colors active:scale-95',
          ].join(' ')}
          aria-label={`Ver detalle pedido #${pago.numeroPedido}`}
          title="Ver detalle"
        >
          <Eye size={14} />
        </button>
        <button
          type="button"
          onClick={() => imprimirComprobantePago(pago)}
          className={[
            'p-1.5 rounded-lg text-gray-500',
            'hover:text-gray-200 hover:bg-neutral-700/60',
            'transition-colors active:scale-95',
          ].join(' ')}
          aria-label={`Imprimir comprobante pedido #${pago.numeroPedido}`}
          title="Imprimir comprobante"
        >
          <Printer size={14} />
        </button>
      </div>
    </div>
  );
}

// ─── Grupo colapsable ─────────────────────────────────────────────────────────

interface GrupoColapsableProps {
  grupo: GrupoMedioPago;
  totalGeneral: number;
  onVerDetalle: (pago: PagoDetalle) => void;
}

function GrupoColapsable({ grupo, totalGeneral, onVerDetalle }: GrupoColapsableProps) {
  const [expandido, setExpandido] = useState(false);
  const { config, pagos, total, cantidad } = grupo;
  const pct = totalGeneral > 0 ? (total / totalGeneral) * 100 : 0;

  return (
    <div className="rounded-xl bg-neutral-800/30 border border-neutral-800/50 overflow-hidden">
      {/* Header del grupo — clickeable */}
      <button
        type="button"
        onClick={() => setExpandido(!expandido)}
        className={[
          'w-full flex items-center gap-3 px-4 py-3.5',
          'hover:bg-neutral-800/50 transition-colors',
          'text-left',
        ].join(' ')}
      >
        {/* Ícono */}
        <div className={`w-10 h-10 rounded-lg border flex items-center justify-center shrink-0 ${config.bgBadge}`}>
          {config.icon}
        </div>

        {/* Label + barra */}
        <div className="flex-1 min-w-0 space-y-1.5">
          <div className="flex items-center gap-2">
            <span className="text-sm font-medium text-gray-200">{config.label}</span>
            {/* Badge cantidad */}
            <span className="text-[10px] font-mono text-gray-500 bg-neutral-800 px-1.5 py-0.5 rounded">
              {cantidad} {cantidad === 1 ? 'pago' : 'pagos'}
            </span>
          </div>
          {/* Barra proporcional */}
          <div className="h-1.5 w-full rounded-full bg-neutral-700/40 overflow-hidden">
            <div
              className={`h-full rounded-full ${config.bgBar} transition-all duration-500 ease-out`}
              style={{ width: `${Math.max(pct, 2)}%` }}
            />
          </div>
        </div>

        {/* Total + chevron */}
        <span className={`text-base font-bold font-mono tabular-nums shrink-0 ${config.color}`}>
          ${fmt(total)}
        </span>
        <div className="text-gray-600 shrink-0 ml-1">
          {expandido ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </div>
      </button>

      {/* Lista expandida de pagos */}
      {expandido && (
        <div className="border-t border-neutral-800/50 py-1">
          {pagos.map((pago, idx) => (
            <FilaPago
              key={`${pago.pedidoId}-${pago.medioPago}-${idx}`}
              pago={pago}
              config={config}
              onVerDetalle={onVerDetalle}
            />
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Componente principal ─────────────────────────────────────────────────────

interface DesglosePagosProps {
  pagosDetalle: PagoDetalle[];
  isLoading: boolean;
  onVerDetallePedido: (pago: PagoDetalle) => void;
}

/**
 * Desglose interactivo por medio de pago.
 *
 * Agrupa los pagos individuales por medio de pago en secciones colapsables.
 * Cada grupo muestra: ícono, label, cantidad de pagos, barra proporcional y total.
 * Al expandir, se listan los pagos individuales con hora, mesa, pedido,
 * monto y botones de acción (ver detalle + imprimir comprobante).
 *
 * Inspirado en el patrón de la imagen de referencia (POS profesional).
 */
export default function DesglosePagos({
  pagosDetalle,
  isLoading,
  onVerDetallePedido,
}: DesglosePagosProps) {
  // ── Agrupar pagos por medio ───────────────────────────────────────────

  const grupos: GrupoMedioPago[] = useMemo(() => {
    if (!pagosDetalle || pagosDetalle.length === 0) return [];

    const mapa = new Map<MedioPago, PagoDetalle[]>();
    for (const pago of pagosDetalle) {
      const arr = mapa.get(pago.medioPago) ?? [];
      arr.push(pago);
      mapa.set(pago.medioPago, arr);
    }

    // Ordenar por config original para mantener orden visual consistente
    return MEDIOS_CONFIG
      .filter((cfg) => mapa.has(cfg.key))
      .map((cfg) => {
        const pagos = mapa.get(cfg.key)!;
        return {
          medio: cfg.key,
          config: cfg,
          pagos,
          total: pagos.reduce((acc, p) => acc + p.monto, 0),
          cantidad: pagos.length,
        };
      });
  }, [pagosDetalle]);

  const totalGeneral = grupos.reduce((acc, g) => acc + g.total, 0);

  // ── Skeleton ────────────────────────────────────────────────────────────

  if (isLoading) return <DesgloseSkeleton />;

  // ── Empty state ─────────────────────────────────────────────────────────

  if (grupos.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <Receipt size={32} className="text-neutral-700 mb-2" />
        <p className="text-sm text-gray-500">Sin ingresos registrados</p>
      </div>
    );
  }

  // ── Render ──────────────────────────────────────────────────────────────

  return (
    <div className="space-y-2">
      {grupos.map((grupo) => (
        <GrupoColapsable
          key={grupo.medio}
          grupo={grupo}
          totalGeneral={totalGeneral}
          onVerDetalle={onVerDetallePedido}
        />
      ))}
    </div>
  );
}
