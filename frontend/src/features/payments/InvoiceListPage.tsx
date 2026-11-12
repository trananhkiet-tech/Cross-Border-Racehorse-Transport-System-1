import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { paymentApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Cell, DataTable, DemoTag, PageHeader, Pagination, StatusBadge, Tabs } from '../../components';
import { InvoiceStatusLabels, type InvoiceStatus } from '../../types/enums';
import type { Invoice } from '../../types/api';
import { formatDate } from '../../utils/format';
import { Money } from '../common/bits';

const TABS: { key: string; label: string; statuses: InvoiceStatus[] }[] = [
  { key: 'all', label: 'Tất cả', statuses: [] },
  { key: 'open', label: 'Chưa thanh toán đủ', statuses: ['ISSUED', 'PARTIALLY_PAID', 'OVERDUE'] },
  { key: 'paid', label: 'Đã thanh toán', statuses: ['PAID'] },
];

export function InvoiceListPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState('all');
  const [page, setPage] = useState(0);
  const statuses = TABS.find((t) => t.key === tab)?.statuses ?? [];
  const state = useAsync(() => paymentApi.invoices({ status: statuses, page, size: 10 }), [tab, page]);
  return (
    <>
      <PageHeader title="Hoá đơn & thanh toán" subtitle="Thanh toán trong hệ thống là mô phỏng – không xử lý thẻ thật." meta={<DemoTag>Thanh toán mô phỏng</DemoTag>} />
      <Tabs tabs={TABS.map((t) => ({ key: t.key, label: t.label }))} active={tab} onChange={(k) => { setTab(k); setPage(0); }} />
      <DataTable<Invoice> rows={state.data?.content} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(i) => i.id}
        onRowClick={(i) => navigate(`/invoices/${i.id}`)} emptyTitle="Chưa có hoá đơn" emptyDescription="Hoá đơn được tạo sau khi báo giá cuối được chấp nhận." emptyIcon="bi-file-earmark-text"
        rowClassName={(i) => (i.overdue ? 'row-error' : undefined)}
        columns={[
          { key: 'n', header: 'Hoá đơn', sortValue: (i) => i.invoiceNumber, render: (i) => <Cell mono main={i.invoiceNumber} sub={i.requestNumber} /> },
          { key: 'c', header: 'Khách hàng', render: (i) => i.customerName },
          { key: 'd', header: 'Hạn thanh toán', sortValue: (i) => i.dueDate, render: (i) => <Cell main={formatDate(i.dueDate)} sub={i.overdue ? 'Quá hạn' : undefined} /> },
          { key: 't', header: 'Tổng', align: 'right', render: (i) => <Money value={i.totalAmount} currency={i.currency} /> },
          { key: 'p', header: 'Đã thanh toán', align: 'right', render: (i) => <Money value={i.paidAmount} currency={i.currency} /> },
          { key: 'o', header: 'Còn lại', align: 'right', render: (i) => <Money strong value={i.outstandingAmount} currency={i.currency} /> },
          { key: 's', header: 'Trạng thái', render: (i) => <StatusBadge status={i.status} labels={InvoiceStatusLabels} /> },
        ]}
        footer={state.data && <Pagination page={state.data.page} size={state.data.size} totalElements={state.data.totalElements} totalPages={state.data.totalPages} onPage={setPage} />} />
    </>
  );
}
