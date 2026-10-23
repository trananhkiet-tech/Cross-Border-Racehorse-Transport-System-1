import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { tripApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import {
  Alert, AsyncView, Badge, Button, Card, ConfirmDialog, DataTable, DescriptionList, EmptyState, PageHeader, Stepper,
  StatusBadge, Tabs, Timeline,
} from '../../components';
import type { StepItem, TimelineEntry } from '../../components';
import { useCurrentUser } from '../../app/AuthContext';
import { refreshCounts } from '../../layouts/AppLayout';
import {
  HorseBehaviorLabels, HorseConditionLabels, IntakeLevelLabels, MovementConditionLabels, ObservationSeverityLabels,
  StaffRoleLabels, StressLevelLabels, TransportModeLabels, TripStatusLabels, VehicleTypeLabels, HandoverStatusLabels,
  IncidentSeverityLabels,
} from '../../types/enums';
import type { TripDetail } from '../../types/api';
import { formatDateTime, minutesToText } from '../../utils/format';
import { TrackingPanel } from '../tracking/TrackingPanel';
import { ArrivalCheckModal, ObservationModal, PreCheckModal } from '../health-observations/HealthForms';
import { AttachmentList } from '../common/Attachments';
import { RefLink } from '../common/bits';
import { AssignStaffModal, AssignVehicleModal, CheckpointModal, EditPlanModal, StatusModal } from './TripModals';
import { IncidentReportModal } from '../incidents/IncidentReportModal';
import { HandoverFormModal } from '../handovers/HandoverFormModal';

type TabKey = 'overview' | 'tracking' | 'health' | 'incidents' | 'handover' | 'timeline';
type Dlg = 'vehicle' | 'staff' | 'plan' | 'cancel' | 'start' | 'checkpoint' | 'status' | 'pre' | 'obs' | 'arrival' | 'incident' | 'handover' | null;

const FLOW: { key: string; label: string; match: string[] }[] = [
  { key: 'prep', label: 'Chuẩn bị', match: ['SCHEDULED', 'PREPARING', 'READY_FOR_DEPARTURE'] },
  { key: 'run', label: 'Vận chuyển', match: ['IN_TRANSIT', 'AT_CHECKPOINT', 'AT_BORDER', 'CUSTOMS_CLEARANCE', 'QUARANTINE', 'DELAYED'] },
  { key: 'arr', label: 'Đến nơi', match: ['ARRIVED'] },
  { key: 'hand', label: 'Bàn giao', match: ['HANDOVER_PENDING'] },
  { key: 'done', label: 'Hoàn tất', match: ['DELIVERED'] },
];

function flowSteps(status: string): StepItem[] {
  if (status === 'CANCELLED') return FLOW.map((f) => ({ label: f.label, state: 'upcoming' as const }));
  const idx = FLOW.findIndex((f) => f.match.includes(status));
  return FLOW.map((f, i) => ({ label: f.label, state: status === 'DELIVERED' || i < idx ? 'done' : i === idx ? 'current' : 'upcoming' }));
}

export function TripDetailPage() {
  const id = Number(useParams().id);
  const user = useCurrentUser();
  const state = useAsync(() => tripApi.get(id), [id]);
  const [tab, setTab] = useState<TabKey>('overview');
  const [dlg, setDlg] = useState<Dlg>(null);
  const [version, setVersion] = useState(0);
  const { run } = useAction();
  const navigate = useNavigate();

  const refresh = () => { state.reload(); setVersion((v) => v + 1); refreshCounts(); };
  const close = () => setDlg(null);

  return (
    <AsyncView state={state}>
      {(t) => {
        const has = (a: string) => t.allowedActions.includes(a);
        const modalProps = { open: false, trip: t, onClose: close, onDone: refresh };
        return (
          <>
            <PageHeader
              title={t.tripNumber}
              back={{ to: '/trips', label: 'Danh sách chuyến' }}
              meta={<span className="app-row" style={{ gap: 6 }}><StatusBadge status={t.status} labels={TripStatusLabels} size="md" />{t.operationBlocked && <Badge tone="critical" icon="bi-hand-thumbs-down">Tạm dừng chờ quyết định</Badge>}</span>}
              subtitle={`${t.routeName} · ${t.originLocation} → ${t.destinationLocation}`}
              actions={
                <>
                  {has('EDIT_PLAN') && <Button variant="outline" icon="bi-pencil" onClick={() => setDlg('plan')}>Sửa kế hoạch</Button>}
                  {has('ASSIGN_VEHICLE') && <Button variant="outline" icon="bi-truck" onClick={() => setDlg('vehicle')}>{t.vehicle ? 'Đổi xe' : 'Gán xe'}</Button>}
                  {has('ASSIGN_STAFF') && <Button variant="outline" icon="bi-person-plus" onClick={() => setDlg('staff')}>Phân công nhân sự</Button>}
                  {has('PRE_CHECK') && <Button variant="outline" icon="bi-clipboard2-check" onClick={() => setDlg('pre')}>Kiểm tra trước chuyến</Button>}
                  {has('START') && <Button icon="bi-play-fill" onClick={() => setDlg('start')}>Bắt đầu chuyến</Button>}
                  {has('CHECKPOINT') && <Button icon="bi-geo-alt" onClick={() => setDlg('checkpoint')}>Cập nhật hành trình</Button>}
                  {has('UPDATE_STATUS') && t.nextStatuses.length > 0 && <Button variant="outline" icon="bi-arrow-repeat" onClick={() => setDlg('status')}>Đổi trạng thái</Button>}
                  {has('HEALTH_OBSERVATION') && <Button variant="outline" icon="bi-heart-pulse" onClick={() => setDlg('obs')}>Quan sát sức khỏe</Button>}
                  {has('ARRIVAL_CHECK') && <Button variant="outline" icon="bi-flag" onClick={() => setDlg('arrival')}>Kiểm tra khi đến</Button>}
                  {has('HANDOVER') && <Button variant="success" icon="bi-box-arrow-in-down" onClick={() => setDlg('handover')}>Lập biên bản bàn giao</Button>}
                  {has('REPORT_INCIDENT') && <Button variant="danger-outline" icon="bi-exclamation-triangle" onClick={() => setDlg('incident')}>Báo sự cố</Button>}
                  {has('DECIDE_INCIDENT') && t.incidents.length > 0 && <Button variant="danger" icon="bi-shield-exclamation" onClick={() => { const open = t.incidents.find((i) => i.status !== 'CLOSED' && i.status !== 'RESOLVED') ?? t.incidents[0]; navigate(`/incidents/${open.id}`); }}>Xử lý sự cố</Button>}
                  {has('CANCEL') && <Button variant="danger-outline" icon="bi-slash-circle" onClick={() => setDlg('cancel')}>Hủy chuyến</Button>}
                </>
              }
            />
            <Card><Stepper steps={flowSteps(t.status)} /></Card>
            {t.status === 'CANCELLED' && <Alert tone="error">Chuyến đã bị hủy.</Alert>}
            {t.operationBlocked && <Alert tone="critical" title="Chuyến đang tạm dừng">Có sự cố nghiêm trọng đang chờ Quản lý Logistics quyết định (tiếp tục / theo dõi / dừng / đổi tuyến).</Alert>}
            <Tabs<TabKey>
              tabs={[
                { key: 'overview', label: 'Tổng quan' },
                { key: 'tracking', label: 'Theo dõi' },
                { key: 'health', label: 'Sức khỏe' },
                { key: 'incidents', label: 'Sự cố', count: t.incidents.length || undefined, alert: t.operationBlocked },
                { key: 'handover', label: 'Bàn giao' },
                { key: 'timeline', label: 'Dòng thời gian' },
              ]}
              active={tab}
              onChange={setTab}
            />
            {tab === 'overview' && <Overview t={t} hideStaffNote={user.role === 'CUSTOMER'} />}
            {tab === 'tracking' && <TrackingPanel tripId={t.id} version={version} />}
            {tab === 'health' && <HealthTab t={t} version={version} />}
            {tab === 'incidents' && (
              <Card title="Sự cố của chuyến" icon="bi-exclamation-triangle" flush>
                <DataTable flush rows={t.incidents} rowKey={(i) => i.id} emptyTitle="Chưa có sự cố" emptyIcon="bi-shield-check"
                  columns={[
                    { key: 'n', header: 'Mã', render: (i) => <RefLink to={`/incidents/${i.id}`}>{i.incidentNumber}</RefLink> },
                    { key: 't', header: 'Tiêu đề', render: (i) => i.title },
                    { key: 's', header: 'Mức độ', render: (i) => <StatusBadge status={i.severity} labels={IncidentSeverityLabels} /> },
                    { key: 'st', header: 'Trạng thái', render: (i) => <StatusBadge status={i.status} /> },
                    { key: 'r', header: 'Báo lúc', render: (i) => formatDateTime(i.reportedAt) },
                  ]} />
              </Card>
            )}
            {tab === 'handover' && <HandoverTab t={t} version={version} />}
            {tab === 'timeline' && <TimelineTab tripId={t.id} version={version} />}

            <AssignVehicleModal {...modalProps} open={dlg === 'vehicle'} />
            <AssignStaffModal {...modalProps} open={dlg === 'staff'} />
            <EditPlanModal {...modalProps} open={dlg === 'plan'} />
            <CheckpointModal {...modalProps} open={dlg === 'checkpoint'} />
            <StatusModal {...modalProps} open={dlg === 'status'} />
            <PreCheckModal open={dlg === 'pre'} onClose={close} onDone={refresh} tripId={t.id} horses={t.horses} />
            <ObservationModal open={dlg === 'obs'} onClose={close} onDone={refresh} tripId={t.id} horses={t.horses} location={t.currentLocation} />
            <ArrivalCheckModal open={dlg === 'arrival'} onClose={close} onDone={refresh} tripId={t.id} horses={t.horses} />
            <IncidentReportModal open={dlg === 'incident'} onClose={close} onDone={refresh} tripId={t.id} horses={t.horses} location={t.currentLocation} />
            <HandoverFormModal open={dlg === 'handover'} onClose={close} onDone={refresh} trip={t} />
            <ConfirmDialog open={dlg === 'start'} onClose={close} title="Bắt đầu chuyến?" confirmLabel="Bắt đầu" tone="success"
              message="Tất cả ngựa đã được kiểm tra trước chuyến và xe đã được gán. Chuyến sẽ chuyển sang Đang vận chuyển."
              onConfirm={async () => { await run(() => tripApi.start(t.id), 'Chuyến đã bắt đầu'); refresh(); }} />
            <ConfirmDialog open={dlg === 'cancel'} onClose={close} title="Hủy chuyến?" tone="error" requireReason reasonLabel="Lý do hủy"
              message="Xe và nhân sự được giải phóng; yêu cầu quay lại trạng thái chờ lập kế hoạch."
              onConfirm={async (n) => { await run(() => tripApi.cancel(t.id, n), 'Đã hủy chuyến'); refresh(); }} />
          </>
        );
      }}
    </AsyncView>
  );
}

function Overview({ t, hideStaffNote }: { t: TripDetail; hideStaffNote?: boolean }) {
  return (
    <div className="app-grid app-grid-main">
      <div className="ir-stack">
        <Card title="Kế hoạch chuyến" icon="bi-calendar2-week">
          <DescriptionList items={[
            { label: 'Yêu cầu', value: <RefLink to={`/requests/${t.requestId}`}>{t.requestNumber}</RefLink> },
            { label: 'Khách hàng', value: `${t.customerName}${t.customerPhone ? ` · ${t.customerPhone}` : ''}` },
            { label: 'Tuyến', value: <RefLink to={`/routes/${t.routeId}`}>{t.routeCode} – {t.routeName}</RefLink> },
            { label: 'Phương thức', value: TransportModeLabels[t.transportMode] },
            { label: 'Khởi hành (kế hoạch)', value: formatDateTime(t.plannedDeparture) },
            { label: 'Đến nơi (kế hoạch)', value: formatDateTime(t.plannedArrival) },
            { label: 'Khởi hành thực tế', value: formatDateTime(t.actualDeparture) },
            { label: 'Đến nơi thực tế', value: formatDateTime(t.actualArrival) },
            { label: 'Địa chỉ nhận ngựa', value: t.destinationAddress, span: true },
            { label: 'Tổng thời gian trễ', value: t.totalDelayMinutes ? minutesToText(t.totalDelayMinutes) : 'Không trễ' },
            { label: 'Điều phối viên', value: t.coordinatorName },
            { label: 'Hướng dẫn vận chuyển', value: t.transportInstructions, span: true },
            { label: 'Ghi chú', value: t.notes, span: true },
          ]} />
        </Card>
        <Card title={`Ngựa trong chuyến (${t.horses.length})`} icon="bi-postcard-heart" flush>
          <DataTable flush dense rows={t.horses} rowKey={(h) => h.horseId}
            columns={[
              { key: 'n', header: 'Tên ngựa', render: (h) => <RefLink to={`/horses/${h.horseId}`}>{h.name}</RefLink> },
              { key: 'c', header: 'Microchip', render: (h) => <span className="ir-mono">{h.microchipNumber}</span> },
              { key: 's', header: 'Chuồng', render: (h) => `${h.stallTypeName} (${h.stallWidthCm} cm)` },
              { key: 'p', header: 'Kiểm tra trước', render: (h) => (h.preCheckDone ? (h.fitForTravelObserved ? <Badge tone="success" icon="bi-check2">Phù hợp</Badge> : <Badge tone="warning">Cần chú ý</Badge>) : <Badge>Chưa kiểm tra</Badge>) },
              { key: 'a', header: 'Khi đến', render: (h) => (h.arrivalCheckDone ? HorseConditionLabels[h.arrivalCondition ?? 'GOOD'] : '—') },
              { key: 'h', header: 'Bàn giao', render: (h) => (h.handoverId ? <RefLink to={`/handovers/${h.handoverId}`}>{h.handoverStatus ? HandoverStatusLabels[h.handoverStatus] : 'Xem'}</RefLink> : '—') },
            ]} />
        </Card>
      </div>
      <div className="ir-stack">
        <Card title="Phương tiện" icon="bi-truck">
          {t.vehicle ? (
            <DescriptionList columns={1} items={[
              { label: 'Mã xe', value: t.vehicle.vehicleCode },
              { label: 'Biển số', value: t.vehicle.plateNumber },
              { label: 'Loại', value: VehicleTypeLabels[t.vehicle.vehicleType] },
              { label: 'Sức chứa', value: `${t.vehicle.capacityHorses} ngựa` },
              { label: 'Điều hòa', value: t.vehicle.climateControlled ? 'Có' : 'Không' },
            ]} />
          ) : <EmptyState compact icon="bi-truck" title="Chưa gán phương tiện" description="Điều phối viên gán xe trước khi chuyến sẵn sàng." />}
        </Card>
        <Card title="Nhân sự" icon="bi-people">
          {t.staff.length ? (
            <div className="ir-stack" style={{ gap: 8 }}>
              {t.staff.map((s) => (
                <div key={s.assignmentId} className="app-row">
                  <div><div className="ir-strong">{s.fullName}</div><div className="ir-muted">{StaffRoleLabels[s.role]}{s.phone ? ` · ${s.phone}` : ''}</div></div>
                </div>
              ))}
            </div>
          ) : <EmptyState compact icon="bi-people" title="Chưa phân công nhân sự" description={hideStaffNote ? undefined : 'Cần ít nhất một lái xe.'} />}
        </Card>
      </div>
    </div>
  );
}

function HealthTab({ t, version }: { t: TripDetail; version: number }) {
  const pre = useAsync(() => tripApi.preChecks(t.id), [t.id, version]);
  const obs = useAsync(() => tripApi.observations(t.id), [t.id, version]);
  const arr = useAsync(() => tripApi.arrivalChecks(t.id), [t.id, version]);
  return (
    <div className="ir-stack">
      <Alert tone="info">Các mục dưới đây là quan sát của nhân viên vận chuyển. Hệ thống không chẩn đoán và không thay thế bác sĩ thú y.</Alert>
      <Card title="Kiểm tra trước vận chuyển" icon="bi-clipboard2-check" flush>
        <DataTable flush dense rows={pre.data} loading={pre.loading} error={pre.error} onRetry={pre.reload} rowKey={(p) => p.id} emptyTitle="Chưa có kiểm tra trước chuyến"
          columns={[
            { key: 'h', header: 'Ngựa', render: (p) => p.horseName },
            { key: 'c', header: 'Tình trạng', render: (p) => HorseConditionLabels[p.generalCondition] },
            { key: 't', header: 'Nhiệt độ', render: (p) => (p.temperatureC != null ? `${p.temperatureC} °C` : '—') },
            { key: 'k', header: 'Danh mục', render: (p) => <span>{p.identityVerified ? '✓' : '✗'} nhận dạng · {p.documentsPresent ? '✓' : '✗'} giấy tờ · {p.loadingOk ? '✓' : '✗'} lên xe</span> },
            { key: 'f', header: 'Phù hợp', render: (p) => (p.fitForTravelObserved ? <Badge tone="success">Phù hợp</Badge> : <Badge tone="warning">Cần chú ý</Badge>) },
            { key: 'b', header: 'Người kiểm tra', render: (p) => `${p.checkedBy} · ${formatDateTime(p.checkedAt)}` },
          ]} />
      </Card>
      <Card title="Quan sát trong hành trình" icon="bi-heart-pulse" flush>
        <DataTable flush dense rows={obs.data} loading={obs.loading} error={obs.error} onRetry={obs.reload} rowKey={(o) => o.id} emptyTitle="Chưa có quan sát"
          columns={[
            { key: 'h', header: 'Ngựa', render: (o) => o.horseName },
            { key: 'time', header: 'Thời điểm', render: (o) => formatDateTime(o.observedAt) },
            { key: 'd', header: 'Chi tiết', render: (o) => (
              <div>
                <div>Ăn: {IntakeLevelLabels[o.appetite]} · Uống: {IntakeLevelLabels[o.waterIntake]} · {HorseBehaviorLabels[o.behavior]} · Căng thẳng {StressLevelLabels[o.stressLevel].toLowerCase()} · {MovementConditionLabels[o.movement]}</div>
                {o.visibleInjury && <div className="ir-error-text">Vết thương: {o.injuryDescription}</div>}
                {o.notes && <div className="ir-muted">{o.notes}</div>}
                {o.photos.length > 0 && <AttachmentList items={o.photos} />}
              </div>
            ) },
            { key: 's', header: 'Mức độ', render: (o) => <StatusBadge status={o.severity} labels={ObservationSeverityLabels} /> },
            { key: 'i', header: 'Sự cố', render: (o) => (o.incidentId ? <RefLink to={`/incidents/${o.incidentId}`}>Xem</RefLink> : '—') },
            { key: 'a', header: 'Người ghi', render: (o) => o.author },
          ]} />
      </Card>
      <Card title="Kiểm tra khi đến nơi" icon="bi-flag" flush>
        <DataTable flush dense rows={arr.data} loading={arr.loading} error={arr.error} onRetry={arr.reload} rowKey={(a) => a.id} emptyTitle="Chưa kiểm tra khi đến nơi"
          columns={[
            { key: 'h', header: 'Ngựa', render: (a) => a.horseName },
            { key: 'c', header: 'Tình trạng', render: (a) => HorseConditionLabels[a.condition] },
            { key: 'i', header: 'Vết thương', render: (a) => (a.visibleInjury ? <Badge tone="error">Có</Badge> : 'Không') },
            { key: 'n', header: 'Ghi chú', render: (a) => a.notes ?? '—' },
            { key: 'b', header: 'Người kiểm tra', render: (a) => `${a.checkedBy} · ${formatDateTime(a.checkedAt)}` },
          ]} />
      </Card>
    </div>
  );
}

function HandoverTab({ t, version }: { t: TripDetail; version: number }) {
  const state = useAsync(() => tripApi.handovers(t.id), [t.id, version]);
  return (
    <Card title="Biên bản bàn giao" icon="bi-box-arrow-in-down" flush>
      <DataTable flush rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(h) => h.id} emptyTitle="Chưa có biên bản bàn giao" emptyIcon="bi-box-seam"
        columns={[
          { key: 'n', header: 'Mã', render: (h) => <RefLink to={`/handovers/${h.id}`}>{h.handoverNumber}</RefLink> },
          { key: 'h', header: 'Ngựa', render: (h) => h.horseName },
          { key: 'r', header: 'Người nhận', render: (h) => h.receiverName },
          { key: 'c', header: 'Tình trạng', render: (h) => HorseConditionLabels[h.condition] },
          { key: 't', header: 'Thời điểm', render: (h) => formatDateTime(h.handoverTime) },
          { key: 's', header: 'Trạng thái', render: (h) => <StatusBadge status={h.status} labels={HandoverStatusLabels} /> },
        ]} />
    </Card>
  );
}

function TimelineTab({ tripId, version }: { tripId: number; version: number }) {
  const state = useAsync(() => tripApi.timeline(tripId), [tripId, version]);
  return (
    <AsyncView state={state} isEmpty={(d) => d.length === 0} empty={<EmptyState title="Chưa có sự kiện" icon="bi-clock-history" />}>
      {(items) => {
        const entries: TimelineEntry[] = items.map((i, idx) => ({
          key: `${i.kind}-${i.refId ?? idx}-${i.time}`,
          title: i.title,
          time: formatDateTime(i.time),
          description: i.description,
          meta: i.actor,
          state: i.tone === 'error' || i.tone === 'critical' ? 'error' : i.tone === 'warning' ? 'warning' : 'done',
        }));
        return <Card><Timeline items={entries} /></Card>;
      }}
    </AsyncView>
  );
}
