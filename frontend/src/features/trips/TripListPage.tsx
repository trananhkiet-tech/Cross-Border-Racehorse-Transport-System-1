import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { tripApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { useDebounced } from '../../hooks/useQueryState';
import { Badge, Cell, DataTable, PageHeader, Pagination, SearchBox, StatusBadge, Tabs } from '../../components';
import { TripStatusLabels, TransportModeLabels, type TripStatus } from '../../types/enums';
import type { TripSummary } from '../../types/api';
import { formatDateTime } from '../../utils/format';

const GROUPS: { key: string; label: string; statuses: TripStatus[] }[] = [
  { key: 'all', label: 'Tất cả', statuses: [] },
  { key: 'plan', label: 'Chuẩn bị', statuses: ['SCHEDULED', 'PREPARING', 'READY_FOR_DEPARTURE'] },
  { key: 'run', label: 'Đang vận chuyển', statuses: ['IN_TRANSIT', 'AT_CHECKPOINT', 'AT_BORDER', 'CUSTOMS_CLEARANCE', 'QUARANTINE', 'DELAYED'] },
  { key: 'arrive', label: 'Đã đến / bàn giao', statuses: ['ARRIVED', 'HANDOVER_PENDING'] },
  { key: 'done', label: 'Hoàn tất / hủy', statuses: ['DELIVERED', 'CANCELLED'] },
];

export function TripListPage() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const [tab, setTab] = useState('all');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const dq = useDebounced(q);
  const statuses = GROUPS.find((g) => g.key === tab)?.statuses ?? [];
  const state = useAsync(() => tripApi.list({ status: statuses, q: dq || undefined, page, size: 10 }), [tab, dq, page]);
  const customer = user.role === 'CUSTOMER';
  const driver = user.role === 'DRIVER_ESCORT';
  return (
    <>
      <PageHeader title={customer ? 'Chuyến đi của tôi' : driver ? 'Chuyến của tôi' : 'Chuyến đi'} subtitle={customer ? 'Theo dõi hành trình ngựa của bạn.' : driver ? 'Các chuyến bạn được phân công.' : 'Toàn bộ chuyến theo quyền của bạn.'} />
      <Tabs tabs={GROUPS.map((g) => ({ key: g.key, label: g.label }))} active={tab} onChange={(k) => { setTab(k); setPage(0); }} />
      <DataTable<TripSummary> rows={state.data?.content} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(t) => t.id} onRowClick={(t) => navigate(`/trips/${t.id}`)}
        rowClassName={(t) => (t.operationBlocked ? 'row-error' : t.status === 'DELAYED' ? 'row-warning' : undefined)}
        emptyTitle="Chưa có chuyến đi" emptyDescription={customer ? 'Chuyến đi xuất hiện sau khi điều phối lập kế hoạch cho yêu cầu của bạn.' : undefined} emptyIcon="bi-truck"
        toolbar={<SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Mã chuyến, yêu cầu, khách hàng, tuyến…" width={340} />}
        columns={[
          { key: 'n', header: 'Chuyến', sortValue: (t) => t.tripNumber, render: (t) => <Cell mono main={t.tripNumber} sub={t.requestNumber} /> },
          ...(!customer ? [{ key: 'c', header: 'Khách hàng', render: (t: TripSummary) => t.customerName }] : []),
          { key: 'r', header: 'Tuyến', render: (t) => <Cell main={t.routeName} sub={TransportModeLabels[t.transportMode]} /> },
          { key: 'd', header: 'Khởi hành', sortValue: (t) => t.plannedDeparture, render: (t) => <Cell main={formatDateTime(t.plannedDeparture)} sub={`đến ${formatDateTime(t.plannedArrival)}`} /> },
          { key: 'v', header: 'Xe / nhân sự', render: (t) => <Cell main={t.vehicleCode ?? <Badge tone="warning">Chưa có xe</Badge>} sub={t.staffNames.length ? t.staffNames.join(', ') : 'Chưa phân công'} /> },
          { key: 'l', header: 'Vị trí', render: (t) => t.currentLocation ?? '—' },
          { key: 's', header: 'Trạng thái', render: (t) => <span className="app-row" style={{ gap: 4 }}><StatusBadge status={t.status} labels={TripStatusLabels} />{t.operationBlocked && <Badge tone="critical" icon="bi-hand-thumbs-down">Tạm dừng</Badge>}</span> },
        ]}
        footer={state.data && <Pagination page={state.data.page} size={state.data.size} totalElements={state.data.totalElements} totalPages={state.data.totalPages} onPage={setPage} />} />
    </>
  );
}
