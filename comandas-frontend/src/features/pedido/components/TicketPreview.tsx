import type {
  TicketImpresionResponse,
  ItemTicket,
  ExtraTicket,
  DesgloseAjuste,
} from '../types-impresion';

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatMoney(amount: number): string {
  return amount.toLocaleString('es-AR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatFecha(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString('es-AR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

function formatHora(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleTimeString('es-AR', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** Línea punteada separadora (simula el corte de impresora térmica) */
function Separador() {
  return (
    <div
      className="border-b border-dashed border-neutral-400 my-2"
      aria-hidden="true"
    />
  );
}

// ─── Componente ───────────────────────────────────────────────────────────────

interface TicketPreviewProps {
  ticket: TicketImpresionResponse;
}

/**
 * Preview visual del ticket de venta — estilo impresora térmica.
 *
 * Renderiza la estructura de TicketImpresionResponse simulando
 * un recibo térmico: fondo claro, fuente monoespaciada, 58mm de ancho visual.
 *
 * Se usa dentro del CerrarMesaModal como columna izquierda.
 */
export default function TicketPreview({ ticket }: TicketPreviewProps) {
  const { header, items, totales, footer } = ticket;

  return (
    <div
      className="
        w-full max-w-[280px] mx-auto
        bg-amber-50 text-neutral-900
        font-mono text-[11px] leading-relaxed
        px-4 py-5
        rounded-lg
        shadow-inner shadow-neutral-300/30
        border border-neutral-300
        select-none
        overflow-y-auto
      "
      role="img"
      aria-label="Preview del ticket de venta"
    >
      {/* ── Header del local ── */}
      <div className="text-center space-y-0.5">
        <p className="text-sm font-bold uppercase tracking-wider">
          {header.nombreLocal}
        </p>
        {header.direccion && (
          <p className="text-[10px] text-neutral-600">{header.direccion}</p>
        )}
        {header.telefono && (
          <p className="text-[10px] text-neutral-600">Tel: {header.telefono}</p>
        )}
        {header.cuit && (
          <p className="text-[10px] text-neutral-600">CUIT: {header.cuit}</p>
        )}
      </div>

      <Separador />

      {/* ── Datos del ticket ── */}
      <div className="flex justify-between text-[10px] text-neutral-600">
        <span>Mesa {header.numeroMesa}</span>
        <span>{formatFecha(header.fechaHora)}</span>
      </div>
      <div className="text-right text-[10px] text-neutral-600">
        {formatHora(header.fechaHora)}
      </div>

      <Separador />

      {/* ── Cabecera de columnas ── */}
      <div className="flex justify-between text-[10px] font-bold text-neutral-500 uppercase tracking-wider">
        <span className="w-6 text-right">Cnt</span>
        <span className="flex-1 ml-2">Descripción</span>
        <span className="w-16 text-right">P.Unit</span>
        <span className="w-16 text-right">Import</span>
      </div>

      <div className="border-b border-neutral-300 my-1" />

      {/* ── Ítems ── */}
      {items.map((item: ItemTicket, idx: number) => (
        <div key={idx}>
          {/* Línea principal del ítem */}
          <div className="flex justify-between py-0.5">
            <span className="w-6 text-right tabular-nums">{item.cantidad}</span>
            <span className="flex-1 ml-2 truncate">{item.descripcion}</span>
            <span className="w-16 text-right tabular-nums">
              {formatMoney(item.precioUnitario)}
            </span>
            <span className="w-16 text-right tabular-nums font-semibold">
              {formatMoney(item.importe)}
            </span>
          </div>
          {/* Sub-líneas de extras (indentadas) */}
          {item.extras && item.extras.length > 0 && (
            <div className="pl-8 space-y-0">
              {item.extras.map((extra: ExtraTicket, eidx: number) => (
                <div key={eidx} className="flex justify-between text-[10px] text-neutral-500">
                  <span className="flex-1 truncate">
                    + {extra.cantidad > 1 ? `${extra.cantidad}x ` : ''}{extra.nombre}
                  </span>
                  <span className="w-16 text-right tabular-nums">
                    {formatMoney(extra.subtotal)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      ))}

      <Separador />

      {/* ── Totales ── */}
      <div className="space-y-0.5">
        <div className="flex justify-between">
          <span>Subtotal</span>
          <span className="tabular-nums">${formatMoney(totales.subtotal)}</span>
        </div>

        {totales.montoDescuentoPromos > 0 && (
          <div className="flex justify-between text-neutral-600">
            <span>Desc. Promos</span>
            <span className="tabular-nums">
              -${formatMoney(totales.montoDescuentoPromos)}
            </span>
          </div>
        )}

        {totales.montoDescuentoManual > 0 && (
          <div className="flex justify-between text-neutral-600">
            <span>Desc. Manual</span>
            <span className="tabular-nums">
              -${formatMoney(totales.montoDescuentoManual)}
            </span>
          </div>
        )}

        {/* Desglose de ajustes económicos */}
        {(totales.desgloseAjustes?.length ?? 0) > 0 && (
          <div className="pl-2 space-y-0.5">
            {totales.desgloseAjustes!.map(
              (ajuste: DesgloseAjuste, idx: number) => (
                <div
                  key={idx}
                  className="flex justify-between text-[10px] text-neutral-500"
                >
                  <span className="truncate">↳ {ajuste.descripcion}</span>
                  <span className="tabular-nums">
                    -${formatMoney(ajuste.monto)}
                  </span>
                </div>
              )
            )}
          </div>
        )}

        <div className="border-b border-double border-neutral-400 my-1" />

        <div className="flex justify-between text-sm font-bold">
          <span>TOTAL</span>
          <span className="tabular-nums">${formatMoney(totales.totalFinal)}</span>
        </div>
      </div>

      <Separador />

      {/* ── Footer ── */}
      <div className="text-center space-y-1">
        {footer.mensajeBienvenida && (
          <p className="text-[10px] text-neutral-500 italic">
            {footer.mensajeBienvenida}
          </p>
        )}
        <p className="text-[10px] text-neutral-400">*** Gracias por su visita ***</p>
      </div>
    </div>
  );
}
