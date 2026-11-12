import type { StepItem } from '../../components';
import type { RequestStatus } from '../../types/enums';

/** Gom 15 trạng thái yêu cầu thành 6 chặng hiển thị trên stepper. */
const STAGES: { label: string; statuses: RequestStatus[] }[] = [
  { label: 'Tạo yêu cầu', statuses: ['DRAFT', 'SUBMITTED'] },
  { label: 'Hồ sơ & tuân thủ', statuses: ['UNDER_DOCUMENT_REVIEW', 'INFORMATION_REQUIRED', 'COMPLIANCE_REVIEW'] },
  { label: 'Phê duyệt', statuses: ['PENDING_MANAGER_APPROVAL', 'APPROVED'] },
  { label: 'Báo giá & thanh toán', statuses: ['QUOTE_SENT', 'QUOTE_ACCEPTED', 'QUOTE_REJECTED'] },
  { label: 'Lập chuyến & vận chuyển', statuses: ['READY_FOR_PLANNING', 'TRIP_PLANNED'] },
  { label: 'Hoàn tất', statuses: ['COMPLETED'] },
];

export function requestSteps(status: RequestStatus): StepItem[] {
  const idx = STAGES.findIndex((s) => s.statuses.includes(status));
  const failed = status === 'REJECTED' || status === 'CANCELLED';
  return STAGES.map((s, i) => ({
    label: s.label,
    state: failed ? (i === 0 ? 'error' : 'upcoming') : status === 'COMPLETED' || i < idx ? 'done' : i === idx ? (status === 'INFORMATION_REQUIRED' || status === 'QUOTE_REJECTED' ? 'error' : 'current') : 'upcoming',
  }));
}

export const STATUS_GROUPS: { key: string; label: string; statuses: RequestStatus[] }[] = [
  { key: 'all', label: 'Tất cả', statuses: [] },
  { key: 'draft', label: 'Nháp', statuses: ['DRAFT'] },
  { key: 'review', label: 'Đang xét hồ sơ', statuses: ['SUBMITTED', 'UNDER_DOCUMENT_REVIEW', 'INFORMATION_REQUIRED', 'COMPLIANCE_REVIEW'] },
  { key: 'approval', label: 'Chờ phê duyệt', statuses: ['PENDING_MANAGER_APPROVAL', 'APPROVED'] },
  { key: 'quote', label: 'Báo giá', statuses: ['QUOTE_SENT', 'QUOTE_ACCEPTED', 'QUOTE_REJECTED'] },
  { key: 'operation', label: 'Vận hành', statuses: ['READY_FOR_PLANNING', 'TRIP_PLANNED'] },
  { key: 'closed', label: 'Đã đóng', statuses: ['COMPLETED', 'REJECTED', 'CANCELLED'] },
];
