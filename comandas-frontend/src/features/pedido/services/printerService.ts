/**
 * HU-29: Servicio de impresión térmica ESC/POS.
 *
 * Abstrae el hardware de impresión con 3 niveles de ejecución:
 *
 * 1. **Producción (Tauri)**: Envía bytes crudos vía `invoke('imprimir_ticket', { payload })`
 *    directamente al dispositivo USB/LAN configurado en Rust.
 *
 * 2. **Mock nivel 1 (navegador sin Tauri)**: Logea el contenido formateado en consola
 *    para debug visual durante desarrollo.
 *
 * 3. **Mock nivel 2 (descarga binaria)**: Genera un archivo .bin descargable con los
 *    bytes ESC/POS para inspección con herramientas de análisis de protocolo.
 *
 * **Logging**: En producción (Tauri), todos los eventos se persisten a archivo
 * vía tauri-plugin-log. Los logs quedan en el directorio de datos de la app
 * para diagnóstico remoto sin acceso a consola.
 *
 * Detección: usa `window.__TAURI_INTERNALS__` para detectar si corre dentro de Tauri.
 *
 * @see EscPosGenerator.java (backend) para la generación del buffer
 */

// ─── Logging persistido ───────────────────────────────────────────────────────

/**
 * Logger que persiste a archivo cuando corre en Tauri (vía tauri-plugin-log).
 * Fallback a console.* cuando no está disponible.
 *
 * En la PC del cliente no hay DevTools, así que estos logs van al mismo
 * archivo que los de Rust: ~/.local/share/com.meiser.foodflow/logs/
 */
const logger = {
  _tauriLog: null as null | typeof import('@tauri-apps/plugin-log'),
  _initialized: false,

  async _init() {
    if (this._initialized) return;
    this._initialized = true;
    if (!esTauri()) return;
    try {
      this._tauriLog = await import('@tauri-apps/plugin-log');
    } catch {
      // Plugin no disponible — fallback silencioso a console
    }
  },

  async info(msg: string) {
    await this._init();
    if (this._tauriLog) {
      this._tauriLog.info(msg);
    } else {
      console.info(msg);
    }
  },

  async error(msg: string) {
    await this._init();
    if (this._tauriLog) {
      this._tauriLog.error(msg);
    } else {
      console.error(msg);
    }
  },

  async warn(msg: string) {
    await this._init();
    if (this._tauriLog) {
      this._tauriLog.warn(msg);
    } else {
      console.warn(msg);
    }
  },

  async debug(msg: string) {
    await this._init();
    if (this._tauriLog) {
      this._tauriLog.debug(msg);
    } else {
      console.debug(msg);
    }
  },
};

// ─── Detección de entorno ─────────────────────────────────────────────────────

/**
 * Detecta si la app está corriendo dentro del runtime de Tauri.
 * Tauri v2 expone `window.__TAURI_INTERNALS__`.
 */
function esTauri(): boolean {
  return typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window;
}

// ─── Tipos ────────────────────────────────────────────────────────────────────

export type PrintMode = 'tauri' | 'mock-console' | 'mock-download';

export interface PrintResult {
  success: boolean;
  mode: PrintMode;
  message: string;
}

// ─── API pública ──────────────────────────────────────────────────────────────

/**
 * Envía un buffer ESC/POS (codificado en Base64) a la impresora térmica.
 *
 * Flujo:
 * 1. Decodifica Base64 → Uint8Array
 * 2. Si Tauri disponible → invoke('imprimir_ticket')
 * 3. Si no → mock según configuración
 *
 * @param base64 Buffer ESC/POS codificado en Base64 (viene del backend)
 * @param label Etiqueta descriptiva para logs (ej: "Comanda Mesa 5")
 * @returns Resultado de la impresión
 */
export async function imprimirEscPos(base64: string, label: string = 'Ticket'): Promise<PrintResult> {
  const bytes = base64ToUint8Array(base64);

  await logger.info(`[PrinterService] Inicio impresión: "${label}", ${bytes.length} bytes, entorno=${esTauri() ? 'tauri' : 'browser'}`);

  if (esTauri()) {
    return imprimirViaTauri(bytes, label);
  }

  // En navegador: mock de desarrollo
  return imprimirMockConsola(bytes, label);
}

/**
 * Descarga el buffer ESC/POS como archivo .bin para inspección.
 * Útil para debug del protocolo sin impresora física.
 */
export function descargarEscPos(base64: string, filename: string = 'comanda.bin'): void {
  const bytes = base64ToUint8Array(base64);
  // .slice() produce Uint8Array<ArrayBuffer> compatible con BlobPart (TS 5.7+)
  const blob = new Blob([bytes.slice()], { type: 'application/octet-stream' });
  const url = URL.createObjectURL(blob);

  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();

  URL.revokeObjectURL(url);
}

// ─── Implementaciones privadas ────────────────────────────────────────────────

/**
 * Envía bytes ESC/POS directamente a la impresora vía Tauri command.
 *
 * El comando Rust `imprimir_ticket` debe estar definido en src-tauri/src/
 * y manejar la conexión USB/LAN al dispositivo.
 */
async function imprimirViaTauri(bytes: Uint8Array, label: string): Promise<PrintResult> {
  try {
    await logger.info(`[PrinterService/Tauri] Invocando imprimir_ticket: "${label}", ${bytes.length} bytes`);
    await logger.debug(`[PrinterService/Tauri] Primeros 20 bytes: ${bytesToHex(bytes.slice(0, 20))}`);

    // Dynamic import de Tauri API para no romper en navegador
    const { invoke } = await import('@tauri-apps/api/core');

    const respuesta = await invoke<string>('imprimir_ticket', {
      payload: Array.from(bytes),
    });

    await logger.info(`[PrinterService/Tauri] OK: "${label}" — ${respuesta}`);

    return {
      success: true,
      mode: 'tauri',
      message: `${label} enviado a impresora`,
    };
  } catch (error) {
    const msg = error instanceof Error ? error.message : String(error);

    // Log detallado para diagnóstico remoto
    await logger.error(
      `[PrinterService/Tauri] FALLO al imprimir "${label}": ${msg}. ` +
      `Payload: ${bytes.length} bytes. Timestamp: ${new Date().toISOString()}`
    );

    return {
      success: false,
      mode: 'tauri',
      message: `Error al imprimir: ${msg}`,
    };
  }
}

/**
 * Mock de consola: renderiza el buffer ESC/POS como un ticket virtual de 48 columnas.
 *
 * Interpreta los comandos ESC/POS principales para simular visualmente
 * lo que saldría por la impresora térmica. Incluye:
 * - Alineación (izquierda, centro, derecha)
 * - Negrita (marcada con asteriscos)
 * - Doble tamaño (marcado con mayúsculas + padding)
 * - Corte de papel (línea de guiones)
 * - Apertura de cajón (notificación)
 *
 * El output usa %c CSS styling para simular fondo de ticket en la consola.
 */
function imprimirMockConsola(bytes: Uint8Array, label: string): PrintResult {
  const LINE_WIDTH = 48;
  const ticketLines: string[] = [];
  let currentLine = '';
  let align: 'left' | 'center' | 'right' = 'left';
  let bold = false;
  let doubleSize = false;

  function flushLine() {
    const effectiveWidth = doubleSize ? Math.floor(LINE_WIDTH / 2) : LINE_WIDTH;

    // Word-wrap: si el texto excede el ancho, dividir por palabras
    const rawLines: string[] = [];
    if (currentLine.length <= effectiveWidth) {
      rawLines.push(currentLine);
    } else {
      const words = currentLine.split(' ');
      let buf = '';
      for (const word of words) {
        if (buf.length === 0) {
          buf = word;
        } else if (buf.length + 1 + word.length <= effectiveWidth) {
          buf += ' ' + word;
        } else {
          rawLines.push(buf);
          buf = word;
        }
      }
      if (buf.length > 0) rawLines.push(buf);
    }

    for (let line of rawLines) {
      // Alineación
      if (align === 'center') {
        const pad = Math.max(0, Math.floor((effectiveWidth - line.length) / 2));
        line = ' '.repeat(pad) + line;
      } else if (align === 'right') {
        const pad = Math.max(0, effectiveWidth - line.length);
        line = ' '.repeat(pad) + line;
      }

      // Pad derecho para ancho fijo
      line = line.padEnd(effectiveWidth);

      // Marcar formato visual
      if (doubleSize) {
        line = `» ${line} «`;
      }
      if (bold) {
        line = `**${line.trimEnd()}**`;
      }

      ticketLines.push(line);
    }
    currentLine = '';
  }

  for (let i = 0; i < bytes.length; i++) {
    const byte = bytes[i];

    // LF (0x0A) → nueva línea
    if (byte === 0x0a) {
      flushLine();
      continue;
    }

    // ESC commands (0x1B)
    if (byte === 0x1b && i + 1 < bytes.length) {
      const cmd = bytes[i + 1];

      // ESC a n → alineación
      if (cmd === 0x61 && i + 2 < bytes.length) {
        const n = bytes[i + 2];
        if (n === 0) align = 'left';
        else if (n === 1) align = 'center';
        else if (n === 2) align = 'right';
        i += 2;
        continue;
      }

      // ESC E n → negrita on/off
      if (cmd === 0x45 && i + 2 < bytes.length) {
        bold = bytes[i + 2] === 1;
        i += 2;
        continue;
      }

      // ESC @ → inicializar impresora
      if (cmd === 0x40) {
        align = 'left';
        bold = false;
        doubleSize = false;
        i += 1;
        continue;
      }

      // Otro ESC command → skip 2 bytes
      i += 2;
      continue;
    }

    // GS commands (0x1D)
    if (byte === 0x1d && i + 1 < bytes.length) {
      const cmd = bytes[i + 1];

      // GS ! n → tamaño de caracter
      if (cmd === 0x21 && i + 2 < bytes.length) {
        doubleSize = bytes[i + 2] !== 0;
        i += 2;
        continue;
      }

      // GS V n → corte de papel
      if (cmd === 0x56 && i + 2 < bytes.length) {
        flushLine();
        ticketLines.push('✂' + '─'.repeat(LINE_WIDTH - 1));
        i += 2;
        continue;
      }

      // Otro GS command → skip 2 bytes
      i += 2;
      continue;
    }

    // ESC p → apertura de cajón
    if (byte === 0x1b && i + 1 < bytes.length && bytes[i + 1] === 0x70) {
      ticketLines.push('🔓 [APERTURA DE CAJÓN]');
      i += 4; // ESC p m t1 t2
      continue;
    }

    // Caracteres imprimibles
    if (byte >= 0x20 && byte <= 0xfe) {
      currentLine += String.fromCharCode(byte);
    }
  }

  // Flush última línea si quedó algo
  if (currentLine.length > 0) {
    flushLine();
  }

  // ── Renderizar en consola con estilo de ticket térmico ──
  const border = '┌' + '─'.repeat(LINE_WIDTH + 2) + '┐';
  const footer = '└' + '─'.repeat(LINE_WIDTH + 2) + '┘';

  const ticketText = [
    border,
    ...ticketLines.map((l) => `│ ${l.padEnd(LINE_WIDTH)} │`),
    footer,
  ].join('\n');

  console.log(
    `%c🖨️ MOCK IMPRESORA — ${label}%c\n\n%c${ticketText}%c\n\n📦 ${bytes.length} bytes | ${ticketLines.length} líneas`,
    'font-size: 14px; font-weight: bold; color: #ff4444;',
    '',
    'font-family: "Courier New", monospace; font-size: 12px; background: #fffde7; color: #333; padding: 8px; border-radius: 4px; line-height: 1.4;',
    '',
  );

  return {
    success: true,
    mode: 'mock-console',
    message: `${label} impreso en consola (mock)`,
  };
}

// ─── API pública: Diagnóstico ─────────────────────────────────────────────────

/**
 * Obtiene la ruta al directorio de logs de la app.
 * Útil para mostrarle al operador dónde están los archivos de diagnóstico
 * cuando necesita enviarlos para soporte técnico.
 *
 * @returns Ruta absoluta al directorio de logs, o null si no está en Tauri
 */
export async function obtenerRutaLogs(): Promise<string | null> {
  if (!esTauri()) return null;

  try {
    const { invoke } = await import('@tauri-apps/api/core');
    return await invoke<string>('obtener_ruta_logs');
  } catch {
    return null;
  }
}

// ─── Utilidades ───────────────────────────────────────────────────────────────

function base64ToUint8Array(base64: string): Uint8Array {
  const binaryString = atob(base64);
  const bytes = new Uint8Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
}

function bytesToHex(bytes: Uint8Array): string {
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join(' ');
}

/**
 * Extrae texto legible de un buffer ESC/POS ignorando bytes de control.
 * Útil para debugging: muestra lo que se imprimiría en la impresora.
 */
function extraerTextoLegible(bytes: Uint8Array): string {
  const lines: string[] = [];
  let currentLine = '';

  for (let i = 0; i < bytes.length; i++) {
    const byte = bytes[i];

    // Salto de línea → guardar línea actual
    if (byte === 0x0a) {
      if (currentLine.trim()) {
        lines.push(currentLine);
      }
      currentLine = '';
      continue;
    }

    // Ignorar comandos ESC (0x1B) y GS (0x1D)
    if (byte === 0x1b) {
      i += 2; // Skip ESC + command byte + param
      continue;
    }
    if (byte === 0x1d) {
      i += 2; // Skip GS + command + param
      continue;
    }

    // Caracteres imprimibles (32-126 + extended latin)
    if (byte >= 0x20 && byte <= 0xfe) {
      currentLine += String.fromCharCode(byte);
    }
  }

  if (currentLine.trim()) {
    lines.push(currentLine);
  }

  return lines.join('\n');
}

// ─── EscPosBuilder — Generador de tickets ESC/POS en frontend ─────────────────

/**
 * Builder liviano para construir buffers ESC/POS sin depender del backend.
 *
 * Replica el subconjunto del protocolo ESC/POS que usa EscPosGenerator.java:
 * - Inicialización (ESC @)
 * - Alineación (ESC a n)
 * - Negrita (ESC E n)
 * - Doble tamaño (GS ! n)
 * - Corte parcial (GS V 65 3)
 * - Apertura de cajón (ESC p)
 *
 * Codifica texto en ISO-8859-1 (Latin-1), el charset estándar del protocolo.
 * El resultado se obtiene como Base64 vía `toBase64()` para alimentar `imprimirEscPos()`.
 *
 * Ancho de papel: 48 columnas (80mm, Font A estándar).
 *
 * @example
 * const base64 = new EscPosBuilder()
 *   .centrado().negrita(true).linea('EGRESO DE CAJA').negrita(false)
 *   .separador()
 *   .izquierda().lineaDosColumnas('Monto:', '$500.00')
 *   .cortePapel()
 *   .toBase64();
 * await imprimirEscPos(base64, 'Egreso EGR-20260303-001');
 */
export class EscPosBuilder {
  private static readonly LINE_WIDTH = 48;

  // Comandos ESC/POS
  private static readonly INIT = [0x1b, 0x40];
  private static readonly ALIGN_LEFT = [0x1b, 0x61, 0x00];
  private static readonly ALIGN_CENTER = [0x1b, 0x61, 0x01];
  private static readonly BOLD_ON = [0x1b, 0x45, 0x01];
  private static readonly BOLD_OFF = [0x1b, 0x45, 0x00];
  private static readonly DOUBLE_SIZE_ON = [0x1d, 0x21, 0x11];
  private static readonly DOUBLE_SIZE_OFF = [0x1d, 0x21, 0x00];
  private static readonly LF = [0x0a];
  private static readonly CUT_PAPER = [0x1d, 0x56, 0x41, 0x03];
  private static readonly OPEN_DRAWER = [0x1b, 0x70, 0x00, 0x19, 0xfa];

  private bytes: number[] = [];

  constructor() {
    this.bytes.push(...EscPosBuilder.INIT);
  }

  centrado(): this {
    this.bytes.push(...EscPosBuilder.ALIGN_CENTER);
    return this;
  }

  izquierda(): this {
    this.bytes.push(...EscPosBuilder.ALIGN_LEFT);
    return this;
  }

  negrita(on: boolean): this {
    this.bytes.push(...(on ? EscPosBuilder.BOLD_ON : EscPosBuilder.BOLD_OFF));
    return this;
  }

  tamanoDoble(on: boolean): this {
    this.bytes.push(...(on ? EscPosBuilder.DOUBLE_SIZE_ON : EscPosBuilder.DOUBLE_SIZE_OFF));
    return this;
  }

  /** Imprime una línea de texto + salto de línea. */
  linea(texto: string): this {
    for (let i = 0; i < texto.length; i++) {
      // ISO-8859-1: truncar a byte (0x00–0xFF)
      this.bytes.push(texto.charCodeAt(i) & 0xff);
    }
    this.bytes.push(...EscPosBuilder.LF);
    return this;
  }

  lineaVacia(): this {
    this.bytes.push(...EscPosBuilder.LF);
    return this;
  }

  /** Línea de guiones (separador sencillo). */
  separador(): this {
    return this.linea('-'.repeat(EscPosBuilder.LINE_WIDTH));
  }

  /** Línea de signos igual (separador doble). */
  separadorDoble(): this {
    return this.linea('='.repeat(EscPosBuilder.LINE_WIDTH));
  }

  /** Imprime dos textos alineados a los extremos de la línea. */
  lineaDosColumnas(izq: string, der: string): this {
    const espacios = Math.max(1, EscPosBuilder.LINE_WIDTH - izq.length - der.length);
    return this.linea(izq + ' '.repeat(espacios) + der);
  }

  /** 3 líneas vacías + corte parcial. */
  cortePapel(): this {
    this.lineaVacia();
    this.lineaVacia();
    this.lineaVacia();
    this.bytes.push(...EscPosBuilder.CUT_PAPER);
    return this;
  }

  /** Pulso al pin del cajón de dinero. */
  abrirCajon(): this {
    this.bytes.push(...EscPosBuilder.OPEN_DRAWER);
    return this;
  }

  /** Retorna el buffer como Base64 listo para `imprimirEscPos()`. */
  toBase64(): string {
    const uint8 = new Uint8Array(this.bytes);
    let binary = '';
    for (let i = 0; i < uint8.length; i++) {
      binary += String.fromCharCode(uint8[i]);
    }
    return btoa(binary);
  }
}
