// Quy tắc validate phía client – đồng bộ với Bean Validation ở backend (backend vẫn kiểm tra lại).
export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d).+$/;
export const PHONE_RE = /^[0-9+()\-\s]*$/;
export const MICROCHIP_RE = /^[0-9A-Za-z]{9,20}$/;

export type Errors<T> = Partial<Record<keyof T | string, string>>;

export function required(v: unknown, label = 'Trường này'): string | undefined {
  if (v === undefined || v === null || (typeof v === 'string' && v.trim() === '')) return `${label} là bắt buộc`;
  return undefined;
}

export function passwordError(pw: string): string | undefined {
  if (!pw) return 'Vui lòng nhập mật khẩu';
  if (pw.length < 8 || pw.length > 72) return 'Mật khẩu từ 8 đến 72 ký tự';
  if (!PASSWORD_RE.test(pw)) return 'Mật khẩu phải có cả chữ và số';
  return undefined;
}

export function hasErrors(e: Record<string, string | undefined>): boolean {
  return Object.values(e).some(Boolean);
}

/** Gộp lỗi validate client với lỗi theo trường trả về từ backend (fieldErrors). */
export function mergeErrors(client: Record<string, string | undefined>, server: Record<string, string>): Record<string, string | undefined> {
  return { ...server, ...Object.fromEntries(Object.entries(client).filter(([, v]) => v)) };
}
