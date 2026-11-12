import { useState } from 'react';
import { quoteApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Alert, Button, Card, Checkbox, Icon, NumberField, SelectField, TextAreaField, TextField, enumOptions } from '../../components';
import { CostCategoryLabels, CostCategoryValues, type CostCategory } from '../../types/enums';
import type { Quote, QuoteItemInput } from '../../types/api';
import { formatMoney, isoDatePlus } from '../../utils/format';

interface Row extends QuoteItemInput { key: number }

/**
 * Quote Builder (Manager). Chỉ gửi số lượng + đơn giá; THÀNH TIỀN và TỔNG do backend tính khi lưu.
 * Dòng DISCOUNT nhập đơn giá dương, backend chuyển thành số âm.
 */
export function QuoteBuilder({ quote, onSaved, onCancel }: { quote: Quote; onSaved: () => void; onCancel: () => void }) {
  const { run, busy, fieldErrors } = useAction();
  const rates = useAsync(() => quoteApi.pricingRates(), []);
  let k = 0;
  const [rows, setRows] = useState<Row[]>(() => quote.items.map((i) => ({
    key: ++k, category: i.category, description: i.description, quantity: i.quantity, unitPrice: Math.abs(i.unitPrice),
    confirmed: i.confirmed, source: i.source, estimatedAmount: i.estimatedAmount,
  })));
  const [validUntil, setValidUntil] = useState(quote.validUntil ?? isoDatePlus(14));
  const [notes, setNotes] = useState(quote.notes ?? '');
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const nextKey = () => Math.max(0, ...rows.map((r) => r.key)) + 1;

  const patch = (key: number, p: Partial<Row>) => setRows((xs) => xs.map((r) => (r.key === key ? { ...r, ...p } : r)));

  const save = async () => {
    const errs: Record<string, string | undefined> = {};
    if (rows.length === 0) errs.rows = 'Báo giá cần ít nhất một dòng chi phí';
    rows.forEach((r) => {
      if (!r.description.trim()) errs[`d${r.key}`] = 'Nhập mô tả';
      if (!(r.quantity > 0)) errs[`q${r.key}`] = 'Số lượng > 0';
      if (r.unitPrice < 0 || Number.isNaN(r.unitPrice)) errs[`p${r.key}`] = 'Đơn giá ≥ 0';
    });
    if (validUntil < isoDatePlus(0)) errs.validUntil = 'Hiệu lực phải từ hôm nay trở đi';
    setErrors(errs);
    if (Object.values(errs).some(Boolean)) return;
    await run(() => quoteApi.update(quote.id, {
      validUntil, notes: notes.trim() || undefined,
      items: rows.map(({ key: _k, ...r }) => { void _k; return { ...r, description: r.description.trim() }; }),
    }), 'Đã lưu báo giá. Tổng tiền được backend tính lại.');
    onSaved();
  };

  return (
    <Card title="Quote Builder" icon="bi-calculator" subtitle="Nhập số lượng và đơn giá; thành tiền và tổng cộng do hệ thống tính khi lưu.">
      <div className="ir-stack">
        {errors.rows && <Alert tone="error">{errors.rows}</Alert>}
        {Object.keys(fieldErrors).length > 0 && <Alert tone="error">{Object.values(fieldErrors).join('; ')}</Alert>}
        {rows.map((r) => (
          <div key={r.key} className="ir-card" style={{ padding: 12 }}>
            <div className="app-grid" style={{ gridTemplateColumns: 'minmax(0,1.2fr) minmax(0,2fr) minmax(0,.7fr) minmax(0,1fr) auto', alignItems: 'start' }}>
              <SelectField label="Nhóm" size="sm" value={r.category} onChange={(v) => patch(r.key, { category: v as CostCategory })} options={enumOptions(CostCategoryValues, CostCategoryLabels)} />
              <TextField label="Mô tả" size="sm" value={r.description} onChange={(v) => patch(r.key, { description: v })} error={errors[`d${r.key}`]} />
              <NumberField label="Số lượng" size="sm" value={r.quantity} onChange={(v) => patch(r.key, { quantity: v ?? 0 })} error={errors[`q${r.key}`]} min={0} step="0.01" />
              <NumberField label={r.category === 'DISCOUNT' ? 'Mức giảm' : 'Đơn giá'} size="sm" value={r.unitPrice} onChange={(v) => patch(r.key, { unitPrice: v ?? 0 })} error={errors[`p${r.key}`]} min={0} step="0.01" suffix={quote.currency} />
              <div style={{ paddingTop: 26 }}>
                <Button variant="ghost" size="sm" icon="bi-trash" aria-label="Xóa dòng" onClick={() => setRows((xs) => xs.filter((x) => x.key !== r.key))} />
              </div>
            </div>
            <div className="app-row" style={{ marginTop: 6 }}>
              <Checkbox label="Đã xác nhận giá" checked={!!r.confirmed} onChange={(v) => patch(r.key, { confirmed: v })} />
              <span className="app-text-xs ir-muted">Thành tiền ≈ {r.category === 'DISCOUNT' ? '−' : ''}{formatMoney(r.quantity * r.unitPrice, quote.currency)} (tạm tính, chốt khi lưu)</span>
            </div>
          </div>
        ))}
        <div className="app-row">
          <Button variant="outline" icon="bi-plus-lg" onClick={() => setRows((xs) => [...xs, { key: nextKey(), category: 'SURCHARGE', description: '', quantity: 1, unitPrice: 0, confirmed: true }])}>Thêm dòng chi phí</Button>
          {rates.data && (
            <div style={{ width: 300 }}>
              <SelectField size="sm" value="" onChange={(id) => {
                const rate = rates.data?.find((x) => String(x.id) === id);
                if (rate) setRows((xs) => [...xs, { key: nextKey(), category: rate.category, description: rate.description, quantity: 1, unitPrice: rate.unitPrice, confirmed: false, source: rate.code }]);
              }} placeholder="Thêm từ bảng giá…" options={rates.data.filter((x) => x.active).map((x) => ({ value: x.id, label: `${x.description} (${x.unitPrice} ${x.currency})` }))} />
            </div>
          )}
        </div>
        <div className="app-grid app-grid-2">
          <TextField label="Báo giá có hiệu lực đến" type="date" value={validUntil} onChange={setValidUntil} min={isoDatePlus(0)} error={errors.validUntil} />
        </div>
        <TextAreaField label="Ghi chú cho khách hàng" value={notes} onChange={setNotes} rows={3} maxLength={2000} optional />
        <Alert tone="info" icon="bi-shield-lock"><Icon name="bi-info-circle" /> Giá trị khai báo của ngựa không tự động trở thành giá vận chuyển; chỉ nhập phí bảo hiểm khi có thỏa thuận.</Alert>
        <div className="app-row" style={{ justifyContent: 'flex-end' }}>
          <Button variant="outline" onClick={onCancel} disabled={busy}>Hủy</Button>
          <Button icon="bi-check2" onClick={save} loading={busy}>Lưu báo giá</Button>
        </div>
      </div>
    </Card>
  );
}
