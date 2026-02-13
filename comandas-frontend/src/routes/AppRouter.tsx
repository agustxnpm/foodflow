import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { MainLayout } from '../layout/MainLayout';
import { Dashboard } from '../pages/Dashboard';
import { MesasPage } from '../pages/MesasPage';
import { ProductosPage } from '../pages/ProductosPage';

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="mesas" element={<MesasPage />} />
          <Route path="productos" element={<ProductosPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
