import { Download, Loader2 } from 'lucide-react';
import { useDescargarReportePdf } from '../hooks/useCaja';

interface BotonDescargarPDFProps {
  jornadaId: string;
  /** Texto alternativo. Por defecto: "Descargar PDF" */
  label?: string;
  /** Variante compacta (solo icono) para tablas */
  compacto?: boolean;
}

/**
 * Botón reutilizable para descargar el reporte PDF de una jornada cerrada.
 *
 * Usado en el historial de jornadas y cualquier vista que muestre
 * jornadas pasadas con opción de re-descarga.
 *
 * @example
 * <BotonDescargarPDF jornadaId={jornada.id} />
 * <BotonDescargarPDF jornadaId={jornada.id} compacto />
 */
export default function BotonDescargarPDF({
  jornadaId,
  label = 'Descargar PDF',
  compacto = false,
}: BotonDescargarPDFProps) {
  const { mutate: descargar, isPending } = useDescargarReportePdf();

  const handleClick = () => {
    if (!isPending) {
      descargar(jornadaId);
    }
  };

  if (compacto) {
    return (
      <button
        type="button"
        onClick={handleClick}
        disabled={isPending}
        title={label}
        className="inline-flex items-center justify-center rounded-md p-2
                   text-neutral-400 hover:text-red-500 hover:bg-neutral-800
                   disabled:opacity-50 disabled:cursor-not-allowed
                   transition-colors"
      >
        {isPending ? (
          <Loader2 className="h-4 w-4 animate-spin" />
        ) : (
          <Download className="h-4 w-4" />
        )}
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={isPending}
      className="inline-flex items-center gap-2 rounded-lg px-4 py-2
                 bg-neutral-800 text-neutral-100 text-sm font-medium
                 hover:bg-neutral-700 active:bg-neutral-600
                 disabled:opacity-50 disabled:cursor-not-allowed
                 transition-colors"
    >
      {isPending ? (
        <Loader2 className="h-4 w-4 animate-spin" />
      ) : (
        <Download className="h-4 w-4" />
      )}
      {label}
    </button>
  );
}
