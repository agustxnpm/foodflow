use tauri::Manager;
use std::sync::Mutex;
use log::{info, error, warn, debug};

#[cfg(target_os = "windows")]
use std::os::windows::process::CommandExt;

mod impresion;

struct AppState {
    backend_process: Mutex<Option<tokio::process::Child>>,
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

      // Lanzar backend Java embebido
      let app_handle = app.handle().clone();
      tauri::async_runtime::spawn(async move {
        match start_backend(&app_handle).await {
          Ok(_) => info!("[Backend] Backend iniciado correctamente"),
          Err(e) => error!("[Backend] Error al iniciar backend: {}", e),
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
              let _ = child.start_kill();
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
  use tokio::io::{AsyncBufReadExt, BufReader};

  // ── Resolver rutas de JRE y JAR desde el directorio de recursos ────────
  let resource_dir = app.path().resource_dir()
    .map_err(|e| format!("No se pudo resolver resource_dir: {}", e))?;

  // En Windows usamos javaw.exe para evitar que se abra una ventana de consola.
  // javaw es el binario headless de Java; el usuario no puede cerrarlo por error.
  let java_bin = if cfg!(windows) {
    resource_dir.join("jre").join("bin").join("javaw.exe")
  } else {
    resource_dir.join("jre").join("bin").join("java")
  };

  let java_cmd = if java_bin.exists() {
    info!("[Backend] Usando JRE embebido: {:?}", java_bin);
    strip_extended_path_prefix(&java_bin.to_string_lossy())
  } else {
    info!("[Backend] JRE embebido no encontrado, usando Java del sistema");
    "java".to_string()
  };

  let jar_path = resource_dir.join("binaries").join("backend.jar");
  if !jar_path.exists() {
    return Err(format!("backend.jar no encontrado en: {:?}", jar_path).into());
  }
  // Windows: resource_dir() devuelve paths con prefijo \\?\ (extended-length)
  // que Java no puede manejar para resolver el classpath dentro del JAR.
  // Stripeamos ese prefijo para que JarLauncher funcione correctamente.
  let jar_path_clean = strip_extended_path_prefix(&jar_path.to_string_lossy());

  // ── Resolver directorio de datos de la aplicación ──────────────────────
  // El archivo SQLite vive en la carpeta que el SO reserva para datos de
  // aplicaciones, NO junto al ejecutable / JAR.
  //   Linux:   ~/.local/share/com.meiser.foodflow/
  //   Windows: %APPDATA%/com.meiser.foodflow/
  //   macOS:   ~/Library/Application Support/com.meiser.foodflow/
  let data_dir = app.path().app_data_dir()
    .map_err(|e| format!("No se pudo resolver app_data_dir: {}", e))?;
  std::fs::create_dir_all(&data_dir)?;

  let db_path = data_dir.join("comandas.db");
  // Normalizar separadores a '/' para compatibilidad JDBC en todos los OS
  let db_path_str = db_path.to_string_lossy().replace('\\', "/");
  info!("[Backend] Ruta SQLite: {}", db_path_str);

  info!("[Backend] Iniciando: {} -jar {}", java_cmd, jar_path_clean);

  let mut command = tokio::process::Command::new(&java_cmd);
  command
    .arg("-Dspring.profiles.active=offline")
    .arg("-jar")
    .arg(&jar_path_clean)
    .env("FOODFLOW_DB_PATH", &db_path_str)
    .stdout(std::process::Stdio::piped())
    .stderr(std::process::Stdio::piped())
    .kill_on_drop(true);

  // En Windows: CREATE_NO_WINDOW evita la ventana de consola residual
  // incluso si javaw no la crea por sí mismo (doble seguridad).
  #[cfg(target_os = "windows")]
  {
    const CREATE_NO_WINDOW: u32 = 0x08000000;
    command.creation_flags(CREATE_NO_WINDOW);
  }

  let mut child = command.spawn()?;

  // Forward stdout
  if let Some(stdout) = child.stdout.take() {
    tauri::async_runtime::spawn(async move {
      let reader = BufReader::new(stdout);
      let mut lines = reader.lines();
      while let Ok(Some(line)) = lines.next_line().await {
        info!("[Backend STDOUT] {}", line);
      }
    });
  }

  // Forward stderr
  if let Some(stderr) = child.stderr.take() {
    tauri::async_runtime::spawn(async move {
      let reader = BufReader::new(stderr);
      let mut lines = reader.lines();
      while let Ok(Some(line)) = lines.next_line().await {
        warn!("[Backend STDERR] {}", line);
      }
    });
  }

  // Guardar referencia al proceso para kill on close
  if let Some(state) = app.try_state::<AppState>() {
    if let Ok(mut backend) = state.backend_process.lock() {
      *backend = Some(child);
    }
  }

  // Esperar un poco para que el backend arranque
  tokio::time::sleep(tokio::time::Duration::from_secs(5)).await;
  info!("[Backend] Backend debería estar listo en http://localhost:8080");

  Ok(())
}

/// Elimina el prefijo `\\?\` de rutas en Windows.
///
/// `tauri::path::resource_dir()` y similares devuelven rutas con el prefijo
/// de extended-length path de Windows (`\\?\C:\...`). Java no maneja este
/// prefijo al resolver el classpath dentro del JAR, provocando
/// `ClassNotFoundException: JarLauncher`.
fn strip_extended_path_prefix(path: &str) -> String {
  if cfg!(windows) {
    path.strip_prefix(r"\\?\").unwrap_or(path).to_string()
  } else {
    path.to_string()
  }
}

