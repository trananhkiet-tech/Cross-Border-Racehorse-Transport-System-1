import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { tripApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { useDebounced } from '../../hooks/useQueryState';
import { useAction } from '../../hooks/useAction';
import { refreshCounts } from '../../layouts/AppLayout';
import { Badge, Button, Cell, ConfirmDialog, DataTable, PageHeader, Pagination, SearchBox, StatusBadge, Tabs } from '../../components';
import { TripStatusLabels, TransportModeLabels, type TripStatus } from '../../types/enums';
import type { TripSummary } from '../../types/api';
import { formatDateTime } from '../../utils/format';

const GROUPS: { key: string; label: string; statuses: TripStatus[] }[] = [
  // Mặc định chỉ hiện chuyến còn đang xử lý; chuyến đã giao / hủy được gom vào tab "Hoàn tất / hủy" cho giao diện gọn.
  { key: 'active', label: 'Đang xử lý', statuses: ['SCHEDULED', 'PREPARING', 'READY_FOR_DEPARTURE', 'IN_TRANSIT', 'AT_CHECKPOINT', 'AT_BORDER', 'CUSTOMS_CLEARANCE', 'QUARANTINE', 'DELAYED', 'ARRIVED', 'HANDOVER_PENDING'] },
  { key: 'all', label: 'Tất cả', statuses: [] },
  { key: 'plan', label: 'Chuẩn bị', statuses: ['SCHEDULED', 'PREPARING', 'READY_FOR_DEPARTURE'] },
  { key: 'run', label: 'Đang vận chuyển', statuses: ['IN_TRANSIT', 'AT_CHECKPOINT', 'AT_BORDER', 'CUSTOMS_CLEARANCE', 'QUARANTINE', 'DELAYED'] },
  { key: 'arrive', label: 'Đã đến / bàn giao', statuses: ['ARRIVED', 'HANDOVER_PENDING'] },
  { key: 'done', label: 'Hoàn tất / hủy', statuses: ['DELIVERED', 'CANCELLED'] },
];

export function TripListPage() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const [tab, setTab] = useState('active');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const dq = useDebounced(q);
  const statuses = GROUPS.find((g) => g.key === tab)?.statuses ?? [];
  const state = useAsync(() => tripApi.list({ status: statuses, q: dq || undefined, page, size: 10 }), [tab, dq, page]);
  const customer = user.role === 'CUSTOMER';
  const driver = user.role === 'DRIVER_ESCORT';
  const manager = user.role === 'LOGISTICS_MANAGER';
  const { run } = useAction();
  const [removing, setRemoving] = useState<TripSummary | null>(null);
  const [cleanup, setCleanup] = useState(false);
  const finished = (t: TripSummary) => t.status === 'DELIVERED' || t.status === 'CANCELLED';
  return (
    <>
      <PageHeader title={customer ? 'Chuyến đi của tôi' : driver ? 'Chuyến của tôi' : 'Chuyến đi'} subtitle={customer ? 'Theo dõi hành trình ngựa của bạn.' : driver ? 'Các chuyến bạn được phân công.' : 'Toàn bộ chuyến theo quyền của bạn.'}
        actions={manager && <Button variant="danger-outline" icon="bi-trash" onClick={() => setCleanup(true)}>Dọn chuyến đã hoàn tất</Button>} />
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
          ...(manager ? [{ key: 'x', header: '', align: 'right' as const, render: (t: TripSummary) => (finished(t) ? <Button size="sm" variant="danger-outline" icon="bi-trash" title="Xóa chuyến đã bàn giao / đã hủy" onClick={(e) => { e.stopPropagation(); setRemoving(t); }}>Xóa</Button> : null) }] : []),
        ]}
        footer={state.data && <Pagination page={state.data.page} size={state.data.size} totalElements={state.data.totalElements} totalPages={state.data.totalPages} onPage={setPage} />} />
      <ConfirmDialog open={!!removing} onClose={() => setRemoving(null)} tone="error" confirmLabel="Xóa chuyến" title="Xóa chuyến này?"
        message={removing ? `Chuyến ${removing.tripNumber} sẽ được lưu trữ: biến mất khỏi danh sách nhưng số liệu báo cáo và lịch sử vẫn được giữ.` : undefined}
        onConfirm={async () => { if (removing) { await run(() => tripApi.archive(removing.id), 'Đã xóa chuyến'); refreshCounts(); state.reload(); } }} />
      <ConfirmDialog open={cleanup} onClose={() => setCleanup(false)} tone="error" confirmLabel="Dọn tất cả" title="Dọn mọi chuyến đã bàn giao / đã hủy?"
        message="Toàn bộ chuyến ở trạng thái Đã giao hoặc Đã hủy sẽ được lưu trữ (ẩn khỏi danh sách). Báo cáo và lịch sử vẫn được giữ."
        onConfirm={async () => { await run(() => tripApi.archiveFinished(0), (r) => `Đã dọn ${r.archived} chuyến`); refreshCounts(); state.reload(); }} />
    </>
  );
}
