import axios, { AxiosError } from 'axios';
import type { ApiErrorBody } from '../types/api';

/** URL gốc của REST API, cấu hình qua biến môi trường VITE_API_BASE_URL (xem .env). */
export const API_BASE_URL: string = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080/api';

export const TOKEN_KEY = 'irtms.accessToken';

export function getToken(): string | null {
  try {
    return window.localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null): void {
  try {
    if (token) window.localStorage.setItem(TOKEN_KEY, token);
    else window.localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* trình duyệt chặn storage: bỏ qua */
  }
}

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Phát sự kiện khi token hết hạn để AuthContext đăng xuất và chuyển về trang đăng nhập. */
export const UNAUTHORIZED_EVENT = 'irtms:unauthorized';

http.interceptors.response.use(
  (res) => res,
  (error: AxiosError<ApiErrorBody>) => {
    if (error.response?.status === 401 && getToken()) {
      setToken(null);
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    return Promise.reject(error);
  },
);

/** Lỗi đã chuẩn hóa để hiển thị cho người dùng. */
export interface AppError {
  status: number;
  code: string;
  message: string;
  fieldErrors: Record<string, string>;
}

export function toAppError(err: unknown): AppError {
  if (axios.isAxiosError(err)) {
    const e = err as AxiosError<ApiErrorBody>;
    if (!e.response) {
      return {
        status: 0,
        code: 'NETWORK',
        message: 'Không kết nối được máy chủ. Kiểm tra backend đang chạy và cấu hình VITE_API_BASE_URL.',
        fieldErrors: {},
      };
    }
    const body = e.response.data;
    const fieldErrors: Record<string, string> = {};
    body?.fieldErrors?.forEach((f) => {
      fieldErrors[f.field] = f.message;
    });
    const fallback: Record<number, string> = {
      400: 'Dữ liệu chưa hợp lệ',
      401: 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại',
      403: 'Bạn không có quyền thực hiện thao tác này',
      404: 'Không tìm thấy dữ liệu',
      409: 'Thao tác không hợp lệ với trạng thái hiện tại',
      500: 'Lỗi hệ thống, vui lòng thử lại sau',
    };
    return {
      status: e.response.status,
      code: body?.code ?? 'ERROR',
      message: body?.message ?? fallback[e.response.status] ?? 'Đã xảy ra lỗi',
      fieldErrors,
    };
  }
  return { status: 0, code: 'UNKNOWN', message: err instanceof Error ? err.message : 'Đã xảy ra lỗi', fieldErrors: {} };
}

export function errorMessage(err: unknown): string {
  const e = toAppError(err);
  const fields = Object.values(e.fieldErrors);
  return fields.length ? `${e.message}: ${fields.join('; ')}` : e.message;
}

/** Tạo query string bỏ qua giá trị rỗng; mảng → lặp tham số (status=A&status=B). */
export function qs(params: Record<string, unknown>): string {
  const sp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return;
    if (Array.isArray(v)) v.forEach((x) => sp.append(k, String(x)));
    else sp.append(k, String(v));
  });
  const s = sp.toString();
  return s ? `?${s}` : '';
}

/** Sự kiện báo lỗi xem/tải tệp (ToastProvider lắng nghe và hiển thị). */
export const FILE_ERROR_EVENT = 'irtms:file-error';

/** Lỗi trả về dạng blob (responseType=blob) → đọc lại JSON để lấy thông điệp tiếng Việt. */
async function blobErrorMessage(err: unknown): Promise<string> {
  if (axios.isAxiosError(err) && err.response?.data instanceof Blob) {
    try {
      const body = JSON.parse(await err.response.data.text()) as ApiErrorBody;
      if (body?.message) return body.message;
    } catch { /* bỏ qua, dùng thông điệp mặc định */ }
  }
  return errorMessage(err);
}

/** Tải tệp có xác thực (Bearer) rồi mở trong tab mới hoặc lưu về máy. Lỗi được hiển thị bằng thông báo. */
export async function openFile(path: string, fileName?: string, download = false): Promise<void> {
  // Mở tab ngay trong sự kiện click để trình duyệt không chặn popup (blob chỉ có sau khi gọi API).
  const win = download ? null : window.open('', '_blank');
  try {
    const res = await http.get(path, { responseType: 'blob', params: { inline: !download } });
    const url = URL.createObjectURL(res.data as Blob);
    if (download) {
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName ?? 'tai-lieu';
      document.body.appendChild(a);
      a.click();
      a.remove();
    } else if (win) {
      win.opener = null;
      win.location.href = url;
    } else {
      window.location.assign(url);
    }
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  } catch (err) {
    win?.close();
    const message = await blobErrorMessage(err);
    window.dispatchEvent(new CustomEvent(FILE_ERROR_EVENT, { detail: message }));
  }
}
