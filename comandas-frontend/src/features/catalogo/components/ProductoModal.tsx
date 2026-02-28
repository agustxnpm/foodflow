import { useState } from 'react';
import { X, Check, HelpCircle } from 'lucide-react';
import { useCrearProducto, useEditarProducto } from '../hooks/useProductos';
import type { ProductoResponse, ProductoRequest } from '../types';
import { useCategorias } from '../../categorias/hooks/useCategorias';

interface ProductoModalProps {
  /** null = crear nuevo, objeto = editar existente */
  producto: ProductoResponse | null;
  onClose: () => void;
}

/**
 * Modal para crear o editar un producto del catálogo.
 *
 * Campos: Nombre, Precio, Controla Stock (checkbox), Color (selector visual).
 *
 * Decisión: Se usa un selector visual de colores predefinidos
 * en lugar de un color picker completo, porque el operador
 * necesita elegir rápido sin precision de diseño.
 */
export default function ProductoModal({ producto, onClose }: ProductoModalProps) {
  const esEdicion = producto !== null;

  const crearProducto = useCrearProducto();
  const editarProducto = useEditarProducto();
  const { data: categorias = [] } = useCategorias();

  // Solo categorías asignables (no "Todos")
  const categoriasAsignables = categorias;

  const [nombre, setNombre] = useState(producto?.nombre ?? '');
  const [precio, setPrecio] = useState(producto?.precio?.toString() ?? '');
  const [controlaStock, setControlaStock] = useState(producto?.controlaStock ?? false);
  const [categoriaId, setCategoriaId] = useState<string | null>(producto?.categoriaId ?? null);
  const [esModificadorEstructural, setEsModificadorEstructural] = useState(
    producto?.esModificadorEstructural ?? false
  );
  const [error, setError] = useState<string | null>(null);

  const handleGuardarProducto = () => {
    setError(null);

    // Validaciones de UI (las de dominio corren en el backend)
    if (!nombre.trim()) {
      setError('El nombre del producto es obligatorio');
      return;
    }

    const precioNum = parseFloat(precio);
    if (isNaN(precioNum) || precioNum < 0) {
      setError('El precio debe ser un número mayor o igual a cero');
      return;
    }

    // Derivar esExtra y colorHex de la categoría seleccionada
    const categoriaSeleccionada = categoriaId
      ? categoriasAsignables.find((c) => c.id === categoriaId)
      : null;

    const esExtra = categoriaSeleccionada?.esCategoriaExtra ?? false;

    const data: ProductoRequest = {
      nombre: nombre.trim(),
      precio: precioNum,
      colorHex: categoriaSeleccionada?.colorHex ?? '#FFFFFF',
      controlaStock,
      esExtra,
      categoriaId: categoriaId ?? undefined,
      // Solo enviar esModificadorEstructural si es un extra; para productos normales siempre false
      esModificadorEstructural: esExtra ? esModificadorEstructural : false,
    };

    if (esEdicion) {
      editarProducto.mutate(
        { id: producto.id, ...data },
        { onSuccess: onClose }
      );
    } else {
      crearProducto.mutate(data, { onSuccess: onClose });
    }
  };

  const isSaving = crearProducto.isPending || editarProducto.isPending;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-50 bg-black/60 animate-backdrop-in"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none">
        <div
          className="bg-neutral-900 border border-gray-800 rounded-xl w-full max-w-md pointer-events-auto animate-modal-in"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-800">
            <h2 className="text-lg font-semibold text-text-primary">
              {esEdicion ? 'Editar Producto' : 'Nuevo Producto'}
            </h2>
            <button
              onClick={onClose}
              className="p-2 rounded-lg hover:bg-gray-800 text-gray-400 transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          {/* Body */}
          <div className="px-6 py-5 space-y-5">
            {/* Nombre */}
            <div className="flex flex-col gap-1">
              <label className="text-sm text-text-secondary">Nombre</label>
              <input
                type="text"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                placeholder="Ej: Hamburguesa Completa"
                className="min-h-[48px] px-4 bg-background-card border border-gray-700 rounded-lg text-text-primary focus:border-primary focus:outline-none"
                autoFocus
              />
            </div>

            {/* Precio */}
            <div className="flex flex-col gap-1">
              <label className="text-sm text-text-secondary">Precio</label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 font-mono">$</span>
                <input
                  type="number"
                  value={precio}
                  onChange={(e) => setPrecio(e.target.value)}
                  placeholder="0"
                  min="0"
                  step="any"
                  className="w-full min-h-[48px] pl-8 pr-4 bg-background-card border border-gray-700 rounded-lg text-text-primary font-mono focus:border-primary focus:outline-none"
                />
              </div>
            </div>

            {/* Controla Stock */}
            <label className="flex items-center gap-3 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={controlaStock}
                onChange={(e) => setControlaStock(e.target.checked)}
                className="w-5 h-5 rounded border-gray-600 bg-background-card text-primary focus:ring-primary focus:ring-offset-0"
              />
              <span className="text-sm text-text-primary">Controla stock</span>
              <span className="text-xs text-text-secondary">(activa inventario)</span>
            </label>

            {/* Selector de Categoría */}
            <div className="space-y-2">
              <label className="text-sm text-text-secondary">Categoría</label>
              <div className="grid grid-cols-2 gap-2">
                {categoriasAsignables.map((cat) => {
                  const isSelected = categoriaId === cat.id;
                  return (
                    <button
                      key={cat.id}
                      type="button"
                      onClick={() => {
                        setCategoriaId(cat.id);
                        // Reset modificador estructural si la nueva categoría no es de extras
                        if (!cat.esCategoriaExtra) setEsModificadorEstructural(false);
                      }}
                      className={[
                        'flex items-center gap-2.5 px-3 py-2.5 rounded-lg border transition-all text-left',
                        isSelected
                          ? 'border-white/40 bg-white/10 text-white ring-1 ring-white/20'
                          : 'border-gray-700 bg-background-card text-gray-400 hover:border-gray-500 hover:text-gray-200',
                      ].join(' ')}
                    >
                      <span
                        className="w-4 h-4 rounded-full shrink-0 border border-white/10"
                        style={{ backgroundColor: cat.colorHex }}
                      />
                      <span className="text-sm font-medium truncate">{cat.nombre}</span>
                      {isSelected && (
                        <Check size={14} className="ml-auto shrink-0 text-green-400" strokeWidth={3} />
                      )}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Afecta composición — solo visible cuando la categoría es de extras */}
            {(() => {
              const catSel = categoriaId ? categoriasAsignables.find((c) => c.id === categoriaId) : null;
              if (!catSel?.esCategoriaExtra) return null;
              return (
                <div className="space-y-2">
                  <label className="flex items-center gap-3 cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={esModificadorEstructural}
                      onChange={(e) => setEsModificadorEstructural(e.target.checked)}
                      className="w-5 h-5 rounded border-gray-600 bg-background-card text-primary focus:ring-primary focus:ring-offset-0"
                    />
                    <span className="text-sm text-text-primary">Afecta composición del producto</span>
                  </label>
                  <div className="flex items-start gap-2 px-3 py-2.5 rounded-lg bg-amber-950/20 border border-amber-800/30">
                    <HelpCircle size={16} className="text-amber-400 shrink-0 mt-0.5" />
                    <p className="text-xs text-amber-200/80 leading-relaxed">
                      Ejemplo: un <strong>medallón extra</strong> suma un disco de carne al plato.
                      Si activás esto, el extra <strong>solo va a aparecer</strong> en el producto más grande
                      (ej: la triple, no en la simple ni la doble).
                      Para extras como queso, huevo o panceta <strong>no hace falta activarlo</strong>.
                    </p>
                  </div>
                </div>
              );
            })()}

            {/* Error */}
            {error && (
              <p className="text-sm text-red-500">{error}</p>
            )}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-800">
            <button
              onClick={onClose}
              className="btn-secondary text-sm !min-h-[42px] px-5"
              disabled={isSaving}
            >
              Cancelar
            </button>
            <button
              onClick={handleGuardarProducto}
              className="btn-primary text-sm !min-h-[42px] px-6 flex items-center gap-2"
              disabled={isSaving}
            >
              {isSaving ? (
                <span className="animate-pulse">Guardando...</span>
              ) : (
                <>
                  <Check size={16} />
                  <span>{esEdicion ? 'Guardar Cambios' : 'Crear Producto'}</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
