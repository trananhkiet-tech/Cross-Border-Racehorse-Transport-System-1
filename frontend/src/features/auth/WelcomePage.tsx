import { Link } from 'react-router-dom';
import { useCurrentUser } from '../../app/AuthContext';
import { RoleLabels } from '../../types/enums';

/** Trang chào tạm thời của nhánh main: đăng nhập thành công nhưng chưa có module nghiệp vụ nào được merge. */
export function WelcomePage() {
  const user = useCurrentUser();
  return (
    <div className="container py-5" style={{ maxWidth: 720 }}>
      <h1 className="h3 mb-2">Xin chào, {user.fullName}</h1>
      <p className="text-muted mb-4">Vai trò: {RoleLabels[user.role]}</p>
      <div className="alert alert-info">
        Đăng nhập thành công. Nhánh <code>main</code> mới chỉ có đăng nhập / đăng ký; các chức năng nghiệp vụ sẽ xuất hiện
        khi nhóm merge nhánh <code>user1</code> … <code>user5</code> vào <code>main</code> (xem <code>docs/GIT_WORKFLOW.md</code>).
      </div>
      <Link className="btn btn-outline-secondary" to="/login">Quay lại trang đăng nhập</Link>
    </div>
  );
}
