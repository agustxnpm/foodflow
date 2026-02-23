import { useState, useMemo } from 'react';
import { X, Check, Search, ArrowRight, ArrowLeft, Zap, Gift, ShoppingBag, Info } from 'lucide-react';
import { useProductos } from '../../catalogo/hooks/useProductos';
import { useAsociarProductos } from '../hooks/usePromociones';
import type { ProductoResponse } from '../../catalogo/types';
import type { PromocionResponse, RolPromocion, ItemScopeParams, TipoEstrategia } from '../types';

// ── Constantes de dominio ─────────────────────────────────────────────────────

/**
 * Descripción contextual del alcance para cada estrategia.
 * Le indica al operador qué productos debe seleccionar y por qué.
 *
 * Regla de dominio (MOTOR_PROMOCIONES.md §6):
 * Solo COMBO_CONDICIONAL necesita distinguir activadores de beneficiados.
 * Las otras 3 estrategias solo necesitan TARGETs ("¿a qué productos aplica?").
 */
const AYUDA_ALCANCE: Record<TipoEstrategia, string> = {
  DESCUENTO_DIRECTO: 'Seleccioná los productos que tendrán el descuento aplicado.',
  CANTIDAD_FIJA: 'Seleccioná los productos que participan de la promoción NxM.',
  COMBO_CONDICIONAL:
    'Definí qué productos activan la promo y cuáles reciben el beneficio.',
  PRECIO_FIJO_CANTIDAD: 'Seleccioná los productos que forman parte del pack.',
};

const ESTRATEGIA_LABEL: Record<TipoEstrategia, string> = {
  DESCUENTO_DIRECTO: 'Descuento',
  CANTIDAD_FIJA: 'NxM',
  COMBO_CONDICIONAL: 'Combo',
  PRECIO_FIJO_CANTIDAD: 'Pack',
};

const ESTRATEGIA_COLOR: Record<TipoEstrategia, string> = {
  DESCUENTO_DIRECTO: '#A855F7',
  CANTIDAD_FIJA: '#3B82F6',
  COMBO_CONDICIONAL: '#F59E0B',
  PRECIO_FIJO_CANTIDAD: '#10B981',
};

// ── Tipos internos ────────────────────────────────────────────────────────────

interface ItemConRol {
  producto: ProductoResponse;
  rol: RolPromocion;
}

interface AsociarScopeModalProps {
  promocion: PromocionResponse;
  onClose: () => void;
}

// ── Componente ────────────────────────────────────────────────────────────────

/**
 * Modal para definir el alcance de una promoción (HU-09).
 *
 * Diseño condicional según estrategia:
 * - COMBO_CONDICIONAL: Transfer list con selector de rol (Activador / Beneficiado)
 * - Resto: Transfer list simple — todo producto movido es TARGET (beneficiado)
 *
 * La regla de dominio es clara:
 * "Solo COMBO_CONDICIONAL necesita TRIGGERs. Las demás solo necesitan TARGETs."
 * @see MOTOR_PROMOCIONES.md §6 — Sistema de Alcance (Scope)
 */
export default function AsociarScopeModal({ promocion, onClose }: AsociarScopeModalProps) {
  const { data: todosLosProductos = [] } = useProductos();
  const asociarProductos = useAsociarProductos();

  const esCombo = promocion.estrategia.tipo === 'COMBO_CONDICIONAL';

  const [busqueda, setBusqueda] = useState('');
  const [rolSeleccionado, setRolSeleccionado] = useState<RolPromocion>('TARGET');

  // Estado: productos en el alcance (con su rol)
  const [asociados, setAsociados] = useState<ItemConRol[]>(() =>
    promocion.alcance.items
      .map((item) => {
        const producto = todosLosProductos.find((p) => p.id === item.referenciaId);
        if (!producto) return null;
        return { producto, rol: item.rol };
      })
      .filter(Boolean) as ItemConRol[]
  );

  // Selección para la transferencia
  const [seleccionIzq, setSeleccionIzq] = useState<Set<string>>(new Set());
  const [seleccionDer, setSeleccionDer] = useState<Set<string>>(new Set());

  const idsAsociados = useMemo(
    () => new Set(asociados.map((a) => a.producto.id)),
    [asociados]
  );

  // Productos disponibles (no asociados + filtro de búsqueda)
  const productosDisponibles = useMemo(
    () =>
      todosLosProductos
        .filter((p) => !idsAsociados.has(p.id))
        .filter((p) => p.nombre.toLowerCase().includes(busqueda.toLowerCase())),
    [todosLosProductos, idsAsociados, busqueda]
  );

  // Separar asociados por rol (para combo)
  const activadores = useMemo(() => asociados.filter((a) => a.rol === 'TRIGGER'), [asociados]);
  const beneficiados = useMemo(() => asociados.filter((a) => a.rol === 'TARGET'), [asociados]);

  // ── Acciones de transferencia ──

  const moverADerecha = () => {
    const nuevos: ItemConRol[] = [];
    seleccionIzq.forEach((id) => {
      const producto = todosLosProductos.find((p) => p.id === id);
      if (producto) {
        // Para no-combo, siempre TARGET. Para combo, usa el rol seleccionado.
        nuevos.push({ producto, rol: esCombo ? rolSeleccionado : 'TARGET' });
      }
    });
    setAsociados((prev) => [...prev, ...nuevos]);
    setSeleccionIzq(new Set());
  };

  const moverAIzquierda = () => {
    setAsociados((prev) =>
      prev.filter((a) => !seleccionDer.has(a.producto.id))
    );
    setSeleccionDer(new Set());
  };

  const toggleSeleccion = (
    id: string,
    setter: React.Dispatch<React.SetStateAction<Set<string>>>
  ) => {
    setter((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  // ── Guardar ──

  const handleGuardar = () => {
    const items: ItemScopeParams[] = asociados.map((a) => ({
      referenciaId: a.producto.id,
      tipo: 'PRODUCTO' as const,
      rol: a.rol,
    }));

    asociarProductos.mutate(
      { id: promocion.id, items },
      { onSuccess: onClose }
    );
  };

  // ── Helpers ──

  function describirEstrategia(): string {
    const { estrategia } = promocion;
    switch (estrategia.tipo) {
      case 'DESCUENTO_DIRECTO':
        return estrategia.modoDescuento === 'PORCENTAJE'
          ? `${estrategia.valorDescuento}% OFF`
          : `$${estrategia.valorDescuento?.toLocaleString('es-AR')} OFF`;
      case 'CANTIDAD_FIJA':
        return `${estrategia.cantidadLlevas}x${estrategia.cantidadPagas}`;
      case 'COMBO_CONDICIONAL':
        return `Comprá ${estrategia.cantidadMinimaTrigger}+ → ${estrategia.porcentajeBeneficio}% OFF`;
      case 'PRECIO_FIJO_CANTIDAD':
        return `${estrategia.cantidadActivacion} por $${estrategia.precioPaquete?.toLocaleString('es-AR')}`;
      default:
        return '';
    }
  }

  // ── Render ──

  const tipoEstrategia = promocion.estrategia.tipo;
  const colorEstrategia = ESTRATEGIA_COLOR[tipoEstrategia];

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-50 bg-black/60 animate-backdrop-in"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none">
        <div
          className="bg-neutral-900 border border-gray-800 rounded-xl w-full max-w-3xl max-h-[85vh] flex flex-col pointer-events-auto animate-modal-in"
          onClick={(e) => e.stopPropagation()}
        >
          {/* ── Header ── */}
          <div className="px-6 py-4 border-b border-gray-800 shrink-0">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <h2 className="text-lg font-semibold text-text-primary">
                  Alcance de la Promoción
                </h2>
                <span
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold"
                  style={{
                    backgroundColor: `${colorEstrategia}15`,
                    color: colorEstrategia,
                    boxShadow: `inset 0 0 0 1px ${colorEstrategia}30`,
                  }}
                >
                  {ESTRATEGIA_LABEL[tipoEstrategia]}
                </span>
              </div>
              <button
                onClick={onClose}
                className="p-2 rounded-lg hover:bg-gray-800 text-gray-400 transition-colors"
              >
                <X size={18} />
              </button>
            </div>

            <p className="text-sm text-text-secondary mt-1">
              {promocion.nombre}
              <span className="text-gray-600 mx-1.5">·</span>
              <span className="text-gray-500">{describirEstrategia()}</span>
            </p>
          </div>

          {/* ── Ayuda contextual + Selector de rol (solo combo) ── */}
          <div className="px-6 py-3 border-b border-gray-800/50 shrink-0 space-y-3">
            {/* Descripción del alcance según estrategia */}
            <div className="flex items-start gap-2">
              <Info size={14} className="text-gray-500 mt-0.5 shrink-0" />
              <p className="text-xs text-gray-400">{AYUDA_ALCANCE[tipoEstrategia]}</p>
            </div>

            {/* Selector de rol: solo visible para COMBO_CONDICIONAL */}
            {esCombo && (
              <div className="flex items-center gap-3">
                <span className="text-xs text-text-secondary">Asignar como:</span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setRolSeleccionado('TRIGGER')}
                    className={[
                      'flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium border transition-all',
                      rolSeleccionado === 'TRIGGER'
                        ? 'border-amber-500/50 bg-amber-500/10 text-amber-400'
                        : 'border-gray-700 text-gray-400 hover:border-gray-600',
                    ].join(' ')}
                  >
                    <Zap size={12} />
                    Activador
                  </button>
                  <button
                    type="button"
                    onClick={() => setRolSeleccionado('TARGET')}
                    className={[
                      'flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium border transition-all',
                      rolSeleccionado === 'TARGET'
                        ? 'border-blue-500/50 bg-blue-500/10 text-blue-400'
                        : 'border-gray-700 text-gray-400 hover:border-gray-600',
                    ].join(' ')}
                  >
                    <Gift size={12} />
                    Beneficiado
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* ── Transfer List ── */}
          <div className="flex-1 overflow-hidden flex gap-0 px-6 py-4 min-h-0">
            {/* Columna izquierda: Catálogo disponible */}
            <div className="flex-1 flex flex-col border border-gray-800 rounded-lg overflow-hidden">
              <div className="p-3 border-b border-gray-800 bg-background-card shrink-0">
                <p className="text-xs text-text-secondary font-medium mb-2">
                  Catálogo ({productosDisponibles.length})
                </p>
                <div className="relative">
                  <Search size={14} className="absolute left-2 top-1/2 -translate-y-1/2 text-gray-500" />
                  <input
                    type="text"
                    value={busqueda}
                    onChange={(e) => setBusqueda(e.target.value)}
                    placeholder="Buscar producto..."
                    className="w-full h-8 pl-7 pr-3 bg-neutral-900 border border-gray-700 rounded text-xs text-text-primary focus:border-primary focus:outline-none"
                  />
                </div>
              </div>
              <div className="flex-1 overflow-y-auto">
                {productosDisponibles.map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => toggleSeleccion(p.id, setSeleccionIzq)}
                    className={[
                      'w-full flex items-center gap-3 px-3 py-2.5 text-left text-sm border-b border-gray-800/30 transition-colors',
                      seleccionIzq.has(p.id)
                        ? 'bg-red-500/10 text-text-primary'
                        : 'text-gray-300 hover:bg-gray-800/50',
                    ].join(' ')}
                  >
                    <div
                      className="w-4 h-4 rounded-full shrink-0 border border-gray-600"
                      style={{ backgroundColor: p.colorHex }}
                    />
                    <span className="truncate">{p.nombre}</span>
                    <span className="text-xs text-gray-500 ml-auto font-mono">
                      ${p.precio.toLocaleString('es-AR')}
                    </span>
                  </button>
                ))}
                {productosDisponibles.length === 0 && (
                  <p className="text-xs text-gray-500 p-4 text-center">Sin resultados</p>
                )}
              </div>
            </div>

            {/* Botones de transferencia */}
            <div className="flex flex-col items-center justify-center gap-2 px-3 shrink-0">
              <button
                type="button"
                onClick={moverADerecha}
                disabled={seleccionIzq.size === 0}
                className="p-2 rounded-lg border border-gray-700 text-gray-400 hover:text-text-primary hover:border-gray-500 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                title="Agregar al alcance"
              >
                <ArrowRight size={16} />
              </button>
              <button
                type="button"
                onClick={moverAIzquierda}
                disabled={seleccionDer.size === 0}
                className="p-2 rounded-lg border border-gray-700 text-gray-400 hover:text-text-primary hover:border-gray-500 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                title="Quitar del alcance"
              >
                <ArrowLeft size={16} />
              </button>
            </div>

            {/* Columna derecha: Productos en el alcance */}
            <div className="flex-1 flex flex-col border border-gray-800 rounded-lg overflow-hidden">
              <div className="p-3 border-b border-gray-800 bg-background-card shrink-0">
                <p className="text-xs text-text-secondary font-medium">
                  En la promoción ({asociados.length})
                </p>
              </div>
              <div className="flex-1 overflow-y-auto">
                {esCombo ? (
                  /* ── Vista agrupada para Combo ──
                   * Separamos activadores de beneficiados para que el operador
                   * vea claramente la estructura del combo.
                   */
                  <>
                    {/* Grupo: Activadores */}
                    {activadores.length > 0 && (
                      <div>
                        <div className="px-3 py-1.5 bg-amber-500/5 border-b border-gray-800/30">
                          <span className="text-[10px] font-semibold text-amber-400 uppercase tracking-wider flex items-center gap-1">
                            <Zap size={10} />
                            Activan la promo ({activadores.length})
                          </span>
                        </div>
                        {activadores.map((item) => (
                          <button
                            key={item.producto.id}
                            type="button"
                            onClick={() => toggleSeleccion(item.producto.id, setSeleccionDer)}
                            className={[
                              'w-full flex items-center gap-2 px-3 py-2.5 text-left text-sm border-b border-gray-800/30 transition-colors',
                              seleccionDer.has(item.producto.id)
                                ? 'bg-red-500/10 text-text-primary'
                                : 'text-gray-300 hover:bg-gray-800/50',
                            ].join(' ')}
                          >
                            <div
                              className="w-3.5 h-3.5 rounded-full shrink-0 border border-gray-600"
                              style={{ backgroundColor: item.producto.colorHex }}
                            />
                            <span className="truncate flex-1">{item.producto.nombre}</span>
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-amber-500/15 text-amber-400 ring-1 ring-amber-500/20">
                              <Zap size={9} />
                              Activador
                            </span>
                          </button>
                        ))}
                      </div>
                    )}

                    {/* Grupo: Beneficiados */}
                    {beneficiados.length > 0 && (
                      <div>
                        <div className="px-3 py-1.5 bg-blue-500/5 border-b border-gray-800/30">
                          <span className="text-[10px] font-semibold text-blue-400 uppercase tracking-wider flex items-center gap-1">
                            <Gift size={10} />
                            Reciben el beneficio ({beneficiados.length})
                          </span>
                        </div>
                        {beneficiados.map((item) => (
                          <button
                            key={item.producto.id}
                            type="button"
                            onClick={() => toggleSeleccion(item.producto.id, setSeleccionDer)}
                            className={[
                              'w-full flex items-center gap-2 px-3 py-2.5 text-left text-sm border-b border-gray-800/30 transition-colors',
                              seleccionDer.has(item.producto.id)
                                ? 'bg-red-500/10 text-text-primary'
                                : 'text-gray-300 hover:bg-gray-800/50',
                            ].join(' ')}
                          >
                            <div
                              className="w-3.5 h-3.5 rounded-full shrink-0 border border-gray-600"
                              style={{ backgroundColor: item.producto.colorHex }}
                            />
                            <span className="truncate flex-1">{item.producto.nombre}</span>
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-blue-500/15 text-blue-400 ring-1 ring-blue-500/20">
                              <Gift size={9} />
                              Beneficiado
                            </span>
                          </button>
                        ))}
                      </div>
                    )}

                    {asociados.length === 0 && (
                      <p className="text-xs text-gray-500 p-4 text-center">
                        Agregá productos activadores y beneficiados para armar el combo.
                      </p>
                    )}
                  </>
                ) : (
                  /* ── Vista simple para no-combo ──
                   * Todos los productos son TARGET → no hace falta mostrar rol.
                   */
                  <>
                    {asociados.map((item) => (
                      <button
                        key={item.producto.id}
                        type="button"
                        onClick={() => toggleSeleccion(item.producto.id, setSeleccionDer)}
                        className={[
                          'w-full flex items-center gap-2 px-3 py-2.5 text-left text-sm border-b border-gray-800/30 transition-colors',
                          seleccionDer.has(item.producto.id)
                            ? 'bg-red-500/10 text-text-primary'
                            : 'text-gray-300 hover:bg-gray-800/50',
                        ].join(' ')}
                      >
                        <div
                          className="w-3.5 h-3.5 rounded-full shrink-0 border border-gray-600"
                          style={{ backgroundColor: item.producto.colorHex }}
                        />
                        <span className="truncate flex-1">{item.producto.nombre}</span>
                        <span className="text-xs text-gray-500 font-mono">
                          ${item.producto.precio.toLocaleString('es-AR')}
                        </span>
                      </button>
                    ))}
                    {asociados.length === 0 && (
                      <div className="flex flex-col items-center justify-center p-6 text-center">
                        <ShoppingBag size={24} className="text-gray-600 mb-2" />
                        <p className="text-xs text-gray-500">
                          Seleccioná productos del catálogo para incluir en esta promoción.
                        </p>
                      </div>
                    )}
                  </>
                )}
              </div>
            </div>
          </div>

          {/* ── Footer ── */}
          <div className="flex items-center justify-between px-6 py-4 border-t border-gray-800 shrink-0">
            {/* Resumen del alcance */}
            <div className="text-xs text-gray-500">
              {esCombo ? (
                <span>
                  {activadores.length} activador{activadores.length !== 1 && 'es'}
                  <span className="mx-1.5 text-gray-700">·</span>
                  {beneficiados.length} beneficiado{beneficiados.length !== 1 && 's'}
                </span>
              ) : (
                <span>
                  {asociados.length} producto{asociados.length !== 1 && 's'} en el alcance
                </span>
              )}
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={onClose}
                className="btn-secondary text-sm !min-h-[42px] px-5"
                disabled={asociarProductos.isPending}
              >
                Cancelar
              </button>
              <button
                onClick={handleGuardar}
                className="btn-primary text-sm !min-h-[42px] px-6 flex items-center gap-2"
                disabled={asociarProductos.isPending}
              >
                {asociarProductos.isPending ? (
                  <span className="animate-pulse">Guardando...</span>
                ) : (
                  <>
                    <Check size={16} />
                    <span>Guardar alcance</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
