import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../app/AuthContext';
import { Icon } from '../../components';
import { canAccess } from '../../routes/access';
import { countryName, formatMoney } from '../../utils/format';

/** "TP.HCM, Việt Nam → Bangkok, Thái Lan" */
export function RouteText({ originCountry, originAddress, destinationCountry, destinationAddress, compact }: { originCountry: string; originAddress?: string; destinationCountry: string; destinationAddress?: string; compact?: boolean }) {
  if (compact) {
    return (
      <span>
        {countryName(originCountry)} <Icon name="bi-arrow-right" size={11} /> {countryName(destinationCountry)}
      </span>
    );
  }
  return (
    <span>
      {originAddress ? `${originAddress}, ` : ''}
      {countryName(originCountry)} <Icon name="bi-arrow-right" size={11} /> {destinationAddress ? `${destinationAddress}, ` : ''}
      {countryName(destinationCountry)}
    </span>
  );
}

export function Money({ value, currency = 'USD', strong }: { value?: number | null; currency?: string; strong?: boolean }) {
  const s = formatMoney(value, currency);
  return strong ? <span className="ir-strong ir-tnum">{s}</span> : <span className="ir-tnum">{s}</span>;
}

/** Liên kết tham chiếu; nếu role hiện tại không được vào trang đích thì chỉ hiển thị chữ (tránh dẫn tới trang 403). */
export function RefLink({ to, children }: { to: string; children: ReactNode }) {
  const { user } = useAuth();
  if (!canAccess(to, user?.role)) return <span className="ir-mono">{children}</span>;
  return (
    <Link to={to} className="ir-link ir-mono" onClick={(e) => e.stopPropagation()}>
      {children}
    </Link>
  );
}

/** Hàng hành động dưới cùng của form. */
export function FormActions({ children }: { children: ReactNode }) {
  return <div className="app-row" style={{ justifyContent: 'flex-end', gap: 8 }}>{children}</div>;
}

export function Section({ title, children, actions }: { title: string; children: ReactNode; actions?: ReactNode }) {
  return (
    <div className="ir-stack" style={{ gap: 12 }}>
      <div className="app-row">
        <h2 className="app-section-title">{title}</h2>
        <span className="app-spacer" />
        {actions}
      </div>
      {children}
    </div>
  );
}
