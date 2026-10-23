import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { employeeApi, requestApi, routeApi, tripApi, vehicleApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Alert, AsyncView, Badge, Button, Card, Cell, ChoiceCard, DataTable, DescriptionList, EmptyState, PageHeader, RouteMap, SelectField, StatusBadge, TextAreaField, TextField } from '../../components';
import { refreshCounts } from '../../layouts/AppLayout';
import { StaffRoleLabels, TransportModeLabels, VehicleTypeLabels, type StaffRole } from '../../types/enums';
import type { RequestSummary } from '../../types/api';
import { countryName, formatDate, isoDateTimePlus } from '../../utils/format';
import { RouteText } from '../common/bits';

/** Trip Planning: chọn yêu cầu READY_FOR_PLANNING → chọn tuyến → xe → nhân sự → tạo chuyến. */
function PlanForm({ req }: { req: RequestSummary }) {
  const navigate = useNavigate();
  const { run, busy } = useAction();
  const detail = useAsync(() => requestApi.get(req.id), [req.id]);
  const routes = useAsync(() => routeApi.list({ status: ['ACTIVE'] }), []);
  const staff = useAsync(() => employeeApi.list('DRIVER_ESCORT'), []);
  const [routeId, setRouteId] = useState<number | ''>('');
  const [dep, setDep] = useState(`${req.requestedDepartureDate}T08:00`);
  const [arr, setArr] = useState('');
  const [vehicleId, setVehicleId] = useState<number | ''>('');
  const [assign, setAssign] = useState<Record<number, StaffRole | ''>>({});
  const [instructions, setInstructions] = useState('');
  const [notes, setNotes] = useState('');
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});

  useEffect(() => {
    if (dep < isoDateTimePlus(0)) setDep(isoDateTimePlus(24));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const matching = useMemo(() => (routes.data ?? []).filter((r) => r.originCountry === req.originCountry && r.destinationCountry === req.destinationCountry), [routes.data, req]);
  const route = routes.data?.find((r) => r.id === routeId);
  const routeDetail = useAsync(() => routeApi.get(routeId as number), [routeId], routeId !== '');
  const horseCount = detail.data?.horses.length ?? req.horseCount;

  useEffect(() => {
    if (route?.estimatedDurationHours && dep) {
      const d = new Date(dep);
      d.setMinutes(d.getMinutes() + Math.round(route.estimatedDurationHours * 60));
      const pad = (n: number) => String(n).padStart(2, '0');
      setArr(`${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`);
    }
  }, [route?.id, route?.estimatedDurationHours, dep]);

  const vehicles = useAsync(() => vehicleApi.available(dep, arr, Math.max(1, horseCount)), [dep, arr, horseCount], !!dep && !!arr && arr > dep);
  const chosen = Object.entries(assign).filter(([, r]) => r).map(([id, role]) => ({ staffId: Number(id), role: role as StaffRole }));

  const submit = async () => {
    const errs = {
      route: !routeId ? 'Chọn tuyến' : undefined,
      dep: !dep ? 'Chọn giờ khởi hành' : dep < isoDateTimePlus(0) ? 'Giờ khởi hành phải ở tương lai' : undefined,
      arr: !arr ? 'Chọn giờ đến dự kiến' : arr <= dep ? 'Giờ đến phải sau giờ khởi hành' : undefined,
      staff: chosen.some((c) => c.role === 'DRIVER') ? undefined : 'Cần ít nhất một tài xế',
    };
    setErrors(errs);
    if (Object.values(errs).some(Boolean)) return;
    const t = await run(() => tripApi.create({
      requestId: req.id, routeId: routeId as number, plannedDeparture: dep, plannedArrival: arr, vehicleId: vehicleId || null,
      transportInstructions: instructions || undefined, notes: notes || undefined, staff: chosen,
    }), 'Đã tạo chuyến. Hệ thống đã kiểm tra trùng lịch và giấy phép.');
    refreshCounts();
    navigate(`/trips/${t.id}`);
  };

  return (
    <div className="ir-stack">
      <Card title={`Yêu cầu ${req.requestNumber}`} icon="bi-clipboard-check">
        <DescriptionList items={[
          { label: 'Khách hàng', value: req.customerName },
          { label: 'Lộ trình', value: <RouteText {...req} /> },
          { label: 'Ngày khởi hành mong muốn', value: formatDate(req.requestedDepartureDate) },
          { label: 'Ngựa', value: `${req.horseCount} con – ${req.horseNames.join(', ')}` },
          { label: 'Phương thức', value: TransportModeLabels[req.transportMode] },
        ]} />
      </Card>
      <Card title="1. Tuyến đường" icon="bi-signpost-2">
        <div className="ir-stack">
          {routes.data && matching.length === 0 && <Alert tone="warning" title="Chưa có tuyến đang dùng cho lộ trình này">Hãy tạo hoặc kích hoạt tuyến {countryName(req.originCountry)} → {countryName(req.destinationCountry)} ở mục Tuyến đường.</Alert>}
          <SelectField label="Tuyến" value={routeId} onChange={(v) => setRouteId(v ? Number(v) : '')} placeholder="Chọn tuyến…" error={errors.route} required
            options={(matching.length ? matching : routes.data ?? []).map((r) => ({ value: r.id, label: `${r.routeCode} – ${r.name}${r.alternativeOfId ? ' (thay thế)' : ''}` }))} />
          {routeDetail.data && <RouteMap stops={routeDetail.data.stops} height={260} />}
        </div>
      </Card>
      <Card title="2. Thời gian" icon="bi-clock">
        <div className="app-grid app-grid-2">
          <TextField label="Khởi hành dự kiến" type="datetime-local" value={dep} onChange={setDep} error={errors.dep} required />
          <TextField label="Đến nơi dự kiến" type="datetime-local" value={arr} onChange={setArr} error={errors.arr} required help="Tự tính theo thời gian ước tính của tuyến, có thể chỉnh." />
        </div>
      </Card>
      <Card title="3. Phương tiện" icon="bi-truck-front" subtitle={`Chỉ hiện xe sẵn sàng, đủ sức chứa (${horseCount} ngựa) và không trùng lịch.`}>
        {!dep || !arr || arr <= dep ? <span className="ir-muted app-text-sm">Chọn thời gian hợp lệ để xem xe khả dụng.</span> : (
          <AsyncView state={vehicles} isEmpty={(v) => v.length === 0} empty={<Alert tone="warning">Không có xe phù hợp trong khoảng thời gian này. Có thể lưu chuyến trước và gán xe sau.</Alert>}>
            {(vs) => (
              <div className="app-grid app-grid-2">
                <ChoiceCard name="vehicle" checked={vehicleId === ''} onChange={() => setVehicleId('')} title="Gán xe sau" description="Chuyến sẽ ở trạng thái chờ gán xe" />
                {vs.map((v) => (
                  <ChoiceCard key={v.id} name="vehicle" checked={vehicleId === v.id} onChange={() => setVehicleId(v.id)} title={`${v.vehicleCode} · ${v.plateNumber}`}
                    description={`${VehicleTypeLabels[v.vehicleType]} – ${v.capacityHorses} ngựa${v.climateControlled ? ' – điều hòa' : ''}`} extra={v.maintenanceDue ? <Badge tone="warning">Sắp bảo dưỡng</Badge> : undefined} />
                ))}
              </div>
            )}
          </AsyncView>
        )}
      </Card>
      <Card title="4. Tài xế & áp tải" icon="bi-people" subtitle="Chọn vai trò trên chuyến cho từng nhân sự. Hệ thống chặn trùng lịch và giấy phép hết hạn." flush>
        {errors.staff && <div style={{ padding: '12px 16px' }}><Alert tone="error">{errors.staff}</Alert></div>}
        <DataTable flush dense rows={staff.data} loading={staff.loading} error={staff.error} onRetry={staff.reload} rowKey={(e) => e.userId} emptyTitle="Chưa có tài xế / áp tải"
          columns={[
            { key: 'n', header: 'Nhân sự', render: (e) => <Cell main={e.fullName} sub={e.licenseNumber ? `GP ${e.licenseNumber}` : undefined} /> },
            { key: 'a', header: 'Chứng chỉ', render: (e) => (e.airEscortCertified ? <Badge tone="info" icon="bi-airplane">Hàng không</Badge> : '—') },
            { key: 'c', header: 'Chuyến đang giao', align: 'right', render: (e) => e.activeAssignments },
            { key: 'r', header: 'Vai trò trên chuyến', render: (e) => (
              <div style={{ width: 200 }}><SelectField size="sm" value={assign[e.userId] ?? ''} onChange={(v) => setAssign((x) => ({ ...x, [e.userId]: v as StaffRole | '' }))} placeholder="Không phân công"
                options={(['DRIVER', 'ESCORT', 'TEAM_LEADER'] as StaffRole[]).map((r) => ({ value: r, label: StaffRoleLabels[r] }))} /></div>
            ) },
          ]} />
      </Card>
      <Card title="5. Hướng dẫn vận chuyển" icon="bi-chat-left-text">
        <div className="app-grid app-grid-2">
          <TextAreaField label="Hướng dẫn cho đội vận hành" value={instructions} onChange={setInstructions} rows={3} maxLength={2000} optional />
          <TextAreaField label="Ghi chú nội bộ" value={notes} onChange={setNotes} rows={3} maxLength={1000} optional />
        </div>
      </Card>
      <div className="app-row" style={{ justifyContent: 'flex-end' }}>
        <Button icon="bi-calendar2-check" onClick={submit} loading={busy}>Tạo chuyến</Button>
      </div>
    </div>
  );
}

export function PlanningPage() {
  const [params, setParams] = useSearchParams();
  const selected = Number(params.get('requestId')) || undefined;
  const state = useAsync(() => requestApi.list({ status: ['READY_FOR_PLANNING', 'TRIP_PLANNED'], size: 50 }), []);
  const req = state.data?.content.find((r) => r.id === selected);
  return (
    <>
      <PageHeader title="Lập chuyến" subtitle="Chọn yêu cầu đã thanh toán cọc, rồi gán tuyến, xe và nhân sự." back={selected ? { to: '/planning', label: 'Danh sách yêu cầu' } : undefined} />
      <AsyncView state={state}>
        {(p) => req ? <PlanForm key={req.id} req={req} /> : (
          <DataTable<RequestSummary> rows={p.content} rowKey={(r) => r.id} onRowClick={(r) => setParams({ requestId: String(r.id) })}
            emptyTitle="Không có yêu cầu nào chờ lập chuyến" emptyDescription="Yêu cầu xuất hiện ở đây sau khi khách hàng chấp nhận báo giá và thanh toán cọc." emptyIcon="bi-calendar-check"
            columns={[
              { key: 'n', header: 'Yêu cầu', render: (r) => <Cell mono main={r.requestNumber} sub={r.customerName} /> },
              { key: 'r', header: 'Lộ trình', render: (r) => <RouteText compact {...r} /> },
              { key: 'h', header: 'Ngựa', render: (r) => `${r.horseCount} – ${r.horseNames.join(', ')}` },
              { key: 'd', header: 'Khởi hành', sortValue: (r) => r.requestedDepartureDate, render: (r) => formatDate(r.requestedDepartureDate) },
              { key: 's', header: 'Trạng thái', render: (r) => <StatusBadge status={r.status} /> },
            ]} />
        )}
      </AsyncView>
      {selected && !req && !state.loading && <EmptyState title="Yêu cầu này chưa sẵn sàng lập chuyến" />}
    </>
  );
}
