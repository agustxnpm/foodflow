import { useState, useMemo } from 'react';
import {
  Search,
  Plus,
  Pencil,
  PackagePlus,
  ToggleLeft,
  ToggleRight,
  ArrowUpDown,
  AlertTriangle,
  Package,
  Tag,
  SlidersHorizontal,
  LayoutGrid,
  List,
  ChevronDown,
  Palette,
  Sparkles,
} from 'lucide-react';
import { useProductos, useEditarProducto } from '../hooks/useProductos';
import type { ProductoResponse } from '../types';
import ProductoModal from './ProductoModal';
import AjusteStockModal from './AjusteStockModal';
import CategoriasModal from './CategoriasModal';
import { useCategoriasUI, useOrdenProductos } from '../../../lib/categorias-ui';

// ── Tipos de ordenamiento ──

type CriterioOrden = 'nombre' | 'precio' | 'stock' | 'categoria';
type DireccionOrden = 'asc' | 'desc';

const OPCIONES_ORDEN: { valor: CriterioOrden; label: string }[] = [
  { valor: 'nombre', label: 'Nombre' },
  { valor: 'precio', label: 'Precio' },
  { valor: 'stock', label: 'Stock' },
  { valor: 'categoria', label: 'Categoría' },
];

// ── Filtro de estado ──

type FiltroEstado = 'todos' | 'activos' | 'inactivos';

// ── Tipo de vista ──

type TipoVista = 'grid' | 'lista';

// ── Helpers ──

function ordenarProductos(
  productos: ProductoResponse[],
  criterio: CriterioOrden,
  direccion: DireccionOrden,
  nombreCategoria: (colorHex: string) => string
): ProductoResponse[] {
  const sorted = [...productos].sort((a, b) => {
    switch (criterio) {
      case 'nombre':
        return a.nombre.localeCompare(b.nombre, 'es');
      case 'precio':
        return a.precio - b.precio;
      case 'stock': {
        const sa = a.stockActual ?? -1;
        const sb = b.stockActual ?? -1;
        return sa - sb;
      }
      case 'categoria':
        return nombreCategoria(a.colorHex).localeCompare(
          nombreCategoria(b.colorHex),
          'es'
        );
      default:
        return 0;
    }
  });

  return direccion === 'desc' ? sorted.reverse() : sorted;
}

/**
 * Vista principal del Catálogo de productos y gestión de stock.
 *
 * Organización por categorías, búsqueda, ordenamiento y doble layout
 * (grid de cards / tabla compacta). Permite acciones rápidas inline:
 * - Toggle activo/inactivo
 * - Editar datos/precio
 * - Ajustar stock
 */
export default function VistaCatalogo() {
  const { data: productos = [], isLoading } = useProductos();
  const editarProducto = useEditarProducto();
  const { categoriasConTodos, nombreCategoria } = useCategoriasUI();

  // ── Estado de UI ──
  const [busqueda, setBusqueda] = useState('');
  const [categoriaActiva, setCategoriaActiva] = useState<string | null>(null);
  const [filtroEstado, setFiltroEstado] = useState<FiltroEstado>('todos');
  const [criterioOrden, setCriterioOrden] = useState<CriterioOrden>('categoria');
  const [direccionOrden, setDireccionOrden] = useState<DireccionOrden>('asc');
  const [tipoVista, setTipoVista] = useState<TipoVista>('grid');
  const [showOrden, setShowOrden] = useState(false);

  // ── Modales ──
  const [productoModal, setProductoModal] = useState<ProductoResponse | null | 'nuevo'>(null);
  const [stockModal, setStockModal] = useState<ProductoResponse | null>(null);
  const [showCategoriasModal, setShowCategoriasModal] = useState(false);

  // ── Métricas rápidas ──
  const metricas = useMemo(() => {
    const activos = productos.filter((p) => p.activo).length;
    const conStock = productos.filter((p) => p.controlaStock);
    const stockBajo = conStock.filter(
      (p) => (p.stockActual ?? 0) <= 5
    ).length;
    const sinStock = conStock.filter(
      (p) => (p.stockActual ?? 0) === 0
    ).length;
    return { total: productos.length, activos, stockBajo, sinStock };
  }, [productos]);

  // ── Conteo por categoría ──
  const conteoPorCategoria = useMemo(() => {
    const mapa: Record<string, number> = {};
    productos.forEach((p) => {
      const key = (p.colorHex || '_sin').toUpperCase();
      mapa[key] = (mapa[key] || 0) + 1;
    });
    return mapa;
  }, [productos]);

  // ── Pipeline de filtrado y ordenamiento ──
  const productosFiltrados = useMemo(() => {
    let resultado = productos;

    // 1. Filtro por categoría (color) — case-insensitive para robustez
    if (categoriaActiva !== null) {
      const catUpper = categoriaActiva.toUpperCase();
      resultado = resultado.filter((p) => p.colorHex?.toUpperCase() === catUpper);
    }

    // 2. Filtro por estado
    if (filtroEstado === 'activos') {
      resultado = resultado.filter((p) => p.activo);
    } else if (filtroEstado === 'inactivos') {
      resultado = resultado.filter((p) => !p.activo);
    }

    // 3. Búsqueda por nombre
    if (busqueda.trim()) {
      const q = busqueda.toLowerCase();
      resultado = resultado.filter((p) =>
        p.nombre.toLowerCase().includes(q)
      );
    }

    // 4. Ordenar
    resultado = ordenarProductos(resultado, criterioOrden, direccionOrden, nombreCategoria);

    return resultado;
  }, [productos, categoriaActiva, filtroEstado, busqueda, criterioOrden, direccionOrden, nombreCategoria]);

  // ── Productos agrupados por categoría (para vista grid) ──
  const { grupos: productosAgrupadosFull } = useOrdenProductos(productosFiltrados);

  const productosAgrupados = useMemo(() => {
    if (categoriaActiva !== null) {
      // Si hay una categoría seleccionada, un solo grupo
      return [{ label: nombreCategoria(categoriaActiva), hex: categoriaActiva, productos: productosFiltrados }];
    }
    // Usar agrupamiento del hook (respeta orden de categorías y de productos)
    return productosAgrupadosFull.map((g) => ({
      label: g.label,
      hex: g.hex,
      productos: g.productos,
    }));
  }, [productosFiltrados, categoriaActiva, productosAgrupadosFull, nombreCategoria]);

  // ── Acciones ──

  const handleToggleActivo = (producto: ProductoResponse) => {
    editarProducto.mutate({
      id: producto.id,
      nombre: producto.nombre,
      precio: producto.precio,
      activo: !producto.activo,
    });
  };

  const toggleOrden = (criterio: CriterioOrden) => {
    if (criterioOrden === criterio) {
      setDireccionOrden((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setCriterioOrden(criterio);
      setDireccionOrden('asc');
    }
    setShowOrden(false);
  };

  // ────────────────────────────────────────────────────────
  // RENDER: Card de producto (vista grid)
  // ────────────────────────────────────────────────────────

  const renderProductoCard = (producto: ProductoResponse) => {
    const stockVal = producto.stockActual ?? 0;
    const stockBajo = producto.controlaStock && stockVal <= 5;
    const stockMedio = producto.controlaStock && stockVal > 5 && stockVal <= 15;
    const tienePromos = producto.promocionesActivas && producto.promocionesActivas.length > 0;

    return (
      <div
        key={producto.id}
        className={[
          'group relative rounded-xl border bg-background-card',
          'transition-all duration-200 hover:border-gray-600 hover:shadow-lg hover:shadow-black/20',
          !producto.activo ? 'opacity-50' : '',
          stockBajo ? 'border-red-500/30' : 'border-gray-800',
        ].join(' ')}
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

        {/* Accent top bar (color de categoría) */}
        <div
          className="h-1 rounded-t-xl"
          style={{ backgroundColor: producto.colorHex }}
        />

        <div className="p-4 space-y-3">
          {/* Fila: Nombre + Toggle */}
          <div className="flex items-start justify-between gap-2">
            <h3 className="text-sm font-bold text-text-primary leading-tight line-clamp-2">
              {producto.nombre}
            </h3>
            <button
              onClick={() => handleToggleActivo(producto)}
              className="shrink-0 p-0.5"
              title={producto.activo ? 'Desactivar' : 'Activar'}
            >
              {producto.activo ? (
                <ToggleRight size={22} className="text-green-400" />
              ) : (
                <ToggleLeft size={22} className="text-gray-600" />
              )}
            </button>
          </div>

          {/* Precio */}
          <p className="text-lg font-bold text-white font-mono">
            ${producto.precio.toLocaleString('es-AR')}
          </p>

          {/* Stock badge */}
          <div className="flex items-center justify-between">
            {producto.controlaStock ? (
              <div
                className={[
                  'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold',
                  stockBajo
                    ? 'bg-red-500/15 text-red-400 ring-1 ring-red-500/20'
                    : stockMedio
                      ? 'bg-yellow-500/15 text-yellow-400 ring-1 ring-yellow-500/20'
                      : 'bg-green-500/15 text-green-400 ring-1 ring-green-500/20',
                ].join(' ')}
              >
                <Package size={12} />
                {stockVal} u.
                {stockBajo && stockVal > 0 && (
                  <AlertTriangle size={11} className="text-red-400" />
                )}
                {stockVal === 0 && <span className="text-red-400">· Agotado</span>}
              </div>
            ) : (
              <span className="text-[11px] text-gray-600">Sin control de stock</span>
            )}

            {/* Categoría mini-label */}
            <span className="text-[10px] text-gray-600 uppercase tracking-wider font-medium">
              {nombreCategoria(producto.colorHex)}
            </span>
          </div>

          {/* Acciones */}
          <div className="flex gap-2 pt-1 border-t border-gray-800/50">
            <button
              onClick={() => setProductoModal(producto)}
              className="flex-1 flex items-center justify-center gap-1.5 py-2 rounded-lg
                text-xs font-medium text-gray-400
                hover:bg-gray-800 hover:text-white transition-all"
            >
              <Pencil size={13} />
              Editar
            </button>
            {producto.controlaStock && (
              <button
                onClick={() => setStockModal(producto)}
                className="flex-1 flex items-center justify-center gap-1.5 py-2 rounded-lg
                  text-xs font-medium text-gray-400
                  hover:bg-gray-800 hover:text-white transition-all"
              >
                <PackagePlus size={13} />
                Stock
              </button>
            )}
          </div>
        </div>
      </div>
    );
  };

  // ────────────────────────────────────────────────────────
  // RENDER: Fila de producto (vista lista)
  // ────────────────────────────────────────────────────────

  const renderProductoFila = (producto: ProductoResponse) => {
    const stockVal = producto.stockActual ?? 0;
    const stockBajo = producto.controlaStock && stockVal <= 5;
    const stockMedio = producto.controlaStock && stockVal > 5 && stockVal <= 15;
    const tienePromos = producto.promocionesActivas && producto.promocionesActivas.length > 0;

    return (
      <div
        key={producto.id}
        className={[
          'flex items-center gap-4 px-4 py-3 rounded-xl border bg-background-card',
          'transition-all duration-150 hover:border-gray-600',
          !producto.activo ? 'opacity-50' : '',
          stockBajo ? 'border-red-500/30' : 'border-gray-800/50',
        ].join(' ')}
      >
        {/* Color dot */}
        <div
          className="w-4 h-4 rounded-full shrink-0 border border-gray-600"
          style={{ backgroundColor: producto.colorHex }}
        />

        {/* Nombre + badge promo inline */}
        <span className="flex-1 flex items-center gap-2 text-sm font-semibold text-text-primary truncate min-w-0">
          <span className="truncate">{producto.nombre}</span>
          {tienePromos && (
            <span className="group/promo relative shrink-0">
              <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md bg-emerald-500/15 text-emerald-400 text-[10px] font-bold uppercase tracking-wider">
                <Sparkles size={10} />
                Promo
              </span>
              {/* Tooltip */}
              <span className="
                invisible group-hover/promo:visible
                absolute left-0 top-full mt-1.5
                bg-neutral-800 border border-neutral-700 rounded-lg
                px-3 py-2 shadow-xl shadow-black/40
                min-w-[180px] max-w-[240px] z-50
                pointer-events-none
              ">
                <span className="block text-[10px] font-bold text-emerald-400 uppercase tracking-wider mb-1">
                  Promo activa
                </span>
                {producto.promocionesActivas.map((promo, i) => (
                  <span key={i} className="block text-xs text-gray-200 leading-snug">
                    {promo.nombre}
                  </span>
                ))}
              </span>
            </span>
          )}
        </span>

        {/* Categoría */}
        <span className="hidden md:block text-xs text-gray-500 w-24 truncate">
          {nombreCategoria(producto.colorHex)}
        </span>

        {/* Precio */}
        <span className="text-sm font-bold text-white font-mono w-24 text-right shrink-0">
          ${producto.precio.toLocaleString('es-AR')}
        </span>

        {/* Stock */}
        <div className="w-20 shrink-0 text-center">
          {producto.controlaStock ? (
            <span
              className={[
                'inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-semibold',
                stockBajo
                  ? 'bg-red-500/15 text-red-400'
                  : stockMedio
                    ? 'bg-yellow-500/15 text-yellow-400'
                    : 'bg-green-500/15 text-green-400',
              ].join(' ')}
            >
              {stockVal} u.
            </span>
          ) : (
            <span className="text-[11px] text-gray-600">—</span>
          )}
        </div>

        {/* Toggle */}
        <button
          onClick={() => handleToggleActivo(producto)}
          className="shrink-0"
          title={producto.activo ? 'Desactivar' : 'Activar'}
        >
          {producto.activo ? (
            <ToggleRight size={20} className="text-green-400" />
          ) : (
            <ToggleLeft size={20} className="text-gray-600" />
          )}
        </button>

        {/* Acciones */}
        <div className="flex gap-1 shrink-0">
          <button
            onClick={() => setProductoModal(producto)}
            className="p-2 rounded-lg hover:bg-gray-700 text-gray-500 hover:text-white transition-colors"
            title="Editar producto"
          >
            <Pencil size={15} />
          </button>
          {producto.controlaStock && (
            <button
              onClick={() => setStockModal(producto)}
              className="p-2 rounded-lg hover:bg-gray-700 text-gray-500 hover:text-white transition-colors"
              title="Ajustar stock"
            >
              <PackagePlus size={15} />
            </button>
          )}
        </div>
      </div>
    );
  };

  // ────────────────────────────────────────────────────────
  // RENDER PRINCIPAL
  // ────────────────────────────────────────────────────────

  return (
    <div className="space-y-5">
      {/* ── Alertas de stock ── */}
      {metricas.stockBajo > 0 && (
        <div className="flex items-center gap-3 p-3 bg-red-500/5 border border-red-500/20 rounded-xl">
          <AlertTriangle size={18} className="text-red-400 shrink-0" />
          <p className="text-sm text-red-400/90">
            <strong>{metricas.stockBajo} producto{metricas.stockBajo !== 1 ? 's' : ''}</strong> con stock bajo
            {metricas.sinStock > 0 && (
              <span className="text-red-400">
                {' '}· {metricas.sinStock} agotado{metricas.sinStock !== 1 ? 's' : ''}
              </span>
            )}
          </p>
        </div>
      )}

      {/* ── Cabecera: Búsqueda + Nuevo Producto ── */}
      <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center">
        <div className="relative flex-1">
          <Search
            size={18}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500"
          />
          <input
            type="text"
            placeholder="Buscar producto..."
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            className="w-full min-h-[48px] pl-10 pr-4 bg-background-card border border-gray-700
              rounded-lg text-text-primary placeholder:text-gray-500
              focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30
              transition-all"
          />
        </div>
        <button
          onClick={() => setShowCategoriasModal(true)}
          className="btn-secondary flex items-center justify-center gap-2 px-4 whitespace-nowrap"
          title="Gestionar categorías"
        >
          <Palette size={18} />
          <span className="hidden sm:inline">Categorías</span>
        </button>
        <button
          onClick={() => setProductoModal('nuevo')}
          className="btn-primary flex items-center justify-center gap-2 px-6 whitespace-nowrap"
        >
          <Plus size={20} />
          <span>Nuevo Producto</span>
        </button>
      </div>

      {/* ── Filtros por categoría (pills horizontales) ── */}
      <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
        {categoriasConTodos.map((cat) => {
          const colorFiltro = cat.colorBase || null;
          const isActiva = categoriaActiva === colorFiltro;
          const count =
            colorFiltro === null
              ? productos.length
              : conteoPorCategoria[colorFiltro.toUpperCase()] ?? 0;

          return (
            <button
              key={cat.id}
              type="button"
              onClick={() =>
                setCategoriaActiva(isActiva ? null : colorFiltro)
              }
              className={[
                'inline-flex items-center gap-2 px-4 py-2 rounded-full text-xs font-semibold',
                'whitespace-nowrap border transition-all duration-150 shrink-0',
                isActiva
                  ? 'bg-white/10 text-white border-gray-500'
                  : 'bg-transparent text-gray-500 border-gray-800 hover:border-gray-600 hover:text-gray-300',
              ].join(' ')}
            >
              {cat.colorBase ? (
                <span
                  className="w-2.5 h-2.5 rounded-full shrink-0"
                  style={{ backgroundColor: cat.colorDisplay }}
                />
              ) : (
                <Tag size={12} />
              )}
              {cat.nombre}
              <span
                className={`text-[10px] ${
                  isActiva ? 'text-gray-300' : 'text-gray-600'
                }`}
              >
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {/* ── Barra de controles: Estado / Orden / Vista ── */}
      <div className="flex items-center justify-between gap-3 flex-wrap">
        {/* Filtro de estado */}
        <div className="flex items-center gap-1 bg-neutral-900 rounded-lg p-0.5 border border-gray-800">
          {(
            [
              { valor: 'todos', label: 'Todos' },
              { valor: 'activos', label: 'Activos' },
              { valor: 'inactivos', label: 'Inactivos' },
            ] as { valor: FiltroEstado; label: string }[]
          ).map(({ valor, label }) => (
            <button
              key={valor}
              onClick={() => setFiltroEstado(valor)}
              className={[
                'px-3 py-1.5 rounded-md text-xs font-medium transition-all',
                filtroEstado === valor
                  ? 'bg-gray-700 text-white'
                  : 'text-gray-500 hover:text-gray-300',
              ].join(' ')}
            >
              {label}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2">
          {/* Selector de orden */}
          <div className="relative">
            <button
              onClick={() => setShowOrden(!showOrden)}
              className="inline-flex items-center gap-2 px-3 py-2 rounded-lg
                text-xs font-medium text-gray-400 border border-gray-800
                hover:border-gray-600 hover:text-gray-300 transition-all"
            >
              <SlidersHorizontal size={14} />
              <span className="hidden sm:inline">
                {OPCIONES_ORDEN.find((o) => o.valor === criterioOrden)?.label}
              </span>
              <ArrowUpDown size={12} className={direccionOrden === 'desc' ? 'rotate-180' : ''} />
            </button>

            {showOrden && (
              <>
                <div
                  className="fixed inset-0 z-40"
                  onClick={() => setShowOrden(false)}
                />
                <div className="absolute right-0 top-full mt-1 z-50 bg-neutral-900 border border-gray-700
                  rounded-lg shadow-xl shadow-black/30 py-1 min-w-[150px]">
                  {OPCIONES_ORDEN.map(({ valor, label }) => (
                    <button
                      key={valor}
                      onClick={() => toggleOrden(valor)}
                      className={[
                        'w-full flex items-center justify-between px-3 py-2 text-xs transition-colors',
                        criterioOrden === valor
                          ? 'text-white bg-gray-800'
                          : 'text-gray-400 hover:bg-gray-800/50 hover:text-gray-200',
                      ].join(' ')}
                    >
                      <span>{label}</span>
                      {criterioOrden === valor && (
                        <ChevronDown
                          size={12}
                          className={`transition-transform ${
                            direccionOrden === 'asc' ? 'rotate-180' : ''
                          }`}
                        />
                      )}
                    </button>
                  ))}
                </div>
              </>
            )}
          </div>

          {/* Toggle grid / lista */}
          <div className="flex bg-neutral-900 rounded-lg p-0.5 border border-gray-800">
            <button
              onClick={() => setTipoVista('grid')}
              className={[
                'p-2 rounded-md transition-all',
                tipoVista === 'grid'
                  ? 'bg-gray-700 text-white'
                  : 'text-gray-500 hover:text-gray-300',
              ].join(' ')}
              title="Vista en grilla"
            >
              <LayoutGrid size={15} />
            </button>
            <button
              onClick={() => setTipoVista('lista')}
              className={[
                'p-2 rounded-md transition-all',
                tipoVista === 'lista'
                  ? 'bg-gray-700 text-white'
                  : 'text-gray-500 hover:text-gray-300',
              ].join(' ')}
              title="Vista en lista"
            >
              <List size={15} />
            </button>
          </div>

          {/* Contador de resultados */}
          <span className="text-xs text-gray-600 hidden sm:block">
            {productosFiltrados.length} de {productos.length}
          </span>
        </div>
      </div>

      {/* ── Contenido principal ── */}
      {isLoading ? (
        <div className="flex items-center justify-center h-48 text-gray-500">
          <div className="animate-pulse flex flex-col items-center gap-2">
            <Package size={28} className="text-gray-600" />
            <span className="text-sm">Cargando catálogo...</span>
          </div>
        </div>
      ) : productosFiltrados.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-48 text-gray-500 gap-3
          border border-dashed border-gray-800 rounded-xl">
          <PackagePlus size={36} className="text-gray-600" />
          <p className="text-sm text-center">
            {busqueda
              ? `No se encontraron productos para "${busqueda}"`
              : categoriaActiva
                ? 'No hay productos en esta categoría'
                : 'No hay productos en el catálogo'}
          </p>
          {!busqueda && !categoriaActiva && (
            <button
              onClick={() => setProductoModal('nuevo')}
              className="text-primary text-sm font-medium hover:underline"
            >
              ¡Creá el primero!
            </button>
          )}
        </div>
      ) : tipoVista === 'grid' ? (
        /* ── Vista Grid: Agrupada por categoría ── */
        <div className="space-y-6">
          {productosAgrupados.map((grupo) => (
            <section key={grupo.label}>
              {/* Header de categoría */}
              {productosAgrupados.length > 1 && (
                <div className="flex items-center gap-2.5 mb-3">
                  <span
                    className="w-3 h-3 rounded-full shrink-0"
                    style={{ backgroundColor: grupo.hex }}
                  />
                  <h3 className="text-sm font-bold text-text-primary">
                    {grupo.label}
                  </h3>
                  <span className="text-xs text-gray-600">
                    {grupo.productos.length}
                  </span>
                  <div className="flex-1 h-px bg-gray-800/50" />
                </div>
              )}

              {/* Grid de cards */}
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3">
                {grupo.productos.map(renderProductoCard)}
              </div>
            </section>
          ))}
        </div>
      ) : (
        /* ── Vista Lista: Compacta ── */
        <div className="space-y-2">
          {productosFiltrados.map(renderProductoFila)}
        </div>
      )}

      {/* ── Modales ── */}
      {productoModal !== null && (
        <ProductoModal
          producto={productoModal === 'nuevo' ? null : productoModal}
          onClose={() => setProductoModal(null)}
        />
      )}

      {stockModal && (
        <AjusteStockModal
          producto={stockModal}
          onClose={() => setStockModal(null)}
        />
      )}

      {showCategoriasModal && (
        <CategoriasModal onClose={() => setShowCategoriasModal(false)} />
      )}
    </div>
  );
}
