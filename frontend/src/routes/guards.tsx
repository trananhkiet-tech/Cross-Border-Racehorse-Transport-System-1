import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../app/AuthContext';
import { LoadingState } from '../components';
import type { Role } from '../types/enums';
import { ForbiddenPage } from '../features/common/ErrorPages';

/** Chỉ cho vào khi đã đăng nhập; chưa đăng nhập → /login (giữ lại trang định vào). */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, initializing } = useAuth();
  const location = useLocation();
  if (initializing) return <LoadingState text="Đang kiểm tra phiên đăng nhập…" />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  return <>{children}</>;
}

/** Ẩn trang khỏi role không phù hợp (backend vẫn là nơi kiểm soát quyền cuối cùng). */
export function RoleGuard({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user } = useAuth();
  if (!user || !roles.includes(user.role)) return <ForbiddenPage />;
  return <>{children}</>;
}

/** Trang đăng nhập/đăng ký: đã đăng nhập thì chuyển vào dashboard. */
export function GuestRoute({ children }: { children: ReactNode }) {
  const { user, initializing } = useAuth();
  if (initializing) return <LoadingState />;
  if (user) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}
