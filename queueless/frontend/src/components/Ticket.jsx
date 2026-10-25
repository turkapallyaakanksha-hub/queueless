import { minutesLabel } from '../format.js';

/** The token the student holds: big number on the left, live queue facts on the tear-off stub. */
export default function Ticket({ token, onCancel, busy }) {
  const waiting = token.status === 'WAITING';
  const serving = token.status === 'SERVING';
  const closeToTurn = waiting && token.peopleAhead != null && token.peopleAhead <= 2;

  let message = '';
  if (serving) message = 'It is your turn. Go to the counter now.';
  else if (closeToTurn) message = 'Your turn is close. Head to the counter.';
  else if (waiting) message = 'You can stay where you are. This page updates by itself.';
  else if (token.status === 'COMPLETED') message = 'This token has been served.';
  else message = 'This token was cancelled.';

  return (
    <article className={`ticket ${serving || closeToTurn ? 'ticket-alert' : ''}`} aria-live="polite">
      <div className="ticket-main">
        <p className="ticket-service">{token.serviceName}</p>
        <p className="ticket-number" aria-label={`Token number ${token.tokenNumber}`}>
          <span className="ticket-hash" aria-hidden="true">#</span>
          {token.tokenNumber}
        </p>
        <p className="ticket-message">{message}</p>
        {waiting && onCancel && (
          <button type="button" className="btn btn-ticket btn-small" onClick={() => onCancel(token)} disabled={busy}>
            Cancel token
          </button>
        )}
      </div>
      <dl className="ticket-stub">
        <div>
          <dt>Now serving</dt>
          <dd>{token.currentlyServing != null ? `#${token.currentlyServing}` : 'No one yet'}</dd>
        </div>
        <div>
          <dt>People ahead</dt>
          <dd>{token.peopleAhead ?? '-'}</dd>
        </div>
        <div>
          <dt>Estimated wait</dt>
          <dd>{token.estimatedWaitMinutes != null ? minutesLabel(token.estimatedWaitMinutes) : '-'}</dd>
        </div>
      </dl>
    </article>
  );
}
