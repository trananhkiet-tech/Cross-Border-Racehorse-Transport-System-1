import { useEffect, useState } from 'react';
import { documentApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { Button, FileDrop, Modal, SelectField, TextField, enumOptions } from '../../components';
import { DocumentTypeLabels, DocumentTypeValues, type DocumentType } from '../../types/enums';
import { COUNTRY_OPTIONS } from '../../utils/format';

export interface UploadTarget {
  requestId?: number;
  horseId?: number;
  checklistItemId?: number;
  documentType?: DocumentType;
  title?: string;
  horseName?: string;
}

/** Tải lên tài liệu (PDF/JPG/PNG/WEBP ≤ 10 MB) gắn với yêu cầu / ngựa / mục checklist. */
export function UploadDocumentModal({ open, target, onClose, onDone }: { open: boolean; target: UploadTarget; onClose: () => void; onDone: () => void }) {
  const { run, busy } = useAction();
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string>();
  const [type, setType] = useState<DocumentType>(target.documentType ?? 'OTHER');
  const [number, setNumber] = useState('');
  const [country, setCountry] = useState('');
  const [issueDate, setIssueDate] = useState('');
  const [expiryDate, setExpiryDate] = useState('');

  useEffect(() => {
    if (open) {
      setFile(null); setFileError(undefined); setType(target.documentType ?? 'OTHER');
      setNumber(''); setCountry(''); setIssueDate(''); setExpiryDate('');
    }
  }, [open, target.documentType, target.checklistItemId]);

  const dateError = issueDate && expiryDate && expiryDate < issueDate ? 'Ngày hết hạn phải sau ngày cấp' : undefined;

  const submit = async () => {
    if (!file) { setFileError('Vui lòng chọn tệp'); return; }
    if (dateError) return;
    await run(
      () => documentApi.upload({
        file, documentType: type, requestId: target.requestId, horseId: target.horseId, checklistItemId: target.checklistItemId,
        title: target.title, documentNumber: number || undefined, issuingCountry: country || undefined,
        issueDate: issueDate || undefined, expiryDate: expiryDate || undefined,
      }),
      'Đã tải tài liệu lên, đang chờ chuyên viên rà soát',
    );
    onDone();
    onClose();
  };

  return (
    <Modal
      open={open} onClose={onClose} onSubmit={submit} busy={busy} size="md" icon="bi-cloud-arrow-up"
      title="Tải lên tài liệu"
      description={[target.title, target.horseName].filter(Boolean).join(' – ') || 'Chọn tệp và nhập thông tin tài liệu'}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" icon="bi-upload" loading={busy}>Tải lên</Button></>}
    >
      <div className="ir-stack">
        <FileDrop file={file} error={fileError} onFile={(f, e) => { setFile(f); setFileError(e); }} />
        <SelectField label="Loại tài liệu" value={type} onChange={(v) => setType(v as DocumentType)} options={enumOptions(DocumentTypeValues, DocumentTypeLabels)} disabled={!!target.documentType} />
        <div className="app-grid app-grid-2">
          <TextField label="Số tài liệu" value={number} onChange={setNumber} optional />
          <SelectField label="Quốc gia cấp" value={country} onChange={setCountry} placeholder="—" options={COUNTRY_OPTIONS.map((c) => ({ value: c.code, label: c.name }))} />
          <TextField label="Ngày cấp" type="date" value={issueDate} onChange={setIssueDate} optional />
          <TextField label="Ngày hết hạn" type="date" value={expiryDate} onChange={setExpiryDate} error={dateError} optional />
        </div>
      </div>
    </Modal>
  );
}
