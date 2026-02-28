import { useState, useMemo, useCallback } from 'react';
import {
  Calendar,
  ChevronDown,
  ChevronUp,
  TrendingUp,
  TrendingDown,
  BarChart3,
  Clock,
  ArrowLeft,
  Receipt,
  Wallet,
  Banknote,
  CreditCard,
  Search,
} from 'lucide-react';
import { Link } from 'react-router-dom';

import { useHistorialJornadas } from '../hooks/useCaja';
import type { JornadaResumen } from '../types';

// ─── Utilidades ───────────────────────────────────────────────────────────────

/** Formatea una fecha ISO a display legible: "Lun 27 Ene" */
function formatFechaCorta(fecha: string): string {
  const d = new Date(fecha + 'T12:00:00');
  return d.toLocaleDateString('es-AR', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  });
}

/** Formatea una fecha ISO a display completo: "Lunes 27 de Enero de 2026" */
function formatFechaLarga(fecha: string): string {
  const d = new Date(fecha + 'T12:00:00');
  return d.toLocaleDateString('es-AR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}

/** Formatea datetime ISO a hora legible: "23:45" */
function formatHora(datetime: string): string {
  return new Date(datetime).toLocaleTimeString('es-AR', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** Formatea un monto a moneda ARS */
function formatMonto(monto: number): string {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(monto);
}

/** Obtiene la fecha ISO de hace N días */
function haceDias(n: number): string {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().split('T')[0];
}

/** Fecha de hoy ISO */
function hoy(): string {
  return new Date().toISOString().split('T')[0];
}

// ─── Rangos predefinidos ──────────────────────────────────────────────────────

interface RangoPredefinido {
  label: string;
  desde: string;
  hasta: string;
}

function getRangosPredefinidos(): RangoPredefinido[] {
  return [
    { label: 'Última semana', desde: haceDias(7), hasta: hoy() },
    { label: 'Últimos 15 días', desde: haceDias(15), hasta: hoy() },
    { label: 'Último mes', desde: haceDias(30), hasta: hoy() },
    { label: 'Últimos 3 meses', desde: haceDias(90), hasta: hoy() },
  ];
}

// ─── Componente: Gráfico de barras interactivo ────────────────────────────────

interface GraficoJornadasProps {
  jornadas: JornadaResumen[];
  metrica: 'totalVentasReales' | 'balanceEfectivo' | 'pedidosCerradosCount';
}

function GraficoJornadas({ jornadas, metrica }: GraficoJornadasProps) {
  const [hoveredIdx, setHoveredIdx] = useState<number | null>(null);

  // Ordenar cronológicamente para el gráfico (ASC)
  const datos = useMemo(
    () => [...jornadas].sort((a, b) => a.fechaOperativa.localeCompare(b.fechaOperativa)),
    [jornadas],
  );

  const maxValor = useMemo(() => {
    const valores = datos.map((j) => Math.abs(Number(j[metrica])));
    return Math.max(...valores, 1);
  }, [datos, metrica]);

  if (datos.length === 0) {
    return (
      <div className="flex items-center justify-center h-44 text-gray-600 text-sm">
        Sin datos para graficar
      </div>
    );
  }

  const etiquetaMetrica: Record<string, string> = {
    totalVentasReales: 'Ventas',
    balanceEfectivo: 'Balance efectivo',
    pedidosCerradosCount: 'Pedidos',
  };

  return (
    <div className="space-y-3">
      {/* Tooltip flotante */}
      {hoveredIdx !== null && (
        <div className="text-center text-xs text-gray-400 transition-all duration-150">
          <span className="font-medium text-gray-300">
            {formatFechaCorta(datos[hoveredIdx].fechaOperativa)}
          </span>
          <span className="mx-2 text-gray-600">·</span>
          <span className="font-mono text-white">
            {metrica === 'pedidosCerradosCount'
              ? `${datos[hoveredIdx][metrica]} pedidos`
              : formatMonto(Number(datos[hoveredIdx][metrica]))}
          </span>
        </div>
      )}

      {/* Barras */}
      <div className="flex items-end gap-[3px] h-36">
        {datos.map((jornada, idx) => {
          const valor = Math.abs(Number(jornada[metrica]));
          const pct = maxValor > 0 ? (valor / maxValor) * 100 : 0;
          const isNegativo = Number(jornada[metrica]) < 0;
          const isHovered = hoveredIdx === idx;

          return (
            <div
              key={jornada.id}
              className="flex-1 flex flex-col items-center gap-1 min-w-0 cursor-pointer"
              onMouseEnter={() => setHoveredIdx(idx)}
              onMouseLeave={() => setHoveredIdx(null)}
            >
              {/* Barra */}
              <div className="w-full flex items-end h-28">
                <div
                  className={`
                    w-full rounded-t transition-all duration-200 min-h-[3px]
                    ${isNegativo
                      ? isHovered ? 'bg-red-400' : 'bg-red-500/60'
                      : isHovered ? 'bg-emerald-400' : 'bg-emerald-500/50'
                    }
                    ${isHovered ? 'shadow-lg shadow-emerald-500/20' : ''}
                  `}
                  style={{ height: `${Math.max(pct, 3)}%` }}
                />
              </div>

              {/* Label de fecha — solo si hay espacio */}
              {datos.length <= 31 && (
                <span className={`
                  text-[9px] font-mono truncate max-w-full
                  ${isHovered ? 'text-gray-300' : 'text-gray-600'}
                `}>
                  {new Date(jornada.fechaOperativa + 'T12:00:00').getDate()}
                </span>
              )}
            </div>
          );
        })}
      </div>

      {/* Leyenda */}
      <div className="flex items-center justify-between text-[10px] text-gray-600">
        <span>{formatFechaCorta(datos[0].fechaOperativa)}</span>
        <span className="text-gray-500 uppercase tracking-wider">
          {etiquetaMetrica[metrica]}
        </span>
        <span>{formatFechaCorta(datos[datos.length - 1].fechaOperativa)}</span>
      </div>
    </div>
  );
}

// ─── Componente: Tarjeta de jornada expandible ────────────────────────────────

interface JornadaCardProps {
  jornada: JornadaResumen;
  isExpanded: boolean;
  onToggle: () => void;
}

function JornadaCard({ jornada, isExpanded, onToggle }: JornadaCardProps) {
  const tendencia = jornada.totalVentasReales - jornada.totalEgresos;

  return (
    <div className={`
      rounded-xl border transition-all duration-200
      ${isExpanded
        ? 'border-neutral-700 bg-neutral-800/60'
        : 'border-neutral-800/50 bg-neutral-800/30 hover:border-neutral-700/70 hover:bg-neutral-800/40'
      }
    `}>
      {/* Header clickeable */}
      <button
        onClick={onToggle}
        className="w-full flex items-center justify-between gap-3 px-4 py-3 text-left"
      >
        <div className="flex items-center gap-3 min-w-0">
          {/* Indicador de tendencia */}
          <div className={`
            w-8 h-8 rounded-lg flex items-center justify-center shrink-0
            ${tendencia >= 0 ? 'bg-emerald-500/15' : 'bg-red-500/15'}
          `}>
            {tendencia >= 0
              ? <TrendingUp size={15} className="text-emerald-400" />
              : <TrendingDown size={15} className="text-red-400" />
            }
          </div>

          <div className="min-w-0">
            <p className="text-sm font-medium text-gray-200 truncate">
              {formatFechaCorta(jornada.fechaOperativa)}
            </p>
            <p className="text-[11px] text-gray-500 flex items-center gap-1">
              <Clock size={10} />
              Cierre {formatHora(jornada.fechaCierre)}
              <span className="mx-1 text-gray-700">·</span>
              {jornada.pedidosCerradosCount} {jornada.pedidosCerradosCount === 1 ? 'pedido' : 'pedidos'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3 shrink-0">
          <span className="text-sm font-mono font-semibold text-gray-200 tabular-nums">
            {formatMonto(jornada.totalVentasReales)}
          </span>
          {isExpanded
            ? <ChevronUp size={16} className="text-gray-500" />
            : <ChevronDown size={16} className="text-gray-500" />
          }
        </div>
      </button>

      {/* Panel expandido */}
      {isExpanded && (
        <div className="px-4 pb-4 pt-1 border-t border-neutral-700/50">
          <div className="grid grid-cols-2 gap-3 mt-3">
            {/* Ventas reales */}
            <div className="flex items-center gap-2.5 p-2.5 rounded-lg bg-neutral-900/60">
              <div className="w-7 h-7 rounded-md bg-emerald-500/15 flex items-center justify-center">
                <Receipt size={13} className="text-emerald-400" />
              </div>
              <div>
                <p className="text-[10px] text-gray-500 uppercase tracking-wide">Ventas</p>
                <p className="text-sm font-mono font-semibold text-emerald-400 tabular-nums">
                  {formatMonto(jornada.totalVentasReales)}
                </p>
              </div>
            </div>

            {/* Egresos */}
            <div className="flex items-center gap-2.5 p-2.5 rounded-lg bg-neutral-900/60">
              <div className="w-7 h-7 rounded-md bg-red-500/15 flex items-center justify-center">
                <Banknote size={13} className="text-red-400" />
              </div>
              <div>
                <p className="text-[10px] text-gray-500 uppercase tracking-wide">Egresos</p>
                <p className="text-sm font-mono font-semibold text-red-400 tabular-nums">
                  {formatMonto(jornada.totalEgresos)}
                </p>
              </div>
            </div>

            {/* Balance efectivo */}
            <div className="flex items-center gap-2.5 p-2.5 rounded-lg bg-neutral-900/60">
              <div className="w-7 h-7 rounded-md bg-blue-500/15 flex items-center justify-center">
                <Wallet size={13} className="text-blue-400" />
              </div>
              <div>
                <p className="text-[10px] text-gray-500 uppercase tracking-wide">Balance $</p>
                <p className={`text-sm font-mono font-semibold tabular-nums ${
                  jornada.balanceEfectivo >= 0 ? 'text-blue-400' : 'text-red-400'
                }`}>
                  {formatMonto(jornada.balanceEfectivo)}
                </p>
              </div>
            </div>

            {/* Consumo interno */}
            <div className="flex items-center gap-2.5 p-2.5 rounded-lg bg-neutral-900/60">
              <div className="w-7 h-7 rounded-md bg-amber-500/15 flex items-center justify-center">
                <CreditCard size={13} className="text-amber-400" />
              </div>
              <div>
                <p className="text-[10px] text-gray-500 uppercase tracking-wide">A Cuenta</p>
                <p className="text-sm font-mono font-semibold text-amber-400 tabular-nums">
                  {formatMonto(jornada.totalConsumoInterno)}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Componente: Tarjetas de resumen del rango ────────────────────────────────

interface ResumenRangoProps {
  jornadas: JornadaResumen[];
}

function ResumenRango({ jornadas }: ResumenRangoProps) {
  const totales = useMemo(() => {
    const ventasTotal = jornadas.reduce((s, j) => s + j.totalVentasReales, 0);
    const egresosTotal = jornadas.reduce((s, j) => s + j.totalEgresos, 0);
    const pedidosTotal = jornadas.reduce((s, j) => s + j.pedidosCerradosCount, 0);
    const promedioVentas = jornadas.length > 0 ? ventasTotal / jornadas.length : 0;

    return { ventasTotal, egresosTotal, pedidosTotal, promedioVentas };
  }, [jornadas]);

  const cards = [
    {
      label: 'Ventas totales',
      valor: formatMonto(totales.ventasTotal),
      color: 'text-emerald-400',
      bgIcon: 'bg-emerald-500/15',
      icon: <TrendingUp size={14} className="text-emerald-400" />,
    },
    {
      label: 'Egresos totales',
      valor: formatMonto(totales.egresosTotal),
      color: 'text-red-400',
      bgIcon: 'bg-red-500/15',
      icon: <TrendingDown size={14} className="text-red-400" />,
    },
    {
      label: 'Pedidos totales',
      valor: totales.pedidosTotal.toLocaleString('es-AR'),
      color: 'text-gray-200',
      bgIcon: 'bg-neutral-700',
      icon: <Receipt size={14} className="text-gray-400" />,
    },
    {
      label: 'Promedio / día',
      valor: formatMonto(totales.promedioVentas),
      color: 'text-blue-400',
      bgIcon: 'bg-blue-500/15',
      icon: <BarChart3 size={14} className="text-blue-400" />,
    },
  ];

  return (
    <div className="grid grid-cols-2 xl:grid-cols-4 gap-3">
      {cards.map((c) => (
        <div
          key={c.label}
          className="flex items-center gap-2.5 px-3 py-2.5 rounded-xl border border-neutral-800/50 bg-neutral-800/30"
        >
          <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${c.bgIcon}`}>
            {c.icon}
          </div>
          <div>
            <p className="text-[10px] text-gray-500 uppercase tracking-wide">{c.label}</p>
            <p className={`text-sm font-mono font-semibold tabular-nums ${c.color}`}>
              {c.valor}
            </p>
          </div>
        </div>
      ))}
    </div>
  );
}

// ─── Selector de métrica para el gráfico ──────────────────────────────────────

type MetricaGrafico = 'totalVentasReales' | 'balanceEfectivo' | 'pedidosCerradosCount';

const METRICAS: { key: MetricaGrafico; label: string }[] = [
  { key: 'totalVentasReales', label: 'Ventas' },
  { key: 'balanceEfectivo', label: 'Balance' },
  { key: 'pedidosCerradosCount', label: 'Pedidos' },
];

// ─── Página principal ─────────────────────────────────────────────────────────

/**
 * Pantalla de consulta histórica de jornadas cerradas.
 *
 * Componentes:
 *   1. Selector de rango (predefinidos + custom) con date pickers
 *   2. Tarjetas de resumen acumulado del rango
 *   3. Gráfico de barras interactivo con selector de métrica
 *   4. Lista expandible de jornadas con detalle financiero
 *
 * Design system FoodFlow:
 *   - Dark theme (neutral-900 bg)
 *   - Rounded-2xl cards, border-neutral-800/60
 *   - Red-600 primary, emerald para positivos
 *   - font-mono tabular-nums para cifras
 */
export default function HistorialJornadasPage() {
  // ── Estado de rango ──
  const rangos = useMemo(() => getRangosPredefinidos(), []);
  const [rangoActivo, setRangoActivo] = useState(0);
  const [desde, setDesde] = useState(rangos[0].desde);
  const [hasta, setHasta] = useState(rangos[0].hasta);

  // ── Estado de UI ──
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [metrica, setMetrica] = useState<MetricaGrafico>('totalVentasReales');

  // ── Data ──
  const { data: jornadas, isLoading } = useHistorialJornadas(desde, hasta);

  // ── Handlers ──
  const handleRangoPredefinido = useCallback(
    (idx: number) => {
      const rango = rangos[idx];
      setRangoActivo(idx);
      setDesde(rango.desde);
      setHasta(rango.hasta);
      setExpandedId(null);
    },
    [rangos],
  );

  const handleDesdeChange = useCallback((value: string) => {
    setDesde(value);
    setRangoActivo(-1); // custom
    setExpandedId(null);
  }, []);

  const handleHastaChange = useCallback((value: string) => {
    setHasta(value);
    setRangoActivo(-1);
    setExpandedId(null);
  }, []);

  const toggleExpanded = useCallback((id: string) => {
    setExpandedId((prev) => (prev === id ? null : id));
  }, []);

  return (
    <section className="h-[calc(100vh-4rem)] bg-neutral-900 overflow-hidden">
      <div className="h-full w-full px-4 sm:px-6 xl:px-8 py-5 flex flex-col">
        {/* ── Header ── */}
        <header className="flex items-center gap-3 mb-5 shrink-0">
          <Link
            to="/caja"
            className="w-10 h-10 rounded-xl bg-neutral-800 hover:bg-neutral-700 flex items-center justify-center transition-colors"
          >
            <ArrowLeft size={18} className="text-gray-400" />
          </Link>
          <div>
            <h1 className="text-xl font-bold text-gray-100 tracking-tight">
              Historial de Jornadas
            </h1>
            <p className="text-sm text-gray-500">
              Consulta de cierres de caja anteriores
            </p>
          </div>
        </header>

        {/* ── Controles de rango ── */}
        <div className="shrink-0 mb-5 space-y-3">
          {/* Rangos predefinidos */}
          <div className="flex flex-wrap gap-2">
            {rangos.map((rango, idx) => (
              <button
                key={rango.label}
                onClick={() => handleRangoPredefinido(idx)}
                className={`
                  px-3 py-1.5 rounded-lg text-xs font-medium transition-all
                  ${rangoActivo === idx
                    ? 'bg-red-600 text-white shadow-lg shadow-red-600/20'
                    : 'bg-neutral-800 text-gray-400 hover:bg-neutral-700 hover:text-gray-300'
                  }
                `}
              >
                {rango.label}
              </button>
            ))}
          </div>

          {/* Date pickers custom */}
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <Calendar size={14} className="text-gray-500" />
              <input
                type="date"
                value={desde}
                onChange={(e) => handleDesdeChange(e.target.value)}
                className="bg-neutral-800 border border-neutral-700 rounded-lg px-3 py-1.5 text-xs text-gray-300 font-mono focus:outline-none focus:ring-1 focus:ring-red-500/50 focus:border-red-500/50"
              />
            </div>
            <span className="text-gray-600 text-xs">→</span>
            <div className="flex items-center gap-2">
              <input
                type="date"
                value={hasta}
                onChange={(e) => handleHastaChange(e.target.value)}
                className="bg-neutral-800 border border-neutral-700 rounded-lg px-3 py-1.5 text-xs text-gray-300 font-mono focus:outline-none focus:ring-1 focus:ring-red-500/50 focus:border-red-500/50"
              />
            </div>
            {jornadas && (
              <span className="text-[11px] text-gray-500 ml-auto">
                {jornadas.length} {jornadas.length === 1 ? 'jornada' : 'jornadas'} encontradas
              </span>
            )}
          </div>
        </div>

        {/* ── Contenido principal ── */}
        <div className="flex-1 min-h-0 grid grid-cols-1 xl:grid-cols-12 gap-5">
          {/* ── Columna izquierda: Resumen + Gráfico ── */}
          <div className="xl:col-span-5 flex flex-col min-h-0 gap-5">
            {/* Tarjetas resumen */}
            {!isLoading && jornadas && jornadas.length > 0 && (
              <div className="shrink-0">
                <ResumenRango jornadas={jornadas} />
              </div>
            )}

            {/* Gráfico */}
            <div className="flex-1 min-h-0 rounded-2xl border border-neutral-800/60 bg-neutral-900/50 p-4 flex flex-col">
              <div className="flex items-center justify-between shrink-0 mb-4">
                <div className="flex items-center gap-2">
                  <BarChart3 size={14} className="text-red-400" />
                  <h2 className="text-xs text-gray-500 uppercase tracking-wider font-medium">
                    Evolución
                  </h2>
                </div>

                {/* Selector de métrica */}
                <div className="flex gap-1">
                  {METRICAS.map((m) => (
                    <button
                      key={m.key}
                      onClick={() => setMetrica(m.key)}
                      className={`
                        px-2.5 py-1 rounded-md text-[10px] font-medium transition-all
                        ${metrica === m.key
                          ? 'bg-neutral-700 text-gray-200'
                          : 'text-gray-500 hover:text-gray-400'
                        }
                      `}
                    >
                      {m.label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex-1 min-h-0 flex items-end">
                {isLoading ? (
                  <SkeletonChart />
                ) : jornadas && jornadas.length > 0 ? (
                  <div className="w-full">
                    <GraficoJornadas jornadas={jornadas} metrica={metrica} />
                  </div>
                ) : (
                  <EmptyState />
                )}
              </div>
            </div>
          </div>

          {/* ── Columna derecha: Lista de jornadas ── */}
          <div className="xl:col-span-7 flex flex-col min-h-0 rounded-2xl border border-neutral-800/60 bg-neutral-900/50 p-4">
            <div className="flex items-center justify-between shrink-0 mb-3">
              <h2 className="text-xs text-gray-500 uppercase tracking-wider font-medium">
                Detalle por jornada
              </h2>
              {jornadas && jornadas.length > 0 && (
                <span className="text-[10px] text-gray-600 font-mono">
                  {formatFechaLarga(desde)} — {formatFechaLarga(hasta)}
                </span>
              )}
            </div>

            <div className="flex-1 overflow-y-auto scrollbar-thin space-y-2 pr-1">
              {isLoading ? (
                <SkeletonList />
              ) : jornadas && jornadas.length > 0 ? (
                jornadas.map((jornada) => (
                  <JornadaCard
                    key={jornada.id}
                    jornada={jornada}
                    isExpanded={expandedId === jornada.id}
                    onToggle={() => toggleExpanded(jornada.id)}
                  />
                ))
              ) : (
                <EmptyState />
              )}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

// ─── Skeletons & Empty ────────────────────────────────────────────────────────

function SkeletonChart() {
  return (
    <div className="w-full space-y-3 animate-pulse">
      <div className="flex items-end gap-[3px] h-36">
        {Array.from({ length: 14 }).map((_, i) => (
          <div
            key={i}
            className="flex-1 rounded-t bg-neutral-700"
            style={{ height: `${15 + Math.random() * 70}%` }}
          />
        ))}
      </div>
      <div className="flex justify-between">
        <div className="h-2 w-12 rounded bg-neutral-700" />
        <div className="h-2 w-12 rounded bg-neutral-700" />
      </div>
    </div>
  );
}

function SkeletonList() {
  return (
    <div className="space-y-2 animate-pulse">
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="h-16 rounded-xl bg-neutral-800/40" />
      ))}
    </div>
  );
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center h-full w-full py-12 text-center">
      <div className="w-12 h-12 rounded-xl bg-neutral-800 flex items-center justify-center mb-3">
        <Search size={20} className="text-gray-600" />
      </div>
      <p className="text-sm text-gray-500">
        No hay jornadas cerradas en este rango
      </p>
      <p className="text-xs text-gray-600 mt-1">
        Probá ajustando las fechas o seleccionando otro período
      </p>
    </div>
  );
}
