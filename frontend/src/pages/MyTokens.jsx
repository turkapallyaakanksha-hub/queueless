import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { STATUS_LABELS, formatDateTime } from '../format.js';
import Ticket from '../components/Ticket.jsx';
import { usePolling } from '../usePolling.js';

export default function MyTokens() {
  const { data, error, loading, refresh } = usePolling(() => api('/api/tokens/my'), 5000);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState('');

  async function cancel(token) {
    if (!window.confirm(`Cancel token #${token.tokenNumber} for ${token.serviceName}?`)) return;
    setBusy(true);
    setActionError('');
    try {
      await api(`/api/tokens/${token.id}/cancel`, { method: 'POST' });
      await refresh();
    } catch (err) {
      setActionError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const active = (data || []).filter((t) => t.status === 'WAITING' || t.status === 'SERVING');

  return (
    <section className="page-section">
      <h1>My tokens</h1>
      {loading && <p>Loading your tokens...</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      {actionError && <p className="form-error" role="alert">{actionError}</p>}

      {data && active.length > 0 && (
        <div className="ticket-stack">
          {active.map((t) => <Ticket key={t.id} token={t} onCancel={cancel} busy={busy} />)}
        </div>
      )}

      {data && data.length === 0 && (
        <p className="empty">
          You have not taken a token yet. <Link to="/take-token">Take your first token</Link>.
        </p>
      )}

      {data && data.length > 0 && (
        <>
          <h2>Token history</h2>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Token</th><th>Service</th><th>Status</th><th>Taken at</th></tr>
              </thead>
              <tbody>
                {data.map((t) => (
                  <tr key={t.id}>
                    <td className="cell-number">#{t.tokenNumber}</td>
                    <td>{t.serviceName}</td>
                    <td><span className={`status status-${t.status.toLowerCase()}`}>{STATUS_LABELS[t.status]}</span></td>
                    <td>{formatDateTime(t.createdTime)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  );
}
