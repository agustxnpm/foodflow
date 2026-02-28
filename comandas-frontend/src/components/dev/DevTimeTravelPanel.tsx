/**
 * Panel flotante de Time-Travel para desarrollo.
 *
 * Permite manipular el reloj del backend sin tocar el OS.
 * Solo se renderiza en modo desarrollo (import.meta.env.DEV).
 *
 * Funcionalidades:
 *   - Fijar hora específica (fecha + hora)
 *   - Avanzar N horas con un click
 *   - Volver al tiempo real
 *   - Ver la hora actual del sistema vs. hora real
 *
 * El panel es colapsable y se posiciona en la esquina inferior izquierda
 * para no interferir con la UI operativa.
 */

import { useState, useEffect, useCallback } from 'react';
import { Clock, FastForward, RotateCcw, ChevronDown, ChevronUp, AlertTriangle } from 'lucide-react';
import apiClient from '../../lib/apiClient';
import { setDevDateOverride } from '../../lib/devClock';

interface ClockStatus {
  horaDelSistema: string;
  horaReal: string;
  overrideActivo: boolean;
  fechaOperativa: string;
}

/**
 * Comunica con los endpoints /api/dev/clock del backend.
 */
const devClockApi = {
  getStatus: async (): Promise<ClockStatus> => {
    const { data } = await apiClient.get('/dev/clock');
    return data;
  },

  setTime: async (dateTime: string): Promise<ClockStatus> => {
    const { data } = await apiClient.post('/dev/clock', { dateTime });
    return data;
  },

  advance: async (hours: number, minutes = 0): Promise<ClockStatus> => {
    const { data } = await apiClient.post('/dev/clock/advance', { hours, minutes });
    return data;
  },

  reset: async (): Promise<ClockStatus> => {
    const { data } = await apiClient.delete('/dev/clock');
    return data;
  },
};

export default function DevTimeTravelPanel() {
  const [expanded, setExpanded] = useState(false);
  const [status, setStatus] = useState<ClockStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [customDate, setCustomDate] = useState('');
  const [customTime, setCustomTime] = useState('');
  const [available, setAvailable] = useState(true);

  // Polling del estado cada 5s cuando está expandido
  const fetchStatus = useCallback(async () => {
    try {
      const data = await devClockApi.getStatus();
      setStatus(data);
      setError(null);
      setAvailable(true);
      // Sincronizar fecha global para que CajaPage use la fecha correcta
      setDevDateOverride(data.overrideActivo ? data.fechaOperativa : null);
    } catch {
      // El endpoint no existe → backend no corriendo con profile "dev"
      setAvailable(false);
      setDevDateOverride(null);
    }
  }, []);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  useEffect(() => {
    if (!expanded || !available) return;
    const interval = setInterval(fetchStatus, 5000);
    return () => clearInterval(interval);
  }, [expanded, available, fetchStatus]);

  // Si el endpoint no está disponible, no renderizar
  if (!available) return null;

  const handleSetTime = async () => {
    if (!customDate || !customTime) {
      setError('Completar fecha y hora');
      return;
    }
    try {
      const dateTime = `${customDate}T${customTime}:00`;
      await devClockApi.setTime(dateTime);
      await fetchStatus();
      setError(null);
    } catch {
      setError('Error al fijar la hora');
    }
  };

  const handleAdvance = async (hours: number) => {
    try {
      await devClockApi.advance(hours);
      await fetchStatus();
      setError(null);
    } catch {
      setError('Error al avanzar');
    }
  };

  const handleReset = async () => {
    try {
      await devClockApi.reset();
      await fetchStatus();
      setError(null);
      setCustomDate('');
      setCustomTime('');
    } catch {
      setError('Error al resetear');
    }
  };

  return (
    <div className="fixed bottom-4 left-4 z-[9999] select-none">
      {/* ── Botón colapsado ── */}
      {!expanded && (
        <button
          onClick={() => { setExpanded(true); fetchStatus(); }}
          className={`flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-mono transition-all shadow-lg ${
            status?.overrideActivo
              ? 'bg-amber-600/90 text-amber-50 hover:bg-amber-600 ring-1 ring-amber-400/50'
              : 'bg-neutral-800/90 text-gray-400 hover:bg-neutral-700 hover:text-gray-200 ring-1 ring-neutral-700/50'
          }`}
        >
          <Clock size={14} />
          <span>
            {status?.overrideActivo
              ? `⏰ ${status.horaDelSistema}`
              : 'Time Travel'}
          </span>
          <ChevronUp size={12} />
        </button>
      )}

      {/* ── Panel expandido ── */}
      {expanded && (
        <div className="w-80 bg-neutral-850 border border-neutral-700 rounded-xl shadow-2xl overflow-hidden"
             style={{ backgroundColor: '#1a1a1a' }}>
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-2.5 border-b border-neutral-700/50">
            <div className="flex items-center gap-2">
              <Clock size={14} className="text-amber-500" />
              <span className="text-xs font-semibold text-gray-300 uppercase tracking-wider">
                Time Travel
              </span>
              {status?.overrideActivo && (
                <span className="px-1.5 py-0.5 text-[10px] bg-amber-600/30 text-amber-400 rounded font-mono">
                  OVERRIDE
                </span>
              )}
            </div>
            <button
              onClick={() => setExpanded(false)}
              className="text-gray-500 hover:text-gray-300 transition-colors"
            >
              <ChevronDown size={16} />
            </button>
          </div>

          <div className="p-4 space-y-3">
            {/* Estado actual */}
            {status && (
              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="bg-neutral-800/80 rounded-lg p-2">
                  <span className="text-gray-500 block mb-0.5">Sistema</span>
                  <span className={`font-mono ${status.overrideActivo ? 'text-amber-400' : 'text-gray-200'}`}>
                    {status.horaDelSistema}
                  </span>
                </div>
                <div className="bg-neutral-800/80 rounded-lg p-2">
                  <span className="text-gray-500 block mb-0.5">Real</span>
                  <span className="font-mono text-gray-400">{status.horaReal}</span>
                </div>
              </div>
            )}

            {/* Warning si override activo */}
            {status?.overrideActivo && (
              <div className="flex items-center gap-2 px-3 py-2 bg-amber-600/10 border border-amber-600/20 rounded-lg text-amber-400 text-[11px]">
                <AlertTriangle size={12} className="shrink-0" />
                <span>El backend NO usa la hora real. Recordá resetear al terminar.</span>
              </div>
            )}

            {/* Fijar hora específica */}
            <div className="space-y-2">
              <label className="text-[10px] text-gray-500 uppercase tracking-wider font-medium">
                Fijar fecha y hora
              </label>
              <div className="flex gap-2">
                <input
                  type="date"
                  value={customDate}
                  onChange={(e) => setCustomDate(e.target.value)}
                  className="flex-1 bg-neutral-800 border border-neutral-700 rounded-lg px-2 py-1.5 text-xs text-gray-200 focus:outline-none focus:ring-1 focus:ring-amber-500/50"
                />
                <input
                  type="time"
                  value={customTime}
                  onChange={(e) => setCustomTime(e.target.value)}
                  className="w-24 bg-neutral-800 border border-neutral-700 rounded-lg px-2 py-1.5 text-xs text-gray-200 focus:outline-none focus:ring-1 focus:ring-amber-500/50"
                />
              </div>
              <button
                onClick={handleSetTime}
                className="w-full py-1.5 bg-amber-600/20 hover:bg-amber-600/30 text-amber-400 text-xs font-medium rounded-lg transition-colors"
              >
                Aplicar
              </button>
            </div>

            {/* Avance rápido */}
            <div className="space-y-2">
              <label className="text-[10px] text-gray-500 uppercase tracking-wider font-medium">
                Avance rápido
              </label>
              <div className="grid grid-cols-4 gap-1.5">
                {[1, 2, 4, 8].map((h) => (
                  <button
                    key={h}
                    onClick={() => handleAdvance(h)}
                    className="flex items-center justify-center gap-1 py-1.5 bg-neutral-800 hover:bg-neutral-700 text-gray-300 text-xs rounded-lg transition-colors"
                  >
                    <FastForward size={10} />
                    +{h}h
                  </button>
                ))}
              </div>
              <div className="grid grid-cols-4 gap-1.5">
                {[12, 24, 48, 72].map((h) => (
                  <button
                    key={h}
                    onClick={() => handleAdvance(h)}
                    className="flex items-center justify-center gap-1 py-1.5 bg-neutral-800 hover:bg-neutral-700 text-gray-300 text-xs rounded-lg transition-colors"
                  >
                    <FastForward size={10} />
                    +{h}h
                  </button>
                ))}
              </div>
            </div>

            {/* Reset */}
            <button
              onClick={handleReset}
              className="w-full flex items-center justify-center gap-2 py-2 bg-red-600/15 hover:bg-red-600/25 text-red-400 text-xs font-medium rounded-lg transition-colors"
            >
              <RotateCcw size={12} />
              Volver al tiempo real
            </button>

            {/* Error */}
            {error && (
              <p className="text-[11px] text-red-400 text-center">{error}</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
