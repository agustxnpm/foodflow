import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

// Store global de FoodFlow
const useAppStore = create(
  devtools(
    (set) => ({
      // Mesa seleccionada actualmente
      mesaSeleccionada: null,
      setMesaSeleccionada: (mesa) => set({ mesaSeleccionada: mesa }),

      // Pedido actual en edición
      pedidoActual: null,
      setPedidoActual: (pedido) => set({ pedidoActual: pedido }),

      // Limpiar todo
      reset: () => set({ mesaSeleccionada: null, pedidoActual: null }),
    }),
    { name: 'FoodFlowStore' }
  )
);

export default useAppStore;
