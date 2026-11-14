import { http } from '../../api/client';
import type { RequestStatus } from '../../types/enums';

export interface OverdueItem {
  id: number; requestNumber: string; customerName: string; status: RequestStatus; lastUpdate?: string; daysWaiting: number; severity: 'WARNING' | 'CRITICAL';
}
export interface OverdueReport { thresholdDays: number; total: number; critical: number; items: OverdueItem[] }

export const overdueApi = {
  report: (days: number) => http.get<OverdueReport>(`/alerts/overdue-requests?days=${days}`).then((r) => r.data),
  notify: (days: number) => http.post<{ overdue: number }>(`/alerts/overdue-requests/notify?days=${days}`).then((r) => r.data),
};
