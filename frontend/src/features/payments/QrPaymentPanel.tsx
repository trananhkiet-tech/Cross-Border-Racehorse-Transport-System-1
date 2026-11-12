import { useEffect, useState } from 'react';
import QRCode from 'qrcode';

const TTL_SECONDS = 300;
const DEMO_BANK = { name: 'IRTMS Demo Bank', account: '0000 1234 5678', holder: 'CONG TY IRTMS (DEMO)' };

/** Nội dung chuyển khoản – duy nhất cho mỗi lần tạo mã. */
export function buildTransferMemo(invoiceNumber: string, nonce: number) {
  return `${invoiceNumber.replace(/[^A-Za-z0-9]/g, '')}${String(nonce).slice(-4)}`;
}

/** Payload dạng văn bản (không phải chuẩn VietQR thật – chỉ mô phỏng). */
export function buildQrPayload(p: { invoiceNumber: string; amount: number; currency: string; memo: string }) {
  return `IRTMS-DEMO|BANK=${DEMO_BANK.name}|ACC=${DEMO_BANK.account.replace(/\s/g, '')}|AMT=${p.amount.toFixed(2)}|CUR=${p.currency}|MEMO=${p.memo}`;
}

export function QrPaymentPanel({ invoiceNumber, amount, currency, onExpiredChange }: { invoiceNumber: string; amount?: number; currency: string; onExpiredChange?: (expired: boolean) => void }) {
  const [nonce, setNonce] = useState(() => Date.now());
  const [left, setLeft] = useState(TTL_SECONDS);
  const [img, setImg] = useState<string>('');
  const valid = !!amount && amount > 0;
  const memo = buildTransferMemo(invoiceNumber, nonce);
  const expired = left <= 0;

  useEffect(() => { onExpiredChange?.(expired); }, [expired, onExpiredChange]);

  useEffect(() => {
    if (!valid) { setImg(''); return; }
    let alive = true;
    QRCode.toDataURL(buildQrPayload({ invoiceNumber, amount: amount as number, currency, memo }), { width: 220, margin: 1, errorCorrectionLevel: 'M' })
      .then((url) => { if (alive) setImg(url); })
      .catch(() => { if (alive) setImg(''); });
    return () => { alive = false; };
  }, [valid, amount, currency, invoiceNumber, memo]);

  useEffect(() => {
    if (expired) return;
    const t = window.setInterval(() => setLeft((s) => s - 1), 1000);
    return () => window.clearInterval(t);
  }, [expired]);

  const regenerate = () => { setNonce(Date.now()); setLeft(TTL_SECONDS); };
  const mm = String(Math.floor(Math.max(left, 0) / 60)).padStart(2, '0');
  const ss = String(Math.max(left, 0) % 60).padStart(2, '0');

  return (
    <div className="app-qr" data-testid="qr-panel">
      <div className={`app-qr-box${expired || !valid ? ' is-off' : ''}`}>
        {valid && img ? <img src={img} alt="Mã QR thanh toán mô phỏng" width={220} height={220} /> : <div className="app-qr-empty">Nhập số tiền để tạo mã</div>}
        {expired && valid && (
          <div className="app-qr-expired">
            <strong>Mã đã hết hạn</strong>
            <button type="button" className="app-qr-btn" onClick={regenerate}>Tạo mã mới</button>
          </div>
        )}
      </div>
      <dl className="app-qr-info">
        <div><dt>Ngân hàng</dt><dd>{DEMO_BANK.name}</dd></div>
        <div><dt>Số tài khoản</dt><dd className="ir-mono">{DEMO_BANK.account}</dd></div>
        <div><dt>Chủ tài khoản</dt><dd>{DEMO_BANK.holder}</dd></div>
        <div><dt>Số tiền</dt><dd><strong>{valid ? `${(amount as number).toFixed(2)} ${currency}` : '—'}</strong></dd></div>
        <div><dt>Nội dung CK</dt><dd className="ir-mono">{memo}</dd></div>
        <div><dt>Hiệu lực</dt><dd className="ir-mono">{expired ? 'Hết hạn' : `${mm}:${ss}`}</dd></div>
      </dl>
      <p className="app-qr-note">Đây là mã QR mô phỏng cho đồ án: quét bằng ứng dụng ngân hàng sẽ không chuyển tiền thật. Sau khi “quét”, bấm xác nhận bên dưới để ghi nhận giao dịch mô phỏng.</p>
    </div>
  );
}
