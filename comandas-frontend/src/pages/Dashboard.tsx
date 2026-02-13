import { useState } from 'react';
import { api } from '../api/client';
import './Dashboard.css';

export function Dashboard() {
  const [status, setStatus] = useState<string>('');
  const [loading, setLoading] = useState(false);

  const checkBackend = async () => {
    setLoading(true);
    setStatus('');
    try {
      await api.get('/mesas');
      setStatus('Backend conectado ✓');
    } catch (error) {
      setStatus('Backend no disponible ✗');
      console.error('Error al conectar con backend:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="dashboard">
      <h1>Dashboard</h1>
      <div className="connection-test">
        <button onClick={checkBackend} disabled={loading}>
          {loading ? 'Verificando...' : 'Verificar Conexión'}
        </button>
        {status && (
          <p className={status.includes('✓') ? 'status-success' : 'status-error'}>
            {status}
          </p>
        )}
      </div>
    </div>
  );
}
