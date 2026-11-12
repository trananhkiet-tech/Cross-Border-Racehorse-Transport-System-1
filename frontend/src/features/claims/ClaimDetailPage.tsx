import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { claimApi } from '../../api/endpoints';
import { openFile } from '../../api/client';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import {
  Alert, AsyncView, Badge, Button, Card, ConfirmDialog, DataTable, DescriptionList, FileDrop, FileRow, Modal, NumberField,
  PageHeader, SelectField, StatusBadge, Stepper, TextAreaField, enumOptions,
} from '../../components';
import type { StepItem } from '../../components';
import { refreshCounts } from '../../layouts/AppLayout';
import {
  ClaimRecommendationLabels, ClaimRecommendationValues, ClaimStatusLabels, ClaimTypeLabels, EvidenceTypeLabels, EvidenceTypeValues,
  IncidentSeverityLabels, PaymentMethodLabels, PaymentMethodValues, SettlementStatusLabels,
  type ClaimRecommendation, type EvidenceType, type PaymentMethod,
} from '../../types/enums';
import type { ClaimDetail } from '../../types/api';
import { formatDate, formatDateTime } from '../../utils/format';
import { Money, RefLink } from '../common/bits';
import { hasErrors } from '../common/validation';

function steps(c: ClaimDetail): StepItem[] {
  const s = c.status;
  const decided = ['APPROVED', 'PARTIALLY_APPROVED', 'REJECTED', 'PAID', 'CLOSED'].includes(s);
  const paid = s === 'PAID' || (s === 'CLOSED' && c.settledAmount > 0);
  return [
    { label: 'Đã nộp', state: 'done' },
    { label: 'Thẩm định', state: s === 'SUBMITTED' ? 'current' : s === 'UNDER_REVIEW' || s === 'EVIDENCE_REQUIRED' ? 'current' : 'done' },
    { label: s === 'REJECTED' ? 'Từ chối' : 'Quyết định', state: s === 'REJECTED' ? 'error' : decided ? 'done' : 'upcoming' },
    { label: 'Chi trả', state: paid ? 'done' : s === 'APPROVED' || s === 'PARTIALLY_APPROVED' ? 'current' : 'upcoming' },
    { label: 'Đóng hồ sơ', state: s === 'CLOSED' ? 'done' : 'upcoming' },
  ];
}

function EvidenceModal({ open, claim, onClose, onDone }: { open: boolean; claim: ClaimDetail; onClose: () => void; onDone: () => void }) {
  const [type, setType] = useState<EvidenceType>('PHOTO');
  const [notes, setNotes] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [fileErr, setFileErr] = useState<string>();
  const { run, busy } = useAction();
  useEffect(() => { if (open) { setType('PHOTO'); setNotes(''); setFile(null); setFileErr(undefined); } }, [open]);
  const submit = async () => {
    if (!file) { setFileErr('Vui lòng chọn tệp'); return; }
    try { await run(() => claimApi.addEvidence(claim.id, file, type, notes.trim() || undefined), 'Đã bổ sung bằng chứng'); onClose(); onDone(); } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Bổ sung bằng chứng" icon="bi-paperclip" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy} icon="bi-upload">Tải lên</Button></>}>
      <div className="ir-stack">
        <SelectField label="Loại bằng chứng" required value={type} onChange={(v) => setType(v as EvidenceType)} options={enumOptions(EvidenceTypeValues, EvidenceTypeLabels)} />
        <FileDrop file={file} error={fileErr} onFile={(f, e) => { setFile(f); setFileErr(e); }} onClear={() => setFile(null)} />
        <TextAreaField label="Ghi chú" optional rows={2} value={notes} onChange={setNotes} maxLength={500} />
      </div>
    </Modal>
  );
}

function ReviewModal({ open, claim, onClose, onDone }: { open: boolean; claim: ClaimDetail; onClose: () => void; onDone: () => void }) {
  const [rec, setRec] = useState<ClaimRecommendation | ''>('');
  const [note, setNote] = useState('');
  const [amount, setAmount] = useState<number | undefined>();
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  useEffect(() => { if (open) { setRec(''); setNote(''); setAmount(undefined); setErr({}); } }, [open]);
  const needsAmount = rec === 'APPROVE' || rec === 'PARTIALLY_APPROVE';
  const submit = async () => {
    const e = {
      rec: rec ? undefined : 'Chọn đề xuất',
      note: note.trim().length < 3 ? 'Nhập nhận định thẩm định' : undefined,
      amount: needsAmount && (amount === undefined || amount <= 0) ? 'Nhập số tiền đề xuất' : amount !== undefined && amount > claim.claimedAmount ? 'Không vượt quá số tiền yêu cầu' : undefined,
    };
    setErr(e);
    if (hasErrors(e)) return;
    try { await run(() => claimApi.review(claim.id, { assessmentNote: note.trim(), recommendation: rec as ClaimRecommendation, recommendedAmount: needsAmount ? amount : undefined }), 'Đã ghi nhận thẩm định'); onClose(); onDone(); } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Thẩm định hồ sơ" description={`Số tiền yêu cầu: ${claim.claimedAmount} ${claim.currency}`} icon="bi-clipboard-check" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Lưu thẩm định</Button></>}>
      <div className="ir-stack">
        <SelectField label="Đề xuất" required value={rec} onChange={(v) => setRec(v as ClaimRecommendation)} placeholder="Chọn đề xuất" error={err.rec} options={enumOptions(ClaimRecommendationValues, ClaimRecommendationLabels)} />
        {needsAmount && <NumberField label={`Số tiền đề xuất (${claim.currency})`} required min={0} step="0.01" value={amount} onChange={setAmount} error={err.amount} />}
        <TextAreaField label="Nhận định thẩm định" required rows={4} value={note} onChange={setNote} error={err.note} maxLength={2000} />
        {rec === 'REQUEST_EVIDENCE' && <Alert tone="info">Hồ sơ chuyển sang "Cần bổ sung bằng chứng"; khách hàng sẽ nhận thông báo.</Alert>}
      </div>
    </Modal>
  );
}

function ApproveModal({ open, claim, onClose, onDone }: { open: boolean; claim: ClaimDetail; onClose: () => void; onDone: () => void }) {
  const [amount, setAmount] = useState<number | undefined>();
  const [note, setNote] = useState('');
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  const last = claim.assessments[claim.assessments.length - 1];
  useEffect(() => { if (open) { setAmount(last?.recommendedAmount ?? undefined); setNote(''); setErr({}); } }, [open]); // eslint-disable-line react-hooks/exhaustive-deps
  const submit = async () => {
    const e = {
      amount: amount === undefined || amount <= 0 ? 'Nhập số tiền duyệt lớn hơn 0' : amount > claim.claimedAmount ? `Không vượt quá số yêu cầu (${claim.claimedAmount})` : undefined,
      note: note.trim().length < 3 ? 'Nhập lý do quyết định' : undefined,
    };
    setErr(e);
    if (hasErrors(e)) return;
    try { await run(() => claimApi.approve(claim.id, amount!, note.trim()), 'Đã phê duyệt hồ sơ'); onClose(); onDone(); } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Phê duyệt bồi thường" description="Số tiền duyệt do bạn quyết định; không tự động lấy từ giá trị khai báo." icon="bi-check-circle" tone="success" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" variant="success" loading={busy}>Phê duyệt</Button></>}>
      <div className="ir-stack">
        <NumberField label={`Số tiền duyệt (${claim.currency})`} required min={0} step="0.01" value={amount} onChange={setAmount} error={err.amount} help={`Số yêu cầu: ${claim.claimedAmount} ${claim.currency}. Nhỏ hơn số yêu cầu = duyệt một phần.`} />
        <TextAreaField label="Lý do quyết định" required rows={3} value={note} onChange={setNote} error={err.note} maxLength={2000} />
      </div>
    </Modal>
  );
}

function SettleModal({ open, claim, onClose, onDone }: { open: boolean; claim: ClaimDetail; onClose: () => void; onDone: () => void }) {
  const remaining = Math.max(0, (claim.approvedAmount ?? 0) - claim.settledAmount);
  const [amount, setAmount] = useState<number | undefined>();
  const [method, setMethod] = useState<PaymentMethod>('SIMULATED_BANK_TRANSFER');
  const [note, setNote] = useState('');
  const [err, setErr] = useState<string>();
  const { run, busy } = useAction();
  useEffect(() => { if (open) { setAmount(remaining || undefined); setMethod('SIMULATED_BANK_TRANSFER'); setNote(''); setErr(undefined); } }, [open]); // eslint-disable-line react-hooks/exhaustive-deps
  const submit = async () => {
    if (amount === undefined || amount <= 0) { setErr('Nhập số tiền chi trả'); return; }
    if (amount > remaining) { setErr(`Không vượt quá số còn lại (${remaining})`); return; }
    try { await run(() => claimApi.settle(claim.id, amount, method, note.trim() || undefined), 'Đã ghi nhận chi trả (mô phỏng)'); onClose(); onDone(); } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Chi trả bồi thường (mô phỏng)" description="Hệ thống chỉ ghi nhận giao dịch mô phỏng, không chuyển tiền thật." icon="bi-cash-coin" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Ghi nhận chi trả</Button></>}>
      <div className="ir-stack">
        <NumberField label={`Số tiền chi trả (${claim.currency})`} required min={0} step="0.01" value={amount} onChange={setAmount} error={err} help={`Còn lại: ${remaining} ${claim.currency}`} />
        <SelectField label="Phương thức" value={method} onChange={(v) => setMethod(v as PaymentMethod)} options={enumOptions(PaymentMethodValues, PaymentMethodLabels)} />
        <TextAreaField label="Ghi chú" optional rows={2} value={note} onChange={setNote} maxLength={1000} />
      </div>
    </Modal>
  );
}

type Dlg = 'evidence' | 'review' | 'approve' | 'reject' | 'settle' | 'close' | null;

export function ClaimDetailPage() {
  const id = Number(useParams().id);
  const state = useAsync(() => claimApi.get(id), [id]);
  const [dlg, setDlg] = useState<Dlg>(null);
  const { run } = useAction();
  const close = () => setDlg(null);
  const done = () => { state.reload(); refreshCounts(); };

  return (
    <AsyncView state={state}>
      {(c) => {
        const has = (a: string) => c.allowedActions.includes(a);
        return (
          <>
            <PageHeader title={c.claimNumber} back={{ to: '/claims', label: 'Danh sách bồi thường' }}
              meta={<StatusBadge status={c.status} labels={ClaimStatusLabels} size="md" />}
              subtitle={`${ClaimTypeLabels[c.claimType]} · ${c.customerName}`}
              actions={
                <>
                  {has('ADD_EVIDENCE') && <Button variant="outline" icon="bi-paperclip" onClick={() => setDlg('evidence')}>Bổ sung bằng chứng</Button>}
                  {has('REVIEW') && <Button variant="outline" icon="bi-clipboard-check" onClick={() => setDlg('review')}>Thẩm định</Button>}
                  {has('APPROVE') && <Button variant="success" icon="bi-check-circle" onClick={() => setDlg('approve')}>Phê duyệt</Button>}
                  {has('REJECT') && <Button variant="danger-outline" icon="bi-x-circle" onClick={() => setDlg('reject')}>Từ chối</Button>}
                  {has('SETTLE') && <Button icon="bi-cash-coin" onClick={() => setDlg('settle')}>Chi trả</Button>}
                  {has('CLOSE') && <Button variant="outline" icon="bi-archive" onClick={() => setDlg('close')}>Đóng hồ sơ</Button>}
                </>
              } />
            <Card><Stepper steps={steps(c)} /></Card>
            {c.status === 'EVIDENCE_REQUIRED' && <Alert tone="warning" title="Cần bổ sung bằng chứng">{c.assessments[c.assessments.length - 1]?.assessmentNote ?? 'Quản lý yêu cầu bổ sung bằng chứng.'}</Alert>}
            {c.status === 'REJECTED' && <Alert tone="error" title="Hồ sơ bị từ chối">{c.decisionNote}</Alert>}
            {(c.status === 'APPROVED' || c.status === 'PARTIALLY_APPROVED' || c.status === 'PAID') && c.decisionNote && <Alert tone="success" title={c.status === 'PARTIALLY_APPROVED' ? 'Duyệt một phần' : 'Đã duyệt'}>{c.decisionNote}</Alert>}
            <div className="app-grid app-grid-main">
              <div className="ir-stack">
                <Card title="Thông tin hồ sơ" icon="bi-shield-exclamation">
                  <DescriptionList items={[
                    { label: 'Yêu cầu', value: <RefLink to={`/requests/${c.requestId}`}>{c.requestNumber}</RefLink> },
                    { label: 'Chuyến', value: c.tripId ? <RefLink to={`/trips/${c.tripId}`}>{c.tripNumber}</RefLink> : '—' },
                    { label: 'Sự cố', value: c.incidentId ? <span className="app-row" style={{ gap: 6 }}><RefLink to={`/incidents/${c.incidentId}`}>{c.incidentNumber}</RefLink>{c.incidentSeverity && <StatusBadge status={c.incidentSeverity} labels={IncidentSeverityLabels} />}</span> : '—' },
                    { label: 'Ngựa', value: c.horseId ? <RefLink to={`/horses/${c.horseId}`}>{c.horseName}</RefLink> : '—' },
                    { label: 'Nộp lúc', value: formatDateTime(c.submittedAt) },
                    { label: 'Người thẩm định', value: c.reviewedBy ?? '—' },
                    { label: 'Mô tả', value: c.description, span: true },
                  ]} />
                </Card>
                <Card title={`Bằng chứng (${c.evidence.length})`} icon="bi-paperclip">
                  {c.evidence.length === 0 ? <span className="ir-muted">Chưa có bằng chứng. Cần ít nhất một bằng chứng trước khi phê duyệt.</span> : (
                    <div className="ir-stack" style={{ gap: 8 }}>
                      {c.evidence.map((e) => (
                        <FileRow key={e.id} name={e.fileName} size={e.sizeBytes} type={e.contentType} meta={`${EvidenceTypeLabels[e.evidenceType]} · ${e.uploadedBy} · ${formatDate(e.uploadedAt)}${e.notes ? ` · ${e.notes}` : ''}`} onOpen={() => void openFile(claimApi.evidencePath(c.id, e.id))} />
                      ))}
                    </div>
                  )}
                </Card>
                <Card title="Thẩm định" icon="bi-clipboard-check" flush>
                  <DataTable flush dense rows={c.assessments} rowKey={(a) => a.id} emptyTitle="Chưa có thẩm định"
                    columns={[
                      { key: 'r', header: 'Đề xuất', render: (a) => <Badge tone="info">{ClaimRecommendationLabels[a.recommendation]}</Badge> },
                      { key: 'a', header: 'Số tiền đề xuất', align: 'right', render: (a) => (a.recommendedAmount != null ? <Money value={a.recommendedAmount} currency={c.currency} /> : '—') },
                      { key: 'n', header: 'Nhận định', render: (a) => a.assessmentNote },
                      { key: 'b', header: 'Người thẩm định', render: (a) => `${a.assessedBy} · ${formatDateTime(a.assessedAt)}` },
                    ]} />
                </Card>
              </div>
              <div className="ir-stack">
                <Card title="Số tiền" icon="bi-cash-stack">
                  <DescriptionList columns={1} items={[
                    { label: 'Yêu cầu', value: <Money value={c.claimedAmount} currency={c.currency} strong /> },
                    { label: 'Giá trị khai báo (tham chiếu)', value: c.declaredValueReference != null ? <Money value={c.declaredValueReference} currency={c.currency} /> : '—' },
                    { label: 'Đã duyệt', value: c.approvedAmount != null ? <Money value={c.approvedAmount} currency={c.currency} strong /> : 'Chưa duyệt' },
                    { label: 'Đã chi trả', value: <Money value={c.settledAmount} currency={c.currency} /> },
                    { label: 'Quyết định lúc', value: formatDateTime(c.decidedAt) },
                  ]} />
                </Card>
                <Card title="Chi trả" icon="bi-cash-coin" flush>
                  <DataTable flush dense rows={c.settlements} rowKey={(s) => s.id} emptyTitle="Chưa có chi trả"
                    columns={[
                      { key: 'r', header: 'Mã', render: (s) => <span className="ir-mono">{s.settlementReference}</span> },
                      { key: 'a', header: 'Số tiền', align: 'right', render: (s) => <Money value={s.amount} currency={s.currency} /> },
                      { key: 's', header: 'Trạng thái', render: (s) => <StatusBadge status={s.status} labels={SettlementStatusLabels} /> },
                      { key: 'd', header: 'Thời điểm', render: (s) => formatDateTime(s.settledAt) },
                    ]} />
                </Card>
              </div>
            </div>
            <EvidenceModal open={dlg === 'evidence'} claim={c} onClose={close} onDone={done} />
            <ReviewModal open={dlg === 'review'} claim={c} onClose={close} onDone={done} />
            <ApproveModal open={dlg === 'approve'} claim={c} onClose={close} onDone={done} />
            <SettleModal open={dlg === 'settle'} claim={c} onClose={close} onDone={done} />
            <ConfirmDialog open={dlg === 'reject'} onClose={close} title="Từ chối hồ sơ?" tone="error" requireReason reasonLabel="Lý do từ chối" confirmLabel="Từ chối" message="Khách hàng sẽ nhận được lý do từ chối."
              onConfirm={async (n) => { await run(() => claimApi.reject(c.id, n), 'Đã từ chối hồ sơ'); done(); }} />
            <ConfirmDialog open={dlg === 'close'} onClose={close} title="Đóng hồ sơ?" allowNote reasonLabel="Ghi chú (không bắt buộc)" confirmLabel="Đóng hồ sơ" message="Hồ sơ đã đóng không thể chỉnh sửa."
              onConfirm={async (n) => { await run(() => claimApi.close(c.id, n || undefined), 'Đã đóng hồ sơ'); done(); }} />
          </>
        );
      }}
    </AsyncView>
  );
}
