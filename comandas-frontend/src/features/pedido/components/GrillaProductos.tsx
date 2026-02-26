import { useMemo, useRef, useEffect } from 'react';
import { Plus, Search, X, PackageOpen, Sparkles, Layers } from 'lucide-react';
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

// ─── Tipos internos ───────────────────────────────────────────────────────────

/**
 * Elemento renderizable en la grilla: puede ser un producto individual
 * o un representante de un grupo de variantes.
 */
interface ElementoGrilla {
  /** Producto que se muestra como tarjeta (representante del grupo o producto suelto) */
  producto: ProductoResponse;
  /** Cantidad de variantes en el grupo (1 = producto individual, >1 = grupo) */
  cantidadVariantes: number;
  /** Precio mínimo del grupo (para mostrar "desde $X" si hay variantes) */
  precioMinimo: number;
  /** true si ALGUNA variante del grupo tiene promociones activas */
  tienePromoEnGrupo: boolean;
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
  cantidadVariantes,
  precioMinimo,
  tienePromoEnGrupo,
}: {
  producto: ProductoResponse;
  onAgregar: () => void;
  cantidadVariantes: number;
  precioMinimo: number;
  /** true si alguna variante del grupo (o el propio producto) tiene promos */
  tienePromoEnGrupo: boolean;
}) {
  const sinStock: boolean =
    !!(producto.controlaStock && producto.stockActual !== null && producto.stockActual <= 0);

  const tienePromos = tienePromoEnGrupo;
  const esGrupo = cantidadVariantes > 1;

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
            <p className="text-[11px] font-bold text-emerald-400 uppercase tracking-wider mb-1">
              Promo activa
            </p>
            {producto.promocionesActivas.map((promo, i) => (
              <div key={i} className="mb-1.5 last:mb-0">
                <p className="text-sm text-gray-200 font-medium leading-snug">
                  {promo.nombre}
                </p>
                {promo.descripcion && (
                  <p className="text-xs text-gray-400 leading-snug mt-0.5">
                    {promo.descripcion}
                  </p>
                )}
              </div>
            ))}
            <div className="absolute -top-1 right-3 w-2 h-2 bg-neutral-800 border-l border-t border-neutral-700 rotate-45" />
          </div>
        </div>
      )}

      {/* Badge variantes + badge "+" */}
      <div className="flex items-start justify-between mb-2">
        {/* Badge de variantes (reemplaza el dot de color) */}
        {esGrupo ? (
          <span className="flex items-center gap-1 px-1.5 py-0.5 rounded-md bg-neutral-800 border border-neutral-700">
            <Layers size={10} className="text-gray-500" />
            <span className="text-[10px] font-semibold text-gray-500">{cantidadVariantes}</span>
          </span>
        ) : (
          <span
            className="w-3 h-3 rounded-full shrink-0 mt-0.5 border border-white/10"
            style={{ backgroundColor: producto.colorHex || '#FFFFFF' }}
          />
        )}
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

      {/* Precio — "desde $X" si es grupo con precios distintos */}
      <p className="text-base font-bold text-gray-100 font-mono tabular-nums">
        {esGrupo && precioMinimo < producto.precio ? (
          <>
            <span className="text-[10px] font-semibold text-gray-500 mr-1">desde</span>
            $ {precioMinimo.toLocaleString('es-AR')}
          </>
        ) : (
          <>$ {producto.precio.toLocaleString('es-AR')}</>
        )}
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

  /**
   * Agrupar productos por grupoVarianteId.
   *
   * Los productos que comparten grupoVarianteId se colapsan en una sola tarjeta.
   * Se muestra el representante del grupo (menor cantidadDiscosCarne = producto base).
   * 
   * Si hay búsqueda activa, un grupo se muestra si CUALQUIER variante del grupo
   * coincide con el término de búsqueda.
   */
  const elementosGrilla = useMemo((): ElementoGrilla[] => {
    const sinGrupo: ProductoResponse[] = [];
    const grupos = new Map<string, ProductoResponse[]>();

    for (const p of productos) {
      if (p.grupoVarianteId) {
        const arr = grupos.get(p.grupoVarianteId) || [];
        arr.push(p);
        grupos.set(p.grupoVarianteId, arr);
      } else {
        sinGrupo.push(p);
      }
    }

    const elementos: ElementoGrilla[] = [];

    // Productos individuales (sin grupo)
    for (const p of sinGrupo) {
      elementos.push({
        producto: p,
        cantidadVariantes: 1,
        precioMinimo: p.precio,
        tienePromoEnGrupo: p.promocionesActivas?.length > 0,
      });
    }

    // Grupos de variantes → 1 tarjeta por grupo
    for (const variantes of grupos.values()) {
      // Ordenar por cantidadDiscosCarne ascendente: el primero es el representante
      const sorted = [...variantes].sort(
        (a, b) => (a.cantidadDiscosCarne ?? 0) - (b.cantidadDiscosCarne ?? 0)
      );
      const representante = sorted[0];
      const precioMinimo = Math.min(...sorted.map((v) => v.precio));
      // Si CUALQUIER variante tiene promo, el badge se muestra en la card grupal
      const tienePromoEnGrupo = sorted.some((v) => v.promocionesActivas?.length > 0);

      elementos.push({
        producto: representante,
        cantidadVariantes: sorted.length,
        precioMinimo,
        tienePromoEnGrupo,
      });
    }

    // Ordenar todo alfabéticamente por nombre
    return elementos.sort((a, b) =>
      a.producto.nombre.localeCompare(b.producto.nombre, 'es')
    );
  }, [productos]);

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
        ) : elementosGrilla.length === 0 ? (
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
            {elementosGrilla.map((elem) => (
              <ProductoCard
                key={elem.producto.id}
                producto={elem.producto}
                onAgregar={() => onAgregarProducto(elem.producto.id)}
                cantidadVariantes={elem.cantidadVariantes}
                precioMinimo={elem.precioMinimo}
                tienePromoEnGrupo={elem.tienePromoEnGrupo}
              />
            ))}
          </div>
        )}
      </div>
    </>
  );
}
