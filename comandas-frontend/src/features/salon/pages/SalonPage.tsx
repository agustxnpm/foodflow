import { useState } from 'react';
import { useSalonState } from '../hooks/useSalonState';
import MesaGrid from '../components/MesaGrid';
import SalonControls from '../components/SalonControls';
import SidebarResumen from '../components/SidebarResumen';
import PedidoDetalleModal from '../components/PedidoDetalleModal';

/**
 * Página principal del módulo Salón — Vista operativa de mesas.
 *
 * Layout Split View (100vh - navbar):
 * - Panel izquierdo (75%): Grid de mesas con representación geométrica
 * - Panel derecho (25%): Sidebar de resumen rápido ("Tickets rápidos")
 *
 * Interacción: click en mesa ocupada abre modal de detalle con Skeleton Loading.
 *
 * HU-02: Ver estado de mesas
 * HU-03: Abrir mesa
 * HU-06: Consultar pedido
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
    mesaSeleccionada,
    modalAbierto,
    cargandoDetalle,
    pedidoDetalle,
    abrirDetalleMesa,
    handleAbrirMesa,
    abriendoMesa,
    cerrarModal,
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
              onMesaClick={abrirDetalleMesa}
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
            onMesaClick={abrirDetalleMesa}
          />
        </aside>
      </section>

      {/* ── Modal de Detalle ── */}
      {modalAbierto && (
        <PedidoDetalleModal
          mesa={mesaSeleccionada}
          pedido={pedidoDetalle}
          cargando={cargandoDetalle}
          onClose={cerrarModal}
          onAbrirMesa={handleAbrirMesa}
          abriendoMesa={abriendoMesa}
        />
      )}
    </>
  );
}
