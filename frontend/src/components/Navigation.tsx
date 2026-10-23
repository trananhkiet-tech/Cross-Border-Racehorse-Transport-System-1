import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from './Icon';
import { formatDateTime } from '../utils/format';

export interface TabItem<K extends string = string> {
  key: K;
  label: ReactNode;
  count?: number;
  alert?: boolean;
  icon?: string;
}

export function Tabs<K extends string>({ tabs, active, onChange, variant = 'line' }: { tabs: TabItem<K>[]; active: K; onChange: (k: K) => void; variant?: 'line' | 'pill' }) {
  return (
    <div className={`ir-tabs ir-tabs-${variant}`} role="tablist" style={{ overflowX: 'auto' }}>
      {tabs.map((t) => (
        <button
          key={t.key}
          type="button"
          role="tab"
          aria-selected={t.key === active}
          className={`ir-tab${t.key === active ? ' is-active' : ''}`}
          onClick={() => onChange(t.key)}
          style={{ position: 'relative' }}
        >
          {t.icon && <Icon name={t.icon} />}
          {t.label}
          {t.count !== undefined && <span className={`ir-tab-count${t.alert ? ' tone-error' : ''}`}>{t.count}</span>}
          {t.alert && t.count === undefined && <span className="ir-tab-alert" />}
        </button>
      ))}
    </div>
  );
}

export interface StepItem {
  label: ReactNode;
  description?: ReactNode;
  state: 'done' | 'current' | 'upcoming' | 'error';
}

export function Stepper({ steps, vertical, wrap = true }: { steps: StepItem[]; vertical?: boolean; wrap?: boolean }) {
  return (
    <ol className={`ir-stepper${vertical ? ' ir-stepper-v' : ''}${wrap && !vertical ? ' ir-stepper-wrap' : ''}`}>
      {steps.map((s, i) => (
        <li key={i} className={`ir-step is-${s.state}`} aria-current={s.state === 'current' ? 'step' : undefined}>
          <span className="ir-step-dot">
            {s.state === 'done' ? <Icon name="bi-check-lg" size={14} /> : s.state === 'error' ? <Icon name="bi-x-lg" size={12} /> : i + 1}
          </span>
          <span className="ir-step-text">
            <span className="ir-step-label">{s.label}</span>
            {s.description && <span className="ir-step-desc">{s.description}</span>}
          </span>
          {i < steps.length - 1 && <span className="ir-step-line" />}
        </li>
      ))}
    </ol>
  );
}

export interface TimelineEntry {
  key: string | number;
  title: ReactNode;
  time?: string;
  description?: ReactNode;
  meta?: ReactNode;
  state?: 'done' | 'current' | 'upcoming' | 'error' | 'warning';
  icon?: string;
  badge?: ReactNode;
}

export function Timeline({ items, dense }: { items: TimelineEntry[]; dense?: boolean }) {
  return (
    <ol className={`ir-timeline${dense ? ' ir-timeline-dense' : ''}`}>
      {items.map((it) => (
        <li key={it.key} className={`ir-tl-item is-${it.state ?? 'done'}`}>
          <span className="ir-tl-marker">
            <Icon name={it.icon ?? (it.state === 'error' ? 'bi-exclamation' : it.state === 'warning' ? 'bi-exclamation' : it.state === 'upcoming' ? 'bi-circle' : 'bi-check')} size={12} />
          </span>
          <div className="ir-tl-body">
            <div className="ir-tl-head">
              <span className="ir-tl-title">{it.title}</span>
              {it.badge}
              {it.time && <span className="ir-tl-time">{formatDateTime(it.time)}</span>}
            </div>
            {it.description && <div className="ir-tl-desc app-pre">{it.description}</div>}
            {it.meta && <div className="ir-tl-meta">{it.meta}</div>}
          </div>
        </li>
      ))}
    </ol>
  );
}

/** Thanh breadcrumb trên topbar. */
export function Breadcrumb({ items }: { items: { label: string; to?: string }[] }) {
  return (
    <nav className="ir-breadcrumb" aria-label="Breadcrumb">
      <ol>
        {items.map((it, i) => (
          <li key={i}>
            {i > 0 && <Icon name="bi-chevron-right" className="ir-bc-sep" size={11} />}
            {it.to && i < items.length - 1 ? <Link to={it.to}>{it.label}</Link> : <span aria-current={i === items.length - 1 ? 'page' : undefined}>{it.label}</span>}
          </li>
        ))}
      </ol>
    </nav>
  );
}
