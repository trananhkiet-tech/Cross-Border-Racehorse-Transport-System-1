import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { paymentApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Alert, AsyncView, Button, Card, ChoiceCard, DataTable, DemoTag, DescriptionList, LinkButton, Modal, NumberField, PageHeader, Progress, StatusBadge, TextField } from '../../components';
import { refreshCounts } from '../../layouts/AppLayout';
import { InvoiceStatusLabels, PaymentMethodLabels, PaymentMethodValues, type PaymentMethod } from '../../types/enums';
import type { Invoice, PaymentItem } from '../../types/api';
import { formatDate, formatDateTime } from '../../utils/format';
import { Money, RefLink } from '../common/bits';

function PayModal({ inv, onClose, onDone }: { inv: Invoice; onClose: () => void; onDone: () => void }) {
  const { run, busy } = useAction();
  const [method, setMethod] = useState<PaymentMethod>('SIMULATED_CARD');
  const [amount, setAmount] = useState<number | undefined>(inv.paidAmount > 0 ? inv.outstandingAmount : inv.depositAmount);
  const [last4, setLast4] = useState('4242');
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const [result, setResult] = useState<PaymentItem | null>(null);

  const submit = async () => {
    const errs = {
      amount: !amount || amount <= 0 ? 'Nhập số tiền' : amount < inv.minimumNextPayment ? `Tối thiểu ${inv.minimumNextPayment.toFixed(2)} ${inv.currency}` : amount > inv.outstandingAmount ? 'Vượt quá số còn lại' : undefined,
      last4: method === 'SIMULATED_CARD' && !/^\d{4}$/.test(last4) ? 'Nhập đúng 4 chữ số' : undefined,
    };
    setErrors(errs);
    if (errs.amount || errs.last4) return;
    // Thất bại mô phỏng trả HTTP 200 với status FAILED – không phải lỗi mạng.
    const p = await run(() => paymentApi.simulate({ invoiceId: inv.id, amount: amount as number, method, cardLast4: method === 'SIMULATED_CARD' ? last4 : undefined }));
    setResult(p);
    refreshCounts();
    onDone();
  };

  if (result) {
    const ok = result.status === 'SUCCEEDED';
    return (
      <Modal open onClose={onClose} size="sm" icon={ok ? 'bi-check-circle' : 'bi-x-octagon'} tone={ok ? 'success' : 'error'} title={ok ? 'Thanh toán mô phỏng thành công' : 'Thanh toán mô phỏng bị từ chối'}
        description={ok ? `Mã giao dịch ${result.paymentReference}` : 'Thẻ mô phỏng kết thúc bằng 0002 luôn bị từ chối. Hãy thử lại với thẻ khác hoặc chuyển khoản mô phỏng.'}
        footer={<><Button variant="outline" onClick={onClose}>Đóng</Button>{!ok && <Button onClick={() => setResult(null)}>Thử lại</Button>}</>}>
        <DescriptionList columns={1} items={[{ label: 'Số tiền', value: <Money strong value={result.amount} currency={result.currency} /> }, { label: 'Phương thức', value: PaymentMethodLabels[result.method] }]} />
      </Modal>
    );
  }

  return (
    <Modal open onClose={onClose} onSubmit={submit} busy={busy} size="md" icon="bi-credit-card" title="Thanh toán mô phỏng" description="Không có tiền thật nào được chuyển. Không nhập số thẻ thật."
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Thanh toán</Button></>}>
      <div className="ir-stack">
        <Alert tone="warning" title="Chỉ là mô phỏng">Chỉ cần nhập 4 số cuối bất kỳ. Kết thúc bằng <strong>0002</strong> để thử trường hợp bị từ chối.</Alert>
        <div className="ir-stack" style={{ gap: 8 }}>
          {PaymentMethodValues.map((m) => (
            <ChoiceCard key={m} name="method" checked={method === m} onChange={() => setMethod(m)} title={PaymentMethodLabels[m]} />
          ))}
        </div>
        {method === 'SIMULATED_CARD' && <TextField label="4 số cuối thẻ (mô phỏng)" value={last4} onChange={(v) => setLast4(v.replace(/\D/g, '').slice(0, 4))} error={errors.last4} inputMode="numeric" maxLength={4} />}
        <NumberField label="Số tiền thanh toán" value={amount} onChange={setAmount} suffix={inv.currency} error={errors.amount} min={0} step="0.01"
          help={inv.paidAmount === 0 ? `Đặt cọc tối thiểu ${inv.depositPercent}% = ${inv.depositAmount.toFixed(2)} ${inv.currency}` : `Còn lại ${inv.outstandingAmount.toFixed(2)} ${inv.currency}`} />
        <div className="app-row">
          <Button size="sm" variant="outline" onClick={() => setAmount(inv.minimumNextPayment)}>Số tối thiểu</Button>
          <Button size="sm" variant="outline" onClick={() => setAmount(inv.outstandingAmount)}>Thanh toán toàn bộ</Button>
        </div>
      </div>
    </Modal>
  );
}

export function InvoiceDetailPage() {
  const id = Number(useParams().id);
  const user = useCurrentUser();
  const state = useAsync(() => paymentApi.invoice(id), [id]);
  const [pay, setPay] = useState(false);
  return (
    <AsyncView state={state}>
      {(inv) => (
        <>
          <PageHeader title={inv.invoiceNumber} back={{ to: '/invoices', label: 'Hoá đơn' }} subtitle={`${inv.customerName} · yêu cầu ${inv.requestNumber}`}
            meta={<><StatusBadge status={inv.status} labels={InvoiceStatusLabels} size="md" /><DemoTag>Thanh toán mô phỏng</DemoTag></>}
            actions={<>
              <LinkButton to={`/quotes/${inv.quoteId}`} variant="outline" icon="bi-receipt">Báo giá {inv.quoteNumber}</LinkButton>
              {user.role === 'CUSTOMER' && inv.outstandingAmount > 0 && inv.status !== 'CANCELLED' && <Button icon="bi-credit-card" onClick={() => setPay(true)}>Thanh toán</Button>}
            </>} />
          {inv.overdue && <Alert tone="error" title="Hoá đơn quá hạn">Hạn thanh toán là {formatDate(inv.dueDate)}.</Alert>}
          {inv.paidAmount === 0 && user.role === 'CUSTOMER' && (
            <Alert tone="primary" title="Đặt cọc để tiếp tục">Thanh toán tối thiểu {inv.depositPercent}% ({inv.depositAmount.toFixed(2)} {inv.currency}) để yêu cầu được chuyển sang lập chuyến.</Alert>
          )}
          <div className="app-grid app-grid-main">
            <Card title="Lịch sử thanh toán" icon="bi-clock-history" flush>
              <DataTable<PaymentItem> flush dense rows={inv.payments} rowKey={(p) => p.id} emptyTitle="Chưa có giao dịch"
                columns={[
                  { key: 'r', header: 'Mã giao dịch', render: (p) => <span className="ir-mono">{p.paymentReference}</span> },
                  { key: 't', header: 'Thời gian', render: (p) => formatDateTime(p.paidAt ?? p.createdAt) },
                  { key: 'm', header: 'Phương thức', render: (p) => PaymentMethodLabels[p.method] + (p.cardLast4 ? ` ••${p.cardLast4}` : '') },
                  { key: 'a', header: 'Số tiền', align: 'right', render: (p) => <Money value={p.amount} currency={p.currency} /> },
                  { key: 's', header: 'Kết quả', render: (p) => <StatusBadge status={p.status} /> },
                ]} />
            </Card>
            <Card title="Tóm tắt" icon="bi-cash-stack">
              <Progress value={inv.paidAmount} max={inv.totalAmount} tone={inv.status === 'PAID' ? 'success' : 'primary'} label="Đã thanh toán" />
              <div style={{ marginTop: 16 }}>
                <DescriptionList columns={1} items={[
                  { label: 'Tổng hoá đơn', value: <Money strong value={inv.totalAmount} currency={inv.currency} /> },
                  { label: 'Đã thanh toán', value: <Money value={inv.paidAmount} currency={inv.currency} /> },
                  { label: 'Còn lại', value: <Money strong value={inv.outstandingAmount} currency={inv.currency} /> },
                  { label: 'Ngày phát hành / hạn', value: `${formatDate(inv.issueDate)} / ${formatDate(inv.dueDate)}` },
                  { label: 'Yêu cầu', value: <RefLink to={`/requests/${inv.requestId}`}>{inv.requestNumber}</RefLink> },
                ]} />
              </div>
            </Card>
          </div>
          {pay && <PayModal inv={inv} onClose={() => setPay(false)} onDone={state.reload} />}
        </>
      )}
    </AsyncView>
  );
}
