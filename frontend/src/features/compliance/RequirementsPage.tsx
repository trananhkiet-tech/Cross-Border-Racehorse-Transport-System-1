import { useState } from 'react';
import { complianceApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Badge, Button, Checkbox, DataTable, Modal, NumberField, PageHeader, SelectField, StatusBadge, TextAreaField, TextField, enumOptions, Cell } from '../../components';
import {
  ComplianceCategoryLabels, ComplianceCategoryValues, DocumentTypeLabels, DocumentTypeValues, MovementTypeLabels, MovementTypeValues,
  TransportPurposeLabels, TransportPurposeValues, type ComplianceCategory, type DocumentType, type MovementType, type TransportPurpose,
} from '../../types/enums';
import type { Requirement, RequirementPayload } from '../../types/api';
import { COUNTRY_OPTIONS, countryName, formatDate, isoDatePlus } from '../../utils/format';

const COUNTRY_OPTS = COUNTRY_OPTIONS.map((c) => ({ value: c.code, label: c.name }));

function blank(): RequirementPayload {
  return { code: '', category: 'HEALTH', documentType: 'HEALTH_CERTIFICATE', title: '', effectiveFrom: isoDatePlus(0), perHorse: true, mandatory: true, active: true };
}

function RequirementModal({ initial, id, onClose, onSaved }: { initial: RequirementPayload; id?: number; onClose: () => void; onSaved: () => void }) {
  const { run, busy, fieldErrors } = useAction();
  const [f, setF] = useState<RequirementPayload>(initial);
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const set = <K extends keyof RequirementPayload>(k: K, v: RequirementPayload[K]) => setF((x) => ({ ...x, [k]: v }));
  const submit = async () => {
    const errs = {
      code: !f.code.trim() ? 'Nhập mã quy định' : undefined,
      title: !f.title.trim() ? 'Nhập tiêu đề' : undefined,
      effectiveTo: f.effectiveTo && f.effectiveTo < f.effectiveFrom ? 'Ngày hết hiệu lực phải sau ngày bắt đầu' : undefined,
    };
    setErrors(errs);
    if (Object.values(errs).some(Boolean)) return;
    const payload = { ...f, originCountry: f.originCountry || undefined, destinationCountry: f.destinationCountry || undefined, movementType: f.movementType || undefined, purpose: f.purpose || undefined, effectiveTo: f.effectiveTo || undefined };
    await run(() => (id ? complianceApi.updateRequirement(id, payload) : complianceApi.createRequirement(payload)), 'Đã lưu quy định');
    onSaved();
    onClose();
  };
  const e = { ...fieldErrors, ...errors };
  return (
    <Modal open onClose={onClose} onSubmit={submit} busy={busy} size="lg" icon="bi-journal-text" title={id ? 'Sửa quy định' : 'Thêm quy định'}
      description="Quy định dùng để sinh checklist theo tuyến. Dữ liệu demo – cần đối chiếu quy định chính thức."
      footer={<><Button variant="outline" onClick={onClose}>Hủy</Button><Button type="submit" loading={busy}>Lưu</Button></>}>
      <div className="ir-stack">
        <div className="app-grid app-grid-2">
          <TextField label="Mã quy định" value={f.code} onChange={(v) => set('code', v)} error={e.code} required />
          <TextField label="Tiêu đề giấy tờ" value={f.title} onChange={(v) => set('title', v)} error={e.title} required />
          <SelectField label="Nhóm" value={f.category} onChange={(v) => set('category', v as ComplianceCategory)} options={enumOptions(ComplianceCategoryValues, ComplianceCategoryLabels)} />
          <SelectField label="Loại tài liệu" value={f.documentType} onChange={(v) => set('documentType', v as DocumentType)} options={enumOptions(DocumentTypeValues, DocumentTypeLabels)} />
          <SelectField label="Quốc gia đi" value={f.originCountry} onChange={(v) => set('originCountry', v || undefined)} placeholder="Mọi quốc gia" options={COUNTRY_OPTS} />
          <SelectField label="Quốc gia đến" value={f.destinationCountry} onChange={(v) => set('destinationCountry', v || undefined)} placeholder="Mọi quốc gia" options={COUNTRY_OPTS} />
          <SelectField label="Loại di chuyển" value={f.movementType} onChange={(v) => set('movementType', (v || undefined) as MovementType | undefined)} placeholder="Mọi loại" options={enumOptions(MovementTypeValues, MovementTypeLabels)} />
          <SelectField label="Mục đích" value={f.purpose} onChange={(v) => set('purpose', (v || undefined) as TransportPurpose | undefined)} placeholder="Mọi mục đích" options={enumOptions(TransportPurposeValues, TransportPurposeLabels)} />
          <NumberField label="Hiệu lực tài liệu" suffix="ngày" value={f.validityDays} onChange={(v) => set('validityDays', v)} min={1} help="Để trống nếu không giới hạn" />
          <div />
          <TextField label="Có hiệu lực từ" type="date" value={f.effectiveFrom} onChange={(v) => set('effectiveFrom', v)} required />
          <TextField label="Hết hiệu lực" type="date" value={f.effectiveTo} onChange={(v) => set('effectiveTo', v || undefined)} error={e.effectiveTo} optional />
        </div>
        <TextAreaField label="Mô tả" value={f.description} onChange={(v) => set('description', v)} rows={2} optional />
        <div className="app-row" style={{ gap: 24 }}>
          <Checkbox label="Áp dụng cho từng ngựa" checked={!!f.perHorse} onChange={(v) => set('perHorse', v)} />
          <Checkbox label="Bắt buộc" checked={!!f.mandatory} onChange={(v) => set('mandatory', v)} />
          <Checkbox label="Đang áp dụng" checked={f.active !== false} onChange={(v) => set('active', v)} />
        </div>
      </div>
    </Modal>
  );
}

/** Quy định nhập khẩu / kiểm dịch / hải quan / xét nghiệm / tiêm chủng. Chỉ Specialist được sửa. */
export function RequirementsPage() {
  const user = useCurrentUser();
  const canEdit = user.role === 'TRANSPORT_SPECIALIST';
  const [cat, setCat] = useState<ComplianceCategory | ''>('');
  const [dest, setDest] = useState('');
  const [inactive, setInactive] = useState(false);
  const state = useAsync(() => complianceApi.requirements({ category: cat ? [cat] : [], destination: dest || undefined, includeInactive: inactive }), [cat, dest, inactive]);
  const [editing, setEditing] = useState<{ id?: number; data: RequirementPayload } | null>(null);

  return (
    <>
      <PageHeader title="Quy định nhập khẩu & tuân thủ" subtitle="Kiểm dịch, hải quan, xét nghiệm, tiêm chủng và giấy tờ theo tuyến. Dữ liệu minh họa – không thay thế quy định chính thức."
        actions={canEdit && <Button icon="bi-plus-lg" onClick={() => setEditing({ data: blank() })}>Thêm quy định</Button>} />
      <DataTable<Requirement>
        rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(r) => r.id} emptyTitle="Chưa có quy định phù hợp" emptyIcon="bi-journal"
        toolbar={<>
          <div style={{ width: 220 }}><SelectField size="sm" value={cat} onChange={(v) => setCat(v as ComplianceCategory | '')} placeholder="Mọi nhóm" options={enumOptions(ComplianceCategoryValues, ComplianceCategoryLabels)} /></div>
          <div style={{ width: 200 }}><SelectField size="sm" value={dest} onChange={setDest} placeholder="Mọi quốc gia đến" options={COUNTRY_OPTS} /></div>
          <Checkbox label="Hiện cả quy định đã ngừng áp dụng" checked={inactive} onChange={setInactive} />
        </>}
        columns={[
          { key: 'code', header: 'Quy định', sortValue: (r) => r.code, render: (r) => <Cell main={r.title} sub={r.code} /> },
          { key: 'cat', header: 'Nhóm', render: (r) => <Cell main={ComplianceCategoryLabels[r.category]} sub={DocumentTypeLabels[r.documentType]} /> },
          { key: 'route', header: 'Áp dụng cho', render: (r) => <Cell main={`${r.originCountry ? countryName(r.originCountry) : 'Mọi nơi'} → ${r.destinationCountry ? countryName(r.destinationCountry) : 'Mọi nơi'}`} sub={[r.movementType && MovementTypeLabels[r.movementType], r.purpose && TransportPurposeLabels[r.purpose]].filter(Boolean).join(' · ') || undefined} /> },
          { key: 'flags', header: 'Thuộc tính', render: (r) => <span className="app-row" style={{ gap: 4 }}>{r.mandatory ? <Badge tone="error">Bắt buộc</Badge> : <Badge>Tùy chọn</Badge>}{r.perHorse && <Badge tone="primary">Từng ngựa</Badge>}{r.validityDays ? <Badge tone="info">{r.validityDays} ngày</Badge> : null}</span> },
          { key: 'eff', header: 'Hiệu lực', render: (r) => `${formatDate(r.effectiveFrom)}${r.effectiveTo ? ` – ${formatDate(r.effectiveTo)}` : ''}` },
          { key: 'st', header: 'Trạng thái', render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} labels={{ ACTIVE: 'Đang áp dụng', INACTIVE: 'Ngừng áp dụng' }} /> },
          ...(canEdit ? [{ key: 'act', header: '', render: (r: Requirement) => <Button size="sm" variant="ghost" icon="bi-pencil" onClick={() => setEditing({ id: r.id, data: { ...r } })}>Sửa</Button> }] : []),
        ]}
      />
      {editing && <RequirementModal id={editing.id} initial={editing.data} onClose={() => setEditing(null)} onSaved={state.reload} />}
    </>
  );
}
