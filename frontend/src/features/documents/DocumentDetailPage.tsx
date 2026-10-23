import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { openFile } from '../../api/client';
import { documentApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Alert, AsyncView, Button, Card, DescriptionList, Modal, PageHeader, SelectField, StatusBadge, TextAreaField } from '../../components';
import { DocumentReviewStatusLabels, DocumentTypeLabels, type DocumentReviewStatus } from '../../types/enums';
import { countryName, formatBytes, formatDate, formatDateTime } from '../../utils/format';
import { RefLink } from '../common/bits';
import { refreshCounts } from '../../layouts/AppLayout';

export function DocumentDetailPage() {
  const id = Number(useParams().id);
  const user = useCurrentUser();
  const state = useAsync(() => documentApi.get(id), [id]);
  const { run, busy } = useAction();
  const [open, setOpen] = useState(false);
  const [status, setStatus] = useState<DocumentReviewStatus>('APPROVED');
  const [note, setNote] = useState('');
  const [err, setErr] = useState<string>();

  const submit = async () => {
    if (status !== 'APPROVED' && note.trim().length < 3) { setErr('Cần ghi rõ lý do khi từ chối hoặc yêu cầu cập nhật'); return; }
    setErr(undefined);
    await run(() => documentApi.review(id, status, note.trim() || undefined), 'Đã ghi nhận kết quả rà soát');
    setOpen(false); setNote('');
    refreshCounts();
    state.reload();
  };

  return (
    <AsyncView state={state}>
      {(d) => (
        <>
          <PageHeader
            title={DocumentTypeLabels[d.documentType]}
            back={{ to: '/documents', label: 'Danh sách tài liệu' }}
            subtitle={d.fileName}
            meta={<StatusBadge status={d.reviewStatus} labels={DocumentReviewStatusLabels} size="md" />}
            actions={<>
              <Button variant="outline" icon="bi-download" onClick={() => void openFile(documentApi.filePath(d.id), d.fileName, true)}>Tải xuống</Button>
              <Button variant={user.role === 'TRANSPORT_SPECIALIST' ? 'outline' : 'primary'} icon="bi-eye" onClick={() => void openFile(documentApi.filePath(d.id))}>Xem tệp</Button>
              {user.role === 'TRANSPORT_SPECIALIST' && d.reviewStatus !== 'EXPIRED' && <Button icon="bi-clipboard-check" onClick={() => setOpen(true)}>Rà soát</Button>}
            </>}
          />
          {d.expired && <Alert tone="error" title="Tài liệu đã hết hạn">Khách hàng cần tải bản còn hiệu lực.</Alert>}
          {d.reviewNote && <Alert tone={d.reviewStatus === 'APPROVED' ? 'success' : 'warning'} title={`Ghi chú rà soát${d.reviewerName ? ` – ${d.reviewerName}` : ''}`}>{d.reviewNote}</Alert>}
          <Card title="Thông tin tài liệu" icon="bi-file-earmark-text">
            <DescriptionList items={[
              { label: 'Loại tài liệu', value: DocumentTypeLabels[d.documentType] },
              { label: 'Số tài liệu', value: d.documentNumber },
              { label: 'Quốc gia cấp', value: d.issuingCountry ? countryName(d.issuingCountry) : undefined },
              { label: 'Ngày cấp', value: formatDate(d.issueDate) },
              { label: 'Ngày hết hạn', value: d.expiryDate ? `${formatDate(d.expiryDate)}${d.daysToExpiry != null && !d.expired ? ` (còn ${d.daysToExpiry} ngày)` : ''}` : undefined },
              { label: 'Ngựa', value: d.horseId ? <RefLink to={`/horses/${d.horseId}`}>{d.horseName}</RefLink> : undefined },
              { label: 'Yêu cầu', value: d.requestId ? <RefLink to={`/requests/${d.requestId}`}>{d.requestNumber}</RefLink> : undefined },
              { label: 'Khách hàng', value: d.customerName },
              { label: 'Tệp', value: `${d.fileName} (${formatBytes(d.sizeBytes)})` },
              { label: 'Tải lên bởi', value: `${d.uploadedByName} – ${formatDateTime(d.uploadedAt)}` },
              { label: 'Người rà soát', value: d.reviewerName ? `${d.reviewerName} – ${formatDateTime(d.reviewedAt)}` : undefined },
            ]} />
          </Card>
          <Modal open={open} onClose={() => setOpen(false)} onSubmit={submit} busy={busy} size="sm" icon="bi-clipboard-check" title="Rà soát tài liệu" description={d.fileName}
            footer={<><Button variant="outline" onClick={() => setOpen(false)}>Hủy</Button><Button type="submit" loading={busy}>Xác nhận</Button></>}>
            <div className="ir-stack">
              <SelectField label="Kết quả" value={status} onChange={(v) => setStatus(v as DocumentReviewStatus)} options={[
                { value: 'APPROVED', label: 'Chấp thuận' }, { value: 'NEEDS_UPDATE', label: 'Cần cập nhật' }, { value: 'REJECTED', label: 'Từ chối' },
              ]} />
              <TextAreaField label="Ghi chú" value={note} onChange={setNote} rows={3} error={err} required={status !== 'APPROVED'} optional={status === 'APPROVED'} />
            </div>
          </Modal>
        </>
      )}
    </AsyncView>
  );
}
