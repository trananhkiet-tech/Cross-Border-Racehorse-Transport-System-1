import { useEffect, useId, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { Button } from './Button';
import { IconButton } from './Button';
import { Icon } from './Icon';
import { TextAreaField } from './Form';
import type { Tone } from '../utils/status';

interface ModalProps {
  open: boolean;
  title: ReactNode;
  description?: ReactNode;
  icon?: string;
  tone?: Tone;
  size?: 'sm' | 'md' | 'lg';
  onClose: () => void;
  children?: ReactNode;
  footer?: ReactNode;
  /** Nếu có: bọc nội dung trong <form> và gọi khi submit (Enter). */
  onSubmit?: () => void;
  busy?: boolean;
}

/** Hộp thoại: đóng bằng Esc / nút X / click nền (trừ khi đang xử lý), focus vào hộp thoại khi mở. */
export function Modal({ open, title, description, icon, tone = 'primary', size = 'md', onClose, children, footer, onSubmit, busy }: ModalProps) {
  const titleId = useId();
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !busy) onClose();
    };
    document.addEventListener('keydown', onKey);
    const prev = document.activeElement as HTMLElement | null;
    const first = ref.current?.querySelector<HTMLElement>('input, select, textarea, button:not(.ir-modal-close button)');
    (first ?? ref.current)?.focus();
    return () => {
      document.removeEventListener('keydown', onKey);
      prev?.focus?.();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  if (!open) return null;

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!busy) onSubmit?.();
  };

  const inner = (
    <>
      <div className="ir-modal-head">
        {icon && (
          <span className={`ir-modal-icon tone-${tone}`}>
            <Icon name={icon} size={20} />
          </span>
        )}
        <div className="ir-modal-titles">
          <h2 className="ir-modal-title" id={titleId}>{title}</h2>
          {description && <p className="ir-modal-desc">{description}</p>}
        </div>
        <span className="ir-modal-close">
          <IconButton icon="bi-x-lg" label="Đóng" onClick={onClose} disabled={busy} />
        </span>
      </div>
      {children && <div className="ir-modal-body">{children}</div>}
      {footer && <div className="ir-modal-foot">{footer}</div>}
    </>
  );

  return createPortal(
    <div className="ir-overlay" onMouseDown={(e) => { if (e.target === e.currentTarget && !busy) onClose(); }}>
      <div ref={ref} className={`ir-modal ir-modal-${size}`} role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1}>
        {onSubmit ? (
          <form onSubmit={handleSubmit} noValidate style={{ display: 'contents' }}>
            {inner}
          </form>
        ) : (
          inner
        )}
      </div>
    </div>,
    document.body,
  );
}

interface ConfirmProps {
  open: boolean;
  title: ReactNode;
  message?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: Tone;
  icon?: string;
  /** Bắt buộc nhập lý do (từ chối, hủy…). */
  requireReason?: boolean;
  reasonLabel?: string;
  reasonPlaceholder?: string;
  /** Cho phép nhập ghi chú tùy chọn. */
  allowNote?: boolean;
  onConfirm: (reason: string) => Promise<unknown> | void;
  onClose: () => void;
  children?: ReactNode;
}

/** Hộp thoại xác nhận cho thao tác quan trọng; tự quản lý trạng thái đang xử lý. */
export function ConfirmDialog({
  open, title, message, confirmLabel = 'Xác nhận', cancelLabel = 'Hủy', tone = 'primary', icon, requireReason,
  reasonLabel = 'Lý do', reasonPlaceholder, allowNote, onConfirm, onClose, children,
}: ConfirmProps) {
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [touched, setTouched] = useState(false);

  useEffect(() => {
    if (open) {
      setReason('');
      setTouched(false);
      setBusy(false);
    }
  }, [open]);

  const invalid = requireReason && reason.trim().length < 3;

  const submit = async () => {
    setTouched(true);
    if (invalid) return;
    setBusy(true);
    try {
      await onConfirm(reason.trim());
      onClose();
    } catch {
      /* lỗi đã được hiển thị qua toast ở nơi gọi */
    } finally {
      setBusy(false);
    }
  };

  const variant = tone === 'error' || tone === 'critical' ? 'danger' : tone === 'success' ? 'success' : 'primary';
  const defaultIcon = tone === 'error' || tone === 'critical' ? 'bi-exclamation-triangle' : tone === 'success' ? 'bi-check-circle' : 'bi-question-circle';

  return (
    <Modal
      open={open}
      title={title}
      description={message}
      icon={icon ?? defaultIcon}
      tone={tone}
      size="sm"
      onClose={onClose}
      onSubmit={submit}
      busy={busy}
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={busy}>
            {cancelLabel}
          </Button>
          <Button type="submit" variant={variant} loading={busy}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      {children}
      {(requireReason || allowNote) && (
        <TextAreaField
          label={reasonLabel}
          required={requireReason}
          value={reason}
          onChange={setReason}
          placeholder={reasonPlaceholder}
          error={touched && invalid ? 'Vui lòng nhập lý do (tối thiểu 3 ký tự)' : undefined}
          rows={3}
        />
      )}
    </Modal>
  );
}
