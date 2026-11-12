import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { routeApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { AsyncView, Button, Card, NumberField, PageHeader, SelectField, TextAreaField, TextField, enumOptions } from '../../components';
import { RouteStatusLabels, RouteStatusValues, TransportModeLabels, TransportModeValues, type RouteStatus, type TransportMode } from '../../types/enums';
import type { RoutePayload } from '../../types/api';
import { COUNTRY_OPTIONS } from '../../utils/format';

const COUNTRY_OPTS = COUNTRY_OPTIONS.map((c) => ({ value: c.code, label: c.name }));

function Form({ id, initial, baseId }: { id?: number; initial: RoutePayload; baseId?: number }) {
  const navigate = useNavigate();
  const { run, busy, fieldErrors } = useAction();
  const [f, setF] = useState<RoutePayload>(initial);
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const set = <K extends keyof RoutePayload>(k: K, v: RoutePayload[K]) => setF((x) => ({ ...x, [k]: v }));
  const candidates = useAsync(() => routeApi.list({}), []);
  const submit = async () => {
    const errs = {
      name: !f.name.trim() ? 'Nhập tên tuyến' : undefined,
      routeCode: f.routeCode && !/^[A-Z0-9-]*$/.test(f.routeCode) ? 'Chỉ gồm A–Z, 0–9 và dấu -' : undefined,
      originLocation: !f.originLocation.trim() ? 'Nhập điểm xuất phát' : undefined,
      destinationLocation: !f.destinationLocation.trim() ? 'Nhập điểm đến' : undefined,
      estimatedDistanceKm: f.estimatedDistanceKm !== undefined && f.estimatedDistanceKm < 0.1 ? 'Tối thiểu 0,1 km' : undefined,
      estimatedDurationHours: f.estimatedDurationHours !== undefined && f.estimatedDurationHours < 0.1 ? 'Tối thiểu 0,1 giờ' : undefined,
    };
    setErrors(errs);
    if (Object.values(errs).some(Boolean)) return;
    const payload: RoutePayload = { ...f, routeCode: f.routeCode || undefined, alternativeOfId: f.alternativeOfId || null, notes: f.notes || undefined };
    // Khi sửa: không gửi lại stops để không ghi đè danh sách điểm dừng (quản lý ở trang chi tiết).
    if (id) delete payload.stops;
    const r = await run(() => (id ? routeApi.update(id, payload) : routeApi.create(payload)), id ? 'Đã cập nhật tuyến' : 'Đã tạo tuyến. Hãy thêm các điểm dừng.');
    navigate(`/routes/${r.id}`);
  };
  const e = { ...fieldErrors, ...errors };
  return (
    <div className="ir-stack">
      <Card title="Thông tin tuyến" icon="bi-signpost-2">
        <div className="ir-stack">
          <div className="app-grid app-grid-2">
            <TextField label="Tên tuyến" value={f.name} onChange={(v) => set('name', v)} error={e.name} required placeholder="TP.HCM – Bangkok (đường bộ)" />
            <TextField label="Mã tuyến" value={f.routeCode} onChange={(v) => set('routeCode', v.toUpperCase())} error={e.routeCode} help="Để trống để hệ thống tự đặt mã" optional />
            <SelectField label="Quốc gia đi" value={f.originCountry} onChange={(v) => set('originCountry', v)} options={COUNTRY_OPTS} />
            <TextField label="Điểm xuất phát" value={f.originLocation} onChange={(v) => set('originLocation', v)} error={e.originLocation} required />
            <SelectField label="Quốc gia đến" value={f.destinationCountry} onChange={(v) => set('destinationCountry', v)} options={COUNTRY_OPTS} />
            <TextField label="Điểm đến" value={f.destinationLocation} onChange={(v) => set('destinationLocation', v)} error={e.destinationLocation} required />
            <SelectField label="Phương thức" value={f.transportMode} onChange={(v) => set('transportMode', v as TransportMode)} options={enumOptions(TransportModeValues, TransportModeLabels)} />
            <SelectField label="Trạng thái" value={f.status ?? 'DRAFT'} onChange={(v) => set('status', v as RouteStatus)} options={enumOptions(RouteStatusValues, RouteStatusLabels)} help="Chỉ tuyến Đang dùng mới chọn được khi lập chuyến." />
            <NumberField label="Khoảng cách ước tính" suffix="km" value={f.estimatedDistanceKm} onChange={(v) => set('estimatedDistanceKm', v)} error={e.estimatedDistanceKm} min={0} />
            <NumberField label="Thời gian ước tính" suffix="giờ" value={f.estimatedDurationHours} onChange={(v) => set('estimatedDurationHours', v)} error={e.estimatedDurationHours} min={0} />
            <SelectField label="Là tuyến thay thế của" value={f.alternativeOfId ?? ''} onChange={(v) => set('alternativeOfId', v ? Number(v) : null)} placeholder="— Không (tuyến chính)" options={(candidates.data ?? []).filter((r) => r.id !== id && !r.alternativeOfId).map((r) => ({ value: r.id, label: `${r.routeCode} – ${r.name}` }))} />
          </div>
          <TextAreaField label="Ghi chú" value={f.notes} onChange={(v) => set('notes', v)} rows={3} maxLength={1000} optional />
        </div>
      </Card>
      <div className="app-row" style={{ justifyContent: 'flex-end' }}>
        <Button variant="outline" onClick={() => navigate(baseId ? `/routes/${baseId}` : '/routes')} disabled={busy}>Hủy</Button>
        <Button icon="bi-check2" onClick={submit} loading={busy}>{id ? 'Lưu thay đổi' : 'Tạo tuyến'}</Button>
      </div>
    </div>
  );
}

export function RouteCreatePage() {
  return (
    <>
      <PageHeader title="Tạo tuyến" back={{ to: '/routes', label: 'Tuyến đường' }} subtitle="Sau khi tạo, thêm các điểm dừng (kiểm tra, biên giới, sân bay…) ở trang chi tiết." />
      <Form initial={{ name: '', originCountry: 'VN', originLocation: '', destinationCountry: 'TH', destinationLocation: '', transportMode: 'ROAD', status: 'DRAFT' }} />
    </>
  );
}

export function RouteEditPage() {
  const id = Number(useParams().id);
  const state = useAsync(() => routeApi.get(id), [id]);
  return (
    <AsyncView state={state}>
      {(r) => (
        <>
          <PageHeader title={`Sửa tuyến ${r.routeCode}`} back={{ to: `/routes/${id}`, label: 'Chi tiết tuyến' }} />
          <Form id={id} baseId={id} initial={{ routeCode: r.routeCode, name: r.name, originCountry: r.originCountry, originLocation: r.originLocation, destinationCountry: r.destinationCountry, destinationLocation: r.destinationLocation, transportMode: r.transportMode, estimatedDistanceKm: r.estimatedDistanceKm, estimatedDurationHours: r.estimatedDurationHours, status: r.status, alternativeOfId: r.alternativeOfId, notes: r.notes }} />
        </>
      )}
    </AsyncView>
  );
}
