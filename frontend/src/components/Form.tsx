import { useId, type ReactNode, type InputHTMLAttributes } from 'react';
import { Icon } from './Icon';

interface FieldShellProps {
  label?: ReactNode;
  required?: boolean;
  optional?: boolean;
  help?: ReactNode;
  error?: string;
  children: ReactNode;
  htmlFor?: string;
  className?: string;
}

/** Khung trường nhập: nhãn, dấu bắt buộc, gợi ý và thông báo lỗi (aria-describedby). */
export function Field({ label, required, optional, help, error, children, htmlFor, className = '' }: FieldShellProps) {
  return (
    <div className={`ir-field ${className}`}>
      {label && (
        <label className="ir-field-label" htmlFor={htmlFor}>
          {label}
          {required && <span className="ir-req"> *</span>}
          {optional && <span className="ir-optional"> (không bắt buộc)</span>}
        </label>
      )}
      {children}
      {error ? (
        <span className="ir-field-error" id={htmlFor ? `${htmlFor}-err` : undefined} role="alert">
          <Icon name="bi-exclamation-circle" size={12} />
          {error}
        </span>
      ) : help ? (
        <span className="ir-field-help">{help}</span>
      ) : null}
    </div>
  );
}

type BaseInput = Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange' | 'value' | 'size'>;

interface TextFieldProps extends BaseInput {
  label?: ReactNode;
  value: string | number | undefined | null;
  onChange: (v: string) => void;
  error?: string;
  help?: ReactNode;
  optional?: boolean;
  icon?: string;
  suffix?: ReactNode;
  size?: 'sm' | 'md' | 'lg';
}

export function TextField({ label, value, onChange, error, help, optional, icon, suffix, size = 'md', required, id, className, ...rest }: TextFieldProps) {
  const autoId = useId();
  const fid = id ?? autoId;
  return (
    <Field label={label} required={required} optional={optional} help={help} error={error} htmlFor={fid} className={className}>
      <div className={`ir-input ir-input-${size}${error ? ' is-invalid' : ''}${rest.disabled ? ' is-disabled' : ''}${rest.readOnly ? ' is-readonly' : ''}`}>
        {icon && <Icon name={icon} className="ir-input-affix" />}
        <input
          id={fid}
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${fid}-err` : undefined}
          required={required}
          {...rest}
        />
        {suffix && <span className="ir-input-suffix">{suffix}</span>}
      </div>
    </Field>
  );
}

export function PasswordField(props: Omit<TextFieldProps, 'type'>) {
  return <TextField {...props} type="password" icon={props.icon ?? 'bi-lock'} />;
}

export function NumberField({ value, onChange, ...rest }: Omit<TextFieldProps, 'type' | 'value' | 'onChange'> & { value: number | undefined | null | ''; onChange: (v: number | undefined) => void }) {
  return (
    <TextField
      {...rest}
      type="number"
      inputMode="decimal"
      value={value === undefined || value === null ? '' : value}
      onChange={(v) => onChange(v === '' ? undefined : Number(v))}
    />
  );
}

export interface Option {
  value: string | number;
  label: string;
  disabled?: boolean;
}

/** Tạo danh sách option từ Values + Labels của enum được sinh tự động. */
export function enumOptions<T extends string>(values: T[], labels: Record<T, string>): Option[] {
  return values.map((v) => ({ value: v, label: labels[v] }));
}

export function SelectField({ label, value, onChange, options, placeholder, error, help, required, optional, disabled, id, className, size = 'md' }: { label?: ReactNode; value: string | number | undefined | null; onChange: (v: string) => void; options: Option[]; placeholder?: string; error?: string; help?: ReactNode; required?: boolean; optional?: boolean; disabled?: boolean; id?: string; className?: string; size?: 'sm' | 'md' | 'lg' }) {
  const autoId = useId();
  const fid = id ?? autoId;
  return (
    <Field label={label} required={required} optional={optional} help={help} error={error} htmlFor={fid} className={className}>
      <div className={`ir-input ir-input-${size} ir-select${error ? ' is-invalid' : ''}${disabled ? ' is-disabled' : ''}`}>
        <select id={fid} value={value ?? ''} onChange={(e) => onChange(e.target.value)} disabled={disabled} required={required} aria-invalid={error ? true : undefined}>
          {placeholder !== undefined && <option value="">{placeholder}</option>}
          {options.map((o) => (
            <option key={o.value} value={o.value} disabled={o.disabled}>
              {o.label}
            </option>
          ))}
        </select>
        <Icon name="bi-chevron-down" className="ir-select-caret" size={12} />
      </div>
    </Field>
  );
}

export function TextAreaField({ label, value, onChange, error, help, required, optional, rows = 4, placeholder, maxLength, id, disabled, className }: { label?: ReactNode; value: string | undefined | null; onChange: (v: string) => void; error?: string; help?: ReactNode; required?: boolean; optional?: boolean; rows?: number; placeholder?: string; maxLength?: number; id?: string; disabled?: boolean; className?: string }) {
  const autoId = useId();
  const fid = id ?? autoId;
  return (
    <Field
      label={label}
      required={required}
      optional={optional}
      error={error}
      htmlFor={fid}
      className={className}
      help={help ?? (maxLength ? <span className="ir-count">{(value ?? '').length}/{maxLength}</span> : undefined)}
    >
      <textarea
        id={fid}
        className={`ir-textarea${error ? ' is-invalid' : ''}`}
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value)}
        rows={rows}
        placeholder={placeholder}
        maxLength={maxLength}
        disabled={disabled}
        aria-invalid={error ? true : undefined}
      />
    </Field>
  );
}

export function Checkbox({ label, description, checked, onChange, disabled, error }: { label: ReactNode; description?: ReactNode; checked: boolean; onChange: (v: boolean) => void; disabled?: boolean; error?: string }) {
  const id = useId();
  return (
    <div className={`ir-choice${disabled ? ' is-disabled' : ''}${error ? ' is-invalid' : ''}`}>
      <input id={id} type="checkbox" className="ir-sr" checked={checked} onChange={(e) => onChange(e.target.checked)} disabled={disabled} />
      <label htmlFor={id} className="ir-choice-label">
        <span className={`ir-check-box${checked ? ' is-on' : ''}`} aria-hidden="true">
          {checked && <Icon name="bi-check-lg" size={12} />}
        </span>
        <span className="ir-choice-text">
          <span>{label}</span>
          {description && <span className="ir-choice-desc">{description}</span>}
          {error && <span className="ir-field-error">{error}</span>}
        </span>
      </label>
    </div>
  );
}

export function Toggle({ label, checked, onChange, disabled }: { label: ReactNode; checked: boolean; onChange: (v: boolean) => void; disabled?: boolean }) {
  const id = useId();
  return (
    <div className="app-row ir-toggle-row">
      <button id={id} type="button" role="switch" aria-checked={checked} className={`ir-toggle${checked ? ' is-on' : ''}`} onClick={() => onChange(!checked)} disabled={disabled}>
        <span className="ir-toggle-knob" />
      </button>
      <label htmlFor={id}>{label}</label>
    </div>
  );
}

/** Nhóm nút chọn một (segmented control). */
export function Segmented<T extends string>({ value, onChange, options, block, size = 'md', label }: { value: T; onChange: (v: T) => void; options: { value: T; label: ReactNode; icon?: string; tone?: string }[]; block?: boolean; size?: 'sm' | 'md' | 'lg'; label?: string }) {
  return (
    <div className={`ir-seg ir-seg-${size}${block ? ' ir-seg-block' : ''}`} role="radiogroup" aria-label={label}>
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          role="radio"
          aria-checked={value === o.value}
          className={`ir-seg-item${value === o.value ? ` is-on${o.tone ? ` tone-${o.tone}` : ''}` : ''}`}
          onClick={() => onChange(o.value)}
        >
          {o.icon && <Icon name={o.icon} />}
          {o.label}
        </button>
      ))}
    </div>
  );
}

/** Thẻ chọn (radio card) – dùng cho chọn loại chuồng, phương thức thanh toán… */
export function ChoiceCard({ name, checked, onChange, title, description, extra }: { name: string; checked: boolean; onChange: () => void; title: ReactNode; description?: ReactNode; extra?: ReactNode }) {
  const id = useId();
  return (
    <div className={`ir-choice ir-choice-card${checked ? ' is-checked' : ''}`}>
      <input id={id} type="radio" name={name} className="ir-sr" checked={checked} onChange={onChange} />
      <label htmlFor={id} className="ir-choice-label">
        <span className="ir-radio-dot" aria-hidden="true" />
        <span className="ir-choice-text" style={{ flex: 1 }}>
          <span className="ir-strong">{title}</span>
          {description && <span className="ir-choice-desc">{description}</span>}
        </span>
        {extra}
      </label>
    </div>
  );
}
