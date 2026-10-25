import { plural } from '../format.js';

const LABELS = { LOW: 'Low', MODERATE: 'Moderate', HIGH: 'High' };

/** Queue load indicator: coloured dot + word, so colour is never the only signal. */
export default function LoadBadge({ load, waiting }) {
  const key = (load || 'LOW').toLowerCase();
  return (
    <span className={`load load-${key}`}>
      <span className="load-dot" aria-hidden="true" />
      <span className="load-word">{LABELS[load] || 'Low'}</span>
      {waiting != null && <span className="load-count">{plural(waiting, 'person', 'people')} waiting</span>}
    </span>
  );
}
