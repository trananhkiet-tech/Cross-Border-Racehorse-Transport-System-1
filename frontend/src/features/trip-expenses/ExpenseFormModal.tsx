import { useState } from 'react';
import { useAction } from '../../hooks/useAction';
import { Button, Modal, NumberField, SelectField, TextField, enumOptions } from '../../components';
import { expenseApi, ExpenseCategoryLabels, type ExpenseCategory } from './api';

/** Tài xế / áp tải ghi chi phí phát sinh trên chuyến được phân công. */
export function ExpenseFormModal({ open, onClose, onDone }: { open: boolean; onClose: () => void; onDone: () => void }) {
  const [tripId, setTripId] = useState<number | undefined>();
  const [category, setCategory] = useState<ExpenseCategory>('FUEL');
  const [amount, setAmount] = useState<number | undefined>();
  const [desc, setDesc] = useState('');
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();

  const submit = async () => {
    const e = {
      trip: tripId ? undefined : 'Nhập ID chuyến',
      amount: amount && amount > 0 ? undefined : 'Số tiền phải lớn hơn 0',
    };
    setErr(e);
    if (e.trip || e.amount) return;
    try {
      await run(() => expenseApi.create(tripId!, { category, amount: amount!, description: desc.trim() || undefined }), 'Đã gửi chi phí, chờ duyệt');
      setAmount(undefined); setDesc(''); onClose(); onDone();
    } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Ghi chi phí phát sinh" icon="bi-cash-coin" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Gửi duyệt</Button></>}>
      <div className="ir-stack">
        <NumberField label="ID chuyến" required value={tripId} onChange={setTripId} error={err.trip} help="Số ID trong đường dẫn /trips/ID" />
        <SelectField label="Loại chi phí" required value={category} onChange={(v) => setCategory(v as ExpenseCategory)} options={enumOptions(Object.keys(ExpenseCategoryLabels) as ExpenseCategory[], ExpenseCategoryLabels)} />
        <NumberField label="Số tiền (USD)" required value={amount} onChange={setAmount} error={err.amount} />
        <TextField label="Ghi chú" optional value={desc} onChange={setDesc} maxLength={500} />
      </div>
    </Modal>
  );
}
