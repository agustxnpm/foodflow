import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastContainer } from '../ui';
import MainLayout from '../layout/MainLayout';
import RequiereJornadaAbierta from '../components/RequiereJornadaAbierta';
import SalonPage from '../features/salon/pages/SalonPage';
import CajaPage from '../features/caja/components/CajaPage';
import HistorialJornadasPage from '../features/caja/components/HistorialJornadasPage';
import MostradorPantalla from '../pages/MostradorPantalla';
import { useLicencia } from '../hooks/useLicencia';
import PantallaBloqueoLicencia from '../features/caja/components/PantallaBloqueoLicencia';
import IndicadorLicencia from '../components/IndicadorLicencia';
import { Loader2 } from 'lucide-react';

// Configuración de React Query
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 30000,
    },
  },
});

/**
 * Router principal de la aplicación FoodFlow
 *
 * MainLayout actúa como wrapper de toda la app,
 * provee la Navbar superior y renderiza <Outlet /> para la ruta activa.
 *
 * La validación de licencia se hace al nivel más alto (antes del layout).
 * Si la licencia está bloqueada, se renderiza la Lock Screen sin navbar.
 *
 * NOTA: El detalle de pedido (PantallaPedido) se renderiza como modal
 * overlay dentro de SalonPage, no como ruta separada.
 */
export function AppRouter() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ToastContainer />
        <LicenciaGateway />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

/**
 * Gateway de licencia: valida el Gist remoto antes de renderizar la app.
 *
 * - Cargando → spinner
 * - BLOQUEADO → Lock Screen (sin navbar, sin navegación)
 * - ACTIVO / POR_VENCER → rutas normales (con indicador sutil si POR_VENCER)
 */
function LicenciaGateway() {
  const { estado, diasRestantes, cargando } = useLicencia();

  if (cargando) {
    return (
      <div className="h-screen bg-neutral-900 flex items-center justify-center">
        <Loader2 size={32} className="text-gray-500 animate-spin" />
      </div>
    );
  }

  if (estado === 'BLOQUEADO') {
    return <PantallaBloqueoLicencia />;
  }

  return (
    <>
      {estado === 'POR_VENCER' && (
        <div className="sticky top-0 z-[60] flex justify-center py-1 bg-neutral-950">
          <IndicadorLicencia estado={estado} diasRestantes={diasRestantes} />
        </div>
      )}
      <Routes>
        {/* Layout principal con Navbar */}
        <Route element={<MainLayout />}>

          {/* Rutas que requieren jornada de caja abierta */}
          <Route element={<RequiereJornadaAbierta />}>
            {/* Home: Salón (gestión de mesas + modal POS) */}
            <Route index element={<SalonPage />} />

            {/* Mostrador: Catálogo + Promociones */}
            <Route path="mostrador" element={<MostradorPantalla />} />
          </Route>

          {/* Caja: tiene su propio gatekeeper (blank state + apertura) */}
          <Route path="caja" element={<CajaPage />} />
          <Route path="caja/historial" element={<HistorialJornadasPage />} />
        </Route>
      </Routes>
    </>
  );
}
