import { useState } from 'react';
import { employeeApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Button, Modal, SelectField, TextField } from '../../components';
import { isoDatePlus } from '../../utils/format';
import { leaveApi } from './api';

/** Coordinator/Manager nhập lịch nghỉ cho tài xế / áp tải. */
export function LeaveFormModal({ open, onClose, onDone }: { open: boolean; onClose: () => void; onDone: () => void }) {
  const staff = useAsync(() => employeeApi.list('DRIVER_ESCORT'), [], open);
  const [staffId, setStaffId] = useState('');
  const [start, setStart] = useState(isoDatePlus(1));
  const [end, setEnd] = useState(isoDatePlus(1));
  const [reason, setReason] = useState('');
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();

  const submit = async () => {
    const e = {
      staff: staffId ? undefined : 'Chọn nhân sự',
      end: end >= start ? undefined : 'Ngày kết thúc phải sau ngày bắt đầu',
    };
    setErr(e);
    if (e.staff || e.end) return;
    try {
      await run(() => leaveApi.create({ staffId: Number(staffId), startDate: start, endDate: end, reason: reason.trim() || undefined }), 'Đã ghi nhận lịch nghỉ');
      setReason(''); onClose(); onDone();
    } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Thêm lịch nghỉ phép" icon="bi-calendar-x" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Lưu</Button></>}>
      <div className="ir-stack">
        <SelectField label="Tài xế / áp tải" required value={staffId} onChange={setStaffId} placeholder="Chọn nhân sự" error={err.staff}
          options={(staff.data ?? []).map((s) => ({ value: String(s.userId), label: s.fullName }))} />
        <div className="app-grid app-grid-2">
          <TextField label="Từ ngày" type="date" required value={start} onChange={setStart} />
          <TextField label="Đến ngày" type="date" required value={end} onChange={setEnd} error={err.end} />
        </div>
        <TextField label="Lý do" optional value={reason} onChange={setReason} maxLength={300} />
      </div>
    </Modal>
  );
}
