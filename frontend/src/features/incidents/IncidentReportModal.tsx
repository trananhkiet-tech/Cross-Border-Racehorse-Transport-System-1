import { useEffect, useState } from 'react';
import { attachmentApi, incidentApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { Alert, Button, FileDrop, Modal, SelectField, TextAreaField, TextField, enumOptions } from '../../components';
import {
  IncidentSeverityLabels, IncidentSeverityValues, IncidentTypeLabels, IncidentTypeValues,
  type IncidentSeverity, type IncidentType,
} from '../../types/enums';
import type { IncidentDetail, TripHorse } from '../../types/api';
import { hasErrors } from '../common/validation';

export function IncidentReportModal({ open, onClose, onDone, tripId, horses, location, onCreated }: { open: boolean; onClose: () => void; onDone: () => void; tripId: number; horses: TripHorse[]; location?: string; onCreated?: (i: IncidentDetail) => void }) {
  const [type, setType] = useState<IncidentType>('OTHER');
  const [severity, setSeverity] = useState<IncidentSeverity>('MEDIUM');
  const [horseId, setHorseId] = useState<number | undefined>();
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState('');
  const [loc, setLoc] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [fileErr, setFileErr] = useState<string>();
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  useEffect(() => {
    if (!open) return;
    setType('OTHER'); setSeverity('MEDIUM'); setHorseId(undefined); setTitle(''); setDesc(''); setLoc(location ?? ''); setFile(null); setFileErr(undefined); setErr({});
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const horseRelated = type === 'HORSE_HEALTH' || type === 'HORSE_INJURY';
  const submit = async () => {
    const e = {
      title: title.trim().length < 5 ? 'Tiêu đề tối thiểu 5 ký tự' : undefined,
      desc: desc.trim().length < 10 ? 'Mô tả tối thiểu 10 ký tự' : undefined,
      horse: horseRelated && !horseId ? 'Chọn ngựa liên quan' : undefined,
      file: fileErr,
    };
    setErr(e);
    if (hasErrors(e)) return;
    try {
      const inc = await run(() => incidentApi.report({ tripId, horseId, incidentType: type, severity, title: title.trim(), description: desc.trim(), location: loc.trim() || undefined }), 'Đã gửi báo cáo sự cố');
      if (file) await attachmentApi.upload('INCIDENT', inc.id, file, 'Ảnh hiện trường').catch(() => undefined);
      onCreated?.(inc);
      onClose(); onDone();
    } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Báo sự cố" description="Sự cố nghiêm trọng (Cao / Nghiêm trọng) sẽ tạm dừng chuyến cho đến khi Quản lý Logistics quyết định." icon="bi-exclamation-triangle" tone="error" size="lg" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" variant="danger" loading={busy}>Gửi báo cáo</Button></>}>
      <div className="ir-stack">
        <div className="app-grid app-grid-2">
          <SelectField label="Loại sự cố" required value={type} onChange={(v) => setType(v as IncidentType)} options={enumOptions(IncidentTypeValues, IncidentTypeLabels)} />
          <SelectField label="Mức độ" required value={severity} onChange={(v) => setSeverity(v as IncidentSeverity)} options={enumOptions(IncidentSeverityValues, IncidentSeverityLabels)} />
        </div>
        <SelectField label="Ngựa liên quan" required={horseRelated} optional={!horseRelated} value={horseId} onChange={(v) => setHorseId(v ? Number(v) : undefined)} placeholder="Không liên quan đến ngựa cụ thể" error={err.horse}
          options={horses.map((h) => ({ value: String(h.horseId), label: `${h.name} · ${h.microchipNumber}` }))} />
        <TextField label="Tiêu đề" required value={title} onChange={setTitle} error={err.title} maxLength={200} />
        <TextAreaField label="Mô tả" required rows={4} value={desc} onChange={setDesc} error={err.desc} maxLength={2000} />
        <TextField label="Vị trí" optional value={loc} onChange={setLoc} maxLength={200} />
        {horseRelated && <Alert tone="warning">Hệ thống không chẩn đoán. Hãy mô tả quan sát và liên hệ bác sĩ thú y bên ngoài nếu cần; Quản lý sẽ ghi nhận tư vấn.</Alert>}
        <FileDrop compact file={file} error={fileErr} title="Ảnh / tài liệu hiện trường (không bắt buộc)" onFile={(f, er) => { setFile(f); setFileErr(er); }} onClear={() => setFile(null)} />
      </div>
    </Modal>
  );
}
