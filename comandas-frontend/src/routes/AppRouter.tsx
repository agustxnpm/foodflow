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
import { useIdentidad } from '../hooks/useIdentidad';
import PantallaBloqueoLicencia from '../features/caja/components/PantallaBloqueoLicencia';
import PantallaActivacion from '../features/activacion/PantallaActivacion';
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
 * Router principal de FoodFlow.
 *
 * Flujo de gatekeeping estricto:
 *   1. useIdentidad() → ¿configurado?
 *      NO  → PantallaActivacion (sin navbar, sin atajos)
 *      SÍ  → paso 2
 *   2. useLicencia() → ¿bloqueado?
 *      BLOQUEADO   → PantallaBloqueoLicencia
 *      POR_VENCER  → banner + app normal
 *      ACTIVO      → app normal
 *
 * Nada de la app principal se renderiza si la identidad no existe o la licencia está bloqueada.
 */
export function AppRouter() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ToastContainer />
        <IdentidadGateway />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

// ── Gate 1: Identidad ────────────────────────────────────────────────────────

function LoadingScreen() {
  return (
    <div className="h-screen bg-neutral-950 flex items-center justify-center">
      <Loader2 size={32} className="text-neutral-500 animate-spin" />
    </div>
  );
}

function IdentidadGateway() {
  const { data, isLoading, isError } = useIdentidad();

  if (isLoading) return <LoadingScreen />;

  // Error de red al consultar identidad → mostrar activación
  // (el backend puede estar arrancando o no alcanzable)
  if (isError || !data) return <PantallaActivacion />;

  if (!data.configured) return <PantallaActivacion />;

  // Identidad confirmada → validar licencia
  return <LicenciaGateway />;
}

// ── Gate 2: Licencia ─────────────────────────────────────────────────────────

function LicenciaGateway() {
  const licencia = useLicencia();

  // Hook retornó null (localId aún no en store) — transición breve
  if (!licencia) return <LoadingScreen />;

  const { estado, diasRestantes, cargando } = licencia;

  if (cargando) return <LoadingScreen />;

  if (estado === 'BLOQUEADO') return <PantallaBloqueoLicencia />;

  return <AppRoutes estado={estado} diasRestantes={diasRestantes} />;
}

// ── Rutas de la aplicación ───────────────────────────────────────────────────

function AppRoutes({ estado, diasRestantes }: { estado: string; diasRestantes: number | null }) {
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
