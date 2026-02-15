# FoodFlow Frontend

Sistema de gestión gastronómica offline-first.

## Estructura Feature-First

```
src/
├── app/             → Router principal
├── features/        → Módulos de negocio
│   ├── salon/       → Gestión de mesas
│   ├── pedido/      → Comanda digital
│   ├── caja/        → Cierre y pagos
│   ├── catalogo/    → Productos y variantes
│   └── promociones/ → Descuentos
├── ui/              → Componentes base reutilizables
├── lib/             → Configuración (Axios, Query)
├── store/           → Estado global (Zustand)
└── styles/          → CSS global (Tailwind)
```

## Ejecutar desarrollo

```bash
npm install
npm run dev
```

## Configuración local

Crear `.env.local`:

```
VITE_LOCAL_ID=00000000-0000-0000-0000-000000000001
```

## Stack

- React 18 + Vite
- TanStack Query (estado servidor)
- Zustand (estado cliente)
- Tailwind CSS
- Lucide React (íconos)
- Axios
- React Router DOM
