import { useState, useCallback, useMemo } from 'react';
import { X, Minus, Plus, MessageSquare, ChefHat, Loader2, Check } from 'lucide-react';
import type { ProductoResponse } from '../../catalogo/types';
import type { CategoriaResponse } from '../../categorias/types';
import { useExtras, useModificadores } from '../../catalogo/hooks/useProductos';

// ─── Límites operativos ───────────────────────────────────────────────────────

/** Cantidad máxima de unidades del producto principal por línea */
const MAX_CANTIDAD_PRINCIPAL = 40;
/** Cantidad máxima de unidades de un extra individual */
const MAX_CANTIDAD_EXTRA = 40;

// ─── Tipos locales ────────────────────────────────────────────────────────────

interface ConfigurarProductoModalProps {
  producto: ProductoResponse;
  onConfirmar: (payload: ConfigurarProductoPayload) => void;
  onCerrar: () => void;
  /** Indica si la mutación está en curso */
  enviando?: boolean;
  /**
   * ID de la variante seleccionada explícitamente (si el producto pertenece a un grupo).
   * Se propaga al payload sin transformaciones.
   */
  varianteId?: string;
  /**
   * Categorías del local (ya cargadas). Se usan para:
   * 1. Resolver qué categorías son de extras (para useExtras genérico)
   * 2. Detectar si la categoría del producto tiene categoriaModificadoresId
   */
  categorias?: CategoriaResponse[];
}

/** Payload que el modal devuelve al confirmar */
export interface ConfigurarProductoPayload {
  productoId: string;
  cantidad: number;
  observaciones?: string;
  extrasIds: string[];
  /** ID de la variante seleccionada explícitamente */
  varianteId?: string;
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
  max,
  tamano = 'md',
}: {
  valor: number;
  onCambiar: (nueva: number) => void;
  min?: number;
  /** Límite superior opcional. Si se alcanza, el botón + se deshabilita */
  max?: number;
  tamano?: 'sm' | 'md';
}) {
  const esChico = tamano === 'sm';
  const enMaximo = max !== undefined && valor >= max;

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
        disabled={enMaximo}
        className={[
          'flex items-center justify-center rounded-lg',
          'transition-colors active:scale-[0.93]',
          'disabled:opacity-30 disabled:cursor-not-allowed',
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
      <ContadorCantidad valor={cantidad} onCambiar={onCambiar} min={0} max={MAX_CANTIDAD_EXTRA} tamano="sm" />
    </div>
  );
}

// ─── Sub-componente: Checkbox de Modificador (Salsas) ─────────────────────────

function ModificadorCheckbox({
  modificador,
  seleccionado,
  onToggle,
}: {
  modificador: ProductoResponse;
  seleccionado: boolean;
  onToggle: (id: string) => void;
}) {
  return (
    <button
      type="button"
      onClick={() => onToggle(modificador.id)}
      className={[
        'flex items-center gap-3',
        'w-full px-4 py-3.5 rounded-xl transition-all duration-150',
        'active:scale-[0.98]',
        seleccionado
          ? 'bg-red-950/30 border-2 border-red-600/60'
          : 'bg-neutral-800/50 border-2 border-transparent hover:border-neutral-700',
      ].join(' ')}
    >
      {/* Checkbox visual */}
      <div
        className={[
          'w-6 h-6 rounded-lg shrink-0',
          'flex items-center justify-center',
          'transition-all duration-150',
          seleccionado
            ? 'bg-red-600 border-red-500'
            : 'bg-neutral-700 border border-neutral-600',
        ].join(' ')}
      >
        {seleccionado && <Check size={14} strokeWidth={3} className="text-white" />}
      </div>

      {/* Nombre */}
      <span
        className={[
          'text-sm font-medium truncate',
          seleccionado ? 'text-gray-100' : 'text-gray-300',
        ].join(' ')}
      >
        {modificador.nombre}
      </span>

      {/* Precio (si > 0) */}
      {modificador.precio > 0 && (
        <span className="ml-auto text-xs text-red-400 font-semibold font-mono tabular-nums shrink-0">
          + $ {formatPrecio(modificador.precio)}
        </span>
      )}
    </button>
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
  varianteId,
  categorias = [],
}: ConfigurarProductoModalProps) {
  // ── Estado local ──
  const [cantidadPrincipal, setCantidadPrincipal] = useState(1);
  const [observaciones, setObservaciones] = useState('');
  /** Map<productoId, cantidad> para extras seleccionados */
  const [extrasSeleccionados, setExtrasSeleccionados] = useState<Record<string, number>>({});

  // ── Resolver categoría de modificadores (si existe) ──
  // Si la categoría del producto tiene categoriaModificadoresId → modo "modificadores específicos"
  // Si no → modo "extras genéricos" (comportamiento original)
  const categoriaDelProducto = useMemo(
    () => categorias.find((c) => c.id === producto.categoriaId),
    [categorias, producto.categoriaId]
  );
  const categoriaModificadoresId = categoriaDelProducto?.categoriaModificadoresId ?? null;

  // Nombre de la categoría de modificadores para el título de la sección
  const nombreCategoriaModificadores = useMemo(() => {
    if (!categoriaModificadoresId) return null;
    return categorias.find((c) => c.id === categoriaModificadoresId)?.nombre ?? 'Modificadores';
  }, [categorias, categoriaModificadoresId]);

  // ── Datos: extras disponibles ──
  // Los extras solo se muestran si:
  // 1. El producto NO es un extra en sí mismo (un extra no puede tener sub-extras)
  // 2. El producto permite extras (permiteExtras !== false; undefined → default true)
  const mostrarExtras = !producto.esExtra && producto.permiteExtras !== false;

  // Extras genéricos (se cargan SOLO si NO hay categoría de modificadores)
  const { data: todosExtras = [], isLoading: cargandoExtras } = useExtras(
    categoriaModificadoresId ? [] : categorias
  );

  // Modificadores específicos (se cargan SOLO si HAY categoría de modificadores)
  const { data: modificadores = [], isLoading: cargandoModificadores } = useModificadores(
    categoriaModificadoresId
  );

  // Si el producto no puede recibir disco extra (no está en variante máxima),
  // excluimos los extras que son modificadores estructurales (ej: disco de carne).
  const extras = producto.puedeAgregarDiscoExtra === false
    ? todosExtras.filter((e) => !e.esModificadorEstructural)
    : todosExtras;

  // ── Handlers ──

  const handleExtraCantidad = useCallback((extraId: string, cantidad: number) => {
    setExtrasSeleccionados((prev) => {
      if (cantidad <= 0) {
        const { [extraId]: _, ...rest } = prev;
        return rest;
      }
      // Limitar cantidad máxima de cada extra
      const cantidadLimitada = Math.min(cantidad, MAX_CANTIDAD_EXTRA);
      return { ...prev, [extraId]: cantidadLimitada };
    });
  }, []);

  /** Toggle on/off de un modificador (checkbox, cantidad siempre 1) */
  const handleToggleModificador = useCallback((modificadorId: string) => {
    setExtrasSeleccionados((prev) => {
      if (prev[modificadorId]) {
        const { [modificadorId]: _, ...rest } = prev;
        return rest;
      }
      return { ...prev, [modificadorId]: 1 };
    });
  }, []);

  // ── Construcción del payload ──

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
      varianteId,
    };
    onConfirmar(payload);
  }, [producto.id, cantidadPrincipal, observaciones, construirExtrasIds, onConfirmar, varianteId]);

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

            {/* ── Sección 2: Modificadores específicos ó Extras genéricos ── */}
            {mostrarExtras && categoriaModificadoresId ? (
            /* ── Modo MODIFICADORES (checkboxes, ej: Salsas para Panchos) ── */
            <section>
              <div className="flex items-center gap-2 mb-2.5">
                <ChefHat size={16} className="text-gray-500" />
                <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider">
                  {nombreCategoriaModificadores}
                </h3>
                {hayExtrasSeleccionados && (
                  <span className="ml-auto text-[10px] font-semibold text-red-400 uppercase tracking-wider">
                    {Object.keys(extrasSeleccionados).length} seleccionados
                  </span>
                )}
              </div>

              {cargandoModificadores ? (
                <div className="flex items-center justify-center py-8 gap-2">
                  <Loader2 size={18} className="text-gray-600 animate-spin" />
                  <span className="text-sm text-gray-600">Cargando {nombreCategoriaModificadores?.toLowerCase()}...</span>
                </div>
              ) : modificadores.length === 0 ? (
                <p className="text-sm text-gray-600 text-center py-6">
                  No hay {nombreCategoriaModificadores?.toLowerCase()} disponibles
                </p>
              ) : (
                <div className="grid grid-cols-2 gap-2 max-h-[280px] overflow-y-auto pr-1">
                  {modificadores.map((mod) => (
                    <ModificadorCheckbox
                      key={mod.id}
                      modificador={mod}
                      seleccionado={!!extrasSeleccionados[mod.id]}
                      onToggle={handleToggleModificador}
                    />
                  ))}
                </div>
              )}
            </section>

            ) : mostrarExtras ? (
            /* ── Modo EXTRAS genéricos (contadores, comportamiento original) ── */
            <section>
              <div className="flex items-center gap-2 mb-2.5">
                <ChefHat size={16} className="text-gray-500" />
                <h3 className="text-sm font-semibold text-gray-400 uppercase tracking-wider">
                  Agregados / Extras
                </h3>
                {hayExtrasSeleccionados && (
                  <span className="ml-auto text-[10px] font-semibold text-red-400 uppercase tracking-wider">
                    {Object.values(extrasSeleccionados).reduce((a, b) => a + b, 0)} seleccionados
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
            ) : !producto.esExtra && producto.permiteExtras === false ? (
              <section>
                <div className="flex items-center gap-2 mb-2">
                  <ChefHat size={16} className="text-gray-600" />
                  <p className="text-sm text-gray-500 italic">
                    Este producto no admite extras
                  </p>
                </div>
              </section>
            ) : null}
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
                onCambiar={(v) => setCantidadPrincipal(Math.min(v, MAX_CANTIDAD_PRINCIPAL))}
                min={1}
                max={MAX_CANTIDAD_PRINCIPAL}
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
                  Agregar al Pedido
                  {cantidadPrincipal > 1 && (
                    <span className="text-red-200 font-normal text-sm ml-1">
                      ({cantidadPrincipal}x)
                    </span>
                  )}
                </>
              )}
            </button>
          </footer>
        </div>
      </div>
    </>
  );
}
