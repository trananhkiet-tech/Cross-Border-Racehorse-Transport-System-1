import { useState } from 'react';
import { openFile } from '../../api/client';
import { complianceApi, documentApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { Alert, Badge, Button, ConfirmDialog, Icon, Progress, StatusBadge } from '../../components';
import { ChecklistItemStatusLabels, ComplianceCategoryLabels, DocumentTypeLabels } from '../../types/enums';
import type { Checklist, ChecklistItem } from '../../types/api';
import { formatDate } from '../../utils/format';
import { UploadDocumentModal, type UploadTarget } from '../documents/UploadDocumentModal';
import { Modal } from '../../components';
import { SelectField } from '../../components';
import { TextAreaField } from '../../components';
import { ChecklistItemStatusValues, type ChecklistItemStatus } from '../../types/enums';

const TONE_BY_STATUS: Record<ChecklistItemStatus, string> = {
  MISSING: 'warning', SUBMITTED: 'info', APPROVED: 'success', REJECTED: 'error', NEEDS_UPDATE: 'warning', EXPIRED: 'error', WAIVED: 'info',
};
const ICON_BY_STATUS: Record<ChecklistItemStatus, string> = {
  MISSING: 'bi-circle', SUBMITTED: 'bi-hourglass-split', APPROVED: 'bi-check-circle-fill', REJECTED: 'bi-x-circle-fill',
  NEEDS_UPDATE: 'bi-exclamation-circle-fill', EXPIRED: 'bi-calendar-x-fill', WAIVED: 'bi-dash-circle',
};

/** Checklist tuân thủ của một yêu cầu: khách hàng tải tài liệu; chuyên viên cập nhật trạng thái mục. */
export function ChecklistPanel({ checklist, canUpload, onChanged }: { checklist: Checklist; canUpload: boolean; onChanged: () => void }) {
  const user = useCurrentUser();
  const isSpecialist = user.role === 'TRANSPORT_SPECIALIST';
  const { run, busy } = useAction();
  const [upload, setUpload] = useState<UploadTarget | null>(null);
  const [edit, setEdit] = useState<ChecklistItem | null>(null);
  const [editStatus, setEditStatus] = useState<ChecklistItemStatus>('APPROVED');
  const [editNote, setEditNote] = useState('');
  const [notifyOpen, setNotifyOpen] = useState(false);
  const s = checklist.summary;
  const grouped = checklist.items.reduce<Record<string, ChecklistItem[]>>((acc, it) => {
    (acc[it.category] ??= []).push(it);
    return acc;
  }, {});

  const openEdit = (it: ChecklistItem) => {
    setEdit(it);
    setEditStatus(it.status === 'MISSING' ? 'WAIVED' : it.status);
    setEditNote(it.reviewerNote ?? '');
  };

  const saveEdit = async () => {
    if (!edit) return;
    await run(() => complianceApi.patchItem(edit.id, editStatus, editNote || undefined, edit.documentId), 'Đã cập nhật mục checklist');
    setEdit(null);
    onChanged();
  };

  return (
    <div className="ir-stack">
      <Alert tone="info" title="Lưu ý">{checklist.disclaimer}</Alert>
      <div className="app-grid app-grid-main">
        <Progress value={s.approved} max={Math.max(s.total, 1)} tone={s.complete ? 'success' : 'primary'} label={`${s.approved}/${s.total} mục đã được duyệt`} />
        <div className="app-row">
          <Badge tone="warning">Thiếu {s.missing}</Badge>
          <Badge tone="info">Chờ duyệt {s.pending}</Badge>
          <Badge tone={s.problems ? 'error' : 'neutral'}>Có vấn đề {s.problems}</Badge>
          {isSpecialist && s.missing + s.problems > 0 && (
            <Button size="sm" variant="outline" icon="bi-bell" onClick={() => setNotifyOpen(true)}>Nhắc khách hàng</Button>
          )}
        </div>
      </div>
      {Object.entries(grouped).map(([cat, items]) => (
        <section key={cat} className="ir-checklist">
          <div className="ir-checklist-head">
            <span className="ir-checklist-title">{ComplianceCategoryLabels[cat as keyof typeof ComplianceCategoryLabels] ?? cat}</span>
            <span className="ir-checklist-count">{items.filter((i) => i.status === 'APPROVED' || i.status === 'WAIVED').length}/{items.length}</span>
          </div>
          <ul className="ir-checklist-list">
            {items.map((it) => {
              const tone = TONE_BY_STATUS[it.status];
              const open = ['MISSING', 'REJECTED', 'NEEDS_UPDATE', 'EXPIRED'].includes(it.status);
              return (
                <li key={it.id} className={`ir-check-item tone-${tone}${open ? ' is-open' : ''}`}>
                  <Icon name={ICON_BY_STATUS[it.status]} className="ir-check-item-icon" size={18} />
                  <div className="ir-check-item-body">
                    <div className="ir-check-item-label">
                      {it.title} {!it.mandatory && <Badge>Tùy chọn</Badge>}
                      {it.horseName && <Badge tone="primary" icon="bi-postcard-heart">{it.horseName}</Badge>}
                    </div>
                    <div className="ir-check-item-meta">
                      {DocumentTypeLabels[it.documentType]}
                      {it.documentFileName && <> · <button type="button" className="ir-link" style={{ border: 0, background: 'none', padding: 0 }} onClick={() => it.documentId && void openFile(documentApi.filePath(it.documentId))}>{it.documentFileName}</button></>}
                      {it.documentExpiryDate && ` · hết hạn ${formatDate(it.documentExpiryDate)}`}
                      {it.reviewerNote && ` · Ghi chú: ${it.reviewerNote}`}
                    </div>
                  </div>
                  <StatusBadge status={it.status} labels={ChecklistItemStatusLabels} />
                  {canUpload && ['MISSING', 'REJECTED', 'NEEDS_UPDATE', 'EXPIRED'].includes(it.status) && (
                    <Button size="sm" variant="outline" icon="bi-upload" onClick={() => setUpload({ requestId: checklist.requestId, horseId: it.horseId, checklistItemId: it.id, documentType: it.documentType, title: it.title, horseName: it.horseName })}>
                      {it.status === 'MISSING' ? 'Tải lên' : 'Tải lại'}
                    </Button>
                  )}
                  {isSpecialist && (
                    <Button size="sm" variant="ghost" icon="bi-pencil" onClick={() => openEdit(it)} aria-label={`Cập nhật ${it.title}`}>Cập nhật</Button>
                  )}
                </li>
              );
            })}
          </ul>
        </section>
      ))}

      {upload && <UploadDocumentModal open target={upload} onClose={() => setUpload(null)} onDone={onChanged} />}

      <Modal
        open={!!edit} onClose={() => setEdit(null)} onSubmit={saveEdit} busy={busy} size="sm" icon="bi-pencil-square"
        title="Cập nhật mục checklist" description={edit?.title}
        footer={<><Button variant="outline" onClick={() => setEdit(null)}>Hủy</Button><Button type="submit" loading={busy}>Lưu</Button></>}
      >
        <div className="ir-stack">
          <SelectField label="Trạng thái" value={editStatus} onChange={(v) => setEditStatus(v as ChecklistItemStatus)}
            options={ChecklistItemStatusValues.filter((v) => v !== 'MISSING').map((v) => ({ value: v, label: ChecklistItemStatusLabels[v] }))}
            help="Miễn trừ (WAIVED) chỉ dùng khi quy định cho phép bỏ qua mục này." />
          <TextAreaField label="Ghi chú" value={editNote} onChange={setEditNote} rows={3} optional />
        </div>
      </Modal>

      <ConfirmDialog
        open={notifyOpen} onClose={() => setNotifyOpen(false)} title="Nhắc khách hàng bổ sung hồ sơ?"
        message="Hệ thống gửi thông báo liệt kê các mục còn thiếu hoặc cần cập nhật." confirmLabel="Gửi nhắc"
        onConfirm={async () => { await run(() => complianceApi.notifyMissing(checklist.requestId), 'Đã gửi nhắc khách hàng'); }}
      />
      {busy && <span className="ir-sr">Đang xử lý</span>}
    </div>
  );
}
