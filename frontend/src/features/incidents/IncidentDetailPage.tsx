import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { healthApi, incidentApi } from '../../api/endpoints';
import { openFile } from '../../api/client';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import {
  Alert, AsyncView, Badge, Button, Card, DescriptionList, LinkButton, Modal, NumberField, PageHeader,
  SelectField, StatusBadge, TextAreaField, Timeline, enumOptions,
} from '../../components';
import type { TimelineEntry } from '../../components';
import { refreshCounts } from '../../layouts/AppLayout';
import {
  IncidentActionTypeLabels, IncidentSeverityLabels, IncidentStatusLabels, IncidentTypeLabels, OperationalDecisionLabels,
  OperationalDecisionValues, RoleLabels, TripStatusLabels, type IncidentActionType, type IncidentStatus,
  type OperationalDecision,
} from '../../types/enums';
import type { IncidentDetail } from '../../types/api';
import { formatDateTime, formatMoney } from '../../utils/format';
import { AttachmentList, AttachmentUploadModal } from '../common/Attachments';
import { RefLink } from '../common/bits';
import { hasErrors } from '../common/validation';
import { VetConsultationModal } from '../health-observations/HealthForms';

const FIELD_ACTIONS: IncidentActionType[] = ['NOTE', 'FIELD_ACTION', 'VET_CONTACTED', 'ESCALATION'];
const NEXT_STATUS: Record<IncidentStatus, IncidentStatus[]> = {
  OPEN: ['UNDER_REVIEW', 'ACTION_REQUIRED', 'MONITORING', 'RESOLVED'],
  UNDER_REVIEW: ['ACTION_REQUIRED', 'MONITORING', 'RESOLVED'],
  ACTION_REQUIRED: ['UNDER_REVIEW', 'MONITORING', 'RESOLVED'],
  MONITORING: ['ACTION_REQUIRED', 'RESOLVED'],
  RESOLVED: ['CLOSED', 'MONITORING'],
  CLOSED: [],
};

function ActionModal({ open, inc, onClose, onDone }: { open: boolean; inc: IncidentDetail; onClose: () => void; onDone: () => void }) {
  const [type, setType] = useState<IncidentActionType>('NOTE');
  const [note, setNote] = useState('');
  const [err, setErr] = useState<string>();
  const { run, busy } = useAction();
  useEffect(() => { if (open) { setType('NOTE'); setNote(''); setErr(undefined); } }, [open]);
  const submit = async () => {
    if (note.trim().length < 3) { setErr('Nhập nội dung (tối thiểu 3 ký tự)'); return; }
    try { await run(() => incidentApi.addAction(inc.id, { actionType: type, note: note.trim() }), 'Đã ghi nhận hành động'); onClose(); onDone(); } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Ghi nhận hành động xử lý" icon="bi-journal-plus" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Lưu</Button></>}>
      <div className="ir-stack">
        <SelectField label="Loại hành động" required value={type} onChange={(v) => setType(v as IncidentActionType)} options={FIELD_ACTIONS.map((a) => ({ value: a, label: IncidentActionTypeLabels[a] }))} />
        <TextAreaField label="Nội dung" required rows={4} value={note} onChange={setNote} error={err} maxLength={2000} />
      </div>
    </Modal>
  );
}

function DecideModal({ open, inc, onClose, onDone }: { open: boolean; inc: IncidentDetail; onClose: () => void; onDone: () => void }) {
  const [decision, setDecision] = useState<OperationalDecision | ''>('');
  const [note, setNote] = useState('');
  const [routeId, setRouteId] = useState<number | undefined>();
  const [cost, setCost] = useState<number | undefined>();
  const [consultationId, setConsultationId] = useState<number | undefined>();
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  useEffect(() => { if (open) { setDecision(''); setNote(''); setRouteId(undefined); setCost(undefined); setConsultationId(undefined); setErr({}); } }, [open]);
  const submit = async () => {
    const e = {
      decision: decision ? undefined : 'Chọn quyết định',
      note: note.trim().length < 3 ? 'Nhập lý do / hướng dẫn (tối thiểu 3 ký tự)' : undefined,
      route: decision === 'REROUTE' && !routeId ? 'Chọn tuyến thay thế' : undefined,
      cost: cost !== undefined && cost < 0 ? 'Chi phí không âm' : undefined,
    };
    setErr(e);
    if (hasErrors(e)) return;
    try {
      await run(() => incidentApi.addAction(inc.id, { actionType: 'DECISION', decision: decision as OperationalDecision, note: note.trim(), newRouteId: decision === 'REROUTE' ? routeId : undefined, extraCost: cost, currency: cost !== undefined ? 'USD' : undefined, consultationId }), 'Đã ghi quyết định vận hành');
      onClose(); onDone();
    } catch { /* toast */ }
  };
  const hint: Record<OperationalDecision, string> = {
    CONTINUE: 'Chuyến tiếp tục hành trình, hết trạng thái tạm dừng.',
    MONITOR: 'Chuyến tiếp tục nhưng sự cố chuyển sang theo dõi.',
    STOP: 'Chuyến giữ trạng thái tạm dừng cho đến khi có quyết định khác.',
    REROUTE: 'Chuyến đổi sang tuyến thay thế; chi phí phát sinh (nếu có) được ghi nhận để báo giá bổ sung.',
  };
  return (
    <Modal open={open} title="Quyết định vận hành" description="Chọn hướng xử lý. Quyết định được ghi lại trong lịch sử sự cố." icon="bi-signpost-split" tone="error" size="lg" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Xác nhận quyết định</Button></>}>
      <div className="ir-stack">
        <SelectField label="Quyết định" required value={decision} onChange={(v) => setDecision(v as OperationalDecision)} placeholder="Chọn quyết định" error={err.decision} options={enumOptions(OperationalDecisionValues, OperationalDecisionLabels)} help={decision ? hint[decision] : undefined} />
        {decision === 'REROUTE' && (
          <SelectField label="Tuyến thay thế" required value={routeId} onChange={(v) => setRouteId(v ? Number(v) : undefined)} placeholder={inc.rerouteOptions.length ? 'Chọn tuyến' : 'Không có tuyến thay thế khả dụng'} error={err.route}
            options={inc.rerouteOptions.map((r) => ({ value: String(r.id), label: `${r.routeCode} – ${r.name}` }))} />
        )}
        {(decision === 'REROUTE' || decision === 'STOP') && <NumberField label="Chi phí phát sinh dự kiến (USD)" optional min={0} step="0.01" value={cost} onChange={setCost} error={err.cost} help="Chỉ ghi nhận; không tự động tính vào hóa đơn." />}
        {inc.consultations.length > 0 && (
          <SelectField label="Dựa trên tư vấn thú y" optional value={consultationId} onChange={(v) => setConsultationId(v ? Number(v) : undefined)} placeholder="Không gắn"
            options={inc.consultations.map((c) => ({ value: String(c.id), label: `${c.veterinarianName} · ${formatDateTime(c.consultedAt)}` }))} />
        )}
        <TextAreaField label="Lý do / hướng dẫn cho đội vận chuyển" required rows={4} value={note} onChange={setNote} error={err.note} maxLength={2000} />
      </div>
    </Modal>
  );
}

function StatusModal({ open, inc, onClose, onDone }: { open: boolean; inc: IncidentDetail; onClose: () => void; onDone: () => void }) {
  const [status, setStatus] = useState<IncidentStatus | ''>('');
  const [note, setNote] = useState('');
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy } = useAction();
  useEffect(() => { if (open) { setStatus(''); setNote(''); setErr({}); } }, [open]);
  const submit = async () => {
    const e = { status: status ? undefined : 'Chọn trạng thái', note: (status === 'RESOLVED' || status === 'CLOSED') && note.trim().length < 3 ? 'Nhập ghi chú giải quyết' : undefined };
    setErr(e);
    if (hasErrors(e)) return;
    try { await run(() => incidentApi.changeStatus(inc.id, status as IncidentStatus, note.trim() || undefined), 'Đã cập nhật trạng thái sự cố'); onClose(); onDone(); } catch { /* toast */ }
  };
  return (
    <Modal open={open} title="Cập nhật trạng thái sự cố" icon="bi-arrow-repeat" onClose={onClose} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Cập nhật</Button></>}>
      <div className="ir-stack">
        <SelectField label="Trạng thái mới" required value={status} onChange={(v) => setStatus(v as IncidentStatus)} placeholder="Chọn trạng thái" error={err.status} options={NEXT_STATUS[inc.status].map((s) => ({ value: s, label: IncidentStatusLabels[s] }))} />
        <TextAreaField label="Ghi chú" required={status === 'RESOLVED' || status === 'CLOSED'} optional={!(status === 'RESOLVED' || status === 'CLOSED')} rows={3} value={note} onChange={setNote} error={err.note} maxLength={1000} />
      </div>
    </Modal>
  );
}

type Dlg = 'action' | 'decide' | 'status' | 'vet' | 'file' | null;

export function IncidentDetailPage() {
  const id = Number(useParams().id);
  const state = useAsync(() => incidentApi.get(id), [id]);
  const [dlg, setDlg] = useState<Dlg>(null);
  const close = () => setDlg(null);
  const done = () => { state.reload(); refreshCounts(); };

  return (
    <AsyncView state={state}>
      {(i) => {
        const has = (a: string) => i.allowedActions.includes(a);
        const entries: TimelineEntry[] = [
          ...i.actions.map((a) => ({
            key: `a${a.id}`,
            title: a.actionType === 'DECISION' && a.decision ? `Quyết định: ${OperationalDecisionLabels[a.decision]}` : IncidentActionTypeLabels[a.actionType],
            time: formatDateTime(a.performedAt),
            description: (
              <div>
                <div>{a.note}</div>
                {a.newRouteName && <div className="ir-muted">Tuyến mới: {a.newRouteName}</div>}
                {a.extraCost != null && <div className="ir-muted">Chi phí phát sinh dự kiến: {formatMoney(a.extraCost, a.currency ?? 'USD')}</div>}
              </div>
            ),
            meta: `${a.performedBy} · ${RoleLabels[a.performedByRole]}`,
            state: (a.actionType === 'DECISION' ? 'current' : 'done') as 'current' | 'done',
          })),
        ].reverse();
        return (
          <>
            <PageHeader
              title={i.incidentNumber}
              back={{ to: '/incidents', label: 'Danh sách sự cố' }}
              meta={<span className="app-row" style={{ gap: 6 }}><StatusBadge status={i.severity} labels={IncidentSeverityLabels} size="md" /><StatusBadge status={i.status} labels={IncidentStatusLabels} size="md" /></span>}
              subtitle={i.title}
              actions={
                <>
                  {has('ADD_ACTION') && <Button variant="outline" icon="bi-journal-plus" onClick={() => setDlg('action')}>Ghi nhận hành động</Button>}
                  {has('RECORD_VET_CONSULTATION') && <Button variant="outline" icon="bi-chat-left-text" onClick={() => setDlg('vet')}>Ghi nhận tư vấn thú y</Button>}
                  {has('DECIDE') && <Button variant="danger" icon="bi-signpost-split" onClick={() => setDlg('decide')}>Quyết định vận hành</Button>}
                  {has('CHANGE_STATUS') && NEXT_STATUS[i.status].length > 0 && <Button variant="outline" icon="bi-arrow-repeat" onClick={() => setDlg('status')}>Đổi trạng thái</Button>}
                  {has('CREATE_CLAIM') && <LinkButton to={`/claims/new?requestId=${i.requestId}&incidentId=${i.id}`} variant="outline" icon="bi-shield-exclamation">Tạo hồ sơ bồi thường</LinkButton>}
                </>
              }
            />
            {i.tripBlocked && <Alert tone="critical" title="Chuyến đang tạm dừng">Chuyến {i.tripNumber} chờ Quản lý Logistics ra quyết định vận hành.</Alert>}
            {i.resolutionNote && <Alert tone="success" title="Đã giải quyết">{i.resolutionNote}</Alert>}
            <div className="app-grid app-grid-main">
              <div className="ir-stack">
                <Card title="Thông tin sự cố" icon="bi-exclamation-triangle">
                  <DescriptionList items={[
                    { label: 'Loại sự cố', value: IncidentTypeLabels[i.incidentType] },
                    { label: 'Báo lúc', value: `${formatDateTime(i.reportedAt)} bởi ${i.reportedBy}` },
                    { label: 'Chuyến', value: <RefLink to={`/trips/${i.tripId}`}>{i.tripNumber}</RefLink> },
                    { label: 'Trạng thái chuyến', value: <StatusBadge status={i.tripStatus} labels={TripStatusLabels} /> },
                    { label: 'Tuyến', value: i.routeName },
                    { label: 'Vị trí', value: i.location ?? i.tripLocation },
                    { label: 'Khách hàng', value: i.customerName },
                    { label: 'Ngựa liên quan', value: i.horseId ? <RefLink to={`/horses/${i.horseId}`}>{i.horseName}</RefLink> : 'Không' },
                    { label: 'Quyết định hiện tại', value: i.decision ? <Badge tone="primary">{OperationalDecisionLabels[i.decision]}</Badge> : 'Chưa có' },
                    { label: 'Người quyết định', value: i.decidedBy ? `${i.decidedBy} · ${formatDateTime(i.decidedAt)}` : '—' },
                    { label: 'Mô tả', value: i.description, span: true },
                  ]} />
                </Card>
                <Card title="Lịch sử xử lý" icon="bi-clock-history">
                  {entries.length ? <Timeline items={entries} /> : <span className="ir-muted">Chưa có hành động xử lý.</span>}
                </Card>
              </div>
              <div className="ir-stack">
                <Card title={`Tư vấn thú y (${i.consultations.length})`} icon="bi-chat-left-text">
                  {i.consultations.length === 0 ? <span className="ir-muted">Chưa có tư vấn. Hệ thống không chẩn đoán; hãy ghi nhận ý kiến bác sĩ thú y bên ngoài.</span> : (
                    <div className="ir-stack" style={{ gap: 12 }}>
                      {i.consultations.map((c) => (
                        <div key={c.id}>
                          <div className="ir-strong">{c.veterinarianName}{c.clinic ? ` · ${c.clinic}` : ''}</div>
                          <div className="ir-muted">{formatDateTime(c.consultedAt)}{c.contact ? ` · ${c.contact}` : ''}</div>
                          <div>{c.advice}</div>
                          {c.recommendedAction && <Badge tone="info">Khuyến nghị: {OperationalDecisionLabels[c.recommendedAction]}</Badge>}
                          {c.reportFileId && <div><button type="button" className="ir-link" style={{ border: 0, background: 'none', padding: 0 }} onClick={() => void openFile(healthApi.reportPath(c.id))}>{c.reportFileName ?? 'Báo cáo thú y'}</button></div>}
                        </div>
                      ))}
                    </div>
                  )}
                </Card>
                <Card title="Tệp đính kèm" icon="bi-paperclip" actions={i.status !== 'CLOSED' && (has('ADD_ACTION') || has('DECIDE')) ? <Button size="sm" variant="outline" icon="bi-upload" onClick={() => setDlg('file')}>Tải lên</Button> : undefined}>
                  <AttachmentList items={i.attachments} />
                </Card>
              </div>
            </div>
            <ActionModal open={dlg === 'action'} inc={i} onClose={close} onDone={done} />
            <DecideModal open={dlg === 'decide'} inc={i} onClose={close} onDone={done} />
            <StatusModal open={dlg === 'status'} inc={i} onClose={close} onDone={done} />
            <VetConsultationModal open={dlg === 'vet'} onClose={close} onDone={done} incidentId={i.id} tripId={i.tripId} horseId={i.horseId} />
            <AttachmentUploadModal open={dlg === 'file'} refType="INCIDENT" refId={i.id} onClose={close} onDone={done} />
          </>
        );
      }}
    </AsyncView>
  );
}
