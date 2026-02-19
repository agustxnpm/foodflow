/**
 * Módulo Mostrador — Placeholder
 *
 * Futuro módulo para pedidos desde mostrador (take-away / delivery).
 * Por ahora solo muestra un aviso de "Próximamente".
 */
export default function MostradorPage() {
  return (
    <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center bg-neutral-900">
      <div className="text-center space-y-3">
        <h1 className="text-3xl font-bold text-gray-100">Mostrador</h1>
        <p className="text-gray-400">Próximamente</p>
      </div>
    </div>
  );
}
