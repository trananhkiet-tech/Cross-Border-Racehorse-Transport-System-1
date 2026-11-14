import { http } from '../../api/client';

export interface FeedbackScores { punctuality: number; care: number; communication: number }
export interface FeedbackPayload extends FeedbackScores { comment?: string }
export interface Feedback extends FeedbackScores {
  id: number; tripId: number; overall: number; comment?: string; createdAt: string; updatedAt: string; editable: boolean;
}
export interface DeliveredTrip { tripId: number; tripNumber: string; requestNumber: string; deliveredAt?: string; feedback?: Feedback }
export interface RouteScore { routeId: number; routeName: string; count: number; punctuality: number; care: number; communication: number; overall: number }
export interface FeedbackSummary { totalFeedbacks: number; overall: number; routes: RouteScore[] }

const d = <T,>(p: Promise<{ data: T }>) => p.then((r) => r.data);

export const feedbackApi = {
  mine: () => d<DeliveredTrip[]>(http.get('/feedback/mine')),
  submit: (tripId: number, p: FeedbackPayload) => d<Feedback>(http.post(`/trips/${tripId}/feedback`, p)),
  summary: () => d<FeedbackSummary>(http.get('/reports/feedback-summary')),
};
