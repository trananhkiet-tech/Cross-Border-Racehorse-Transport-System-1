import { useState } from 'react';
import { useAsync } from '../../hooks/useAsync';
import { useCurrentUser } from '../../app/AuthContext';
import { Button, Cell, DataTable, PageHeader } from '../../components';
import { formatDate } from '../../utils/format';
import { feedbackApi, type DeliveredTrip, type RouteScore } from './api';
import { FeedbackModal } from './FeedbackModal';

const stars = (n: number) => `${'★'.repeat(Math.round(n))}${'☆'.repeat(5 - Math.round(n))} ${n.toFixed(1)}`;

function CustomerView() {
  const state = useAsync(() => feedbackApi.mine(), []);
  const [selected, setSelected] = useState<DeliveredTrip | undefined>();
  return (
    <>
      <PageHeader title="Đánh giá chuyến" subtitle="Cho chúng tôi biết chuyến đã giao của bạn thế nào. Có thể sửa trong 7 ngày." />
      <DataTable<DeliveredTrip> rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(t) => t.tripId}
        emptyTitle="Chưa có chuyến nào đã giao" emptyIcon="bi-star"
        columns={[
          { key: 't', header: 'Chuyến', render: (t) => <Cell mono main={t.tripNumber} sub={t.requestNumber} /> },
          { key: 'd', header: 'Ngày giao', render: (t) => formatDate(t.deliveredAt) },
          { key: 'r', header: 'Đánh giá', render: (t) => (t.feedback ? stars(t.feedback.overall) : 'Chưa đánh giá') },
          { key: 'a', header: '', render: (t) => (!t.feedback || t.feedback.editable
            ? <Button size="sm" variant={t.feedback ? 'outline' : 'primary'} onClick={() => setSelected(t)}>{t.feedback ? 'Sửa' : 'Đánh giá'}</Button> : null) },
        ]} />
      <FeedbackModal trip={selected} onClose={() => setSelected(undefined)} onDone={state.reload} />
    </>
  );
}

function ManagerView() {
  const state = useAsync(() => feedbackApi.summary(), []);
  return (
    <>
      <PageHeader title="Đánh giá dịch vụ" subtitle={state.data ? `${state.data.totalFeedbacks} đánh giá · điểm trung bình ${state.data.overall.toFixed(1)}/5` : undefined} />
      <DataTable<RouteScore> rows={state.data?.routes} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(r) => r.routeId}
        emptyTitle="Chưa có đánh giá nào" emptyIcon="bi-star"
        columns={[
          { key: 'n', header: 'Tuyến', sortValue: (r) => r.routeName, render: (r) => <Cell main={r.routeName} sub={`${r.count} đánh giá`} /> },
          { key: 'p', header: 'Đúng giờ', render: (r) => r.punctuality.toFixed(1) },
          { key: 'c', header: 'Chăm sóc ngựa', render: (r) => r.care.toFixed(1) },
          { key: 'm', header: 'Giao tiếp', render: (r) => r.communication.toFixed(1) },
          { key: 'o', header: 'Tổng', sortValue: (r) => r.overall, render: (r) => stars(r.overall) },
        ]} />
    </>
  );
}

export function FeedbackPage() {
  const user = useCurrentUser();
  return user.role === 'LOGISTICS_MANAGER' ? <ManagerView /> : <CustomerView />;
}
