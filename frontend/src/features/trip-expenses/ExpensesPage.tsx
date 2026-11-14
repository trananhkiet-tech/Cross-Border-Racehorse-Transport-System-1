import { useState } from 'react';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { useCurrentUser } from '../../app/AuthContext';
import { Button, Cell, ConfirmDialog, DataTable, PageHeader, StatusBadge, Tabs } from '../../components';
import { formatDateTime, formatMoney } from '../../utils/format';
import { expenseApi, ExpenseCategoryLabels, ExpenseStatusLabels, type Expense, type ExpenseStatus } from './api';
import { ExpenseFormModal } from './ExpenseFormModal';

const TABS: { key: string; label: string; status?: ExpenseStatus }[] = [
  { key: 'pending', label: 'Chờ duyệt', status: 'PENDING' },
  { key: 'approved', label: 'Đã duyệt', status: 'APPROVED' },
  { key: 'rejected', label: 'Từ chối', status: 'REJECTED' },
  { key: 'all', label: 'Tất cả' },
];

/** Chi phí phát sinh trên đường: tài xế / áp tải ghi, Manager / Coordinator duyệt hoàn ứng. */
export function ExpensesPage() {
  const user = useCurrentUser();
  const isDriver = user.role === 'DRIVER_ESCORT';
  const [tab, setTab] = useState('pending');
  const status = TABS.find((t) => t.key === tab)?.status;
  const state = useAsync(() => expenseApi.list(status), [tab]);
  const [adding, setAdding] = useState(false);
  const [rejecting, setRejecting] = useState<Expense | undefined>();
  const { run } = useAction();

  const approve = (e: Expense) => run(() => expenseApi.review(e.id, true), 'Đã duyệt hoàn ứng').then(state.reload).catch(() => undefined);

  return (
    <>
      <PageHeader title="Chi phí phát sinh" subtitle={isDriver ? 'Ghi các khoản bạn đã ứng trên đường để được hoàn.' : 'Duyệt các khoản chi phí tài xế / áp tải đã ứng.'}
        actions={isDriver ? <Button icon="bi-plus-lg" onClick={() => setAdding(true)}>Ghi chi phí</Button> : undefined} />
      <Tabs tabs={TABS.map((t) => ({ key: t.key, label: t.label }))} active={tab} onChange={setTab} />
      <DataTable<Expense> rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(e) => e.id}
        emptyTitle="Chưa có chi phí" emptyIcon="bi-cash-coin"
        columns={[
          { key: 't', header: 'Chuyến', render: (e) => <Cell mono main={e.tripNumber ?? `#${e.tripId}`} sub={e.staffName} /> },
          { key: 'c', header: 'Loại', render: (e) => <Cell main={ExpenseCategoryLabels[e.category]} sub={e.description} /> },
          { key: 'a', header: 'Số tiền', sortValue: (e) => e.amount, render: (e) => formatMoney(e.amount, e.currency) },
          { key: 'd', header: 'Ngày ghi', sortValue: (e) => e.createdAt, render: (e) => formatDateTime(e.createdAt) },
          { key: 's', header: 'Trạng thái', render: (e) => <Cell main={<StatusBadge status={e.status} labels={ExpenseStatusLabels} />} sub={e.reviewNote} /> },
          { key: 'x', header: '', render: (e) => (!isDriver && e.status === 'PENDING'
            ? <span style={{ display: 'flex', gap: 6 }}><Button size="sm" onClick={() => approve(e)}>Duyệt</Button><Button size="sm" variant="outline" onClick={() => setRejecting(e)}>Từ chối</Button></span> : null) },
        ]} />
      <ExpenseFormModal open={adding} onClose={() => setAdding(false)} onDone={state.reload} />
      <ConfirmDialog open={!!rejecting} title="Từ chối chi phí" message={rejecting ? `${formatMoney(rejecting.amount, rejecting.currency)} – ${rejecting.staffName ?? ''}` : ''}
        confirmLabel="Từ chối" tone="error" requireReason reasonLabel="Lý do từ chối"
        onConfirm={async (reason) => { if (rejecting) { await run(() => expenseApi.review(rejecting.id, false, reason), 'Đã từ chối'); state.reload(); } }}
        onClose={() => setRejecting(undefined)} />
    </>
  );
}
