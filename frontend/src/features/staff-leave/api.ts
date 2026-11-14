import { http } from '../../api/client';

export interface Leave { id: number; staffId: number; staffName: string; startDate: string; endDate: string; reason?: string; status: 'ACTIVE' | 'CANCELLED' }
export interface LeavePayload { staffId: number; startDate: string; endDate: string; reason?: string }

const d = <T,>(p: Promise<{ data: T }>) => p.then((r) => r.data);

export const leaveApi = {
  upcoming: () => d<Leave[]>(http.get('/staff-leaves')),
  mine: () => d<Leave[]>(http.get('/staff-leaves/mine')),
  create: (p: LeavePayload) => d<Leave>(http.post('/staff-leaves', p)),
  cancel: (id: number) => d<Leave>(http.post(`/staff-leaves/${id}/cancel`)),
};
