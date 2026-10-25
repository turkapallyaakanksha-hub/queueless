import { useState } from 'react';
import { api } from '../api.js';
import { formatClock } from '../format.js';
import LoadBadge from '../components/LoadBadge.jsx';
import { usePolling } from '../usePolling.js';

export default function Admin() {
  const { data, setData, error, loading, refresh } = usePolling(() => api('/api/admin/queues'), 4000);
  const [busyId, setBusyId] = useState(null);
  const [actionError, setActionError] = useState('');

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [averageTime, setAverageTime] = useState(3);
  const [adding, setAdding] = useState(false);

  // Each admin action returns the updated queue for that counter, so the screen changes instantly.
  async function run(serviceId, path, options = { method: 'POST' }) {
    setBusyId(serviceId);
    setActionError('');
    try {
      const updated = await api(path, options);
      setData((current) => (current || []).map((q) => (q.serviceId === updated.serviceId ? updated : q)));
    } catch (err) {
      setActionError(err.message);
      refresh();
    } finally {
      setBusyId(null);
    }
  }

  function togglePause(queue) {
    run(queue.serviceId, `/api/admin/services/${queue.serviceId}`, {
      method: 'PUT',
      body: {
        serviceName: queue.serviceName,
        description: queue.description,
        averageTime: queue.averageTime,
        active: !queue.active,
      },
    });
  }

  function removeToken(queue, entry) {
    if (!window.confirm(`Remove token #${entry.tokenNumber} (${entry.userName}) from the queue?`)) return;
    run(queue.serviceId, `/api/admin/tokens/${entry.id}/cancel`);
  }

  async function addCounter(e) {
    e.preventDefault();
    setAdding(true);
    setActionError('');
    try {
      const created = await api('/api/admin/services', {
        method: 'POST',
        body: { serviceName: name, description, averageTime: Number(averageTime), active: true },
      });
      setData((current) => [...(current || []), created].sort((a, b) => a.serviceName.localeCompare(b.serviceName)));
      setName('');
      setDescription('');
      setAverageTime(3);
    } catch (err) {
      setActionError(err.message);
    } finally {
      setAdding(false);
    }
  }

  return (
    <section className="page-section page-wide">
      <h1>Queue dashboard</h1>
      {loading && <p>Loading queues...</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      {actionError && <p className="form-error" role="alert">{actionError}</p>}

      <div className="consoles">
        {(data || []).map((q) => {
          const busy = busyId === q.serviceId;
          return (
            <article className={`console ${q.active ? '' : 'console-paused'}`} key={q.serviceId}>
              <header className="console-head">
                <div>
                  <h2>{q.serviceName}</h2>
                  <p className="console-meta">
                    {q.servedToday} served today. About {q.averageTime} min per person.
                  </p>
                </div>
                <LoadBadge load={q.load} waiting={q.waiting.length} />
              </header>

              <div className="console-body">
                <div className="console-now">
                  <p className="console-label">Currently serving</p>
                  <p className="console-number">{q.serving ? `#${q.serving.tokenNumber}` : '--'}</p>
                  <p className="console-person">
                    {q.serving ? `${q.serving.userName}, called at ${formatClock(q.serving.calledTime)}` : 'Counter is free'}
                  </p>
                  <div className="row-actions">
                    <button type="button" className="btn btn-primary" disabled={busy || !q.active}
                      onClick={() => run(q.serviceId, `/api/admin/services/${q.serviceId}/call-next`)}>
                      Call next
                    </button>
                    <button type="button" className="btn btn-outline" disabled={busy || !q.serving}
                      onClick={() => run(q.serviceId, `/api/admin/services/${q.serviceId}/complete`)}>
                      Complete
                    </button>
                    <button type="button" className="btn btn-quiet" disabled={busy} onClick={() => togglePause(q)}>
                      {q.active ? 'Pause counter' : 'Resume counter'}
                    </button>
                  </div>
                </div>

                <div className="console-waiting">
                  <p className="console-label">Waiting ({q.waiting.length})</p>
                  {q.waiting.length === 0 ? (
                    <p className="empty">No one is waiting.</p>
                  ) : (
                    <ul className="waiting-list">
                      {q.waiting.map((w) => (
                        <li key={w.id}>
                          <span className="waiting-number">#{w.tokenNumber}</span>
                          <span className="waiting-name">{w.userName}</span>
                          <span className="waiting-time">{formatClock(w.createdTime)}</span>
                          <button type="button" className="link-button" disabled={busy} onClick={() => removeToken(q, w)}>
                            Remove
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            </article>
          );
        })}
      </div>

      <form className="form add-counter" onSubmit={addCounter}>
        <h2>Add a counter</h2>
        <label>
          Counter name
          <input value={name} onChange={(e) => setName(e.target.value)} maxLength={100} required />
        </label>
        <label>
          Description
          <input value={description} onChange={(e) => setDescription(e.target.value)} maxLength={500} />
        </label>
        <label>
          Average minutes per person
          <input type="number" min="1" max="120" value={averageTime} onChange={(e) => setAverageTime(e.target.value)} required />
        </label>
        <button className="btn btn-primary" disabled={adding}>{adding ? 'Adding...' : 'Add counter'}</button>
      </form>
    </section>
  );
}
