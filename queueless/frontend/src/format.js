export function formatDateTime(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleString([], {
    day: 'numeric',
    month: 'short',
    hour: 'numeric',
    minute: '2-digit',
  });
}

export function formatClock(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
}

export function minutesLabel(minutes) {
  if (minutes == null) return '';
  if (minutes <= 0) return 'No wait';
  if (minutes < 60) return `About ${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m === 0 ? `About ${h} h` : `About ${h} h ${m} min`;
}

export function plural(count, one, many) {
  return `${count} ${count === 1 ? one : many}`;
}

export const STATUS_LABELS = {
  WAITING: 'Waiting',
  SERVING: 'Being served',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};
