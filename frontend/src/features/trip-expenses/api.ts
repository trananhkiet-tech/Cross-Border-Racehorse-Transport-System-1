import { http } from '../../api/client';

export type ExpenseCategory = 'FUEL' | 'TOLL' | 'FEED' | 'VET' | 'LODGING' | 'OTHER';
export type ExpenseStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export const ExpenseCategoryLabels: Record<ExpenseCategory, string> = {
  FUEL: 'Nhiên liệu', TOLL: 'Phí cầu đường', FEED: 'Thức ăn / nước', VET: 'Thú y', LODGING: 'Lưu trú', OTHER: 'Khác',
};
export const ExpenseStatusLabels: Record<ExpenseStatus, string> = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối' };

export interface Expense {
  id: number; tripId: number; tripNumber?: string; staffId: number; staffName?: string; category: ExpenseCategory; amount: number;
  currency: string; description?: string; status: ExpenseStatus; reviewNote?: string; reviewedAt?: string; createdAt: string;
}
export interface ExpensePayload { category: ExpenseCategory; amount: number; description?: string }

const d = <T,>(p: Promise<{ data: T }>) => p.then((r) => r.data);

export const expenseApi = {
  list: (status?: ExpenseStatus) => d<Expense[]>(http.get('/expenses', { params: status ? { status } : undefined })),
  create: (tripId: number, p: ExpensePayload) => d<Expense>(http.post(`/trips/${tripId}/expenses`, p)),
  review: (id: number, approve: boolean, note?: string) => d<Expense>(http.post(`/expenses/${id}/review`, { approve, note })),
};
