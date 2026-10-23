import { useState } from 'react';
import { vehicleApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { useDebounced } from '../../hooks/useQueryState';
import {
  Alert, Badge, Button, Card, Cell, Checkbox, ConfirmDialog, DataTable, DescriptionList, Icon, Modal, NumberField, PageHeader,
  SearchBox, SelectField, StatusBadge, TextAreaField, TextField, enumOptions,
} from '../../components';
import { VehicleStatusLabels, VehicleStatusValues, VehicleTypeLabels, VehicleTypeValues, type VehicleStatus, type VehicleType } from '../../types/enums';
import type { Vehicle, VehiclePayload } from '../../types/api';
import { formatDate, formatDateTime } from '../../utils/format';

const blank = (): VehiclePayload => ({ vehicleCode: '', plateNumber: '', vehicleType: 'HORSE_TRUCK', capacityHorses: 2, climateControlled: true, cameraMonitoring: false });

function VehicleForm({ id, initial, onClose, onSaved }: { id?: number; initial: VehiclePayload; onClose: () => void; onSaved: () => void }) {
  const { run, busy, fieldErrors } = useAction();
  const [f, setF] = useState(initial);
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const set = <K extends keyof VehiclePayload>(k: K, v: VehiclePayload[K]) => setF((x) => ({ ...x, [k]: v }));
  const submit = async () => {
    const errs = {
      vehicleCode: !f.vehicleCode.trim() ? 'Nhập mã phương tiện' : undefined,
      plateNumber: !f.plateNumber.trim() ? 'Nhập biển số' : undefined,
      capacityHorses: !f.capacityHorses || f.capacityHorses < 1 || f.capacityHorses > 20 ? 'Sức chứa 1–20 ngựa' : undefined,
      nextMaintenanceDate: f.lastMaintenanceDate && f.nextMaintenanceDate && f.nextMaintenanceDate < f.lastMaintenanceDate ? 'Ngày bảo dưỡng tới phải sau lần gần nhất' : undefined,
    };
    setErrors(errs);
    if (Object.values(errs).some(Boolean)) return;
    await run(() => (id ? vehicleApi.update(id, f) : vehicleApi.create(f)), id ? 'Đã cập nhật phương tiện' : 'Đã thêm phương tiện');
    onSaved(); onClose();
  };
  const e = { ...fieldErrors, ...errors };
  return (
    <Modal open onClose={onClose} onSubmit={submit} busy={busy} size="lg" icon="bi-truck-front" title={id ? 'Sửa phương tiện' : 'Thêm phương tiện'}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Lưu</Button></>}>
      <div className="ir-stack">
        <div className="app-grid app-grid-2">
          <TextField label="Mã phương tiện" value={f.vehicleCode} onChange={(v) => set('vehicleCode', v)} error={e.vehicleCode} required placeholder="HT-01" />
          <TextField label="Biển số" value={f.plateNumber} onChange={(v) => set('plateNumber', v)} error={e.plateNumber} required />
          <SelectField label="Loại" value={f.vehicleType} onChange={(v) => set('vehicleType', v as VehicleType)} options={enumOptions(VehicleTypeValues, VehicleTypeLabels)} />
          <TextField label="Model" value={f.model} onChange={(v) => set('model', v)} optional />
          <NumberField label="Sức chứa" suffix="ngựa" value={f.capacityHorses} onChange={(v) => set('capacityHorses', v ?? 0)} error={e.capacityHorses} min={1} max={20} required />
          <TextField label="Vị trí hiện tại" value={f.currentLocation} onChange={(v) => set('currentLocation', v)} optional />
          <TextField label="Bảo dưỡng gần nhất" type="date" value={f.lastMaintenanceDate} onChange={(v) => set('lastMaintenanceDate', v || undefined)} optional />
          <TextField label="Bảo dưỡng tiếp theo" type="date" value={f.nextMaintenanceDate} onChange={(v) => set('nextMaintenanceDate', v || undefined)} error={e.nextMaintenanceDate} optional />
        </div>
        <div className="app-row" style={{ gap: 24 }}>
          <Checkbox label="Có điều hòa / kiểm soát nhiệt độ" checked={!!f.climateControlled} onChange={(v) => set('climateControlled', v)} />
          <Checkbox label="Có camera giám sát" checked={!!f.cameraMonitoring} onChange={(v) => set('cameraMonitoring', v)} />
        </div>
        <TextAreaField label="Ghi chú" value={f.notes} onChange={(v) => set('notes', v)} rows={2} optional />
      </div>
    </Modal>
  );
}

function VehicleDrawer({ v, canEdit, onClose, onChanged }: { v: Vehicle; canEdit: boolean; onClose: () => void; onChanged: () => void }) {
  const { run } = useAction();
  const [status, setStatus] = useState<VehicleStatus>(v.status);
  const [note, setNote] = useState('');
  const [edit, setEdit] = useState(false);
  const [archive, setArchive] = useState(false);
  return (
    <Modal open onClose={onClose} size="lg" icon="bi-truck-front" title={`${v.vehicleCode} · ${v.plateNumber}`} description={`${VehicleTypeLabels[v.vehicleType]} – sức chứa ${v.capacityHorses} ngựa`}
      footer={canEdit ? <>
        <Button variant="danger-outline" icon="bi-archive" onClick={() => setArchive(true)} disabled={v.archived}>Lưu trữ</Button>
        <Button variant="outline" icon="bi-pencil" onClick={() => setEdit(true)}>Sửa</Button>
        <Button variant="outline" onClick={onClose}>Đóng</Button>
      </> : <Button variant="outline" onClick={onClose}>Đóng</Button>}>
      <div className="ir-stack">
        {v.maintenanceDue && <Alert tone="warning" title="Đến hạn bảo dưỡng">Bảo dưỡng tiếp theo: {formatDate(v.nextMaintenanceDate)}.</Alert>}
        <DescriptionList items={[
          { label: 'Trạng thái', value: <StatusBadge status={v.status} labels={VehicleStatusLabels} /> },
          { label: 'Vị trí', value: v.currentLocation },
          { label: 'Model', value: v.model },
          { label: 'Tiện ích', value: <span className="app-row">{v.climateControlled && <Badge tone="info" icon="bi-thermometer-snow">Điều hòa</Badge>}{v.cameraMonitoring && <Badge tone="info" icon="bi-camera-video">Camera</Badge>}</span> },
          { label: 'Bảo dưỡng gần nhất', value: formatDate(v.lastMaintenanceDate) },
          { label: 'Bảo dưỡng tiếp theo', value: formatDate(v.nextMaintenanceDate) },
          { label: 'Ghi chú', value: v.notes, span: true },
        ]} />
        {canEdit && (
          <Card title="Đổi trạng thái" icon="bi-arrow-repeat">
            <div className="app-row" style={{ alignItems: 'flex-end' }}>
              <div style={{ width: 220 }}><SelectField label="Trạng thái mới" value={status} onChange={(x) => setStatus(x as VehicleStatus)} options={enumOptions(VehicleStatusValues, VehicleStatusLabels)} /></div>
              <div style={{ flex: 1, minWidth: 200 }}><TextField label="Ghi chú" value={note} onChange={setNote} optional /></div>
              <Button disabled={status === v.status} onClick={async () => { await run(() => vehicleApi.setStatus(v.id, status, note || undefined), 'Đã đổi trạng thái phương tiện'); onChanged(); onClose(); }}>Cập nhật</Button>
            </div>
          </Card>
        )}
        <Card title="Chuyến sắp tới" icon="bi-calendar-event" flush>
          <DataTable flush dense rows={v.upcomingTrips} rowKey={(t) => t.tripId} emptyTitle="Không có chuyến nào được gán"
            columns={[
              { key: 'n', header: 'Chuyến', render: (t) => <Cell mono main={t.tripNumber} sub={t.routeName} /> },
              { key: 'd', header: 'Khởi hành', render: (t) => formatDateTime(t.plannedDeparture) },
              { key: 'a', header: 'Đến', render: (t) => formatDateTime(t.plannedArrival) },
              { key: 's', header: 'Trạng thái', render: (t) => <StatusBadge status={t.status} /> },
            ]} />
        </Card>
      </div>
      {edit && <VehicleForm id={v.id} initial={{ vehicleCode: v.vehicleCode, plateNumber: v.plateNumber, vehicleType: v.vehicleType, model: v.model, capacityHorses: v.capacityHorses, currentLocation: v.currentLocation, climateControlled: v.climateControlled, cameraMonitoring: v.cameraMonitoring, lastMaintenanceDate: v.lastMaintenanceDate, nextMaintenanceDate: v.nextMaintenanceDate, notes: v.notes }} onClose={() => setEdit(false)} onSaved={() => { onChanged(); onClose(); }} />}
      <ConfirmDialog open={archive} onClose={() => setArchive(false)} tone="error" title="Lưu trữ phương tiện?" message="Phương tiện đã lưu trữ không thể gán cho chuyến mới. Không thực hiện được nếu còn chuyến đang chạy." confirmLabel="Lưu trữ"
        onConfirm={async () => { await run(() => vehicleApi.archive(v.id), 'Đã lưu trữ phương tiện'); onChanged(); onClose(); }} />
    </Modal>
  );
}

export function FleetPage() {
  const user = useCurrentUser();
  const canEdit = user.role === 'FLEET_ROUTE_COORDINATOR';
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<VehicleStatus | ''>('');
  const [type, setType] = useState<VehicleType | ''>('');
  const dq = useDebounced(q);
  const state = useAsync(() => vehicleApi.list({ status: status ? [status] : [], type: type ? [type] : [], q: dq || undefined }), [status, type, dq]);
  const [sel, setSel] = useState<Vehicle | null>(null);
  const [creating, setCreating] = useState(false);
  const counts = (s: VehicleStatus) => state.data?.filter((v) => v.status === s).length ?? 0;

  return (
    <>
      <PageHeader title="Đội xe" subtitle="Phương tiện vận chuyển ngựa: trạng thái, sức chứa, bảo dưỡng và lịch chuyến."
        meta={state.data && <span className="app-row"><Badge tone="success">Sẵn sàng {counts('AVAILABLE')}</Badge><Badge tone="primary">Đang dùng {counts('IN_USE')}</Badge><Badge tone="warning">Bảo dưỡng {counts('MAINTENANCE')}</Badge></span>}
        actions={canEdit && <Button icon="bi-plus-lg" onClick={() => setCreating(true)}>Thêm phương tiện</Button>} />
      <DataTable<Vehicle> rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(v) => v.id} onRowClick={setSel}
        rowClassName={(v) => (v.maintenanceDue ? 'row-warning' : undefined)} emptyTitle="Không có phương tiện phù hợp" emptyIcon="bi-truck-front"
        toolbar={<>
          <SearchBox value={q} onChange={setQ} placeholder="Mã xe, biển số, model…" />
          <div style={{ width: 180 }}><SelectField size="sm" value={status} onChange={(v) => setStatus(v as VehicleStatus | '')} placeholder="Mọi trạng thái" options={enumOptions(VehicleStatusValues, VehicleStatusLabels)} /></div>
          <div style={{ width: 200 }}><SelectField size="sm" value={type} onChange={(v) => setType(v as VehicleType | '')} placeholder="Mọi loại" options={enumOptions(VehicleTypeValues, VehicleTypeLabels)} /></div>
        </>}
        columns={[
          { key: 'c', header: 'Phương tiện', sortValue: (v) => v.vehicleCode, render: (v) => <Cell main={`${v.vehicleCode} · ${v.plateNumber}`} sub={v.model} /> },
          { key: 't', header: 'Loại', render: (v) => VehicleTypeLabels[v.vehicleType] },
          { key: 'cap', header: 'Sức chứa', align: 'right', sortValue: (v) => v.capacityHorses, render: (v) => `${v.capacityHorses} ngựa` },
          { key: 'f', header: 'Tiện ích', render: (v) => <span className="app-row" style={{ gap: 6 }}>{v.climateControlled && <Icon name="bi-thermometer-snow" title="Điều hòa" />}{v.cameraMonitoring && <Icon name="bi-camera-video" title="Camera" />}</span> },
          { key: 'l', header: 'Vị trí', render: (v) => v.currentLocation ?? '—' },
          { key: 'm', header: 'Bảo dưỡng tới', sortValue: (v) => v.nextMaintenanceDate, render: (v) => <Cell main={formatDate(v.nextMaintenanceDate)} sub={v.maintenanceDue ? 'Đến hạn' : undefined} /> },
          { key: 's', header: 'Trạng thái', render: (v) => <StatusBadge status={v.status} labels={VehicleStatusLabels} /> },
        ]} />
      {sel && <VehicleDrawer v={sel} canEdit={canEdit} onClose={() => setSel(null)} onChanged={state.reload} />}
      {creating && <VehicleForm initial={blank()} onClose={() => setCreating(false)} onSaved={state.reload} />}
    </>
  );
}
