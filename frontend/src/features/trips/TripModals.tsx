import { useEffect, useState } from 'react';
import { employeeApi, tripApi, vehicleApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Alert, Button, Modal, NumberField, SelectField, TextAreaField, TextField, enumOptions } from '../../components';
import {
  StaffRoleLabels, StaffRoleValues, TrackingEventTypeLabels, TripStatusLabels, VehicleTypeLabels,
  type StaffRole, type TrackingEventType, type TripStatus,
} from '../../types/enums';
import type { TripDetail } from '../../types/api';
import { toLocalInput } from '../../utils/format';
import { hasErrors } from '../common/validation';

interface P { open: boolean; trip: TripDetail; onClose: () => void; onDone: () => void }

function Footer({ busy, onClose, label }: { busy: boolean; onClose: () => void; label: string }) {
  return (
    <>
      <Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button>
      <Button type="submit" loading={busy}>{label}</Button>
    </>
  );
}

export function AssignVehicleModal({ open, trip, onClose, onDone }: P) {
  const [vehicleId, setVehicleId] = useState<number | undefined>();
  const [err, setErr] = useState<string>();
  const { run, busy } = useAction();
  const need = trip.horses.length;
  const state = useAsync(() => vehicleApi.available(trip.plannedDeparture, trip.plannedArrival, need, trip.id), [trip.id, open], open);
  useEffect(() => { if (open) { setVehicleId(trip.vehicle?.id); setErr(undefined); } }, [open]); // eslint-disable-line react-hooks/exhaustive-deps
  const submit = async () => {
    if (!vehicleId) { setErr('Chọn phương tiện'); return; }
    try { await run(() => tripApi.assignVehicle(trip.id, vehicleId), 'Đã gán phương tiện'); onClose(); onDone(); } catch { /* toast */ }
  };
  const list = state.data ?? [];
  return (
    <Modal open={open} title="Gán phương tiện" description={`Chỉ hiển thị xe sẵn sàng, đủ sức chứa ${need} ngựa và không trùng lịch.`} icon="bi-truck" onClose={onClose} onSubmit={submit} busy={busy} footer={<Footer busy={busy} onClose={onClose} label="Gán xe" />}>
      <div className="ir-stack">
        {state.error && <Alert tone="error">{state.error}</Alert>}
        {!state.loading && !state.error && list.length === 0 && <Alert tone="warning">Không có xe phù hợp trong khung giờ này. Hãy đổi thời gian hoặc cập nhật đội xe.</Alert>}
        <SelectField label="Phương tiện" required value={vehicleId} onChange={(v) => setVehicleId(v ? Number(v) : undefined)} placeholder={state.loading ? 'Đang tải…' : 'Chọn phương tiện'} error={err}
          options={list.map((v) => ({ value: String(v.id), label: `${v.vehicleCode} · ${v.plateNumber} · ${VehicleTypeLabels[v.vehicleType]} · ${v.capacityHorses} ngựa` }))} />
      </div>
    </Modal>
  );
}

export function AssignStaffModal({ open, trip, onClose, onDone }: P) {
  const [staffId, setStaffId] = useState<number | undefined>();
  const [role, setRole] = useState<StaffRole>('DRIVER');
  const [err, setErr] = useState<string>();
  const { run, busy } = useAction();
  const staff = useAsync(() => employeeApi.list('DRIVER_ESCORT'), [open], open);
  useEffect(() => { if (open) { setStaffId(undefined); setRole('DRIVER'); setErr(undefined); } }, [open]);
  const assigned = new Set(trip.staff.map((s) => s.staffId));
  const submit = async () => {
    if (!staffId) { setErr('Chọn nhân sự'); return; }
    try { await run(() => tripApi.assignStaff(trip.id, staffId, role), 'Đã phân công nhân sự'); onClose(); onDone(); } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Phân công nhân sự" icon="bi-person-plus" onClose={onClose} onSubmit={submit} busy={busy} footer={<Footer busy={busy} onClose={onClose} label="Phân công" />}>
      <div className="ir-stack">
        {staff.error && <Alert tone="error">{staff.error}</Alert>}
        <SelectField label="Nhân sự" required value={staffId} onChange={(v) => setStaffId(v ? Number(v) : undefined)} placeholder="Chọn lái xe / hộ tống" error={err}
          options={(staff.data ?? []).filter((e) => e.enabled && !assigned.has(e.userId)).map((e) => ({ value: String(e.userId), label: `${e.fullName}${e.staffRole ? ` · ${StaffRoleLabels[e.staffRole]}` : ''} · ${e.activeAssignments} chuyến đang phụ trách` }))} />
        <SelectField label="Vai trò trong chuyến" required value={role} onChange={(v) => setRole(v as StaffRole)} options={enumOptions(StaffRoleValues, StaffRoleLabels)} />
      </div>
    </Modal>
  );
}

export function EditPlanModal({ open, trip, onClose, onDone }: P) {
  const [dep, setDep] = useState('');
  const [arr, setArr] = useState('');
  const [ins, setIns] = useState('');
  const [notes, setNotes] = useState('');
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  useEffect(() => { if (open) { setDep(toLocalInput(trip.plannedDeparture)); setArr(toLocalInput(trip.plannedArrival)); setIns(trip.transportInstructions ?? ''); setNotes(trip.notes ?? ''); setErr({}); } }, [open]); // eslint-disable-line react-hooks/exhaustive-deps
  const submit = async () => {
    const e = { dep: dep ? undefined : 'Chọn giờ khởi hành', arr: !arr ? 'Chọn giờ đến' : arr <= dep ? 'Giờ đến phải sau giờ khởi hành' : undefined };
    setErr(e);
    if (hasErrors(e)) return;
    try { await run(() => tripApi.updatePlan(trip.id, { plannedDeparture: dep, plannedArrival: arr, transportInstructions: ins.trim() || undefined, notes: notes.trim() || undefined }), 'Đã cập nhật kế hoạch'); onClose(); onDone(); } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Chỉnh sửa kế hoạch chuyến" description="Đổi giờ có thể làm xe/nhân sự đã gán bị trùng lịch; hệ thống sẽ kiểm tra lại." icon="bi-calendar2-week" onClose={onClose} onSubmit={submit} busy={busy} footer={<Footer busy={busy} onClose={onClose} label="Lưu kế hoạch" />}>
      <div className="ir-stack">
        <div className="app-grid app-grid-2">
          <TextField label="Khởi hành" required type="datetime-local" value={dep} onChange={setDep} error={err.dep} />
          <TextField label="Dự kiến đến" required type="datetime-local" value={arr} onChange={setArr} error={err.arr} />
        </div>
        <TextAreaField label="Hướng dẫn vận chuyển" optional rows={3} value={ins} onChange={setIns} maxLength={2000} />
        <TextAreaField label="Ghi chú" optional rows={2} value={notes} onChange={setNotes} maxLength={1000} />
      </div>
    </Modal>
  );
}

/** Ghi checkpoint / cập nhật vị trí / báo trễ trong chuyến. */
export function CheckpointModal({ open, trip, onClose, onDone }: P) {
  const [type, setType] = useState<TrackingEventType>('CHECKPOINT_ARRIVED');
  const [stopId, setStopId] = useState<number | undefined>();
  const [loc, setLoc] = useState('');
  const [delay, setDelay] = useState<number | undefined>();
  const [note, setNote] = useState('');
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  useEffect(() => { if (open) { setType('CHECKPOINT_ARRIVED'); setStopId(undefined); setLoc(trip.currentLocation ?? ''); setDelay(undefined); setNote(''); setErr({}); } }, [open]); // eslint-disable-line react-hooks/exhaustive-deps
  const needsStop = type === 'CHECKPOINT_ARRIVED' || type === 'CHECKPOINT_DEPARTED';
  const submit = async () => {
    const e = {
      stop: needsStop && !stopId ? 'Chọn điểm dừng' : undefined,
      delay: type === 'DELAY' && (delay === undefined || delay <= 0) ? 'Nhập số phút trễ (> 0)' : delay !== undefined && delay < 0 ? 'Số phút không âm' : undefined,
      loc: !needsStop && type !== 'NOTE' && !loc.trim() ? 'Nhập vị trí' : undefined,
    };
    setErr(e);
    if (hasErrors(e)) return;
    try {
      await run(() => tripApi.checkpoint(trip.id, { eventType: type, routeStopId: stopId, location: loc.trim() || undefined, delayMinutes: delay, note: note.trim() || undefined }), 'Đã ghi nhận sự kiện hành trình');
      onClose(); onDone();
    } catch { /* toast */ }
  };
  const types = (['CHECKPOINT_ARRIVED', 'CHECKPOINT_DEPARTED', 'LOCATION_UPDATE', 'DELAY', 'NOTE'] as TrackingEventType[]);
  return (
    <Modal open={open} title="Cập nhật hành trình" description="Ghi nhận vị trí, điểm dừng hoặc thời gian trễ. Vị trí được nhập thủ công (không dùng GPS)." icon="bi-geo-alt" onClose={onClose} onSubmit={submit} busy={busy} footer={<Footer busy={busy} onClose={onClose} label="Ghi nhận" />}>
      <div className="ir-stack">
        <SelectField label="Loại sự kiện" required value={type} onChange={(v) => setType(v as TrackingEventType)} options={types.map((t) => ({ value: t, label: TrackingEventTypeLabels[t] }))} />
        {needsStop && <SelectField label="Điểm dừng" required value={stopId} onChange={(v) => setStopId(v ? Number(v) : undefined)} placeholder="Chọn điểm dừng" error={err.stop} options={trip.stops.map((s) => ({ value: String(s.id), label: `${s.stopOrder}. ${s.name}` }))} />}
        <TextField label="Vị trí hiện tại" optional={needsStop || type === 'NOTE'} required={!needsStop && type !== 'NOTE'} value={loc} onChange={setLoc} maxLength={200} error={err.loc} />
        <NumberField label="Trễ (phút)" required={type === 'DELAY'} optional={type !== 'DELAY'} min={0} value={delay} onChange={setDelay} error={err.delay} />
        <TextAreaField label="Ghi chú" optional rows={2} value={note} onChange={setNote} maxLength={1000} />
      </div>
    </Modal>
  );
}

export function StatusModal({ open, trip, onClose, onDone }: P) {
  const [status, setStatus] = useState<TripStatus | ''>('');
  const [note, setNote] = useState('');
  const [loc, setLoc] = useState('');
  const [delay, setDelay] = useState<number | undefined>();
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  useEffect(() => { if (open) { setStatus(''); setNote(''); setLoc(trip.currentLocation ?? ''); setDelay(undefined); setErr({}); } }, [open]); // eslint-disable-line react-hooks/exhaustive-deps
  const submit = async () => {
    const e = { status: status ? undefined : 'Chọn trạng thái mới', delay: status === 'DELAYED' && (delay === undefined || delay <= 0) ? 'Nhập số phút trễ (> 0)' : undefined };
    setErr(e);
    if (hasErrors(e)) return;
    try {
      await run(() => tripApi.updateStatus(trip.id, { status: status as TripStatus, note: note.trim() || undefined, location: loc.trim() || undefined, delayMinutes: delay }), 'Đã cập nhật trạng thái chuyến');
      onClose(); onDone();
    } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Cập nhật trạng thái chuyến" description="Chỉ hiển thị các trạng thái hợp lệ kế tiếp." icon="bi-arrow-repeat" onClose={onClose} onSubmit={submit} busy={busy} footer={<Footer busy={busy} onClose={onClose} label="Cập nhật" />}>
      <div className="ir-stack">
        {trip.nextStatuses.length === 0 && <Alert tone="warning">Không có trạng thái kế tiếp hợp lệ.</Alert>}
        <SelectField label="Trạng thái mới" required value={status} onChange={(v) => setStatus(v as TripStatus)} placeholder="Chọn trạng thái" error={err.status} options={trip.nextStatuses.map((s) => ({ value: s, label: TripStatusLabels[s] }))} />
        <TextField label="Vị trí" optional value={loc} onChange={setLoc} maxLength={200} />
        {status === 'DELAYED' && <NumberField label="Trễ (phút)" required min={1} value={delay} onChange={setDelay} error={err.delay} />}
        <TextAreaField label="Ghi chú" optional rows={2} value={note} onChange={setNote} maxLength={1000} />
      </div>
    </Modal>
  );
}
