import { useState } from 'react';
import { attachmentApi } from '../../api/endpoints';
import { openFile } from '../../api/client';
import { useAction } from '../../hooks/useAction';
import { Button, FileDrop, FileRow, Modal, TextField } from '../../components';
import type { AttachmentRef } from '../../types/api';
import type { AttachmentRefType } from '../../types/enums';

/** Danh sách tệp đính kèm (ảnh / PDF) – mở bằng request có token. */
export function AttachmentList({ items }: { items: AttachmentRef[] }) {
  if (!items.length) return <span className="ir-muted">Chưa có tệp đính kèm.</span>;
  return (
    <div className="ir-stack" style={{ gap: 6 }}>
      {items.map((a) => (
        <FileRow key={a.id} name={a.fileName} type={a.contentType} onOpen={() => void openFile(attachmentApi.filePath(a.id))} />
      ))}
    </div>
  );
}

/** Hộp thoại tải tệp đính kèm cho một đối tượng (quan sát, sự cố, bàn giao…). */
export function AttachmentUploadModal({ open, refType, refId, title = 'Tải ảnh / tài liệu đính kèm', onClose, onDone }: { open: boolean; refType: AttachmentRefType; refId: number; title?: string; onClose: () => void; onDone: () => void }) {
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string>();
  const [caption, setCaption] = useState('');
  const { run, busy } = useAction();
  const close = () => { setFile(null); setError(undefined); setCaption(''); onClose(); };
  const submit = async () => {
    if (!file) { setError('Vui lòng chọn tệp'); return; }
    try {
      await run(() => attachmentApi.upload(refType, refId, file, caption.trim() || undefined), 'Đã tải tệp đính kèm');
      close();
      onDone();
    } catch { /* toast đã hiển thị */ }
  };
  return (
    <Modal open={open} title={title} icon="bi-paperclip" onClose={close} onSubmit={submit} busy={busy}
      footer={<><Button variant="outline" onClick={close} disabled={busy}>Hủy</Button><Button type="submit" loading={busy} icon="bi-upload">Tải lên</Button></>}>
      <div className="ir-stack">
        <FileDrop file={file} error={error} onFile={(f, e) => { setFile(f); setError(e); }} onClear={() => setFile(null)} />
        <TextField label="Chú thích" optional value={caption} onChange={setCaption} maxLength={200} />
      </div>
    </Modal>
  );
}
