import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfirmDialog, DataTable, StatusBadge } from '../components';
import { TripStatusLabels } from '../types/enums';

interface Row { id: number; name: string }
const cols = [{ key: 'n', header: 'Tên', render: (r: Row) => r.name }];

describe('DataTable', () => {
  it('hiển thị dòng dữ liệu', () => {
    render(<DataTable<Row> rows={[{ id: 1, name: 'Thunder' }]} rowKey={(r) => r.id} columns={cols} />);
    expect(screen.getByText('Thunder')).toBeInTheDocument();
  });
  it('trạng thái rỗng', () => {
    render(<DataTable<Row> rows={[]} rowKey={(r) => r.id} columns={cols} emptyTitle="Chưa có ngựa" />);
    expect(screen.getByText('Chưa có ngựa')).toBeInTheDocument();
  });
  it('trạng thái lỗi có nút thử lại', async () => {
    const retry = vi.fn();
    render(<DataTable<Row> rows={undefined} error="Máy chủ không phản hồi" onRetry={retry} rowKey={(r) => r.id} columns={cols} />);
    expect(screen.getByText('Máy chủ không phản hồi')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /thử lại/i }));
    expect(retry).toHaveBeenCalled();
  });
  it('bấm vào dòng gọi onRowClick', async () => {
    const click = vi.fn();
    render(<DataTable<Row> rows={[{ id: 7, name: 'Storm' }]} rowKey={(r) => r.id} columns={cols} onRowClick={click} />);
    await userEvent.click(screen.getByText('Storm'));
    expect(click).toHaveBeenCalledWith({ id: 7, name: 'Storm' });
  });
});

describe('ConfirmDialog', () => {
  it('bắt buộc nhập lý do: không gọi onConfirm khi để trống', async () => {
    const onConfirm = vi.fn();
    render(<ConfirmDialog open title="Từ chối?" requireReason reasonLabel="Lý do" confirmLabel="Từ chối" onConfirm={onConfirm} onClose={() => undefined} />);
    await userEvent.click(screen.getByRole('button', { name: 'Từ chối' }));
    expect(onConfirm).not.toHaveBeenCalled();
    await userEvent.type(screen.getByLabelText(/Lý do/), 'Thiếu hồ sơ tiêm chủng');
    await userEvent.click(screen.getByRole('button', { name: 'Từ chối' }));
    expect(onConfirm).toHaveBeenCalledWith('Thiếu hồ sơ tiêm chủng');
  });
  it('Esc / nút Hủy đóng hộp thoại', async () => {
    const onClose = vi.fn();
    render(<ConfirmDialog open title="Hủy?" onConfirm={() => undefined} onClose={onClose} />);
    await userEvent.click(screen.getByRole('button', { name: 'Hủy' }));
    expect(onClose).toHaveBeenCalled();
  });
});

describe('StatusBadge', () => {
  it('hiển thị nhãn tiếng Việt theo trạng thái', () => {
    render(<StatusBadge status="DELAYED" labels={TripStatusLabels} />);
    expect(screen.getByText(TripStatusLabels.DELAYED)).toBeInTheDocument();
  });
});
