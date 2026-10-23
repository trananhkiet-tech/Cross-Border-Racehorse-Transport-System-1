import type { ReactNode } from 'react';
import { Icon } from './Icon';
import { Button } from './Button';

export function LoadingState({ text = 'Đang tải dữ liệu…', compact }: { text?: string; compact?: boolean }) {
  return (
    <div className={`ir-state ir-state-loading${compact ? ' ir-state-compact' : ''}`} role="status" aria-live="polite">
      <span className="spinner-border text-primary" aria-hidden="true" />
      <p className="ir-state-desc">{text}</p>
    </div>
  );
}

export function EmptyState({ title, description, icon = 'bi-inbox', actions, compact }: { title: string; description?: ReactNode; icon?: string; actions?: ReactNode; compact?: boolean }) {
  return (
    <div className={`ir-state ir-state-empty${compact ? ' ir-state-compact' : ''}`} role="status">
      <span className="ir-state-icon tone-neutral">
        <Icon name={icon} size={22} />
      </span>
      <h3 className="ir-state-title">{title}</h3>
      {description && <p className="ir-state-desc">{description}</p>}
      {actions && <div className="ir-state-actions">{actions}</div>}
    </div>
  );
}

export function ErrorState({ message, onRetry, title = 'Không tải được dữ liệu' }: { message: string; onRetry?: () => void; title?: string }) {
  return (
    <div className="ir-state ir-state-error" role="alert">
      <span className="ir-state-icon tone-error">
        <Icon name="bi-exclamation-triangle" size={22} />
      </span>
      <h3 className="ir-state-title">{title}</h3>
      <p className="ir-state-desc">{message}</p>
      {onRetry && (
        <div className="ir-state-actions">
          <Button variant="outline" icon="bi-arrow-clockwise" onClick={onRetry}>
            Thử lại
          </Button>
        </div>
      )}
    </div>
  );
}

/** Hiển thị loading / lỗi / rỗng theo kết quả của useAsync, nếu có dữ liệu thì render children. */
export function AsyncView<T>({ state, children, empty, isEmpty }: { state: { data?: T; loading: boolean; error?: string; reload: () => void }; children: (data: T) => ReactNode; empty?: ReactNode; isEmpty?: (data: T) => boolean }) {
  if (state.loading && state.data === undefined) return <LoadingState />;
  if (state.error) return <ErrorState message={state.error} onRetry={state.reload} />;
  if (state.data === undefined) return <LoadingState />;
  if (isEmpty && isEmpty(state.data)) return <>{empty ?? <EmptyState title="Chưa có dữ liệu" />}</>;
  return <>{children(state.data)}</>;
}
