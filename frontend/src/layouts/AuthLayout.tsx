import { Outlet } from 'react-router-dom';
import { Icon } from '../components';

/** Bố cục trang đăng nhập / đăng ký: panel giới thiệu bên trái (ẩn trên mobile) + form bên phải. */
export function AuthLayout() {
  return (
    <div className="auth-page">
      <section className="auth-hero">
        <div className="ir-logo is-dark">
          <span className="ir-logo-mark">
            <Icon name="bi-globe-asia-australia" size={18} />
          </span>
          <span className="ir-logo-text">
            <span className="ir-logo-name">IRTMS</span>
            <span className="ir-logo-sub">International Racehorse Transportation</span>
          </span>
        </div>
        <div>
          <h2>Vận chuyển ngựa đua xuyên quốc gia – minh bạch từ yêu cầu đến bàn giao.</h2>
          <p>Một nền tảng cho chủ ngựa, điều phối, chuyên viên hồ sơ và đội vận chuyển cùng theo dõi hồ sơ tuân thủ, báo giá, hành trình và tình trạng ngựa.</p>
          <ul className="auth-points">
            <li><Icon name="bi-shield-check" /> Checklist tuân thủ theo tuyến và loại di chuyển</li>
            <li><Icon name="bi-geo-alt" /> Theo dõi hành trình, điểm dừng và sự cố theo thời gian thực</li>
            <li><Icon name="bi-receipt" /> Báo giá minh bạch, thanh toán mô phỏng an toàn</li>
          </ul>
        </div>
        <p className="app-text-xs" style={{ color: '#8FA0B8', margin: 0 }}>
          Đồ án môn học – dữ liệu trong hệ thống là dữ liệu demo, không có giá trị pháp lý.
        </p>
      </section>
      <section className="auth-panel">
        <Outlet />
      </section>
    </div>
  );
}
