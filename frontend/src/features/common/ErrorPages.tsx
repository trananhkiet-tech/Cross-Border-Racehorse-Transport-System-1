import { LinkButton } from '../../components';
import { Icon } from '../../components';

function ErrorPage({ code, title, description, icon }: { code: string; title: string; description: string; icon: string }) {
  return (
    <div className="ir-state ir-state-bordered" style={{ margin: '48px auto', maxWidth: 520 }} role="alert">
      <span className="ir-state-icon tone-error">
        <Icon name={icon} size={22} />
      </span>
      <span className="ir-state-code">{code}</span>
      <h1 className="ir-state-title">{title}</h1>
      <p className="ir-state-desc">{description}</p>
      <div className="ir-state-actions">
        <LinkButton to="/dashboard" icon="bi-house">
          Về trang tổng quan
        </LinkButton>
      </div>
    </div>
  );
}

export function ForbiddenPage() {
  return <ErrorPage code="403" title="Không có quyền truy cập" description="Vai trò của bạn không được phép xem trang này. Nếu cần quyền, hãy liên hệ Quản lý Logistics." icon="bi-shield-lock" />;
}

export function NotFoundPage() {
  return <ErrorPage code="404" title="Không tìm thấy trang" description="Đường dẫn không tồn tại hoặc đã bị thay đổi." icon="bi-signpost-split" />;
}
