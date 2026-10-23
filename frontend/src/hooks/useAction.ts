import { useCallback, useState } from 'react';
import { errorMessage, toAppError, type AppError } from '../api/client';
import { useToast } from '../app/ToastContext';

/**
 * Chạy một thao tác ghi (POST/PUT…) với trạng thái đang xử lý, toast thành công/thất bại.
 * Trả về kết quả hoặc ném lỗi (để form hiển thị lỗi theo trường nếu cần).
 */
export function useAction() {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const [lastError, setLastError] = useState<AppError | undefined>();

  const run = useCallback(
    async <T,>(fn: () => Promise<T>, success?: string | ((r: T) => string)): Promise<T> => {
      setBusy(true);
      setLastError(undefined);
      try {
        const r = await fn();
        if (success) toast.success(typeof success === 'function' ? success(r) : success);
        return r;
      } catch (e) {
        setLastError(toAppError(e));
        toast.error(errorMessage(e));
        throw e;
      } finally {
        setBusy(false);
      }
    },
    [toast],
  );

  return { run, busy, lastError, fieldErrors: lastError?.fieldErrors ?? {} };
}
