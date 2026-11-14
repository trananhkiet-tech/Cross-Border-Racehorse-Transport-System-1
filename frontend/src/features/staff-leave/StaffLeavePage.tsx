import { useState } from 'react';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { useCurrentUser } from '../../app/AuthContext';
import { Button, Cell, ConfirmDialog, DataTable, PageHeader } from '../../components';
import { formatDate } from '../../utils/format';
import { leaveApi, type Leave } from './api';
import { LeaveFormModal } from './LeaveFormModal';

/** Lịch nghỉ phép: Coordinator/Manager quản lý, tài xế / áp tải xem lịch của mình. */
export function StaffLeavePage() {
  const user = useCurrentUser();
  const isStaffSelf = user.role === 'DRIVER_ESCORT';
  const state = useAsync(() => (isStaffSelf ? leaveApi.mine() : leaveApi.upcoming()), [isStaffSelf]);
  const [adding, setAdding] = useState(false);
  const [cancelling, setCancelling] = useState<Leave | undefined>();
  const { run } = useAction();

  const doCancel = async () => {
    if (!cancelling) return;
    await run(() => leaveApi.cancel(cancelling.id), 'Đã hủy kỳ nghỉ');
    state.reload();
  };
  return (
    <>
      <PageHeader title="Lịch nghỉ phép" subtitle="Nhân sự đang nghỉ sẽ không được phân công chuyến trong khoảng này."
        actions={isStaffSelf ? undefined : <Button icon="bi-plus-lg" onClick={() => setAdding(true)}>Thêm lịch nghỉ</Button>} />
      <DataTable<Leave> rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(l) => l.id}
        emptyTitle="Chưa có lịch nghỉ" emptyIcon="bi-calendar-x"
        columns={[
          { key: 's', header: 'Nhân sự', sortValue: (l) => l.staffName, render: (l) => <Cell main={l.staffName} sub={l.reason} /> },
          { key: 'f', header: 'Từ ngày', sortValue: (l) => l.startDate, render: (l) => formatDate(l.startDate) },
          { key: 't', header: 'Đến ngày', render: (l) => formatDate(l.endDate) },
          { key: 'st', header: 'Trạng thái', render: (l) => (l.status === 'ACTIVE' ? 'Có hiệu lực' : 'Đã hủy') },
          { key: 'a', header: '', render: (l) => (!isStaffSelf && l.status === 'ACTIVE' ? <Button size="sm" variant="outline" onClick={() => setCancelling(l)}>Hủy</Button> : null) },
        ]} />
      <LeaveFormModal open={adding} onClose={() => setAdding(false)} onDone={state.reload} />
      <ConfirmDialog open={!!cancelling} title="Hủy kỳ nghỉ?" message={`${cancelling?.staffName ?? ''}: ${formatDate(cancelling?.startDate)} – ${formatDate(cancelling?.endDate)}`}
        confirmLabel="Hủy kỳ nghỉ" tone="error" onConfirm={doCancel} onClose={() => setCancelling(undefined)} />
    </>
  );
}
