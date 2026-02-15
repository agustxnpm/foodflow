import { createBrowserRouter } from 'react-router-dom';
import POSLayout from '../ui/layout/POSLayout';

// Lazy loading de features
import { SalonRoutes } from '../features/salon/routes';
import { PedidoRoutes } from '../features/pedido/routes';
import { CajaRoutes } from '../features/caja/routes';
import { CatalogoRoutes } from '../features/catalogo/routes';
import { PromocionesRoutes } from '../features/promociones/routes';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <POSLayout />,
    children: [
      {
        index: true,
        element: <div className="p-8">Dashboard placeholder</div>,
      },
      ...SalonRoutes,
      ...PedidoRoutes,
      ...CajaRoutes,
      ...CatalogoRoutes,
      ...PromocionesRoutes,
    ],
  },
]);
