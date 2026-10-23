import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../app/AuthContext';
import { toAppError } from '../../api/client';
import { Alert, Button, PasswordField, TextField } from '../../components';
import { EMAIL_RE } from '../common/validation';

/** Tài khoản demo do DemoDataSeeder tạo (chỉ profile dev). Mật khẩu chung: Demo@123. */
const DEMO_ACCOUNTS = [
  { email: 'customer@irtms.vn', label: 'Khách hàng' },
  { email: 'manager@irtms.vn', label: 'Quản lý Logistics' },
  { email: 'specialist@irtms.vn', label: 'Chuyên viên vận chuyển' },
  { email: 'coordinator@irtms.vn', label: 'Điều phối đội xe & tuyến' },
  { email: 'driver@irtms.vn', label: 'Tài xế / Áp tải' },
];

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/dashboard';
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
  const [formError, setFormError] = useState<string>();
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const errs = {
      email: !email.trim() ? 'Vui lòng nhập email' : !EMAIL_RE.test(email.trim()) ? 'Email không hợp lệ' : undefined,
      password: !password ? 'Vui lòng nhập mật khẩu' : undefined,
    };
    setErrors(errs);
    if (errs.email || errs.password) return;
    setBusy(true);
    setFormError(undefined);
    try {
      await login(email.trim(), password);
      navigate(from, { replace: true });
    } catch (err) {
      const e2 = toAppError(err);
      setFormError(e2.status === 401 ? 'Email hoặc mật khẩu không đúng, hoặc tài khoản đã bị khóa.' : e2.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="auth-card" onSubmit={submit} noValidate>
      <div>
        <h1>Đăng nhập</h1>
        <p className="ir-muted" style={{ margin: '4px 0 0' }}>Đăng nhập để quản lý yêu cầu vận chuyển ngựa đua của bạn.</p>
      </div>
      {formError && <Alert tone="error" title="Không đăng nhập được">{formError}</Alert>}
      <TextField label="Email" type="email" autoComplete="email" icon="bi-envelope" value={email} onChange={setEmail} error={errors.email} required placeholder="ten@congty.vn" />
      <PasswordField label="Mật khẩu" autoComplete="current-password" value={password} onChange={setPassword} error={errors.password} required />
      <Button type="submit" size="lg" block loading={busy}>
        Đăng nhập
      </Button>
      <p className="app-text-sm" style={{ margin: 0, textAlign: 'center' }}>
        Chưa có tài khoản? <Link to="/register">Đăng ký tài khoản khách hàng</Link>
      </p>
      {import.meta.env.DEV && (
        <div className="ir-card" style={{ padding: 12 }}>
          <div className="app-text-xs ir-muted" style={{ marginBottom: 8 }}>
            Tài khoản demo (môi trường dev, mật khẩu <code>Demo@123</code>):
          </div>
          <div className="app-row" style={{ gap: 6 }}>
            {DEMO_ACCOUNTS.map((a) => (
              <button
                key={a.email}
                type="button"
                className="ir-chip"
                style={{ paddingRight: 10, border: 0, cursor: 'pointer' }}
                onClick={() => {
                  setEmail(a.email);
                  setPassword('Demo@123');
                }}
              >
                {a.label}
              </button>
            ))}
          </div>
        </div>
      )}
    </form>
  );
}
