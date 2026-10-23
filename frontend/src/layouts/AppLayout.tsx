import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth, useCurrentUser } from '../app/AuthContext';
import { notificationApi, reportApi } from '../api/endpoints';
import { Avatar, Breadcrumb, Icon, IconButton } from '../components';
import { MENUS, PAGE_TITLES, notificationLink } from '../routes/menu';
import { RoleLabels, type NotificationType } from '../types/enums';
import type { NotificationItem } from '../types/api';
import { timeAgo } from '../utils/format';

/** Sự kiện để các trang yêu cầu làm mới bộ đếm (thông báo, menu) sau khi thao tác. */
export const REFRESH_COUNTS_EVENT = 'irtms:refresh-counts';
export function refreshCounts() {
  window.dispatchEvent(new Event(REFRESH_COUNTS_EVENT));
}

const NOTE_TONE: Record<NotificationType, { tone: string; icon: string }> = {
  INFO: { tone: 'info', icon: 'bi-info-circle' },
  SUCCESS: { tone: 'success', icon: 'bi-check-circle' },
  WARNING: { tone: 'warning', icon: 'bi-exclamation-triangle' },
  ACTION_REQUIRED: { tone: 'primary', icon: 'bi-hand-index' },
  CRITICAL: { tone: 'critical', icon: 'bi-exclamation-octagon-fill' },
};

function useOutsideClose(open: boolean, close: () => void) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) close();
    };
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && close();
    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [open, close]);
  return ref;
}

function NotificationBell() {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [count, setCount] = useState(0);
  const [items, setItems] = useState<NotificationItem[] | null>(null);
  const close = useCallback(() => setOpen(false), []);
  const ref = useOutsideClose(open, close);

  const loadCount = useCallback(() => {
    notificationApi.unreadCount().then((r) => setCount(r.count)).catch(() => undefined);
  }, []);

  useEffect(() => {
    loadCount();
    const t = window.setInterval(loadCount, 60_000);
    window.addEventListener(REFRESH_COUNTS_EVENT, loadCount);
    return () => {
      window.clearInterval(t);
      window.removeEventListener(REFRESH_COUNTS_EVENT, loadCount);
    };
  }, [loadCount]);

  useEffect(() => {
    if (open) notificationApi.list({ size: 6 }).then((p) => setItems(p.content)).catch(() => setItems([]));
  }, [open]);

  const openItem = async (n: NotificationItem) => {
    if (!n.read) {
      await notificationApi.markRead(n.id).catch(() => undefined);
      loadCount();
    }
    setOpen(false);
    const link = notificationLink(n.referenceType, n.referenceId);
    navigate(link ?? '/notifications');
  };

  const readAll = async () => {
    await notificationApi.readAll().catch(() => undefined);
    setItems((xs) => xs?.map((x) => ({ ...x, read: true })) ?? null);
    setCount(0);
  };

  return (
    <div className="ir-pop-anchor" ref={ref}>
      <IconButton icon="bi-bell" label={`Thông báo${count ? ` (${count} chưa đọc)` : ''}`} onClick={() => setOpen((o) => !o)} badge={count} />
      {open && (
        <div className="ir-pop ir-pop-right" style={{ width: 400, maxWidth: 'calc(100vw - 24px)' }}>
          <div className="ir-menu ir-notes-panel">
            <div className="ir-notes-head">
              <span className="ir-notes-title">Thông báo</span>
              <button type="button" className="ir-btn ir-btn-ghost ir-btn-sm" onClick={readAll} disabled={!count}>
                Đánh dấu đã đọc tất cả
              </button>
            </div>
            <ul className="ir-notes">
              {items === null && <li className="ir-note ir-muted">Đang tải…</li>}
              {items?.length === 0 && <li className="ir-note ir-muted">Chưa có thông báo nào.</li>}
              {items?.map((n) => {
                const t = NOTE_TONE[n.type];
                return (
                  <li key={n.id} className={`ir-note app-clickable${n.read ? '' : ' is-unread'}${n.type === 'CRITICAL' ? ' is-critical' : ''}`} onClick={() => openItem(n)}>
                    <span className={`ir-note-icon tone-${t.tone}`}>
                      <Icon name={t.icon} size={15} />
                    </span>
                    <div className="ir-note-body">
                      <div className="ir-note-title">{n.title}</div>
                      <div className="ir-note-text">{n.message}</div>
                      <div className="ir-note-meta">{timeAgo(n.createdAt)}</div>
                    </div>
                    {!n.read && <span className="ir-note-dot" aria-label="Chưa đọc" />}
                  </li>
                );
              })}
            </ul>
            <div className="ir-notes-foot">
              <Link to="/notifications" onClick={() => setOpen(false)}>
                Xem tất cả thông báo
              </Link>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function UserMenu() {
  const user = useCurrentUser();
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const close = useCallback(() => setOpen(false), []);
  const ref = useOutsideClose(open, close);
  return (
    <div className="ir-pop-anchor" ref={ref}>
      <button type="button" className="ir-user-trigger" onClick={() => setOpen((o) => !o)} aria-haspopup="menu" aria-expanded={open}>
        <Avatar name={user.fullName} size="sm" />
        <span className="ir-user-text">
          <span className="ir-user-name">{user.fullName}</span>
          <span className="ir-user-role">{RoleLabels[user.role]}</span>
        </span>
        <Icon name="bi-chevron-down" size={12} />
      </button>
      {open && (
        <div className="ir-pop ir-pop-right" style={{ minWidth: 240 }}>
          <div className="ir-menu" role="menu">
            <div className="ir-menu-header">
              <div className="ir-strong">{user.fullName}</div>
              <div className="app-text-xs ir-muted">{user.email}</div>
            </div>
            <div className="ir-menu-sep" />
            <button type="button" role="menuitem" className="ir-menu-item" onClick={() => { setOpen(false); navigate('/profile'); }}>
              <Icon name="bi-person" /> <span className="ir-menu-label">Hồ sơ & cài đặt</span>
            </button>
            <button type="button" role="menuitem" className="ir-menu-item" onClick={() => { setOpen(false); navigate('/notifications'); }}>
              <Icon name="bi-bell" /> <span className="ir-menu-label">Thông báo</span>
            </button>
            <div className="ir-menu-sep" />
            <button type="button" role="menuitem" className="ir-menu-item is-danger" onClick={() => { logout(); navigate('/login', { replace: true }); }}>
              <Icon name="bi-box-arrow-right" /> <span className="ir-menu-label">Đăng xuất</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export function AppLayout() {
  const user = useCurrentUser();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [counts, setCounts] = useState<Record<string, string>>({});

  const loadCounts = useCallback(() => {
    reportApi
      .dashboard()
      .then((d) => setCounts(Object.fromEntries(d.cards.map((c) => [c.key, c.value]))))
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    loadCounts();
    window.addEventListener(REFRESH_COUNTS_EVENT, loadCounts);
    return () => window.removeEventListener(REFRESH_COUNTS_EVENT, loadCounts);
  }, [loadCounts]);

  useEffect(() => setMobileOpen(false), [location.pathname]);

  const page = PAGE_TITLES.find((p) => p.pattern.test(location.pathname));
  const crumbs = [...(page?.parent ? [page.parent] : []), { label: page?.title ?? 'IRTMS' }];

  useEffect(() => {
    document.title = `${page?.title ?? 'IRTMS'} · IRTMS`;
  }, [page?.title]);

  return (
    <div className={`ir-shell app-shell${mobileOpen ? ' sidebar-open' : ''}`}>
      <aside className="ir-sidebar" aria-label="Điều hướng chính">
        <div className="ir-sidebar-top">
          <Link to="/dashboard" className="ir-logo is-dark app-link-reset">
            <span className="ir-logo-mark">
              <Icon name="bi-globe-asia-australia" size={18} />
            </span>
            <span className="ir-logo-text">
              <span className="ir-logo-name">IRTMS</span>
              <span className="ir-logo-sub">Vận chuyển ngựa đua</span>
            </span>
          </Link>
        </div>
        <nav className="ir-sidebar-scroll">
          {MENUS[user.role].map((g) => (
            <div className="ir-nav-group" key={g.label}>
              <div className="ir-nav-sep" />
              <div className="ir-nav-group-label">{g.label}</div>
              <ul>
                {g.items.map((it) => {
                  const c = it.countKey ? Number(counts[it.countKey] ?? 0) : 0;
                  return (
                    <li key={it.to}>
                      <NavLink to={it.to} className={({ isActive }) => `ir-nav-item${isActive ? ' is-active' : ''}`}>
                        <i className={`bi ${it.icon}`} aria-hidden="true" />
                        <span className="ir-nav-label">{it.label}</span>
                        {c > 0 && <span className="ir-counter">{c}</span>}
                      </NavLink>
                    </li>
                  );
                })}
              </ul>
            </div>
          ))}
        </nav>
        <div className="ir-sidebar-foot">
          <div className="ir-sidebar-role">
            <span className="ir-overline">Đăng nhập với vai trò</span>
            <span>{RoleLabels[user.role]}</span>
          </div>
        </div>
      </aside>
      <div className="app-sidebar-backdrop" onClick={() => setMobileOpen(false)} />
      <div className="ir-shell-main">
        <header className="ir-topbar">
          <span className="app-mobile-toggle">
            <IconButton icon="bi-list" label="Mở menu" onClick={() => setMobileOpen(true)} />
          </span>
          <div className="ir-topbar-left">
            <Breadcrumb items={crumbs} />
          </div>
          <div className="ir-topbar-right">
            <NotificationBell />
            <span className="ir-topbar-sep" />
            <UserMenu />
          </div>
        </header>
        <main className="ir-content" id="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
