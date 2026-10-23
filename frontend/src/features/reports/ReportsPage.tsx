import { useState, type ReactNode } from 'react';
import { Bar, BarChart, CartesianGrid, Cell as RCell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { reportApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Alert, AsyncView, Card, DataTable, DemoTag, KpiCard, PageHeader, Tabs } from '../../components';
import type { Point } from '../../types/api';
import { formatMoney, formatNumber } from '../../utils/format';

const COLORS = ['#16325C', '#1B7340', '#8F5300', '#B42323', '#1D5FB0', '#6B4C9A', '#4F6D7A', '#A0522D'];

function ChartCard({ title, children, empty }: { title: string; children: ReactNode; empty?: boolean }) {
  return (
    <Card title={title} icon="bi-bar-chart">
      {empty ? <span className="ir-muted">Chưa có dữ liệu.</span> : <div className="app-chart-box">{children}</div>}
    </Card>
  );
}

function BarsCard({ title, data, name = 'Số lượng', horizontal, money }: { title: string; data: Point[]; name?: string; horizontal?: boolean; money?: string }) {
  const fmt = (v: unknown) => (money ? formatMoney(Number(v), money) : formatNumber(Number(v)));
  return (
    <ChartCard title={title} empty={data.length === 0}>
      <ResponsiveContainer>
        <BarChart data={data} layout={horizontal ? 'vertical' : 'horizontal'} margin={{ top: 8, right: 8, left: horizontal ? 40 : 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={horizontal} horizontal={!horizontal} />
          {horizontal ? <XAxis type="number" allowDecimals={false} tick={{ fontSize: 12 }} /> : <XAxis dataKey="label" tick={{ fontSize: 12 }} />}
          {horizontal ? <YAxis type="category" dataKey="label" width={120} tick={{ fontSize: 12 }} /> : <YAxis allowDecimals={!!money} tick={{ fontSize: 12 }} />}
          <Tooltip formatter={(v) => fmt(v)} />
          <Bar dataKey="value" name={name} fill="#16325C" radius={horizontal ? [0, 4, 4, 0] : [4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

function PieCard({ title, data }: { title: string; data: Point[] }) {
  const rows = data.filter((d) => d.value > 0);
  return (
    <ChartCard title={title} empty={rows.length === 0}>
      <ResponsiveContainer>
        <PieChart>
          <Pie data={rows} dataKey="value" nameKey="label" innerRadius={50} outerRadius={90} paddingAngle={2}>
            {rows.map((_, i) => <RCell key={i} fill={COLORS[i % COLORS.length]} />)}
          </Pie>
          <Tooltip />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}

function Note({ children }: { children: ReactNode }) {
  return <Alert tone="info" title={<span className="app-row" style={{ gap: 8 }}>Lưu ý về dữ liệu <DemoTag /></span>}>{children}</Alert>;
}

function RevenueTab() {
  const state = useAsync(() => reportApi.revenue(), []);
  return (
    <AsyncView state={state}>
      {(r) => (
        <div className="ir-stack">
          <div className="app-grid app-grid-4">
            <KpiCard label="Đã xuất hóa đơn" value={formatMoney(r.invoicedTotal, r.currency)} hint={`${r.invoiceCount} hóa đơn`} icon="bi-receipt" />
            <KpiCard label="Đã thu" value={formatMoney(r.collectedTotal, r.currency)} hint={`${r.paidInvoiceCount} hóa đơn đã thanh toán`} icon="bi-cash-stack" tone="success" />
            <KpiCard label="Còn phải thu" value={formatMoney(r.outstandingTotal, r.currency)} icon="bi-hourglass-split" tone="warning" />
            <KpiCard label="Đã chi bồi thường" value={formatMoney(r.claimSettledTotal, r.currency)} icon="bi-shield-exclamation" tone="error" />
          </div>
          <div className="app-grid app-grid-2">
            <ChartCard title="Doanh thu theo tháng" empty={r.monthly.length === 0}>
              <ResponsiveContainer>
                <LineChart data={r.monthly} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="label" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip formatter={(v) => formatMoney(Number(v), r.currency)} />
                  <Legend />
                  <Line type="monotone" dataKey="value" name="Xuất hóa đơn" stroke="#16325C" strokeWidth={2} />
                  <Line type="monotone" dataKey="value2" name="Đã thu" stroke="#1B7340" strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </ChartCard>
            <PieCard title="Cơ cấu chi phí" data={r.costBreakdown} />
          </div>
          <Note>{r.note}</Note>
        </div>
      )}
    </AsyncView>
  );
}

function TransportTab() {
  const state = useAsync(() => reportApi.transport(), []);
  return (
    <AsyncView state={state}>
      {(r) => (
        <div className="ir-stack">
          <div className="app-grid app-grid-4">
            <KpiCard label="Tổng số chuyến" value={r.totalTrips} icon="bi-truck" />
            <KpiCard label="Đang vận chuyển" value={r.activeTrips} icon="bi-signpost-2" tone="primary" />
            <KpiCard label="Đã giao" value={r.deliveredTrips} icon="bi-check2-circle" tone="success" />
            <KpiCard label="Ngựa đã vận chuyển" value={r.horsesTransported} icon="bi-postcard-heart" />
          </div>
          <div className="app-grid app-grid-2">
            <BarsCard title="Chuyến theo tháng" data={r.tripsByMonth} />
            <PieCard title="Chuyến theo trạng thái" data={r.tripsByStatus} />
            <BarsCard title="Tuyến được dùng nhiều" data={r.topRoutes} horizontal />
            <PieCard title="Phương thức vận chuyển" data={r.byTransportMode} />
          </div>
          <BarsCard title="Yêu cầu theo trạng thái" data={r.requestsByStatus} horizontal />
        </div>
      )}
    </AsyncView>
  );
}

function IncidentTab() {
  const state = useAsync(() => reportApi.incidents(), []);
  return (
    <AsyncView state={state}>
      {(r) => (
        <div className="ir-stack">
          <div className="app-grid app-grid-4">
            <KpiCard label="Tổng sự cố" value={r.total} icon="bi-exclamation-triangle" />
            <KpiCard label="Đang mở" value={r.open} icon="bi-folder2-open" tone="warning" />
            <KpiCard label="Nghiêm trọng" value={r.critical} icon="bi-exclamation-octagon" tone="error" />
            <KpiCard label="Thời gian xử lý TB" value={formatNumber(r.avgResolutionHours, 1)} unit="giờ" icon="bi-stopwatch" hint={`Tổng trễ ${formatNumber(r.totalDelayMinutes)} phút`} />
          </div>
          <div className="app-grid app-grid-2">
            <PieCard title="Theo mức độ" data={r.bySeverity} />
            <BarsCard title="Theo loại" data={r.byType} horizontal />
            <BarsCard title="Theo tháng" data={r.byMonth} />
            <PieCard title="Quyết định vận hành" data={r.byDecision} />
          </div>
        </div>
      )}
    </AsyncView>
  );
}

function ClaimTab() {
  const state = useAsync(() => reportApi.claims(), []);
  return (
    <AsyncView state={state}>
      {(r) => (
        <div className="ir-stack">
          <div className="app-grid app-grid-4">
            <KpiCard label="Tổng hồ sơ" value={r.total} hint={`${r.open} đang xử lý`} icon="bi-shield-exclamation" />
            <KpiCard label="Số tiền yêu cầu" value={formatMoney(r.claimedTotal)} icon="bi-cash" />
            <KpiCard label="Đã duyệt" value={formatMoney(r.approvedTotal)} hint={`Tỷ lệ duyệt ${formatNumber(r.approvalRatioPercent, 1)}%`} icon="bi-check-circle" tone="success" />
            <KpiCard label="Đã chi trả" value={formatMoney(r.settledTotal)} icon="bi-cash-coin" tone="primary" />
          </div>
          <div className="app-grid app-grid-2">
            <PieCard title="Theo trạng thái" data={r.byStatus} />
            <BarsCard title="Theo loại bồi thường" data={r.byType} horizontal />
          </div>
        </div>
      )}
    </AsyncView>
  );
}

function KpiTab() {
  const state = useAsync(() => reportApi.kpi(), []);
  return (
    <AsyncView state={state}>
      {(r) => (
        <div className="ir-stack">
          <Card title="Chỉ số hiệu quả vận hành (KPI)" icon="bi-speedometer2" flush>
            <DataTable flush rows={r.kpis} rowKey={(k) => k.key}
              columns={[
                { key: 'l', header: 'Chỉ số', render: (k) => <span className="ir-strong">{k.label}</span> },
                { key: 'v', header: 'Giá trị', align: 'right', render: (k) => <span className="ir-tnum">{formatNumber(k.value, 1)} {k.unit}</span> },
                { key: 'd', header: 'Cách tính', render: (k) => k.description },
              ]} />
          </Card>
          <Note>{r.note}</Note>
        </div>
      )}
    </AsyncView>
  );
}

type TabKey = 'revenue' | 'transport' | 'incident' | 'claim' | 'kpi';

export function ReportsPage() {
  const [tab, setTab] = useState<TabKey>('transport');
  return (
    <>
      <PageHeader title="Báo cáo" subtitle="Thống kê vận hành, doanh thu, sự cố và bồi thường. Số liệu tính ở backend từ dữ liệu hiện có." />
      <Tabs<TabKey> tabs={[
        { key: 'transport', label: 'Vận chuyển' },
        { key: 'revenue', label: 'Doanh thu' },
        { key: 'incident', label: 'Sự cố' },
        { key: 'claim', label: 'Bồi thường' },
        { key: 'kpi', label: 'KPI' },
      ]} active={tab} onChange={setTab} />
      {tab === 'transport' && <TransportTab />}
      {tab === 'revenue' && <RevenueTab />}
      {tab === 'incident' && <IncidentTab />}
      {tab === 'claim' && <ClaimTab />}
      {tab === 'kpi' && <KpiTab />}
    </>
  );
}
