// Ánh xạ trạng thái → nhãn tiếng Việt + tone màu thống nhất (badge dùng chung toàn hệ thống).
import * as E from '../types/enums';

export type Tone = 'neutral' | 'primary' | 'info' | 'success' | 'warning' | 'error' | 'critical';

const TONES: Record<string, Tone> = {
  // request
  DRAFT: 'neutral', SUBMITTED: 'info', UNDER_DOCUMENT_REVIEW: 'info', INFORMATION_REQUIRED: 'warning',
  COMPLIANCE_REVIEW: 'info', PENDING_MANAGER_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'error',
  QUOTE_SENT: 'primary', QUOTE_ACCEPTED: 'success', QUOTE_REJECTED: 'error', CANCELLED: 'neutral',
  READY_FOR_PLANNING: 'primary', TRIP_PLANNED: 'primary', COMPLETED: 'success',
  // trip
  SCHEDULED: 'neutral', PREPARING: 'info', READY_FOR_DEPARTURE: 'primary', IN_TRANSIT: 'primary',
  AT_CHECKPOINT: 'info', AT_BORDER: 'warning', CUSTOMS_CLEARANCE: 'warning', QUARANTINE: 'warning',
  DELAYED: 'error', ARRIVED: 'success', HANDOVER_PENDING: 'warning', DELIVERED: 'success',
  // document / checklist
  PENDING_REVIEW: 'warning', NEEDS_UPDATE: 'warning', EXPIRED: 'error', MISSING: 'neutral', WAIVED: 'info',
  // incident
  OPEN: 'error', UNDER_REVIEW: 'info', ACTION_REQUIRED: 'error', MONITORING: 'warning', RESOLVED: 'success', CLOSED: 'neutral',
  LOW: 'neutral', MEDIUM: 'warning', HIGH: 'error', CRITICAL: 'critical',
  // quote / invoice / payment
  ESTIMATED: 'info', SENT: 'primary', ACCEPTED: 'success', SUPERSEDED: 'neutral', ISSUED: 'info',
  PARTIALLY_PAID: 'warning', PAID: 'success', OVERDUE: 'error', SUCCEEDED: 'success', FAILED: 'error',
  // claim
  EVIDENCE_REQUIRED: 'warning', PARTIALLY_APPROVED: 'success',
  // vehicle / route
  AVAILABLE: 'success', IN_USE: 'primary', MAINTENANCE: 'warning', UNAVAILABLE: 'error', RETIRED: 'neutral',
  ACTIVE: 'success', INACTIVE: 'neutral',
  // handover
  PENDING_CONFIRMATION: 'warning', CONFIRMED: 'success', DISPUTED: 'error',
  // observation
  NORMAL: 'success', ATTENTION: 'warning', URGENT: 'critical',
};

/** Một số mã trùng tên giữa các enum (APPROVED, REJECTED…) – nhãn lấy theo enum được truyền vào. */
const LABEL_SOURCES: Record<string, string>[] = [
  E.RequestStatusLabels, E.TripStatusLabels, E.DocumentReviewStatusLabels, E.ChecklistItemStatusLabels,
  E.IncidentStatusLabels, E.IncidentSeverityLabels, E.QuoteStatusLabels, E.InvoiceStatusLabels,
  E.PaymentStatusLabels, E.ClaimStatusLabels, E.VehicleStatusLabels, E.RouteStatusLabels,
  E.HandoverStatusLabels, E.ObservationSeverityLabels,
];

export function toneOf(status?: string | null): Tone {
  if (!status) return 'neutral';
  return TONES[status] ?? 'neutral';
}

export function labelOf(status?: string | null, labels?: Record<string, string>): string {
  if (!status) return '—';
  if (labels && labels[status]) return labels[status];
  for (const src of LABEL_SOURCES) {
    if (src[status]) return src[status];
  }
  return status;
}

const ICONS: Partial<Record<Tone, string>> = {
  success: 'bi-check-circle-fill',
  warning: 'bi-exclamation-circle-fill',
  error: 'bi-x-circle-fill',
  critical: 'bi-exclamation-octagon-fill',
  info: 'bi-info-circle-fill',
  primary: 'bi-arrow-right-circle-fill',
  neutral: 'bi-circle',
};

export function iconOf(tone: Tone): string {
  return ICONS[tone] ?? 'bi-circle';
}
