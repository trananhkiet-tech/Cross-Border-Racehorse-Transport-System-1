import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { reportApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { AsyncView, Card, DemoTag, EmptyState, Icon, KpiCard, LinkButton, PageHeader, StatusBadge } from '../../components';
import type { Dashboard, DashboardSection } from '../../types/api';

const CARD_ICONS: Record<string, string> = {
  activeRequests: 'bi-clipboard-check', pendingDocuments: 'bi-file-earmark-text', quotesAwaiting: 'bi-receipt',
  activeTrips: 'bi-truck', openClaims: 'bi-shield-check', awaitingApproval: 'bi-hourglass-split', toQuote: 'bi-calculator',
  delayedTrips: 'bi-clock-history', openIncidents: 'bi-exclamation-octagon', claimsReview: 'bi-shield-exclamation',
  collected: 'bi-cash-coin', missingDocuments: 'bi-file-earmark-x', expiring: 'bi-calendar-x', inReview: 'bi-search',
  readyForPlanning: 'bi-calendar2-plus', needAssignment: 'bi-person-plus', upcoming: 'bi-calendar-event',
  availableVehicles: 'bi-truck-front', activeRoutes: 'bi-signpost-2', assigned: 'bi-list-task', running: 'bi-speedometer2',
  handover: 'bi-box-arrow-in-down', incidents: 'bi-exclamation-triangle',
};

function SectionCard({ s }: { s: DashboardSection }) {
  return (
    <Card title={s.title} actions={s.link ? <Link to={s.link} className="app-text-sm">Xem tất cả</Link> : undefined} flush>
      {s.items.length === 0 ? (
        <EmptyState compact title={s.emptyText} icon="bi-check2-circle" />
      ) : (
        <ul className="ir-notes ir-notes-full" style={{ padding: 4 }}>
          {s.items.map((it) => (
            <li key={`${s.key}-${it.id}`} className="ir-note" style={{ alignItems: 'center' }}>
              <div className="ir-note-body">
                <div className="ir-note-title">
                  {it.link ? <Link to={it.link} className="app-link-reset">{it.title}</Link> : it.title}
                </div>
                {it.subtitle && <div className="ir-note-text">{it.subtitle}</div>}
                {it.meta && <div className="ir-note-meta">{it.meta}</div>}
              </div>
              {it.status && <StatusBadge status={it.status} />}
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}

const QUICK_ACTIONS: Record<string, { to: string; label: string; icon: string; variant?: 'primary' | 'outline' }[]> = {
  CUSTOMER: [
    { to: '/horses/new', label: 'Thêm ngựa', icon: 'bi-plus-lg', variant: 'outline' },
    { to: '/requests/new', label: 'Tạo yêu cầu vận chuyển', icon: 'bi-plus-circle' },
  ],
  LOGISTICS_MANAGER: [{ to: '/reports', label: 'Xem báo cáo', icon: 'bi-bar-chart-line', variant: 'outline' }],
  TRANSPORT_SPECIALIST: [{ to: '/documents?status=PENDING_REVIEW', label: 'Rà soát tài liệu', icon: 'bi-file-earmark-check' }],
  FLEET_ROUTE_COORDINATOR: [
    { to: '/routes/new', label: 'Tạo tuyến', icon: 'bi-signpost-2', variant: 'outline' },
    { to: '/planning', label: 'Lập chuyến', icon: 'bi-calendar2-plus' },
  ],
  DRIVER_ESCORT: [{ to: '/trips', label: 'Chuyến của tôi', icon: 'bi-truck' }],
};

function DashboardView({ d }: { d: Dashboard }) {
  const user = useCurrentUser();
  return (
    <>
      <PageHeader
        title={d.greeting}
        subtitle={new Intl.DateTimeFormat('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date())}
        meta={<DemoTag>Số liệu lấy từ cơ sở dữ liệu (dữ liệu demo)</DemoTag>}
        actions={QUICK_ACTIONS[user.role]?.map((a) => (
          <LinkButton key={a.to} to={a.to} icon={a.icon} variant={a.variant ?? 'primary'}>{a.label}</LinkButton>
        ))}
      />
      <div className={`app-grid app-grid-${Math.min(d.cards.length, 6)}`}>
        {d.cards.map((c) => (
          <KpiCard key={c.key} label={c.label} value={c.value} hint={c.hint} tone={c.tone} to={c.link} icon={CARD_ICONS[c.key] ?? 'bi-bar-chart'} />
        ))}
      </div>
      <div className="app-grid app-grid-3">
        {d.sections.map((s) => (
          <SectionCard key={s.key} s={s} />
        ))}
      </div>
      {d.chart.length > 0 && (
        <Card title={d.chartTitle ?? 'Biểu đồ'} icon="bi-bar-chart">
          <div className="app-chart-box">
            <ResponsiveContainer>
              <BarChart data={d.chart} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="label" tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="value" name="Số lượng" fill="#16325C" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      )}
    </>
  );
}

export function DashboardPage() {
  const state = useAsync(() => reportApi.dashboard(), []);
  return (
    <AsyncView state={state}>
      {(d) => (
        <>
          <DashboardView d={d} />
          <p className="app-text-xs ir-muted" style={{ margin: 0 }}>
            <Icon name="bi-info-circle" /> Các chỉ số được tính tại thời điểm tải trang theo quyền của tài khoản.
          </p>
        </>
      )}
    </AsyncView>
  );
}
