import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { horseApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { AsyncView, Button, Card, NumberField, PageHeader, SelectField, TextAreaField, TextField, enumOptions } from '../../components';
import { HorseSexLabels, HorseSexValues, type HorseSex } from '../../types/enums';
import type { HorsePayload } from '../../types/api';
import { COUNTRY_OPTIONS, isoDatePlus } from '../../utils/format';
import { MICROCHIP_RE, hasErrors } from '../common/validation';

const EMPTY: HorsePayload = { registeredName: '', microchipNumber: '', passportNumber: '', passportCountry: 'VN' };

export function validateHorse(f: HorsePayload): Record<string, string | undefined> {
  return {
    registeredName: !f.registeredName.trim() ? 'Vui lòng nhập tên đăng ký' : undefined,
    microchipNumber: !f.microchipNumber.trim() ? 'Vui lòng nhập số microchip' : !MICROCHIP_RE.test(f.microchipNumber.trim()) ? 'Microchip gồm 9–20 ký tự chữ/số' : undefined,
    passportNumber: !f.passportNumber.trim() ? 'Vui lòng nhập số hộ chiếu ngựa' : undefined,
    heightCm: f.heightCm !== undefined && (f.heightCm < 100 || f.heightCm > 220) ? 'Chiều cao từ 100 đến 220 cm' : undefined,
    dateOfBirth: f.dateOfBirth && f.dateOfBirth > isoDatePlus(0) ? 'Ngày sinh không được ở tương lai' : undefined,
  };
}

function Form({ initial, id }: { initial: HorsePayload; id?: number }) {
  const navigate = useNavigate();
  const { run, busy, fieldErrors } = useAction();
  const [f, setF] = useState<HorsePayload>(initial);
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const set = <K extends keyof HorsePayload>(k: K, v: HorsePayload[K]) => setF((x) => ({ ...x, [k]: v }));
  useEffect(() => setErrors((e) => ({ ...e, ...fieldErrors })), [fieldErrors]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const errs = validateHorse(f);
    setErrors(errs);
    if (hasErrors(errs)) return;
    const payload = { ...f, registeredName: f.registeredName.trim(), microchipNumber: f.microchipNumber.trim(), passportNumber: f.passportNumber.trim() };
    const h = await run(() => (id ? horseApi.update(id, payload) : horseApi.create(payload)), id ? 'Đã cập nhật hồ sơ ngựa' : 'Đã thêm ngựa');
    navigate(`/horses/${h.id}`);
  };

  return (
    <form className="ir-stack" onSubmit={submit} noValidate>
      <Card title="Định danh" icon="bi-upc-scan">
        <div className="app-grid app-grid-2">
          <TextField label="Tên đăng ký" value={f.registeredName} onChange={(v) => set('registeredName', v)} error={errors.registeredName} required placeholder="VD: Silver Arrow" />
          <TextField label="Giống" value={f.breed} onChange={(v) => set('breed', v)} placeholder="Thoroughbred" optional />
          <TextField label="Số microchip" value={f.microchipNumber} onChange={(v) => set('microchipNumber', v)} error={errors.microchipNumber} required help="9–20 ký tự chữ/số, duy nhất trong hệ thống" />
          <TextField label="Số hộ chiếu ngựa" value={f.passportNumber} onChange={(v) => set('passportNumber', v)} error={errors.passportNumber} required />
          <SelectField label="Quốc gia cấp hộ chiếu" value={f.passportCountry} onChange={(v) => set('passportCountry', v)} options={COUNTRY_OPTIONS.map((c) => ({ value: c.code, label: c.name }))} />
        </div>
      </Card>
      <Card title="Đặc điểm" icon="bi-postcard-heart">
        <div className="app-grid app-grid-3">
          <TextField label="Ngày sinh" type="date" value={f.dateOfBirth} onChange={(v) => set('dateOfBirth', v || undefined)} error={errors.dateOfBirth} max={isoDatePlus(0)} />
          <SelectField label="Giới tính" value={f.sex} onChange={(v) => set('sex', (v || undefined) as HorseSex | undefined)} placeholder="—" options={enumOptions(HorseSexValues, HorseSexLabels)} />
          <TextField label="Màu lông" value={f.color} onChange={(v) => set('color', v)} />
          <NumberField label="Chiều cao" suffix="cm" value={f.heightCm} onChange={(v) => set('heightCm', v)} error={errors.heightCm} min={100} max={220} />
        </div>
        <div className="app-grid app-grid-2" style={{ marginTop: 16 }}>
          <TextAreaField label="Yêu cầu đặc biệt khi vận chuyển" value={f.specialRequirements} onChange={(v) => set('specialRequirements', v)} maxLength={1000} placeholder="VD: dễ căng thẳng khi lên xe, cần chuồng rộng" />
          <TextAreaField label="Ghi chú" value={f.notes} onChange={(v) => set('notes', v)} maxLength={1000} />
        </div>
      </Card>
      <div className="app-row" style={{ justifyContent: 'flex-end' }}>
        <Button variant="outline" onClick={() => navigate(-1)} disabled={busy}>Hủy</Button>
        <Button type="submit" icon="bi-check2" loading={busy}>{id ? 'Lưu thay đổi' : 'Thêm ngựa'}</Button>
      </div>
    </form>
  );
}

export function HorseCreatePage() {
  return (
    <>
      <PageHeader title="Thêm ngựa" back={{ to: '/horses', label: 'Ngựa của tôi' }} subtitle="Hồ sơ ngựa dùng lại cho mọi yêu cầu vận chuyển." />
      <Form initial={EMPTY} />
    </>
  );
}

export function HorseEditPage() {
  const id = Number(useParams().id);
  const state = useAsync(() => horseApi.get(id), [id]);
  return (
    <AsyncView state={state}>
      {(h) => (
        <>
          <PageHeader title={`Chỉnh sửa: ${h.registeredName}`} back={{ to: `/horses/${id}`, label: 'Hồ sơ ngựa' }} />
          <Form
            id={id}
            initial={{
              registeredName: h.registeredName, breed: h.breed, dateOfBirth: h.dateOfBirth, sex: h.sex, color: h.color, heightCm: h.heightCm,
              microchipNumber: h.microchipNumber, passportNumber: h.passportNumber, passportCountry: h.passportCountry,
              specialRequirements: h.specialRequirements, notes: h.notes,
            }}
          />
        </>
      )}
    </AsyncView>
  );
}
