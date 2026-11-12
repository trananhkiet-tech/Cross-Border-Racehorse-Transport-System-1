import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../../api/endpoints';
import { toAppError } from '../../api/client';
import { Alert, Button, TextField } from '../../components';
import { EMAIL_RE } from '../common/validation';

/** Yêu cầu đặt lại mật khẩu. Backend luôn trả cùng một thông báo (không lộ email có tồn tại hay không). */
export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string>();
  const [formError, setFormError] = useState<string>();
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState<{ message: string; devToken?: string }>();

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const err = !email.trim() ? 'Vui lòng nhập email' : !EMAIL_RE.test(email.trim()) ? 'Email không hợp lệ' : undefined;
    setError(err);
    if (err) return;
    setBusy(true);
    setFormError(undefined);
    try {
      const r = await authApi.forgotPassword(email.trim());
      setDone({ message: r.message, devToken: r.devResetToken });
    } catch (e2) {
      setFormError(toAppError(e2).message);
    } finally {
      setBusy(false);
    }
  };

  if (done) {
    return (
      <div className="auth-card">
        <div>
          <h1>Kiểm tra email của bạn</h1>
          <p className="ir-muted" style={{ margin: '4px 0 0' }}>{done.message}</p>
        </div>
        {done.devToken && (
          <Alert tone="warning" title="Môi trường dev (chưa cấu hình email)">
            Chưa có SMTP nên hệ thống không gửi được email. Dùng liên kết sau để đặt lại:{' '}
            <Link to={`/reset-password?token=${encodeURIComponent(done.devToken)}`}>Đặt lại mật khẩu</Link>
          </Alert>
        )}
        <Link to="/login" className="app-text-sm" style={{ textAlign: 'center' }}>Quay lại đăng nhập</Link>
      </div>
    );
  }

  return (
    <form className="auth-card" onSubmit={submit} noValidate>
      <div>
        <h1>Quên mật khẩu</h1>
        <p className="ir-muted" style={{ margin: '4px 0 0' }}>Nhập email đã đăng ký, chúng tôi sẽ gửi liên kết đặt lại mật khẩu (hiệu lực 30 phút).</p>
      </div>
      {formError && <Alert tone="error">{formError}</Alert>}
      <TextField label="Email" type="email" autoComplete="email" icon="bi-envelope" value={email} onChange={setEmail} error={error} required placeholder="ten@congty.vn" />
      <Button type="submit" size="lg" block loading={busy}>
        Gửi liên kết đặt lại
      </Button>
      <p className="app-text-sm" style={{ margin: 0, textAlign: 'center' }}>
        Nhớ ra rồi? <Link to="/login">Đăng nhập</Link>
      </p>
    </form>
  );
}
