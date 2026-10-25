import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { minutesLabel } from '../format.js';
import LoadBadge from '../components/LoadBadge.jsx';
import Ticket from '../components/Ticket.jsx';
import { usePolling } from '../usePolling.js';

const ACTIVE = ['WAITING', 'SERVING'];

export default function TakeToken() {
  const [params] = useSearchParams();
  const services = usePolling(() => api('/api/services'), 8000);
  const mine = usePolling(() => api('/api/tokens/my'), 5000);

  const [selected, setSelected] = useState(params.get('service') || '');
  const [created, setCreated] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const activeServiceIds = new Set(
    (mine.data || []).filter((t) => ACTIVE.includes(t.status)).map((t) => String(t.serviceId))
  );
  // Prefer the live copy of the token (updates every few seconds) over the one returned at creation.
  const ticket = created ? (mine.data || []).find((t) => t.id === created.id) || created : null;

  async function submit(e) {
    e.preventDefault();
    if (!selected) {
      setError('Choose a counter first.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      const token = await api('/api/tokens', { method: 'POST', body: { serviceId: Number(selected) } });
      setCreated(token);
      mine.refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function cancel(token) {
    if (!window.confirm(`Cancel token #${token.tokenNumber} for ${token.serviceName}?`)) return;
    setBusy(true);
    try {
      await api(`/api/tokens/${token.id}/cancel`, { method: 'POST' });
      await mine.refresh();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  if (ticket) {
    return (
      <section className="page-section">
        <h1>Your token</h1>
        <Ticket token={ticket} onCancel={cancel} busy={busy} />
        {error && <p className="form-error" role="alert">{error}</p>}
        <div className="row-actions">
          <button type="button" className="btn btn-primary" onClick={() => { setCreated(null); setSelected(''); }}>
            Take another token
          </button>
          <Link className="btn btn-quiet" to="/my-tokens">See all my tokens</Link>
        </div>
      </section>
    );
  }

  return (
    <section className="page-section">
      <h1>Take a token</h1>
      <p className="lead">Choose the counter you need. You will get a number and a live wait estimate.</p>

      {services.loading && <p>Loading counters...</p>}
      {services.error && <p className="form-error" role="alert">{services.error}</p>}

      {services.data && (
        <form onSubmit={submit}>
          <fieldset className="choices">
            <legend>Counter</legend>
            {services.data.map((s) => {
              const hasToken = activeServiceIds.has(String(s.id));
              return (
                <label key={s.id} className={`choice ${selected === String(s.id) ? 'choice-on' : ''} ${hasToken ? 'choice-off' : ''}`}>
                  <input
                    type="radio"
                    name="service"
                    value={s.id}
                    checked={selected === String(s.id)}
                    disabled={hasToken}
                    onChange={() => setSelected(String(s.id))}
                  />
                  <span className="choice-text">
                    <strong>{s.serviceName}</strong>
                    <small>
                      {hasToken
                        ? 'You already have a token here. Find it under My tokens.'
                        : `${minutesLabel(s.estimatedWaitMinutes)} if you join now`}
                    </small>
                  </span>
                  <LoadBadge load={s.load} waiting={s.waitingCount} />
                </label>
              );
            })}
          </fieldset>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Getting your token...' : 'Get token'}</button>
        </form>
      )}
    </section>
  );
}
