import { useSearchParams } from 'react-router-dom';
import { useCallback, useEffect, useState } from 'react';

/** Đồng bộ một tham số lọc với query string (giữ bộ lọc khi tải lại / chia sẻ link). */
export function useQueryParam(key: string, fallback = ''): [string, (v: string) => void] {
  const [params, setParams] = useSearchParams();
  const value = params.get(key) ?? fallback;
  const set = useCallback(
    (v: string) => {
      setParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          if (v) next.set(key, v);
          else next.delete(key);
          if (key !== 'page') next.delete('page');
          return next;
        },
        { replace: true },
      );
    },
    [key, setParams],
  );
  return [value, set];
}

/** Giá trị trì hoãn cho ô tìm kiếm. */
export function useDebounced<T>(value: T, ms = 350): T {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms);
    return () => clearTimeout(t);
  }, [value, ms]);
  return v;
}
