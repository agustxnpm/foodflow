/**
 * Módulo Salón - Feature Index
 * Exporta los componentes y hooks públicos del módulo
 */

// Páginas
export { default as SalonPage } from './pages/SalonPage';

// Componentes
export { default as MesaCard } from './components/MesaCard';
export { default as MesaGrid } from './components/MesaGrid';
export { default as SalonControls } from './components/SalonControls';
export { default as SidebarResumen } from './components/SidebarResumen';
export { default as PedidoDetalleModal } from './components/PedidoDetalleModal';
export { default as CierreMesaModal } from './components/CierreMesaModal';

// Hooks
export { useSalonState } from './hooks/useSalonState';
export {
  useMesas,
  usePedidoMesa,
  useCrearMesa,
  useAbrirMesa,
  useCerrarMesa,
  useEliminarMesa,
  useObtenerTicket,
  useObtenerComanda,
} from './hooks/useMesas';

// API
export { mesasApi } from './api/mesasApi';

// Tipos
export type {
  Mesa,
  EstadoMesa,
  EstadoPedido,
  MedioPago,
  CrearMesaRequest,
  AbrirMesaRequest,
  AbrirMesaResponse,
  PagoRequest,
  PagoResponse,
  CerrarMesaRequest,
  CerrarMesaResponse,
  ReabrirPedidoResponse,
} from './types';
