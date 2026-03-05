import { ShieldAlert } from 'lucide-react';

/**
 * Pantalla de bloqueo total por expiración del período de evaluación.
 *
 * Se renderiza cuando `EstadoCajaResponse.trialExpired === true`.
 * Cubre toda la pantalla sin opción de cierre ni navegación.
 *
 * Los datos del local permanecen intactos — solo se bloquea la operación
 * hasta que se active la licencia.
 */
export default function PantallaBloqueoLicencia() {
  return (
    <div className="fixed inset-0 z-[9999] bg-neutral-950 flex items-center justify-center p-6">
      <div className="max-w-md text-center space-y-6">
        {/* Ícono */}
        <div className="flex justify-center">
          <div className="rounded-full bg-red-950/60 p-5">
            <ShieldAlert size={64} className="text-red-500" strokeWidth={1.5} />
          </div>
        </div>

        {/* Título */}
        <h1 className="text-2xl font-bold text-white tracking-tight">
          Período de Evaluación Finalizado
        </h1>

        {/* Descripción */}
        <p className="text-neutral-400 text-base leading-relaxed">
          Gracias por probar FoodFlow. Para activar tu licencia permanente
          y continuar operando con todos tus datos intactos, por favor
          contactá al soporte técnico.
        </p>

        {/* Datos de contacto */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-lg p-4 space-y-2">
          <p className="text-neutral-300 text-sm font-medium">Soporte Técnico</p>
          <p className="text-white text-lg font-semibold tracking-wide">
            +54 9 2804510876
          </p>
        </div>

        {/* Nota de seguridad */}
        <p className="text-neutral-600 text-xs">
          Tus datos están seguros y se conservarán tras la activación.
        </p>
      </div>
    </div>
  );
}
