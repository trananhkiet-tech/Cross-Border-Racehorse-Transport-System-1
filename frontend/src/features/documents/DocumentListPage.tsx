import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { openFile } from '../../api/client';
import { documentApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { useDebounced } from '../../hooks/useQueryState';
import { Button, Cell, DataTable, PageHeader, Pagination, SearchBox, SelectField, StatusBadge, Tabs, enumOptions } from '../../components';
import { DocumentReviewStatusLabels, DocumentTypeLabels, DocumentTypeValues, type DocumentReviewStatus, type DocumentType } from '../../types/enums';
import type { DocumentItem } from '../../types/api';
import { formatBytes, formatDate } from '../../utils/format';

type Tab = 'PENDING_REVIEW' | 'ALL' | 'EXPIRING' | 'PROBLEM';
const TAB_STATUS: Record<Exclude<Tab, 'EXPIRING'>, DocumentReviewStatus[]> = {
  PENDING_REVIEW: ['PENDING_REVIEW'],
  ALL: [],
  PROBLEM: ['REJECTED', 'NEEDS_UPDATE', 'EXPIRED'],
};

export function DocumentListPage() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const initial: Tab = params.get('expiring') ? 'EXPIRING' : params.get('status') === 'PENDING_REVIEW' ? 'PENDING_REVIEW' : user.role === 'TRANSPORT_SPECIALIST' ? 'PENDING_REVIEW' : 'ALL';
  const [tab, setTab] = useState<Tab>(initial);
  const [type, setType] = useState<DocumentType | ''>('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const dq = useDebounced(q);
  const { run, busy } = useAction();

  const list = useAsync(
    () => documentApi.search({ status: tab === 'EXPIRING' ? [] : TAB_STATUS[tab], type: type ? [type] : [], q: dq || undefined, page, size }),
    [tab, type, dq, page, size], tab !== 'EXPIRING',
  );
  const expiring = useAsync(() => documentApi.expiring(30), [], tab === 'EXPIRING');
  const isSpecialist = user.role === 'TRANSPORT_SPECIALIST';

  const columns = (rows?: DocumentItem[]) => [
    { key: 'type', header: 'Loại tài liệu', sortValue: (d: DocumentItem) => d.documentType, render: (d: DocumentItem) => <Cell main={DocumentTypeLabels[d.documentType]} sub={d.horseName} /> },
    { key: 'file', header: 'Tệp', render: (d: DocumentItem) => <Cell main={<button type="button" className="ir-link" style={{ border: 0, background: 'none', padding: 0 }} onClick={(e) => { e.stopPropagation(); void openFile(documentApi.filePath(d.id)); }}>{d.fileName}</button>} sub={`${formatBytes(d.sizeBytes)} · ${d.uploadedByName}`} /> },
    { key: 'req', header: 'Yêu cầu', render: (d: DocumentItem) => <Cell mono main={d.requestNumber ?? '—'} sub={d.customerName} /> },
    { key: 'exp', header: 'Hết hạn', sortValue: (d: DocumentItem) => d.expiryDate, render: (d: DocumentItem) => <Cell main={formatDate(d.expiryDate)} sub={d.expired ? 'Đã hết hạn' : d.daysToExpiry != null ? `còn ${d.daysToExpiry} ngày` : undefined} /> },
    { key: 'up', header: 'Tải lên', sortValue: (d: DocumentItem) => d.uploadedAt, render: (d: DocumentItem) => formatDate(d.uploadedAt) },
    { key: 'st', header: 'Trạng thái', render: (d: DocumentItem) => <StatusBadge status={d.reviewStatus} labels={DocumentReviewStatusLabels} /> },
    ...(rows ? [] : []),
  ];

  return (
    <>
      <PageHeader
        title={isSpecialist ? 'Rà soát tài liệu' : 'Tài liệu'}
        subtitle="Tài liệu hồ sơ ngựa và tuân thủ. Tệp chỉ tải được khi bạn có quyền với yêu cầu liên quan."
        actions={isSpecialist && (
          <Button variant="outline" icon="bi-calendar-x" loading={busy} onClick={async () => { await run(() => documentApi.markExpired(), (r) => `Đã đánh dấu hết hạn ${r.updated} tài liệu`); list.reload(); expiring.reload(); }}>
            Đánh dấu tài liệu hết hạn
          </Button>
        )}
      />
      <Tabs<Tab>
        tabs={[{ key: 'PENDING_REVIEW', label: 'Chờ rà soát' }, { key: 'PROBLEM', label: 'Có vấn đề' }, { key: 'EXPIRING', label: 'Sắp hết hạn (30 ngày)' }, { key: 'ALL', label: 'Tất cả' }]}
        active={tab} onChange={(t) => { setTab(t); setPage(0); }}
      />
      {tab === 'EXPIRING' ? (
        <DataTable<DocumentItem> rows={expiring.data} loading={expiring.loading} error={expiring.error} onRetry={expiring.reload} rowKey={(d) => d.id}
          onRowClick={(d) => navigate(`/documents/${d.id}`)} columns={columns()} emptyTitle="Không có tài liệu sắp hết hạn" emptyIcon="bi-calendar-check" />
      ) : (
        <DataTable<DocumentItem> rows={list.data?.content} loading={list.loading} error={list.error} onRetry={list.reload} rowKey={(d) => d.id}
          onRowClick={(d) => navigate(`/documents/${d.id}`)} columns={columns()} emptyTitle="Không có tài liệu phù hợp" emptyIcon="bi-file-earmark"
          toolbar={<>
            <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Tên tệp, số tài liệu, mã yêu cầu…" width={320} />
            <div style={{ width: 240 }}><SelectField size="sm" value={type} onChange={(v) => { setType(v as DocumentType | ''); setPage(0); }} placeholder="Mọi loại tài liệu" options={enumOptions(DocumentTypeValues, DocumentTypeLabels)} /></div>
          </>}
          footer={list.data && <Pagination page={list.data.page} size={list.data.size} totalElements={list.data.totalElements} totalPages={list.data.totalPages} onPage={setPage} onSize={(s) => { setSize(s); setPage(0); }} />} />
      )}
    </>
  );
}
