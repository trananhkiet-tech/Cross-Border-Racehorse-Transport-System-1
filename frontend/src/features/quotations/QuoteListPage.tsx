import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { quoteApi, requestApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Alert, Button, Card, Cell, DataTable, PageHeader, Pagination, StatusBadge, Tabs } from '../../components';
import { QuoteStatusLabels, type QuoteStatus } from '../../types/enums';
import type { Quote, RequestSummary } from '../../types/api';
import { formatDate } from '../../utils/format';
import { Money, RouteText } from '../common/bits';

const TABS: { key: string; label: string; statuses: QuoteStatus[] }[] = [
  { key: 'all', label: 'Tất cả', statuses: [] },
  { key: 'draft', label: 'Bản nháp', statuses: ['DRAFT'] },
  { key: 'sent', label: 'Đã gửi', statuses: ['SENT'] },
  { key: 'accepted', label: 'Đã chấp nhận', statuses: ['ACCEPTED'] },
  { key: 'closed', label: 'Từ chối / hết hạn', statuses: ['REJECTED', 'EXPIRED', 'SUPERSEDED'] },
];

/** Yêu cầu đã duyệt nhưng chưa có báo giá cuối: Manager bấm để tạo bản nháp. */
function PendingQuotes({ highlight }: { highlight?: number }) {
  const navigate = useNavigate();
  const { run, busy } = useAction();
  const state = useAsync(() => requestApi.list({ status: ['APPROVED', 'QUOTE_REJECTED'], size: 50 }), []);
  const create = async (r: RequestSummary) => {
    const q = await run(() => quoteApi.createFinal(r.id), 'Đã tạo bản nháp báo giá cuối');
    navigate(`/quotes/${q.id}`);
  };
  return (
    <Card title="Yêu cầu cần lập báo giá cuối" icon="bi-calculator" subtitle="Yêu cầu đã được phê duyệt hoặc báo giá trước bị từ chối." flush>
      <DataTable<RequestSummary> flush dense rows={state.data?.content} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(r) => r.id}
        rowClassName={(r) => (r.id === highlight ? 'is-selected' : undefined)} emptyTitle="Không có yêu cầu nào đang chờ báo giá" emptyIcon="bi-check2-circle"
        columns={[
          { key: 'n', header: 'Yêu cầu', render: (r) => <Cell mono main={r.requestNumber} sub={r.customerName} /> },
          { key: 'r', header: 'Lộ trình', render: (r) => <RouteText compact {...r} /> },
          { key: 'h', header: 'Ngựa', render: (r) => r.horseNames.join(', ') },
          { key: 's', header: 'Trạng thái', render: (r) => <StatusBadge status={r.status} /> },
          { key: 'a', header: '', align: 'right', render: (r) => <Button size="sm" icon="bi-plus-lg" loading={busy} onClick={(e) => { e.stopPropagation(); void create(r); }}>Lập báo giá</Button> },
        ]} />
    </Card>
  );
}

export function QuoteListPage() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const requestId = Number(params.get('requestId')) || undefined;
  const [tab, setTab] = useState('all');
  const [page, setPage] = useState(0);
  const statuses = TABS.find((t) => t.key === tab)?.statuses ?? [];
  const state = useAsync(() => quoteApi.list({ status: statuses, page, size: 10 }), [tab, page]);
  const isManager = user.role === 'LOGISTICS_MANAGER';

  return (
    <>
      <PageHeader title="Báo giá" subtitle={isManager ? 'Lập, gửi và theo dõi báo giá cuối cho khách hàng.' : 'Báo giá cuối bạn nhận được. Chi phí ước tính xem trong chi tiết yêu cầu.'} />
      {isManager && <PendingQuotes highlight={requestId} />}
      {!isManager && <Alert tone="info">Chỉ báo giá cuối đã được gửi mới có thể chấp nhận. Giá trị khai báo của ngựa không phải giá vận chuyển.</Alert>}
      <Tabs tabs={TABS.map((t) => ({ key: t.key, label: t.label }))} active={tab} onChange={(k) => { setTab(k); setPage(0); }} />
      <DataTable<Quote>
        rows={state.data?.content} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(q) => q.id}
        onRowClick={(q) => navigate(`/quotes/${q.id}`)} emptyTitle="Chưa có báo giá" emptyIcon="bi-receipt"
        columns={[
          { key: 'n', header: 'Báo giá', sortValue: (q) => q.quoteNumber, render: (q) => <Cell mono main={q.quoteNumber} sub={`Phiên bản ${q.versionNo}`} /> },
          { key: 'r', header: 'Yêu cầu', render: (q) => <Cell mono main={q.requestNumber} sub={isManager ? q.customerName : q.route} /> },
          { key: 't', header: 'Tổng cộng', align: 'right', sortValue: (q) => q.totalAmount, render: (q) => <Money strong value={q.totalAmount} currency={q.currency} /> },
          { key: 'v', header: 'Hiệu lực đến', render: (q) => <Cell main={formatDate(q.validUntil)} sub={q.expired ? 'Đã hết hạn' : undefined} /> },
          { key: 's', header: 'Trạng thái', render: (q) => <StatusBadge status={q.status} labels={QuoteStatusLabels} /> },
        ]}
        footer={state.data && <Pagination page={state.data.page} size={state.data.size} totalElements={state.data.totalElements} totalPages={state.data.totalPages} onPage={setPage} />}
      />
    </>
  );
}
