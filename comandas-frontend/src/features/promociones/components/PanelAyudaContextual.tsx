import { useState } from 'react';
import {
  HelpCircle,
  X,
  Lightbulb,
  Calculator,
  ShoppingBag,
  CalendarCheck,
} from 'lucide-react';
import type { TipoEstrategia } from '../types';

// ── Tipos ──

interface PanelAyudaContextualProps {
  /** Paso actual del wizard (1–4) */
  pasoActual: number;
  /** Estrategia seleccionada en el paso 2 (afecta ayuda de pasos 2 y 3) */
  estrategiaSeleccionada: TipoEstrategia;
}

// ── Iconos por paso ──

const ICON_POR_PASO = [
  Lightbulb,    // Paso 1
  Calculator,   // Paso 2
  ShoppingBag,  // Paso 3
  CalendarCheck, // Paso 4
];

// ── Contenido dinámico ──

function ContenidoPaso1() {
  return (
    <div className="space-y-4">
      <div>
        <h4 className="text-sm font-semibold text-text-primary mb-2">💡 Consejos para el nombre</h4>
        <ul className="space-y-2 text-xs text-gray-400 leading-relaxed">
          <li className="flex gap-2">
            <span className="text-green-400 shrink-0">✓</span>
            <span>Usá nombres claros: <strong className="text-gray-300">"Happy Hour Cervezas"</strong>, <strong className="text-gray-300">"2x1 Empanadas Viernes"</strong></span>
          </li>
          <li className="flex gap-2">
            <span className="text-green-400 shrink-0">✓</span>
            <span>Incluí el día o el beneficio en el nombre para encontrarla rápido.</span>
          </li>
          <li className="flex gap-2">
            <span className="text-red-400 shrink-0">✗</span>
            <span>Evitá nombres genéricos como <strong className="text-gray-300">"Promo 1"</strong> o <strong className="text-gray-300">"Descuento"</strong>.</span>
          </li>
        </ul>
      </div>

      <div className="border-t border-gray-800 pt-3">
        <h4 className="text-sm font-semibold text-text-primary mb-2">🔢 ¿Qué es la prioridad?</h4>
        <p className="text-xs text-gray-400 leading-relaxed">
          Si un mismo producto está en <strong className="text-gray-300">dos promociones</strong> al mismo tiempo,
          se aplica la que tenga <strong className="text-gray-300">mayor número</strong> de prioridad.
        </p>
        <div className="mt-2 p-2.5 bg-gray-800/40 rounded-lg">
          <p className="text-[11px] text-gray-500 mb-1.5">Ejemplo:</p>
          <div className="space-y-1 text-xs">
            <div className="flex justify-between">
              <span className="text-gray-300">"2×1 Cervezas"</span>
              <span className="text-green-400 font-mono">Prio 10 ← gana</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">"10% OFF Bebidas"</span>
              <span className="text-gray-500 font-mono">Prio 5</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function ContenidoPaso2({ estrategia }: { estrategia: TipoEstrategia }) {
  return (
    <div className="space-y-4">
      <div>
        <h4 className="text-sm font-semibold text-text-primary mb-2">📊 ¿Cómo se calcula?</h4>
        {estrategia === 'DESCUENTO_DIRECTO' && (
          <div className="space-y-3">
            <p className="text-xs text-gray-400 leading-relaxed">
              Un descuento simple que se aplica directamente sobre el precio del producto.
            </p>
            <div className="p-2.5 bg-gray-800/40 rounded-lg space-y-2">
              <p className="text-[11px] text-gray-500">Ejemplo: 20% en empanadas ($2.000 c/u)</p>
              <div className="text-xs space-y-0.5">
                <p className="text-gray-400">Pedido: 3 empanadas</p>
                <p className="text-gray-400">Subtotal: $6.000</p>
                <p className="text-green-400 font-medium">Descuento: −$1.200 (20%)</p>
                <p className="text-text-primary font-bold">Total: $4.800</p>
              </div>
            </div>
            <div className="p-2.5 bg-gray-800/40 rounded-lg space-y-2">
              <p className="text-[11px] text-gray-500">Ejemplo: $500 off en pizza ($5.000)</p>
              <div className="text-xs space-y-0.5">
                <p className="text-gray-400">Pedido: 2 pizzas</p>
                <p className="text-gray-400">Subtotal: $10.000</p>
                <p className="text-green-400 font-medium">Descuento: −$1.000 ($500 × 2)</p>
                <p className="text-text-primary font-bold">Total: $9.000</p>
              </div>
            </div>
          </div>
        )}

        {estrategia === 'CANTIDAD_FIJA' && (
          <div className="space-y-3">
            <p className="text-xs text-gray-400 leading-relaxed">
              El cliente lleva más unidades de las que paga. El descuento se calcula por <strong className="text-gray-300">ciclos completos</strong>.
            </p>
            <div className="p-2.5 bg-gray-800/40 rounded-lg space-y-2">
              <p className="text-[11px] text-gray-500">Ejemplo: 2×1 en cervezas ($3.000 c/u)</p>
              <div className="text-xs space-y-0.5">
                <p className="text-gray-400">1 cerveza → descuento: $0</p>
                <p className="text-green-400">2 cervezas → 1 gratis = <strong>−$3.000</strong></p>
                <p className="text-gray-400">3 cervezas → 1 gratis = −$3.000</p>
                <p className="text-green-400">4 cervezas → 2 gratis = <strong>−$6.000</strong></p>
              </div>
            </div>
            <p className="text-[11px] text-amber-400/80 leading-relaxed">
              💡 Las unidades que no completan un ciclo se cobran a precio normal.
            </p>
          </div>
        )}

        {estrategia === 'COMBO_CONDICIONAL' && (
          <div className="space-y-3">
            <p className="text-xs text-gray-400 leading-relaxed">
              Si el cliente compra un producto <strong className="text-gray-300">(activador)</strong>,
              otro producto <strong className="text-gray-300">(beneficiado)</strong> obtiene un descuento.
            </p>
            <div className="p-2.5 bg-gray-800/40 rounded-lg space-y-2">
              <p className="text-[11px] text-gray-500">Ejemplo: Comprando hamburguesa, gaseosa al 50% off</p>
              <div className="text-xs space-y-0.5">
                <p className="text-gray-400">Hamburguesa ($8.000) → precio normal</p>
                <p className="text-green-400">Gaseosa ($2.000) → <strong>50% off = $1.000</strong></p>
                <p className="text-text-primary font-bold">Total: $9.000 en vez de $10.000</p>
              </div>
            </div>
            <p className="text-[11px] text-amber-400/80 leading-relaxed">
              ⚠️ Este tipo requiere que definas productos "activadores" y "beneficiados" en el paso siguiente.
            </p>
          </div>
        )}

        {estrategia === 'PRECIO_FIJO_CANTIDAD' && (
          <div className="space-y-3">
            <p className="text-xs text-gray-400 leading-relaxed">
              Un pack con precio especial. Las unidades que no completan el pack se cobran a precio normal.
            </p>
            <div className="p-2.5 bg-gray-800/40 rounded-lg space-y-2">
              <p className="text-[11px] text-gray-500">Ejemplo: 2 hamburguesas por $22.000 (c/u $13.000)</p>
              <div className="text-xs space-y-0.5">
                <p className="text-gray-400">1 unidad → $13.000 (sin pack)</p>
                <p className="text-green-400">2 unidades → <strong>$22.000 (ahorrás $4.000)</strong></p>
                <p className="text-gray-400">3 unidades → $22.000 + $13.000 = $35.000</p>
                <p className="text-green-400">4 unidades → <strong>$44.000 (ahorrás $8.000)</strong></p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function ContenidoPaso3({ estrategia }: { estrategia: TipoEstrategia }) {
  const esCombo = estrategia === 'COMBO_CONDICIONAL';

  return (
    <div className="space-y-4">
      <div>
        <h4 className="text-sm font-semibold text-text-primary mb-2">🛒 ¿Cómo asociar productos?</h4>

        {esCombo ? (
          <div className="space-y-3">
            <p className="text-xs text-gray-400 leading-relaxed">
              Para un <strong className="text-gray-300">combo</strong> necesitás definir dos listas:
            </p>
            <div className="space-y-2">
              <div className="p-2.5 bg-amber-500/5 border border-amber-500/20 rounded-lg">
                <p className="text-xs font-semibold text-amber-400 mb-1">🔑 Productos que activan la promo</p>
                <p className="text-[11px] text-gray-400">
                  Son los productos que el cliente debe comprar para que se active el descuento.
                </p>
                <p className="text-[11px] text-gray-500 mt-1">Ej: Hamburguesa, Milanesa</p>
              </div>
              <div className="p-2.5 bg-blue-500/5 border border-blue-500/20 rounded-lg">
                <p className="text-xs font-semibold text-blue-400 mb-1">🎁 Productos que reciben el descuento</p>
                <p className="text-[11px] text-gray-400">
                  Son los productos que tendrán el descuento cuando se active la promo.
                </p>
                <p className="text-[11px] text-gray-500 mt-1">Ej: Gaseosa, Agua mineral, Jugo</p>
              </div>
            </div>
            <div className="p-2.5 bg-gray-800/40 rounded-lg">
              <p className="text-[11px] text-gray-500 mb-1">Ejemplo completo:</p>
              <p className="text-xs text-gray-300">
                Si seleccionás <strong>"Hamburguesa"</strong> como activador y <strong>"Papas Fritas"</strong> como beneficiado,
                cuando un cliente pida una hamburguesa, las papas tendrán descuento automático.
              </p>
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-xs text-gray-400 leading-relaxed">
              Seleccioná los productos a los que se les aplicará el descuento.
              Podés elegir varios a la vez.
            </p>
            <div className="p-2.5 bg-gray-800/40 rounded-lg">
              <p className="text-[11px] text-gray-500 mb-1">Ejemplo:</p>
              <p className="text-xs text-gray-300">
                Si creaste un "2×1 en Cervezas", seleccioná todas las cervezas que participan:
                IPA, Stout, Rubia, etc.
              </p>
            </div>
            <p className="text-[11px] text-amber-400/80 leading-relaxed">
              💡 Solo los productos que selecciones tendrán el descuento.
              Los que no estén en la lista se cobran a precio normal.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

function ContenidoPaso4() {
  return (
    <div className="space-y-4">
      <div>
        <h4 className="text-sm font-semibold text-text-primary mb-2">📅 ¿Cuándo es válida?</h4>
        <p className="text-xs text-gray-400 leading-relaxed mb-3">
          Configurá en qué días, horarios y fechas aplica la promoción.
          Podés combinar varias condiciones a la vez.
        </p>

        <div className="space-y-2">
          <div className="p-2.5 bg-gray-800/40 rounded-lg">
            <p className="text-xs font-medium text-gray-300 mb-1">Ejemplo: Happy Hour</p>
            <p className="text-[11px] text-gray-500">
              ✓ Días: Lunes a Viernes<br />
              ✓ Horario: 18:00 a 21:00<br />
              ✓ Vigencia: todo el año
            </p>
          </div>

          <div className="p-2.5 bg-gray-800/40 rounded-lg">
            <p className="text-xs font-medium text-gray-300 mb-1">Ejemplo: Promo fin de semana</p>
            <p className="text-[11px] text-gray-500">
              ✓ Días: Sábado y Domingo<br />
              ✓ Horario: todo el día<br />
              ✓ Vigencia: solo marzo 2026
            </p>
          </div>
        </div>
      </div>

      <div className="border-t border-gray-800 pt-3">
        <p className="text-[11px] text-amber-400/80 leading-relaxed">
          💡 Si no activás ninguna condición de días u horarios, la promo estará activa <strong>todos los días, a toda hora</strong> durante el rango de fechas.
        </p>
      </div>

      <div className="border-t border-gray-800 pt-3">
        <h4 className="text-sm font-semibold text-text-primary mb-2">💰 Monto mínimo</h4>
        <p className="text-xs text-gray-400 leading-relaxed">
          Si activás esta condición, el descuento solo aplica cuando el pedido total supera el monto indicado.
        </p>
        <div className="mt-2 p-2.5 bg-gray-800/40 rounded-lg">
          <p className="text-[11px] text-gray-500">
            Ejemplo: "10% OFF solo si el pedido supera $15.000"
          </p>
        </div>
      </div>
    </div>
  );
}

// ── Componente principal ──

/**
 * Panel lateral de ayuda contextual para el wizard de promociones.
 *
 * Cambia su contenido dinámicamente según el paso actual del wizard
 * y la estrategia seleccionada. Oculta la jerga técnica del motor
 * de reglas y muestra ejemplos concretos del negocio gastronómico.
 */
export default function PanelAyudaContextual({
  pasoActual,
  estrategiaSeleccionada,
}: PanelAyudaContextualProps) {
  const [visible, setVisible] = useState(true);

  const IconoPaso = ICON_POR_PASO[pasoActual - 1] ?? Lightbulb;

  const TITULO_POR_PASO = [
    'Consejos',
    '¿Cómo funciona?',
    'Productos',
    'Condiciones',
  ];

  if (!visible) {
    return (
      <button
        onClick={() => setVisible(true)}
        className="flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-medium
          text-gray-400 hover:text-gray-300 bg-gray-800/40 hover:bg-gray-800/60
          border border-gray-700/50 transition-all self-start"
      >
        <HelpCircle size={14} />
        Ver ayuda
      </button>
    );
  }

  return (
    <div className="flex flex-col h-full bg-gray-900/50 border border-gray-800 rounded-xl overflow-hidden">
      {/* Header del panel */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-800 shrink-0">
        <div className="flex items-center gap-2">
          <IconoPaso size={15} className="text-primary" />
          <h3 className="text-sm font-semibold text-text-primary">
            {TITULO_POR_PASO[pasoActual - 1]}
          </h3>
        </div>
        <button
          onClick={() => setVisible(false)}
          className="p-1 rounded hover:bg-gray-800 text-gray-500 transition-colors"
          title="Ocultar ayuda"
        >
          <X size={14} />
        </button>
      </div>

      {/* Contenido dinámico */}
      <div className="flex-1 overflow-y-auto p-4">
        {pasoActual === 1 && <ContenidoPaso1 />}
        {pasoActual === 2 && <ContenidoPaso2 estrategia={estrategiaSeleccionada} />}
        {pasoActual === 3 && <ContenidoPaso3 estrategia={estrategiaSeleccionada} />}
        {pasoActual === 4 && <ContenidoPaso4 />}
      </div>
    </div>
  );
}
