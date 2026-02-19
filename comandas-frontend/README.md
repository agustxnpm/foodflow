# FoodFlow Frontend

Sistema de punto de venta (POS) para restaurantes y locales gastronómicos.

## 🚀 Inicio Rápido

### Prerrequisitos

- Node.js 18+ 
- npm o pnpm
- Backend Java Spring Boot corriendo en `http://localhost:8080`

### Instalación

```bash
npm install
```

### Configuración

El archivo `.env` ya está configurado con valores por defecto:

```env
VITE_LOCAL_ID=1
VITE_API_URL=http://localhost:8080/api
```

### Desarrollo

```bash
npm run dev
```

La aplicación estará disponible en `http://localhost:3000`

### Build

```bash
npm run build
```

## 🏗️ Arquitectura

### Stack Tecnológico

- **React 18** + **TypeScript**
- **Vite** - Build tool
- **TanStack Query v5** - Sincronización de estado con backend
- **React Router DOM** - Navegación
- **Tailwind CSS** - Estilos
- **Lucide React** - Iconografía
- **Zustand** - Estado global (toasts)

### Estructura Feature-First

```
src/
├── features/          # Módulos de negocio
│   ├── salon/        # ✅ Gestión de mesas (HU-02 a HU-16)
│   ├── pedido/       # 🚧 Gestión de pedidos
│   ├── caja/         # 🚧 Cierre de caja
│   ├── catalogo/     # 🚧 Productos
│   └── promociones/  # 🚧 Descuentos
├── ui/               # Componentes base reutilizables
├── hooks/            # Hooks globales (useToast)
├── store/            # Estado global (Zustand)
└── lib/              # Configuración (apiClient, queryClient)
```

### Principios de Diseño

#### Feature-First Architecture
Cada feature es autónomo y contiene:
- `types.ts` - Tipos de dominio
- `api/` - Cliente HTTP
- `hooks/` - React Query hooks
- `components/` - Componentes UI
- `pages/` - Páginas principales
- `index.ts` - Exportaciones públicas

#### Sincronización con Backend
- **TanStack Query** maneja el estado del servidor
- Invalidación automática por prefijos
- No duplicamos lógica de negocio en frontend

#### Identidad Visual (FoodFlow UI)
- **Tema oscuro** obligatorio
- **Colores**: Rojo primario + Negro/Gris + Blanco
- **Táctil-friendly**: Botones grandes (min h-12)
- **Feedback visual** claro en todas las acciones

## 📋 Módulos Implementados

### ✅ Salón (Home)

**Ruta**: `/` (página principal)

**Historias de Usuario**:
- HU-02: Ver estado de mesas
- HU-03: Abrir mesa
- HU-04: Cerrar mesa
- HU-06: Ver pedido de mesa
- HU-12: Cierre con liquidación final
- HU-15: Crear mesa
- HU-16: Eliminar mesa

**Componentes**:
- `MesaCard` - Tarjeta individual de mesa
- `MesaGrid` - Grid responsive de mesas
- `SalonControls` - Controles de gestión
- `CierreMesaModal` - Modal de cierre
- `SalonPage` - Página orquestadora

## 🔧 Scripts Disponibles

```bash
npm run dev          # Servidor de desarrollo
npm run build        # Build de producción
npm run preview      # Preview del build
npm run lint         # Ejecutar ESLint
```

## 🎨 Guía de Estilos

### Colores Principales

```css
/* Fondos */
bg-neutral-900       /* Fondo principal */
bg-neutral-800       /* Tarjetas/superficies */

/* Acción primaria */
bg-red-600           /* Botones principales */
text-red-500         /* Énfasis */

/* Estados */
border-gray-600      /* Mesa libre */
border-red-600       /* Mesa ocupada */

/* Textos */
text-gray-100        /* Texto principal */
text-gray-400        /* Texto secundario */
```

### Componentes Base (ui/)

```tsx
<Button variant="primary">Acción Principal</Button>
<Card>Contenido</Card>
<Input label="Campo" error="Mensaje de error" />
```

### Sistema de Toasts

```tsx
import useToast from '@/hooks/useToast';

const toast = useToast();

toast.success('Operación exitosa');
toast.error('Error en la operación');
toast.warning('Advertencia');
toast.info('Información');
```

## 🔄 Integración con Backend

El frontend espera los siguientes endpoints:

### Mesas
- `GET /api/mesas` - Listar mesas
- `POST /api/mesas` - Crear mesa
- `POST /api/mesas/{id}/abrir` - Abrir mesa
- `DELETE /api/mesas/{id}` - Eliminar mesa
- `GET /api/mesas/{id}/pedido-actual` - Consultar pedido
- `POST /api/mesas/{id}/cierre` - Cerrar mesa

Todos los requests incluyen el header:
```
X-Local-Id: 1
```

## 📝 Próximas Features

- [ ] Módulo Pedido (gestión de ítems)
- [ ] Módulo Catálogo (ABM productos)
- [ ] Módulo Promociones
- [ ] Módulo Caja (cierre de jornada)
- [ ] Impresión de tickets
- [ ] Modo offline con sincronización

## 🤝 Contribución

Este proyecto sigue la arquitectura hexagonal del backend:
- El **dominio** vive en el backend
- El frontend **orquesta** acciones de usuario
- **React Query** sincroniza estado

---

**FoodFlow** - Sistema POS para locales gastronómicos pequeños


```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```
