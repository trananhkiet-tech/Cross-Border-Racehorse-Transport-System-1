import { useEffect, useState } from 'react';
import { handoverApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { Alert, Button, Checkbox, Modal, SelectField, TextAreaField, TextField, enumOptions } from '../../components';
import { HorseConditionLabels, HorseConditionValues, type HorseCondition } from '../../types/enums';
import type { Handover, TripDetail } from '../../types/api';
import { hasErrors, PHONE_RE } from '../common/validation';

/** Lập hoặc sửa biên bản bàn giao (Driver/Escort). Mỗi ngựa một biên bản; cần kiểm tra khi đến trước. */
export function HandoverFormModal({ open, trip, handover, onClose, onDone }: { open: boolean; trip: Pick<TripDetail, 'id' | 'horses' | 'destinationAddress'>; handover?: Handover; onClose: () => void; onDone: () => void }) {
  const [horseId, setHorseId] = useState<number | undefined>();
  const [receiver, setReceiver] = useState('');
  const [contact, setContact] = useState('');
  const [dest, setDest] = useState('');
  const [cond, setCond] = useState<HorseCondition>('GOOD');
  const [docs, setDocs] = useState(true);
  const [notes, setNotes] = useState('');
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  const editing = !!handover;
  const candidates = editing ? trip.horses : trip.horses.filter((h) => h.arrivalCheckDone && !h.handoverId);

  useEffect(() => {
    if (!open) return;
    if (handover) {
      setHorseId(handover.horseId); setReceiver(handover.receiverName); setContact(handover.receiverContact ?? ''); setDest(handover.destination);
      setCond(handover.condition); setDocs(handover.documentsHandedOver); setNotes(handover.notes ?? '');
    } else {
      setHorseId(candidates.length === 1 ? candidates[0].horseId : undefined); setReceiver(''); setContact(''); setDest(trip.destinationAddress);
      setCond('GOOD'); setDocs(true); setNotes('');
    }
    setErr({});
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const submit = async () => {
    const e = {
      horse: horseId ? undefined : 'Chọn ngựa',
      receiver: receiver.trim() ? undefined : 'Nhập tên người nhận',
      contact: PHONE_RE.test(contact) ? undefined : 'Số liên hệ không hợp lệ',
      notes: (cond === 'POOR' || cond === 'INJURED') && !notes.trim() ? 'Cần ghi chú khi tình trạng không tốt' : undefined,
    };
    setErr(e);
    if (hasErrors(e)) return;
    const payload = { tripId: trip.id, horseId: horseId!, destination: dest.trim() || undefined, receiverName: receiver.trim(), receiverContact: contact.trim() || undefined, condition: cond, documentsHandedOver: docs, notes: notes.trim() || undefined };
    try {
      await run(() => (handover ? handoverApi.update(handover.id, payload) : handoverApi.create(payload)), editing ? 'Đã cập nhật biên bản' : 'Đã lập biên bản – chờ khách hàng xác nhận');
      onClose(); onDone();
    } catch { /* toast */ }
  };
  return (
    <Modal open={open} title={editing ? `Sửa biên bản ${handover!.handoverNumber}` : 'Lập biên bản bàn giao'} icon="bi-box-arrow-in-down" size="lg" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>{editing ? 'Lưu thay đổi' : 'Lập biên bản'}</Button></>}>
      <div className="ir-stack">
        {!editing && candidates.length === 0 && <Alert tone="warning">Chưa có ngựa nào sẵn sàng bàn giao. Hãy kiểm tra tình trạng khi đến nơi trước.</Alert>}
        <SelectField label="Ngựa" required disabled={editing} value={horseId} onChange={(v) => setHorseId(v ? Number(v) : undefined)} placeholder="Chọn ngựa" error={err.horse}
          options={candidates.map((h) => ({ value: String(h.horseId), label: `${h.name} · ${h.microchipNumber}` }))} />
        <div className="app-grid app-grid-2">
          <TextField label="Người nhận" required value={receiver} onChange={setReceiver} error={err.receiver} maxLength={150} />
          <TextField label="Liên hệ người nhận" optional value={contact} onChange={setContact} error={err.contact} maxLength={150} />
        </div>
        <TextField label="Địa điểm bàn giao" optional value={dest} onChange={setDest} maxLength={255} />
        <SelectField label="Tình trạng ngựa khi bàn giao" required value={cond} onChange={(v) => setCond(v as HorseCondition)} options={enumOptions(HorseConditionValues, HorseConditionLabels)} />
        <Checkbox label="Đã bàn giao đầy đủ giấy tờ đi kèm" checked={docs} onChange={setDocs} />
        <TextAreaField label="Ghi chú" optional={!(cond === 'POOR' || cond === 'INJURED')} required={cond === 'POOR' || cond === 'INJURED'} rows={3} value={notes} onChange={setNotes} maxLength={2000} error={err.notes} />
        <Alert tone="info">Sau khi lập, khách hàng sẽ nhận thông báo để xác nhận hoặc phản hồi. Có thể đính kèm ảnh bằng chứng ở trang chi tiết biên bản.</Alert>
      </div>
    </Modal>
  );
}
