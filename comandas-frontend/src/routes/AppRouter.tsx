import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastContainer } from '../ui';
import MainLayout from '../layout/MainLayout';
import SalonPage from '../features/salon/pages/SalonPage';
import CajaIndex from '../features/caja/components/CajaIndex';

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
 * NOTA: El detalle de pedido (PantallaPedido) se renderiza como modal
 * overlay dentro de SalonPage, no como ruta separada.
 */
export function AppRouter() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ToastContainer />
        <Routes>
          {/* Layout principal con Navbar */}
          <Route element={<MainLayout />}>
            {/* Home: Salón (gestión de mesas + modal POS) */}
            <Route index element={<SalonPage />} />

            {/* Caja */}
            <Route path="caja" element={<CajaIndex />} />

            {/* Rutas futuras */}
            {/* <Route path="catalogo" element={<CatalogoPage />} /> */}
            {/* <Route path="promociones" element={<PromocionesPage />} /> */}
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
