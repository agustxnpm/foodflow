import { useState, useEffect } from 'react';
import useIdentityStore from '../store/useIdentityStore';

type EstadoLicencia = 'ACTIVO' | 'POR_VENCER' | 'BLOQUEADO';

interface Licencia {
  estado: EstadoLicencia;
  diasRestantes: number | null;
  cargando: boolean;
}

interface LicenciaGist {
  cliente: string;
  estado: string;
  validoHasta: string;
}

/**
 * Hook que valida la licencia del local contra un GitHub Gist remoto.
 *
 * - Lee el localId dinámico desde el store de identidad.
 * - Si no hay localId (app no activada), retorna null sin hacer fetch.
 * - Consulta el Gist al montar el componente.
 * - Compara validoHasta con la fecha actual.
 * - Devuelve ACTIVO, POR_VENCER (≤ 5 días) o BLOQUEADO.
 * - Fail-Open: si no hay red o el fetch falla, retorna ACTIVO
 *   para no frenar la operatoria del local.
 */
export function useLicencia(): Licencia | null {
  const localId = useIdentityStore((s) => s.localId);
  const [licencia, setLicencia] = useState<Licencia>({
    estado: 'ACTIVO',
    diasRestantes: null,
    cargando: true,
  });

  useEffect(() => {
    if (!localId) {
      setLicencia({ estado: 'ACTIVO', diasRestantes: null, cargando: false });
      return;
    }

    const validar = async () => {
      try {
        const gistUrl = import.meta.env.VITE_LICENSE_GIST_URL;

        if (!gistUrl) {
          // Sin configuración → Fail-Open
          setLicencia({ estado: 'ACTIVO', diasRestantes: null, cargando: false });
          return;
        }

        // Cache-bust para evitar respuestas cacheadas por el CDN de GitHub
        const url = `${gistUrl}?t=${Date.now()}`;
        const res = await fetch(url, { cache: 'no-store' });

        if (!res.ok) {
          // Error HTTP → Fail-Open
          setLicencia({ estado: 'ACTIVO', diasRestantes: null, cargando: false });
          return;
        }

        const data: Record<string, LicenciaGist> = await res.json();
        const entrada = data[localId];

        if (!entrada) {
          // Local no encontrado en el Gist → Fail-Open
          setLicencia({ estado: 'ACTIVO', diasRestantes: null, cargando: false });
          return;
        }

        // Bloqueo manual desde el Gist (estado != ACTIVO)
        if (entrada.estado !== 'ACTIVO') {
          setLicencia({ estado: 'BLOQUEADO', diasRestantes: 0, cargando: false });
          return;
        }

        // Calcular días restantes
        const hoy = new Date();
        hoy.setHours(0, 0, 0, 0);
        const vencimiento = new Date(entrada.validoHasta + 'T00:00:00');
        const diffMs = vencimiento.getTime() - hoy.getTime();
        const diasRestantes = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

        let estado: EstadoLicencia;
        if (diasRestantes < 0) {
          estado = 'BLOQUEADO';
        } else if (diasRestantes <= 5) {
          estado = 'POR_VENCER';
        } else {
          estado = 'ACTIVO';
        }

        setLicencia({ estado, diasRestantes, cargando: false });
      } catch {
        // Error de red, timeout, JSON inválido → Fail-Open
        setLicencia({ estado: 'ACTIVO', diasRestantes: null, cargando: false });
      }
    };

    validar();
  }, [localId]);

  if (!localId) return null;

  return licencia;
}
