import CajaPage from '../components/CajaPage';
import HistorialJornadasPage from '../components/HistorialJornadasPage';

export const CajaRoutes = [
  {
    path: '/caja',
    element: <CajaPage />,
  },
  {
    path: '/caja/historial',
    element: <HistorialJornadasPage />,
  },
];
