import type { QuoteItem } from '../types/api';
import { CostCategoryLabels } from '../types/enums';
import { formatMoney } from '../utils/format';

/** Bảng chi phí báo giá (chỉ hiển thị – tổng tiền do backend tính). */
export function CostTable({ items, subtotal, discountTotal, total, currency, estimatedTotal, note }: { items: QuoteItem[]; subtotal: number; discountTotal: number; total: number; currency: string; estimatedTotal?: number; note?: string }) {
  const sorted = [...items].sort((a, b) => a.sortOrder - b.sortOrder);
  return (
    <div className="ir-cost">
      <table className="ir-cost-table">
        <tbody>
          {sorted.map((it) => (
            <tr key={it.id} className={it.category === 'DISCOUNT' ? 'is-muted' : undefined}>
              <th scope="row">
                <span className="ir-cost-label">{it.description}</span>
                <span className="ir-cost-note">
                  {CostCategoryLabels[it.category]} · {it.quantity} × {formatMoney(it.unitPrice, currency)}
                  {!it.confirmed && ' · ước tính'}
                </span>
              </th>
              <td>{formatMoney(it.amount, currency)}</td>
            </tr>
          ))}
          <tr className="ir-cost-sub">
            <th scope="row">Tạm tính</th>
            <td>{formatMoney(subtotal, currency)}</td>
          </tr>
          {discountTotal !== 0 && (
            <tr className="ir-cost-sub">
              <th scope="row">Giảm giá</th>
              <td>{formatMoney(discountTotal, currency)}</td>
            </tr>
          )}
          <tr className="ir-cost-total">
            <th scope="row">Tổng cộng</th>
            <td>
              {formatMoney(total, currency).replace(`${currency} `, '')}
              <span className="ir-cost-cur">{currency}</span>
            </td>
          </tr>
        </tbody>
      </table>
      {estimatedTotal !== undefined && estimatedTotal !== null && (
        <p className="ir-cost-foot">Ước tính ban đầu khi gửi yêu cầu: {formatMoney(estimatedTotal, currency)}</p>
      )}
      <p className="ir-cost-foot">
        {note ?? 'Giá trị khai báo của ngựa chỉ dùng để tính phí bảo hiểm (nếu có) và tham chiếu bồi thường – không phải giá vận chuyển.'}
      </p>
    </div>
  );
}
