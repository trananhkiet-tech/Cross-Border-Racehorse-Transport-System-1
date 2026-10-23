import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { incidentApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Badge, Cell, DataTable, PageHeader, Pagination, StatusBadge, Tabs } from '../../components';
import { IncidentSeverityLabels, IncidentStatusLabels, IncidentTypeLabels, OperationalDecisionLabels, type IncidentStatus } from '../../types/enums';
import type { IncidentSummary } from '../../types/api';
import { formatDateTime } from '../../utils/format';

const GROUPS: { key: string; label: string; statuses: IncidentStatus[] }[] = [
  { key: 'open', label: 'Đang xử lý', statuses: ['OPEN', 'UNDER_REVIEW', 'ACTION_REQUIRED', 'MONITORING'] },
  { key: 'done', label: 'Đã giải quyết / đóng', statuses: ['RESOLVED', 'CLOSED'] },
  { key: 'all', label: 'Tất cả', statuses: [] },
];

export function IncidentListPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState('open');
  const [page, setPage] = useState(0);
  const statuses = GROUPS.find((g) => g.key === tab)?.statuses ?? [];
  const state = useAsync(() => incidentApi.list({ status: statuses, page, size: 10 }), [tab, page]);
  return (
    <>
      <PageHeader title="Sự cố" subtitle="Theo dõi và xử lý sự cố phát sinh trong quá trình vận chuyển." />
      <Tabs tabs={GROUPS.map((g) => ({ key: g.key, label: g.label }))} active={tab} onChange={(k) => { setTab(k); setPage(0); }} />
      <DataTable<IncidentSummary> rows={state.data?.content} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(i) => i.id} onRowClick={(i) => navigate(`/incidents/${i.id}`)}
        rowClassName={(i) => (i.tripBlocked ? 'row-error' : undefined)} emptyTitle="Không có sự cố" emptyIcon="bi-shield-check"
        emptyDescription="Chưa có sự cố nào trong nhóm này."
        columns={[
          { key: 'n', header: 'Sự cố', sortValue: (i) => i.incidentNumber, render: (i) => <Cell mono main={i.incidentNumber} sub={i.title} /> },
          { key: 't', header: 'Loại', render: (i) => IncidentTypeLabels[i.incidentType] },
          { key: 'sv', header: 'Mức độ', sortValue: (i) => i.severity, render: (i) => <StatusBadge status={i.severity} labels={IncidentSeverityLabels} /> },
          { key: 'tr', header: 'Chuyến', render: (i) => <Cell mono main={i.tripNumber} sub={i.horseName ?? i.customerName} /> },
          { key: 'd', header: 'Quyết định', render: (i) => (i.decision ? OperationalDecisionLabels[i.decision] : '—') },
          { key: 'r', header: 'Báo lúc', sortValue: (i) => i.reportedAt, render: (i) => <Cell main={formatDateTime(i.reportedAt)} sub={i.reportedBy} /> },
          { key: 's', header: 'Trạng thái', render: (i) => <span className="app-row" style={{ gap: 4 }}><StatusBadge status={i.status} labels={IncidentStatusLabels} />{i.tripBlocked && <Badge tone="critical">Chuyến tạm dừng</Badge>}</span> },
        ]}
        footer={state.data && <Pagination page={state.data.page} size={state.data.size} totalElements={state.data.totalElements} totalPages={state.data.totalPages} onPage={setPage} />} />
    </>
  );
}
