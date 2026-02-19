import { Clock, Receipt } from 'lucide-react';
import type { Mesa } from '../types';

interface SidebarResumenProps {
  mesasAbiertas: Mesa[];
  onMesaClick: (mesa: Mesa) => void;
}

/**
 * Panel lateral derecho con resumen rápido de mesas activas ("Tickets rápidos").
 *
 * Objetivo operativo: lectura instantánea de facturación acumulada
 * sin necesidad de entrar al detalle de cada mesa.
 *
 * Cada ítem muestra: Número de Mesa, Hora de Ingreso y Total Acumulado (destacado).
 */
export default function SidebarResumen({
  mesasAbiertas,
  onMesaClick,
}: SidebarResumenProps) {
  const formatHora = (fecha: string) => {
    const d = new Date(fecha);
    return d.toLocaleTimeString('es-AR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const totalGeneral = mesasAbiertas.reduce(
    (acc, m) => acc + (m.pedidoActual?.total ?? 0),
    0
  );

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
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

      {/* Lista de mesas activas */}
      <div className="flex-1 overflow-y-auto">
        {mesasAbiertas.length === 0 ? (
          <div className="flex items-center justify-center h-full p-4">
            <p className="text-gray-600 text-sm text-center">
              No hay mesas abiertas en este momento
            </p>
          </div>
        ) : (
          <ul className="divide-y divide-neutral-800">
            {mesasAbiertas.map((mesa) => (
              <li key={mesa.id}>
                <button
                  type="button"
                  onClick={() => onMesaClick(mesa)}
                  className="
                    w-full px-4 py-4
                    flex items-center justify-between
                    hover:bg-neutral-800/60
                    transition-colors
                    active:scale-[0.98]
                    text-left
                  "
                  aria-label={`Ver detalle Mesa ${mesa.numero}`}
                >
                  {/* Info izquierda */}
                  <div className="space-y-1">
                    <span className="text-base font-bold text-gray-100">
                      Mesa {mesa.numero}
                    </span>
                    {mesa.pedidoActual && (
                      <div className="flex items-center gap-1.5 text-xs text-gray-500">
                        <Clock size={12} />
                        <span>
                          Ingreso{' '}
                          {formatHora(mesa.pedidoActual.fechaApertura)}
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Total acumulado */}
                  {mesa.pedidoActual && (
                    <div className="text-right">
                      <p className="text-[10px] text-gray-500 uppercase tracking-wider">
                        Total acumulado
                      </p>
                      <p className="text-lg font-bold text-red-400 font-mono">
                        $ {mesa.pedidoActual.total.toLocaleString('es-AR')}
                      </p>
                    </div>
                  )}
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Footer con total general */}
      {mesasAbiertas.length > 0 && (
        <div className="border-t border-neutral-800 px-4 py-4 bg-neutral-900/80">
          <div className="flex items-center justify-between">
            <span className="text-sm font-semibold text-gray-400">
              Facturación activa
            </span>
            <span className="text-xl font-bold text-red-500 font-mono">
              $ {totalGeneral.toLocaleString('es-AR')}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
