import { describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import { screen } from '@testing-library/react';
import type { UserSummary } from '../types/api';
import { makeUser, renderAt } from './utils';

let current: { user: UserSummary | null; initializing: boolean } = { user: null, initializing: false };
vi.mock('../app/AuthContext', () => ({
  useAuth: () => current,
  useCurrentUser: () => current.user,
}));

import { ProtectedRoute, RoleGuard } from '../routes/guards';

function app() {
  return (
    <Routes>
      <Route path="/login" element={<div>TRANG-DANG-NHAP</div>} />
      <Route path="/secret" element={<ProtectedRoute><div>NOI-DUNG-RIENG</div></ProtectedRoute>} />
      <Route path="/reports" element={<ProtectedRoute><RoleGuard roles={['LOGISTICS_MANAGER']}><div>BAO-CAO</div></RoleGuard></ProtectedRoute>} />
    </Routes>
  );
}

describe('ProtectedRoute / RoleGuard', () => {
  it('chưa đăng nhập → chuyển về /login', () => {
    current = { user: null, initializing: false };
    renderAt(app(), '/secret');
    expect(screen.getByText('TRANG-DANG-NHAP')).toBeInTheDocument();
  });

  it('đang kiểm tra phiên → hiển thị trạng thái tải, không lộ nội dung', () => {
    current = { user: null, initializing: true };
    renderAt(app(), '/secret');
    expect(screen.queryByText('NOI-DUNG-RIENG')).not.toBeInTheDocument();
    expect(screen.queryByText('TRANG-DANG-NHAP')).not.toBeInTheDocument();
  });

  it('đã đăng nhập → vào được trang bảo vệ', () => {
    current = { user: makeUser('CUSTOMER'), initializing: false };
    renderAt(app(), '/secret');
    expect(screen.getByText('NOI-DUNG-RIENG')).toBeInTheDocument();
  });

  it('Manager xem được báo cáo', () => {
    current = { user: makeUser('LOGISTICS_MANAGER'), initializing: false };
    renderAt(app(), '/reports');
    expect(screen.getByText('BAO-CAO')).toBeInTheDocument();
  });

  it.each(['CUSTOMER', 'TRANSPORT_SPECIALIST', 'FLEET_ROUTE_COORDINATOR', 'DRIVER_ESCORT'] as const)('%s bị chặn khỏi báo cáo (trang 403)', (role) => {
    current = { user: makeUser(role), initializing: false };
    renderAt(app(), '/reports');
    expect(screen.queryByText('BAO-CAO')).not.toBeInTheDocument();
  });
});
