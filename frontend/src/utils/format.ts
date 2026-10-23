// Định dạng hiển thị. Quy ước thời gian: backend trả LocalDateTime/LocalDate là giờ nghiệp vụ
// (mặc định Asia/Ho_Chi_Minh, không kèm múi giờ) – frontend hiển thị nguyên giá trị, không quy đổi.

const COUNTRY_NAMES: Record<string, string> = {
  VN: 'Việt Nam', TH: 'Thái Lan', KH: 'Campuchia', LA: 'Lào', MY: 'Malaysia', SG: 'Singapore', HK: 'Hồng Kông',
  JP: 'Nhật Bản', KR: 'Hàn Quốc', CN: 'Trung Quốc', AU: 'Úc', AE: 'UAE', GB: 'Vương quốc Anh', FR: 'Pháp',
  IE: 'Ireland', US: 'Hoa Kỳ',
};

export const COUNTRY_OPTIONS = Object.entries(COUNTRY_NAMES).map(([code, name]) => ({ code, name }));

export function countryName(code?: string | null): string {
  if (!code) return '—';
  return COUNTRY_NAMES[code] ?? code;
}

export function formatMoney(value?: number | null, currency = 'USD'): string {
  if (value === undefined || value === null || Number.isNaN(value)) return '—';
  const n = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
  return `${currency} ${n}`;
}

export function formatNumber(value?: number | null, digits = 0): string {
  if (value === undefined || value === null) return '—';
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: digits, minimumFractionDigits: digits }).format(value);
}

function parts(value: string): { y: string; m: string; d: string; hh?: string; mm?: string } | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2}))?/.exec(value);
  if (!m) return null;
  return { y: m[1], m: m[2], d: m[3], hh: m[4], mm: m[5] };
}

export function formatDate(value?: string | null): string {
  if (!value) return '—';
  const p = parts(value);
  return p ? `${p.d}/${p.m}/${p.y}` : value;
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '—';
  const p = parts(value);
  if (!p) return value;
  return p.hh ? `${p.d}/${p.m}/${p.y} ${p.hh}:${p.mm}` : `${p.d}/${p.m}/${p.y}`;
}

export function formatTime(value?: string | null): string {
  if (!value) return '—';
  const p = parts(value);
  return p?.hh ? `${p.hh}:${p.mm}` : '—';
}

/** "5 phút trước", "3 giờ trước"… dựa trên giờ trình duyệt (cùng múi giờ nghiệp vụ khi demo tại VN). */
export function timeAgo(value?: string | null): string {
  if (!value) return '';
  const t = new Date(value).getTime();
  if (Number.isNaN(t)) return '';
  const diff = Math.round((Date.now() - t) / 60000);
  if (diff < 1) return 'vừa xong';
  if (diff < 60) return `${diff} phút trước`;
  if (diff < 60 * 24) return `${Math.round(diff / 60)} giờ trước`;
  return `${Math.round(diff / 1440)} ngày trước`;
}

export function formatBytes(n?: number | null): string {
  if (!n && n !== 0) return '—';
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(0)} KB`;
  return `${(n / 1024 / 1024).toFixed(1)} MB`;
}

export function initials(name?: string | null): string {
  if (!name) return '?';
  const words = name.trim().split(/\s+/);
  const first = words[0]?.[0] ?? '';
  const last = words.length > 1 ? words[words.length - 1][0] : '';
  return (first + last).toUpperCase();
}

/** Giá trị cho <input type="datetime-local"> từ chuỗi LocalDateTime. */
export function toLocalInput(value?: string | null): string {
  if (!value) return '';
  return value.slice(0, 16);
}

/** yyyy-MM-dd của hôm nay + n ngày (giờ trình duyệt). */
export function isoDatePlus(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  const pad = (x: number) => String(x).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function isoDateTimePlus(hours: number): string {
  const d = new Date(Date.now() + hours * 3600 * 1000);
  const pad = (x: number) => String(x).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:00`;
}

export function minutesToText(min?: number | null): string {
  if (!min) return '0 phút';
  const h = Math.floor(min / 60);
  const m = min % 60;
  return h ? `${h} giờ${m ? ` ${m} phút` : ''}` : `${m} phút`;
}
