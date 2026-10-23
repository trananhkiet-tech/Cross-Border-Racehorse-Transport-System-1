import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { handoverApi, tripApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Alert, AsyncView, Button, Card, ConfirmDialog, DescriptionList, PageHeader, StatusBadge } from '../../components';
import { refreshCounts } from '../../layouts/AppLayout';
import { HandoverStatusLabels, HorseConditionLabels, TripStatusLabels } from '../../types/enums';
import { formatDateTime } from '../../utils/format';
import { AttachmentList, AttachmentUploadModal } from '../common/Attachments';
import { RefLink } from '../common/bits';
import { HandoverFormModal } from './HandoverFormModal';

export function HandoverDetailPage() {
  const id = Number(useParams().id);
  const state = useAsync(() => handoverApi.get(id), [id]);
  const [dlg, setDlg] = useState<'edit' | 'file' | 'confirm' | 'dispute' | null>(null);
  const { run } = useAction();
  const close = () => setDlg(null);
  const done = () => { state.reload(); refreshCounts(); };
  const trip = useAsync(() => tripApi.get(state.data!.tripId), [state.data?.tripId], !!state.data && dlg === 'edit');

  return (
    <AsyncView state={state}>
      {(h) => {
        const has = (a: string) => h.allowedActions.includes(a);
        return (
          <>
            <PageHeader title={h.handoverNumber} back={{ to: '/handovers', label: 'Danh sách bàn giao' }}
              meta={<StatusBadge status={h.status} labels={HandoverStatusLabels} size="md" />}
              subtitle={`${h.horseName} · chuyến ${h.tripNumber}`}
              actions={
                <>
                  {has('EDIT') && <Button variant="outline" icon="bi-pencil" onClick={() => setDlg('edit')}>Sửa biên bản</Button>}
                  {has('ADD_EVIDENCE') && <Button variant="outline" icon="bi-camera" onClick={() => setDlg('file')}>Thêm bằng chứng</Button>}
                  {has('DISPUTE') && <Button variant="danger-outline" icon="bi-chat-left-dots" onClick={() => setDlg('dispute')}>Phản hồi vấn đề</Button>}
                  {has('CONFIRM') && <Button variant="success" icon="bi-check-circle" onClick={() => setDlg('confirm')}>Xác nhận đã nhận ngựa</Button>}
                </>
              } />
            {h.status === 'PENDING_CONFIRMATION' && <Alert tone="info" title="Đang chờ khách hàng xác nhận">Khách hàng xác nhận sau khi nhận ngựa; nếu có vấn đề, khách hàng gửi phản hồi kèm mô tả.</Alert>}
            {h.status === 'DISPUTED' && <Alert tone="error" title="Khách hàng phản hồi có vấn đề">{h.customerComment}</Alert>}
            {h.status === 'CONFIRMED' && <Alert tone="success" title="Đã xác nhận bàn giao">{h.confirmedBy} xác nhận lúc {formatDateTime(h.confirmedAt)}.{h.customerComment ? ` Ghi chú: ${h.customerComment}` : ''}</Alert>}
            <div className="app-grid app-grid-main">
              <Card title="Thông tin bàn giao" icon="bi-box-arrow-in-down">
                <DescriptionList items={[
                  { label: 'Ngựa', value: <RefLink to={`/horses/${h.horseId}`}>{h.horseName}</RefLink> },
                  { label: 'Microchip', value: <span className="ir-mono">{h.microchipNumber}</span> },
                  { label: 'Chuyến', value: <RefLink to={`/trips/${h.tripId}`}>{h.tripNumber}</RefLink> },
                  { label: 'Trạng thái chuyến', value: <StatusBadge status={h.tripStatus} labels={TripStatusLabels} /> },
                  { label: 'Địa điểm', value: h.destination, span: true },
                  { label: 'Thời điểm', value: formatDateTime(h.handoverTime) },
                  { label: 'Người lập', value: h.createdBy },
                  { label: 'Người nhận', value: h.receiverName },
                  { label: 'Liên hệ', value: h.receiverContact },
                  { label: 'Tình trạng khi đến', value: h.arrivalCondition ? HorseConditionLabels[h.arrivalCondition] : '—' },
                  { label: 'Tình trạng khi bàn giao', value: HorseConditionLabels[h.condition] },
                  { label: 'Giấy tờ', value: h.documentsHandedOver ? 'Đã bàn giao đầy đủ' : 'Chưa bàn giao' },
                  { label: 'Ghi chú', value: h.notes, span: true },
                ]} />
              </Card>
              <Card title="Bằng chứng" icon="bi-paperclip"><AttachmentList items={h.evidence} /></Card>
            </div>
            {dlg === 'edit' && trip.data && <HandoverFormModal open trip={trip.data} handover={h} onClose={close} onDone={done} />}
            <AttachmentUploadModal open={dlg === 'file'} refType="HANDOVER" refId={h.id} title="Thêm bằng chứng bàn giao" onClose={close} onDone={done} />
            <ConfirmDialog open={dlg === 'confirm'} onClose={close} title="Xác nhận đã nhận ngựa?" tone="success" confirmLabel="Xác nhận" allowNote reasonLabel="Ghi chú (không bắt buộc)"
              message={`Bạn xác nhận đã nhận ${h.horseName} theo biên bản ${h.handoverNumber}.`}
              onConfirm={async (n) => { await run(() => handoverApi.confirm(h.id, true, n || undefined), 'Đã xác nhận bàn giao'); done(); }} />
            <ConfirmDialog open={dlg === 'dispute'} onClose={close} title="Phản hồi vấn đề khi nhận ngựa" tone="error" requireReason reasonLabel="Mô tả vấn đề" confirmLabel="Gửi phản hồi"
              message="Quản lý và điều phối sẽ nhận thông báo. Bạn vẫn có thể tạo hồ sơ bồi thường nếu cần."
              onConfirm={async (n) => { await run(() => handoverApi.confirm(h.id, false, n), 'Đã gửi phản hồi'); done(); }} />
          </>
        );
      }}
    </AsyncView>
  );
}
