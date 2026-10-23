import { useRef, useState, type DragEvent } from 'react';
import { Icon } from './Icon';
import { formatBytes } from '../utils/format';

export const ALLOWED_TYPES = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];
export const MAX_FILE_BYTES = 10 * 1024 * 1024;

/** Kiểm tra phía client (backend vẫn kiểm tra lại magic bytes, dung lượng, tên tệp). */
export function validateFile(file: File, accept = ALLOWED_TYPES, maxBytes = MAX_FILE_BYTES): string | undefined {
  if (!accept.includes(file.type)) return 'Chỉ chấp nhận tệp PDF, JPG, PNG hoặc WEBP';
  if (file.size > maxBytes) return `Tệp vượt quá dung lượng cho phép (${formatBytes(maxBytes)})`;
  if (file.size === 0) return 'Tệp rỗng';
  return undefined;
}

/** Vùng kéo-thả / chọn tệp theo design system (ir-drop). */
export function FileDrop({ onFile, file, error, compact, title = 'Kéo thả tệp vào đây hoặc bấm để chọn', hint = 'PDF, JPG, PNG, WEBP – tối đa 10 MB', accept = ALLOWED_TYPES, disabled, onClear }: { onFile: (f: File | null, error?: string) => void; file?: File | null; error?: string; compact?: boolean; title?: string; hint?: string; accept?: string[]; disabled?: boolean; onClear?: () => void }) {
  const input = useRef<HTMLInputElement>(null);
  const [drag, setDrag] = useState(false);

  const handle = (f: File | undefined) => {
    if (!f) return;
    const err = validateFile(f, accept);
    onFile(err ? null : f, err);
  };

  const onDrop = (e: DragEvent) => {
    e.preventDefault();
    setDrag(false);
    if (!disabled) handle(e.dataTransfer.files?.[0]);
  };

  return (
    <div className="ir-stack" style={{ gap: 8 }}>
      <div
        className={`ir-drop${compact ? ' ir-drop-compact' : ''}${drag ? ' is-dragging' : ''}${error ? ' is-invalid' : ''}`}
        role="button"
        tabIndex={0}
        aria-disabled={disabled}
        onClick={() => !disabled && input.current?.click()}
        onKeyDown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && !disabled) { e.preventDefault(); input.current?.click(); } }}
        onDragOver={(e) => { e.preventDefault(); setDrag(true); }}
        onDragLeave={() => setDrag(false)}
        onDrop={onDrop}
      >
        <span className="ir-drop-icon">
          <Icon name="bi-cloud-arrow-up" size={18} />
        </span>
        <span className="ir-drop-text">
          <span className="ir-drop-title">{title}</span>
          <span className="ir-drop-hint">{hint}</span>
        </span>
        <input
          ref={input}
          type="file"
          className="app-file-input"
          accept={accept.join(',')}
          onChange={(e) => {
            handle(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
      </div>
      {error && (
        <span className="ir-field-error" role="alert">
          <Icon name="bi-exclamation-circle" size={12} /> {error}
        </span>
      )}
      {file && (
        <ul className="ir-files">
          <FileRow name={file.name} size={file.size} type={file.type} onRemove={onClear ?? (() => onFile(null))} />
        </ul>
      )}
    </div>
  );
}

export function FileRow({ name, size, type, meta, onOpen, onRemove }: { name: string; size?: number; type?: string; meta?: string; onOpen?: () => void; onRemove?: () => void }) {
  const icon = type?.startsWith('image/') ? 'bi-file-earmark-image' : 'bi-file-earmark-pdf';
  return (
    <li className="ir-file">
      <span className="ir-file-icon">
        <Icon name={icon} size={18} />
      </span>
      <div className="ir-file-body">
        <span className="ir-file-name">{name}</span>
        <span className="ir-file-meta">{meta ?? formatBytes(size)}</span>
      </div>
      {onOpen && (
        <button type="button" className="ir-iconbtn ir-iconbtn-ghost ir-iconbtn-sm" onClick={onOpen} aria-label={`Xem ${name}`} title="Xem tệp">
          <Icon name="bi-eye" />
        </button>
      )}
      {onRemove && (
        <button type="button" className="ir-iconbtn ir-iconbtn-ghost ir-iconbtn-sm" onClick={onRemove} aria-label={`Bỏ ${name}`} title="Bỏ tệp">
          <Icon name="bi-x-lg" />
        </button>
      )}
    </li>
  );
}
