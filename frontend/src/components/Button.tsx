import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from './Icon';

type Variant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'danger-outline' | 'success' | 'critical';
type Size = 'sm' | 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  icon?: string;
  iconRight?: string;
  loading?: boolean;
  block?: boolean;
  children?: ReactNode;
}

/** Nút theo design system (ir-btn). Khi loading: khóa nút và hiện spinner để tránh bấm lặp. */
export function Button({ variant = 'primary', size = 'md', icon, iconRight, loading, block, className = '', children, disabled, type = 'button', ...rest }: ButtonProps) {
  return (
    <button
      type={type}
      className={`ir-btn ir-btn-${variant} ir-btn-${size}${block ? ' ir-btn-block' : ''} ${className}`}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading ? <span className="spinner-border spinner-border-sm" aria-hidden="true" /> : icon ? <Icon name={icon} /> : null}
      {children !== undefined && <span className="ir-btn-label">{children}</span>}
      {iconRight && !loading ? <Icon name={iconRight} /> : null}
    </button>
  );
}

export function LinkButton({ to, variant = 'primary', size = 'md', icon, children, className = '' }: { to: string; variant?: Variant; size?: Size; icon?: string; children: ReactNode; className?: string }) {
  return (
    <Link to={to} className={`ir-btn ir-btn-${variant} ir-btn-${size} ${className}`}>
      {icon ? <Icon name={icon} /> : null}
      <span className="ir-btn-label">{children}</span>
    </Link>
  );
}

export function IconButton({ icon, label, variant = 'ghost', size = 'md', onClick, badge, disabled }: { icon: string; label: string; variant?: 'ghost' | 'outline' | 'surface' | 'inverse'; size?: Size; onClick?: () => void; badge?: number; disabled?: boolean }) {
  return (
    <button type="button" className={`ir-iconbtn ir-iconbtn-${variant} ir-iconbtn-${size}`} aria-label={label} title={label} onClick={onClick} disabled={disabled}>
      <Icon name={icon} size={18} />
      {badge ? <span className="ir-counter ir-iconbtn-badge">{badge > 99 ? '99+' : badge}</span> : null}
    </button>
  );
}
