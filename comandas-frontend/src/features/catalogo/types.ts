/**
 * Tipos de dominio para el módulo Catálogo (Productos + Stock)
 *
 * Refleja los DTOs del backend: ProductoRequest, ProductoResponse,
 * StockAjusteRequestBody, AjustarStockResponse.
 *
 * @see backend: com.agustinpalma.comandas.application.dto
 */

// ─── Enums ────────────────────────────────────────────────────────────────────

/**
 * Tipos de movimiento de stock.
 * Cada movimiento registra la razón del cambio en el inventario.
 */
export type TipoMovimientoStock =
  | 'VENTA'
  | 'REAPERTURA_PEDIDO'
  | 'AJUSTE_MANUAL'
  | 'INGRESO_MERCADERIA';

// ─── Producto ─────────────────────────────────────────────────────────────────

/**
 * DTO de entrada para crear o editar un producto.
 * Refleja ProductoRequest del backend.
 * Las validaciones de negocio se ejecutan en el dominio.
 */
export interface ProductoRequest {
  nombre: string;
  /** Debe ser > 0 */
  precio: number;
  /** Opcional, si es null/undefined se asume true en creación */
  activo?: boolean;
  /** Color hex para botones en el frontend (ej: "#FF0000"). Default: "#FFFFFF" */
  colorHex?: string;
  /** Activa el control de inventario. Default: false en creación */
  controlaStock?: boolean;
  /** Indica si el producto es un extra (huevo, queso, disco, etc.). Default: false */
  esExtra?: boolean;
  /** UUID de la categoría a la que pertenece el producto. Nullable */
  categoriaId?: string;
  /** Si el producto admite extras/agregados. Default: true */
  permiteExtras?: boolean;
  /** Si el POS debe abrir modal de configuración. Default: true */
  requiereConfiguracion?: boolean;
}

/**
 * Información resumida de una promoción activa asociada a un producto.
 * Refleja ProductoResponse.PromocionActivaInfo del backend.
 */
export interface PromocionActivaInfo {
  nombre: string;
  /** Tipo de estrategia: DESCUENTO_DIRECTO, CANTIDAD_FIJA, COMBO_CONDICIONAL, PRECIO_FIJO_CANTIDAD */
  tipoEstrategia: string;
}

/**
 * DTO de salida para productos.
 * Refleja ProductoResponse del backend.
 * Incluye colorHex para renderizar botones y datos de stock.
 * Incluye promociones activas asociadas al producto.
 */
export interface ProductoResponse {
  /** UUID como string */
  id: string;
  nombre: string;
  precio: number;
  activo: boolean;
  /** Siempre normalizado a mayúsculas (ej: "#FF0000") */
  colorHex: string;
  /** Cantidad actual en inventario */
  stockActual: number | null;
  /** Si el producto tiene control de inventario activo */
  controlaStock: boolean | null;
  /** true si es un extra (huevo, queso, disco de carne, etc.) */
  esExtra: boolean;
  /** UUID de la categoría a la que pertenece. Puede ser null */
  categoriaId?: string | null;
  /** Si el producto admite extras/agregados. Default true */
  permiteExtras?: boolean;
  /** Si true, el POS abre modal de configuración (observaciones + extras) antes de agregar */
  requiereConfiguracion: boolean;
  /** UUID del grupo de variantes (ej: todas las hamburguesas simples/dobles/triples) */
  grupoVarianteId?: string | null;
  /** Cantidad de discos de carne del producto (para ordenar variantes). Puede ser null */
  cantidadDiscosCarne?: number | null;
  /** Promociones activas que aplican a este producto (puede estar vacía) */
  promocionesActivas: PromocionActivaInfo[];
}

// ─── Stock ────────────────────────────────────────────────────────────────────

/**
 * Body HTTP para ajuste manual de stock (HU-22).
 * Refleja StockAjusteRequestBody del backend.
 * El productoId viaja como path param.
 */
export interface StockAjusteRequest {
  /** La cantidad no puede ser cero */
  cantidad: number;
  tipo: TipoMovimientoStock;
  /** Motivo obligatorio del ajuste */
  motivo: string;
}

/**
 * Respuesta a un ajuste de stock (HU-22).
 * Refleja AjustarStockResponse del backend.
 */
export interface AjustarStockResponse {
  productoId: string;
  nombreProducto: string;
  stockActual: number;
  cantidadAjustada: number;
  tipo: TipoMovimientoStock;
  motivo: string;
  /** ISO 8601 datetime */
  fecha: string;
}

// ─── Variantes ────────────────────────────────────────────────────────────────

/**
 * DTO de entrada para crear una variante de un producto base.
 * Refleja VarianteProductoRequest del backend.
 *
 * El productoBaseId viaja como path param.
 * El grupoVarianteId se determina automáticamente en el use case.
 */
export interface VarianteRequest {
  nombre: string;
  /** Debe ser > 0 */
  precio: number;
  /** Jerarquía de la variante (1 = simple, 2 = doble, etc.) */
  cantidadDiscosCarne: number;
  activo?: boolean;
  colorHex?: string;
  categoriaId?: string;
  permiteExtras?: boolean;
  requiereConfiguracion?: boolean;
  controlaStock?: boolean;
}

/**
 * DTO de salida al crear una variante.
 * Refleja VarianteProductoResponse del backend.
 *
 * Incluye la variante recién creada y todas las hermanas del grupo.
 */
export interface VarianteResponse {
  varianteCreada: ProductoResponse;
  variantesDelGrupo: ProductoResponse[];
}
