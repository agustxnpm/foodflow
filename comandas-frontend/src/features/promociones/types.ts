/**
 * Tipos de dominio para el módulo Promociones
 *
 * Refleja los DTOs del backend: CrearPromocionCommand, EditarPromocionCommand,
 * AsociarScopeCommand, PromocionResponse.
 *
 * El sistema de promociones usa un patrón Specification/Composite Trigger:
 * - Triggers (criterios de activación): condiciones que deben cumplirse (AND)
 * - Estrategia: define el beneficio que se otorga una vez activada
 * - Alcance (scope): qué productos/categorías participan y con qué rol
 *
 * @see backend: com.agustinpalma.comandas.application.dto
 */

// ─── Enums ────────────────────────────────────────────────────────────────────

/** Estado de activación de una promoción */
export type EstadoPromocion = 'ACTIVA' | 'INACTIVA';

/** Tipo de estrategia de beneficio */
export type TipoEstrategia =
  | 'DESCUENTO_DIRECTO'
  | 'CANTIDAD_FIJA'
  | 'COMBO_CONDICIONAL'
  | 'PRECIO_FIJO_CANTIDAD';

/** Modo de descuento directo */
export type ModoDescuento = 'PORCENTAJE' | 'MONTO_FIJO';

/** Tipo de criterio de activación (trigger) */
export type TipoCriterio = 'TEMPORAL' | 'CONTENIDO' | 'MONTO_MINIMO';

/**
 * Si la referencia del alcance es un producto individual o una categoría.
 * HU-09: Asociar productos a promociones.
 */
export type TipoAlcance = 'PRODUCTO' | 'CATEGORIA';

/**
 * Rol de un producto/categoría en una promoción (HU-09).
 * - TRIGGER: Activa la promoción (satisface el CriterioContenido)
 * - TARGET: Recibe el beneficio de la estrategia
 */
export type RolPromocion = 'TRIGGER' | 'TARGET';

/** Día de la semana (ISO 8601, usado en triggers temporales) */
export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

// ─── Estrategia (params de creación) ──────────────────────────────────────────

/** Params para estrategia DESCUENTO_DIRECTO */
export interface DescuentoDirectoParams {
  /** "PORCENTAJE" o "MONTO_FIJO" */
  modo: ModoDescuento;
  valor: number;
}

/** Params para estrategia CANTIDAD_FIJA (NxM, ej: 2x1) */
export interface CantidadFijaParams {
  cantidadLlevas: number;
  cantidadPagas: number;
}

/** Params para estrategia COMBO_CONDICIONAL */
export interface ComboCondicionalParams {
  cantidadMinimaTrigger: number;
  porcentajeBeneficio: number;
}

/** Params para estrategia PRECIO_FIJO_CANTIDAD (Pack con precio especial) */
export interface PrecioFijoPorCantidadParams {
  /** Cantidad de activación (min 2) */
  cantidadActivacion: number;
  /** Precio especial del paquete */
  precioPaquete: number;
}

// ─── Triggers (criterios de activación) ───────────────────────────────────────

/**
 * Parámetros polimórficos para triggers (criterios de activación).
 *
 * El tipo determina qué campos son obligatorios:
 * - TEMPORAL: fechaDesde, fechaHasta, diasSemana, horaDesde, horaHasta
 * - CONTENIDO: productosRequeridos
 * - MONTO_MINIMO: montoMinimo
 */
export interface TriggerParams {
  tipo: TipoCriterio;
  /** ISO 8601 date (TEMPORAL) */
  fechaDesde?: string;
  /** ISO 8601 date (TEMPORAL) */
  fechaHasta?: string;
  diasSemana?: DayOfWeek[];
  /** ISO 8601 time "HH:mm" (TEMPORAL) */
  horaDesde?: string;
  /** ISO 8601 time "HH:mm" (TEMPORAL) */
  horaHasta?: string;
  /** UUIDs de productos requeridos (CONTENIDO) */
  productosRequeridos?: string[];
  /** Umbral mínimo del pedido (MONTO_MINIMO) */
  montoMinimo?: number;
}

// ─── Alcance (Scope) ──────────────────────────────────────────────────────────

/**
 * Ítem dentro del alcance de la promoción (HU-09).
 * Define qué productos/categorías ACTIVAN (TRIGGER) o RECIBEN (TARGET) el beneficio.
 */
export interface ItemScopeParams {
  referenciaId: string;
  tipo: TipoAlcance;
  rol: RolPromocion;
}

/** Command para asociar scope a una promoción (HU-09) */
export interface AsociarScopeCommand {
  items: ItemScopeParams[];
}

// ─── Requests ─────────────────────────────────────────────────────────────────

/**
 * Command para crear una promoción con triggers configurables.
 * Refleja CrearPromocionCommand del backend.
 */
export interface CrearPromocionCommand {
  nombre: string;
  descripcion?: string;
  prioridad: number;
  tipoEstrategia: TipoEstrategia;
  descuentoDirecto?: DescuentoDirectoParams;
  cantidadFija?: CantidadFijaParams;
  comboCondicional?: ComboCondicionalParams;
  precioFijoPorCantidad?: PrecioFijoPorCantidadParams;
  triggers: TriggerParams[];
}

/**
 * Command para editar una promoción existente.
 * Refleja EditarPromocionCommand del backend.
 * Actualización parcial: solo se modifican los campos presentes.
 * Incluye campos opcionales de estrategia para permitir cambiar el beneficio.
 */
export interface EditarPromocionCommand {
  nombre: string;
  descripcion?: string;
  prioridad?: number;
  triggers?: TriggerParams[];
  tipoEstrategia?: TipoEstrategia;
  descuentoDirecto?: DescuentoDirectoParams;
  cantidadFija?: CantidadFijaParams;
  comboCondicional?: ComboCondicionalParams;
  precioFijoPorCantidad?: PrecioFijoPorCantidadParams;
}

// ─── Responses ────────────────────────────────────────────────────────────────

/** Representación del alcance en la respuesta REST */
export interface AlcanceResponse {
  items: ItemAlcanceResponse[];
}

/** Ítem dentro del alcance en la respuesta */
export interface ItemAlcanceResponse {
  referenciaId: string;
  tipo: TipoAlcance;
  rol: RolPromocion;
}

/**
 * Estrategia de la promoción en la respuesta REST.
 * Formato aplanado: tipo + parámetros condicionales según el tipo.
 */
export interface EstrategiaResponse {
  tipo: TipoEstrategia;
  modoDescuento?: ModoDescuento;
  valorDescuento?: number;
  cantidadLlevas?: number;
  cantidadPagas?: number;
  cantidadMinimaTrigger?: number;
  porcentajeBeneficio?: number;
  cantidadActivacion?: number;
  precioPaquete?: number;
}

/**
 * Trigger en la respuesta REST.
 * Formato aplanado con tipo + parámetros condicionales.
 */
export interface TriggerResponse {
  tipo: TipoCriterio;
  /** ISO 8601 date */
  fechaDesde?: string;
  /** ISO 8601 date */
  fechaHasta?: string;
  diasSemana?: DayOfWeek[];
  /** ISO 8601 time */
  horaDesde?: string;
  /** ISO 8601 time */
  horaHasta?: string;
  productosRequeridos?: string[];
  montoMinimo?: number;
}

/**
 * Respuesta completa de una promoción.
 * Refleja PromocionResponse del backend.
 */
export interface PromocionResponse {
  id: string;
  nombre: string;
  descripcion?: string;
  prioridad: number;
  estado: EstadoPromocion;
  estrategia: EstrategiaResponse;
  triggers: TriggerResponse[];
  alcance: AlcanceResponse;
}
