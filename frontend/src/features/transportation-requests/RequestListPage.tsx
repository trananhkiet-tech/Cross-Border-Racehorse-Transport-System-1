import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { requestApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { useDebounced } from '../../hooks/useQueryState';
import { Cell, DataTable, LinkButton, PageHeader, Pagination, SearchBox, StatusBadge, Tabs } from '../../components';
import { RequestStatusLabels, TransportModeLabels, TransportPurposeLabels, type RequestStatus } from '../../types/enums';
import type { RequestSummary } from '../../types/api';
import { formatDate } from '../../utils/format';
import { Money, RouteText } from '../common/bits';
import { STATUS_GROUPS } from './requestStages';

export function RequestListPage() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const explicit = params.getAll('status') as RequestStatus[];
  const [group, setGroup] = useState(explicit.length ? 'custom' : 'all');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const dq = useDebounced(q);
  const statuses = group === 'custom' ? explicit : STATUS_GROUPS.find((g) => g.key === group)?.statuses ?? [];
  const state = useAsync(() => requestApi.list({ status: statuses, q: dq || undefined, page, size }), [group, statuses.join(','), dq, page, size]);
  const isCustomer = user.role === 'CUSTOMER';

  const tabs = [
    ...STATUS_GROUPS.map((g) => ({ key: g.key, label: g.label })),
    ...(group === 'custom' ? [{ key: 'custom', label: explicit.map((s) => RequestStatusLabels[s]).join(', ') }] : []),
  ];

  return (
    <>
      <PageHeader
        title="Yêu cầu vận chuyển"
        subtitle={isCustomer ? 'Theo dõi các yêu cầu bạn đã tạo, từ hồ sơ đến bàn giao.' : 'Danh sách yêu cầu theo quyền xử lý của bạn.'}
        actions={isCustomer && <LinkButton to="/requests/new" icon="bi-plus-circle">Tạo yêu cầu vận chuyển</LinkButton>}
      />
      <Tabs tabs={tabs} active={group} onChange={(k) => { setGroup(k); setPage(0); if (k !== 'custom') setParams({}, { replace: true }); }} />
      <DataTable<RequestSummary>
        rows={state.data?.content}
        loading={state.loading}
        error={state.error}
        onRetry={state.reload}
        rowKey={(r) => r.id}
        onRowClick={(r) => navigate(`/requests/${r.id}`)}
        rowClassName={(r) => (r.status === 'INFORMATION_REQUIRED' ? 'row-warning' : undefined)}
        emptyTitle={isCustomer ? 'Chưa có yêu cầu vận chuyển' : 'Không có yêu cầu phù hợp'}
        emptyDescription={isCustomer ? 'Tạo yêu cầu đầu tiên để nhận checklist hồ sơ và chi phí ước tính.' : undefined}
        emptyIcon="bi-clipboard"
        toolbar={<SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Mã yêu cầu, khách hàng, tên ngựa…" width={320} />}
        columns={[
          { key: 'no', header: 'Mã yêu cầu', sortValue: (r) => r.requestNumber, render: (r) => <Cell mono main={r.requestNumber} sub={TransportPurposeLabels[r.purpose]} /> },
          ...(!isCustomer ? [{ key: 'cust', header: 'Khách hàng', sortValue: (r: RequestSummary) => r.customerName, render: (r: RequestSummary) => r.customerName }] : []),
          { key: 'route', header: 'Lộ trình', render: (r) => <Cell main={<RouteText compact {...r} />} sub={TransportModeLabels[r.transportMode]} /> },
          { key: 'horses', header: 'Ngựa', render: (r) => <Cell main={`${r.horseCount} con`} sub={r.horseNames.join(', ')} /> },
          { key: 'date', header: 'Khởi hành', sortValue: (r) => r.requestedDepartureDate, render: (r) => formatDate(r.requestedDepartureDate) },
          { key: 'value', header: 'Giá trị khai báo', align: 'right', sortValue: (r) => r.declaredValueTotal, render: (r) => <Money value={r.declaredValueTotal} currency={r.currency} /> },
          { key: 'st', header: 'Trạng thái', render: (r) => <StatusBadge status={r.status} labels={RequestStatusLabels} /> },
        ]}
        footer={state.data && <Pagination page={state.data.page} size={state.data.size} totalElements={state.data.totalElements} totalPages={state.data.totalPages} onPage={setPage} onSize={(s) => { setSize(s); setPage(0); }} />}
      />
    </>
  );
}
