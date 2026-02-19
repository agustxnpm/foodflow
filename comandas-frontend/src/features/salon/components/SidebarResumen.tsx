import { useState, useMemo } from 'react';
import { Clock, ChevronRight, Receipt, UtensilsCrossed, Armchair, Search, ArrowDownUp, ArrowUp, ArrowDown } from 'lucide-react';
import type { Mesa } from '../types';
import {
  usePedidosMesasAbiertas,
  type ResumenMesaAbierta,
} from '../hooks/usePedidosMesasAbiertas';

// ─── Skeleton Loading ─────────────────────────────────────────────────────────

/**
 * Skeleton individual que simula la estructura de un ítem del sidebar.
 * Mantiene la misma altura y distribución que el ítem real para evitar
 * layout shift cuando los datos llegan.
 */
function SidebarItemSkeleton() {
  return (
    <div
      className="px-3 py-4 border-b-2 border-neutral-800 animate-pulse"
      aria-hidden="true"
    >
      <div className="grid grid-cols-[1fr_auto_1fr_auto] items-center gap-3">
        {/* Col izquierda: Ingreso */}
        <div className="flex flex-col items-center gap-1.5">
          <div className="h-3 w-12 rounded bg-neutral-700" />
          <div className="h-7 w-16 rounded-md bg-neutral-700" />
        </div>

        {/* Col central: Mesa */}
        <div className="flex flex-col items-center gap-1.5">
          <div className="h-8 w-8 rounded-full bg-neutral-700" />
          <div className="h-4 w-14 rounded bg-neutral-700" />
        </div>

        {/* Col derecha: Total */}
        <div className="flex flex-col items-center gap-1.5">
          <div className="h-3 w-16 rounded bg-neutral-700" />
          <div className="h-6 w-14 rounded bg-neutral-700" />
        </div>

        {/* Chevron */}
        <div className="h-5 w-5 rounded bg-neutral-700" />
      </div>
    </div>
  );
}

// ─── Ítem de mesa activa ──────────────────────────────────────────────────────

interface SidebarItemProps {
  resumen: ResumenMesaAbierta;
  onVerPedido: (mesaId: string) => void;
}

/**
 * Tarjeta individual de mesa activa en el sidebar.
 *
 * Estructura visual (3 bloques):
 * - Tiempo: badge con hora de ingreso
 * - Identidad: número de mesa + total acumulado destacado
 * - Acción: botón de acceso al detalle
 */
function SidebarItem({ resumen, onVerPedido }: SidebarItemProps) {
  const fecha = new Date(resumen.fechaApertura);
  const horaIngreso = `${String(fecha.getHours()).padStart(2, '0')}:${String(fecha.getMinutes()).padStart(2, '0')}`;

  return (
    <li className="border-b-2 border-red-600/30 last:border-b-0">
      <button
        type="button"
        onClick={() => onVerPedido(resumen.mesaId)}
        className="
          w-full px-3 py-4
          grid grid-cols-[1fr_auto_1fr_auto] items-center gap-3
          hover:bg-neutral-800/50
          transition-colors duration-150
          active:scale-[0.98]
          group
        "
        aria-label={`Ver pedido de Mesa ${resumen.numeroMesa}`}
      >
        {/* ── Columna Izquierda: Ingreso ── */}
        <div className="flex flex-col items-center gap-1">
          <span className="text-[10px] font-semibold text-gray-500 uppercase tracking-widest">
            Ingreso
          </span>
          <div
            className="
              flex items-center gap-1.5
              px-2.5 py-1
              rounded-md
              border border-neutral-700
              bg-neutral-800/80
            "
          >
            <Clock size={12} className="text-gray-500 shrink-0" />
            <span className="text-sm font-semibold text-gray-300 tabular-nums whitespace-nowrap">
              {horaIngreso}
            </span>
          </div>
        </div>

        {/* ── Columna Central: Mesa ── */}
        <div className="flex flex-col items-center gap-1">
          <div className="w-9 h-9 rounded-full bg-neutral-800 border border-neutral-700 flex items-center justify-center">
            <Armchair size={18} className="text-red-500" />
          </div>
          <span className="text-sm font-bold text-gray-100">
            Mesa {resumen.numeroMesa}
          </span>
        </div>

        {/* ── Columna Derecha: Total Acumulado ── */}
        <div className="flex flex-col items-center gap-1">
          <span className="text-[10px] font-semibold text-gray-500 uppercase tracking-widest">
            Total acumulado
          </span>
          <span className="text-lg font-bold text-gray-50 font-mono tabular-nums">
            $ {resumen.totalAcumulado.toLocaleString('es-AR')}
          </span>
        </div>

        {/* ── Chevron ── */}
        <ChevronRight
          size={20}
          className="text-gray-600 group-hover:text-red-500 transition-colors duration-150"
        />
      </button>
    </li>
  );
}

// ─── Sidebar Principal ────────────────────────────────────────────────────────

interface SidebarResumenProps {
  mesasAbiertas: Mesa[];
  onMesaClick: (mesa: Mesa) => void;
}

/**
 * Panel lateral derecho con resumen rápido de mesas activas.
 *
 * Objetivo operativo: lectura instantánea de facturación acumulada
 * sin necesidad de entrar al detalle de cada mesa.
 *
 * Cada ítem muestra: Hora de Ingreso, Número de Mesa y Total Acumulado.
 * El total general de facturación activa se muestra en el footer.
 */
export default function SidebarResumen({
  mesasAbiertas,
  onMesaClick,
}: SidebarResumenProps) {
  const [busqueda, setBusqueda] = useState('');
  const [orden, setOrden] = useState<'mesa' | 'llegada'>('mesa');
  const [direccion, setDireccion] = useState<'asc' | 'desc'>('asc');

  const { resumenes, totalGeneral, cargando } =
    usePedidosMesasAbiertas(mesasAbiertas);

  /** Filtra y ordena los resúmenes según búsqueda, criterio y dirección */
  const resumenesFiltrados = useMemo(() => {
    let resultado = busqueda.trim()
      ? resumenes.filter((r) =>
          String(r.numeroMesa).includes(busqueda.trim())
        )
      : [...resumenes];

    const factor = direccion === 'asc' ? 1 : -1;

    if (orden === 'llegada') {
      resultado.sort(
        (a, b) => factor * (new Date(a.fechaApertura).getTime() - new Date(b.fechaApertura).getTime())
      );
    } else {
      resultado.sort((a, b) => factor * (a.numeroMesa - b.numeroMesa));
    }

    return resultado;
  }, [resumenes, busqueda, orden, direccion]);

  /** Convierte el click del sidebar (por mesaId) al formato que espera SalonPage */
  const handleVerPedido = (mesaId: string) => {
    const mesa = mesasAbiertas.find((m) => m.id === mesaId);
    if (mesa) onMesaClick(mesa);
  };

  return (
    <div className="flex flex-col h-full">
      {/* ── Header ── */}
      <div className="px-4 py-5 border-b border-neutral-800">
        <div className="flex items-center gap-2">
          <Receipt size={20} className="text-red-500" />
          <h2 className="text-lg font-bold text-gray-100">Mesas Activas</h2>
        </div>
        <p className="text-xs text-gray-500 mt-1">
          {mesasAbiertas.length} mesa{mesasAbiertas.length !== 1 ? 's' : ''} con
          pedido abierto
        </p>
      </div>

      {/* ── Buscador typeahead + ordenamiento ── */}
      {mesasAbiertas.length > 0 && (
        <div className="px-4 py-3 border-b border-neutral-800 space-y-2">
          <div className="relative">
            <Search
              size={16}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 pointer-events-none"
            />
            <input
              type="text"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              placeholder="Buscar mesa..."
              className="
                w-full pl-9 pr-3 py-2
                bg-neutral-800 border border-neutral-700
                rounded-lg text-sm text-gray-200
                placeholder:text-gray-600
                focus:outline-none focus:border-red-600
                transition-colors
              "
            />
          </div>

          {/* Toggle de ordenamiento */}
          <div className="flex items-center gap-1.5">
            <ArrowDownUp size={12} className="text-gray-600 shrink-0" />
            <button
              type="button"
              onClick={() => {
                if (orden === 'mesa') {
                  setDireccion((d) => (d === 'asc' ? 'desc' : 'asc'));
                } else {
                  setOrden('mesa');
                  setDireccion('asc');
                }
              }}
              className={[
                'px-2.5 py-1 rounded-md text-[11px] font-semibold transition-colors inline-flex items-center gap-1',
                orden === 'mesa'
                  ? 'bg-neutral-700 text-gray-200'
                  : 'text-gray-600 hover:text-gray-400',
              ].join(' ')}
            >
              Nº mesa
              {orden === 'mesa' && (
                direccion === 'asc'
                  ? <ArrowUp size={10} className="text-red-500" />
                  : <ArrowDown size={10} className="text-red-500" />
              )}
            </button>
            <button
              type="button"
              onClick={() => {
                if (orden === 'llegada') {
                  setDireccion((d) => (d === 'asc' ? 'desc' : 'asc'));
                } else {
                  setOrden('llegada');
                  setDireccion('asc');
                }
              }}
              className={[
                'px-2.5 py-1 rounded-md text-[11px] font-semibold transition-colors inline-flex items-center gap-1',
                orden === 'llegada'
                  ? 'bg-neutral-700 text-gray-200'
                  : 'text-gray-600 hover:text-gray-400',
              ].join(' ')}
            >
              Llegada
              {orden === 'llegada' && (
                direccion === 'asc'
                  ? <ArrowUp size={10} className="text-red-500" />
                  : <ArrowDown size={10} className="text-red-500" />
              )}
            </button>
          </div>
        </div>
      )}

      {/* ── Lista de mesas activas ── */}
      <div className="flex-1 overflow-y-auto">
        {mesasAbiertas.length === 0 ? (
          /* Estado vacío */
          <div className="flex flex-col items-center justify-center h-full p-6 gap-3">
            <div className="w-14 h-14 rounded-full bg-neutral-800 flex items-center justify-center">
              <UtensilsCrossed size={24} className="text-gray-600" />
            </div>
            <p className="text-gray-600 text-sm text-center leading-relaxed">
              No hay mesas abiertas
              <br />
              <span className="text-gray-700 text-xs">
                Abrí una mesa desde el panel izquierdo
              </span>
            </p>
          </div>
        ) : cargando ? (
          /* Skeleton loading */
          <ul aria-busy="true" aria-label="Cargando resumen de mesas">
            {Array.from({ length: mesasAbiertas.length }).map((_, i) => (
              <SidebarItemSkeleton key={i} />
            ))}
          </ul>
        ) : resumenesFiltrados.length === 0 && busqueda.trim() ? (
          /* Sin resultados de búsqueda */
          <div className="flex flex-col items-center justify-center p-6 gap-2">
            <Search size={20} className="text-gray-600" />
            <p className="text-gray-600 text-sm text-center">
              No se encontró la mesa <span className="font-bold text-gray-400">{busqueda}</span>
            </p>
          </div>
        ) : (
          /* Lista real */
          <ul>
            {resumenesFiltrados.map((resumen) => (
              <SidebarItem
                key={resumen.mesaId}
                resumen={resumen}
                onVerPedido={handleVerPedido}
              />
            ))}
          </ul>
        )}
      </div>

      {/* ── Footer: Total general ── */}
      {mesasAbiertas.length > 0 && !cargando && (
        <div className="border-t border-neutral-800 px-4 py-4 bg-neutral-900/80">
          <div className="flex items-center justify-between">
            <span className="text-sm font-semibold text-gray-400">
              Facturación hasta el momento
            </span>
            <span className="text-xl font-bold text-red-500 font-mono tabular-nums">
              $ {totalGeneral.toLocaleString('es-AR')}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
