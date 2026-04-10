import { Navigate, Outlet } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useEstadoCaja } from '../features/caja/hooks/useCaja';

/**
 * Gatekeeper global: bloquea rutas operativas si no hay jornada de caja abierta.
 *
 * Envuelve las rutas que requieren una jornada activa (salón, mostrador, etc.).
 * Si la caja está CERRADA, redirige a /caja donde el operador ve el blank state
 * con el botón "Abrir nueva jornada".
 *
 * Rutas que NO pasan por este gatekeeper:
 * - /caja (tiene su propio blank state + modal de apertura)
 * - /caja/historial (consulta histórica, no requiere jornada activa)
 *
 * Usa el mismo hook `useEstadoCaja()` que CajaPage, por lo que comparten caché
 * de TanStack Query (key: ['estado-caja']). No hay requests duplicados.
 */
export default function RequiereJornadaAbierta() {
  const { data: estado, isLoading } = useEstadoCaja();

  // Mientras carga, muestra spinner (evita flash de redirect)
  if (isLoading) {
    return (
      <div className="h-[calc(100vh-4rem)] bg-neutral-900 flex items-center justify-center">
        <Loader2 size={32} className="text-gray-500 animate-spin" />
      </div>
    );
  }

  // Si no hay jornada abierta → redirigir a /caja
  if (estado?.estado !== 'ABIERTA') {
    return <Navigate to="/caja" replace />;
  }

  // Jornada abierta → renderizar la ruta hija normalmente
  return <Outlet />;
}
