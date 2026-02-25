import { useState } from 'react';
import {
  X,
  Plus,
  Pencil,
  Trash2,
  Check,
  Palette,
  AlertTriangle,
  ArrowLeft,
} from 'lucide-react';
import {
  useCategorias,
  useCrearCategoria,
  useEditarCategoria,
  useEliminarCategoria,
} from '../../categorias/hooks/useCategorias';
import type { CategoriaResponse } from '../../categorias/types';

// ─── Colores predefinidos ──────────────────────────────────────────────────────

/**
 * Paleta de colores para elegir al crear/editar una categoría.
 *
 * Nota: si un color ya está en uso, se muestra deshabilitado.
 */
const PALETA_COLORES: { hex: string; display: string; label: string }[] = [
  { hex: '#FF0000', display: '#FF0000', label: 'Rojo' },
  { hex: '#0000FF', display: '#3B82F6', label: 'Azul' },
  { hex: '#00FF00', display: '#22C55E', label: 'Verde' },
  { hex: '#FFA500', display: '#F97316', label: 'Naranja' },
  { hex: '#800080', display: '#A855F7', label: 'Violeta' },
  { hex: '#FFFF00', display: '#EAB308', label: 'Amarillo' },
  { hex: '#FF69B4', display: '#EC4899', label: 'Rosa' },
  { hex: '#00CED1', display: '#06B6D4', label: 'Cyan' },
  { hex: '#8B4513', display: '#A16207', label: 'Marrón' },
  { hex: '#708090', display: '#64748B', label: 'Gris' },
  { hex: '#FF6347', display: '#EF4444', label: 'Tomate' },
  { hex: '#2E8B57', display: '#059669', label: 'Esmeralda' },
];

// ─── Tipos internos ────────────────────────────────────────────────────────────

interface CategoriasModalProps {
  onClose: () => void;
}

type ModoFormulario = 'lista' | 'crear' | 'editar';

interface FormState {
  nombre: string;
  colorHex: string;
  admiteVariantes: boolean;
  esCategoriaExtra: boolean;
}

const FORM_VACIO: FormState = {
  nombre: '',
  colorHex: '',
  admiteVariantes: false,
  esCategoriaExtra: false,
};

// ─── Componente ────────────────────────────────────────────────────────────────

/**
 * Modal para gestionar las categorías visuales.
 *
 * Permite:
 * - Ver todas las categorías con su color y cantidad de productos
 * - Crear nuevas categorías
 * - Editar nombre, color y flag esExtra
 * - Eliminar categorías (con confirmación)
 * - Reordenar arrastrando (drag visual simple con botones ▲▼)
 */
export default function CategoriasModal({ onClose }: CategoriasModalProps) {
  const { data: categorias = [] } = useCategorias();
  const crearCategoriaMutation = useCrearCategoria();
  const editarCategoriaMutation = useEditarCategoria();
  const eliminarCategoriaMutation = useEliminarCategoria();

  const [modo, setModo] = useState<ModoFormulario>('lista');
  const [editandoId, setEditandoId] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>(FORM_VACIO);
  const [error, setError] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [showCustomColor, setShowCustomColor] = useState(false);

  // ── Colores ya usados ──
  const coloresEnUso = new Set(
    categorias
      .filter((c) => c.id !== editandoId)
      .map((c) => c.colorHex.toUpperCase())
  );

  // ── Handlers ──

  const iniciarCrear = () => {
    setForm(FORM_VACIO);
    setError(null);
    setModo('crear');
  };

  const iniciarEditar = (cat: CategoriaResponse) => {
    setEditandoId(cat.id);
    setForm({
      nombre: cat.nombre,
      colorHex: cat.colorHex,
      admiteVariantes: cat.admiteVariantes,
      esCategoriaExtra: cat.esCategoriaExtra,
    });
    setError(null);
    setModo('editar');
  };

  const cancelarFormulario = () => {
    setModo('lista');
    setEditandoId(null);
    setForm(FORM_VACIO);
    setError(null);
    setShowCustomColor(false);
  };

  const handleGuardar = async () => {
    setError(null);

    if (!form.nombre.trim()) {
      setError('El nombre es obligatorio');
      return;
    }

    if (!form.colorHex) {
      setError('Seleccioná un color');
      return;
    }

    // Validar formato hex
    const hexRegex = /^#[0-9A-F]{6}$/i;
    if (!hexRegex.test(form.colorHex)) {
      setError('El color debe tener formato #RRGGBB (ej: #FF0000)');
      return;
    }

    // Validar unicidad del color
    if (coloresEnUso.has(form.colorHex.toUpperCase())) {
      setError('Ese color ya está asignado a otra categoría');
      return;
    }

    setSaving(true);
    try {
      if (modo === 'crear') {
        await crearCategoriaMutation.mutateAsync({
          nombre: form.nombre.trim(),
          colorHex: form.colorHex,
          admiteVariantes: form.admiteVariantes,
          esCategoriaExtra: form.esCategoriaExtra,
        });
      } else if (modo === 'editar' && editandoId) {
        await editarCategoriaMutation.mutateAsync({
          id: editandoId,
          nombre: form.nombre.trim(),
          colorHex: form.colorHex,
          admiteVariantes: form.admiteVariantes,
          esCategoriaExtra: form.esCategoriaExtra,
        });
      }
      cancelarFormulario();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error al guardar');
    } finally {
      setSaving(false);
    }
  };

  const handleEliminar = async (id: string) => {
    try {
      await eliminarCategoriaMutation.mutateAsync(id);
      setConfirmDelete(null);
    } catch (e) {
      console.error('Error al eliminar categoría:', e);
    }
  };

  // ── Seleccionar color de la paleta ──

  const seleccionarColor = (color: (typeof PALETA_COLORES)[number]) => {
    setForm((f) => ({
      ...f,
      colorHex: color.hex,
    }));
  };

  // ────────────────────────────────────────────────────────
  // RENDER: Formulario (crear / editar)
  // ────────────────────────────────────────────────────────

  const renderFormulario = () => (
    <div className="space-y-5">
      {/* Nombre */}
      <div className="flex flex-col gap-1">
        <label className="text-sm text-text-secondary">Nombre</label>
        <input
          type="text"
          value={form.nombre}
          onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))}
          placeholder="Ej: Hamburguesas, Bebidas..."
          className="min-h-[48px] px-4 bg-background-card border border-gray-700 rounded-lg text-text-primary focus:border-primary focus:outline-none"
          autoFocus
        />
      </div>

      {/* Selector de color */}
      <div className="space-y-3">
        <label className="text-sm text-text-secondary">Color</label>

        {/* Paleta predefinida */}
        <div className="grid grid-cols-4 gap-2">
          {PALETA_COLORES.map((color) => {
            const enUso = coloresEnUso.has(color.hex.toUpperCase());
            const seleccionado =
              form.colorHex.toUpperCase() === color.hex.toUpperCase() && !showCustomColor;

            return (
              <button
                key={color.hex}
                type="button"
                disabled={enUso}
                onClick={() => {
                  seleccionarColor(color);
                  setShowCustomColor(false);
                }}
                className={[
                  'flex items-center gap-2 px-2.5 py-2 rounded-lg border transition-all text-left',
                  enUso
                    ? 'opacity-30 cursor-not-allowed border-gray-800 bg-background-card'
                    : seleccionado
                      ? 'border-white/40 bg-white/10 text-white ring-1 ring-white/20'
                      : 'border-gray-700 bg-background-card text-gray-400 hover:border-gray-500 hover:text-gray-200',
                ].join(' ')}
                title={enUso ? `"${color.label}" ya está en uso` : color.label}
              >
                <span
                  className="w-4 h-4 rounded-full shrink-0 border border-white/10"
                  style={{ backgroundColor: color.display }}
                />
                <span className="text-xs font-medium truncate">{color.label}</span>
                {seleccionado && (
                  <Check size={12} className="ml-auto shrink-0 text-green-400" strokeWidth={3} />
                )}
              </button>
            );
          })}
        </div>

        {/* Toggle color personalizado */}
        {!showCustomColor ? (
          <button
            type="button"
            onClick={() => setShowCustomColor(true)}
            className="flex items-center gap-2 text-xs text-gray-500 hover:text-gray-300 transition-colors"
          >
            <Palette size={14} />
            <span>Elegir un color personalizado</span>
          </button>
        ) : (
          <div className="flex items-center gap-3 p-3 rounded-lg border border-gray-700 bg-background-card">
            {/* Color picker nativo */}
            <label className="relative cursor-pointer group" title="Abrir selector de color">
              <input
                type="color"
                value={form.colorHex || '#FF0000'}
                onChange={(e) => {
                  const hex = e.target.value.toUpperCase();
                  setForm((f) => ({ ...f, colorHex: hex }));
                }}
                className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
              />
              <span
                className="block w-10 h-10 rounded-lg border-2 border-gray-600 group-hover:border-gray-400 transition-colors"
                style={{ backgroundColor: form.colorHex || '#FF0000' }}
              />
            </label>

            {/* Input hex manual */}
            <div className="flex-1">
              <label className="text-[11px] text-gray-600 mb-0.5 block">Código HEX</label>
              <input
                type="text"
                value={form.colorHex}
                onChange={(e) => {
                  let val = e.target.value;
                  if (!val.startsWith('#')) val = '#' + val;
                  val = val.slice(0, 7).toUpperCase();
                  setForm((f) => ({ ...f, colorHex: val }));
                }}
                placeholder="#FF0000"
                maxLength={7}
                className="w-full px-3 py-2 bg-neutral-800 border border-gray-700 rounded-lg text-text-primary font-mono text-sm focus:border-primary focus:outline-none"
              />
            </div>

            {/* Preview + cerrar */}
            <button
              type="button"
              onClick={() => setShowCustomColor(false)}
              className="p-1.5 rounded-lg hover:bg-gray-700 text-gray-500 hover:text-gray-300 transition-colors self-start"
              title="Volver a la paleta"
            >
              <X size={14} />
            </button>
          </div>
        )}

        {/* Indicador de color seleccionado */}
        {form.colorHex && (
          <div className="flex items-center gap-2 text-xs text-gray-500">
            <span
              className="w-3 h-3 rounded-full border border-white/10"
              style={{ backgroundColor: form.colorHex }}
            />
            <span>Seleccionado: <span className="font-mono text-gray-400">{form.colorHex}</span></span>
            {coloresEnUso.has(form.colorHex.toUpperCase()) && (
              <span className="text-red-400 font-semibold">· Ya en uso</span>
            )}
          </div>
        )}
      </div>

      {/* Es Categoría Extra */}
      <label className="flex items-center gap-3 cursor-pointer select-none">
        <input
          type="checkbox"
          checked={form.esCategoriaExtra}
          onChange={(e) => setForm((f) => ({ ...f, esCategoriaExtra: e.target.checked }))}
          className="w-5 h-5 rounded border-gray-600 bg-background-card text-primary focus:ring-primary focus:ring-offset-0"
        />
        <span className="text-sm text-text-primary">Es un extra</span>
        <span className="text-xs text-text-secondary">(huevo, queso, medallón, etc.)</span>
      </label>

      {/* Admite Variantes */}
      <label className="flex items-center gap-3 cursor-pointer select-none">
        <input
          type="checkbox"
          checked={form.admiteVariantes}
          onChange={(e) => setForm((f) => ({ ...f, admiteVariantes: e.target.checked }))}
          className="w-5 h-5 rounded border-gray-600 bg-background-card text-primary focus:ring-primary focus:ring-offset-0"
        />
        <span className="text-sm text-text-primary">Admite variantes</span>
        <span className="text-xs text-text-secondary">(simple, doble, triple, etc.)</span>
      </label>

      {/* Error */}
      {error && (
        <p className="text-sm text-red-500 flex items-center gap-2">
          <AlertTriangle size={14} />
          {error}
        </p>
      )}
    </div>
  );

  // ────────────────────────────────────────────────────────
  // RENDER: Lista de categorías
  // ────────────────────────────────────────────────────────

  const renderLista = () => (
    <div className="space-y-2">
      {categorias.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-12 text-gray-500 gap-3">
          <Palette size={36} className="text-gray-600" />
          <p className="text-sm text-center">
            No hay categorías creadas.
            <br />
            <span className="text-gray-600">
              Las categorías agrupan tus productos visualmente.
            </span>
          </p>
        </div>
      ) : (
        categorias.map((cat) => (
          <div
            key={cat.id}
            className="flex items-center gap-3 px-3 py-2.5 rounded-xl border border-gray-800 bg-background-card hover:border-gray-700 transition-all group"
          >
            {/* Color dot */}
            <span
              className="w-5 h-5 rounded-full shrink-0 border border-white/10"
              style={{ backgroundColor: cat.colorHex }}
            />

            {/* Nombre + info */}
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold text-text-primary truncate">
                {cat.nombre}
              </p>
              <p className="text-[11px] text-gray-600 flex items-center gap-2">
                {cat.esCategoriaExtra && (
                  <span className="text-yellow-500/70">Extra</span>
                )}
                {cat.admiteVariantes && (
                  <span className="text-blue-400/70">Variantes</span>
                )}
                <span className="font-mono">{cat.colorHex}</span>
              </p>
            </div>

            {/* Acciones */}
            <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                onClick={() => iniciarEditar(cat)}
                className="p-2 rounded-lg hover:bg-gray-700 text-gray-500 hover:text-white transition-colors"
                title="Editar"
              >
                <Pencil size={14} />
              </button>

              {confirmDelete === cat.id ? (
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => handleEliminar(cat.id)}
                    className="px-2 py-1.5 rounded-lg bg-red-500/20 text-red-400 text-xs font-semibold hover:bg-red-500/30 transition-colors"
                  >
                    Sí, eliminar
                  </button>
                  <button
                    onClick={() => setConfirmDelete(null)}
                    className="p-1.5 rounded-lg hover:bg-gray-700 text-gray-500 transition-colors"
                  >
                    <X size={14} />
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setConfirmDelete(cat.id)}
                  className="p-2 rounded-lg hover:bg-red-500/10 text-gray-500 hover:text-red-400 transition-colors"
                  title="Eliminar"
                >
                  <Trash2 size={14} />
                </button>
              )}
            </div>
          </div>
        ))
      )}
    </div>
  );

  // ────────────────────────────────────────────────────────
  // RENDER PRINCIPAL
  // ────────────────────────────────────────────────────────

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
          className="bg-neutral-900 border border-gray-800 rounded-xl w-full max-w-lg pointer-events-auto animate-modal-in max-h-[85vh] flex flex-col"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-800 shrink-0">
            <div className="flex items-center gap-3">
              {modo !== 'lista' && (
                <button
                  onClick={cancelarFormulario}
                  className="p-1.5 rounded-lg hover:bg-gray-800 text-gray-400 hover:text-white transition-colors"
                  title="Volver a la lista"
                >
                  <ArrowLeft size={18} />
                </button>
              )}
              <div className="flex items-center gap-2">
                <Palette size={18} className="text-primary" />
                <h2 className="text-lg font-semibold text-text-primary">
                  {modo === 'lista'
                    ? 'Categorías'
                    : modo === 'crear'
                      ? 'Nueva Categoría'
                      : 'Editar Categoría'}
                </h2>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-2 rounded-lg hover:bg-gray-800 text-gray-400 transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          {/* Body (scrollable) */}
          <div className="px-6 py-5 overflow-y-auto flex-1 min-h-0">
            {modo === 'lista' ? renderLista() : renderFormulario()}
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between gap-3 px-6 py-4 border-t border-gray-800 shrink-0">
            {modo === 'lista' ? (
              <>
                <span className="text-xs text-gray-600">
                  {categorias.length} categoría{categorias.length !== 1 ? 's' : ''}
                </span>
                <div className="flex gap-3">
                  <button
                    onClick={onClose}
                    className="btn-secondary text-sm !min-h-[42px] px-5"
                  >
                    Cerrar
                  </button>
                  <button
                    onClick={iniciarCrear}
                    className="btn-primary text-sm !min-h-[42px] px-5 flex items-center gap-2"
                  >
                    <Plus size={16} />
                    <span>Nueva</span>
                  </button>
                </div>
              </>
            ) : (
              <>
                <div />
                <div className="flex gap-3">
                  <button
                    onClick={cancelarFormulario}
                    className="btn-secondary text-sm !min-h-[42px] px-5"
                    disabled={saving}
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={handleGuardar}
                    className="btn-primary text-sm !min-h-[42px] px-6 flex items-center gap-2"
                    disabled={saving}
                  >
                    {saving ? (
                      <span className="animate-pulse">Guardando...</span>
                    ) : (
                      <>
                        <Check size={16} />
                        <span>{modo === 'crear' ? 'Crear' : 'Guardar'}</span>
                      </>
                    )}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
