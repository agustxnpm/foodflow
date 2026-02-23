import { useState, useEffect, useCallback, useRef } from 'react';
import { X, Printer, Loader2, Receipt } from 'lucide-react';
import type { TicketImpresionResponse } from '../types-impresion';
import { useObtenerTicket } from '../../salon/hooks/useMesas';
import TicketPreview from './TicketPreview';

// ─── Props ────────────────────────────────────────────────────────────────────

interface TicketPreviewModalProps {
  mesaId: string;
  onClose: () => void;
}

// ─── Componente ───────────────────────────────────────────────────────────────

/**
 * Modal de Control de Mesa — Preview del ticket para revisión con el cliente.
 *
 * Permite al operador mostrar la cuenta al cliente antes de cobrar,
 * sin necesidad de iniciar el cierre de mesa. Opcionalmente se puede
 * imprimir el ticket para entregárselo al cliente.
 *
 * Flujo:
 * 1. Se abre desde el botón "Control de Mesa" en TicketPedido
 * 2. Carga el ticket vía GET /mesas/{mesaId}/ticket
 * 3. Muestra el TicketPreview (estilo impresora térmica)
 * 4. Permite imprimir o volver al pedido
 *
 * No modifica el estado del pedido ni de la mesa.
 */
export default function TicketPreviewModal({
  mesaId,
  onClose,
}: TicketPreviewModalProps) {
  const obtenerTicket = useObtenerTicket();

  const [ticketData, setTicketData] = useState<TicketImpresionResponse | null>(
    null
  );
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const ticketRef = useRef<HTMLDivElement>(null);

  // ── Cargar ticket al montar ──
  useEffect(() => {
    obtenerTicket.mutate(mesaId, {
      onSuccess: (data) => {
        setTicketData(data);
        setCargando(false);
      },
      onError: () => {
        setError('No se pudo cargar el ticket');
        setCargando(false);
      },
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mesaId]);

  // ── Imprimir ticket en ventana nueva ──
  const handleImprimir = useCallback(() => {
    if (!ticketRef.current) return;

    const contenido = ticketRef.current.innerHTML;
    const ventana = window.open('', '_blank', 'width=360,height=640');
    if (!ventana) return;

    ventana.document.write(`
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8" />
        <title>Ticket</title>
        <style>
          * { margin: 0; padding: 0; box-sizing: border-box; }
          body {
            font-family: 'Courier New', Courier, monospace;
            font-size: 11px;
            line-height: 1.6;
            width: 280px;
            margin: 0 auto;
            padding: 12px;
            color: #1a1a1a;
            background: #fff;
          }
          .ticket-print-wrapper > div {
            background: #fff !important;
            color: #1a1a1a !important;
            border: none !important;
            box-shadow: none !important;
            max-width: 100% !important;
            padding: 0 !important;
            border-radius: 0 !important;
          }
          .ticket-print-wrapper span,
          .ticket-print-wrapper p,
          .ticket-print-wrapper div {
            color: #1a1a1a !important;
          }
          @media print {
            body { width: 58mm; margin: 0; padding: 2mm; }
          }
        </style>
      </head>
      <body>
        <div class="ticket-print-wrapper">${contenido}</div>
        <script>
          window.onload = function() {
            window.print();
            window.onafterprint = function() { window.close(); };
            setTimeout(function() { window.close(); }, 10000);
          };
        <\/script>
      </body>
      </html>
    `);
    ventana.document.close();
  }, []);

  return (
    <>
      {/* ── Backdrop ── */}
      <div
        className="fixed inset-0 z-[80] bg-black/60 backdrop-blur-sm animate-backdrop-in"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* ── Modal ── */}
      <div
        className="fixed inset-0 z-[85] flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-label="Control de mesa — Preview del ticket"
      >
        <div
          className="
            bg-neutral-900 rounded-2xl border border-neutral-700
            shadow-2xl shadow-black/60
            w-full max-w-md
            flex flex-col
            max-h-[85vh]
            animate-modal-in
          "
        >
          {/* ── Header ── */}
          <div className="flex items-center justify-between px-5 py-4 border-b border-neutral-800">
            <div className="flex items-center gap-2">
              <Receipt size={18} className="text-red-400" />
              <h3 className="text-lg font-bold text-gray-100">
                Control de Mesa
              </h3>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="
                w-8 h-8 rounded-lg
                flex items-center justify-center
                text-gray-500
                hover:bg-neutral-800 hover:text-gray-300
                transition-colors
              "
              aria-label="Cerrar"
            >
              <X size={18} />
            </button>
          </div>

          {/* ── Contenido ── */}
          <div className="flex-1 overflow-y-auto p-5">
            {cargando ? (
              <div className="flex flex-col items-center justify-center py-12 gap-3">
                <Loader2 size={28} className="animate-spin text-red-400" />
                <p className="text-sm text-gray-500">Generando ticket…</p>
              </div>
            ) : error ? (
              <div className="flex flex-col items-center justify-center py-12 gap-3">
                <p className="text-sm text-red-400">{error}</p>
                <button
                  type="button"
                  onClick={onClose}
                  className="text-sm text-gray-400 underline hover:text-gray-200"
                >
                  Volver
                </button>
              </div>
            ) : ticketData ? (
              <div ref={ticketRef}>
                <TicketPreview ticket={ticketData} />
              </div>
            ) : null}
          </div>

          {/* ── Footer con acciones ── */}
          {ticketData && (
            <div className="px-5 py-4 border-t border-neutral-800 flex gap-3">
              <button
                type="button"
                onClick={onClose}
                className="
                  flex-1 h-11 rounded-xl
                  text-sm font-semibold
                  bg-neutral-800 text-gray-300
                  border border-neutral-700
                  hover:border-neutral-600 hover:text-gray-100
                  transition-colors active:scale-[0.98]
                "
              >
                Volver al pedido
              </button>

              <button
                type="button"
                onClick={handleImprimir}
                className="
                  flex-1 flex items-center justify-center gap-2
                  h-11 rounded-xl
                  text-sm font-semibold
                  bg-red-600 text-white
                  hover:bg-red-500
                  transition-colors active:scale-[0.98]
                  shadow-sm shadow-red-950/40
                "
              >
                <Printer size={16} />
                <span>Imprimir</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
