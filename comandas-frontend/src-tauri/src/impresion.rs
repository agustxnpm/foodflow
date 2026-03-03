//! HU-29: Módulo de impresión térmica ESC/POS.
//!
//! Soporta dos modos de conexión:
//! - **USB**: Escritura directa al dispositivo (ej: `/dev/usb/lp0` en Linux)
//! - **TCP**: Conexión de red al puerto estándar ESC/POS (9100)
//!
//! La configuración se persiste en `printer_config.json` dentro del
//! directorio de datos de la app (gestionado por Tauri).
//!
//! **Logging**: Todas las operaciones se loguean a archivo vía tauri-plugin-log.
//! Los logs quedan en el directorio de datos de la app para diagnóstico
//! en producción (la PC del cliente no tiene consola de Tauri).

use log::{info, warn, error, debug};
use serde::{Deserialize, Serialize};
use std::io::Write;
use std::path::PathBuf;
use tauri::Manager;

// ─── Configuración ────────────────────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct PrinterConfig {
    /// Tipo de conexión: "usb" o "tcp"
    #[serde(default = "default_connection_type")]
    pub connection_type: String,

    /// Ruta al dispositivo USB (Linux: /dev/usb/lp0, Windows: \\.\USB001)
    #[serde(default = "default_usb_path")]
    pub usb_path: String,

    /// Host de la impresora de red
    #[serde(default = "default_tcp_host")]
    pub tcp_host: String,

    /// Puerto de la impresora de red (estándar ESC/POS: 9100)
    #[serde(default = "default_tcp_port")]
    pub tcp_port: u16,
}

fn default_connection_type() -> String {
    "usb".into()
}
fn default_usb_path() -> String {
    "/dev/usb/lp0".into()
}
fn default_tcp_host() -> String {
    "192.168.1.100".into()
}
fn default_tcp_port() -> u16 {
    9100
}

impl Default for PrinterConfig {
    fn default() -> Self {
        Self {
            connection_type: default_connection_type(),
            usb_path: default_usb_path(),
            tcp_host: default_tcp_host(),
            tcp_port: default_tcp_port(),
        }
    }
}

// ─── Persistencia de configuración ────────────────────────────────────────────

fn config_path(app: &tauri::AppHandle) -> Result<PathBuf, String> {
    app.path()
        .app_data_dir()
        .map(|dir| dir.join("printer_config.json"))
        .map_err(|e| format!("No se pudo obtener directorio de datos: {}", e))
}

pub fn load_config(app: &tauri::AppHandle) -> PrinterConfig {
    let path = match config_path(app) {
        Ok(p) => p,
        Err(e) => {
            warn!("[Impresora] No se pudo resolver ruta de config: {}. Usando defaults.", e);
            return PrinterConfig::default();
        }
    };

    debug!("[Impresora] Leyendo config de: {:?}", path);

    match std::fs::read_to_string(&path) {
        Ok(json) => match serde_json::from_str::<PrinterConfig>(&json) {
            Ok(config) => {
                info!(
                    "[Impresora] Config cargada: tipo={}, usb_path={}, tcp={}:{}",
                    config.connection_type, config.usb_path, config.tcp_host, config.tcp_port
                );
                config
            }
            Err(e) => {
                error!(
                    "[Impresora] Config corrupta en {:?}: {}. Contenido: {}. Usando defaults.",
                    path, e, json.chars().take(200).collect::<String>()
                );
                PrinterConfig::default()
            }
        },
        Err(e) => {
            info!(
                "[Impresora] No existe config en {:?} ({}). Usando defaults: usb={}",
                path, e, default_usb_path()
            );
            PrinterConfig::default()
        }
    }
}

pub fn save_config(app: &tauri::AppHandle, config: &PrinterConfig) -> Result<(), String> {
    let path = config_path(app)?;

    info!(
        "[Impresora] Guardando config en {:?}: tipo={}, usb={}, tcp={}:{}",
        path, config.connection_type, config.usb_path, config.tcp_host, config.tcp_port
    );

    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent).map_err(|e| {
            let msg = format!(
                "No se pudo crear directorio {:?}: {}. Permisos del FS?",
                parent, e
            );
            error!("[Impresora] {}", msg);
            msg
        })?;
    }

    let json = serde_json::to_string_pretty(config).map_err(|e| {
        let msg = format!("Error al serializar configuración: {}", e);
        error!("[Impresora] {}", msg);
        msg
    })?;

    std::fs::write(&path, &json).map_err(|e| {
        let msg = format!("No se pudo escribir en {:?}: {}", path, e);
        error!("[Impresora] {}", msg);
        msg
    })?;

    info!("[Impresora] Config guardada exitosamente");
    Ok(())
}

// ─── Envío a impresora ────────────────────────────────────────────────────────

pub fn enviar_a_impresora(config: &PrinterConfig, payload: &[u8]) -> Result<String, String> {
    info!(
        "[Impresora] Inicio envío: {} bytes, conexión={}, usb_path={}, tcp={}:{}",
        payload.len(),
        config.connection_type,
        config.usb_path,
        config.tcp_host,
        config.tcp_port
    );

    if payload.is_empty() {
        let msg = "Payload ESC/POS vacío — nada que imprimir";
        warn!("[Impresora] {}", msg);
        return Err(msg.into());
    }

    // Log primeros bytes para diagnóstico de protocolo
    let preview: Vec<String> = payload.iter().take(20).map(|b| format!("{:02X}", b)).collect();
    debug!("[Impresora] Primeros bytes: [{}]", preview.join(" "));

    let resultado = match config.connection_type.as_str() {
        "usb" => enviar_usb(&config.usb_path, payload),
        "tcp" => enviar_tcp(&config.tcp_host, config.tcp_port, payload),
        otro => {
            let msg = format!(
                "Tipo de conexión no soportado: '{}'. Usá 'usb' o 'tcp'.",
                otro
            );
            error!("[Impresora] {}", msg);
            Err(msg)
        }
    };

    match &resultado {
        Ok(msg) => info!("[Impresora] Envío exitoso: {}", msg),
        Err(msg) => error!("[Impresora] Envío fallido: {}", msg),
    }

    resultado
}

/// Escribe bytes ESC/POS directamente al dispositivo USB.
///
/// En Linux, la impresora térmica aparece como `/dev/usb/lp0`.
/// Requiere que el usuario tenga permisos sobre el dispositivo
/// (grupo `lp`: `sudo usermod -a -G lp $USER`).
fn enviar_usb(path: &str, payload: &[u8]) -> Result<String, String> {
    info!("[Impresora/USB] Abriendo dispositivo: {}", path);

    // Verificar que el dispositivo existe antes de intentar abrir
    let device_path = std::path::Path::new(path);
    if !device_path.exists() {
        let msg = format!(
            "El dispositivo '{}' no existe. Posibles causas:\n\
             - La impresora no está conectada por USB\n\
             - El driver no creó el device node\n\
             - La ruta configurada es incorrecta\n\
             Verificar con: ls -la /dev/usb/",
            path
        );
        error!("[Impresora/USB] {}", msg);
        return Err(msg);
    }

    debug!("[Impresora/USB] Dispositivo existe, abriendo para escritura...");

    let mut file = std::fs::OpenOptions::new()
        .write(true)
        .open(path)
        .map_err(|e| {
            let msg = format!(
                "No se pudo abrir '{}': {} (kind: {:?}). Posibles causas:\n\
                 - Sin permisos (ejecutar: sudo usermod -a -G lp $USER y reiniciar sesión)\n\
                 - Dispositivo ocupado por otro proceso\n\
                 - Driver USB no cargado",
                path, e, e.kind()
            );
            error!("[Impresora/USB] {}", msg);
            msg
        })?;

    debug!("[Impresora/USB] Dispositivo abierto, escribiendo {} bytes...", payload.len());

    file.write_all(payload).map_err(|e| {
        let msg = format!(
            "Error escribiendo en '{}': {} (kind: {:?}). \
             La impresora puede haberse desconectado durante la escritura.",
            path, e, e.kind()
        );
        error!("[Impresora/USB] {}", msg);
        msg
    })?;

    file.flush().map_err(|e| {
        let msg = format!(
            "Error en flush '{}': {} (kind: {:?}). \
             Datos parcialmente enviados — verificar impresión.",
            path, e, e.kind()
        );
        error!("[Impresora/USB] {}", msg);
        msg
    })?;

    let msg = format!("Impreso vía USB [{}] ({} bytes)", path, payload.len());
    info!("[Impresora/USB] {}", msg);
    Ok(msg)
}

/// Envía bytes ESC/POS a la impresora mediante conexión TCP.
///
/// Puerto estándar ESC/POS: 9100.
/// Timeout de conexión: 5 segundos.
fn enviar_tcp(host: &str, port: u16, payload: &[u8]) -> Result<String, String> {
    use std::net::TcpStream;
    use std::time::Duration;

    let addr = format!("{}:{}", host, port);
    info!("[Impresora/TCP] Conectando a {} (timeout: 5s)...", addr);

    let socket_addr: std::net::SocketAddr = addr.parse().map_err(|e| {
        let msg = format!(
            "Dirección inválida '{}': {}. Verificá host y puerto en la configuración.",
            addr, e
        );
        error!("[Impresora/TCP] {}", msg);
        msg
    })?;

    let mut stream =
        TcpStream::connect_timeout(&socket_addr, Duration::from_secs(5)).map_err(|e| {
            let msg = format!(
                "No se pudo conectar a '{}': {} (kind: {:?}). Posibles causas:\n\
                 - Impresora apagada o desconectada de la red\n\
                 - IP incorrecta (verificar con ping {})\n\
                 - Puerto {} bloqueado por firewall\n\
                 - Impresora en otra subred",
                addr, e, e.kind(), host, port
            );
            error!("[Impresora/TCP] {}", msg);
            msg
        })?;

    debug!("[Impresora/TCP] Conexión establecida, enviando {} bytes...", payload.len());

    stream.write_all(payload).map_err(|e| {
        let msg = format!(
            "Error enviando datos a '{}': {} (kind: {:?}). \
             Conexión interrumpida durante la escritura.",
            addr, e, e.kind()
        );
        error!("[Impresora/TCP] {}", msg);
        msg
    })?;

    stream.flush().map_err(|e| {
        let msg = format!(
            "Error en flush '{}': {} (kind: {:?}). \
             Datos parcialmente enviados — verificar impresión.",
            addr, e, e.kind()
        );
        error!("[Impresora/TCP] {}", msg);
        msg
    })?;

    let msg = format!("Impreso vía TCP {} ({} bytes)", addr, payload.len());
    info!("[Impresora/TCP] {}", msg);
    Ok(msg)
}
