import { useState, useMemo, useCallback } from 'react';
import { X, Minus, Plus, MessageSquare, ChefHat, Loader2 } from 'lucide-react';
import type { ProductoResponse } from '../../catalogo/types';
import { useExtras } from '../../catalogo/hooks/useProductos';

// ─── Tipos locales ────────────────────────────────────────────────────────────

interface ConfigurarProductoModalProps {
  producto: ProductoResponse;
  onConfirmar: (payload: ConfigurarProductoPayload) => void;
  onCerrar: () => void;
  /** Indica si la mutación está en curso */
  enviando?: boolean;
}

/** Payload que el modal devuelve al confirmar */
export interface ConfigurarProductoPayload {
  productoId: string;
  cantidad: number;
  observaciones?: string;
  extrasIds: string[];
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatPrecio(monto: number): string {
  return monto.toLocaleString('es-AR');
}

// ─── Sub-componente: Control de cantidad ──────────────────────────────────────

function ContadorCantidad({
  valor,
  onCambiar,
  min = 0,
  tamano = 'md',
}: {
  valor: number;
  onCambiar: (nueva: number) => void;
  min?: number;
  tamano?: 'sm' | 'md';
}) {
  const esChico = tamano === 'sm';

  return (
    <div className="flex items-center gap-1">
      <button
        type="button"
        onClick={() => onCambiar(Math.max(min, valor - 1))}
        disabled={valor <= min}
        className={[
          'flex items-center justify-center rounded-lg',
          'transition-colors active:scale-[0.93]',
          'disabled:opacity-30 disabled:cursor-not-allowed',
          esChico
            ? 'w-8 h-8 bg-neutral-800 hover:bg-neutral-700 text-gray-400'
            : 'w-10 h-10 bg-neutral-800 hover:bg-neutral-700 text-gray-300',
        ].join(' ')}
        aria-label="Disminuir cantidad"
      >
        <Minus size={esChico ? 14 : 16} strokeWidth={2.5} />
      </button>

      <span
        className={[
          'font-bold tabular-nums text-center select-none',
          esChico
            ? 'text-sm text-gray-300 w-6'
            : 'text-lg text-gray-100 w-8',
        ].join(' ')}
      >
        {valor}
      </span>

      <button
        type="button"
        onClick={() => onCambiar(valor + 1)}
        className={[
          'flex items-center justify-center rounded-lg',
          'transition-colors active:scale-[0.93]',
          esChico
            ? 'w-8 h-8 bg-neutral-800 hover:bg-neutral-700 text-gray-400'
            : 'w-10 h-10 bg-neutral-800 hover:bg-neutral-700 text-gray-300',
        ].join(' ')}
        aria-label="Aumentar cantidad"
      >
        <Plus size={esChico ? 14 : 16} strokeWidth={2.5} />
      </button>
    </div>
  );
}

// ─── Sub-componente: Fila de Extra ────────────────────────────────────────────

function ExtraRow({
  extra,
  cantidad,
  onCambiar,
}: {
  extra: ProductoResponse;
  cantidad: number;
  onCambiar: (nueva: number) => void;
}) {
  return (
    <div
      className={[
        'flex items-center justify-between gap-3',
        'px-4 py-3 rounded-xl transition-colors',
        cantidad > 0
          ? 'bg-red-950/20 border border-red-900/40'
          : 'bg-neutral-800/50 border border-transparent hover:border-neutral-700',
      ].join(' ')}
    >
      {/* Info del extra */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-gray-200 truncate">
          {extra.nombre}
        </p>
        <p className="text-xs text-red-400 font-semibold font-mono tabular-nums mt-0.5">
          + $ {formatPrecio(extra.precio)}
        </p>
      </div>

      {/* Controles de cantidad */}
      <ContadorCantidad valor={cantidad} onCambiar={onCambiar} min={0} tamano="sm" />
    </div>
  );
}

// ─── Componente Principal ─────────────────────────────────────────────────────

/**
 * Modal de configuración de producto antes de agregarlo al pedido.
 *
 * Permite al operador:
 * 1. Agregar observaciones libres (sin cebolla, aderezo aparte, etc.)
 * 2. Seleccionar extras disponibles (huevo, queso, disco de carne)
 * 3. Definir la cantidad del producto principal
 *
 * REGLA CRÍTICA: El frontend NO calcula normalizaciones (ej: simple → doble).
 * Esa lógica vive en NormalizadorVariantesService del backend.
 * El frontend solo envía IDs crudos.
 *
 * El array extrasIds repite el UUID del extra tantas veces como su cantidad.
 * Ej: 2x Disco de carne → [uuid, uuid]
 */
export default function ConfigurarProductoModal({
  producto,
  onConfirmar,
  onCerrar,
  enviando = false,
}: ConfigurarProductoModalProps) {
  // ── Estado local ──
  const [cantidadPrincipal, setCantidadPrincipal] = useState(1);
  const [observaciones, setObservaciones] = useState('');
  /** Map<productoId, cantidad> para extras seleccionados */
  const [extrasSeleccionados, setExtrasSeleccionados] = useState<Record<string, number>>({});

  // ── Datos: extras disponibles ──
  const { data: extras = [], isLoading: cargandoExtras } = useExtras();

  // ── Handlers ──

  const handleExtraCantidad = useCallback((extraId: string, cantidad: number) => {
    setExtrasSeleccionados((prev) => {
      if (cantidad <= 0) {
        const { [extraId]: _, ...rest } = prev;
        return rest;
      }
      return { ...prev, [extraId]: cantidad };
    });
  }, []);

  // ── Cálculos ──

  /** Subtotal de los extras seleccionados */
  const subtotalExtras = useMemo(() => {
    return Object.entries(extrasSeleccionados).reduce((acc, [extraId, qty]) => {
      const extra = extras.find((e) => e.id === extraId);
      return acc + (extra ? extra.precio * qty : 0);
    }, 0);
  }, [extrasSeleccionados, extras]);

  /** Total dinámico del footer */
  const totalDinamico = useMemo(() => {
    return (producto.precio + subtotalExtras) * cantidadPrincipal;
  }, [producto.precio, subtotalExtras, cantidadPrincipal]);

  /** Construir el array extrasIds (IDs repetidos según cantidad) */
  const construirExtrasIds = useCallback((): string[] => {
    const ids: string[] = [];
    for (const [extraId, qty] of Object.entries(extrasSeleccionados)) {
      for (let i = 0; i < qty; i++) {
        ids.push(extraId);
      }
    }
    return ids;
  }, [extrasSeleccionados]);

  const handleConfirmar = useCallback(() => {
    const payload: ConfigurarProductoPayload = {
      productoId: producto.id,
      cantidad: cantidadPrincipal,
      observaciones: observaciones.trim() || undefined,
      extrasIds: construirExtrasIds(),
    };
    onConfirmar(payload);
  }, [producto.id, cantidadPrincipal, observaciones, construirExtrasIds, onConfirmar]);

  const hayExtrasSeleccionados = Object.values(extrasSeleccionados).some((q) => q > 0);

  return (
    <>
      {/* ── Backdrop ── */}
      <div
        className="fixed inset-0 z-[80] bg-black/60 backdrop-blur-sm"
        onClick={onCerrar}
        aria-hidden="true"
      />

      {/* ── Modal ── */}
      <div
        className="fixed inset-0 z-[90] flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-label={`Configurar ${producto.nombre}`}
      >
        <div
          className="
            w-full max-w-lg
            bg-neutral-950 border border-neutral-800
            rounded-2xl shadow-2xl shadow-black/60
            flex flex-col
            max-h-[85vh]
            animate-modal-in
          "
        >
          {/* ── Cabecera ── */}
          <header className="flex items-start justify-between gap-4 px-5 pt-5 pb-4 border-b border-neutral-800 shrink-0">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <span
                  className="w-3 h-3 rounded-full shrink-0 border border-white/10"
                  style={{ backgroundColor: producto.colorHex || '#FFFFFF' }}
                />
                <h2 className="text-lg font-bold text-gray-100 truncate">
                  {producto.nombre}
                </h2>
              </div>
              <p className="text-xl font-bold text-red-500 font-mono tabular-nums">
                $ {formatPrecio(producto.precio)}
              </p>
            </div>

            <button
              type="button"
              onClick={onCerrar}
              className="
                w-9 h-9 rounded-xl
                flex items-center justify-center
                bg-neutral-800 text-gray-500
                hover:text-gray-300 hover:bg-neutral-700
                transition-colors active:scale-[0.93]
              "
              aria-label="Cerrar"
            >
              <X size={18} />
            </button>
          </header>

          {/* ── Cuerpo scrollable ── */}
          <div className="flex-1 overflow-y-auto px-5 py-4 space-y-5">

            {/* ── Sección 1: Observaciones ── */}
            <section>
              <div className="flex items-center gap-2 mb-2.5">
                <MessageSquare size={16} className="text-gray-500" />
                <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider">
                  Observaciones
                </h3>
              </div>
              <textarea
                value={observaciones}
                onChange={(e) => setObservaciones(e.target.value)}
                placeholder="Ej: Sin cebolla, aderezo aparte..."
                rows={3}
                className="
                  w-full px-4 py-3 rounded-xl
                  bg-neutral-800 border border-neutral-700
                  text-sm text-gray-200 placeholder:text-gray-600
                  resize-none
                  focus:outline-none focus:border-red-600/50 focus:ring-1 focus:ring-red-600/30
                  transition-colors
                "
              />
            </section>

            {/* ── Sección 2: Extras / Agregados ── */}
            <section>
              <div className="flex items-center gap-2 mb-2.5">
                <ChefHat size={16} className="text-gray-500" />
                <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider">
                  Agregados / Extras
                </h3>
                {hayExtrasSeleccionados && (
                  <span className="ml-auto text-xs font-semibold text-red-400 font-mono tabular-nums">
                    + $ {formatPrecio(subtotalExtras)}
                  </span>
                )}
              </div>

              {cargandoExtras ? (
                <div className="flex items-center justify-center py-8 gap-2">
                  <Loader2 size={18} className="text-gray-600 animate-spin" />
                  <span className="text-sm text-gray-600">Cargando extras...</span>
                </div>
              ) : extras.length === 0 ? (
                <p className="text-sm text-gray-600 text-center py-6">
                  No hay extras disponibles en el catálogo
                </p>
              ) : (
                <div className="space-y-2 max-h-[240px] overflow-y-auto pr-1">
                  {extras.map((extra) => (
                    <ExtraRow
                      key={extra.id}
                      extra={extra}
                      cantidad={extrasSeleccionados[extra.id] ?? 0}
                      onCambiar={(qty) => handleExtraCantidad(extra.id, qty)}
                    />
                  ))}
                </div>
              )}
            </section>
          </div>

          {/* ── Footer fijo: Cantidad principal + Botón Agregar ── */}
          <footer className="px-5 py-4 border-t border-neutral-800 shrink-0 space-y-3">
            {/* Fila: Cantidad del producto principal */}
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-gray-400">
                Cantidad
              </span>
              <ContadorCantidad
                valor={cantidadPrincipal}
                onCambiar={setCantidadPrincipal}
                min={1}
                tamano="md"
              />
            </div>

            {/* Botón Agregar */}
            <button
              type="button"
              onClick={handleConfirmar}
              disabled={enviando}
              className="
                w-full h-14 rounded-xl
                flex items-center justify-center gap-3
                text-base font-bold text-white
                bg-red-600 hover:bg-red-500
                disabled:opacity-50 disabled:cursor-not-allowed
                transition-colors active:scale-[0.98]
                shadow-lg shadow-red-950/40
              "
            >
              {enviando ? (
                <>
                  <Loader2 size={20} className="animate-spin" />
                  Agregando…
                </>
              ) : (
                <>
                  <Plus size={20} strokeWidth={2.5} />
                  Agregar al Pedido — $ {formatPrecio(totalDinamico)}
                </>
              )}
            </button>
          </footer>
        </div>
      </div>
    </>
  );
}
