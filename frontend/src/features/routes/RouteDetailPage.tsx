import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { routeApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { AsyncView, Badge, Button, Card, Cell, ConfirmDialog, DataTable, DescriptionList, LinkButton, Modal, NumberField, PageHeader, RouteMap, SelectField, StatusBadge, TextAreaField, TextField, enumOptions } from '../../components';
import { RouteStatusLabels, StopTypeLabels, StopTypeValues, TransportModeLabels, type StopType } from '../../types/enums';
import type { RouteDetail, RouteStop, RouteStopPayload } from '../../types/api';
import { COUNTRY_OPTIONS, countryName, formatNumber, minutesToText } from '../../utils/format';
import { RefLink } from '../common/bits';

const COUNTRY_OPTS = COUNTRY_OPTIONS.map((c) => ({ value: c.code, label: c.name }));

function StopModal({ routeId, stop, onClose, onSaved }: { routeId: number; stop?: RouteStop; onClose: () => void; onSaved: () => void }) {
  const { run, busy, fieldErrors } = useAction();
  const [f, setF] = useState<RouteStopPayload>(stop ? { ...stop } : { stopType: 'CHECKPOINT', name: '' });
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const set = <K extends keyof RouteStopPayload>(k: K, v: RouteStopPayload[K]) => setF((x) => ({ ...x, [k]: v }));
  const submit = async () => {
    const errs = {
      name: !f.name.trim() ? 'Nhập tên điểm dừng' : undefined,
      latitude: f.latitude != null && (f.latitude < -90 || f.latitude > 90) ? 'Vĩ độ −90…90' : undefined,
      longitude: f.longitude != null && (f.longitude < -180 || f.longitude > 180) ? 'Kinh độ −180…180' : undefined,
    };
    setErrors(errs);
    if (Object.values(errs).some(Boolean)) return;
    await run(() => (stop ? routeApi.updateStop(routeId, stop.id, f) : routeApi.addStop(routeId, f)), stop ? 'Đã cập nhật điểm dừng' : 'Đã thêm điểm dừng');
    onSaved(); onClose();
  };
  const e = { ...fieldErrors, ...errors };
  return (
    <Modal open onClose={onClose} onSubmit={submit} busy={busy} size="lg" icon="bi-geo-alt" title={stop ? 'Sửa điểm dừng' : 'Thêm điểm dừng'}
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Lưu</Button></>}>
      <div className="ir-stack">
        <div className="app-grid app-grid-2">
          <TextField label="Tên điểm dừng" value={f.name} onChange={(v) => set('name', v)} error={e.name} required />
          <SelectField label="Loại" value={f.stopType} onChange={(v) => set('stopType', v as StopType)} options={enumOptions(StopTypeValues, StopTypeLabels)} />
          <TextField label="Địa chỉ" value={f.address} onChange={(v) => set('address', v)} optional />
          <SelectField label="Quốc gia" value={f.country} onChange={(v) => set('country', v || undefined)} placeholder="—" options={COUNTRY_OPTS} />
          <NumberField label="Vĩ độ" value={f.latitude} onChange={(v) => set('latitude', v)} error={e.latitude} step="0.0001" />
          <NumberField label="Kinh độ" value={f.longitude} onChange={(v) => set('longitude', v)} error={e.longitude} step="0.0001" />
          <NumberField label="Đến sau (kể từ lúc khởi hành)" suffix="phút" value={f.plannedArrivalOffsetMin} onChange={(v) => set('plannedArrivalOffsetMin', v)} min={0} />
          <NumberField label="Thời gian dừng" suffix="phút" value={f.plannedDurationMin} onChange={(v) => set('plannedDurationMin', v)} min={0} />
          {!stop && <NumberField label="Vị trí trong tuyến" value={f.stopOrder} onChange={(v) => set('stopOrder', v)} min={1} help="Để trống để thêm vào cuối" />}
        </div>
        <TextAreaField label="Hướng dẫn tại điểm dừng" value={f.instructions} onChange={(v) => set('instructions', v)} rows={2} optional />
      </div>
    </Modal>
  );
}

function Detail({ r, reload }: { r: RouteDetail; reload: () => void }) {
  const user = useCurrentUser();
  const canEdit = user.role === 'FLEET_ROUTE_COORDINATOR';
  const { run } = useAction();
  const [stopModal, setStopModal] = useState<RouteStop | 'new' | null>(null);
  const [del, setDel] = useState<RouteStop | null>(null);
  const [alt, setAlt] = useState<number | null>(null);
  const altDetail = useAsync(() => routeApi.get(alt as number), [alt], alt !== null);

  return (
    <>
      <PageHeader title={r.name} back={{ to: '/routes', label: 'Tuyến đường' }} subtitle={`${r.routeCode} · ${TransportModeLabels[r.transportMode]}`}
        meta={<StatusBadge status={r.status} labels={RouteStatusLabels} size="md" />}
        actions={canEdit && <LinkButton to={`/routes/${r.id}/edit`} variant="outline" icon="bi-pencil">Sửa tuyến</LinkButton>} />
      <Card title="Sơ đồ tuyến" icon="bi-map" actions={r.alternatives.length > 0 && (
        <div style={{ width: 260 }}><SelectField size="sm" value={alt ?? ''} onChange={(v) => setAlt(v ? Number(v) : null)} placeholder="Hiện tuyến thay thế…" options={r.alternatives.map((a) => ({ value: a.id, label: `${a.routeCode} – ${a.name}` }))} /></div>
      )}>
        <RouteMap stops={r.stops} alternative={alt !== null ? altDetail.data?.stops : undefined} height={340} />
      </Card>
      <div className="app-grid app-grid-main">
        <Card title={`Điểm dừng (${r.stops.length})`} icon="bi-pin-map" flush actions={canEdit && <Button size="sm" icon="bi-plus-lg" onClick={() => setStopModal('new')}>Thêm điểm dừng</Button>}>
          <DataTable<RouteStop> flush dense rows={r.stops} rowKey={(s) => s.id} emptyTitle="Tuyến chưa có điểm dừng" emptyDescription="Thêm ít nhất điểm đón và điểm giao." emptyIcon="bi-pin-map"
            columns={[
              { key: 'o', header: '#', render: (s) => s.stopOrder, width: 50 },
              { key: 'n', header: 'Điểm dừng', render: (s) => <Cell main={s.name} sub={[s.address, s.country && countryName(s.country)].filter(Boolean).join(', ')} /> },
              { key: 't', header: 'Loại', render: (s) => <Badge tone="primary">{StopTypeLabels[s.stopType]}</Badge> },
              { key: 'e', header: 'Dự kiến', render: (s) => <Cell main={s.plannedArrivalOffsetMin != null ? `+${minutesToText(s.plannedArrivalOffsetMin)}` : '—'} sub={s.plannedDurationMin ? `dừng ${minutesToText(s.plannedDurationMin)}` : undefined} /> },
              ...(canEdit ? [{ key: 'a', header: '', align: 'right' as const, render: (s: RouteStop) => (
                <span className="app-row" style={{ justifyContent: 'flex-end', gap: 4 }}>
                  <Button size="sm" variant="ghost" icon="bi-pencil" aria-label={`Sửa ${s.name}`} onClick={() => setStopModal(s)} />
                  <Button size="sm" variant="ghost" icon="bi-trash" aria-label={`Xóa ${s.name}`} onClick={() => setDel(s)} />
                </span>
              ) }] : []),
            ]} />
        </Card>
        <div className="ir-stack">
          <Card title="Thông tin" icon="bi-info-circle">
            <DescriptionList columns={1} items={[
              { label: 'Từ', value: `${r.originLocation}, ${countryName(r.originCountry)}` },
              { label: 'Đến', value: `${r.destinationLocation}, ${countryName(r.destinationCountry)}` },
              { label: 'Khoảng cách', value: r.estimatedDistanceKm ? `${formatNumber(r.estimatedDistanceKm)} km` : undefined },
              { label: 'Thời gian ước tính', value: r.estimatedDurationHours ? `${formatNumber(r.estimatedDurationHours, 1)} giờ` : undefined },
              { label: 'Số chuyến đã dùng', value: r.tripCount },
              { label: 'Tuyến thay thế của', value: r.alternativeOfId ? <RefLink to={`/routes/${r.alternativeOfId}`}>{r.alternativeOfName}</RefLink> : undefined },
              { label: 'Ghi chú', value: r.notes },
            ]} />
          </Card>
          <Card title="Tuyến thay thế" icon="bi-sign-turn-right">
            {r.alternatives.length === 0 ? <span className="ir-muted app-text-sm">Chưa có tuyến thay thế. Tạo tuyến mới và chọn “Là tuyến thay thế của” tuyến này.</span> : (
              <ul style={{ margin: 0, paddingLeft: 18 }}>{r.alternatives.map((a) => <li key={a.id}><RefLink to={`/routes/${a.id}`}>{a.routeCode}</RefLink> {a.name}</li>)}</ul>
            )}
          </Card>
        </div>
      </div>
      {stopModal && <StopModal routeId={r.id} stop={stopModal === 'new' ? undefined : stopModal} onClose={() => setStopModal(null)} onSaved={reload} />}
      <ConfirmDialog open={!!del} onClose={() => setDel(null)} tone="error" title="Xóa điểm dừng?" message={del ? `Điểm dừng “${del.name}” sẽ bị xóa khỏi tuyến.` : undefined} confirmLabel="Xóa"
        onConfirm={async () => { if (del) { await run(() => routeApi.deleteStop(r.id, del.id), 'Đã xóa điểm dừng'); reload(); } }} />
    </>
  );
}

export function RouteDetailPage() {
  const id = Number(useParams().id);
  const state = useAsync(() => routeApi.get(id), [id]);
  return <AsyncView state={state}>{(r) => <Detail r={r} reload={state.reload} />}</AsyncView>;
}
