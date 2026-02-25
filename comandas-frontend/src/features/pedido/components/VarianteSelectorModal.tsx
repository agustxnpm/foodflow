import { useMemo } from 'react';
import { X, Layers } from 'lucide-react';
import type { ProductoResponse } from '../../catalogo/types';

// ─── Tipos ────────────────────────────────────────────────────────────────────

interface VarianteSelectorModalProps {
  /** Producto representante del grupo (el que se mostró en la grilla) */
  productoBase: ProductoResponse;
  /** Todas las variantes del grupo (incluye al propio base) */
  variantes: ProductoResponse[];
  /** Callback al seleccionar una variante */
  onSeleccionar: (variante: ProductoResponse) => void;
  /** Callback al cerrar sin seleccionar */
  onCerrar: () => void;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatPrecio(monto: number): string {
  return monto.toLocaleString('es-AR');
}

/**
 * Extrae una etiqueta corta para la variante.
 *
 * Estrategia: si el nombre de la variante contiene el nombre base como prefijo,
 * usa el sufijo como etiqueta (ej: "Hamburguesa Completa Doble" → "Doble").
 * Si no se puede derivar, usa el nombre completo.
 *
 * Fallback por cantidadDiscosCarne si la etiqueta queda vacía.
 */
function extraerEtiqueta(variante: ProductoResponse, nombreBase: string): string {
  const nombre = variante.nombre.trim();

  // Intentar encontrar un prefijo común para extraer el sufijo diferenciador
  // Ej: "Hamburguesa Completa" es base, "Hamburguesa Completa Doble" → "Doble"
  const palabrasBase = nombreBase.split(/\s+/);
  const palabrasVariante = nombre.split(/\s+/);

  // Buscar cuántas palabras coinciden desde el inicio
  let coincidencias = 0;
  for (let i = 0; i < Math.min(palabrasBase.length, palabrasVariante.length); i++) {
    if (palabrasBase[i].toLowerCase() === palabrasVariante[i].toLowerCase()) {
      coincidencias++;
    } else {
      break;
    }
  }

  // Si al menos la mitad de las palabras base coinciden, usar el sufijo
  if (coincidencias >= Math.ceil(palabrasBase.length / 2) && coincidencias < palabrasVariante.length) {
    return palabrasVariante.slice(coincidencias).join(' ');
  }

  // Fallback: usar nombre completo
  return nombre;
}

/**
 * Genera la abreviatura de 1 letra para el botón.
 * Ej: "Simple" → "S", "Doble" → "D", "Triple" → "T"
 */
function abreviatura(etiqueta: string): string {
  return etiqueta.charAt(0).toUpperCase();
}

// ─── Componente Principal ─────────────────────────────────────────────────────

/**
 * Modal de selección de variante para productos agrupados.
 *
 * Se muestra cuando el operador toca un producto que pertenece a un grupo
 * de variantes (ej: hamburguesas Simple / Doble / Triple).
 *
 * Muestra una botonera dinámica con todas las variantes del grupo,
 * ordenadas por cantidadDiscosCarne ascendente.
 *
 * Diseño: botones grandes, touch-friendly, con precio visible.
 * Paleta: rojo/negro acorde a la identidad FoodFlow.
 */
export default function VarianteSelectorModal({
  productoBase,
  variantes,
  onSeleccionar,
  onCerrar,
}: VarianteSelectorModalProps) {

  // Ordenar variantes por cantidadDiscosCarne (ascendente)
  const variantesOrdenadas = useMemo(() => {
    return [...variantes].sort(
      (a, b) => (a.cantidadDiscosCarne ?? 0) - (b.cantidadDiscosCarne ?? 0)
    );
  }, [variantes]);

  // Nombre base para extraer etiquetas diferenciadoras
  // Usamos el primer producto (menor discos) como referencia del grupo
  const nombreReferencia = useMemo(() => {
    if (variantesOrdenadas.length === 0) return productoBase.nombre;
    // Buscar el prefijo común más largo entre todas las variantes
    const nombres = variantesOrdenadas.map((v) => v.nombre);
    if (nombres.length === 1) return nombres[0];

    const palabrasPrimer = nombres[0].split(/\s+/);
    let prefijo = '';
    for (let i = 0; i < palabrasPrimer.length; i++) {
      const candidato = palabrasPrimer.slice(0, i + 1).join(' ');
      if (nombres.every((n) => n.startsWith(candidato))) {
        prefijo = candidato;
      } else {
        break;
      }
    }
    return prefijo || productoBase.nombre;
  }, [variantesOrdenadas, productoBase.nombre]);

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
        aria-label={`Seleccionar variante de ${productoBase.nombre}`}
      >
        <div
          className="
            w-full max-w-md
            bg-neutral-950 border border-neutral-800
            rounded-2xl shadow-2xl shadow-black/60
            flex flex-col
            max-h-[70vh]
            animate-modal-in
          "
        >
          {/* ── Cabecera ── */}
          <header className="flex items-center justify-between gap-4 px-5 pt-5 pb-4 border-b border-neutral-800 shrink-0">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-10 h-10 rounded-xl bg-red-950/40 flex items-center justify-center shrink-0">
                <Layers size={20} className="text-red-400" />
              </div>
              <div className="min-w-0">
                <h2 className="text-lg font-bold text-gray-100 truncate">
                  {nombreReferencia}
                </h2>
                <p className="text-xs text-gray-500">
                  Elegí la variante
                </p>
              </div>
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

          {/* ── Botonera de variantes ── */}
          <div className="flex-1 overflow-y-auto px-5 py-5">
            <div className="grid grid-cols-1 gap-3">
              {variantesOrdenadas.map((variante) => {
                const etiqueta = extraerEtiqueta(variante, nombreReferencia);
                const letra = abreviatura(etiqueta);
                const sinStock =
                  !!(variante.controlaStock && variante.stockActual !== null && variante.stockActual <= 0);

                return (
                  <button
                    key={variante.id}
                    type="button"
                    onClick={() => onSeleccionar(variante)}
                    disabled={sinStock}
                    className={[
                      'group flex items-center gap-4',
                      'w-full px-4 py-4 rounded-xl',
                      'text-left transition-all duration-150',
                      'active:scale-[0.97] focus:outline-none focus-visible:ring-2 focus-visible:ring-red-500',
                      sinStock
                        ? 'bg-neutral-900/50 border-2 border-neutral-800 opacity-50 cursor-not-allowed'
                        : 'bg-neutral-900 border-2 border-neutral-800 hover:border-red-600/60 hover:bg-neutral-800/80 cursor-pointer',
                    ].join(' ')}
                    aria-label={`${variante.nombre} — $${variante.precio}`}
                  >
                    {/* Letra abreviada */}
                    <span
                      className={[
                        'w-12 h-12 rounded-xl flex items-center justify-center',
                        'text-lg font-black shrink-0 transition-colors',
                        sinStock
                          ? 'bg-neutral-800 text-neutral-600'
                          : 'bg-red-950/40 text-red-400 group-hover:bg-red-600 group-hover:text-white',
                      ].join(' ')}
                    >
                      {letra}
                    </span>

                    {/* Nombre completo */}
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-gray-200 truncate">
                        {etiqueta}
                      </p>
                      {sinStock && (
                        <p className="text-[10px] font-bold uppercase text-red-400 mt-0.5">
                          Sin stock
                        </p>
                      )}
                    </div>

                    {/* Precio */}
                    <p className="text-base font-bold text-gray-100 font-mono tabular-nums shrink-0">
                      $ {formatPrecio(variante.precio)}
                    </p>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
