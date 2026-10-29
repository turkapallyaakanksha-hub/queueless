import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../AuthContext.jsx';
import LoadBadge from '../components/LoadBadge.jsx';
import { usePolling } from '../usePolling.js';

export default function Home() {
  const { user } = useAuth();
  const { data: services, error, loading } = usePolling(() => api('/api/services'), 8000);

  return (
    <>
      <section className="hero">
        <h1>Take a token. Skip the line.</h1>
        <p className="hero-lead">
          Pick a counter from your phone, see how many people are ahead of you, and walk over when your
          number is close.
        </p>
        <div className="hero-actions">
          <Link className="btn btn-primary" to={user ? '/take-token' : '/register'}>Take a token</Link>
          {!user && <Link className="btn btn-quiet" to="/login">Sign in</Link>}
        </div>
      </section>

      <section className="board" aria-labelledby="board-title">
        <h2 id="board-title" className="board-title">Counters right now</h2>
        {loading && <p className="board-note">Loading counters...</p>}
        {error && <p className="board-note" role="alert">{error}</p>}
        {services && services.length === 0 && (
          <p className="board-note">No counters are open yet. An admin can add one from the dashboard.</p>
        )}
        {services && services.length > 0 && (
          <div className="board-table" role="table" aria-label="Live queue status">
            <div className="board-head" role="row">
              <span role="columnheader">Counter</span>
              <span role="columnheader">Now serving</span>
              <span role="columnheader">Waiting</span>
              <span role="columnheader">Queue load</span>
              <span role="columnheader">Best time to visit</span>
            </div>
            {services.map((s) => (
              <div className="board-row" role="row" key={s.id}>
                <span role="cell" className="board-name">
                  <Link to={`/take-token?service=${s.id}`}>{s.serviceName}</Link>
                  <small>{s.description}</small>
                </span>
                <span role="cell" data-label="Now serving" className="led">
                  {s.currentlyServing != null ? `#${s.currentlyServing}` : '--'}
                </span>
                <span role="cell" data-label="Waiting">{s.waitingCount}</span>
                <span role="cell" data-label="Queue load"><LoadBadge load={s.load} /></span>
                <span role="cell" data-label="Best time to visit" className="board-best">
                  {s.bestTime || 'Still collecting data'}
                </span>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="how" aria-labelledby="how-title">
        <h2 id="how-title">How it works</h2>
        <ol>
          <li><strong>Sign in with your college email.</strong> It takes a minute and you only do it once.</li>
          <li><strong>Choose a counter and take a token.</strong> You get a number and an estimated wait.</li>
          <li><strong>Come when your turn is close.</strong> The page tells you when to head over.</li>
        </ol>
      </section>
    </>
  );
}
