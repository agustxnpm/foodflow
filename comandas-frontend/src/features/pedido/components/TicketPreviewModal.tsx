import { useState, useEffect, useCallback, useRef } from 'react';
import { X, Printer, Loader2, Receipt } from 'lucide-react';
import type { TicketImpresionResponse } from '../types-impresion';
import { useObtenerTicket, useGenerarTicketEscPos } from '../../salon/hooks/useMesas';
import { imprimirEscPos } from '../services/printerService';
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
  const generarTicketEscPos = useGenerarTicketEscPos();

  const [ticketData, setTicketData] = useState<TicketImpresionResponse | null>(
    null
  );
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [imprimiendo, setImprimiendo] = useState(false);

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

  // ── Imprimir ticket vía impresora térmica ESC/POS ──
  const handleImprimir = useCallback(async () => {
    if (imprimiendo) return;
    setImprimiendo(true);

    try {
      const response = await generarTicketEscPos.mutateAsync(mesaId);
      const result = await imprimirEscPos(
        response.escPosBase64,
        `Ticket Mesa ${ticketData?.header?.numeroMesa ?? mesaId}`
      );

      if (!result.success) {
        console.error('[TicketPreviewModal] Error de impresión:', result.message);
      }
    } catch (err) {
      console.error('[TicketPreviewModal] Error generando ticket ESC/POS:', err);
    } finally {
      setImprimiendo(false);
    }
  }, [mesaId, imprimiendo, generarTicketEscPos, ticketData]);

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
                disabled={imprimiendo}
                className="
                  flex-1 flex items-center justify-center gap-2
                  h-11 rounded-xl
                  text-sm font-semibold
                  bg-red-600 text-white
                  hover:bg-red-500
                  disabled:bg-neutral-700 disabled:text-gray-500 disabled:cursor-not-allowed
                  transition-colors active:scale-[0.98]
                  shadow-sm shadow-red-950/40
                "
              >
                {imprimiendo ? (
                  <>
                    <Loader2 size={16} className="animate-spin" />
                    <span>Imprimiendo…</span>
                  </>
                ) : (
                  <>
                    <Printer size={16} />
                    <span>Imprimir</span>
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
