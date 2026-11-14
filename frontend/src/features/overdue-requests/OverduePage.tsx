import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Alert, Button, Cell, DataTable, PageHeader, SelectField, StatusBadge } from '../../components';
import { RequestStatusLabels } from '../../types/enums';
import { formatDateTime } from '../../utils/format';
import { overdueApi, type OverdueItem } from './api';

const DAY_OPTIONS = [1, 2, 3, 5, 7].map((d) => ({ value: String(d), label: `Quá ${d} ngày` }));

/** Manager: yêu cầu đang chờ nhân viên xử lý mà lâu không có cập nhật. */
export function OverduePage() {
  const navigate = useNavigate();
  const [days, setDays] = useState(3);
  const state = useAsync(() => overdueApi.report(days), [days]);
  const { run, busy } = useAction();
  const rep = state.data;
  return (
    <>
      <PageHeader title="Yêu cầu quá hạn xử lý" subtitle="Yêu cầu đang chờ nhân viên mà không có cập nhật trong khoảng thời gian đã chọn."
        actions={<Button icon="bi-bell" loading={busy} disabled={!rep || rep.total === 0}
          onClick={() => run(() => overdueApi.notify(days), 'Đã gửi thông báo cho quản lý').catch(() => undefined)}>Gửi nhắc việc</Button>} />
      <div style={{ maxWidth: 240, marginBottom: 12 }}>
        <SelectField label="Ngưỡng" value={String(days)} onChange={(v) => setDays(Number(v))} options={DAY_OPTIONS} />
      </div>
      {rep && rep.critical > 0 && <Alert tone="critical" title={`${rep.critical} yêu cầu nghiêm trọng`}>Đã chờ quá {rep.thresholdDays * 2} ngày. Cần xử lý ngay.</Alert>}
      <DataTable<OverdueItem> rows={rep?.items} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(r) => r.id}
        onRowClick={(r) => navigate(`/requests/${r.id}`)} rowClassName={(r) => (r.severity === 'CRITICAL' ? 'row-error' : undefined)}
        emptyTitle="Không có yêu cầu nào quá hạn" emptyIcon="bi-check2-circle"
        columns={[
          { key: 'n', header: 'Yêu cầu', sortValue: (r) => r.requestNumber, render: (r) => <Cell mono main={r.requestNumber} sub={r.customerName} /> },
          { key: 's', header: 'Trạng thái', render: (r) => <StatusBadge status={r.status} labels={RequestStatusLabels} /> },
          { key: 'u', header: 'Cập nhật cuối', render: (r) => formatDateTime(r.lastUpdate) },
          { key: 'd', header: 'Đã chờ', sortValue: (r) => r.daysWaiting, render: (r) => `${r.daysWaiting} ngày` },
          { key: 'v', header: 'Mức độ', render: (r) => (r.severity === 'CRITICAL' ? 'Nghiêm trọng' : 'Cảnh báo') },
        ]} />
    </>
  );
}
