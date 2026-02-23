import { useState, useMemo } from 'react';
import {
  Plus,
  Pencil,
  ToggleLeft,
  ToggleRight,
  Percent,
  Tag,
  Clock,
  DollarSign,
  Sparkles,
  Search,
  SlidersHorizontal,
  ArrowUpDown,
  ChevronDown,
  LayoutGrid,
  List,
  Zap,
  ShieldCheck,
  ShieldOff,
  Package,
  Link2,
} from 'lucide-react';
import { usePromociones, useToggleEstadoPromocion } from '../hooks/usePromociones';
import type { PromocionResponse, TipoEstrategia, TriggerResponse } from '../types';
import PromocionWizardModal from './PromocionWizardModal';

// ── Helpers de visualización ──

const ESTRATEGIA_BADGE: Record<TipoEstrategia, string> = {
  DESCUENTO_DIRECTO: 'bg-purple-500/20 text-purple-400 ring-1 ring-purple-500/30',
  CANTIDAD_FIJA: 'bg-blue-500/20 text-blue-400 ring-1 ring-blue-500/30',
  COMBO_CONDICIONAL: 'bg-amber-500/20 text-amber-400 ring-1 ring-amber-500/30',
  PRECIO_FIJO_CANTIDAD: 'bg-emerald-500/20 text-emerald-400 ring-1 ring-emerald-500/30',
};

const ESTRATEGIA_COLOR: Record<TipoEstrategia, string> = {
  DESCUENTO_DIRECTO: '#A855F7',
  CANTIDAD_FIJA: '#3B82F6',
  COMBO_CONDICIONAL: '#F59E0B',
  PRECIO_FIJO_CANTIDAD: '#10B981',
};


const DIAS_CORTOS: Record<string, string> = {
  MONDAY: 'Lun',
  TUESDAY: 'Mar',
  WEDNESDAY: 'Mié',
  THURSDAY: 'Jue',
  FRIDAY: 'Vie',
  SATURDAY: 'Sáb',
  SUNDAY: 'Dom',
};

// ── Tipos de filtrado y ordenamiento ──

type FiltroEstado = 'todos' | 'activas' | 'inactivas';
type FiltroEstrategia = TipoEstrategia | null;
type CriterioOrden = 'nombre' | 'prioridad' | 'estrategia' | 'estado';
type DireccionOrden = 'asc' | 'desc';
type TipoVista = 'grid' | 'lista';

const OPCIONES_ORDEN: { valor: CriterioOrden; label: string }[] = [
  { valor: 'nombre', label: 'Nombre' },
  { valor: 'prioridad', label: 'Prioridad' },
  { valor: 'estrategia', label: 'Estrategia' },
  { valor: 'estado', label: 'Estado' },
];

// ── Pills de filtrado por estrategia ──

interface PillEstrategia {
  tipo: TipoEstrategia | null;
  label: string;
  icon: typeof Tag;
}

const PILLS_ESTRATEGIA: PillEstrategia[] = [
  { tipo: null, label: 'Todas', icon: Tag },
  { tipo: 'DESCUENTO_DIRECTO', label: 'Descuento', icon: Percent },
  { tipo: 'CANTIDAD_FIJA', label: 'NxM', icon: Zap },
  { tipo: 'COMBO_CONDICIONAL', label: 'Combo', icon: Package },
  { tipo: 'PRECIO_FIJO_CANTIDAD', label: 'Pack', icon: DollarSign },
];

// ── Helpers ──

function describirEstrategia(promo: PromocionResponse): string {
  const { estrategia } = promo;
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
      return 'Promoción';
  }
}

function describirTriggers(triggers: TriggerResponse[]): string[] {
  return triggers.map((t) => {
    switch (t.tipo) {
      case 'TEMPORAL': {
        const dias = t.diasSemana?.map((d) => DIAS_CORTOS[d] ?? d).join(', ');
        const horario =
          t.horaDesde && t.horaHasta ? `${t.horaDesde}–${t.horaHasta}` : null;
        const parts = [dias, horario].filter(Boolean);
        return parts.length > 0 ? parts.join(' · ') : 'Rango de fechas';
      }
      case 'MONTO_MINIMO':
        return `Monto ≥ $${t.montoMinimo?.toLocaleString('es-AR')}`;
      case 'CONTENIDO':
        return `${t.productosRequeridos?.length ?? 0} productos requeridos`;
      default:
        return 'Criterio';
    }
  });
}

function ordenarPromociones(
  promos: PromocionResponse[],
  criterio: CriterioOrden,
  direccion: DireccionOrden,
): PromocionResponse[] {
  const sorted = [...promos].sort((a, b) => {
    switch (criterio) {
      case 'nombre':
        return a.nombre.localeCompare(b.nombre, 'es');
      case 'prioridad':
        return a.prioridad - b.prioridad;
      case 'estrategia':
        return a.estrategia.tipo.localeCompare(b.estrategia.tipo);
      case 'estado':
        return a.estado.localeCompare(b.estado);
      default:
        return 0;
    }
  });
  return direccion === 'desc' ? sorted.reverse() : sorted;
}

/**
 * Vista completa del Motor de Promociones.
 *
 * Incluye:
 * - Métricas rápidas (total, activas, inactivas, con productos asociados)
 * - Búsqueda por nombre/descripción
 * - Filtros por tipo de estrategia (pills) y estado (segmented)
 * - Ordenamiento por nombre, prioridad, estrategia, estado
 * - Doble layout: cards (grid) o filas compactas (lista)
 * - Acciones: crear, editar, toggle activo, asociar productos
 */
export default function VistaPromociones() {
  const { data: response, isLoading } = usePromociones();
  const toggleEstado = useToggleEstadoPromocion();

  const [wizardModal, setWizardModal] = useState<PromocionResponse | null | 'nueva'>(null);

  // ── Estado de UI ──
  const [busqueda, setBusqueda] = useState('');
  const [filtroEstado, setFiltroEstado] = useState<FiltroEstado>('todos');
  const [filtroEstrategia, setFiltroEstrategia] = useState<FiltroEstrategia>(null);
  const [criterioOrden, setCriterioOrden] = useState<CriterioOrden>('prioridad');
  const [direccionOrden, setDireccionOrden] = useState<DireccionOrden>('asc');
  const [tipoVista, setTipoVista] = useState<TipoVista>('grid');
  const [showOrden, setShowOrden] = useState(false);

  const promociones: PromocionResponse[] = response?.data ?? [];

  // ── Métricas rápidas ──
  const metricas = useMemo(() => {
    const activas = promociones.filter((p) => p.estado === 'ACTIVA').length;
    const inactivas = promociones.length - activas;
    const conProductos = promociones.filter((p) => p.alcance.items.length > 0).length;
    const sinProductos = promociones.filter((p) => p.alcance.items.length === 0).length;
    return { total: promociones.length, activas, inactivas, conProductos, sinProductos };
  }, [promociones]);

  // ── Conteo por estrategia ──
  const conteoPorEstrategia = useMemo(() => {
    const mapa: Partial<Record<TipoEstrategia, number>> = {};
    promociones.forEach((p) => {
      mapa[p.estrategia.tipo] = (mapa[p.estrategia.tipo] || 0) + 1;
    });
    return mapa;
  }, [promociones]);

  // ── Pipeline de filtrado y ordenamiento ──
  const promocionesFiltradas = useMemo(() => {
    let resultado = promociones;

    // 1. Filtro por estrategia
    if (filtroEstrategia !== null) {
      resultado = resultado.filter((p) => p.estrategia.tipo === filtroEstrategia);
    }

    // 2. Filtro por estado
    if (filtroEstado === 'activas') {
      resultado = resultado.filter((p) => p.estado === 'ACTIVA');
    } else if (filtroEstado === 'inactivas') {
      resultado = resultado.filter((p) => p.estado === 'INACTIVA');
    }

    // 3. Búsqueda por nombre o descripción
    if (busqueda.trim()) {
      const q = busqueda.toLowerCase();
      resultado = resultado.filter(
        (p) =>
          p.nombre.toLowerCase().includes(q) ||
          p.descripcion?.toLowerCase().includes(q),
      );
    }

    // 4. Ordenar
    resultado = ordenarPromociones(resultado, criterioOrden, direccionOrden);

    return resultado;
  }, [promociones, filtroEstrategia, filtroEstado, busqueda, criterioOrden, direccionOrden]);

  // ── Acciones ──

  const handleToggleEstado = (promo: PromocionResponse) => {
    const nuevoEstado = promo.estado === 'ACTIVA' ? 'INACTIVA' : 'ACTIVA';
    toggleEstado.mutate({ id: promo.id, estado: nuevoEstado });
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
  // RENDER: Card de promoción (vista grid)
  // ────────────────────────────────────────────────────────

  const renderPromoCard = (promo: PromocionResponse) => {
    const isActiva = promo.estado === 'ACTIVA';

    return (
      <div
        key={promo.id}
        className={[
          'group relative rounded-xl border bg-background-card',
          'transition-all duration-200 hover:border-gray-600 hover:shadow-lg hover:shadow-black/20',
          !isActiva ? 'opacity-55' : '',
          'border-gray-800',
        ].join(' ')}
      >
        {/* Accent top bar (color de estrategia) */}
        <div
          className="h-1 rounded-t-xl"
          style={{ backgroundColor: ESTRATEGIA_COLOR[promo.estrategia.tipo] }}
        />

        <div className="p-5 space-y-3">
          {/* Fila: Nombre + Toggle */}
          <div className="flex items-start justify-between gap-2">
            <h3 className="text-sm font-bold text-text-primary leading-tight line-clamp-2 flex-1">
              {promo.nombre}
            </h3>
            <button
              onClick={() => handleToggleEstado(promo)}
              className="shrink-0 p-0.5"
              title={isActiva ? 'Desactivar' : 'Activar'}
            >
              {isActiva ? (
                <ToggleRight size={22} className="text-green-400" />
              ) : (
                <ToggleLeft size={22} className="text-gray-600" />
              )}
            </button>
          </div>

          {/* Descripción */}
          {promo.descripcion && (
            <p className="text-xs text-text-secondary leading-relaxed line-clamp-2">
              {promo.descripcion}
            </p>
          )}

          {/* Badge de estrategia (beneficio) */}
          <span
            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold ${ESTRATEGIA_BADGE[promo.estrategia.tipo]}`}
          >
            {promo.estrategia.tipo === 'DESCUENTO_DIRECTO' ? (
              <Percent size={12} />
            ) : promo.estrategia.tipo === 'PRECIO_FIJO_CANTIDAD' ? (
              <DollarSign size={12} />
            ) : (
              <Tag size={12} />
            )}
            {describirEstrategia(promo)}
          </span>

          {/* Triggers */}
          {promo.triggers.length > 0 && (
            <div className="flex flex-wrap gap-1.5">
              {describirTriggers(promo.triggers).map((label, i) => (
                <span
                  key={i}
                  className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-medium bg-gray-800/80 text-gray-400 ring-1 ring-gray-700/50"
                >
                  <Clock size={10} className="text-gray-500" />
                  {label}
                </span>
              ))}
            </div>
          )}

          {/* Footer: prioridad + alcance + acciones */}
          <div className="flex items-center justify-between pt-2 border-t border-gray-800/50">
            <div className="flex items-center gap-3">
              <span className="text-[10px] text-gray-600 uppercase tracking-wider font-medium">
                Prio {promo.prioridad}
              </span>
              {promo.alcance.items.length > 0 && (
                <span className="inline-flex items-center gap-1 text-[11px] text-red-400/80">
                  <Link2 size={10} />
                  {promo.alcance.items.length}
                </span>
              )}
            </div>

            <div className="flex gap-1.5">
              <button
                onClick={() => setWizardModal(promo)}
                className="p-2 rounded-lg hover:bg-gray-800 text-gray-500 hover:text-white transition-colors"
                title="Editar promoción"
              >
                <Pencil size={13} />
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  };

  // ────────────────────────────────────────────────────────
  // RENDER: Fila de promoción (vista lista)
  // ────────────────────────────────────────────────────────

  const renderPromoFila = (promo: PromocionResponse) => {
    const isActiva = promo.estado === 'ACTIVA';

    return (
      <div
        key={promo.id}
        className={[
          'flex items-center gap-4 px-4 py-3 rounded-xl border bg-background-card',
          'transition-all duration-150 hover:border-gray-600',
          !isActiva ? 'opacity-55' : '',
          'border-gray-800',
        ].join(' ')}
      >
        {/* Color dot */}
        <div
          className="w-3 h-3 rounded-full shrink-0"
          style={{ backgroundColor: ESTRATEGIA_COLOR[promo.estrategia.tipo] }}
        />

        {/* Nombre */}
        <span className="flex-1 text-sm font-semibold text-text-primary truncate min-w-0">
          {promo.nombre}
        </span>

        {/* Badge estrategia */}
        <span
          className={`hidden sm:inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold shrink-0 ${ESTRATEGIA_BADGE[promo.estrategia.tipo]}`}
        >
          {describirEstrategia(promo)}
        </span>

        {/* Prioridad */}
        <span className="text-xs text-gray-600 w-14 text-center shrink-0">
          Prio {promo.prioridad}
        </span>

        {/* Alcance */}
        <div className="w-16 shrink-0 text-center">
          {promo.alcance.items.length > 0 ? (
            <span className="inline-flex items-center gap-1 text-xs text-red-400">
              <Link2 size={11} />
              {promo.alcance.items.length}
            </span>
          ) : (
            <span className="text-[11px] text-gray-600">—</span>
          )}
        </div>

        {/* Toggle */}
        <button
          onClick={() => handleToggleEstado(promo)}
          className="shrink-0"
          title={isActiva ? 'Desactivar' : 'Activar'}
        >
          {isActiva ? (
            <ToggleRight size={20} className="text-green-400" />
          ) : (
            <ToggleLeft size={20} className="text-gray-600" />
          )}
        </button>

        {/* Acciones */}
        <div className="flex gap-1 shrink-0">
          <button
            onClick={() => setWizardModal(promo)}
            className="p-2 rounded-lg hover:bg-gray-700 text-gray-500 hover:text-white transition-colors"
            title="Editar"
          >
            <Pencil size={15} />
          </button>
        </div>
      </div>
    );
  };

  // ────────────────────────────────────────────────────────
  // RENDER PRINCIPAL
  // ────────────────────────────────────────────────────────

  return (
    <div className="space-y-5">
      {/* ── Métricas rápidas ── */}
      {metricas.total > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { label: 'Total', value: metricas.total, icon: Sparkles, color: 'text-white' },
            { label: 'Activas', value: metricas.activas, icon: ShieldCheck, color: 'text-green-400' },
            { label: 'Inactivas', value: metricas.inactivas, icon: ShieldOff, color: 'text-gray-500' },
            { label: 'Sin productos', value: metricas.sinProductos, icon: Link2, color: metricas.sinProductos > 0 ? 'text-amber-400' : 'text-gray-500' },
          ].map(({ label, value, icon: Icon, color }) => (
            <div
              key={label}
              className="flex items-center gap-3 px-4 py-3 rounded-xl border border-gray-800 bg-background-card"
            >
              <Icon size={18} className={color} />
              <div>
                <p className={`text-lg font-bold ${color}`}>{value}</p>
                <p className="text-[11px] text-gray-600 uppercase tracking-wider">{label}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ── Cabecera: Búsqueda + Nueva Promoción ── */}
      <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center">
        <div className="relative flex-1">
          <Search
            size={18}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500"
          />
          <input
            type="text"
            placeholder="Buscar promoción..."
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            className="w-full min-h-[48px] pl-10 pr-4 bg-background-card border border-gray-700
              rounded-lg text-text-primary placeholder:text-gray-500
              focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30
              transition-all"
          />
        </div>
        <button
          onClick={() => setWizardModal('nueva')}
          className="btn-primary flex items-center justify-center gap-2 px-6 whitespace-nowrap"
        >
          <Plus size={20} />
          <span>Nueva Promoción</span>
        </button>
      </div>

      {/* ── Pills de filtrado por estrategia ── */}
      <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
        {PILLS_ESTRATEGIA.map(({ tipo, label, icon: Icon }) => {
          const isActiva = filtroEstrategia === tipo;
          const count =
            tipo === null
              ? promociones.length
              : conteoPorEstrategia[tipo] ?? 0;
          const dotColor = tipo ? ESTRATEGIA_COLOR[tipo] : undefined;

          return (
            <button
              key={tipo ?? '__todos__'}
              type="button"
              onClick={() => setFiltroEstrategia(isActiva ? null : tipo)}
              className={[
                'inline-flex items-center gap-2 px-4 py-2 rounded-full text-xs font-semibold',
                'whitespace-nowrap border transition-all duration-150 shrink-0',
                isActiva
                  ? 'bg-white/10 text-white border-gray-500'
                  : 'bg-transparent text-gray-500 border-gray-800 hover:border-gray-600 hover:text-gray-300',
              ].join(' ')}
            >
              {dotColor ? (
                <span
                  className="w-2.5 h-2.5 rounded-full shrink-0"
                  style={{ backgroundColor: dotColor }}
                />
              ) : (
                <Icon size={12} />
              )}
              {label}
              <span
                className={`text-[10px] ${isActiva ? 'text-gray-300' : 'text-gray-600'}`}
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
              { valor: 'activas', label: 'Activas' },
              { valor: 'inactivas', label: 'Inactivas' },
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
                          className={`transition-transform ${direccionOrden === 'asc' ? 'rotate-180' : ''}`}
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

          {/* Contador */}
          <span className="text-xs text-gray-600 hidden sm:block">
            {promocionesFiltradas.length} de {promociones.length}
          </span>
        </div>
      </div>

      {/* ── Contenido principal ── */}
      {isLoading ? (
        <div className="flex items-center justify-center h-48 text-gray-500">
          <div className="animate-pulse flex flex-col items-center gap-2">
            <Sparkles size={28} className="text-gray-600" />
            <span className="text-sm">Cargando promociones...</span>
          </div>
        </div>
      ) : promocionesFiltradas.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-48 text-gray-500 gap-3
          border border-dashed border-gray-800 rounded-xl">
          <Sparkles size={36} className="text-gray-600" />
          <p className="text-sm text-center">
            {busqueda
              ? `No se encontraron promociones para "${busqueda}"`
              : filtroEstado !== 'todos' || filtroEstrategia
                ? 'No hay promociones con estos filtros'
                : 'No hay promociones configuradas'}
          </p>
          {!busqueda && filtroEstado === 'todos' && !filtroEstrategia && (
            <button
              onClick={() => setWizardModal('nueva')}
              className="text-primary text-sm font-medium hover:underline"
            >
              ¡Creá la primera!
            </button>
          )}
        </div>
      ) : tipoVista === 'grid' ? (
        /* ── Vista Grid ── */
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
          {promocionesFiltradas.map(renderPromoCard)}
        </div>
      ) : (
        /* ── Vista Lista ── */
        <div className="space-y-2">
          {promocionesFiltradas.map(renderPromoFila)}
        </div>
      )}

      {/* ── Modales ── */}
      {wizardModal !== null && (
        <PromocionWizardModal
          promocion={wizardModal === 'nueva' ? null : wizardModal}
          onClose={() => setWizardModal(null)}
        />
      )}
    </div>
  );
}
