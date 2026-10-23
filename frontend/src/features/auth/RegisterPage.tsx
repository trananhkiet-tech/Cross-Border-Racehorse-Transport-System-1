import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../app/AuthContext';
import { toAppError } from '../../api/client';
import { Alert, Button, Checkbox, PasswordField, SelectField, TextField, enumOptions } from '../../components';
import { CustomerTypeLabels, CustomerTypeValues, type CustomerType } from '../../types/enums';
import { COUNTRY_OPTIONS } from '../../utils/format';
import { EMAIL_RE, PHONE_RE, hasErrors, passwordError } from '../common/validation';

/** Đăng ký công khai luôn tạo tài khoản CUSTOMER (backend cũng ép role này). */
export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [f, setF] = useState({ fullName: '', email: '', phone: '', companyName: '', customerType: 'INDIVIDUAL' as CustomerType, country: 'VN', password: '', confirm: '', acceptTerms: false });
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const [formError, setFormError] = useState<string>();
  const [busy, setBusy] = useState(false);
  const set = <K extends keyof typeof f>(k: K, v: (typeof f)[K]) => setF((x) => ({ ...x, [k]: v }));

  const validate = () => ({
    fullName: !f.fullName.trim() ? 'Vui lòng nhập họ tên' : undefined,
    email: !f.email.trim() ? 'Vui lòng nhập email' : !EMAIL_RE.test(f.email.trim()) ? 'Email không hợp lệ' : undefined,
    phone: f.phone && !PHONE_RE.test(f.phone) ? 'Số điện thoại không hợp lệ' : undefined,
    password: passwordError(f.password),
    confirm: f.confirm !== f.password ? 'Mật khẩu xác nhận không khớp' : undefined,
    acceptTerms: !f.acceptTerms ? 'Bạn cần đồng ý điều khoản sử dụng' : undefined,
  });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const errs = validate();
    setErrors(errs);
    if (hasErrors(errs)) return;
    setBusy(true);
    setFormError(undefined);
    try {
      await register({
        fullName: f.fullName.trim(), email: f.email.trim(), phone: f.phone || undefined, companyName: f.companyName || undefined,
        customerType: f.customerType, country: f.country, password: f.password, acceptTerms: f.acceptTerms,
      });
      navigate('/dashboard', { replace: true });
    } catch (err) {
      const ae = toAppError(err);
      setErrors((x) => ({ ...x, ...ae.fieldErrors }));
      setFormError(ae.status === 409 ? 'Email này đã được đăng ký.' : ae.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="auth-card" onSubmit={submit} noValidate style={{ maxWidth: 520 }}>
      <div>
        <h1>Tạo tài khoản khách hàng</h1>
        <p className="ir-muted" style={{ margin: '4px 0 0' }}>Dành cho chủ ngựa, trại ngựa và đại lý. Tài khoản nhân viên do Quản lý Logistics cấp.</p>
      </div>
      {formError && <Alert tone="error">{formError}</Alert>}
      <TextField label="Họ và tên" value={f.fullName} onChange={(v) => set('fullName', v)} error={errors.fullName} required autoComplete="name" />
      <div className="app-grid app-grid-2">
        <TextField label="Email" type="email" value={f.email} onChange={(v) => set('email', v)} error={errors.email} required autoComplete="email" />
        <TextField label="Số điện thoại" value={f.phone} onChange={(v) => set('phone', v)} error={errors.phone} optional autoComplete="tel" />
      </div>
      <div className="app-grid app-grid-2">
        <SelectField label="Loại khách hàng" value={f.customerType} onChange={(v) => set('customerType', v as CustomerType)} options={enumOptions(CustomerTypeValues, CustomerTypeLabels)} />
        <SelectField label="Quốc gia" value={f.country} onChange={(v) => set('country', v)} options={COUNTRY_OPTIONS.map((c) => ({ value: c.code, label: c.name }))} />
      </div>
      <TextField label="Tên trại ngựa / công ty" value={f.companyName} onChange={(v) => set('companyName', v)} optional />
      <div className="app-grid app-grid-2">
        <PasswordField label="Mật khẩu" value={f.password} onChange={(v) => set('password', v)} error={errors.password} required help="Tối thiểu 8 ký tự, có chữ và số" autoComplete="new-password" />
        <PasswordField label="Nhập lại mật khẩu" value={f.confirm} onChange={(v) => set('confirm', v)} error={errors.confirm} required autoComplete="new-password" />
      </div>
      <Checkbox label="Tôi đồng ý với điều khoản sử dụng và chính sách xử lý dữ liệu của IRTMS" checked={f.acceptTerms} onChange={(v) => set('acceptTerms', v)} error={errors.acceptTerms} />
      <Button type="submit" size="lg" block loading={busy}>
        Tạo tài khoản
      </Button>
      <p className="app-text-sm" style={{ margin: 0, textAlign: 'center' }}>
        Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
      </p>
    </form>
  );
}
