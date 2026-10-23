import { useState } from 'react';
import { employeeApi } from '../../api/endpoints';
import { useCurrentUser } from '../../app/AuthContext';
import { useAction } from '../../hooks/useAction';
import { useAsync } from '../../hooks/useAsync';
import { Badge, Button, Cell, Checkbox, DataTable, Modal, PageHeader, PasswordField, SelectField, StatusBadge, TextField, enumOptions } from '../../components';
import { RoleLabels, RoleValues, StaffRoleLabels, StaffRoleValues, type Role, type StaffRole } from '../../types/enums';
import type { Employee } from '../../types/api';
import { formatDate } from '../../utils/format';
import { EMAIL_RE, passwordError } from '../common/validation';

function CreateEmployee({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const { run, busy, fieldErrors } = useAction();
  const [f, setF] = useState({ email: '', fullName: '', phone: '', role: 'DRIVER_ESCORT' as Role, initialPassword: '', employeeCode: '', department: '', staffRole: 'DRIVER' as StaffRole, licenseNumber: '', licenseExpiry: '', airEscortCertified: false, emergencyContact: '', baseLocation: '' });
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const set = <K extends keyof typeof f>(k: K, v: (typeof f)[K]) => setF((x) => ({ ...x, [k]: v }));
  const driver = f.role === 'DRIVER_ESCORT';
  const submit = async () => {
    const errs = {
      email: !EMAIL_RE.test(f.email.trim()) ? 'Email không hợp lệ' : undefined,
      fullName: !f.fullName.trim() ? 'Nhập họ tên' : undefined,
      employeeCode: !f.employeeCode.trim() ? 'Nhập mã nhân viên' : undefined,
      initialPassword: passwordError(f.initialPassword),
    };
    setErrors(errs);
    if (Object.values(errs).some(Boolean)) return;
    await run(() => employeeApi.create({ ...f, phone: f.phone || undefined, department: f.department || undefined, staffRole: driver ? f.staffRole : undefined, licenseNumber: f.licenseNumber || undefined, licenseExpiry: f.licenseExpiry || undefined, emergencyContact: f.emergencyContact || undefined, baseLocation: f.baseLocation || undefined }), 'Đã tạo tài khoản nhân viên');
    onSaved(); onClose();
  };
  const e = { ...fieldErrors, ...errors };
  return (
    <Modal open onClose={onClose} onSubmit={submit} busy={busy} size="lg" icon="bi-person-plus" title="Tạo tài khoản nhân viên" description="Nhân viên không tự đăng ký. Hệ thống có đúng 5 vai trò."
      footer={<><Button variant="outline" onClick={onClose} disabled={busy}>Hủy</Button><Button type="submit" loading={busy}>Tạo tài khoản</Button></>}>
      <div className="ir-stack">
        <div className="app-grid app-grid-2">
          <TextField label="Họ tên" value={f.fullName} onChange={(v) => set('fullName', v)} error={e.fullName} required />
          <TextField label="Email" type="email" value={f.email} onChange={(v) => set('email', v)} error={e.email} required />
          <TextField label="Điện thoại" value={f.phone} onChange={(v) => set('phone', v)} optional />
          <TextField label="Mã nhân viên" value={f.employeeCode} onChange={(v) => set('employeeCode', v)} error={e.employeeCode} required />
          <SelectField label="Vai trò" value={f.role} onChange={(v) => set('role', v as Role)} options={RoleValues.filter((r) => r !== 'CUSTOMER').map((r) => ({ value: r, label: RoleLabels[r] }))} />
          <TextField label="Bộ phận" value={f.department} onChange={(v) => set('department', v)} optional />
          <PasswordField label="Mật khẩu khởi tạo" value={f.initialPassword} onChange={(v) => set('initialPassword', v)} error={e.initialPassword} required help="Tối thiểu 8 ký tự, có chữ và số" autoComplete="new-password" />
          <TextField label="Nơi đóng quân" value={f.baseLocation} onChange={(v) => set('baseLocation', v)} optional />
        </div>
        {driver && (
          <div className="app-grid app-grid-2">
            <SelectField label="Vai trò trên chuyến" value={f.staffRole} onChange={(v) => set('staffRole', v as StaffRole)} options={enumOptions(StaffRoleValues, StaffRoleLabels)} />
            <TextField label="Số giấy phép / chứng chỉ" value={f.licenseNumber} onChange={(v) => set('licenseNumber', v)} optional />
            <TextField label="Giấy phép hết hạn" type="date" value={f.licenseExpiry} onChange={(v) => set('licenseExpiry', v)} optional />
            <TextField label="Liên hệ khẩn cấp" value={f.emergencyContact} onChange={(v) => set('emergencyContact', v)} optional />
            <Checkbox label="Có chứng chỉ áp tải đường hàng không" checked={f.airEscortCertified} onChange={(v) => set('airEscortCertified', v)} />
          </div>
        )}
      </div>
    </Modal>
  );
}

export function StaffPage() {
  const user = useCurrentUser();
  const isManager = user.role === 'LOGISTICS_MANAGER';
  const [role, setRole] = useState<Role | ''>('');
  const state = useAsync(() => employeeApi.list(role || undefined), [role]);
  const { run } = useAction();
  const [creating, setCreating] = useState(false);
  return (
    <>
      <PageHeader title={isManager ? 'Nhân sự vận hành' : 'Tài xế & áp tải'} subtitle="Danh sách nhân viên, giấy phép và số chuyến đang được phân công."
        actions={isManager && <Button icon="bi-person-plus" onClick={() => setCreating(true)}>Tạo tài khoản nhân viên</Button>} />
      <DataTable<Employee> rows={state.data} loading={state.loading} error={state.error} onRetry={state.reload} rowKey={(e) => e.userId} emptyTitle="Không có nhân viên" emptyIcon="bi-people"
        toolbar={<div style={{ width: 240 }}><SelectField size="sm" value={role} onChange={(v) => setRole(v as Role | '')} placeholder="Mọi vai trò" options={RoleValues.filter((r) => r !== 'CUSTOMER').map((r) => ({ value: r, label: RoleLabels[r] }))} /></div>}
        columns={[
          { key: 'n', header: 'Nhân viên', sortValue: (e) => e.fullName, render: (e) => <Cell main={e.fullName} sub={`${e.employeeCode ?? ''} · ${e.email}`} /> },
          { key: 'r', header: 'Vai trò', render: (e) => <Cell main={RoleLabels[e.role]} sub={e.staffRole && StaffRoleLabels[e.staffRole]} /> },
          { key: 'l', header: 'Giấy phép', render: (e) => (e.licenseNumber ? <Cell main={e.licenseNumber} sub={`đến ${formatDate(e.licenseExpiry)}`} /> : '—') },
          { key: 'a', header: 'Chứng chỉ', render: (e) => (e.airEscortCertified ? <Badge tone="info" icon="bi-airplane">Áp tải hàng không</Badge> : '—') },
          { key: 'c', header: 'Chuyến đang giao', align: 'right', sortValue: (e) => e.activeAssignments, render: (e) => e.activeAssignments },
          { key: 's', header: 'Trạng thái', render: (e) => <StatusBadge status={e.enabled ? 'ACTIVE' : 'INACTIVE'} labels={{ ACTIVE: 'Hoạt động', INACTIVE: 'Đã khóa' }} /> },
          ...(isManager ? [{ key: 'x', header: '', render: (e: Employee) => (
            <Button size="sm" variant={e.enabled ? 'danger-outline' : 'outline'} disabled={e.userId === user.id} onClick={async () => { await run(() => employeeApi.setEnabled(e.userId, !e.enabled), e.enabled ? 'Đã khóa tài khoản' : 'Đã mở khóa tài khoản'); state.reload(); }}>{e.enabled ? 'Khóa' : 'Mở khóa'}</Button>
          ) }] : []),
        ]} />
      {creating && <CreateEmployee onClose={() => setCreating(false)} onSaved={state.reload} />}
    </>
  );
}
