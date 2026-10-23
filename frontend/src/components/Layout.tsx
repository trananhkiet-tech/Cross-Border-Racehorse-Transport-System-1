import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from './Icon';
import type { Tone } from '../utils/status';

export function PageHeader({ title, subtitle, actions, back, meta }: { title: ReactNode; subtitle?: ReactNode; actions?: ReactNode; back?: { to: string; label: string }; meta?: ReactNode }) {
  return (
    <div className="ir-pagehead">
      <div className="ir-pagehead-main">
        {back && (
          <Link to={back.to} className="ir-back app-text-sm d-inline-flex align-items-center gap-1 mb-1 text-decoration-none">
            <Icon name="bi-arrow-left" /> {back.label}
          </Link>
        )}
        <div className="ir-pagehead-title-row">
          <h1 className="ir-pagehead-title">{title}</h1>
          {meta}
        </div>
        {subtitle && <p className="ir-pagehead-sub">{subtitle}</p>}
      </div>
      {actions && <div className="ir-pagehead-actions">{actions}</div>}
    </div>
  );
}

export function Card({ title, subtitle, icon, actions, children, footer, flush, className = '', tone }: { title?: ReactNode; subtitle?: ReactNode; icon?: string; actions?: ReactNode; children?: ReactNode; footer?: ReactNode; flush?: boolean; className?: string; tone?: Tone }) {
  return (
    <section className={`ir-card${tone ? ` ir-card-toned tone-${tone}` : ''} ${className}`}>
      {(title || actions) && (
        <header className="ir-card-head">
          {icon && (
            <span className="ir-card-icon">
              <Icon name={icon} size={18} />
            </span>
          )}
          <div className="ir-card-titles">
            {title && <h3 className="ir-card-title">{title}</h3>}
            {subtitle && <p className="ir-card-sub">{subtitle}</p>}
          </div>
          {actions && <div className="ir-card-actions">{actions}</div>}
        </header>
      )}
      <div className={flush ? 'ir-card-flush' : 'ir-card-body'}>{children}</div>
      {footer && <footer className="ir-card-foot">{footer}</footer>}
    </section>
  );
}

export function KpiCard({ label, value, hint, icon = 'bi-bar-chart', tone = 'primary', to, unit, emphasized }: { label: string; value: ReactNode; hint?: ReactNode; icon?: string; tone?: string; to?: string; unit?: string; emphasized?: boolean }) {
  const content = (
    <>
      <div className="ir-kpi-head">
        <span className={`ir-kpi-icon tone-${tone}`}>
          <Icon name={icon} size={16} />
        </span>
        <span className="ir-kpi-label">{label}</span>
      </div>
      <div className="ir-kpi-row">
        <span className="ir-kpi-value">
          {value}
          {unit && <span className="ir-kpi-unit"> {unit}</span>}
        </span>
      </div>
      {hint && (
        <div className="ir-kpi-foot">
          <span className="ir-kpi-hint">{hint}</span>
        </div>
      )}
    </>
  );
  const cls = `ir-kpi${emphasized ? ` ir-kpi-emph tone-${tone}` : ''}${to ? ' ir-card-interactive' : ''}`;
  return to ? (
    <Link to={to} className={cls}>
      {content}
    </Link>
  ) : (
    <div className={cls}>{content}</div>
  );
}

export function Alert({ tone = 'info', title, children, actions, icon }: { tone?: Tone; title?: ReactNode; children?: ReactNode; actions?: ReactNode; icon?: string }) {
  const defaultIcon: Record<string, string> = {
    info: 'bi-info-circle', success: 'bi-check-circle', warning: 'bi-exclamation-triangle', error: 'bi-x-octagon',
    critical: 'bi-exclamation-octagon-fill', primary: 'bi-info-circle', neutral: 'bi-info-circle',
  };
  return (
    <div className={`ir-alert tone-${tone}`} role={tone === 'error' || tone === 'critical' ? 'alert' : 'status'}>
      <Icon name={icon ?? defaultIcon[tone]} className="ir-alert-icon" size={18} />
      <div className="ir-alert-body">
        {title && <div className="ir-alert-title">{title}</div>}
        {children && <div className="ir-alert-text">{children}</div>}
      </div>
      {actions && <div className="ir-alert-side">{actions}</div>}
    </div>
  );
}

export function DescriptionList({ items, columns = 2 }: { items: { label: ReactNode; value: ReactNode; span?: boolean }[]; columns?: number }) {
  return (
    <dl className="ir-dl" style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }}>
      {items.map((it, i) => (
        <div className="ir-dl-item" key={i} style={it.span ? { gridColumn: '1 / -1' } : undefined}>
          <dt>{it.label}</dt>
          <dd>{it.value === undefined || it.value === null || it.value === '' ? <span className="ir-cell-empty">—</span> : it.value}</dd>
        </div>
      ))}
    </dl>
  );
}

export function Progress({ value, max = 100, tone = 'primary', label, showValue = true, size = 'md' }: { value: number; max?: number; tone?: Tone; label?: ReactNode; showValue?: boolean; size?: 'sm' | 'md' | 'lg' }) {
  const pct = max <= 0 ? 0 : Math.max(0, Math.min(100, Math.round((value / max) * 100)));
  return (
    <div className="ir-progress-wrap">
      {(label || showValue) && (
        <div className="ir-progress-head">
          {label && <span className="ir-progress-label">{label}</span>}
          {showValue && <span className="ir-progress-value">{pct}%</span>}
        </div>
      )}
      <div className={`ir-progress ir-progress-${size} tone-${tone}`} role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
        <div className="ir-progress-fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

export function Avatar({ name, size = 'md' }: { name: string; size?: 'sm' | 'md' | 'lg' | 'xl' }) {
  const words = name.trim().split(/\s+/);
  const initials = ((words[0]?.[0] ?? '') + (words.length > 1 ? words[words.length - 1][0] : '')).toUpperCase();
  return (
    <span className={`ir-avatar ir-avatar-${size}`} title={name} aria-label={name} role="img">
      {initials}
    </span>
  );
}
