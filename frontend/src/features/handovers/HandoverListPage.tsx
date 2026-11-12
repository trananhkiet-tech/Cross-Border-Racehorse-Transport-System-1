import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { handoverApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Cell, DataTable, PageHeader, StatusBadge, Tabs } from '../../components';
import { HandoverStatusLabels, HorseConditionLabels, type HandoverStatus } from '../../types/enums';
import type { Handover } from '../../types/api';
import { formatDateTime } from '../../utils/format';

const GROUPS: { key: string; label: string; statuses: HandoverStatus[] }[] = [
  { key: 'pending', label: 'Chờ xác nhận', statuses: ['PENDING_CONFIRMATION'] },
  { key: 'disputed', label: 'Có phản hồi', statuses: ['DISPUTED'] },
  { key: 'confirmed', label: 'Đã xác nhận', statuses: ['CONFIRMED'] },
  { key: 'all', label: 'Tất cả', statuses: [] },
];

export function HandoverListPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState('pending');
  const statuses = GROUPS.find((g) => g.key === tab)?.statuses ?? [];
  const state = useAsync(() => handoverApi.list(statuses), [tab]);
  return (
    <>
      <PageHeader title="Bàn giao" subtitle="Biên bản bàn giao ngựa tại điểm đến." />
      <Tabs tabs={GROUPS.map((g) => ({ key: g.key, label: g.label }))} active={tab} onChange={setTab} />
      <DataTable<Handover> rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(h) => h.id} onRowClick={(h) => navigate(`/handovers/${h.id}`)}
        rowClassName={(h) => (h.status === 'DISPUTED' ? 'row-error' : undefined)} emptyTitle="Chưa có biên bản bàn giao" emptyIcon="bi-box-seam"
        columns={[
          { key: 'n', header: 'Biên bản', sortValue: (h) => h.handoverNumber, render: (h) => <Cell mono main={h.handoverNumber} sub={h.tripNumber} /> },
          { key: 'h', header: 'Ngựa', render: (h) => <Cell main={h.horseName} sub={h.microchipNumber} mono /> },
          { key: 'r', header: 'Người nhận', render: (h) => <Cell main={h.receiverName} sub={h.receiverContact} /> },
          { key: 'c', header: 'Tình trạng', render: (h) => HorseConditionLabels[h.condition] },
          { key: 't', header: 'Thời điểm', sortValue: (h) => h.handoverTime, render: (h) => formatDateTime(h.handoverTime) },
          { key: 's', header: 'Trạng thái', render: (h) => <StatusBadge status={h.status} labels={HandoverStatusLabels} /> },
        ]} />
    </>
  );
}
