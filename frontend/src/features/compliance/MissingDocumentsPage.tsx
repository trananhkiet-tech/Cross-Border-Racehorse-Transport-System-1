import { useNavigate } from 'react-router-dom';
import { complianceApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { Cell, DataTable, PageHeader, StatusBadge } from '../../components';
import { ChecklistItemStatusLabels, ComplianceCategoryLabels, DocumentTypeLabels } from '../../types/enums';
import type { OpenItem } from '../../types/api';
import { formatDate } from '../../utils/format';

export function MissingDocumentsPage() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const state = useAsync(() => complianceApi.openItems(), []);
  return (
    <>
      <PageHeader title="Tài liệu còn thiếu" subtitle={user.role === 'CUSTOMER' ? 'Các giấy tờ bạn cần bổ sung cho yêu cầu đang xử lý.' : 'Các mục checklist chưa đạt, sắp theo ngày khởi hành.'} />
      <DataTable<OpenItem>
        rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(i) => i.itemId}
        onRowClick={(i) => navigate(`/requests/${i.requestId}/compliance`)} emptyTitle="Không có mục nào cần bổ sung" emptyDescription="Mọi hồ sơ đều đầy đủ hoặc đang chờ rà soát." emptyIcon="bi-check2-circle"
        columns={[
          { key: 'req', header: 'Yêu cầu', sortValue: (i) => i.requestNumber, render: (i) => <Cell mono main={i.requestNumber} sub={i.customerName} /> },
          { key: 'item', header: 'Giấy tờ', render: (i) => <Cell main={i.title} sub={`${ComplianceCategoryLabels[i.category]} · ${DocumentTypeLabels[i.documentType]}${i.horseName ? ` · ${i.horseName}` : ''}`} /> },
          { key: 'note', header: 'Ghi chú', render: (i) => i.reviewerNote ?? '—' },
          { key: 'dep', header: 'Khởi hành', sortValue: (i) => i.requestedDepartureDate, render: (i) => formatDate(i.requestedDepartureDate) },
          { key: 'st', header: 'Trạng thái', render: (i) => <StatusBadge status={i.status} labels={ChecklistItemStatusLabels} /> },
        ]}
      />
    </>
  );
}
