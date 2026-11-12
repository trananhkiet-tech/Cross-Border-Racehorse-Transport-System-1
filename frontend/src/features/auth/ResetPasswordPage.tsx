import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../../api/endpoints';
import { toAppError } from '../../api/client';
import { Alert, Button, PasswordField } from '../../components';
import { hasErrors, passwordError } from '../common/validation';

/** Đặt mật khẩu mới từ liên kết trong email (?token=...). */
export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const navigate = useNavigate();
  const [pw, setPw] = useState('');
  const [confirm, setConfirm] = useState('');
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const [formError, setFormError] = useState<string>();
  const [busy, setBusy] = useState(false);
  const [ok, setOk] = useState(false);

  if (!token) {
    return (
      <div className="auth-card">
        <Alert tone="error" title="Liên kết không hợp lệ">Liên kết đặt lại mật khẩu thiếu mã xác thực. Hãy yêu cầu liên kết mới.</Alert>
        <Link to="/forgot-password" className="app-text-sm" style={{ textAlign: 'center' }}>Yêu cầu liên kết mới</Link>
      </div>
    );
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const errs = {
      pw: passwordError(pw),
      confirm: confirm !== pw ? 'Mật khẩu xác nhận không khớp' : undefined,
    };
    setErrors(errs);
    if (hasErrors(errs)) return;
    setBusy(true);
    setFormError(undefined);
    try {
      await authApi.resetPassword(token, pw);
      setOk(true);
      window.setTimeout(() => navigate('/login', { replace: true }), 2500);
    } catch (err) {
      const ae = toAppError(err);
      setErrors((x) => ({ ...x, pw: ae.fieldErrors?.newPassword ?? x.pw }));
      setFormError(ae.status === 400 && !ae.fieldErrors?.newPassword ? 'Liên kết không hợp lệ hoặc đã hết hạn. Hãy yêu cầu liên kết mới.' : ae.message);
    } finally {
      setBusy(false);
    }
  };

  if (ok) {
    return (
      <div className="auth-card">
        <Alert tone="success" title="Đã đặt lại mật khẩu">Bạn có thể đăng nhập với mật khẩu mới. Đang chuyển về trang đăng nhập…</Alert>
        <Link to="/login" className="app-text-sm" style={{ textAlign: 'center' }}>Đăng nhập ngay</Link>
      </div>
    );
  }

  return (
    <form className="auth-card" onSubmit={submit} noValidate>
      <div>
        <h1>Đặt mật khẩu mới</h1>
        <p className="ir-muted" style={{ margin: '4px 0 0' }}>Nhập mật khẩu mới cho tài khoản của bạn.</p>
      </div>
      {formError && (
        <Alert tone="error">
          {formError} <Link to="/forgot-password">Yêu cầu liên kết mới</Link>
        </Alert>
      )}
      <PasswordField label="Mật khẩu mới" value={pw} onChange={setPw} error={errors.pw} required help="Tối thiểu 8 ký tự, có chữ và số" autoComplete="new-password" />
      <PasswordField label="Nhập lại mật khẩu" value={confirm} onChange={setConfirm} error={errors.confirm} required autoComplete="new-password" />
      <Button type="submit" size="lg" block loading={busy}>
        Đặt lại mật khẩu
      </Button>
    </form>
  );
}
