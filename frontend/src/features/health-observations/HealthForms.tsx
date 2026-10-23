import { useEffect, useState } from 'react';
import { attachmentApi, healthApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { Alert, Button, Checkbox, FileDrop, Modal, NumberField, SelectField, TextAreaField, TextField, enumOptions } from '../../components';
import {
  HorseBehaviorLabels, HorseBehaviorValues, HorseConditionLabels, HorseConditionValues, IntakeLevelLabels, IntakeLevelValues,
  MovementConditionLabels, MovementConditionValues, ObservationSeverityLabels, ObservationSeverityValues,
  OperationalDecisionLabels, OperationalDecisionValues, StressLevelLabels, StressLevelValues,
  type HorseBehavior, type HorseCondition, type IntakeLevel, type MovementCondition, type ObservationSeverity,
  type OperationalDecision, type StressLevel,
} from '../../types/enums';
import type { TripHorse } from '../../types/api';
import { toLocalInput } from '../../utils/format';
import { hasErrors } from '../common/validation';

interface BaseProps { open: boolean; onClose: () => void; onDone: () => void }

function horseOptions(horses: TripHorse[]) {
  return horses.map((h) => ({ value: String(h.horseId), label: `${h.name} · ${h.microchipNumber}` }));
}

function tempError(t?: number): string | undefined {
  if (t === undefined) return undefined;
  return t < 30 || t > 45 ? 'Nhiệt độ hợp lệ từ 30 đến 45 °C' : undefined;
}

/** Kiểm tra trước vận chuyển – Lái xe/Hộ tống ghi nhận quan sát, không phải chẩn đoán thú y. */
export function PreCheckModal({ open, onClose, onDone, tripId, horses, horseId: initial }: BaseProps & { tripId: number; horses: TripHorse[]; horseId?: number }) {
  const [horseId, setHorseId] = useState<number | undefined>(initial);
  const [temp, setTemp] = useState<number | undefined>();
  const [cond, setCond] = useState<HorseCondition>('GOOD');
  const [identity, setIdentity] = useState(false);
  const [docs, setDocs] = useState(false);
  const [loading, setLoading] = useState(false);
  const [fit, setFit] = useState(false);
  const [notes, setNotes] = useState('');
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  useEffect(() => { if (open) { setHorseId(initial ?? (horses.length === 1 ? horses[0].horseId : undefined)); setTemp(undefined); setCond('GOOD'); setIdentity(false); setDocs(false); setLoading(false); setFit(false); setNotes(''); setErr({}); } }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const submit = async () => {
    const e = { horse: horseId ? undefined : 'Chọn ngựa', temp: tempError(temp), notes: cond === 'INJURED' && !notes.trim() ? 'Mô tả tình trạng chấn thương' : undefined };
    setErr(e);
    if (hasErrors(e)) return;
    try {
      await run(() => healthApi.preCheck({ tripId, horseId: horseId!, temperatureC: temp, generalCondition: cond, identityVerified: identity, documentsPresent: docs, loadingOk: loading, fitForTravelObserved: fit, notes: notes.trim() || undefined }), 'Đã lưu kiểm tra trước vận chuyển');
      onClose(); onDone();
    } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Kiểm tra trước vận chuyển" description="Ghi nhận quan sát thực tế trước khi xuất phát." icon="bi-clipboard2-check" size="lg" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Lưu kiểm tra</Button></>}>
      <div className="ir-stack">
        <Alert tone="info">Đây là quan sát của nhân viên vận chuyển, không thay thế khám hoặc chứng nhận của bác sĩ thú y.</Alert>
        <SelectField label="Ngựa" required value={horseId} onChange={(v) => setHorseId(v ? Number(v) : undefined)} options={horseOptions(horses)} placeholder="Chọn ngựa" error={err.horse} />
        <div className="app-grid app-grid-2">
          <NumberField label="Nhiệt độ (°C)" optional step="0.1" value={temp} onChange={setTemp} error={err.temp} />
          <SelectField label="Tình trạng chung" required value={cond} onChange={(v) => setCond(v as HorseCondition)} options={enumOptions(HorseConditionValues, HorseConditionLabels)} />
        </div>
        <Checkbox label="Đã đối chiếu microchip / nhận dạng" checked={identity} onChange={setIdentity} />
        <Checkbox label="Đủ giấy tờ đi kèm ngựa" checked={docs} onChange={setDocs} />
        <Checkbox label="Việc lên xe diễn ra an toàn" checked={loading} onChange={setLoading} />
        <Checkbox label="Quan sát thấy ngựa phù hợp để vận chuyển" description="Cần cho tất cả ngựa trước khi bắt đầu chuyến đi." checked={fit} onChange={setFit} />
        <TextAreaField label="Ghi chú" optional rows={3} value={notes} onChange={setNotes} maxLength={1000} error={err.notes} />
      </div>
    </Modal>
  );
}

/** Quan sát sức khỏe trong chuyến. Mức ATTENTION/URGENT có thể tạo sự cố. */
export function ObservationModal({ open, onClose, onDone, tripId, horses, location, onCreated }: BaseProps & { tripId: number; horses: TripHorse[]; location?: string; onCreated?: (obsId: number) => void }) {
  const [horseId, setHorseId] = useState<number | undefined>();
  const [loc, setLoc] = useState('');
  const [appetite, setAppetite] = useState<IntakeLevel>('NORMAL');
  const [water, setWater] = useState<IntakeLevel>('NORMAL');
  const [behavior, setBehavior] = useState<HorseBehavior>('CALM');
  const [stress, setStress] = useState<StressLevel>('LOW');
  const [movement, setMovement] = useState<MovementCondition>('NORMAL');
  const [injury, setInjury] = useState(false);
  const [injuryText, setInjuryText] = useState('');
  const [temp, setTemp] = useState<number | undefined>();
  const [severity, setSeverity] = useState<ObservationSeverity>('NORMAL');
  const [notes, setNotes] = useState('');
  const [report, setReport] = useState(false);
  const [photo, setPhoto] = useState<File | null>(null);
  const [photoErr, setPhotoErr] = useState<string>();
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  useEffect(() => {
    if (!open) return;
    setHorseId(horses.length === 1 ? horses[0].horseId : undefined); setLoc(location ?? ''); setAppetite('NORMAL'); setWater('NORMAL'); setBehavior('CALM'); setStress('LOW');
    setMovement('NORMAL'); setInjury(false); setInjuryText(''); setTemp(undefined); setSeverity('NORMAL'); setNotes(''); setReport(false); setPhoto(null); setPhotoErr(undefined); setErr({});
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const submit = async () => {
    const e = {
      horse: horseId ? undefined : 'Chọn ngựa', temp: tempError(temp),
      injury: injury && !injuryText.trim() ? 'Mô tả vết thương' : undefined,
      notes: severity !== 'NORMAL' && !notes.trim() ? 'Cần ghi chú khi mức độ không bình thường' : undefined,
      photo: photoErr,
    };
    setErr(e);
    if (hasErrors(e)) return;
    try {
      const obs = await run(() => healthApi.observe({ tripId, horseId: horseId!, location: loc.trim() || undefined, appetite, waterIntake: water, behavior, stressLevel: stress, movement, visibleInjury: injury, injuryDescription: injury ? injuryText.trim() : undefined, temperatureC: temp, severity, notes: notes.trim() || undefined, reportIncident: report }), 'Đã ghi nhận quan sát sức khỏe');
      if (photo) {
        await attachmentApi.upload('HEALTH_OBSERVATION', obs.id, photo, 'Ảnh quan sát').catch(() => undefined);
      }
      onCreated?.(obs.id);
      onClose(); onDone();
    } catch { /* toast */ }
  };
  const intake = enumOptions(IntakeLevelValues, IntakeLevelLabels);
  return (
    <Modal open={open} title="Ghi nhận quan sát sức khỏe" description="Ghi nhận định kỳ trong quá trình vận chuyển." icon="bi-heart-pulse" size="lg" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Lưu quan sát</Button></>}>
      <div className="ir-stack">
        <div className="app-grid app-grid-2">
          <SelectField label="Ngựa" required value={horseId} onChange={(v) => setHorseId(v ? Number(v) : undefined)} options={horseOptions(horses)} placeholder="Chọn ngựa" error={err.horse} />
          <TextField label="Vị trí" optional value={loc} onChange={setLoc} maxLength={200} />
          <SelectField label="Ăn uống" value={appetite} onChange={(v) => setAppetite(v as IntakeLevel)} options={intake} />
          <SelectField label="Uống nước" value={water} onChange={(v) => setWater(v as IntakeLevel)} options={intake} />
          <SelectField label="Hành vi" value={behavior} onChange={(v) => setBehavior(v as HorseBehavior)} options={enumOptions(HorseBehaviorValues, HorseBehaviorLabels)} />
          <SelectField label="Mức căng thẳng" value={stress} onChange={(v) => setStress(v as StressLevel)} options={enumOptions(StressLevelValues, StressLevelLabels)} />
          <SelectField label="Vận động" value={movement} onChange={(v) => setMovement(v as MovementCondition)} options={enumOptions(MovementConditionValues, MovementConditionLabels)} />
          <NumberField label="Nhiệt độ (°C)" optional step="0.1" value={temp} onChange={setTemp} error={err.temp} />
        </div>
        <Checkbox label="Thấy vết thương / chấn thương" checked={injury} onChange={setInjury} />
        {injury && <TextAreaField label="Mô tả vết thương" required rows={2} value={injuryText} onChange={setInjuryText} maxLength={1000} error={err.injury} />}
        <SelectField label="Mức độ đánh giá" value={severity} onChange={(v) => setSeverity(v as ObservationSeverity)} options={enumOptions(ObservationSeverityValues, ObservationSeverityLabels)} />
        <TextAreaField label="Ghi chú" optional={severity === 'NORMAL'} required={severity !== 'NORMAL'} rows={3} value={notes} onChange={setNotes} maxLength={1000} error={err.notes} />
        {severity !== 'NORMAL' && <Checkbox label="Đồng thời báo sự cố cho điều phối / quản lý" description="Chuyến có thể bị tạm dừng đến khi Quản lý quyết định." checked={report} onChange={setReport} />}
        <FileDrop compact file={photo} error={photoErr} title="Ảnh đính kèm (không bắt buộc)" onFile={(f, er) => { setPhoto(f); setPhotoErr(er); }} onClear={() => setPhoto(null)} />
      </div>
    </Modal>
  );
}

export function ArrivalCheckModal({ open, onClose, onDone, tripId, horses }: BaseProps & { tripId: number; horses: TripHorse[] }) {
  const [horseId, setHorseId] = useState<number | undefined>();
  const [cond, setCond] = useState<HorseCondition>('GOOD');
  const [injury, setInjury] = useState(false);
  const [notes, setNotes] = useState('');
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  const pending = horses.filter((h) => !h.arrivalCheckDone);
  useEffect(() => { if (open) { setHorseId(pending.length === 1 ? pending[0].horseId : undefined); setCond('GOOD'); setInjury(false); setNotes(''); setErr({}); } }, [open]); // eslint-disable-line react-hooks/exhaustive-deps
  const submit = async () => {
    const e = { horse: horseId ? undefined : 'Chọn ngựa', notes: (injury || cond === 'INJURED' || cond === 'POOR') && !notes.trim() ? 'Cần ghi chú khi tình trạng không tốt' : undefined };
    setErr(e);
    if (hasErrors(e)) return;
    try {
      await run(() => healthApi.arrival({ tripId, horseId: horseId!, condition: cond, visibleInjury: injury, notes: notes.trim() || undefined }), 'Đã lưu kiểm tra khi đến nơi');
      onClose(); onDone();
    } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Kiểm tra khi đến nơi" icon="bi-flag" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Lưu</Button></>}>
      <div className="ir-stack">
        <SelectField label="Ngựa" required value={horseId} onChange={(v) => setHorseId(v ? Number(v) : undefined)} options={horseOptions(pending)} placeholder={pending.length ? 'Chọn ngựa' : 'Tất cả ngựa đã được kiểm tra'} error={err.horse} />
        <SelectField label="Tình trạng" required value={cond} onChange={(v) => setCond(v as HorseCondition)} options={enumOptions(HorseConditionValues, HorseConditionLabels)} />
        <Checkbox label="Thấy vết thương / chấn thương" checked={injury} onChange={setInjury} />
        <TextAreaField label="Ghi chú" optional rows={3} value={notes} onChange={setNotes} maxLength={1000} error={err.notes} />
      </div>
    </Modal>
  );
}

/** Ghi nhận tư vấn từ bác sĩ thú y bên ngoài (hệ thống không có tài khoản thú y). */
export function VetConsultationModal({ open, onClose, onDone, incidentId, tripId, horseId, onSaved }: BaseProps & { incidentId?: number; tripId?: number; horseId?: number; onSaved?: (id: number) => void }) {
  const [name, setName] = useState('');
  const [clinic, setClinic] = useState('');
  const [contact, setContact] = useState('');
  const [at, setAt] = useState('');
  const [advice, setAdvice] = useState('');
  const [rec, setRec] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [fileErr, setFileErr] = useState<string>();
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  useEffect(() => { if (open) { setName(''); setClinic(''); setContact(''); setAt(toLocalInput(new Date().toISOString())); setAdvice(''); setRec(''); setFile(null); setFileErr(undefined); setErr({}); } }, [open]);
  const submit = async () => {
    const e = { name: name.trim() ? undefined : 'Nhập tên bác sĩ thú y', advice: advice.trim().length >= 3 ? undefined : 'Nhập nội dung tư vấn', file: fileErr };
    setErr(e);
    if (hasErrors(e)) return;
    try {
      const c = await run(() => healthApi.consult({ incidentId, tripId, horseId, veterinarianName: name.trim(), clinic: clinic.trim() || undefined, contact: contact.trim() || undefined, consultedAt: at || undefined, advice: advice.trim(), recommendedAction: (rec || undefined) as OperationalDecision | undefined }), 'Đã ghi nhận tư vấn thú y');
      if (file) await run(() => healthApi.uploadReport(c.id, file), 'Đã tải báo cáo thú y');
      onSaved?.(c.id);
      onClose(); onDone();
    } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Ghi nhận tư vấn thú y" description="Nhập thông tin bác sĩ thú y bên ngoài đã tư vấn qua điện thoại hoặc trực tiếp." icon="bi-chat-left-text" size="lg" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Lưu tư vấn</Button></>}>
      <div className="ir-stack">
        <div className="app-grid app-grid-2">
          <TextField label="Bác sĩ thú y" required value={name} onChange={setName} error={err.name} maxLength={150} />
          <TextField label="Phòng khám / đơn vị" optional value={clinic} onChange={setClinic} maxLength={150} />
          <TextField label="Liên hệ" optional value={contact} onChange={setContact} maxLength={100} />
          <TextField label="Thời điểm tư vấn" optional type="datetime-local" value={at} onChange={setAt} />
        </div>
        <TextAreaField label="Nội dung tư vấn" required rows={4} value={advice} onChange={setAdvice} maxLength={2000} error={err.advice} />
        <SelectField label="Hướng xử lý được khuyến nghị" optional value={rec} onChange={setRec} placeholder="Không nêu" options={enumOptions(OperationalDecisionValues, OperationalDecisionLabels)} />
        <FileDrop compact file={file} error={fileErr} title="Báo cáo của bác sĩ thú y (không bắt buộc)" onFile={(f, er) => { setFile(f); setFileErr(er); }} onClear={() => setFile(null)} />
      </div>
    </Modal>
  );
}
