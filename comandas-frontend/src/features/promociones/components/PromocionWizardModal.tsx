import { useState, useMemo, useCallback } from 'react';
import {
  X,
  Check,
  ChevronRight,
  ChevronLeft,
  Percent,
  Tag,
  DollarSign,
  Package,
  Calendar,
  Clock,
  Search,
  ShoppingBag,
  Gift,
  Zap,
} from 'lucide-react';
import { useCrearPromocion, useEditarPromocion, useAsociarProductos } from '../hooks/usePromociones';
import { useProductos } from '../../catalogo/hooks/useProductos';
import type { ProductoResponse } from '../../catalogo/types';
import type {
  PromocionResponse,
  TipoEstrategia,
  ModoDescuento,
  DayOfWeek,
  CrearPromocionCommand,
  TriggerParams,
  ItemScopeParams,
} from '../types';
import PanelAyudaContextual from './PanelAyudaContextual';

// ── Configuración constante ──

interface PasoConfig {
  numero: number;
  titulo: string;
  descripcion: string;
}

const PASOS: PasoConfig[] = [
  { numero: 1, titulo: 'Datos Básicos', descripcion: 'Nombre y prioridad' },
  { numero: 2, titulo: 'El Beneficio', descripcion: '¿Qué descuento vas a dar?' },
  { numero: 3, titulo: 'Productos', descripcion: '¿A qué productos aplica?' },
  { numero: 4, titulo: 'Condiciones', descripcion: '¿Cuándo es válida?' },
];

const TIPOS_BENEFICIO: {
  valor: TipoEstrategia;
  label: string;
  desc: string;
  icon: typeof Percent;
  color: string;
}[] = [
  {
    valor: 'DESCUENTO_DIRECTO',
    label: 'Descuento Directo',
    desc: 'Porcentaje o monto fijo de descuento',
    icon: Percent,
    color: 'purple',
  },
  {
    valor: 'CANTIDAD_FIJA',
    label: 'Llevás N, Pagás M',
    desc: 'Ej: 2×1, 3×2',
    icon: Tag,
    color: 'blue',
  },
  {
    valor: 'COMBO_CONDICIONAL',
    label: 'Combo',
    desc: 'Comprando X, descuento en Y',
    icon: Package,
    color: 'amber',
  },
  {
    valor: 'PRECIO_FIJO_CANTIDAD',
    label: 'Precio por Pack',
    desc: 'N unidades por un precio especial',
    icon: DollarSign,
    color: 'emerald',
  },
];

const DIAS_SEMANA: { valor: DayOfWeek; label: string; corto: string }[] = [
  { valor: 'MONDAY', label: 'Lunes', corto: 'L' },
  { valor: 'TUESDAY', label: 'Martes', corto: 'M' },
  { valor: 'WEDNESDAY', label: 'Miércoles', corto: 'X' },
  { valor: 'THURSDAY', label: 'Jueves', corto: 'J' },
  { valor: 'FRIDAY', label: 'Viernes', corto: 'V' },
  { valor: 'SATURDAY', label: 'Sábado', corto: 'S' },
  { valor: 'SUNDAY', label: 'Domingo', corto: 'D' },
];

// ── Color helpers para tarjetas de beneficio ──

const COLOR_CLASSES: Record<string, { selected: string; idle: string }> = {
  purple: {
    selected: 'border-purple-500 bg-purple-500/10 ring-1 ring-purple-500/30',
    idle: 'border-gray-700 hover:border-purple-500/40 hover:bg-purple-500/5',
  },
  blue: {
    selected: 'border-blue-500 bg-blue-500/10 ring-1 ring-blue-500/30',
    idle: 'border-gray-700 hover:border-blue-500/40 hover:bg-blue-500/5',
  },
  amber: {
    selected: 'border-amber-500 bg-amber-500/10 ring-1 ring-amber-500/30',
    idle: 'border-gray-700 hover:border-amber-500/40 hover:bg-amber-500/5',
  },
  emerald: {
    selected: 'border-emerald-500 bg-emerald-500/10 ring-1 ring-emerald-500/30',
    idle: 'border-gray-700 hover:border-emerald-500/40 hover:bg-emerald-500/5',
  },
};

// ── Props ──

interface PromocionWizardModalProps {
  /** null = crear nueva, objeto = editar existente */
  promocion: PromocionResponse | null;
  onClose: () => void;
}

/**
 * Wizard de 4 pasos para crear/editar promociones.
 *
 * Layout: 2 columnas (formulario 70% + ayuda contextual 30%).
 * Oculta toda la jerga técnica (Scope, Triggers, Strategies) y usa
 * lenguaje ubicuo gastronómico para el operador del local.
 *
 * Pasos:
 * 1. Datos Básicos: nombre, descripción, prioridad
 * 2. El Beneficio: tipo de descuento + parámetros
 * 3. Productos: asociar productos (para combo: activadores + beneficiados)
 * 4. Condiciones: cuándo es válida la promo (días, horarios, fechas, monto mínimo)
 */
export default function PromocionWizardModal({ promocion, onClose }: PromocionWizardModalProps) {
  const esEdicion = promocion !== null;

  // ── Mutations ──
  const crearPromocion = useCrearPromocion();
  const editarPromocion = useEditarPromocion();
  const asociarProductos = useAsociarProductos();

  // ── Catálogo de productos para paso 3 ──
  const { data: todosLosProductos = [] } = useProductos();

  // ── Estado del wizard ──
  const [pasoActual, setPasoActual] = useState(1);
  const [error, setError] = useState<string | null>(null);

  // ── Paso 1: Datos básicos ──
  const [nombre, setNombre] = useState(promocion?.nombre ?? '');
  const [descripcion, setDescripcion] = useState(promocion?.descripcion ?? '');
  const [prioridad, setPrioridad] = useState(promocion?.prioridad?.toString() ?? '5');

  // ── Paso 2: El Beneficio (mapea a EstrategiaPromocion) ──
  const [tipoEstrategia, setTipoEstrategia] = useState<TipoEstrategia>(
    promocion?.estrategia.tipo ?? 'DESCUENTO_DIRECTO'
  );
  const [modoDescuento, setModoDescuento] = useState<ModoDescuento>(
    promocion?.estrategia.modoDescuento ?? 'PORCENTAJE'
  );
  const [valorDescuento, setValorDescuento] = useState(
    promocion?.estrategia.valorDescuento?.toString() ?? ''
  );
  const [cantidadLlevas, setCantidadLlevas] = useState(
    promocion?.estrategia.cantidadLlevas?.toString() ?? '2'
  );
  const [cantidadPagas, setCantidadPagas] = useState(
    promocion?.estrategia.cantidadPagas?.toString() ?? '1'
  );
  const [cantidadMinimaTrigger, setCantidadMinimaTrigger] = useState(
    promocion?.estrategia.cantidadMinimaTrigger?.toString() ?? '1'
  );
  const [porcentajeBeneficio, setPorcentajeBeneficio] = useState(
    promocion?.estrategia.porcentajeBeneficio?.toString() ?? ''
  );
  const [cantidadActivacion, setCantidadActivacion] = useState(
    promocion?.estrategia.cantidadActivacion?.toString() ?? '2'
  );
  const [precioPaquete, setPrecioPaquete] = useState(
    promocion?.estrategia.precioPaquete?.toString() ?? ''
  );

  // ── Paso 3: Productos Asociados (mapea a AlcancePromocion) ──
  const [busquedaProductos, setBusquedaProductos] = useState('');

  // Para todas las estrategias: productos que reciben el descuento
  const [productosTarget, setProductosTarget] = useState<ProductoResponse[]>(() => {
    if (!promocion) return [];
    return promocion.alcance.items
      .filter((i) => i.rol === 'TARGET')
      .map((i) => todosLosProductos.find((p) => p.id === i.referenciaId))
      .filter(Boolean) as ProductoResponse[];
  });

  // Solo para COMBO_CONDICIONAL: productos que activan la promo
  const [productosTrigger, setProductosTrigger] = useState<ProductoResponse[]>(() => {
    if (!promocion) return [];
    return promocion.alcance.items
      .filter((i) => i.rol === 'TRIGGER')
      .map((i) => todosLosProductos.find((p) => p.id === i.referenciaId))
      .filter(Boolean) as ProductoResponse[];
  });

  // ── Paso 4: Condiciones de validez (mapea a CriterioActivacion) ──
  const triggerTemporal = promocion?.triggers.find((t) => t.tipo === 'TEMPORAL');
  const triggerMonto = promocion?.triggers.find((t) => t.tipo === 'MONTO_MINIMO');

  const [usaDiasHorarios, setUsaDiasHorarios] = useState(
    () => !!(triggerTemporal?.diasSemana?.length || triggerTemporal?.horaDesde)
  );
  const [usaRangoFechas, setUsaRangoFechas] = useState(
    () => !!(triggerTemporal?.fechaDesde)
  );
  const [usaMontoMinimo, setUsaMontoMinimo] = useState(() => !!triggerMonto);

  const [fechaDesde, setFechaDesde] = useState(triggerTemporal?.fechaDesde ?? '');
  const [fechaHasta, setFechaHasta] = useState(triggerTemporal?.fechaHasta ?? '');
  const [diasSemana, setDiasSemana] = useState<DayOfWeek[]>(triggerTemporal?.diasSemana ?? []);
  const [horaDesde, setHoraDesde] = useState(triggerTemporal?.horaDesde ?? '');
  const [horaHasta, setHoraHasta] = useState(triggerTemporal?.horaHasta ?? '');
  const [montoMinimo, setMontoMinimo] = useState(triggerMonto?.montoMinimo?.toString() ?? '');

  // ── Productos filtrados para buscador ──
  const idsUsados = useMemo(() => {
    const set = new Set<string>();
    productosTarget.forEach((p) => set.add(p.id));
    productosTrigger.forEach((p) => set.add(p.id));
    return set;
  }, [productosTarget, productosTrigger]);

  const productosDisponibles = useMemo(
    () =>
      todosLosProductos
        .filter((p) => p.activo)
        .filter((p) => !idsUsados.has(p.id))
        .filter((p) =>
          p.nombre.toLowerCase().includes(busquedaProductos.toLowerCase())
        ),
    [todosLosProductos, idsUsados, busquedaProductos]
  );

  // ── Helpers ──

  const esCombo = tipoEstrategia === 'COMBO_CONDICIONAL';

  const toggleDia = useCallback((dia: DayOfWeek) => {
    setDiasSemana((prev) =>
      prev.includes(dia) ? prev.filter((d) => d !== dia) : [...prev, dia]
    );
  }, []);

  const agregarProducto = useCallback(
    (producto: ProductoResponse, rol: 'TARGET' | 'TRIGGER') => {
      if (rol === 'TARGET') {
        setProductosTarget((prev) => [...prev, producto]);
      } else {
        setProductosTrigger((prev) => [...prev, producto]);
      }
    },
    []
  );

  const quitarProducto = useCallback(
    (productoId: string, rol: 'TARGET' | 'TRIGGER') => {
      if (rol === 'TARGET') {
        setProductosTarget((prev) => prev.filter((p) => p.id !== productoId));
      } else {
        setProductosTrigger((prev) => prev.filter((p) => p.id !== productoId));
      }
    },
    []
  );

  // ── Validación por paso ──

  const validarPaso = (): boolean => {
    setError(null);

    if (pasoActual === 1) {
      if (!nombre.trim()) {
        setError('Dale un nombre a la promoción');
        return false;
      }
    }

    if (pasoActual === 2) {
      if (tipoEstrategia === 'DESCUENTO_DIRECTO') {
        if (!valorDescuento || parseFloat(valorDescuento) <= 0) {
          setError('Ingresá el valor del descuento');
          return false;
        }
        if (modoDescuento === 'PORCENTAJE' && parseFloat(valorDescuento) > 100) {
          setError('El porcentaje no puede ser mayor a 100');
          return false;
        }
      }
      if (tipoEstrategia === 'CANTIDAD_FIJA') {
        const llevas = parseInt(cantidadLlevas, 10);
        const pagas = parseInt(cantidadPagas, 10);
        if (llevas <= pagas) {
          setError('La cantidad que se lleva debe ser mayor que la que se paga');
          return false;
        }
      }
      if (tipoEstrategia === 'COMBO_CONDICIONAL') {
        if (!porcentajeBeneficio || parseFloat(porcentajeBeneficio) <= 0) {
          setError('Ingresá el porcentaje de descuento para el producto beneficiado');
          return false;
        }
        if (parseFloat(porcentajeBeneficio) > 100) {
          setError('El porcentaje no puede ser mayor a 100');
          return false;
        }
      }
      if (tipoEstrategia === 'PRECIO_FIJO_CANTIDAD') {
        if (!precioPaquete || parseFloat(precioPaquete) <= 0) {
          setError('Ingresá el precio del pack');
          return false;
        }
      }
    }

    if (pasoActual === 3) {
      if (productosTarget.length === 0) {
        setError(
          esCombo
            ? 'Seleccioná al menos un producto que reciba el descuento'
            : 'Seleccioná al menos un producto para la promoción'
        );
        return false;
      }
      if (esCombo && productosTrigger.length === 0) {
        setError('Seleccioná al menos un producto que active la promo');
        return false;
      }
    }

    if (pasoActual === 4) {
      if (usaRangoFechas && (!fechaDesde || !fechaHasta)) {
        setError('Completá las fechas de inicio y fin');
        return false;
      }
      if (usaRangoFechas && fechaDesde > fechaHasta) {
        setError('La fecha de inicio no puede ser posterior a la de fin');
        return false;
      }
      if (usaMontoMinimo && (!montoMinimo || parseFloat(montoMinimo) <= 0)) {
        setError('El monto mínimo debe ser mayor a cero');
        return false;
      }
      if (usaDiasHorarios && horaDesde && horaHasta && horaDesde >= horaHasta) {
        setError('La hora de inicio debe ser anterior a la hora de fin');
        return false;
      }
    }

    return true;
  };

  // ── Navegación ──

  const handleSiguiente = () => {
    if (validarPaso()) {
      setPasoActual((p) => Math.min(p + 1, 4));
    }
  };

  const handleAnterior = () => {
    setError(null);
    setPasoActual((p) => Math.max(p - 1, 1));
  };

  // ── Submit ──

  const handleGuardar = () => {
    if (!validarPaso()) return;

    // 1. Construir triggers
    const triggers: TriggerParams[] = [];

    // Trigger temporal: siempre necesario (el backend lo requiere)
    const tieneFechas = usaRangoFechas && fechaDesde && fechaHasta;
    const hoy = new Date().toISOString().split('T')[0];

    triggers.push({
      tipo: 'TEMPORAL',
      fechaDesde: tieneFechas ? fechaDesde : hoy,
      fechaHasta: tieneFechas ? fechaHasta : '2099-12-31',
      diasSemana: usaDiasHorarios && diasSemana.length > 0 ? diasSemana : undefined,
      horaDesde: usaDiasHorarios && horaDesde ? horaDesde : undefined,
      horaHasta: usaDiasHorarios && horaHasta ? horaHasta : undefined,
    });

    if (usaMontoMinimo && montoMinimo) {
      triggers.push({
        tipo: 'MONTO_MINIMO',
        montoMinimo: parseFloat(montoMinimo),
      });
    }

    // 2. Construir command
    const command: CrearPromocionCommand = {
      nombre: nombre.trim(),
      descripcion: descripcion.trim() || undefined,
      prioridad: parseInt(prioridad, 10) || 0,
      tipoEstrategia,
      triggers,
    };

    // 3. Parámetros de la estrategia
    switch (tipoEstrategia) {
      case 'DESCUENTO_DIRECTO':
        command.descuentoDirecto = {
          modo: modoDescuento,
          valor: parseFloat(valorDescuento),
        };
        break;
      case 'CANTIDAD_FIJA':
        command.cantidadFija = {
          cantidadLlevas: parseInt(cantidadLlevas, 10),
          cantidadPagas: parseInt(cantidadPagas, 10),
        };
        break;
      case 'COMBO_CONDICIONAL':
        command.comboCondicional = {
          cantidadMinimaTrigger: parseInt(cantidadMinimaTrigger, 10),
          porcentajeBeneficio: parseFloat(porcentajeBeneficio),
        };
        break;
      case 'PRECIO_FIJO_CANTIDAD':
        command.precioFijoPorCantidad = {
          cantidadActivacion: parseInt(cantidadActivacion, 10),
          precioPaquete: parseFloat(precioPaquete),
        };
        break;
    }

    // 4. Construir scope items
    const scopeItems: ItemScopeParams[] = [
      ...productosTarget.map((p) => ({
        referenciaId: p.id,
        tipo: 'PRODUCTO' as const,
        rol: 'TARGET' as const,
      })),
      ...productosTrigger.map((p) => ({
        referenciaId: p.id,
        tipo: 'PRODUCTO' as const,
        rol: 'TRIGGER' as const,
      })),
    ];

    // 5. Ejecutar
    if (esEdicion) {
      editarPromocion.mutate(
        {
          id: promocion.id,
          nombre: command.nombre,
          descripcion: command.descripcion,
          prioridad: command.prioridad,
          triggers: command.triggers,
          tipoEstrategia: command.tipoEstrategia,
          descuentoDirecto: command.descuentoDirecto,
          cantidadFija: command.cantidadFija,
          comboCondicional: command.comboCondicional,
          precioFijoPorCantidad: command.precioFijoPorCantidad,
        },
        {
          onSuccess: () => {
            // Actualizar scope también
            if (scopeItems.length > 0) {
              asociarProductos.mutate(
                { id: promocion.id, items: scopeItems },
                { onSuccess: onClose }
              );
            } else {
              onClose();
            }
          },
        }
      );
    } else {
      crearPromocion.mutate(command, {
        onSuccess: (response) => {
          // Asociar scope a la promo recién creada
          const nuevaPromoId = response?.data?.id;
          if (nuevaPromoId && scopeItems.length > 0) {
            asociarProductos.mutate(
              { id: nuevaPromoId, items: scopeItems },
              { onSuccess: onClose }
            );
          } else {
            onClose();
          }
        },
      });
    }
  };

  const isSaving =
    crearPromocion.isPending ||
    editarPromocion.isPending ||
    asociarProductos.isPending;

  // ────────────────────────────────────────────────────────
  // RENDER: Paso 1 — Datos Básicos
  // ────────────────────────────────────────────────────────

  const renderPaso1 = () => (
    <div className="space-y-5">
      <div className="flex flex-col gap-1.5">
        <label className="text-sm font-medium text-text-primary">Nombre de la promoción</label>
        <input
          type="text"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          placeholder='Ej: "Happy Hour Cervezas", "2×1 Empanadas Viernes"'
          className="min-h-[48px] px-4 bg-background-card border border-gray-700 rounded-lg
            text-text-primary placeholder:text-gray-600
            focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30
            transition-all"
          autoFocus
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label className="text-sm font-medium text-text-primary">
          Descripción <span className="text-gray-600 font-normal">(opcional)</span>
        </label>
        <textarea
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
          placeholder="Breve descripción para que recuerdes de qué se trata"
          rows={2}
          className="px-4 py-3 bg-background-card border border-gray-700 rounded-lg
            text-text-primary placeholder:text-gray-600 resize-none
            focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30
            transition-all text-sm"
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label className="text-sm font-medium text-text-primary">
          Prioridad
        </label>
        <p className="text-xs text-gray-500 -mt-0.5">
          Si un producto tiene 2 promos activas, se aplica la de mayor prioridad.
        </p>
        <input
          type="number"
          value={prioridad}
          onChange={(e) => setPrioridad(e.target.value)}
          min="0"
          className="min-h-[48px] px-4 bg-background-card border border-gray-700 rounded-lg
            text-text-primary font-mono w-28
            focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30
            transition-all"
        />
      </div>
    </div>
  );

  // ────────────────────────────────────────────────────────
  // RENDER: Paso 2 — El Beneficio
  // ────────────────────────────────────────────────────────

  const renderPaso2 = () => (
    <div className="space-y-5">
      {/* Tarjetas de tipo de beneficio */}
      <div>
        <label className="text-sm font-medium text-text-primary mb-3 block">
          ¿Qué tipo de descuento vas a dar?
        </label>
        <div className="grid grid-cols-2 gap-3">
          {TIPOS_BENEFICIO.map(({ valor, label, desc, icon: Icon, color }) => {
            const isSelected = tipoEstrategia === valor;
            const colorCls = COLOR_CLASSES[color];

            return (
              <button
                key={valor}
                type="button"
                onClick={() => setTipoEstrategia(valor)}
                disabled={esEdicion}
                className={[
                  'flex flex-col items-start gap-2 p-4 rounded-xl border-2 text-left transition-all duration-200',
                  isSelected ? colorCls.selected : colorCls.idle,
                  esEdicion && !isSelected ? 'opacity-30 cursor-not-allowed' : '',
                  esEdicion && isSelected ? 'cursor-default' : 'cursor-pointer',
                ].join(' ')}
              >
                <div className="flex items-center gap-2.5">
                  <div
                    className={`p-1.5 rounded-lg ${
                      isSelected ? 'bg-white/10' : 'bg-gray-800'
                    }`}
                  >
                    <Icon size={18} className={isSelected ? 'text-white' : 'text-gray-400'} />
                  </div>
                  <span className={`text-sm font-semibold ${isSelected ? 'text-white' : 'text-gray-300'}`}>
                    {label}
                  </span>
                </div>
                <span className={`text-xs ${isSelected ? 'text-gray-300' : 'text-gray-500'}`}>
                  {desc}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Parámetros del beneficio seleccionado */}
      <div className="p-4 bg-background-card rounded-xl border border-gray-800 space-y-4">
        <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">
          Configuración del beneficio
        </p>

        {/* DESCUENTO_DIRECTO */}
        {tipoEstrategia === 'DESCUENTO_DIRECTO' && (
          <div className="space-y-3">
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setModoDescuento('PORCENTAJE')}
                className={`flex-1 py-3 rounded-lg text-sm font-semibold border-2 transition-all ${
                  modoDescuento === 'PORCENTAJE'
                    ? 'border-purple-500 bg-purple-500/10 text-white'
                    : 'border-gray-700 text-gray-400 hover:border-gray-600'
                }`}
              >
                % Porcentaje
              </button>
              <button
                type="button"
                onClick={() => setModoDescuento('MONTO_FIJO')}
                className={`flex-1 py-3 rounded-lg text-sm font-semibold border-2 transition-all ${
                  modoDescuento === 'MONTO_FIJO'
                    ? 'border-purple-500 bg-purple-500/10 text-white'
                    : 'border-gray-700 text-gray-400 hover:border-gray-600'
                }`}
              >
                $ Monto Fijo
              </button>
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-gray-400">
                {modoDescuento === 'PORCENTAJE'
                  ? '¿Cuánto porcentaje de descuento? (1–100)'
                  : '¿Cuántos pesos de descuento por unidad?'}
              </label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 font-mono text-sm">
                  {modoDescuento === 'PORCENTAJE' ? '%' : '$'}
                </span>
                <input
                  type="number"
                  value={valorDescuento}
                  onChange={(e) => setValorDescuento(e.target.value)}
                  placeholder={modoDescuento === 'PORCENTAJE' ? '20' : '500'}
                  min="0"
                  max={modoDescuento === 'PORCENTAJE' ? '100' : undefined}
                  className="w-full min-h-[48px] pl-8 pr-4 bg-neutral-900 border border-gray-700 rounded-lg
                    text-text-primary font-mono text-lg
                    focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
                />
              </div>
            </div>
          </div>
        )}

        {/* CANTIDAD_FIJA */}
        {tipoEstrategia === 'CANTIDAD_FIJA' && (
          <div className="flex items-end gap-3">
            <div className="flex-1 min-w-0 flex flex-col gap-1.5">
              <label className="text-xs text-gray-400">Llevás</label>
              <input
                type="number"
                value={cantidadLlevas}
                onChange={(e) => setCantidadLlevas(e.target.value)}
                min="2"
                className="w-full min-h-[48px] px-4 bg-neutral-900 border border-gray-700 rounded-lg
                  text-text-primary font-mono text-lg text-center
                  focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
              />
            </div>
            <span className="text-3xl text-gray-500 font-bold mb-2">×</span>
            <div className="flex-1 min-w-0 flex flex-col gap-1.5">
              <label className="text-xs text-gray-400">Pagás</label>
              <input
                type="number"
                value={cantidadPagas}
                onChange={(e) => setCantidadPagas(e.target.value)}
                min="1"
                className="w-full min-h-[48px] px-4 bg-neutral-900 border border-gray-700 rounded-lg
                  text-text-primary font-mono text-lg text-center
                  focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
              />
            </div>
          </div>
        )}

        {/* COMBO_CONDICIONAL */}
        {tipoEstrategia === 'COMBO_CONDICIONAL' && (
          <div className="space-y-3">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-gray-400">
                ¿Cuántas unidades del activador debe comprar el cliente?
              </label>
              <input
                type="number"
                value={cantidadMinimaTrigger}
                onChange={(e) => setCantidadMinimaTrigger(e.target.value)}
                min="1"
                className="w-24 min-h-[48px] px-4 bg-neutral-900 border border-gray-700 rounded-lg
                  text-text-primary font-mono text-lg text-center
                  focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-xs text-gray-400">
                ¿Cuánto % de descuento recibe el producto beneficiado?
              </label>
              <div className="relative w-32">
                <input
                  type="number"
                  value={porcentajeBeneficio}
                  onChange={(e) => setPorcentajeBeneficio(e.target.value)}
                  placeholder="50"
                  min="1"
                  max="100"
                  className="w-full min-h-[48px] pl-4 pr-8 bg-neutral-900 border border-gray-700 rounded-lg
                    text-text-primary font-mono text-lg
                    focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 font-mono">%</span>
              </div>
            </div>
          </div>
        )}

        {/* PRECIO_FIJO_CANTIDAD */}
        {tipoEstrategia === 'PRECIO_FIJO_CANTIDAD' && (
          <div className="flex items-end gap-3">
            <div className="flex-1 min-w-0 flex flex-col gap-1.5">
              <label className="text-xs text-gray-400">Cantidad de unidades</label>
              <input
                type="number"
                value={cantidadActivacion}
                onChange={(e) => setCantidadActivacion(e.target.value)}
                min="2"
                className="w-full min-h-[48px] px-4 bg-neutral-900 border border-gray-700 rounded-lg
                  text-text-primary font-mono text-lg text-center
                  focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
              />
            </div>
            <span className="text-gray-500 text-sm font-medium mb-3">por</span>
            <div className="flex-1 min-w-0 flex flex-col gap-1.5">
              <label className="text-xs text-gray-400">Precio del pack ($)</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 font-mono">$</span>
                <input
                  type="number"
                  value={precioPaquete}
                  onChange={(e) => setPrecioPaquete(e.target.value)}
                  placeholder="22000"
                  min="0"
                  className="w-full min-h-[48px] pl-7 pr-4 bg-neutral-900 border border-gray-700 rounded-lg
                    text-text-primary font-mono text-lg
                    focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
                />
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );

  // ────────────────────────────────────────────────────────
  // RENDER: Paso 3 — Productos Asociados
  // ────────────────────────────────────────────────────────

  const renderListaProductos = (
    titulo: string,
    icono: React.ReactNode,
    productos: ProductoResponse[],
    rol: 'TARGET' | 'TRIGGER',
    colorBadge: string
  ) => (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          {icono}
          <span className="text-sm font-medium text-text-primary">{titulo}</span>
        </div>
        {productos.length > 0 && (
          <span className="text-xs text-gray-500">{productos.length} seleccionados</span>
        )}
      </div>

      {/* Chips de productos seleccionados */}
      {productos.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {productos.map((p) => (
            <span
              key={p.id}
              className={`inline-flex items-center gap-1.5 pl-1.5 pr-2 py-1 rounded-full text-xs font-medium ${colorBadge}`}
            >
              <span
                className="w-3.5 h-3.5 rounded-full shrink-0 border border-white/20"
                style={{ backgroundColor: p.colorHex }}
              />
              {p.nombre}
              <button
                type="button"
                onClick={() => quitarProducto(p.id, rol)}
                className="ml-0.5 hover:text-white transition-colors"
              >
                <X size={12} />
              </button>
            </span>
          ))}
        </div>
      )}

      {productos.length === 0 && (
        <p className="text-xs text-gray-600 italic py-2">
          Buscá y seleccioná productos de la lista de abajo.
        </p>
      )}
    </div>
  );

  const renderPaso3 = () => (
    <div className="space-y-4">
      {/* Listas de productos seleccionados */}
      {esCombo ? (
        <div className="space-y-4">
          {renderListaProductos(
            'Productos que activan la promo',
            <Zap size={15} className="text-amber-400" />,
            productosTrigger,
            'TRIGGER',
            'bg-amber-500/15 text-amber-300 ring-1 ring-amber-500/20'
          )}
          <div className="border-t border-gray-800" />
          {renderListaProductos(
            'Productos que reciben el descuento',
            <Gift size={15} className="text-blue-400" />,
            productosTarget,
            'TARGET',
            'bg-blue-500/15 text-blue-300 ring-1 ring-blue-500/20'
          )}
        </div>
      ) : (
        renderListaProductos(
          'Productos a los que se les aplica el descuento',
          <ShoppingBag size={15} className="text-primary" />,
          productosTarget,
          'TARGET',
          'bg-red-500/15 text-red-300 ring-1 ring-red-500/20'
        )
      )}

      {/* Buscador de productos */}
      <div className="border-t border-gray-800 pt-3">
        <div className="relative mb-2">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
          <input
            type="text"
            value={busquedaProductos}
            onChange={(e) => setBusquedaProductos(e.target.value)}
            placeholder="Buscar productos..."
            className="w-full min-h-[42px] pl-9 pr-4 bg-neutral-900 border border-gray-700 rounded-lg
              text-text-primary text-sm placeholder:text-gray-600
              focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
          />
        </div>

        {/* Lista de productos disponibles */}
        <div className="max-h-[200px] overflow-y-auto rounded-lg border border-gray-800 bg-neutral-950">
          {productosDisponibles.length === 0 ? (
            <p className="text-xs text-gray-600 text-center py-6">
              {busquedaProductos
                ? 'No se encontraron productos'
                : 'Todos los productos ya están seleccionados'}
            </p>
          ) : (
            productosDisponibles.map((p) => (
              <div
                key={p.id}
                className="flex items-center gap-3 px-3 py-2.5 border-b border-gray-800/40
                  hover:bg-gray-800/40 transition-colors"
              >
                <span
                  className="w-4 h-4 rounded-full shrink-0 border border-gray-600"
                  style={{ backgroundColor: p.colorHex }}
                />
                <span className="text-sm text-gray-300 flex-1 truncate">{p.nombre}</span>
                <span className="text-xs text-gray-500 font-mono shrink-0">
                  ${p.precio.toLocaleString('es-AR')}
                </span>

                {/* Botones de agregar */}
                {esCombo ? (
                  <div className="flex gap-1.5 shrink-0">
                    <button
                      type="button"
                      onClick={() => agregarProducto(p, 'TRIGGER')}
                      className="px-2.5 py-1 rounded-md text-[11px] font-semibold
                        bg-amber-500/10 text-amber-400 ring-1 ring-amber-500/20
                        hover:bg-amber-500/20 transition-all"
                      title="Agregar como activador"
                    >
                      <Zap size={11} className="inline mr-0.5 -mt-0.5" />
                      Activador
                    </button>
                    <button
                      type="button"
                      onClick={() => agregarProducto(p, 'TARGET')}
                      className="px-2.5 py-1 rounded-md text-[11px] font-semibold
                        bg-blue-500/10 text-blue-400 ring-1 ring-blue-500/20
                        hover:bg-blue-500/20 transition-all"
                      title="Agregar como beneficiado"
                    >
                      <Gift size={11} className="inline mr-0.5 -mt-0.5" />
                      Beneficiado
                    </button>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() => agregarProducto(p, 'TARGET')}
                    className="px-2.5 py-1.5 rounded-md text-[11px] font-semibold
                      bg-red-500/10 text-red-400 ring-1 ring-red-500/20
                      hover:bg-red-500/20 transition-all shrink-0"
                  >
                    + Agregar
                  </button>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );

  // ────────────────────────────────────────────────────────
  // RENDER: Paso 4 — Condiciones de validez
  // ────────────────────────────────────────────────────────

  const renderPaso4 = () => (
    <div className="space-y-4">
      {/* Switch: Rango de fechas */}
      <div
        className={`p-4 rounded-xl border transition-all ${
          usaRangoFechas ? 'border-gray-700 bg-background-card' : 'border-gray-800/50'
        }`}
      >
        <label className="flex items-center justify-between cursor-pointer">
          <div className="flex items-center gap-3">
            <Calendar size={18} className={usaRangoFechas ? 'text-primary' : 'text-gray-500'} />
            <div>
              <p className="text-sm font-medium text-text-primary">Rango de fechas</p>
              <p className="text-xs text-gray-500">Solo válida entre dos fechas específicas</p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setUsaRangoFechas(!usaRangoFechas)}
            className={`relative w-11 h-6 rounded-full transition-colors ${
              usaRangoFechas ? 'bg-primary' : 'bg-gray-700'
            }`}
          >
            <span
              className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white transition-transform ${
                usaRangoFechas ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </label>

        {usaRangoFechas && (
          <div className="grid grid-cols-2 gap-3 mt-3 pt-3 border-t border-gray-800">
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-400">Desde</label>
              <input
                type="date"
                value={fechaDesde}
                onChange={(e) => setFechaDesde(e.target.value)}
                className="min-h-[42px] px-3 bg-neutral-900 border border-gray-700 rounded-lg
                  text-text-primary text-sm
                  focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs text-gray-400">Hasta</label>
              <input
                type="date"
                value={fechaHasta}
                onChange={(e) => setFechaHasta(e.target.value)}
                className="min-h-[42px] px-3 bg-neutral-900 border border-gray-700 rounded-lg
                  text-text-primary text-sm
                  focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
              />
            </div>
          </div>
        )}
      </div>

      {/* Switch: Días y horarios */}
      <div
        className={`p-4 rounded-xl border transition-all ${
          usaDiasHorarios ? 'border-gray-700 bg-background-card' : 'border-gray-800/50'
        }`}
      >
        <label className="flex items-center justify-between cursor-pointer">
          <div className="flex items-center gap-3">
            <Clock size={18} className={usaDiasHorarios ? 'text-primary' : 'text-gray-500'} />
            <div>
              <p className="text-sm font-medium text-text-primary">Días y horarios específicos</p>
              <p className="text-xs text-gray-500">Ej: solo viernes de 18 a 22hs</p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setUsaDiasHorarios(!usaDiasHorarios)}
            className={`relative w-11 h-6 rounded-full transition-colors ${
              usaDiasHorarios ? 'bg-primary' : 'bg-gray-700'
            }`}
          >
            <span
              className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white transition-transform ${
                usaDiasHorarios ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </label>

        {usaDiasHorarios && (
          <div className="space-y-3 mt-3 pt-3 border-t border-gray-800">
            {/* Selector de días */}
            <div>
              <label className="text-xs text-gray-400 mb-2 block">
                Días activos <span className="text-gray-600">(si no seleccionás ninguno, aplica todos)</span>
              </label>
              <div className="flex gap-1.5">
                {DIAS_SEMANA.map(({ valor, label, corto }) => (
                  <button
                    key={valor}
                    type="button"
                    onClick={() => toggleDia(valor)}
                    title={label}
                    className={[
                      'w-10 h-10 rounded-lg text-xs font-bold transition-all border-2',
                      diasSemana.includes(valor)
                        ? 'bg-red-500/20 border-red-500 text-red-400'
                        : 'border-gray-700 text-gray-500 hover:border-gray-500 hover:text-gray-400',
                    ].join(' ')}
                  >
                    {corto}
                  </button>
                ))}
              </div>
            </div>

            {/* Rango horario */}
            <div className="grid grid-cols-2 gap-3">
              <div className="flex flex-col gap-1">
                <label className="text-xs text-gray-400">Hora desde</label>
                <input
                  type="time"
                  value={horaDesde}
                  onChange={(e) => setHoraDesde(e.target.value)}
                  className="min-h-[42px] px-3 bg-neutral-900 border border-gray-700 rounded-lg
                    text-text-primary text-sm
                    focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
                />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-xs text-gray-400">Hora hasta</label>
                <input
                  type="time"
                  value={horaHasta}
                  onChange={(e) => setHoraHasta(e.target.value)}
                  className="min-h-[42px] px-3 bg-neutral-900 border border-gray-700 rounded-lg
                    text-text-primary text-sm
                    focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
                />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Switch: Monto mínimo */}
      <div
        className={`p-4 rounded-xl border transition-all ${
          usaMontoMinimo ? 'border-gray-700 bg-background-card' : 'border-gray-800/50'
        }`}
      >
        <label className="flex items-center justify-between cursor-pointer">
          <div className="flex items-center gap-3">
            <DollarSign size={18} className={usaMontoMinimo ? 'text-primary' : 'text-gray-500'} />
            <div>
              <p className="text-sm font-medium text-text-primary">Monto mínimo de compra</p>
              <p className="text-xs text-gray-500">Solo aplica si el pedido supera un monto</p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setUsaMontoMinimo(!usaMontoMinimo)}
            className={`relative w-11 h-6 rounded-full transition-colors ${
              usaMontoMinimo ? 'bg-primary' : 'bg-gray-700'
            }`}
          >
            <span
              className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-white transition-transform ${
                usaMontoMinimo ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </label>

        {usaMontoMinimo && (
          <div className="mt-3 pt-3 border-t border-gray-800">
            <label className="text-xs text-gray-400 mb-1.5 block">
              Monto mínimo del pedido ($)
            </label>
            <div className="relative w-48">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 font-mono">$</span>
              <input
                type="number"
                value={montoMinimo}
                onChange={(e) => setMontoMinimo(e.target.value)}
                placeholder="15000"
                min="0"
                className="w-full min-h-[42px] pl-7 pr-4 bg-neutral-900 border border-gray-700 rounded-lg
                  text-text-primary font-mono
                  focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
              />
            </div>
          </div>
        )}
      </div>

      {/* Info si no activa nada */}
      {!usaRangoFechas && !usaDiasHorarios && !usaMontoMinimo && (
        <div className="flex items-start gap-2 p-3 bg-amber-500/5 border border-amber-500/20 rounded-lg">
          <span className="text-amber-400 shrink-0 mt-0.5">💡</span>
          <p className="text-xs text-amber-400/80 leading-relaxed">
            Sin condiciones activas, la promoción será válida <strong>todos los días, a toda hora, sin límite de fecha</strong>.
          </p>
        </div>
      )}
    </div>
  );

  // ────────────────────────────────────────────────────────
  // RENDER: Layout principal
  // ────────────────────────────────────────────────────────

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 z-50 bg-black/70 animate-backdrop-in"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none">
        <div
          className="bg-neutral-900 border border-gray-800 rounded-2xl w-full max-w-5xl max-h-[92vh]
            flex flex-col pointer-events-auto animate-modal-in shadow-2xl shadow-black/50"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-800 shrink-0">
            <h2 className="text-lg font-bold text-text-primary">
              {esEdicion ? 'Editar Promoción' : 'Nueva Promoción'}
            </h2>
            <button
              onClick={onClose}
              className="p-2 rounded-lg hover:bg-gray-800 text-gray-400 transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          {/* Stepper */}
          <div className="flex items-center gap-1 px-6 py-3 border-b border-gray-800/50 shrink-0 overflow-x-auto">
            {PASOS.map((paso, i) => (
              <div key={paso.numero} className="flex items-center gap-1">
                {i > 0 && (
                  <div
                    className={`w-8 h-px shrink-0 ${
                      pasoActual > i ? 'bg-green-500/50' : 'bg-gray-700'
                    }`}
                  />
                )}
                <button
                  type="button"
                  onClick={() => {
                    // Permitir ir hacia atrás o al paso actual
                    if (paso.numero <= pasoActual) {
                      setError(null);
                      setPasoActual(paso.numero);
                    }
                  }}
                  className={`flex items-center gap-2 shrink-0 ${
                    paso.numero <= pasoActual ? 'cursor-pointer' : 'cursor-default'
                  }`}
                >
                  <div
                    className={[
                      'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-colors',
                      pasoActual === paso.numero
                        ? 'bg-red-500 text-white'
                        : pasoActual > paso.numero
                          ? 'bg-green-500/20 text-green-400'
                          : 'bg-gray-800 text-gray-500',
                    ].join(' ')}
                  >
                    {pasoActual > paso.numero ? <Check size={14} /> : paso.numero}
                  </div>
                  <div className="hidden md:block">
                    <p
                      className={`text-xs font-medium ${
                        pasoActual >= paso.numero ? 'text-text-primary' : 'text-gray-500'
                      }`}
                    >
                      {paso.titulo}
                    </p>
                    <p className="text-[10px] text-gray-600">{paso.descripcion}</p>
                  </div>
                </button>
              </div>
            ))}
          </div>

          {/* Body: 2 columnas */}
          <div className="flex-1 flex min-h-0 overflow-hidden">
            {/* Columna izquierda: Formulario (70%) */}
            <div className="flex-[7] overflow-y-auto px-6 py-5 border-r border-gray-800/50">
              {pasoActual === 1 && renderPaso1()}
              {pasoActual === 2 && renderPaso2()}
              {pasoActual === 3 && renderPaso3()}
              {pasoActual === 4 && renderPaso4()}

              {/* Error */}
              {error && (
                <div className="mt-4 p-3 bg-red-500/10 border border-red-500/20 rounded-lg">
                  <p className="text-sm text-red-400">{error}</p>
                </div>
              )}
            </div>

            {/* Columna derecha: Ayuda contextual (30%) */}
            <div className="hidden lg:flex flex-[3] p-4">
              <PanelAyudaContextual
                pasoActual={pasoActual}
                estrategiaSeleccionada={tipoEstrategia}
              />
            </div>
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between px-6 py-4 border-t border-gray-800 shrink-0">
            <button
              onClick={pasoActual === 1 ? onClose : handleAnterior}
              className="min-h-[44px] px-5 rounded-lg text-sm font-medium
                bg-gray-800 text-gray-300 border border-gray-700
                hover:bg-gray-700 hover:text-white transition-all
                flex items-center gap-2"
              disabled={isSaving}
            >
              {pasoActual === 1 ? (
                'Cancelar'
              ) : (
                <>
                  <ChevronLeft size={16} />
                  <span>Anterior</span>
                </>
              )}
            </button>

            {pasoActual < 4 ? (
              <button
                onClick={handleSiguiente}
                className="min-h-[44px] px-6 rounded-lg text-sm font-semibold
                  bg-primary text-white
                  hover:bg-red-600 transition-all
                  flex items-center gap-2"
              >
                <span>Siguiente</span>
                <ChevronRight size={16} />
              </button>
            ) : (
              <button
                onClick={handleGuardar}
                className="min-h-[44px] px-6 rounded-lg text-sm font-semibold
                  bg-primary text-white
                  hover:bg-red-600 transition-all
                  flex items-center gap-2"
                disabled={isSaving}
              >
                {isSaving ? (
                  <span className="animate-pulse">Guardando...</span>
                ) : (
                  <>
                    <Check size={16} />
                    <span>{esEdicion ? 'Guardar Cambios' : 'Crear Promoción'}</span>
                  </>
                )}
              </button>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
