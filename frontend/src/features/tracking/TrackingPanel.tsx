import { tripApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { AsyncView, Badge, Card, DataTable, DescriptionList, Progress, RouteMap, StatusBadge, Timeline } from '../../components';
import type { TimelineEntry } from '../../components';
import { StopTypeLabels, TrackingEventTypeLabels, TripStatusLabels } from '../../types/enums';
import type { StopProgress, Tracking } from '../../types/api';
import { formatDateTime, minutesToText } from '../../utils/format';

export function TrackingView({ t }: { t: Tracking }) {
  const stops = t.stops.map((s) => s.stop);
  const done = t.stops.filter((s) => s.state === 'DONE').map((s) => s.stop.id);
  const events: TimelineEntry[] = [...t.events].reverse().map((e, i) => ({
    key: e.id,
    title: TrackingEventTypeLabels[e.eventType],
    time: formatDateTime(e.eventTime),
    description: [e.location, e.routeStopName, e.note].filter(Boolean).join(' · ') || undefined,
    meta: `${e.recordedBy}${e.delayMinutes ? ` · trễ ${minutesToText(e.delayMinutes)}` : ''}`,
    state: i === 0 ? 'current' : 'done',
    badge: <StatusBadge status={e.tripStatus} labels={TripStatusLabels} />,
  }));
  return (
    <div className="app-grid app-grid-main">
      <div className="ir-stack">
        <Card title="Bản đồ hành trình" icon="bi-map" subtitle="Sơ đồ minh họa theo tọa độ điểm dừng, không phải GPS thời gian thực.">
          <RouteMap stops={stops} doneStopIds={done} currentStopId={t.currentStopId} showVehicle blocked={t.operationBlocked} />
        </Card>
        <Card title="Điểm dừng" icon="bi-signpost-split" flush>
          <DataTable<StopProgress> flush dense rows={t.stops} rowKey={(s) => s.stop.id} rowClassName={(s) => (s.state === 'CURRENT' ? 'row-info' : undefined)}
            columns={[
              { key: 'o', header: '#', width: 40, render: (s) => s.stop.stopOrder },
              { key: 'n', header: 'Điểm dừng', render: (s) => s.stop.name },
              { key: 't', header: 'Loại', render: (s) => StopTypeLabels[s.stop.stopType] },
              { key: 'p', header: 'Dự kiến', render: (s) => formatDateTime(s.plannedArrival) },
              { key: 'a', header: 'Thực tế', render: (s) => formatDateTime(s.actualArrival) },
              { key: 's', header: 'Trạng thái', render: (s) => (s.state === 'DONE' ? <Badge tone="success" icon="bi-check2">Đã qua</Badge> : s.state === 'CURRENT' ? <Badge tone="primary" icon="bi-geo-alt-fill">Đang ở đây</Badge> : <Badge>Sắp tới</Badge>) },
            ]} />
        </Card>
      </div>
      <div className="ir-stack">
        <Card title="Tiến độ" icon="bi-speedometer2">
          <div style={{ marginBottom: 16 }}><Progress value={t.progressPercent} label="Hoàn thành hành trình" /></div>
          <DescriptionList columns={1} items={[
            { label: 'Vị trí hiện tại', value: t.currentLocation ?? '—' },
            { label: 'Dự kiến đến (kế hoạch)', value: formatDateTime(t.plannedArrival) },
            { label: 'Dự kiến đến (cập nhật)', value: formatDateTime(t.estimatedArrival) },
            { label: 'Tổng thời gian trễ', value: t.totalDelayMinutes ? <Badge tone="warning">{minutesToText(t.totalDelayMinutes)}</Badge> : 'Không trễ' },
            { label: 'Cập nhật lần cuối', value: formatDateTime(t.lastUpdate) },
          ]} />
        </Card>
        <Card title="Nhật ký hành trình" icon="bi-clock-history">
          {events.length ? <Timeline items={events} dense /> : <span className="ir-muted">Chưa có sự kiện hành trình.</span>}
        </Card>
      </div>
    </div>
  );
}

export function TrackingPanel({ tripId, version }: { tripId: number; version: unknown }) {
  const state = useAsync(() => tripApi.tracking(tripId), [tripId, version]);
  return <AsyncView state={state}>{(t) => <TrackingView t={t} />}</AsyncView>;
}
