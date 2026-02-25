import { useState, useMemo } from 'react';
import { X, Plus, Layers, Loader2, Check } from 'lucide-react';
import type { ProductoResponse } from '../types';
import { useVariantes, useCrearVariante } from '../hooks/useProductos';

// ─── Tipos ────────────────────────────────────────────────────────────────────

interface VariantesProductoModalProps {
  /** Producto base sobre el cual se gestionan variantes */
  producto: ProductoResponse;
  onClose: () => void;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatPrecio(monto: number): string {
  return monto.toLocaleString('es-AR');
}

// ─── Componente Principal ─────────────────────────────────────────────────────

/**
 * Modal para gestionar variantes de un producto desde el Catálogo (admin).
 *
 * Permite:
 * 1. Ver las variantes existentes del grupo (si las hay)
 * 2. Crear nuevas variantes (ej: Doble, Triple)
 *
 * Decisión: El formulario de creación es inline (no otro modal),
 * porque crear una variante es una operación rápida de 3 campos.
 *
 * El backend crea el grupo automáticamente si el producto base
 * no pertenece a uno todavía.
 */
export default function VariantesProductoModal({
  producto,
  onClose,
}: VariantesProductoModalProps) {
  const { data: variantes = [], isLoading } = useVariantes(producto.id);
  const crearVariante = useCrearVariante();

  // ── Estado del formulario de nueva variante ──
  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [nombre, setNombre] = useState('');
  const [precio, setPrecio] = useState('');
  const [nivel, setNivel] = useState('');
  const [error, setError] = useState<string | null>(null);

  // Variantes ordenadas por nivel (cantidadDiscosCarne)
  const variantesOrdenadas = useMemo(
    () => [...variantes].sort((a, b) => (a.cantidadDiscosCarne ?? 0) - (b.cantidadDiscosCarne ?? 0)),
    [variantes]
  );

  const tieneVariantes = variantesOrdenadas.length > 0;

  // Sugerir el próximo nivel automáticamente
  const proximoNivel = useMemo(() => {
    if (variantesOrdenadas.length === 0) return 2; // El base será 1
    const maxNivel = Math.max(...variantesOrdenadas.map((v) => v.cantidadDiscosCarne ?? 0));
    return maxNivel + 1;
  }, [variantesOrdenadas]);

  const handleCrearVariante = () => {
    setError(null);

    if (!nombre.trim()) {
      setError('El nombre de la variante es obligatorio');
      return;
    }

    const precioNum = parseFloat(precio);
    if (isNaN(precioNum) || precioNum <= 0) {
      setError('El precio debe ser un número mayor a cero');
      return;
    }

    const nivelNum = parseInt(nivel || String(proximoNivel), 10);
    if (isNaN(nivelNum) || nivelNum < 1) {
      setError('El nivel debe ser un número mayor o igual a 1');
      return;
    }

    crearVariante.mutate(
      {
        productoBaseId: producto.id,
        nombre: nombre.trim(),
        precio: precioNum,
        cantidadDiscosCarne: nivelNum,
        categoriaId: producto.categoriaId ?? undefined,
        colorHex: producto.colorHex,
      },
      {
        onSuccess: () => {
          // Limpiar formulario y mantener modal abierto para agregar más
          setNombre('');
          setPrecio('');
          setNivel('');
          setMostrarFormulario(false);
        },
        onError: (err: any) => {
          const msg = err?.response?.data?.message || 'Error al crear la variante';
          setError(msg);
        },
      }
    );
  };

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
          className="bg-neutral-900 border border-gray-800 rounded-xl w-full max-w-lg pointer-events-auto animate-modal-in flex flex-col max-h-[80vh]"
          onClick={(e) => e.stopPropagation()}
        >
          {/* ── Header ── */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-800 shrink-0">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-9 h-9 rounded-lg bg-red-950/40 flex items-center justify-center shrink-0">
                <Layers size={18} className="text-red-400" />
              </div>
              <div className="min-w-0">
                <h2 className="text-lg font-semibold text-gray-100 truncate">
                  Variantes
                </h2>
                <p className="text-xs text-gray-500 truncate">
                  {producto.nombre}
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-2 rounded-lg hover:bg-gray-800 text-gray-400 transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          {/* ── Body ── */}
          <div className="flex-1 overflow-y-auto px-6 py-5 space-y-5">

            {/* Info contextual */}
            {!tieneVariantes && !mostrarFormulario && (
              <div className="text-center py-6 space-y-3">
                <div className="w-14 h-14 rounded-full bg-neutral-800 flex items-center justify-center mx-auto">
                  <Layers size={24} className="text-gray-600" />
                </div>
                <div>
                  <p className="text-sm text-gray-400">
                    Este producto no tiene variantes todavía.
                  </p>
                  <p className="text-xs text-gray-600 mt-1">
                    Creá variantes como Doble, Triple, etc. para agruparlas en el POS.
                  </p>
                </div>
              </div>
            )}

            {/* Lista de variantes existentes */}
            {isLoading ? (
              <div className="flex items-center justify-center py-8 gap-2">
                <Loader2 size={18} className="text-gray-600 animate-spin" />
                <span className="text-sm text-gray-600">Cargando variantes...</span>
              </div>
            ) : tieneVariantes ? (
              <div className="space-y-2">
                <p className="text-xs text-gray-500 uppercase tracking-wider font-semibold mb-2">
                  Variantes del grupo ({variantesOrdenadas.length})
                </p>
                {variantesOrdenadas.map((v) => (
                  <div
                    key={v.id}
                    className="flex items-center gap-3 px-4 py-3 rounded-xl bg-neutral-800/50 border border-neutral-700/50"
                  >
                    {/* Nivel badge */}
                    <span className="w-9 h-9 rounded-lg bg-red-950/30 flex items-center justify-center text-sm font-bold text-red-400 shrink-0">
                      {v.cantidadDiscosCarne ?? '?'}
                    </span>

                    {/* Nombre */}
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-gray-200 truncate">
                        {v.nombre}
                      </p>
                      {v.id === producto.id && (
                        <p className="text-[10px] text-gray-500 uppercase tracking-wider">
                          Producto base
                        </p>
                      )}
                    </div>

                    {/* Precio */}
                    <p className="text-sm font-bold text-gray-100 font-mono tabular-nums shrink-0">
                      $ {formatPrecio(v.precio)}
                    </p>

                    {/* Estado */}
                    <span
                      className={[
                        'text-[10px] font-bold uppercase px-1.5 py-0.5 rounded',
                        v.activo
                          ? 'text-green-400 bg-green-950/30'
                          : 'text-gray-500 bg-neutral-800',
                      ].join(' ')}
                    >
                      {v.activo ? 'Activo' : 'Inactivo'}
                    </span>
                  </div>
                ))}
              </div>
            ) : null}

            {/* ── Formulario de nueva variante ── */}
            {mostrarFormulario && (
              <div className="space-y-4 p-4 rounded-xl border border-neutral-700 bg-neutral-800/30">
                <p className="text-xs text-gray-400 uppercase tracking-wider font-semibold">
                  Nueva variante de "{producto.nombre}"
                </p>

                {/* Nombre */}
                <div className="flex flex-col gap-1">
                  <label className="text-xs text-gray-500">Nombre</label>
                  <input
                    type="text"
                    value={nombre}
                    onChange={(e) => setNombre(e.target.value)}
                    placeholder={`Ej: ${producto.nombre} Doble`}
                    className="min-h-[44px] px-4 bg-neutral-900 border border-neutral-700 rounded-lg text-sm text-gray-200 placeholder:text-gray-600 focus:border-red-600/50 focus:outline-none focus:ring-1 focus:ring-red-600/30"
                    autoFocus
                  />
                </div>

                {/* Precio + Nivel en fila */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="flex flex-col gap-1">
                    <label className="text-xs text-gray-500">Precio</label>
                    <div className="relative">
                      <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-600 font-mono text-sm">$</span>
                      <input
                        type="number"
                        value={precio}
                        onChange={(e) => setPrecio(e.target.value)}
                        placeholder="0"
                        min="0"
                        step="any"
                        className="w-full min-h-[44px] pl-7 pr-3 bg-neutral-900 border border-neutral-700 rounded-lg text-sm text-gray-200 font-mono placeholder:text-gray-600 focus:border-red-600/50 focus:outline-none focus:ring-1 focus:ring-red-600/30"
                      />
                    </div>
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-xs text-gray-500">Nivel (orden)</label>
                    <input
                      type="number"
                      value={nivel}
                      onChange={(e) => setNivel(e.target.value)}
                      placeholder={String(proximoNivel)}
                      min="1"
                      className="min-h-[44px] px-4 bg-neutral-900 border border-neutral-700 rounded-lg text-sm text-gray-200 font-mono placeholder:text-gray-600 focus:border-red-600/50 focus:outline-none focus:ring-1 focus:ring-red-600/30"
                    />
                    <p className="text-[10px] text-gray-600">
                      1=Simple, 2=Doble, 3=Triple...
                    </p>
                  </div>
                </div>

                {/* Error */}
                {error && (
                  <p className="text-sm text-red-400">{error}</p>
                )}

                {/* Acciones del formulario */}
                <div className="flex items-center gap-2 pt-1">
                  <button
                    type="button"
                    onClick={() => {
                      setMostrarFormulario(false);
                      setNombre('');
                      setPrecio('');
                      setNivel('');
                      setError(null);
                    }}
                    className="flex-1 h-11 rounded-lg text-sm font-medium text-gray-400 border border-neutral-700 hover:bg-neutral-800 transition-colors"
                    disabled={crearVariante.isPending}
                  >
                    Cancelar
                  </button>
                  <button
                    type="button"
                    onClick={handleCrearVariante}
                    disabled={crearVariante.isPending}
                    className="flex-1 h-11 rounded-lg text-sm font-bold text-white bg-red-600 hover:bg-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
                  >
                    {crearVariante.isPending ? (
                      <>
                        <Loader2 size={16} className="animate-spin" />
                        Creando…
                      </>
                    ) : (
                      <>
                        <Check size={16} />
                        Crear Variante
                      </>
                    )}
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* ── Footer ── */}
          <div className="flex items-center justify-between gap-3 px-6 py-4 border-t border-gray-800 shrink-0">
            <button
              onClick={onClose}
              className="px-5 h-11 rounded-lg text-sm font-medium text-gray-400 border border-neutral-700 hover:bg-neutral-800 transition-colors"
            >
              Cerrar
            </button>
            {!mostrarFormulario && (
              <button
                type="button"
                onClick={() => {
                  setMostrarFormulario(true);
                  setError(null);
                }}
                className="flex items-center gap-2 px-5 h-11 rounded-lg text-sm font-bold text-white bg-red-600 hover:bg-red-500 transition-colors"
              >
                <Plus size={16} />
                Agregar Variante
              </button>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
