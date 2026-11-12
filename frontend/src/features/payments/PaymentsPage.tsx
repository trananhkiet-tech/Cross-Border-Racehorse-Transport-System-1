import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { paymentApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Cell, DataTable, DemoTag, PageHeader, Pagination, StatusBadge } from '../../components';
import { PaymentMethodLabels } from '../../types/enums';
import type { PaymentItem } from '../../types/api';
import { formatDateTime } from '../../utils/format';
import { Money } from '../common/bits';

export function PaymentsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const state = useAsync(() => paymentApi.payments({ page, size: 15 }), [page]);
  return (
    <>
      <PageHeader title="Thanh toán" subtitle="Toàn bộ giao dịch mô phỏng của khách hàng." meta={<DemoTag>Thanh toán mô phỏng</DemoTag>} />
      <DataTable<PaymentItem> rows={state.data?.content} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(p) => p.id}
        onRowClick={(p) => navigate(`/invoices/${p.invoiceId}`)} emptyTitle="Chưa có giao dịch" emptyIcon="bi-credit-card"
        columns={[
          { key: 'r', header: 'Mã giao dịch', render: (p) => <Cell mono main={p.paymentReference} sub={p.invoiceNumber} /> },
          { key: 'c', header: 'Khách hàng', render: (p) => <Cell main={p.customerName} sub={p.requestNumber} /> },
          { key: 't', header: 'Thời gian', sortValue: (p) => p.paidAt ?? p.createdAt, render: (p) => formatDateTime(p.paidAt ?? p.createdAt) },
          { key: 'm', header: 'Phương thức', render: (p) => PaymentMethodLabels[p.method] },
          { key: 'a', header: 'Số tiền', align: 'right', render: (p) => <Money strong value={p.amount} currency={p.currency} /> },
          { key: 's', header: 'Kết quả', render: (p) => <StatusBadge status={p.status} /> },
        ]}
        footer={state.data && <Pagination page={state.data.page} size={state.data.size} totalElements={state.data.totalElements} totalPages={state.data.totalPages} onPage={setPage} />} />
    </>
  );
}
