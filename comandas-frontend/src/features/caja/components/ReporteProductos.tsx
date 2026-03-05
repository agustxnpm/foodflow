import { useState, useMemo } from 'react';
import { Package, TrendingDown } from 'lucide-react';

import { useReporteVentasProductos } from '../hooks/useCaja';

// ─── Helpers ────────────────────────────────────────────────────────────────

/** Formatea un número como moneda ARS sin centavos */
function formatearMonto(monto: number): string {
  return new Intl.NumberFormat('es-AR', {
    style: 'currency',
    currency: 'ARS',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(monto);
}

/** Obtiene la fecha de hoy formateada como YYYY-MM-DD */
function fechaHoy(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

// ─── Skeleton de carga ──────────────────────────────────────────────────────

function SkeletonTabla() {
  return (
    <div className="space-y-2">
      {Array.from({ length: 6 }).map((_, i) => (
        <div
          key={i}
          className="flex items-center gap-4 rounded-lg bg-neutral-800 px-4 py-3 animate-pulse"
        >
          <div className="h-4 w-2/5 rounded bg-neutral-700" />
          <div className="h-4 w-1/5 rounded bg-neutral-700 ml-auto" />
          <div className="h-4 w-1/4 rounded bg-neutral-700" />
        </div>
      ))}
    </div>
  );
}

// ─── Estado vacío ───────────────────────────────────────────────────────────

function EstadoVacio() {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-neutral-500">
      <TrendingDown className="h-12 w-12 mb-3 opacity-40" />
      <p className="text-sm font-medium">No hay ventas registradas para esta fecha</p>
      <p className="text-xs mt-1 opacity-60">Los productos aparecerán cuando se cierren pedidos</p>
    </div>
  );
}

// ─── Componente principal ───────────────────────────────────────────────────

/**
 * Reporte de ventas por producto.
 *
 * Muestra una tabla con el desglose de productos vendidos en una fecha,
 * agrupando cantidades y sumando totales (solo pedidos CERRADOS).
 *
 * Incluye un input date para seleccionar la fecha operativa.
 * Respeta la estética FoodFlow: tema oscuro, acentos rojos, touch-friendly.
 */
export default function ReporteProductos() {
  const [fecha, setFecha] = useState(fechaHoy);
  const { data: productos, isPending, isError } = useReporteVentasProductos(fecha);

  // Totales agregados
  const totales = useMemo(() => {
    if (!productos || productos.length === 0) return { unidades: 0, recaudado: 0 };
    return productos.reduce(
      (acc, p) => ({
        unidades: acc.unidades + p.cantidadTotal,
        recaudado: acc.recaudado + p.totalRecaudado,
      }),
      { unidades: 0, recaudado: 0 },
    );
  }, [productos]);

  return (
    <section className="rounded-xl border border-neutral-700 bg-neutral-900 p-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-5">
        <div className="flex items-center gap-2">
          <Package className="h-5 w-5 text-red-500" />
          <h2 className="text-lg font-semibold text-neutral-100">
            Ventas por Producto
          </h2>
        </div>

        {/* Selector de fecha */}
        <input
          type="date"
          value={fecha}
          onChange={(e) => setFecha(e.target.value)}
          className="rounded-lg border border-neutral-600 bg-neutral-800 px-3 py-2 text-sm text-neutral-200 focus:border-red-500 focus:outline-none focus:ring-1 focus:ring-red-500"
        />
      </div>

      {/* Contenido */}
      {isPending ? (
        <SkeletonTabla />
      ) : isError ? (
        <div className="text-center py-8 text-red-400 text-sm">
          Error al cargar el reporte. Intentá nuevamente.
        </div>
      ) : !productos || productos.length === 0 ? (
        <EstadoVacio />
      ) : (
        <>
          {/* Tabla */}
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-neutral-700">
                  <th className="pb-3 text-xs font-medium uppercase tracking-wide text-neutral-400">
                    Nombre del producto
                  </th>
                  <th className="pb-3 text-xs font-medium uppercase tracking-wide text-neutral-400 text-right">
                    Cantidad
                  </th>
                  <th className="pb-3 text-xs font-medium uppercase tracking-wide text-neutral-400 text-right">
                    Total Recaudado
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-800">
                {productos.map((producto) => (
                  <tr
                    key={producto.productoNombre}
                    className="hover:bg-neutral-800/60 transition-colors"
                  >
                    <td className="py-3 pr-4 text-sm text-neutral-200">
                      {producto.productoNombre}
                    </td>
                    <td className="py-3 text-sm font-mono text-neutral-300 text-right tabular-nums">
                      {producto.cantidadTotal}
                    </td>
                    <td className="py-3 text-sm font-mono font-medium text-red-500 text-right tabular-nums">
                      {formatearMonto(producto.totalRecaudado)}
                    </td>
                  </tr>
                ))}
              </tbody>

              {/* Footer con totales */}
              <tfoot>
                <tr className="border-t border-neutral-600">
                  <td className="pt-3 text-sm font-semibold text-neutral-200">
                    Total
                  </td>
                  <td className="pt-3 text-sm font-mono font-semibold text-neutral-200 text-right tabular-nums">
                    {totales.unidades}
                  </td>
                  <td className="pt-3 text-sm font-mono font-bold text-red-400 text-right tabular-nums">
                    {formatearMonto(totales.recaudado)}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        </>
      )}
    </section>
  );
}
