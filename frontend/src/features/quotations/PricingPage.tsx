import { quoteApi } from '../../api/endpoints';
import { useAsync } from '../../hooks/useAsync';
import { Cell, DataTable, DemoTag, PageHeader, StatusBadge } from '../../components';
import { CostCategoryLabels, PricingUnitLabels, TransportModeLabels } from '../../types/enums';
import type { PricingRate } from '../../types/api';
import { countryName, formatMoney } from '../../utils/format';

export function PricingPage() {
  const state = useAsync(() => quoteApi.pricingRates(), []);
  return (
    <>
      <PageHeader title="Bảng giá" subtitle="Đơn giá dùng để tính chi phí ước tính và gợi ý khi lập báo giá cuối." meta={<DemoTag>Bảng giá demo, không phải giá thực tế</DemoTag>} />
      <DataTable<PricingRate> rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(r) => r.id} emptyTitle="Chưa có đơn giá"
        columns={[
          { key: 'c', header: 'Hạng mục', sortValue: (r) => r.code, render: (r) => <Cell main={r.description} sub={r.code} /> },
          { key: 'cat', header: 'Nhóm', render: (r) => CostCategoryLabels[r.category] },
          { key: 'u', header: 'Đơn vị', render: (r) => PricingUnitLabels[r.unit] },
          { key: 'p', header: 'Đơn giá', align: 'right', render: (r) => (r.unit === 'PERCENT_OF_DECLARED_VALUE' ? `${r.unitPrice}%` : formatMoney(r.unitPrice, r.currency)) },
          { key: 'a', header: 'Áp dụng', render: (r) => [r.transportMode && TransportModeLabels[r.transportMode], r.destinationCountry && countryName(r.destinationCountry)].filter(Boolean).join(' · ') || 'Chung' },
          { key: 's', header: 'Trạng thái', render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} labels={{ ACTIVE: 'Đang dùng', INACTIVE: 'Ngừng' }} /> },
        ]} />
    </>
  );
}
