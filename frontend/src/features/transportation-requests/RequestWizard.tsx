import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { horseApi, referenceApi, requestApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import {
  Alert, AsyncView, Button, Card, ChoiceCard, DescriptionList, EmptyState, Icon, LinkButton, NumberField, PageHeader,
  SelectField, Stepper, TextAreaField, TextField, enumOptions, type StepItem,
} from '../../components';
import { refreshCounts } from '../../layouts/AppLayout';
import {
  MovementTypeLabels, MovementTypeValues, TransportModeLabels, TransportModeValues, TransportPurposeLabels,
  TransportPurposeValues, type MovementType, type TransportMode, type TransportPurpose,
} from '../../types/enums';
import type { Horse, RequestDetail, RequestHorseInput, RequestPayload, StallType } from '../../types/api';
import { COUNTRY_OPTIONS, countryName, formatDate, formatMoney, isoDatePlus } from '../../utils/format';
import { hasErrors } from '../common/validation';

const COUNTRY_OPTS = COUNTRY_OPTIONS.map((c) => ({ value: c.code, label: c.name }));
const STEPS = ['Lộ trình & mục đích', 'Ngựa & chuồng', 'Xem lại & gửi'];

type HorseRow = RequestHorseInput & { declaredValue: number };

function emptyPayload(): RequestPayload {
  return {
    originCountry: 'VN', originAddress: '', destinationCountry: 'TH', destinationAddress: '',
    requestedDepartureDate: isoDatePlus(30), returnDate: null, movementType: 'TEMPORARY', purpose: 'RACING',
    transportMode: 'ROAD', currency: 'USD', specialRequirements: '', notes: '', horses: [],
  };
}

export function validateStep1(f: RequestPayload): Record<string, string | undefined> {
  return {
    originAddress: !f.originAddress.trim() ? 'Nhập địa điểm nhận ngựa' : undefined,
    destinationAddress: !f.destinationAddress.trim() ? 'Nhập địa điểm giao ngựa' : undefined,
    destinationCountry: f.destinationCountry === f.originCountry && f.originAddress.trim() === f.destinationAddress.trim() ? 'Điểm đến phải khác điểm đi' : undefined,
    requestedDepartureDate: !f.requestedDepartureDate ? 'Chọn ngày khởi hành' : f.requestedDepartureDate < isoDatePlus(0) ? 'Ngày khởi hành không được ở quá khứ' : undefined,
    returnDate: f.movementType === 'TEMPORARY' && f.returnDate && f.returnDate <= f.requestedDepartureDate ? 'Ngày quay về phải sau ngày khởi hành' : undefined,
  };
}

export function validateHorses(rows: HorseRow[]): Record<string, string | undefined> {
  const e: Record<string, string | undefined> = {};
  if (rows.length === 0) e.horses = 'Cần chọn ít nhất một ngựa';
  if (rows.length > 12) e.horses = 'Tối đa 12 ngựa cho một yêu cầu';
  rows.forEach((r, i) => {
    if (!r.stallTypeId) e[`stall-${i}`] = 'Chọn loại chuồng';
    if (!r.declaredValue || r.declaredValue <= 0) e[`value-${i}`] = 'Giá trị khai báo phải lớn hơn 0';
  });
  return e;
}

function Wizard({ initial, editing, horses, stalls }: { initial: RequestPayload; editing?: RequestDetail; horses: Horse[]; stalls: StallType[] }) {
  const navigate = useNavigate();
  const { run, busy } = useAction();
  const [step, setStep] = useState(0);
  const [f, setF] = useState<RequestPayload>(initial);
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const set = <K extends keyof RequestPayload>(k: K, v: RequestPayload[K]) => setF((x) => ({ ...x, [k]: v }));
  const rows = f.horses as HorseRow[];
  const setRow = (i: number, patch: Partial<HorseRow>) => set('horses', rows.map((r, j) => (j === i ? { ...r, ...patch } : r)));
  const horseById = useMemo(() => new Map(horses.map((h) => [h.id, h])), [horses]);
  const stallById = useMemo(() => new Map(stalls.map((s) => [s.id, s])), [stalls]);

  const toggleHorse = (h: Horse) => {
    if (rows.some((r) => r.horseId === h.id)) set('horses', rows.filter((r) => r.horseId !== h.id));
    else set('horses', [...rows, { horseId: h.id, stallTypeId: stalls[0]?.id ?? 0, declaredValue: 0 }]);
  };

  const next = () => {
    const errs = step === 0 ? validateStep1(f) : step === 1 ? validateHorses(rows) : {};
    setErrors(errs);
    if (hasErrors(errs)) return;
    setStep((s) => s + 1);
  };

  const payload = (): RequestPayload => ({
    ...f,
    originAddress: f.originAddress.trim(),
    destinationAddress: f.destinationAddress.trim(),
    returnDate: f.movementType === 'TEMPORARY' ? f.returnDate || null : null,
    specialRequirements: f.specialRequirements?.trim() || undefined,
    notes: f.notes?.trim() || undefined,
  });

  const save = async (submit: boolean) => {
    const all = { ...validateStep1(f), ...validateHorses(rows) };
    setErrors(all);
    if (hasErrors(all)) {
      setStep(hasErrors(validateStep1(f)) ? 0 : 1);
      return;
    }
    const saved = await run(() => (editing ? requestApi.update(editing.id, payload()) : requestApi.create(payload())), submit ? undefined : 'Đã lưu nháp yêu cầu');
    if (submit) {
      await run(() => requestApi.submit(saved.id), 'Đã gửi yêu cầu. Checklist hồ sơ và chi phí ước tính đã được tạo.');
    }
    refreshCounts();
    navigate(`/requests/${saved.id}`);
  };

  const steps: StepItem[] = STEPS.map((label, i) => ({ label, state: i < step ? 'done' : i === step ? 'current' : 'upcoming' }));

  return (
    <div className="ir-stack">
      <Card>
        <Stepper steps={steps} />
      </Card>

      {step === 0 && (
        <Card title="Lộ trình và mục đích vận chuyển" icon="bi-geo-alt" subtitle="Thông tin này quyết định checklist giấy tờ theo tuyến.">
          <div className="ir-stack">
            <div className="app-grid app-grid-2">
              <SelectField label="Quốc gia đi" value={f.originCountry} onChange={(v) => set('originCountry', v)} options={COUNTRY_OPTS} required />
              <TextField label="Địa điểm nhận ngựa" value={f.originAddress} onChange={(v) => set('originAddress', v)} error={errors.originAddress} required placeholder="VD: Trường đua Đại Nam, Bình Dương" />
              <SelectField label="Quốc gia đến" value={f.destinationCountry} onChange={(v) => set('destinationCountry', v)} options={COUNTRY_OPTS} error={errors.destinationCountry} required />
              <TextField label="Địa điểm giao ngựa" value={f.destinationAddress} onChange={(v) => set('destinationAddress', v)} error={errors.destinationAddress} required placeholder="VD: Royal Bangkok Sports Club" />
            </div>
            <div className="app-grid app-grid-3">
              <SelectField label="Loại di chuyển" value={f.movementType} onChange={(v) => set('movementType', v as MovementType)} options={enumOptions(MovementTypeValues, MovementTypeLabels)} help="Tạm thời: có kế hoạch quay về; Vĩnh viễn: chuyển hẳn." />
              <SelectField label="Mục đích" value={f.purpose} onChange={(v) => set('purpose', v as TransportPurpose)} options={enumOptions(TransportPurposeValues, TransportPurposeLabels)} />
              <SelectField label="Phương thức" value={f.transportMode} onChange={(v) => set('transportMode', v as TransportMode)} options={enumOptions(TransportModeValues, TransportModeLabels)} />
              <TextField label="Ngày khởi hành mong muốn" type="date" min={isoDatePlus(0)} value={f.requestedDepartureDate} onChange={(v) => set('requestedDepartureDate', v)} error={errors.requestedDepartureDate} required />
              {f.movementType === 'TEMPORARY' && (
                <TextField label="Ngày quay về (dự kiến)" type="date" min={f.requestedDepartureDate} value={f.returnDate ?? ''} onChange={(v) => set('returnDate', v || null)} error={errors.returnDate} optional />
              )}
              <SelectField label="Tiền tệ báo giá" value={f.currency} onChange={(v) => set('currency', v)} options={['USD', 'VND', 'EUR', 'SGD', 'THB'].map((c) => ({ value: c, label: c }))} help="Bảng giá demo tính theo USD." />
            </div>
          </div>
        </Card>
      )}

      {step === 1 && (
        <Card title="Chọn ngựa, loại chuồng và giá trị khai báo" icon="bi-postcard-heart">
          {horses.length === 0 ? (
            <EmptyState title="Bạn chưa có ngựa đang hoạt động" description="Thêm hồ sơ ngựa trước khi tạo yêu cầu." actions={<LinkButton to="/horses/new" icon="bi-plus-lg">Thêm ngựa</LinkButton>} />
          ) : (
            <div className="ir-stack">
              {errors.horses && <Alert tone="error">{errors.horses}</Alert>}
              <div className="app-grid app-grid-3">
                {horses.map((h) => {
                  const on = rows.some((r) => r.horseId === h.id);
                  return (
                    <div key={h.id} className={`ir-choice ir-choice-card${on ? ' is-checked' : ''}`}>
                      <label className="ir-choice-label">
                        <input type="checkbox" className="ir-sr" checked={on} onChange={() => toggleHorse(h)} />
                        <span className={`ir-check-box${on ? ' is-on' : ''}`} aria-hidden="true">{on && <Icon name="bi-check-lg" size={12} />}</span>
                        <span className="ir-choice-text">
                          <span className="ir-strong">{h.registeredName}</span>
                          <span className="ir-choice-desc">{h.breed ?? '—'} · chip {h.microchipNumber}</span>
                          {h.specialRequirements && <span className="ir-choice-desc">⚠ {h.specialRequirements}</span>}
                        </span>
                      </label>
                    </div>
                  );
                })}
              </div>
              {rows.map((r, i) => {
                const h = horseById.get(r.horseId);
                return (
                  <Card key={r.horseId} title={h?.registeredName ?? `Ngựa #${r.horseId}`} subtitle="Chọn loại chuồng phù hợp kích thước và tính cách ngựa">
                    <div className="ir-stack">
                      <div className="app-grid app-grid-3">
                        {stalls.map((s) => (
                          <ChoiceCard
                            key={s.id}
                            name={`stall-${r.horseId}`}
                            checked={r.stallTypeId === s.id}
                            onChange={() => setRow(i, { stallTypeId: s.id })}
                            title={`${s.name} · ${s.widthCm} cm`}
                            description={s.description}
                            extra={<span className="ir-strong ir-tnum">{s.surcharge > 0 ? `+${formatMoney(s.surcharge, s.currency)}` : 'Không phụ phí'}</span>}
                          />
                        ))}
                      </div>
                      {errors[`stall-${i}`] && <span className="ir-field-error">{errors[`stall-${i}`]}</span>}
                      <div className="app-grid app-grid-2">
                        <NumberField
                          label="Giá trị khai báo (Declared Value)"
                          value={r.declaredValue || undefined}
                          onChange={(v) => setRow(i, { declaredValue: v ?? 0 })}
                          suffix={f.currency}
                          min={0}
                          required
                          error={errors[`value-${i}`]}
                          help="Chỉ dùng tham chiếu bảo hiểm/hải quan và giới hạn bồi thường – không phải giá vận chuyển."
                        />
                        <TextField label="Ghi chú cho ngựa này" value={r.notes} onChange={(v) => setRow(i, { notes: v })} optional />
                      </div>
                    </div>
                  </Card>
                );
              })}
            </div>
          )}
        </Card>
      )}

      {step === 2 && (
        <>
          <Card title="Yêu cầu đặc biệt" icon="bi-chat-left-text">
            <div className="app-grid app-grid-2">
              <TextAreaField label="Yêu cầu đặc biệt" value={f.specialRequirements} onChange={(v) => set('specialRequirements', v)} maxLength={1000} placeholder="VD: cần áp tải có chứng chỉ hàng không, dừng nghỉ mỗi 4 giờ…" />
              <TextAreaField label="Ghi chú khác" value={f.notes} onChange={(v) => set('notes', v)} maxLength={1000} />
            </div>
          </Card>
          <Card title="Xem lại yêu cầu" icon="bi-clipboard-check">
            <DescriptionList items={[
              { label: 'Điểm đi', value: `${f.originAddress}, ${countryName(f.originCountry)}` },
              { label: 'Điểm đến', value: `${f.destinationAddress}, ${countryName(f.destinationCountry)}` },
              { label: 'Khởi hành', value: formatDate(f.requestedDepartureDate) },
              { label: 'Quay về', value: f.movementType === 'TEMPORARY' ? formatDate(f.returnDate) : 'Không (chuyển vĩnh viễn)' },
              { label: 'Loại di chuyển / mục đích', value: `${MovementTypeLabels[f.movementType]} · ${TransportPurposeLabels[f.purpose]}` },
              { label: 'Phương thức', value: TransportModeLabels[f.transportMode] },
              {
                label: 'Ngựa', span: true, value: (
                  <ul style={{ margin: 0, paddingLeft: 18 }}>
                    {rows.map((r) => (
                      <li key={r.horseId}>
                        {horseById.get(r.horseId)?.registeredName} – {stallById.get(r.stallTypeId)?.name} – khai báo {formatMoney(r.declaredValue, f.currency)}
                      </li>
                    ))}
                  </ul>
                ),
              },
            ]} />
            <div style={{ marginTop: 16 }}>
              <Alert tone="info" title="Sau khi gửi yêu cầu">
                Hệ thống tạo checklist giấy tờ theo tuyến {countryName(f.originCountry)} → {countryName(f.destinationCountry)} và chi phí ước tính (tham khảo). Báo giá cuối do Quản lý Logistics lập sau khi hồ sơ được duyệt.
              </Alert>
            </div>
          </Card>
        </>
      )}

      <div className="app-row">
        <Button variant="ghost" onClick={() => navigate(-1)} disabled={busy}>Hủy</Button>
        <span className="app-spacer" />
        {step > 0 && <Button variant="outline" icon="bi-arrow-left" onClick={() => setStep((s) => s - 1)} disabled={busy}>Quay lại</Button>}
        <Button variant="outline" icon="bi-save" onClick={() => save(false)} loading={busy}>Lưu nháp</Button>
        {step < STEPS.length - 1 ? (
          <Button iconRight="bi-arrow-right" onClick={next}>Tiếp tục</Button>
        ) : (
          <Button icon="bi-send" onClick={() => save(true)} loading={busy}>Gửi yêu cầu</Button>
        )}
      </div>
    </div>
  );
}

export function RequestCreatePage() {
  const [params] = useSearchParams();
  const preHorse = Number(params.get('horseId')) || undefined;
  const ready = useAsync(async () => ({ horses: await horseApi.list({ status: 'ACTIVE', size: 100 }).then((p) => p.content), stalls: await referenceApi.stallTypes() }), []);
  return (
    <>
      <PageHeader title="Tạo yêu cầu vận chuyển" back={{ to: '/requests', label: 'Yêu cầu vận chuyển' }} subtitle="Điền theo từng bước; có thể lưu nháp và gửi sau." />
      <AsyncView state={ready}>
        {(d) => {
          const init = emptyPayload();
          if (preHorse && d.horses.some((h) => h.id === preHorse)) init.horses = [{ horseId: preHorse, stallTypeId: d.stalls[0]?.id ?? 0, declaredValue: 0 }];
          return <Wizard initial={init} horses={d.horses} stalls={d.stalls} />;
        }}
      </AsyncView>
    </>
  );
}

export function RequestEditPage() {
  const id = Number(useParams().id);
  const state = useAsync(async () => {
    const [req, horses, stalls] = await Promise.all([requestApi.get(id), horseApi.list({ status: 'ACTIVE', size: 100 }).then((p) => p.content), referenceApi.stallTypes()]);
    return { req, horses, stalls };
  }, [id]);
  const navigate = useNavigate();
  useEffect(() => {
    if (state.data && !state.data.req.allowedActions.includes('EDIT')) navigate(`/requests/${id}`, { replace: true });
  }, [state.data, id, navigate]);
  return (
    <AsyncView state={state}>
      {({ req, horses, stalls }) => (
        <>
          <PageHeader title={`Chỉnh sửa ${req.requestNumber}`} back={{ to: `/requests/${id}`, label: 'Chi tiết yêu cầu' }} />
          {req.status === 'INFORMATION_REQUIRED' && req.lastDecisionNote && (
            <Alert tone="warning" title="Nội dung cần bổ sung">{req.lastDecisionNote}</Alert>
          )}
          <Wizard
            editing={req}
            horses={horses}
            stalls={stalls}
            initial={{
              originCountry: req.originCountry, originAddress: req.originAddress, destinationCountry: req.destinationCountry,
              destinationAddress: req.destinationAddress, requestedDepartureDate: req.requestedDepartureDate, returnDate: req.returnDate ?? null,
              movementType: req.movementType, purpose: req.purpose, transportMode: req.transportMode, currency: req.currency,
              specialRequirements: req.specialRequirements ?? '', notes: req.notes ?? '',
              horses: req.horses.map((h) => ({ horseId: h.horseId, stallTypeId: h.stallTypeId, declaredValue: h.declaredValue, notes: h.notes })),
            }}
          />
        </>
      )}
    </AsyncView>
  );
}
