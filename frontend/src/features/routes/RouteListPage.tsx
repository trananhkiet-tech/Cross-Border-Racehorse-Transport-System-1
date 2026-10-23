import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { routeApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { useDebounced } from '../../hooks/useQueryState';
import { Badge, Cell, DataTable, LinkButton, PageHeader, SearchBox, SelectField, StatusBadge, enumOptions } from '../../components';
import { RouteStatusLabels, RouteStatusValues, TransportModeLabels, type RouteStatus } from '../../types/enums';
import type { RouteSummary } from '../../types/api';
import { countryName, formatNumber } from '../../utils/format';

export function RouteListPage() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<RouteStatus | ''>('');
  const dq = useDebounced(q);
  const state = useAsync(() => routeApi.list({ q: dq || undefined, status: status ? [status] : [] }), [dq, status]);
  return (
    <>
      <PageHeader title="Tuyến đường" subtitle="Tuyến chuẩn, điểm dừng và tuyến thay thế dùng khi lập chuyến hoặc đổi tuyến khẩn cấp."
        actions={user.role === 'FLEET_ROUTE_COORDINATOR' && <LinkButton to="/routes/new" icon="bi-plus-lg">Tạo tuyến</LinkButton>} />
      <DataTable<RouteSummary> rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(r) => r.id} onRowClick={(r) => navigate(`/routes/${r.id}`)}
        emptyTitle="Chưa có tuyến phù hợp" emptyIcon="bi-signpost-2"
        toolbar={<>
          <SearchBox value={q} onChange={setQ} placeholder="Mã tuyến, tên, điểm đi/đến…" width={320} />
          <div style={{ width: 200 }}><SelectField size="sm" value={status} onChange={(v) => setStatus(v as RouteStatus | '')} placeholder="Mọi trạng thái" options={enumOptions(RouteStatusValues, RouteStatusLabels)} /></div>
        </>}
        columns={[
          { key: 'c', header: 'Tuyến', sortValue: (r) => r.routeCode, render: (r) => <Cell main={r.name} sub={<>{r.routeCode} {r.alternativeOfId && <Badge tone="info">Thay thế</Badge>}</>} /> },
          { key: 'o', header: 'Lộ trình', render: (r) => <Cell main={`${r.originLocation} → ${r.destinationLocation}`} sub={`${countryName(r.originCountry)} → ${countryName(r.destinationCountry)}`} /> },
          { key: 'm', header: 'Phương thức', render: (r) => TransportModeLabels[r.transportMode] },
          { key: 'd', header: 'Khoảng cách', align: 'right', sortValue: (r) => r.estimatedDistanceKm, render: (r) => (r.estimatedDistanceKm ? `${formatNumber(r.estimatedDistanceKm)} km` : '—') },
          { key: 't', header: 'Thời gian', align: 'right', render: (r) => (r.estimatedDurationHours ? `${formatNumber(r.estimatedDurationHours, 1)} giờ` : '—') },
          { key: 'p', header: 'Điểm dừng', align: 'right', render: (r) => r.stopCount },
          { key: 's', header: 'Trạng thái', render: (r) => <StatusBadge status={r.status} labels={RouteStatusLabels} /> },
        ]} />
    </>
  );
}
