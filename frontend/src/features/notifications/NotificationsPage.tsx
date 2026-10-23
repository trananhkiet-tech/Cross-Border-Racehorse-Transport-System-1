import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { notificationApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { useAction } from '../../hooks/useAction';
import { AsyncView, Button, Card, EmptyState, Icon, PageHeader, Pagination, Tabs } from '../../components';
import { refreshCounts } from '../../layouts/AppLayout';
import { notificationLink } from '../../routes/menu';
import { NotificationTypeLabels, type NotificationType } from '../../types/enums';
import type { NotificationItem } from '../../types/api';
import { formatDateTime, timeAgo } from '../../utils/format';

const TONE: Record<NotificationType, { tone: string; icon: string }> = {
  INFO: { tone: 'info', icon: 'bi-info-circle' },
  SUCCESS: { tone: 'success', icon: 'bi-check-circle' },
  WARNING: { tone: 'warning', icon: 'bi-exclamation-triangle' },
  ACTION_REQUIRED: { tone: 'primary', icon: 'bi-hand-index' },
  CRITICAL: { tone: 'critical', icon: 'bi-exclamation-octagon-fill' },
};

export function NotificationsPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState<'all' | 'unread'>('all');
  const [page, setPage] = useState(0);
  const state = useAsync(() => notificationApi.list({ unreadOnly: tab === 'unread', page, size: 15 }), [tab, page]);
  const { run, busy } = useAction();

  const open = async (n: NotificationItem) => {
    if (!n.read) {
      await notificationApi.markRead(n.id).catch(() => undefined);
      refreshCounts();
    }
    const link = notificationLink(n.referenceType, n.referenceId);
    if (link) navigate(link);
    else state.reload();
  };

  return (
    <>
      <PageHeader
        title="Thông báo"
        subtitle="Cập nhật về yêu cầu, tài liệu, báo giá, chuyến đi, sự cố và bồi thường."
        actions={
          <Button variant="outline" icon="bi-check2-all" loading={busy} onClick={async () => { await run(() => notificationApi.readAll(), 'Đã đánh dấu tất cả là đã đọc'); refreshCounts(); state.reload(); }}>
            Đánh dấu đã đọc tất cả
          </Button>
        }
      />
      <Card flush>
        <div style={{ padding: '0 16px' }}>
          <Tabs tabs={[{ key: 'all', label: 'Tất cả' }, { key: 'unread', label: 'Chưa đọc' }]} active={tab} onChange={(k) => { setTab(k); setPage(0); }} />
        </div>
        <AsyncView state={state} isEmpty={(p) => p.content.length === 0} empty={<EmptyState title="Không có thông báo" icon="bi-bell-slash" />}>
          {(p) => (
            <>
              <ul className="ir-notes ir-notes-full">
                {p.content.map((n) => {
                  const t = TONE[n.type];
                  return (
                    <li key={n.id} className={`ir-note app-clickable${n.read ? '' : ' is-unread'}${n.type === 'CRITICAL' ? ' is-critical' : ''}`} onClick={() => open(n)}>
                      <span className={`ir-note-icon tone-${t.tone}`}>
                        <Icon name={t.icon} size={15} />
                      </span>
                      <div className="ir-note-body">
                        <div className="ir-note-title">{n.title}</div>
                        <div className="ir-note-text">{n.message}</div>
                        <div className="ir-note-meta" title={formatDateTime(n.createdAt)}>
                          {NotificationTypeLabels[n.type]} · {timeAgo(n.createdAt)}
                        </div>
                      </div>
                      {!n.read && <span className="ir-note-dot" aria-label="Chưa đọc" />}
                    </li>
                  );
                })}
              </ul>
              <div className="ir-table-foot">
                <Pagination page={p.page} size={p.size} totalElements={p.totalElements} totalPages={p.totalPages} onPage={setPage} />
              </div>
            </>
          )}
        </AsyncView>
      </Card>
    </>
  );
}
