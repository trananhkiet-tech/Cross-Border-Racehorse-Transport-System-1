import type { ReactNode } from 'react';
import { iconOf, labelOf, toneOf, type Tone } from '../utils/status';
import { Icon } from './Icon';

/** Badge trạng thái thống nhất: cùng mã trạng thái → cùng màu ở mọi màn hình. */
export function StatusBadge({ status, labels, size = 'sm', variant = 'soft', title }: { status?: string | null; labels?: Record<string, string>; size?: 'sm' | 'md' | 'lg'; variant?: 'soft' | 'solid' | 'outline'; title?: string }) {
  if (!status) return <span className="ir-cell-empty">—</span>;
  const tone = toneOf(status);
  return (
    <span className={`ir-status tone-${tone} ir-status-${variant} ir-status-${size}`} title={title ?? status}>
      <Icon name={iconOf(tone)} size={11} />
      <span>{labelOf(status, labels)}</span>
    </span>
  );
}

export function Badge({ tone = 'neutral', icon, children, size = 'sm', variant = 'soft' }: { tone?: Tone; icon?: string; children: ReactNode; size?: 'sm' | 'md'; variant?: 'soft' | 'solid' | 'outline' }) {
  return (
    <span className={`ir-badge tone-${tone} ir-badge-${variant}${size === 'sm' ? ' ir-badge-sm' : ''}`}>
      {icon ? <Icon name={icon} size={11} /> : null}
      {children}
    </span>
  );
}

export function DemoTag({ children = 'Dữ liệu demo' }: { children?: ReactNode }) {
  return (
    <span className="app-demo-tag">
      <Icon name="bi-info-circle" /> {children}
    </span>
  );
}
