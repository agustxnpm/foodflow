import { useState } from 'react';
import { useSalonState } from '../hooks/useSalonState';
import MesaGrid from '../components/MesaGrid';
import SalonControls from '../components/SalonControls';
import SidebarResumen from '../components/SidebarResumen';
import PantallaPedido from '../../pedido/pages/PantallaPedido';

/**
 * Página principal del módulo Salón — Vista operativa de mesas.
 *
 * Layout Split View (100vh - navbar):
 * - Panel izquierdo (75%): Grid de mesas con representación geométrica
 * - Panel derecho (25%): Sidebar de resumen rápido ("Tickets rápidos")
 *
 * Interacción:
 * - Mesa ABIERTA → abre modal POS (PantallaPedido como overlay)
 * - Mesa LIBRE → abre pedido y luego abre modal POS
 *
 * El modal POS se renderiza condicionalmente sobre el salón.
 * Solo se cierra con el botón "Atrás" para evitar cierres accidentales.
 *
 * HU-02: Ver estado de mesas
 * HU-03: Abrir mesa
 * HU-15: Crear mesa
 * HU-16: Eliminar mesa
 */
export default function SalonPage() {
  const [modoEdicion, setModoEdicion] = useState(false);

  const {
    mesas,
    mesasAbiertas,
    cargandoMesas,
    errorMesas,
    handleMesaClick,
    mesaSeleccionadaId,
    cerrarPedido,
    mesaPendienteApertura,
    confirmarAperturaMesa,
    cancelarAperturaMesa,
    abriendoMesa,
  } = useSalonState();

  return (
    <>
      <section className="h-[calc(100vh-4rem)] flex overflow-hidden">
        {/* ── Panel Izquierdo: Grid de Mesas (75%) ── */}
        <div className="w-3/4 flex flex-col overflow-hidden">
          {/* Controles superiores */}
          <header className="flex items-center justify-between px-6 py-4 shrink-0">
            <h1 className="text-xl font-bold text-gray-100 tracking-tight">
              Salón
            </h1>
            <SalonControls
              modoEdicion={modoEdicion}
              onToggleModoEdicion={() => setModoEdicion(!modoEdicion)}
            />
          </header>

          {/* Banner modo edición */}
          {modoEdicion && (
            <div className="mx-6 mb-3 bg-red-950/30 border border-red-800 rounded-xl px-4 py-3 text-center shrink-0">
              <p className="text-red-400 text-sm font-semibold">
                Modo Edición — Hacé click en × para eliminar mesas libres
              </p>
            </div>
          )}

          {/* Grid de mesas (scroll interno) */}
          <div className="flex-1 overflow-y-auto px-6 pb-6">
            <MesaGrid
              mesas={mesas}
              onMesaClick={handleMesaClick}
              modoEdicion={modoEdicion}
              isLoading={cargandoMesas}
              isError={errorMesas}
            />
          </div>
        </div>

        {/* ── Panel Derecho: Sidebar Resumen (25%) ── */}
        <aside className="w-1/4 border-l border-neutral-800 bg-neutral-950 overflow-hidden">
          <SidebarResumen
            mesasAbiertas={mesasAbiertas}
            onMesaClick={handleMesaClick}
          />
        </aside>
      </section>

      {/* ── Pre-modal: Confirmación de apertura de mesa LIBRE ── */}
      {mesaPendienteApertura && (
        <>
          <div
            className="fixed inset-0 z-[60] bg-black/60 backdrop-blur-sm"
            onClick={cancelarAperturaMesa}
            aria-hidden="true"
          />
          <div className="fixed inset-0 z-[70] flex items-center justify-center p-4">
            <div className="bg-neutral-900 border border-neutral-700 rounded-2xl shadow-2xl shadow-black/50 p-6 max-w-sm w-full text-center space-y-4">
              {/* Icono mesa */}
              <div className="mx-auto w-16 h-16 rounded-full bg-neutral-800 flex items-center justify-center">
                <span className="text-3xl">🍽️</span>
              </div>

              {/* Título */}
              <h3 className="text-lg font-bold text-gray-100">
                Mesa {mesaPendienteApertura.numero}
              </h3>

              {/* Mensaje */}
              <p className="text-sm text-gray-400 leading-relaxed">
                Esta mesa está libre.{' '}
                <span className="text-gray-300 font-medium">
                  Abrila para iniciar un nuevo pedido.
                </span>
              </p>

              {/* Botones */}
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={cancelarAperturaMesa}
                  className="
                    flex-1 h-11 rounded-xl
                    text-sm font-semibold
                    bg-neutral-800 text-gray-400
                    border border-neutral-700
                    hover:border-neutral-600 hover:text-gray-300
                    transition-colors active:scale-[0.97]
                  "
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={confirmarAperturaMesa}
                  disabled={abriendoMesa}
                  className="
                    flex-1 h-11 rounded-xl
                    text-sm font-bold
                    bg-red-600 text-white
                    hover:bg-red-500
                    disabled:opacity-50 disabled:cursor-not-allowed
                    transition-colors active:scale-[0.97]
                    shadow-sm shadow-red-950/40
                  "
                >
                  {abriendoMesa ? 'Abriendo…' : 'Abrir Mesa'}
                </button>
              </div>
            </div>
          </div>
        </>
      )}

      {/* ── Modal POS: Detalle de Pedido ── */}
      {mesaSeleccionadaId && (
        <PantallaPedido
          mesaId={mesaSeleccionadaId}
          onCerrar={cerrarPedido}
        />
      )}
    </>
  );
}
