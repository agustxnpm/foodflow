import { useMemo, useRef, useEffect } from 'react';
import { Plus, Search, X, PackageOpen, Sparkles } from 'lucide-react';
import type { ProductoResponse } from '../../catalogo/types';

interface GrillaProductosProps {
  productos: ProductoResponse[];
  cargando: boolean;
  onAgregarProducto: (productoId: string) => void;
  /** Texto actual de búsqueda typeahead */
  busqueda: string;
  /** Setter para actualizar la búsqueda */
  onBusquedaChange: (valor: string) => void;
  /** Total de productos antes de filtrar (para mostrar "N de M") */
  totalProductos: number;
}

// ─── Skeleton ─────────────────────────────────────────────────────────────────

function ProductoCardSkeleton() {
  return (
    <div
      className="
        rounded-xl border-2 border-neutral-800
        bg-neutral-900 p-3
        flex flex-col gap-2
        animate-pulse
      "
      aria-hidden="true"
    >
      <div className="h-3 w-3 rounded-full bg-neutral-800" />
      <div className="h-4 w-3/4 rounded bg-neutral-800" />
      <div className="h-5 w-1/2 rounded bg-neutral-800" />
    </div>
  );
}

// ─── Tarjeta de Producto ──────────────────────────────────────────────────────

function ProductoCard({
  producto,
  onAgregar,
}: {
  producto: ProductoResponse;
  onAgregar: () => void;
}) {
  const sinStock: boolean =
    !!(producto.controlaStock && producto.stockActual !== null && producto.stockActual <= 0);

  const tienePromos = producto.promocionesActivas && producto.promocionesActivas.length > 0;

  return (
    <button
      type="button"
      onClick={onAgregar}
      disabled={sinStock}
      className={[
        'group relative flex flex-col justify-between',
        'w-full rounded-xl p-3',
        'text-left transition-all duration-150',
        'active:scale-[0.97] focus:outline-none focus-visible:ring-2 focus-visible:ring-red-500',
        sinStock
          ? 'bg-neutral-900/50 border-2 border-neutral-800 opacity-50 cursor-not-allowed'
          : 'bg-neutral-900 border-2 border-neutral-800 hover:border-neutral-600 hover:bg-neutral-800/80 cursor-pointer',
      ].join(' ')}
      aria-label={`Agregar ${producto.nombre} — $${producto.precio}`}
    >
      {/* Badge de promoción activa */}
      {tienePromos && (
        <div className="absolute -top-1.5 -right-1.5 z-10 group/promo">
          <span className="flex items-center justify-center w-6 h-6 rounded-full bg-emerald-500 shadow-lg shadow-emerald-500/30 ring-2 ring-neutral-900">
            <Sparkles size={12} className="text-white" />
          </span>
          {/* Tooltip on hover */}
          <div className="
            invisible group-hover/promo:visible
            absolute right-0 top-full mt-1.5
            bg-neutral-800 border border-neutral-700 rounded-lg
            px-3 py-2 shadow-xl shadow-black/40
            min-w-[180px] max-w-[240px] z-50
            pointer-events-none
          ">
            <p className="text-[10px] font-bold text-emerald-400 uppercase tracking-wider mb-1">
              Promo activa
            </p>
            {producto.promocionesActivas.map((promo, i) => (
              <p key={i} className="text-xs text-gray-200 leading-snug">
                {promo.nombre}
              </p>
            ))}
            <div className="absolute -top-1 right-3 w-2 h-2 bg-neutral-800 border-l border-t border-neutral-700 rotate-45" />
          </div>
        </div>
      )}

      {/* Indicador de color + badge "+" */}
      <div className="flex items-start justify-between mb-2">
        <span
          className="w-3 h-3 rounded-full shrink-0 mt-0.5 border border-white/10"
          style={{ backgroundColor: producto.colorHex || '#FFFFFF' }}
        />
        <span
          className={[
            'w-7 h-7 rounded-lg flex items-center justify-center',
            'transition-colors duration-150',
            sinStock
              ? 'bg-neutral-800 text-neutral-600'
              : 'bg-neutral-800 text-gray-500 group-hover:bg-red-600 group-hover:text-white',
          ].join(' ')}
        >
          <Plus size={16} strokeWidth={2.5} />
        </span>
      </div>

      {/* Nombre */}
      <p className="text-sm font-semibold text-gray-200 leading-tight line-clamp-2 mb-1">
        {producto.nombre}
      </p>

      {/* Precio */}
      <p className="text-base font-bold text-gray-100 font-mono tabular-nums">
        $ {producto.precio.toLocaleString('es-AR')}
      </p>

      {/* Badge sin stock */}
      {sinStock && (
        <span className="absolute bottom-2 right-2 text-[9px] font-bold uppercase text-red-400 bg-red-950/60 px-1.5 py-0.5 rounded">
          Sin stock
        </span>
      )}
    </button>
  );
}

// ─── Componente Principal ─────────────────────────────────────────────────────

/**
 * Grilla central del POS con búsqueda typeahead y organización visual.
 *
 * Características:
 * - Input de búsqueda con foco automático y atajo de teclado
 * - Productos organizados alfabéticamente para fácil escaneo visual
 * - Indicador de resultados filtrados vs totales
 * - Mensajes guía sutiles para orientar al operador
 *
 * Grid: 5 columnas desktop, 4 tablet, 3 mobile — compacto para escaneo rápido.
 */
export default function GrillaProductos({
  productos,
  cargando,
  onAgregarProducto,
  busqueda,
  onBusquedaChange,
  totalProductos,
}: GrillaProductosProps) {
  const inputRef = useRef<HTMLInputElement>(null);

  // Foco automático al abrir el modal
  useEffect(() => {
    const timer = setTimeout(() => inputRef.current?.focus(), 150);
    return () => clearTimeout(timer);
  }, []);

  // Productos ordenados alfabéticamente para escaneo rápido
  const productosOrdenados = useMemo(
    () => [...productos].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es')),
    [productos]
  );

  const hayBusqueda = busqueda.trim().length > 0;

  return (
    <>
      {/* ── Barra de búsqueda ── */}
      <div className="sticky top-0 z-10 bg-neutral-900/95 backdrop-blur-sm border-b border-neutral-800 px-4 py-3 shrink-0">
        <div className="relative">
          <Search
            size={16}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-600 pointer-events-none"
          />
          <input
            ref={inputRef}
            type="text"
            value={busqueda}
            onChange={(e) => onBusquedaChange(e.target.value)}
            placeholder="Buscar producto..."
            className="
              w-full h-10 pl-10 pr-10
              bg-neutral-800 border border-neutral-700
              rounded-xl text-sm text-gray-200
              placeholder:text-gray-600
              focus:outline-none focus:border-red-600/50 focus:ring-1 focus:ring-red-600/30
              transition-colors
            "
          />
          {hayBusqueda && (
            <button
              type="button"
              onClick={() => {
                onBusquedaChange('');
                inputRef.current?.focus();
              }}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 transition-colors"
              aria-label="Limpiar búsqueda"
            >
              <X size={16} />
            </button>
          )}
        </div>

        {/* Indicador de resultados */}
        <div className="flex items-center justify-between mt-2">
          <p className="text-[11px] text-gray-600">
            {hayBusqueda ? (
              <>
                <span className="text-gray-400 font-semibold">{productos.length}</span>
                {' '}de {totalProductos} productos
              </>
            ) : (
              <>
                <span className="text-gray-400 font-semibold">{totalProductos}</span>
                {' '}productos disponibles
              </>
            )}
          </p>
          {!hayBusqueda && !cargando && (
            <p className="text-[11px] text-gray-700 flex items-center gap-1">
              <Sparkles size={10} />
              Tocá un producto para agregarlo
            </p>
          )}
        </div>
      </div>

      {/* ── Contenido: Loading / Vacío / Grilla ── */}
      <div className="flex-1 overflow-y-auto">
        {cargando ? (
          <div className="grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-5 gap-3 p-4">
            {Array.from({ length: 12 }).map((_, i) => (
              <ProductoCardSkeleton key={i} />
            ))}
          </div>
        ) : productosOrdenados.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full p-6 gap-3">
            <div className="w-14 h-14 rounded-full bg-neutral-800 flex items-center justify-center">
              <PackageOpen size={24} className="text-gray-600" />
            </div>
            <p className="text-gray-600 text-sm text-center leading-relaxed">
              {hayBusqueda ? (
                <>
                  Sin resultados para "<span className="text-gray-400">{busqueda}</span>"
                  <br />
                  <span className="text-gray-700 text-xs">
                    Probá con otro nombre o limpiá el filtro
                  </span>
                </>
              ) : (
                <>
                  No hay productos en esta categoría
                  <br />
                  <span className="text-gray-700 text-xs">
                    Seleccioná otra categoría del menú lateral
                  </span>
                </>
              )}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-5 gap-3 p-4">
            {productosOrdenados.map((producto) => (
              <ProductoCard
                key={producto.id}
                producto={producto}
                onAgregar={() => onAgregarProducto(producto.id)}
              />
            ))}
          </div>
        )}
      </div>
    </>
  );
}
