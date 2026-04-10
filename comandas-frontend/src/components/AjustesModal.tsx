import { useState, useEffect } from 'react';
import { X, Printer, Check, RefreshCw, Store, Loader2 } from 'lucide-react';
import { useImpresoras, useImpresoraPredeterminada, useGuardarImpresoraPredeterminada } from '../hooks/useConfiguracion';
import useToast from '../hooks/useToast';
import apiClient from '../lib/apiClient';
import type { IdentityResponse } from '../hooks/useIdentidad';

interface AjustesModalProps {
  onClose: () => void;
}

/**
 * Modal de Ajustes del sistema.
 *
 * Sección principal: Configuración de impresora.
 * - Lista impresoras detectadas por el SO (vía backend javax.print)
 * - Permite seleccionar y guardar la impresora predeterminada
 */
export default function AjustesModal({ onClose }: AjustesModalProps) {
  const toast = useToast();
  const { data: impresoras = [], isLoading: cargandoImpresoras, refetch: recargarImpresoras } = useImpresoras();
  const { data: impresoraActual = '' } = useImpresoraPredeterminada();
  const guardarImpresora = useGuardarImpresoraPredeterminada();

  const [seleccion, setSeleccion] = useState('');

  // Sincronizar selección con la impresora guardada
  useEffect(() => {
    if (impresoraActual) {
      setSeleccion(impresoraActual);
    }
  }, [impresoraActual]);

  // ── Datos del local ──
  const [datosLocal, setDatosLocal] = useState({
    nombreLocal: '',
    direccion: '',
    telefono: '',
    mensajeBienvenida: '',
  });
  const [cargandoDatos, setCargandoDatos] = useState(true);
  const [guardandoDatos, setGuardandoDatos] = useState(false);

  useEffect(() => {
    apiClient.get<IdentityResponse>('/config/identity')
      .then(({ data }) => {
        if (data.configured) {
          setDatosLocal({
            nombreLocal: data.nombreLocal ?? '',
            direccion: data.direccion ?? '',
            telefono: data.telefono ?? '',
            mensajeBienvenida: data.mensajeBienvenida ?? '',
          });
        }
      })
      .catch(() => {
        toast.error('No se pudieron cargar los datos del local');
      })
      .finally(() => setCargandoDatos(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleDatoChange = (field: keyof typeof datosLocal) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setDatosLocal((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const handleGuardarDatos = () => {
    if (!datosLocal.nombreLocal.trim()) {
      toast.warning('El nombre del local es obligatorio');
      return;
    }

    setGuardandoDatos(true);
    apiClient.put('/config/identity', {
      nombreLocal: datosLocal.nombreLocal.trim(),
      direccion: datosLocal.direccion.trim() || null,
      telefono: datosLocal.telefono.trim() || null,
      cuit: null,
      mensajeBienvenida: datosLocal.mensajeBienvenida.trim() || null,
    })
      .then(() => {
        toast.success('Datos del local actualizados');
      })
      .catch(() => {
        toast.error('Error al guardar los datos del local');
      })
      .finally(() => setGuardandoDatos(false));
  };

  const handleGuardar = () => {
    if (!seleccion) {
      toast.warning('Seleccioná una impresora antes de guardar');
      return;
    }

    guardarImpresora.mutate(seleccion, {
      onSuccess: () => {
        toast.success(`Impresora guardada: ${seleccion}`);
        onClose();
      },
      onError: () => {
        toast.error('Error al guardar la impresora');
      },
    });
  };

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
          className="bg-neutral-900 border border-gray-800 rounded-xl w-full max-w-lg pointer-events-auto animate-modal-in"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-800">
            <h2 className="text-lg font-semibold text-text-primary">Ajustes</h2>
            <button
              onClick={onClose}
              className="p-2 rounded-lg hover:bg-gray-800 text-gray-400 transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          {/* Body */}
          <div className="px-6 py-5 space-y-6 max-h-[70vh] overflow-y-auto">
            {/* Sección: Datos del local */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Store size={18} className="text-gray-400" />
                <h3 className="text-sm font-semibold text-text-primary uppercase tracking-wider">
                  Datos del local
                </h3>
              </div>

              {cargandoDatos ? (
                <p className="text-sm text-gray-500">Cargando datos...</p>
              ) : (
                <div className="space-y-2.5">
                  <input
                    type="text"
                    value={datosLocal.nombreLocal}
                    onChange={handleDatoChange('nombreLocal')}
                    placeholder="Nombre del local *"
                    className="w-full px-3 py-2.5 bg-background-card border border-gray-700 rounded-lg text-text-primary placeholder-gray-500 text-sm focus:border-red-500 focus:outline-none"
                  />
                  <input
                    type="text"
                    value={datosLocal.direccion}
                    onChange={handleDatoChange('direccion')}
                    placeholder="Dirección"
                    className="w-full px-3 py-2.5 bg-background-card border border-gray-700 rounded-lg text-text-primary placeholder-gray-500 text-sm focus:border-red-500 focus:outline-none"
                  />
                  <div className="grid grid-cols-2 gap-2.5">
                    <input
                      type="text"
                      value={datosLocal.telefono}
                      onChange={handleDatoChange('telefono')}
                      placeholder="Teléfono"
                      className="w-full px-3 py-2.5 bg-background-card border border-gray-700 rounded-lg text-text-primary placeholder-gray-500 text-sm focus:border-red-500 focus:outline-none"
                    />
                    <input
                      type="text"
                      value={datosLocal.mensajeBienvenida}
                      onChange={handleDatoChange('mensajeBienvenida')}
                      placeholder="Mensaje de bienvenida"
                      className="w-full px-3 py-2.5 bg-background-card border border-gray-700 rounded-lg text-text-primary placeholder-gray-500 text-sm focus:border-red-500 focus:outline-none"
                    />
                  </div>
                  <button
                    onClick={handleGuardarDatos}
                    disabled={guardandoDatos || !datosLocal.nombreLocal.trim()}
                    className="flex items-center gap-2 px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-sm font-medium transition-colors"
                  >
                    {guardandoDatos ? (
                      <Loader2 size={14} className="animate-spin" />
                    ) : (
                      <Check size={14} />
                    )}
                    {guardandoDatos ? 'Guardando...' : 'Guardar datos'}
                  </button>
                </div>
              )}
            </div>

            {/* Separador */}
            <div className="border-t border-gray-800" />

            {/* Sección: Impresora */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Printer size={18} className="text-gray-400" />
                  <h3 className="text-sm font-semibold text-text-primary uppercase tracking-wider">
                    Impresora térmica
                  </h3>
                </div>
                <button
                  onClick={() => recargarImpresoras()}
                  disabled={cargandoImpresoras}
                  className="p-1.5 rounded-lg hover:bg-gray-800 text-gray-400 transition-colors disabled:opacity-50"
                  title="Recargar impresoras"
                >
                  <RefreshCw size={14} className={cargandoImpresoras ? 'animate-spin' : ''} />
                </button>
              </div>

              {cargandoImpresoras ? (
                <p className="text-sm text-gray-500">Buscando impresoras...</p>
              ) : impresoras.length === 0 ? (
                <div className="bg-yellow-500/10 border border-yellow-500/20 rounded-lg px-4 py-3">
                  <p className="text-sm text-yellow-400">
                    No se detectaron impresoras instaladas en el sistema.
                  </p>
                  <p className="text-xs text-yellow-500/70 mt-1">
                    Verificá que la impresora esté encendida y conectada, y que los drivers estén instalados.
                  </p>
                </div>
              ) : (
                <select
                  value={seleccion}
                  onChange={(e) => setSeleccion(e.target.value)}
                  className="w-full min-h-[48px] px-4 bg-background-card border border-gray-700 rounded-lg text-text-primary focus:border-primary focus:outline-none appearance-none cursor-pointer"
                >
                  <option value="">Seleccionar impresora...</option>
                  {impresoras.map((nombre) => (
                    <option key={nombre} value={nombre}>
                      {nombre}
                    </option>
                  ))}
                </select>
              )}

              {impresoraActual && (
                <p className="text-xs text-gray-500">
                  Configurada actualmente: <span className="text-gray-300">{impresoraActual}</span>
                </p>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-800">
            <button
              onClick={onClose}
              className="px-4 py-2.5 rounded-lg border border-gray-700 text-gray-400 hover:text-gray-200 hover:border-gray-500 transition-colors text-sm font-medium"
            >
              Cancelar
            </button>
            <button
              onClick={handleGuardar}
              disabled={guardarImpresora.isPending || !seleccion || impresoras.length === 0}
              className="flex items-center gap-2 px-5 py-2.5 rounded-lg bg-red-600 hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed text-white text-sm font-medium transition-colors"
            >
              <Check size={16} />
              {guardarImpresora.isPending ? 'Guardando...' : 'Guardar'}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
