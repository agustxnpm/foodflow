use tauri::Manager;
use tauri_plugin_shell::ShellExt;
use tauri_plugin_shell::process::CommandChild;
use std::sync::Mutex;

struct AppState {
    backend_process: Mutex<Option<CommandChild>>,
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
  tauri::Builder::default()
    .plugin(tauri_plugin_shell::init())
    .manage(AppState {
        backend_process: Mutex::new(None),
    })
    .setup(|app| {
      if cfg!(debug_assertions) {
        app.handle().plugin(
          tauri_plugin_log::Builder::default()
            .level(log::LevelFilter::Info)
            .build(),
        )?;
      }

      // Lanzar backend como sidecar
      let app_handle = app.handle().clone();
      tauri::async_runtime::spawn(async move {
        match start_backend(&app_handle).await {
          Ok(_) => println!("Backend iniciado correctamente"),
          Err(e) => eprintln!("Error al iniciar backend: {}", e),
        }
      });

      Ok(())
    })
    .on_window_event(|window, event| {
      if let tauri::WindowEvent::CloseRequested { .. } = event {
        let app_handle = window.app_handle();
        if let Some(state) = app_handle.try_state::<AppState>() {
          if let Ok(mut backend) = state.backend_process.lock() {
            if let Some(mut child) = backend.take() {
              let _ = child.kill();
              println!("Backend detenido");
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

  println!("Intentando iniciar sidecar: {}", sidecar_name);

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
          println!("[Backend STDOUT] {}", String::from_utf8_lossy(&line));
        }
        CommandEvent::Stderr(line) => {
          eprintln!("[Backend STDERR] {}", String::from_utf8_lossy(&line));
        }
        CommandEvent::Error(err) => {
          eprintln!("[Backend ERROR] {}", err);
        }
        CommandEvent::Terminated(payload) => {
          println!("[Backend] Proceso terminado: {:?}", payload);
        }
        _ => {}
      }
    }
  });

  // Esperar un poco para que el backend arranque
  tokio::time::sleep(tokio::time::Duration::from_secs(5)).await;
  println!("Backend debería estar listo en http://localhost:8080");

  Ok(())
}

