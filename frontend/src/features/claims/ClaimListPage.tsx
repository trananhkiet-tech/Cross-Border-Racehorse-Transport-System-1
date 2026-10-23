import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { claimApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { Cell, DataTable, LinkButton, PageHeader, Pagination, StatusBadge, Tabs } from '../../components';
import { ClaimStatusLabels, ClaimTypeLabels, type ClaimStatus } from '../../types/enums';
import type { ClaimSummary } from '../../types/api';
import { formatDate } from '../../utils/format';
import { Money } from '../common/bits';

const GROUPS: { key: string; label: string; statuses: ClaimStatus[] }[] = [
  { key: 'open', label: 'Đang xử lý', statuses: ['SUBMITTED', 'UNDER_REVIEW', 'EVIDENCE_REQUIRED'] },
  { key: 'decided', label: 'Đã quyết định', statuses: ['APPROVED', 'PARTIALLY_APPROVED', 'REJECTED', 'PAID'] },
  { key: 'closed', label: 'Đã đóng', statuses: ['CLOSED'] },
  { key: 'all', label: 'Tất cả', statuses: [] },
];

export function ClaimListPage() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const [tab, setTab] = useState('open');
  const [page, setPage] = useState(0);
  const statuses = GROUPS.find((g) => g.key === tab)?.statuses ?? [];
  const state = useAsync(() => claimApi.list({ status: statuses, page, size: 10 }), [tab, page]);
  const customer = user.role === 'CUSTOMER';
  return (
    <>
      <PageHeader title="Bồi thường" subtitle={customer ? 'Hồ sơ bồi thường của bạn.' : 'Thẩm định và quyết định hồ sơ bồi thường.'}
        actions={customer && <LinkButton to="/claims/new" icon="bi-plus-lg">Tạo hồ sơ bồi thường</LinkButton>} />
      <Tabs tabs={GROUPS.map((g) => ({ key: g.key, label: g.label }))} active={tab} onChange={(k) => { setTab(k); setPage(0); }} />
      <DataTable<ClaimSummary> rows={state.data?.content} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(c) => c.id} onRowClick={(c) => navigate(`/claims/${c.id}`)}
        emptyTitle="Chưa có hồ sơ bồi thường" emptyIcon="bi-shield-exclamation"
        columns={[
          { key: 'n', header: 'Hồ sơ', sortValue: (c) => c.claimNumber, render: (c) => <Cell mono main={c.claimNumber} sub={c.requestNumber} /> },
          ...(!customer ? [{ key: 'c', header: 'Khách hàng', render: (c: ClaimSummary) => c.customerName }] : []),
          { key: 't', header: 'Loại', render: (c) => <Cell main={ClaimTypeLabels[c.claimType]} sub={c.horseName ?? c.incidentNumber} /> },
          { key: 'a', header: 'Số tiền yêu cầu', align: 'right', sortValue: (c) => c.claimedAmount, render: (c) => <Money value={c.claimedAmount} currency={c.currency} /> },
          { key: 'p', header: 'Số tiền duyệt', align: 'right', render: (c) => (c.approvedAmount != null ? <Money value={c.approvedAmount} currency={c.currency} /> : '—') },
          { key: 'd', header: 'Nộp ngày', sortValue: (c) => c.submittedAt, render: (c) => formatDate(c.submittedAt) },
          { key: 's', header: 'Trạng thái', render: (c) => <StatusBadge status={c.status} labels={ClaimStatusLabels} /> },
        ]}
        footer={state.data && <Pagination page={state.data.page} size={state.data.size} totalElements={state.data.totalElements} totalPages={state.data.totalPages} onPage={setPage} />} />
    </>
  );
}
