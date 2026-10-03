import { useState, useEffect } from 'react';
import { fetchTasks } from '../api';

const DEBOUNCE_MS = 300;

export function useTasks(query, status, page, pageSize) {
  const [tasks, setTasks] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    // Clear any previous error so a later success does not leave a stale message.
    setError(null);

    // Ignore responses from a request that is no longer current, so a slow
    // earlier request cannot overwrite the results of a newer one.
    let cancelled = false;

    // Debounce so typing issues one request per pause rather than per keystroke.
    const handle = setTimeout(() => {
      fetchTasks({ query, status, page, pageSize })
        .then((data) => {
          if (cancelled) return;
          setTasks(data.items);
          setTotal(data.total);
        })
        .catch((err) => {
          if (cancelled) return;
          setError(err.message);
        })
        // Runs on both success and failure: without this a failed request left
        // loading=true forever and the error was never shown.
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
    }, DEBOUNCE_MS);

    return () => {
      cancelled = true;
      clearTimeout(handle);
    };
  }, [query, status, page, pageSize]);

  return { tasks, total, loading, error };
}
