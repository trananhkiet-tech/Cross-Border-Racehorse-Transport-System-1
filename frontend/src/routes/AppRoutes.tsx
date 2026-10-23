import type { ReactElement } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from '../app/AuthContext';
import { AppLayout } from '../layouts/AppLayout';
import { AuthLayout } from '../layouts/AuthLayout';
import type { Role } from '../types/enums';
import { ROUTE_ACCESS } from './access';
import { GuestRoute, ProtectedRoute, RoleGuard } from './guards';
import { ForbiddenPage, NotFoundPage } from '../features/common/ErrorPages';
import { LoginPage } from '../features/auth/LoginPage';
import { RegisterPage } from '../features/auth/RegisterPage';
import { WelcomePage } from '../features/auth/WelcomePage';

// ===== user1: Ngựa / Yêu cầu vận chuyển / Hồ sơ cá nhân (import) =====
// [user1:imports]

// ===== user2: Tài liệu / Tuân thủ / Sức khoẻ (import) =====
// [user2:imports]

// ===== user3: Đội xe / Tuyến / Chuyến (import) =====
// [user3:imports]

// ===== user4: Theo dõi / Sự cố / Bàn giao (import) =====
// [user4:imports]

// ===== user5: Báo giá / Thanh toán / Bồi thường / Báo cáo / Dashboard / Thông báo (import) =====
// [user5:imports]

/**
 * Bảng quyền truy cập trang theo role (ẩn trang vượt quyền). Backend vẫn kiểm tra lại mọi API;
 * bảng này chỉ để UI không dẫn người dùng vào trang họ không dùng được.
 *
 * Nhánh main chỉ có đăng nhập / đăng ký. Mỗi thành viên (user1…user5) CHỈ thay dòng `// [userN:...]`
 * của mình bằng import / route của module mình — nhờ vậy 5 nhánh merge vào nhau không bị xung đột.
 */
export const ROUTE_ROLES: { path: string; roles?: Role[]; element: ReactElement }[] = [

  // ===== user1: Ngựa / Yêu cầu vận chuyển / Hồ sơ cá nhân =====
  // [user1:routes]

  // ===== user2: Tài liệu / Tuân thủ / Sức khoẻ =====
  // [user2:routes]

  // ===== user3: Đội xe / Tuyến / Chuyến =====
  // [user3:routes]

  // ===== user4: Theo dõi / Sự cố / Bàn giao =====
  // [user4:routes]

  // ===== user5: Báo giá / Thanh toán / Bồi thường / Báo cáo / Dashboard / Thông báo =====
  // [user5:routes]
];

/** "/" → dashboard nếu module Dashboard (user5) đã được merge; chưa có thì hiện trang chào của main. */
function HomeRoute() {
  const { user } = useAuth();
  const dashboardRoles = ROUTE_ACCESS['dashboard'];
  const hasDashboard = ROUTE_ROLES.some((r) => r.path === 'dashboard')
    && !!user && (!dashboardRoles || dashboardRoles.includes(user.role));
  return hasDashboard ? <Navigate to="/dashboard" replace /> : <WelcomePage />;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<GuestRoute><AuthLayout /></GuestRoute>}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
        <Route path="/" element={<HomeRoute />} />
        {ROUTE_ROLES.map((r) => (
          <Route key={r.path} path={r.path} element={r.roles ? <RoleGuard roles={r.roles}>{r.element}</RoleGuard> : r.element} />
        ))}
        <Route path="/403" element={<ForbiddenPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
