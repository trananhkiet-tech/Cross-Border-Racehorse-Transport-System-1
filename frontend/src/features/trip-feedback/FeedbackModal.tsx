import { useEffect, useState } from 'react';
import { useAction } from '../../hooks/useAction';
import { Button, Modal, TextAreaField } from '../../components';
import { feedbackApi, type DeliveredTrip } from './api';
import { StarInput } from './StarInput';

/** Khách đánh giá (hoặc sửa trong 7 ngày) một chuyến đã giao. */
export function FeedbackModal({ trip, onClose, onDone }: { trip?: DeliveredTrip; onClose: () => void; onDone: () => void }) {
  const [p, setP] = useState(5);
  const [c, setC] = useState(5);
  const [m, setM] = useState(5);
  const [comment, setComment] = useState('');
  const { run, busy } = useAction();

  useEffect(() => {
    if (!trip) return;
    setP(trip.feedback?.punctuality ?? 5); setC(trip.feedback?.care ?? 5); setM(trip.feedback?.communication ?? 5);
    setComment(trip.feedback?.comment ?? '');
  }, [trip]);

  const submit = async () => {
    if (!trip) return;
    try {
      await run(() => feedbackApi.submit(trip.tripId, { punctuality: p, care: c, communication: m, comment: comment.trim() || undefined }), 'Đã gửi đánh giá, cảm ơn bạn');
      onClose(); onDone();
    } catch { /* toast */ }
  };

  return (
    <Modal open={!!trip} title={`Đánh giá chuyến ${trip?.tripNumber ?? ''}`} icon="bi-star" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Gửi đánh giá</Button></>}>
      <div className="ir-stack">
        <StarInput label="Đúng giờ" value={p} onChange={setP} />
        <StarInput label="Chăm sóc ngựa" value={c} onChange={setC} />
        <StarInput label="Giao tiếp" value={m} onChange={setM} />
        <TextAreaField label="Nhận xét" optional value={comment} onChange={setComment} maxLength={1000} rows={3} />
      </div>
    </Modal>
  );
}
