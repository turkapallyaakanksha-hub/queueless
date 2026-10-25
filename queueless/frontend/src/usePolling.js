import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Loads data now and then again every `interval` ms (while the tab is visible),
 * so queue numbers stay current without the student refreshing the page.
 */
export function usePolling(load, interval = 6000) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const loadRef = useRef(load);
  loadRef.current = load;

  const refresh = useCallback(async () => {
    try {
      setData(await loadRef.current());
      setError('');
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
    const id = setInterval(() => {
      if (!document.hidden) refresh();
    }, interval);
    return () => clearInterval(id);
  }, [refresh, interval]);

  return { data, setData, error, loading, refresh };
}
