import { useCallback, useEffect, useRef, useState, type DependencyList } from 'react';
import { errorMessage } from '../api/client';

export interface AsyncState<T> {
  data?: T;
  loading: boolean;
  error?: string;
  reload: () => void;
  setData: (d: T | undefined) => void;
}

/**
 * Tải dữ liệu bất đồng bộ theo dependency; bỏ qua kết quả cũ khi dependency đổi (tránh race condition).
 * Truyền `enabled=false` để tạm không tải.
 */
export function useAsync<T>(fn: () => Promise<T>, deps: DependencyList, enabled = true): AsyncState<T> {
  const [data, setData] = useState<T | undefined>(undefined);
  const [loading, setLoading] = useState<boolean>(enabled);
  const [error, setError] = useState<string | undefined>(undefined);
  const [tick, setTick] = useState(0);
  const seq = useRef(0);

  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return;
    }
    const id = ++seq.current;
    setLoading(true);
    setError(undefined);
    fn()
      .then((d) => {
        if (id === seq.current) setData(d);
      })
      .catch((e) => {
        if (id === seq.current) setError(errorMessage(e));
      })
      .finally(() => {
        if (id === seq.current) setLoading(false);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick, enabled]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { data, loading, error, reload, setData };
}
