use tauri::Manager;
use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandChild;
use std::sync::Mutex;
use log::{info, error, warn, debug};

mod impresion;

struct AppState {
    backend_process: Mutex<Option<CommandChild>>,
}

// ─── Comandos Tauri: Impresión térmica ESC/POS ───────────────────────────────

/// Envía bytes ESC/POS a la impresora térmica configurada.
/// Invocado desde el frontend como: invoke('imprimir_ticket', { payload: [...] })
#[tauri::command]
fn imprimir_ticket(app: tauri::AppHandle, payload: Vec<u8>) -> Result<String, String> {
    info!(
        "[Cmd:imprimir_ticket] Recibido payload de {} bytes desde frontend",
        payload.len()
    );
    let config = impresion::load_config(&app);
    let resultado = impresion::enviar_a_impresora(&config, &payload);

    if let Err(ref e) = resultado {
        error!("[Cmd:imprimir_ticket] FALLO: {}", e);
    }

    resultado
}

/// Persiste la configuración de la impresora (tipo conexión, ruta USB, host TCP, puerto).
#[tauri::command]
fn configurar_impresora(
    app: tauri::AppHandle,
    config: impresion::PrinterConfig,
) -> Result<String, String> {
    info!(
        "[Cmd:configurar_impresora] tipo={}, usb={}, tcp={}:{}",
        config.connection_type, config.usb_path, config.tcp_host, config.tcp_port
    );
    impresion::save_config(&app, &config)?;
    Ok("Configuración de impresora guardada".into())
}

/// Devuelve la configuración actual de la impresora (o defaults si no existe).
#[tauri::command]
fn obtener_config_impresora(app: tauri::AppHandle) -> impresion::PrinterConfig {
    debug!("[Cmd:obtener_config_impresora] Solicitada desde frontend");
    impresion::load_config(&app)
}

/// Devuelve la ruta al directorio de logs para que el frontend pueda
/// mostrársela al operador cuando necesite enviar logs de diagnóstico.
#[tauri::command]
fn obtener_ruta_logs(app: tauri::AppHandle) -> Result<String, String> {
    let log_dir = app
        .path()
        .app_log_dir()
        .map_err(|e| format!("No se pudo resolver directorio de logs: {}", e))?;

    let ruta = log_dir.to_string_lossy().to_string();
    info!("[Cmd:obtener_ruta_logs] Ruta de logs: {}", ruta);
    Ok(ruta)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
  tauri::Builder::default()
    .plugin(tauri_plugin_store::Builder::default().build())
    .plugin(tauri_plugin_shell::init())
    .manage(AppState {
        backend_process: Mutex::new(None),
    })
    .setup(|app| {
      // Logging persistido a archivo — SIEMPRE activo (debug y release).
      // En la PC del cliente no hay consola de Tauri, así que los logs
      // se escriben a disco para poder diagnosticar problemas de impresora,
      // backend, etc. sin acceso remoto.
      //
      // Ubicación del archivo:
      //   Linux:   ~/.local/share/com.meiser.foodflow/logs/
      //   Windows: %APPDATA%/com.meiser.foodflow/logs/
      {
        use tauri_plugin_log::{Target, TargetKind, TimezoneStrategy, RotationStrategy};

        let log_level = if cfg!(debug_assertions) {
          log::LevelFilter::Debug
        } else {
          log::LevelFilter::Info
        };

        app.handle().plugin(
          tauri_plugin_log::Builder::default()
            .level(log_level)
            .timezone_strategy(TimezoneStrategy::UseLocal)
            .rotation_strategy(RotationStrategy::KeepAll)
            .max_file_size(5_000_000) // 5 MB por archivo
            .targets([
              // Archivo en disco (persistido)
              Target::new(TargetKind::LogDir { file_name: Some("foodflow".into()) }),
              // Stdout para debug local
              Target::new(TargetKind::Stdout),
              // Webview console (visible en DevTools si están habilitados)
              Target::new(TargetKind::Webview),
            ])
            .build(),
        )?;
      }

      // Lanzar backend como sidecar
      let app_handle = app.handle().clone();
      tauri::async_runtime::spawn(async move {
        match start_backend(&app_handle).await {
          Ok(_) => info!("[Backend] Sidecar iniciado correctamente"),
          Err(e) => error!("[Backend] Error al iniciar sidecar: {}", e),
        }
      });

      Ok(())
    })
    .invoke_handler(tauri::generate_handler![
      imprimir_ticket,
      configurar_impresora,
      obtener_config_impresora,
      obtener_ruta_logs,
    ])
    .on_window_event(|window, event| {
      if let tauri::WindowEvent::CloseRequested { .. } = event {
        let app_handle = window.app_handle();
        if let Some(state) = app_handle.try_state::<AppState>() {
          if let Ok(mut backend) = state.backend_process.lock() {
            if let Some(mut child) = backend.take() {
              let _ = child.kill();
              info!("[Backend] Proceso detenido por cierre de ventana");
            }
          }
        }
      }
    })
    .run(tauri::generate_context!())
    .expect("error while running tauri application");
}

async fn start_backend(app: &tauri::AppHandle) -> Result<(), Box<dyn std::error::Error>> {
  use tauri_plugin_shell::process::CommandEvent;

  let shell = app.shell();
  
  // Usar el nombre base, Tauri agregará automáticamente el target triple
  let sidecar_name = "backend";

  info!("[Backend] Intentando iniciar sidecar: {}", sidecar_name);

  let (mut rx, child) = shell
    .sidecar(sidecar_name)?
    .spawn()?;

  // Guardar referencia al proceso
  if let Some(state) = app.try_state::<AppState>() {
    if let Ok(mut backend) = state.backend_process.lock() {
      *backend = Some(child);
    }
  }

  // Escuchar eventos del proceso
  tauri::async_runtime::spawn(async move {
    while let Some(event) = rx.recv().await {
      match event {
        CommandEvent::Stdout(line) => {
          info!("[Backend STDOUT] {}", String::from_utf8_lossy(&line));
        }
        CommandEvent::Stderr(line) => {
          warn!("[Backend STDERR] {}", String::from_utf8_lossy(&line));
        }
        CommandEvent::Error(err) => {
          error!("[Backend ERROR] {}", err);
        }
        CommandEvent::Terminated(payload) => {
          warn!("[Backend] Proceso terminado: {:?}", payload);
        }
        _ => {}
      }
    }
  });

  // Esperar un poco para que el backend arranque
  tokio::time::sleep(tokio::time::Duration::from_secs(5)).await;
  info!("[Backend] Sidecar debería estar listo en http://localhost:8080");

  Ok(())
}

