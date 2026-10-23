import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { quoteApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Alert, AsyncView, Button, Card, ConfirmDialog, CostTable, DescriptionList, LinkButton, PageHeader, StatusBadge } from '../../components';
import { refreshCounts } from '../../layouts/AppLayout';
import { QuoteStatusLabels, QuoteTypeLabels } from '../../types/enums';
import { formatDate, formatDateTime } from '../../utils/format';
import { RefLink } from '../common/bits';
import { QuoteBuilder } from './QuoteBuilder';

type Dlg = 'send' | 'accept' | 'reject' | 'version' | null;

export function QuoteDetailPage() {
  const id = Number(useParams().id);
  const navigate = useNavigate();
  const state = useAsync(() => quoteApi.get(id), [id]);
  const { run } = useAction();
  const [dlg, setDlg] = useState<Dlg>(null);
  const [editing, setEditing] = useState(false);

  return (
    <AsyncView state={state}>
      {(q) => {
        const has = (a: string) => q.allowedActions.includes(a);
        const reload = () => { refreshCounts(); state.reload(); };
        return (
          <>
            <PageHeader
              title={q.quoteNumber}
              back={{ to: '/quotes', label: 'Danh sách báo giá' }}
              subtitle={`${QuoteTypeLabels[q.quoteType]} · phiên bản ${q.versionNo} · ${q.route}`}
              meta={<StatusBadge status={q.status} labels={QuoteStatusLabels} size="md" />}
              actions={<>
                {has('EDIT') && !editing && <Button variant="outline" icon="bi-pencil" onClick={() => setEditing(true)}>Sửa chi phí</Button>}
                {has('SEND') && !editing && <Button icon="bi-send" onClick={() => setDlg('send')}>Gửi khách hàng</Button>}
                {has('NEW_VERSION') && <Button variant="outline" icon="bi-files" onClick={() => setDlg('version')}>Tạo phiên bản mới</Button>}
                {has('REJECT') && <Button variant="danger-outline" icon="bi-x-circle" onClick={() => setDlg('reject')}>Từ chối</Button>}
                {has('ACCEPT') && <Button variant="success" icon="bi-check-circle" onClick={() => setDlg('accept')}>Chấp nhận báo giá</Button>}
                {q.invoiceId && <LinkButton to={`/invoices/${q.invoiceId}`} icon="bi-credit-card">Hoá đơn & thanh toán</LinkButton>}
              </>}
            />
            {q.status === 'SENT' && q.expired && <Alert tone="error" title="Báo giá đã hết hạn">Vui lòng liên hệ Quản lý Logistics để nhận phiên bản mới.</Alert>}
            {q.status === 'SENT' && !q.expired && has('ACCEPT') && (
              <Alert tone="primary" title="Bước tiếp theo">Chấp nhận báo giá để hệ thống tạo hoá đơn. Bạn thanh toán cọc 50% (mô phỏng) để yêu cầu chuyển sang giai đoạn lập chuyến.</Alert>
            )}
            {q.customerResponseNote && <Alert tone={q.status === 'REJECTED' ? 'error' : 'info'} title="Phản hồi của khách hàng">{q.customerResponseNote}</Alert>}
            <div className="app-grid app-grid-main">
              {editing ? (
                <QuoteBuilder quote={q} onCancel={() => setEditing(false)} onSaved={() => { setEditing(false); reload(); }} />
              ) : (
                <Card title="Chi phí" icon="bi-receipt">
                  <CostTable items={q.items} subtotal={q.subtotal} discountTotal={q.discountTotal} total={q.totalAmount} currency={q.currency} estimatedTotal={q.estimatedTotal} />
                </Card>
              )}
              <Card title="Thông tin báo giá" icon="bi-info-circle">
                <DescriptionList columns={1} items={[
                  { label: 'Yêu cầu', value: <RefLink to={`/requests/${q.requestId}`}>{q.requestNumber}</RefLink> },
                  { label: 'Khách hàng', value: q.customerName },
                  { label: 'Hiệu lực đến', value: formatDate(q.validUntil) },
                  { label: 'Giá trị khai báo (tham chiếu)', value: `${q.declaredValueTotal.toLocaleString('en-US')} ${q.currency}` },
                  { label: 'Tạo bởi', value: `${q.createdBy} – ${formatDateTime(q.createdAt)}` },
                  { label: 'Đã gửi', value: formatDateTime(q.sentAt) },
                  { label: 'Đã chấp nhận', value: formatDateTime(q.acceptedAt) },
                  { label: 'Ghi chú', value: q.notes },
                ]} />
              </Card>
            </div>

            <ConfirmDialog open={dlg === 'send'} onClose={() => setDlg(null)} title="Gửi báo giá cho khách hàng?" confirmLabel="Gửi báo giá"
              message="Sau khi gửi không thể sửa dòng chi phí; nếu cần thay đổi hãy tạo phiên bản mới."
              onConfirm={async () => { await run(() => quoteApi.send(q.id), 'Đã gửi báo giá'); reload(); }} />
            <ConfirmDialog open={dlg === 'version'} onClose={() => setDlg(null)} title="Tạo phiên bản mới?" confirmLabel="Tạo phiên bản"
              message="Phiên bản hiện tại sẽ được đánh dấu là đã thay thế khi bản mới được gửi."
              onConfirm={async () => { const n = await run(() => quoteApi.createFinal(q.requestId), 'Đã tạo phiên bản mới'); navigate(`/quotes/${n.id}`); }} />
            <ConfirmDialog open={dlg === 'accept'} onClose={() => setDlg(null)} title="Chấp nhận báo giá?" tone="success" allowNote reasonLabel="Ghi chú (không bắt buộc)" confirmLabel="Chấp nhận"
              message={`Bạn đồng ý với tổng chi phí ${q.totalAmount.toLocaleString('en-US', { minimumFractionDigits: 2 })} ${q.currency}.`}
              onConfirm={async (n) => { const r = await run(() => quoteApi.accept(q.id, n || undefined), 'Đã chấp nhận báo giá. Hoá đơn đã được tạo.'); refreshCounts(); navigate(r.invoiceId ? `/invoices/${r.invoiceId}` : '/invoices'); }} />
            <ConfirmDialog open={dlg === 'reject'} onClose={() => setDlg(null)} title="Từ chối báo giá?" tone="error" requireReason reasonLabel="Lý do từ chối" confirmLabel="Từ chối"
              message="Quản lý Logistics sẽ nhận lý do và có thể gửi phiên bản mới."
              onConfirm={async (n) => { await run(() => quoteApi.reject(q.id, n), 'Đã từ chối báo giá'); reload(); }} />
          </>
        );
      }}
    </AsyncView>
  );
}
