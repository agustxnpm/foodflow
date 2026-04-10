import { useState } from 'react';
import { Loader2, ShieldCheck } from 'lucide-react';
import apiClient from '../../lib/apiClient';
import type { AxiosError } from 'axios';

interface FormData {
  localId: string;
  nombreLocal: string;
  direccion: string;
  telefono: string;
  mensajeBienvenida: string;
}

const INITIAL_FORM: FormData = {
  localId: '',
  nombreLocal: '',
  direccion: '',
  telefono: '',
  mensajeBienvenida: '',
};

/**
 * Pantalla de primer arranque — Activación del sistema.
 *
 * Se muestra cuando GET /api/config/identity devuelve { configured: false }.
 * Sin navbar, sin sidebars. Centrada, paleta roja/negra, touch-friendly.
 */
export default function PantallaActivacion() {
  const [form, setForm] = useState<FormData>(INITIAL_FORM);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleChange = (field: keyof FormData) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
    if (error) setError(null);
  };

  const puedeEnviar =
    form.localId.trim().length > 0 && form.nombreLocal.trim().length > 0;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!puedeEnviar || enviando) return;

    setEnviando(true);
    setError(null);

    try {
      await apiClient.post('/config/identity', {
        localId: form.localId.trim(),
        nombreLocal: form.nombreLocal.trim(),
        direccion: form.direccion.trim() || null,
        telefono: form.telefono.trim() || null,
        cuit: null,
        mensajeBienvenida: form.mensajeBienvenida.trim() || null,
      });

      // Activación exitosa → recargar la app para que el flujo normal arranque
      window.location.reload();
    } catch (err) {
      const axiosErr = err as AxiosError<{ error?: string }>;

      if (axiosErr.response) {
        const status = axiosErr.response.status;
        const msg = axiosErr.response.data?.error;

        if (status === 400) {
          setError(msg || 'Código de activación inválido.');
        } else if (status === 409) {
          setError(msg || 'El sistema ya fue activado con otro código.');
        } else {
          setError(`Error del servidor (${status}).`);
        }
      } else {
        setError('Error de conexión. Verificar que el sistema esté encendido.');
      }

      setEnviando(false);
    }
  };

  return (
    <div className="min-h-screen bg-neutral-950 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Branding */}
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-red-600 mb-3 shadow-lg shadow-red-600/20">
            <ShieldCheck size={28} className="text-white" />
          </div>
          <h1 className="text-xl font-semibold text-white">FoodFlow</h1>
          <p className="text-neutral-400 text-sm mt-1">
            Activá tu sistema en menos de 1 minuto
          </p>
        </div>

        {/* Formulario */}
        <form onSubmit={handleSubmit} className="space-y-3">
          {/* Código de activación */}
          <div>
            <input
              type="text"
              value={form.localId}
              onChange={handleChange('localId')}
              placeholder="Código de activación"
              className="w-full px-4 py-3 rounded-xl bg-neutral-800 border border-neutral-700
                         text-white placeholder-neutral-500 text-base
                         focus:outline-none focus:ring-2 focus:ring-red-500"
              autoFocus
              disabled={enviando}
            />
          </div>

          {/* Nombre del local */}
          <div>
            <input
              type="text"
              value={form.nombreLocal}
              onChange={handleChange('nombreLocal')}
              placeholder="Nombre del local"
              className="w-full px-4 py-3 rounded-xl bg-neutral-800 border border-neutral-700
                         text-white placeholder-neutral-500 text-base
                         focus:outline-none focus:ring-2 focus:ring-red-500"
              disabled={enviando}
            />
          </div>

          {/* Opcionales */}
          <div className="pt-2">
            <p className="text-xs text-neutral-500 mb-2">
              Opcional (podés completarlo después)
            </p>
          </div>

          <input
            type="text"
            value={form.direccion}
            onChange={handleChange('direccion')}
            placeholder="Dirección"
            className="w-full px-4 py-3 rounded-xl bg-neutral-800 border border-neutral-700
                       text-white placeholder-neutral-500"
            disabled={enviando}
          />

          <input
            type="text"
            value={form.telefono}
            onChange={handleChange('telefono')}
            placeholder="Teléfono"
            className="w-full px-4 py-3 rounded-xl bg-neutral-800 border border-neutral-700
                       text-white placeholder-neutral-500"
            disabled={enviando}
          />

          <input
            type="text"
            value={form.mensajeBienvenida}
            onChange={handleChange('mensajeBienvenida')}
            placeholder="Mensaje de bienvenida"
            className="w-full px-4 py-3 rounded-xl bg-neutral-800 border border-neutral-700
                       text-white placeholder-neutral-500"
            disabled={enviando}
          />

          {/* Error */}
          {error && (
            <div className="px-4 py-3 rounded-xl bg-red-900/40 border border-red-700 text-red-300 text-sm animate-pulse">
              {error}
            </div>
          )}

          {/* Botón activar */}
          <button
            type="submit"
            disabled={!puedeEnviar || enviando}
            className="w-full py-4 rounded-xl font-semibold text-base transition-all
                       bg-red-600 text-white hover:bg-red-700 active:scale-[0.98]
                       disabled:bg-neutral-700 disabled:text-neutral-500"
          >
            {enviando ? (
              <span className="inline-flex items-center gap-2">
                <Loader2 size={18} className="animate-spin" />
                Activando...
              </span>
            ) : (
              'Activar sistema'
            )}
          </button>
        </form>

        <p className="text-center text-neutral-600 text-xs mt-5">
          ¿No tenés código? Contactá al proveedor
        </p>
      </div>
    </div>
  );
}
