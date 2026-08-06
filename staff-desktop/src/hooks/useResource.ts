import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../api/client';
import { useAuth } from '../state/AuthContext';

interface ResourceState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
  reload: () => void;
}

export function useResource<T>(
  loader: (token: string) => Promise<T>,
  deps: unknown[] = [],
): ResourceState<T> {
  const { session, logout } = useAuth();
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tick, setTick] = useState(0);

  const reload = useCallback(() => setTick((value) => value + 1), []);

  useEffect(() => {
    if (!session) return;
    let active = true;
    setLoading(true);
    setError(null);
    loader(session.token)
      .then((result) => {
        if (active) setData(result);
      })
      .catch((err) => {
        if (!active) return;
        if (err instanceof ApiError && err.message.toLowerCase().includes('não autorizado')) {
          logout();
          return;
        }
        setError(err instanceof ApiError ? err.message : 'Não foi possível carregar os dados.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session, tick, ...deps]);

  return { data, loading, error, reload };
}
