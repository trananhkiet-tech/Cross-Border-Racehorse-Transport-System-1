import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { AxiosError, type AxiosResponse } from 'axios';
import type { UserSummary } from '../types/api';
import { makeUser, renderAt } from './utils';

// `login` là hàm thường (không phải vi.fn) để một promise bị từ chối được xử lý hoàn toàn trong LoginPage.
const calls: [string, string][] = [];
let outcome: () => Promise<UserSummary> = () => Promise.resolve(makeUser('CUSTOMER'));
vi.mock('../app/AuthContext', () => ({
  useAuth: () => ({
    login: (email: string, password: string) => {
      calls.push([email, password]);
      return outcome();
    },
    user: null,
    initializing: false,
  }),
}));

import { LoginPage } from '../features/auth/LoginPage';

function page() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/dashboard" element={<div>DASHBOARD</div>} />
    </Routes>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    calls.length = 0;
    outcome = () => Promise.resolve(makeUser('CUSTOMER'));
  });

  it('submit rỗng → báo lỗi từng trường, không gọi API', async () => {
    renderAt(page(), '/login');
    await userEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));
    expect(await screen.findByText('Vui lòng nhập email')).toBeInTheDocument();
    expect(screen.getByText('Vui lòng nhập mật khẩu')).toBeInTheDocument();
    expect(calls).toHaveLength(0);
  });

  it('email sai định dạng', async () => {
    renderAt(page(), '/login');
    await userEvent.type(screen.getByLabelText(/Email/), 'khong-phai-email');
    await userEvent.type(screen.getByLabelText(/Mật khẩu/), 'x');
    await userEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));
    expect(await screen.findByText('Email không hợp lệ')).toBeInTheDocument();
    expect(calls).toHaveLength(0);
  });

  it('sai thông tin (401) → hiển thị thông báo lỗi thân thiện', async () => {
    outcome = () => Promise.reject(new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', undefined, undefined, { status: 401, data: { message: 'Bad credentials' } } as AxiosResponse));
    renderAt(page(), '/login');
    await userEvent.type(screen.getByLabelText(/Email/), 'customer@irtms.vn');
    await userEvent.type(screen.getByLabelText(/Mật khẩu/), 'Sai@12345');
    await userEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));
    expect(await screen.findByText(/Email hoặc mật khẩu không đúng/)).toBeInTheDocument();
  });

  it('đăng nhập thành công → chuyển tới dashboard', async () => {
    renderAt(page(), '/login');
    await userEvent.type(screen.getByLabelText(/Email/), 'customer@irtms.vn');
    await userEvent.type(screen.getByLabelText(/Mật khẩu/), 'Demo@123');
    await userEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));
    await waitFor(() => expect(screen.getByText('DASHBOARD')).toBeInTheDocument());
    expect(calls).toEqual([['customer@irtms.vn', 'Demo@123']]);
  });
});
