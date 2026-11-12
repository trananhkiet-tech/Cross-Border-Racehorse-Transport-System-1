import { useEffect, useState, type FormEvent } from 'react';
import { accountApi } from '../../api/endpoints';
import { useAuth, useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { AsyncView, Avatar, Badge, Button, Card, DescriptionList, PageHeader, PasswordField, SelectField, TextField, enumOptions } from '../../components';
import { CustomerTypeLabels, CustomerTypeValues, RoleLabels, type CustomerType } from '../../types/enums';
import type { CustomerProfile } from '../../types/api';
import { COUNTRY_OPTIONS, formatDateTime } from '../../utils/format';
import { PHONE_RE, hasErrors, passwordError } from '../common/validation';

function CustomerProfileForm() {
  const state = useAsync(() => accountApi.customerProfile(), []);
  const { run, busy, fieldErrors } = useAction();
  const { refresh } = useAuth();
  const [f, setF] = useState<CustomerProfile | null>(null);
  useEffect(() => {
    if (state.data) setF(state.data);
  }, [state.data]);
  const set = <K extends keyof CustomerProfile>(k: K, v: CustomerProfile[K]) => setF((x) => (x ? { ...x, [k]: v } : x));
  const phoneErr = f?.phone && !PHONE_RE.test(f.phone) ? 'Số điện thoại không hợp lệ' : undefined;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!f || !f.fullName.trim() || phoneErr) return;
    const { userId: _u, email: _e, createdAt: _c, ...payload } = f;
    void _u; void _e; void _c;
    await run(() => accountApi.updateCustomerProfile(payload), 'Đã lưu hồ sơ khách hàng');
    await refresh();
  };

  return (
    <AsyncView state={state}>
      {() =>
        f && (
          <Card title="Thông tin khách hàng" icon="bi-building">
            <form className="ir-stack" onSubmit={submit} noValidate>
              <div className="app-grid app-grid-2">
                <TextField label="Họ và tên" value={f.fullName} onChange={(v) => set('fullName', v)} required error={!f.fullName.trim() ? 'Vui lòng nhập họ tên' : fieldErrors.fullName} />
                <TextField label="Số điện thoại" value={f.phone} onChange={(v) => set('phone', v)} error={phoneErr ?? fieldErrors.phone} />
                <TextField label="Tên trại ngựa / công ty" value={f.companyName} onChange={(v) => set('companyName', v)} />
                <SelectField label="Loại khách hàng" value={f.customerType} onChange={(v) => set('customerType', v as CustomerType)} options={enumOptions(CustomerTypeValues, CustomerTypeLabels)} />
                <TextField label="Địa chỉ" value={f.address} onChange={(v) => set('address', v)} />
                <SelectField label="Quốc gia" value={f.country} onChange={(v) => set('country', v)} placeholder="—" options={COUNTRY_OPTIONS.map((c) => ({ value: c.code, label: c.name }))} />
                <TextField label="Mã số thuế" value={f.taxCode} onChange={(v) => set('taxCode', v)} />
                <SelectField label="Ngôn ngữ" value={f.preferredLanguage} onChange={(v) => set('preferredLanguage', v)} options={[{ value: 'vi', label: 'Tiếng Việt' }, { value: 'en', label: 'English' }]} />
              </div>
              <div className="app-row" style={{ justifyContent: 'flex-end' }}>
                <Button type="submit" icon="bi-check2" loading={busy}>Lưu thay đổi</Button>
              </div>
            </form>
          </Card>
        )
      }
    </AsyncView>
  );
}

function AccountForm() {
  const user = useCurrentUser();
  const { setUser } = useAuth();
  const { run, busy } = useAction();
  const [fullName, setFullName] = useState(user.fullName);
  const [phone, setPhone] = useState(user.phone ?? '');
  const phoneErr = phone && !PHONE_RE.test(phone) ? 'Số điện thoại không hợp lệ' : undefined;
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!fullName.trim() || phoneErr) return;
    const u = await run(() => accountApi.update(fullName.trim(), phone || undefined), 'Đã cập nhật tài khoản');
    setUser(u);
  };
  return (
    <Card title="Thông tin tài khoản" icon="bi-person">
      <form className="ir-stack" onSubmit={submit} noValidate>
        <div className="app-grid app-grid-2">
          <TextField label="Họ và tên" value={fullName} onChange={setFullName} required error={!fullName.trim() ? 'Vui lòng nhập họ tên' : undefined} />
          <TextField label="Số điện thoại" value={phone} onChange={setPhone} error={phoneErr} />
        </div>
        <div className="app-row" style={{ justifyContent: 'flex-end' }}>
          <Button type="submit" icon="bi-check2" loading={busy}>Lưu</Button>
        </div>
      </form>
    </Card>
  );
}

function PasswordForm() {
  const { run, busy } = useAction();
  const [cur, setCur] = useState('');
  const [pw, setPw] = useState('');
  const [confirm, setConfirm] = useState('');
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const errs = {
      cur: !cur ? 'Nhập mật khẩu hiện tại' : undefined,
      pw: passwordError(pw) ?? (pw === cur ? 'Mật khẩu mới phải khác mật khẩu cũ' : undefined),
      confirm: confirm !== pw ? 'Mật khẩu xác nhận không khớp' : undefined,
    };
    setErrors(errs);
    if (hasErrors(errs)) return;
    await run(() => accountApi.changePassword(cur, pw), 'Đã đổi mật khẩu');
    setCur(''); setPw(''); setConfirm('');
  };
  return (
    <Card title="Đổi mật khẩu" icon="bi-shield-lock">
      <form className="ir-stack" onSubmit={submit} noValidate>
        <PasswordField label="Mật khẩu hiện tại" value={cur} onChange={setCur} error={errors.cur} autoComplete="current-password" />
        <div className="app-grid app-grid-2">
          <PasswordField label="Mật khẩu mới" value={pw} onChange={setPw} error={errors.pw} help="Tối thiểu 8 ký tự, có chữ và số" autoComplete="new-password" />
          <PasswordField label="Nhập lại mật khẩu mới" value={confirm} onChange={setConfirm} error={errors.confirm} autoComplete="new-password" />
        </div>
        <div className="app-row" style={{ justifyContent: 'flex-end' }}>
          <Button type="submit" variant="outline" icon="bi-key" loading={busy}>Đổi mật khẩu</Button>
        </div>
      </form>
    </Card>
  );
}

export function ProfilePage() {
  const user = useCurrentUser();
  return (
    <>
      <PageHeader title="Hồ sơ & cài đặt" subtitle="Quản lý thông tin tài khoản và bảo mật." />
      <div className="app-grid app-grid-main">
        <div className="ir-stack">
          {user.role === 'CUSTOMER' ? <CustomerProfileForm /> : <AccountForm />}
          <PasswordForm />
        </div>
        <Card title="Tài khoản" icon="bi-person-badge">
          <div className="ir-user-card" style={{ marginBottom: 16 }}>
            <Avatar name={user.fullName} size="lg" />
            <div>
              <div className="ir-strong">{user.fullName}</div>
              <div className="ir-muted app-text-sm">{user.email}</div>
              <Badge tone="primary">{RoleLabels[user.role]}</Badge>
            </div>
          </div>
          <DescriptionList columns={1} items={[
            { label: 'Trạng thái', value: user.enabled ? 'Đang hoạt động' : 'Đã khóa' },
            { label: 'Ngày tạo', value: formatDateTime(user.createdAt) },
            { label: 'Đăng nhập gần nhất', value: formatDateTime(user.lastLoginAt) },
          ]} />
        </Card>
      </div>
    </>
  );
}
