import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { horseApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAsync } from '../../hooks/useAsync';
import { useDebounced } from '../../hooks/useQueryState';
import { Cell, DataTable, LinkButton, PageHeader, Pagination, SearchBox, SelectField, StatusBadge, enumOptions } from '../../components';
import { HorseSexLabels, HorseStatusLabels, HorseStatusValues, type HorseStatus } from '../../types/enums';
import type { Horse } from '../../types/api';
import { countryName } from '../../utils/format';

export function HorseListPage() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const isCustomer = user.role === 'CUSTOMER';
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<HorseStatus | ''>('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const dq = useDebounced(q);
  const state = useAsync(() => horseApi.list({ q: dq || undefined, status: status || undefined, page, size }), [dq, status, page, size]);

  return (
    <>
      <PageHeader
        title={isCustomer ? 'Ngựa của tôi' : 'Hồ sơ ngựa'}
        subtitle={isCustomer ? 'Quản lý hồ sơ ngựa để tạo yêu cầu vận chuyển.' : 'Tra cứu hồ sơ ngựa của khách hàng.'}
        actions={isCustomer && <LinkButton to="/horses/new" icon="bi-plus-lg">Thêm ngựa</LinkButton>}
      />
      <DataTable<Horse>
        rows={state.data?.content}
        loading={state.loading}
        error={state.error}
        onRetry={state.reload}
        rowKey={(h) => h.id}
        onRowClick={(h) => navigate(`/horses/${h.id}`)}
        emptyTitle={isCustomer ? 'Bạn chưa có ngựa nào' : 'Không có hồ sơ ngựa phù hợp'}
        emptyDescription={isCustomer ? 'Thêm hồ sơ ngựa đầu tiên để bắt đầu tạo yêu cầu vận chuyển.' : undefined}
        emptyIcon="bi-postcard-heart"
        toolbar={
          <>
            <SearchBox value={q} onChange={(v) => { setQ(v); setPage(0); }} placeholder="Tên, microchip, hộ chiếu…" />
            <div style={{ width: 200 }}>
              <SelectField size="sm" value={status} onChange={(v) => { setStatus(v as HorseStatus | ''); setPage(0); }} placeholder="Tất cả trạng thái" options={enumOptions(HorseStatusValues, HorseStatusLabels)} />
            </div>
          </>
        }
        columns={[
          { key: 'name', header: 'Tên ngựa', sortValue: (h) => h.registeredName, render: (h) => <Cell main={h.registeredName} sub={[h.breed, h.sex && HorseSexLabels[h.sex], h.ageYears !== undefined && h.ageYears !== null ? `${h.ageYears} tuổi` : null].filter(Boolean).join(' · ')} /> },
          { key: 'chip', header: 'Microchip', render: (h) => <span className="ir-mono">{h.microchipNumber}</span> },
          { key: 'passport', header: 'Hộ chiếu', render: (h) => <Cell main={h.passportNumber} sub={countryName(h.passportCountry)} /> },
          ...(!isCustomer ? [{ key: 'owner', header: 'Chủ sở hữu', sortValue: (h: Horse) => h.ownerName, render: (h: Horse) => h.ownerName }] : []),
          { key: 'status', header: 'Trạng thái', render: (h) => <StatusBadge status={h.status} labels={HorseStatusLabels} /> },
        ]}
        footer={state.data && <Pagination page={state.data.page} size={state.data.size} totalElements={state.data.totalElements} totalPages={state.data.totalPages} onPage={setPage} onSize={(s) => { setSize(s); setPage(0); }} />}
      />
    </>
  );
}
