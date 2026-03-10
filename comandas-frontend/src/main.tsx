import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { AppRouter } from './routes/AppRouter'

// ── Captura global de errores JS no manejados ─────────────────────────────
// Estos handlers atrapan errores que escapan de React y de Promises,
// y los imprimen en la consola de Tauri (visible en DevTools y en logs de archivo).

window.onerror = (message, source, lineno, colno, error) => {
  console.error(
    '[Global JS Error]',
    `\nMessage: ${message}`,
    `\nSource: ${source}:${lineno}:${colno}`,
    `\nStack: ${error?.stack ?? '(sin stack)'}`
  );
};

window.onunhandledrejection = (event: PromiseRejectionEvent) => {
  const reason = event.reason;
  console.error(
    '[Unhandled Promise Rejection]',
    `\nReason:`, reason,
    `\nStack: ${reason?.stack ?? '(sin stack)'}`
  );
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppRouter />
  </StrictMode>,
)
