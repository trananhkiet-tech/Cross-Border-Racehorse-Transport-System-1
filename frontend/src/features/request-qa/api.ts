import { http } from '../../api/client';
import type { Role } from '../../types/enums';

export interface Question {
  id: number; requestId: number; requestNumber: string; question: string; askedByName: string; askedByRole?: Role;
  createdAt: string; answer?: string; answeredByName?: string; answeredAt?: string; canAnswer: boolean;
}

const d = <T,>(p: Promise<{ data: T }>) => p.then((r) => r.data);

export const qaApi = {
  inbox: () => d<Question[]>(http.get('/questions/inbox')),
  listByRequest: (requestId: number) => d<Question[]>(http.get(`/requests/${requestId}/questions`)),
  ask: (requestId: number, question: string) => d<Question>(http.post(`/requests/${requestId}/questions`, { question })),
  answer: (id: number, answer: string) => d<Question>(http.post(`/questions/${id}/answer`, { answer })),
};
