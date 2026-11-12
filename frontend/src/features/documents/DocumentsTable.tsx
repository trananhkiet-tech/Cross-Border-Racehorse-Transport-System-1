import { useNavigate } from 'react-router-dom';
import { openFile } from '../../api/client';
import { documentApi } from '../../api/endpoints';
import type { AsyncState } from '../../hooks/useAsync';
import { Cell, DataTable, StatusBadge } from '../../components';
import { DocumentReviewStatusLabels, DocumentTypeLabels } from '../../types/enums';
import type { DocumentItem } from '../../types/api';
import { formatBytes, formatDate } from '../../utils/format';

export function DocumentsTable({ state, showRequest = true }: { state: AsyncState<DocumentItem[]>; showRequest?: boolean }) {
  const navigate = useNavigate();
  return (
    <DataTable<DocumentItem>
      rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(d) => d.id}
      onRowClick={(d) => navigate(`/documents/${d.id}`)} emptyTitle="Chưa có tài liệu" emptyIcon="bi-file-earmark"
      columns={[
        { key: 'type', header: 'Loại', sortValue: (d) => d.documentType, render: (d) => <Cell main={DocumentTypeLabels[d.documentType]} sub={d.horseName} /> },
        { key: 'file', header: 'Tệp', render: (d) => (
          <Cell main={<button type="button" className="ir-link" style={{ border: 0, background: 'none', padding: 0 }} onClick={(e) => { e.stopPropagation(); void openFile(documentApi.filePath(d.id)); }}>{d.fileName}</button>} sub={`${formatBytes(d.sizeBytes)} · ${d.uploadedByName}`} />
        ) },
        ...(showRequest ? [{ key: 'req', header: 'Yêu cầu', render: (d: DocumentItem) => <Cell main={d.requestNumber ?? '—'} sub={d.customerName} /> }] : []),
        { key: 'exp', header: 'Hết hạn', sortValue: (d) => d.expiryDate, render: (d) => (
          <Cell main={formatDate(d.expiryDate)} sub={d.daysToExpiry !== undefined && d.daysToExpiry !== null ? (d.expired ? 'Đã hết hạn' : `còn ${d.daysToExpiry} ngày`) : undefined} />
        ) },
        { key: 'st', header: 'Trạng thái', render: (d) => <StatusBadge status={d.reviewStatus} labels={DocumentReviewStatusLabels} /> },
      ]}
    />
  );
}
