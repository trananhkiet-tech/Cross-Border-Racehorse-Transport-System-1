import type { Role } from '../types/enums';

const C: Role = 'CUSTOMER';
const M: Role = 'LOGISTICS_MANAGER';
const S: Role = 'TRANSPORT_SPECIALIST';
const K: Role = 'FLEET_ROUTE_COORDINATOR';
const D: Role = 'DRIVER_ESCORT';

/**
 * Quyền xem từng trang theo role. `undefined` = mọi role đã đăng nhập.
 * Đối chiếu với @PreAuthorize ở backend (backend vẫn là nơi kiểm soát cuối cùng);
 * bảng này chỉ để UI không dẫn người dùng vào trang chắc chắn bị từ chối.
 */
export const ROUTE_ACCESS: Record<string, Role[] | undefined> = {
  dashboard: undefined,
  notifications: undefined,
  profile: undefined,

  horses: [C],
  'horses/new': [C],
  'horses/:id': [C, M, S, K],
  'horses/:id/edit': [C],

  requests: [C, M, S, K],
  'requests/new': [C],
  'requests/:id': [C, M, S, K],
  'requests/:id/edit': [C],
  'requests/:id/compliance': [M, S],

  documents: [M, S],
  'documents/:id': [C, M, S, K],
  'compliance/missing': [M, S],
  'compliance/requirements': [M, S],

  quotes: [C, M],
  'quotes/:id': [C, M],
  pricing: [M],
  invoices: [C, M],
  'invoices/:id': [C, M],
  payments: [M],

  fleet: [M, K],
  staff: [M, K],
  routes: [M, K],
  'routes/new': [M, K],
  'routes/:id': [M, K],
  'routes/:id/edit': [M, K],
  planning: [M, K],

  trips: undefined,
  'trips/:id': undefined,
  incidents: [M, S, K, D],
  'incidents/:id': undefined,
  handovers: [C, M, K, D],
  'handovers/:id': undefined,

  claims: [C, M],
  'claims/new': [C],
  'claims/:id': [C, M],

  reports: [M],
};

/** Chuyển đường dẫn cụ thể (/horses/12) về mẫu (horses/:id) để tra bảng quyền. */
export function routePattern(path: string): string | undefined {
  const clean = path.split('?')[0].replace(/^\//, '').replace(/\/$/, '');
  if (clean in ROUTE_ACCESS) return clean;
  const parts = clean.split('/');
  const candidates = Object.keys(ROUTE_ACCESS).filter((p) => p.split('/').length === parts.length);
  // ưu tiên mẫu có nhiều đoạn cố định khớp nhất ("horses/new" trước "horses/:id")
  return candidates
    .filter((p) => p.split('/').every((seg, i) => seg.startsWith(':') || seg === parts[i]))
    .sort((a, b) => (a.match(/:/g)?.length ?? 0) - (b.match(/:/g)?.length ?? 0))[0];
}

export function canAccess(path: string, role: Role | undefined): boolean {
  if (!role) return false;
  const pattern = routePattern(path);
  if (!pattern) return true;
  const roles = ROUTE_ACCESS[pattern];
  return !roles || roles.includes(role);
}
