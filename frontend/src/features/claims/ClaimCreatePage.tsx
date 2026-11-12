import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { claimApi, requestApi } from '../../api/endpoints';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Alert, Button, Card, FileDrop, NumberField, PageHeader, SelectField, TextAreaField, enumOptions } from '../../components';
import { ClaimTypeLabels, ClaimTypeValues, EvidenceTypeLabels, EvidenceTypeValues, type ClaimType, type EvidenceType } from '../../types/enums';
import { formatMoney } from '../../utils/format';
import { FormActions, Money } from '../common/bits';
import { hasErrors } from '../common/validation';

const TOTAL_LOSS: ClaimType[] = ['MORTALITY', 'HUMANE_EUTHANASIA', 'THEFT_OR_LOSS'];
const NEEDS_HORSE: ClaimType[] = [...TOTAL_LOSS, 'INJURY_MEDICAL_EXPENSE'];

export function ClaimCreatePage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [requestId, setRequestId] = useState<number | undefined>(params.get('requestId') ? Number(params.get('requestId')) : undefined);
  const incidentId = params.get('incidentId') ? Number(params.get('incidentId')) : undefined;
  const [type, setType] = useState<ClaimType>('INJURY_MEDICAL_EXPENSE');
  const [horseId, setHorseId] = useState<number | undefined>();
  const [tripId, setTripId] = useState<number | undefined>();
  const [amount, setAmount] = useState<number | undefined>();
  const [desc, setDesc] = useState('');
  const [evType, setEvType] = useState<EvidenceType>('PHOTO');
  const [file, setFile] = useState<File | null>(null);
  const [fileErr, setFileErr] = useState<string>();
  const [err, setErr] = useState<Record<string, string | undefined>>({});
  const { run, busy, fieldErrors } = useAction();

  const requests = useAsync(() => requestApi.list({ status: ['TRIP_PLANNED', 'COMPLETED'], size: 100 }), []);
  const detail = useAsync(() => requestApi.get(requestId!), [requestId], !!requestId);
  const r = detail.data;
  const horse = r?.horses.find((h) => h.horseId === horseId);
  const total = TOTAL_LOSS.includes(type);

  const submit = async () => {
    const e = {
      request: requestId ? undefined : 'Chọn yêu cầu vận chuyển',
      horse: NEEDS_HORSE.includes(type) && !horseId && !incidentId ? 'Chọn ngựa liên quan' : undefined,
      amount: amount === undefined || amount <= 0 ? 'Nhập số tiền yêu cầu lớn hơn 0' : total && horse && amount > horse.declaredValue ? `Không vượt quá giá trị khai báo (${formatMoney(horse.declaredValue, r?.currency)})` : undefined,
      desc: desc.trim().length < 10 ? 'Mô tả tối thiểu 10 ký tự' : undefined,
      file: fileErr,
    };
    setErr(e);
    if (hasErrors(e)) return;
    try {
      const c = await run(() => claimApi.create({ requestId: requestId!, tripId, incidentId, horseId, claimType: type, claimedAmount: amount!, description: desc.trim() }), 'Đã nộp hồ sơ bồi thường');
      if (file) await claimApi.addEvidence(c.id, file, evType).catch(() => undefined);
      navigate(`/claims/${c.id}`, { replace: true });
    } catch { /* toast */ }
  };

  return (
    <>
      <PageHeader title="Tạo hồ sơ bồi thường" back={{ to: '/claims', label: 'Danh sách bồi thường' }} subtitle="Chỉ áp dụng cho yêu cầu đã có chuyến vận chuyển." />
      <div className="app-grid app-grid-main">
        <Card title="Thông tin hồ sơ" icon="bi-shield-exclamation">
          <form className="ir-stack" onSubmit={(ev) => { ev.preventDefault(); void submit(); }} noValidate>
            <SelectField label="Yêu cầu vận chuyển" required value={requestId} onChange={(v) => { setRequestId(v ? Number(v) : undefined); setHorseId(undefined); setTripId(undefined); }} placeholder={requests.loading ? 'Đang tải…' : 'Chọn yêu cầu'} error={err.request}
              options={(requests.data?.content ?? []).map((x) => ({ value: String(x.id), label: `${x.requestNumber} · ${x.originCountry} → ${x.destinationCountry}` }))} />
            {requests.data && requests.data.content.length === 0 && <Alert tone="warning">Bạn chưa có yêu cầu nào đã lập chuyến nên chưa thể tạo hồ sơ bồi thường.</Alert>}
            <SelectField label="Loại bồi thường" required value={type} onChange={(v) => setType(v as ClaimType)} options={enumOptions(ClaimTypeValues, ClaimTypeLabels)} />
            <div className="app-grid app-grid-2">
              <SelectField label="Ngựa liên quan" required={NEEDS_HORSE.includes(type) && !incidentId} optional={!(NEEDS_HORSE.includes(type) && !incidentId)} value={horseId} onChange={(v) => setHorseId(v ? Number(v) : undefined)} placeholder={r ? 'Chọn ngựa' : 'Chọn yêu cầu trước'} error={err.horse ?? fieldErrors.horseId}
                options={(r?.horses ?? []).map((h) => ({ value: String(h.horseId), label: h.horseName }))} />
              <SelectField label="Chuyến liên quan" optional value={tripId} onChange={(v) => setTripId(v ? Number(v) : undefined)} placeholder="Không chọn"
                options={(r?.trips ?? []).map((t) => ({ value: String(t.id), label: t.tripNumber }))} />
            </div>
            <NumberField label={`Số tiền yêu cầu (${r?.currency ?? 'USD'})`} required min={0} step="0.01" value={amount} onChange={setAmount} error={err.amount ?? fieldErrors.claimedAmount}
              help="Nhập số tiền thực tế bạn yêu cầu. Hệ thống không tự điền từ giá trị khai báo." />
            <TextAreaField label="Mô tả sự việc" required rows={5} value={desc} onChange={setDesc} error={err.desc ?? fieldErrors.description} maxLength={2000} />
            {incidentId && <Alert tone="info">Hồ sơ sẽ được gắn với sự cố #{incidentId}.</Alert>}
            <div className="app-grid app-grid-2">
              <SelectField label="Loại bằng chứng" optional value={evType} onChange={(v) => setEvType(v as EvidenceType)} options={enumOptions(EvidenceTypeValues, EvidenceTypeLabels)} />
            </div>
            <FileDrop compact file={file} error={fileErr} title="Bằng chứng đính kèm (không bắt buộc, có thể bổ sung sau)" onFile={(f, er) => { setFile(f); setFileErr(er); }} onClear={() => setFile(null)} />
            <FormActions>
              <Button variant="outline" onClick={() => navigate('/claims')} disabled={busy}>Hủy</Button>
              <Button type="submit" loading={busy} icon="bi-send">Nộp hồ sơ</Button>
            </FormActions>
          </form>
        </Card>
        <div className="ir-stack">
          <Alert tone="info" title="Lưu ý">
            Giá trị khai báo của ngựa chỉ là số tham chiếu cho việc giới hạn bồi thường tổn thất toàn phần. Số tiền duyệt do Quản lý Logistics quyết định sau thẩm định; hệ thống không tự bồi thường.
          </Alert>
          {horse && (
            <Card title="Tham chiếu giá trị khai báo">
              <div className="ir-muted">{horse.horseName}</div>
              <Money value={horse.declaredValue} currency={r?.currency} strong />
              {total && <div className="ir-muted" style={{ marginTop: 6 }}>Với loại này, số tiền yêu cầu không vượt giá trị khai báo.</div>}
            </Card>
          )}
        </div>
      </div>
    </>
  );
}
