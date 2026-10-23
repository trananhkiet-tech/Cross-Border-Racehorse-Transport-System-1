import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../api/endpoints';
import { getToken, setToken, UNAUTHORIZED_EVENT } from '../api/client';
import type { RegisterPayload, UserSummary } from '../types/api';
import type { Role } from '../types/enums';

interface AuthState {
  user: UserSummary | null;
  /** Đang kiểm tra token lúc khởi động. */
  initializing: boolean;
  login: (email: string, password: string) => Promise<UserSummary>;
  register: (payload: RegisterPayload) => Promise<UserSummary>;
  logout: () => void;
  refresh: () => Promise<void>;
  setUser: (u: UserSummary) => void;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthCtx = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [initializing, setInitializing] = useState<boolean>(() => !!getToken());

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
  }, []);

  const refresh = useCallback(async () => {
    if (!getToken()) {
      setUser(null);
      return;
    }
    try {
      setUser(await authApi.me());
    } catch {
      setToken(null);
      setUser(null);
    }
  }, []);

  useEffect(() => {
    if (getToken()) refresh().finally(() => setInitializing(false));
    else setInitializing(false);
  }, [refresh]);

  useEffect(() => {
    const onUnauthorized = () => setUser(null);
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login(email, password);
    setToken(res.accessToken);
    setUser(res.user);
    return res.user;
  }, []);

  const register = useCallback(async (payload: RegisterPayload) => {
    const res = await authApi.register(payload);
    setToken(res.accessToken);
    setUser(res.user);
    return res.user;
  }, []);

  const hasRole = useCallback((...roles: Role[]) => !!user && roles.includes(user.role), [user]);

  const value = useMemo(
    () => ({ user, initializing, login, register, logout, refresh, setUser, hasRole }),
    [user, initializing, login, register, logout, refresh, hasRole],
  );
  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthCtx);
  if (!ctx) throw new Error('useAuth phải dùng bên trong AuthProvider');
  return ctx;
}

/** Người dùng hiện tại (chỉ dùng trong trang đã bảo vệ). */
export function useCurrentUser(): UserSummary {
  const { user } = useAuth();
  if (!user) throw new Error('Chưa đăng nhập');
  return user;
}
