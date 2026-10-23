// Toàn bộ lời gọi REST API theo module (đồng bộ docs/API_CONTRACT.md).
import { http, qs } from './client';
import type {
  ArrivalCheck, Attachment, AuthResponse, Checklist, ChecklistItem, ClaimDetail, ClaimSummary, CreateTripPayload,
  CustomerProfile, Dashboard, DocumentItem, Employee, Handover, Horse, HorseHealthHistory, HorsePayload,
  IncidentDetail, IncidentReport, IncidentSummary, Invoice, KpiReport, NotificationItem, Observation, OpenItem, Page,
  PaymentItem, PreCheck, PricingRate, Quote, QuoteItemInput, RegisterPayload, RequestDetail, RequestPayload,
  RequestSummary, Requirement, RequirementPayload, RevenueReport, RouteDetail, RoutePayload, RouteStopPayload,
  RouteSummary, StallType, TimelineItem, Tracking, TrackingEventItem, TransportReport, TripDetail, TripSummary,
  UserSummary, Vehicle, VehiclePayload, VetConsultation, ClaimReport,
} from '../types/api';
import type {
  AttachmentRefType, ChecklistItemStatus, ClaimRecommendation, ClaimStatus, ClaimType, ComplianceCategory,
  DocumentReviewStatus, DocumentType, EvidenceType, HandoverStatus, HorseBehavior, HorseCondition, HorseSex,
  HorseStatus, IncidentActionType, IncidentSeverity, IncidentStatus, IncidentType, IntakeLevel, InvoiceStatus,
  MovementCondition, ObservationSeverity, OperationalDecision, PaymentMethod, QuoteStatus, RequestStatus, Role,
  RouteStatus, StaffRole, StressLevel, TrackingEventType, TripStatus, VehicleStatus, VehicleType,
} from '../types/enums';

const d = <T,>(p: Promise<{ data: T }>) => p.then((r) => r.data);

// ---------------------------------------------------------------- auth & account
export const authApi = {
  login: (email: string, password: string) => d<AuthResponse>(http.post('/auth/login', { email, password })),
  register: (payload: RegisterPayload) => d<AuthResponse>(http.post('/auth/register', payload)),
  me: () => d<UserSummary>(http.get('/auth/me')),
};
export const accountApi = {
  update: (fullName: string, phone?: string) => d<UserSummary>(http.put('/users/me', { fullName, phone })),
  changePassword: (currentPassword: string, newPassword: string) =>
    d<{ message: string }>(http.post('/users/me/password', { currentPassword, newPassword })),
  customerProfile: () => d<CustomerProfile>(http.get('/customers/me')),
  updateCustomerProfile: (p: Omit<CustomerProfile, 'userId' | 'email' | 'createdAt'>) => d<CustomerProfile>(http.put('/customers/me', p)),
};
export const employeeApi = {
  list: (role?: Role) => d<Employee[]>(http.get(`/employees${qs({ role })}`)),
  get: (id: number) => d<Employee>(http.get(`/employees/${id}`)),
  create: (p: Record<string, unknown>) => d<Employee>(http.post('/employees', p)),
  setEnabled: (id: number, enabled: boolean) => d<Employee>(http.patch(`/employees/${id}/status`, { enabled })),
};
export const referenceApi = {
  countries: () => d<Record<string, string>>(http.get('/reference/countries')),
  stallTypes: () => d<StallType[]>(http.get('/reference/stall-types')),
};

// ---------------------------------------------------------------- horses
export const horseApi = {
  list: (p: { q?: string; status?: HorseStatus; ownerId?: number; page?: number; size?: number } = {}) =>
    d<Page<Horse>>(http.get(`/horses${qs(p)}`)),
  get: (id: number) => d<Horse>(http.get(`/horses/${id}`)),
  create: (p: HorsePayload) => d<Horse>(http.post('/horses', p)),
  update: (id: number, p: HorsePayload) => d<Horse>(http.put(`/horses/${id}`, p)),
  archive: (id: number) => d<Horse>(http.delete(`/horses/${id}`)),
  documents: (id: number) => d<DocumentItem[]>(http.get(`/horses/${id}/documents`)),
  healthHistory: (id: number) => d<HorseHealthHistory>(http.get(`/horses/${id}/health-history`)),
};

// ---------------------------------------------------------------- requests
export const requestApi = {
  list: (p: { status?: RequestStatus[]; q?: string; page?: number; size?: number } = {}) =>
    d<Page<RequestSummary>>(http.get(`/transportation-requests${qs(p)}`)),
  get: (id: number) => d<RequestDetail>(http.get(`/transportation-requests/${id}`)),
  create: (p: RequestPayload) => d<RequestDetail>(http.post('/transportation-requests', p)),
  update: (id: number, p: RequestPayload) => d<RequestDetail>(http.put(`/transportation-requests/${id}`, p)),
  submit: (id: number) => d<RequestDetail>(http.post(`/transportation-requests/${id}/submit`)),
  cancel: (id: number, note?: string) => d<RequestDetail>(http.post(`/transportation-requests/${id}/cancel`, { note })),
  startReview: (id: number) => d<RequestDetail>(http.post(`/transportation-requests/${id}/start-review`)),
  completeDocumentReview: (id: number, note?: string) =>
    d<RequestDetail>(http.post(`/transportation-requests/${id}/complete-document-review`, { note })),
  completeCompliance: (id: number, note?: string) =>
    d<RequestDetail>(http.post(`/transportation-requests/${id}/complete-compliance`, { note })),
  requestInformation: (id: number, note: string) =>
    d<RequestDetail>(http.post(`/transportation-requests/${id}/request-information`, { note })),
  approve: (id: number, note?: string) => d<RequestDetail>(http.post(`/transportation-requests/${id}/approve`, { note })),
  reject: (id: number, note: string) => d<RequestDetail>(http.post(`/transportation-requests/${id}/reject`, { note })),
  documents: (id: number) => d<DocumentItem[]>(http.get(`/transportation-requests/${id}/documents`)),
  quotes: (id: number) => d<Quote[]>(http.get(`/transportation-requests/${id}/quotes`)),
};

// ---------------------------------------------------------------- documents & compliance
export interface UploadDocumentPayload {
  file: File; documentType: DocumentType; requestId?: number; horseId?: number; checklistItemId?: number;
  title?: string; documentNumber?: string; issuingCountry?: string; issueDate?: string; expiryDate?: string;
}
export const documentApi = {
  upload: (p: UploadDocumentPayload) => {
    const fd = new FormData();
    fd.append('file', p.file);
    Object.entries(p).forEach(([k, v]) => {
      if (k !== 'file' && v !== undefined && v !== null && v !== '') fd.append(k, String(v));
    });
    return d<DocumentItem>(http.post('/documents/upload', fd));
  },
  search: (p: { status?: DocumentReviewStatus[]; type?: DocumentType[]; q?: string; page?: number; size?: number } = {}) =>
    d<Page<DocumentItem>>(http.get(`/documents${qs(p)}`)),
  get: (id: number) => d<DocumentItem>(http.get(`/documents/${id}`)),
  review: (id: number, status: DocumentReviewStatus, note?: string) =>
    d<DocumentItem>(http.post(`/documents/${id}/review`, { status, note })),
  expiring: (days = 30) => d<DocumentItem[]>(http.get(`/documents/expiring${qs({ days })}`)),
  markExpired: () => d<{ updated: number }>(http.post('/documents/mark-expired')),
  filePath: (id: number) => `/documents/${id}/file`,
};
export const complianceApi = {
  requirements: (p: { category?: ComplianceCategory[]; destination?: string; includeInactive?: boolean } = {}) =>
    d<Requirement[]>(http.get(`/compliance/requirements${qs(p)}`)),
  createRequirement: (p: RequirementPayload) => d<Requirement>(http.post('/compliance/requirements', p)),
  updateRequirement: (id: number, p: RequirementPayload) => d<Requirement>(http.put(`/compliance/requirements/${id}`, p)),
  checklist: (requestId: number) => d<Checklist>(http.get(`/compliance/checklists/${requestId}`)),
  generate: (requestId: number) => d<Checklist>(http.post(`/compliance/checklists/${requestId}/generate`)),
  patchItem: (itemId: number, status: ChecklistItemStatus, note?: string, documentId?: number) =>
    d<ChecklistItem>(http.patch(`/compliance/checklists/${itemId}`, { status, note, documentId })),
  notifyMissing: (requestId: number) => d<{ items: number }>(http.post(`/compliance/checklists/${requestId}/notify-missing`)),
  openItems: () => d<OpenItem[]>(http.get('/compliance/open-items')),
};

// ---------------------------------------------------------------- quotes & payments
export const quoteApi = {
  list: (p: { status?: QuoteStatus[]; page?: number; size?: number } = {}) => d<Page<Quote>>(http.get(`/quotes${qs(p)}`)),
  get: (id: number) => d<Quote>(http.get(`/quotes/${id}`)),
  createFinal: (requestId: number) => d<Quote>(http.post('/quotes', { requestId })),
  update: (id: number, p: { validUntil?: string; notes?: string; items: QuoteItemInput[] }) => d<Quote>(http.put(`/quotes/${id}`, p)),
  send: (id: number) => d<Quote>(http.post(`/quotes/${id}/send`)),
  accept: (id: number, note?: string) => d<Quote>(http.post(`/quotes/${id}/accept`, { note })),
  reject: (id: number, note: string) => d<Quote>(http.post(`/quotes/${id}/reject`, { note })),
  pricingRates: () => d<PricingRate[]>(http.get('/pricing-rates')),
};
export const paymentApi = {
  invoices: (p: { status?: InvoiceStatus[]; page?: number; size?: number } = {}) => d<Page<Invoice>>(http.get(`/invoices${qs(p)}`)),
  invoice: (id: number) => d<Invoice>(http.get(`/invoices/${id}`)),
  createInvoice: (quoteId: number) => d<Invoice>(http.post('/invoices', { quoteId })),
  simulate: (p: { invoiceId: number; amount: number; method: PaymentMethod; cardLast4?: string; note?: string }) =>
    d<PaymentItem>(http.post('/payments/simulate', p)),
  payments: (p: { page?: number; size?: number } = {}) => d<Page<PaymentItem>>(http.get(`/payments${qs(p)}`)),
  payment: (id: number) => d<PaymentItem>(http.get(`/payments/${id}`)),
};

// ---------------------------------------------------------------- fleet & routes
export const vehicleApi = {
  list: (p: { status?: VehicleStatus[]; type?: VehicleType[]; q?: string } = {}) => d<Vehicle[]>(http.get(`/vehicles${qs(p)}`)),
  available: (from: string, to: string, minCapacity = 1, excludeTripId?: number) =>
    d<Vehicle[]>(http.get(`/vehicles/available${qs({ from, to, minCapacity, excludeTripId })}`)),
  get: (id: number) => d<Vehicle>(http.get(`/vehicles/${id}`)),
  create: (p: VehiclePayload) => d<Vehicle>(http.post('/vehicles', p)),
  update: (id: number, p: VehiclePayload) => d<Vehicle>(http.put(`/vehicles/${id}`, p)),
  setStatus: (id: number, status: VehicleStatus, note?: string) => d<Vehicle>(http.patch(`/vehicles/${id}/status`, { status, note })),
  archive: (id: number) => d<Vehicle>(http.delete(`/vehicles/${id}`)),
};
export const routeApi = {
  list: (p: { status?: RouteStatus[]; origin?: string; destination?: string; q?: string } = {}) =>
    d<RouteSummary[]>(http.get(`/routes${qs(p)}`)),
  get: (id: number) => d<RouteDetail>(http.get(`/routes/${id}`)),
  create: (p: RoutePayload) => d<RouteDetail>(http.post('/routes', p)),
  update: (id: number, p: RoutePayload) => d<RouteDetail>(http.put(`/routes/${id}`, p)),
  addStop: (id: number, p: RouteStopPayload) => d<RouteDetail>(http.post(`/routes/${id}/stops`, p)),
  updateStop: (id: number, stopId: number, p: RouteStopPayload) => d<RouteDetail>(http.put(`/routes/${id}/stops/${stopId}`, p)),
  deleteStop: (id: number, stopId: number) => d<RouteDetail>(http.delete(`/routes/${id}/stops/${stopId}`)),
};

// ---------------------------------------------------------------- trips & tracking
export const tripApi = {
  list: (p: { status?: TripStatus[]; q?: string; page?: number; size?: number } = {}) => d<Page<TripSummary>>(http.get(`/trips${qs(p)}`)),
  get: (id: number) => d<TripDetail>(http.get(`/trips/${id}`)),
  create: (p: CreateTripPayload) => d<TripDetail>(http.post('/trips', p)),
  updatePlan: (id: number, p: { plannedDeparture: string; plannedArrival: string; transportInstructions?: string; notes?: string }) =>
    d<TripDetail>(http.put(`/trips/${id}`, p)),
  assignVehicle: (id: number, vehicleId: number) => d<TripDetail>(http.post(`/trips/${id}/assign-vehicle`, { vehicleId })),
  assignStaff: (id: number, staffId: number, role: StaffRole) => d<TripDetail>(http.post(`/trips/${id}/assign-staff`, { staffId, role })),
  unassignStaff: (id: number, staffId: number) => d<TripDetail>(http.delete(`/trips/${id}/staff/${staffId}`)),
  cancel: (id: number, note: string) => d<TripDetail>(http.post(`/trips/${id}/cancel`, { note })),
  start: (id: number) => d<TripDetail>(http.post(`/trips/${id}/start`)),
  updateStatus: (id: number, p: { status: TripStatus; note?: string; location?: string; delayMinutes?: number }) =>
    d<TripDetail>(http.patch(`/trips/${id}/status`, p)),
  checkpoint: (id: number, p: { routeStopId?: number; eventType: TrackingEventType; location?: string; delayMinutes?: number; note?: string; status?: TripStatus }) =>
    d<TrackingEventItem>(http.post(`/trips/${id}/checkpoints`, p)),
  tracking: (id: number) => d<Tracking>(http.get(`/trips/${id}/tracking`)),
  timeline: (id: number) => d<TimelineItem[]>(http.get(`/trips/${id}/timeline`)),
  preChecks: (id: number) => d<PreCheck[]>(http.get(`/trips/${id}/pre-transport-checks`)),
  observations: (id: number) => d<Observation[]>(http.get(`/trips/${id}/health-observations`)),
  arrivalChecks: (id: number) => d<ArrivalCheck[]>(http.get(`/trips/${id}/arrival-checks`)),
  handovers: (id: number) => d<Handover[]>(http.get(`/trips/${id}/handovers`)),
};

// ---------------------------------------------------------------- health
export const healthApi = {
  preCheck: (p: { tripId: number; horseId: number; temperatureC?: number; generalCondition: HorseCondition; identityVerified: boolean; documentsPresent: boolean; loadingOk: boolean; fitForTravelObserved: boolean; notes?: string }) =>
    d<PreCheck>(http.post('/health-checks/pre-transport', p)),
  observe: (p: { tripId: number; horseId: number; location?: string; appetite: IntakeLevel; waterIntake: IntakeLevel; behavior: HorseBehavior; stressLevel: StressLevel; movement: MovementCondition; visibleInjury: boolean; injuryDescription?: string; temperatureC?: number; severity?: ObservationSeverity; notes?: string; reportIncident?: boolean }) =>
    d<Observation>(http.post('/health-observations', p)),
  arrival: (p: { tripId: number; horseId: number; condition: HorseCondition; visibleInjury: boolean; notes?: string }) =>
    d<ArrivalCheck>(http.post('/health-checks/arrival', p)),
  consult: (p: { incidentId?: number; tripId?: number; horseId?: number; veterinarianName: string; clinic?: string; contact?: string; consultedAt?: string; advice: string; recommendedAction?: OperationalDecision }) =>
    d<VetConsultation>(http.post('/veterinary-consultations', p)),
  consultation: (id: number) => d<VetConsultation>(http.get(`/veterinary-consultations/${id}`)),
  uploadReport: (id: number, file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    return d<VetConsultation>(http.post(`/veterinary-consultations/${id}/report`, fd));
  },
  reportPath: (id: number) => `/veterinary-consultations/${id}/report`,
};

// ---------------------------------------------------------------- attachments
export const attachmentApi = {
  upload: (refType: AttachmentRefType, refId: number, file: File, caption?: string) => {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('refType', refType);
    fd.append('refId', String(refId));
    if (caption) fd.append('caption', caption);
    return d<Attachment>(http.post('/attachments', fd));
  },
  list: (refType: AttachmentRefType, refId: number) => d<Attachment[]>(http.get(`/attachments${qs({ refType, refId })}`)),
  filePath: (id: number) => `/attachments/${id}/file`,
};

// ---------------------------------------------------------------- incidents & handover
export const incidentApi = {
  list: (p: { status?: IncidentStatus[]; severity?: IncidentSeverity[]; tripId?: number; page?: number; size?: number } = {}) =>
    d<Page<IncidentSummary>>(http.get(`/incidents${qs(p)}`)),
  get: (id: number) => d<IncidentDetail>(http.get(`/incidents/${id}`)),
  report: (p: { tripId: number; horseId?: number; incidentType: IncidentType; severity: IncidentSeverity; title: string; description: string; location?: string }) =>
    d<IncidentDetail>(http.post('/incidents', p)),
  addAction: (id: number, p: { actionType: IncidentActionType; note: string; decision?: OperationalDecision; newRouteId?: number; extraCost?: number; currency?: string; consultationId?: number }) =>
    d<IncidentDetail>(http.post(`/incidents/${id}/actions`, p)),
  changeStatus: (id: number, status: IncidentStatus, note?: string) => d<IncidentDetail>(http.patch(`/incidents/${id}/status`, { status, note })),
};
export const handoverApi = {
  list: (status?: HandoverStatus[]) => d<Handover[]>(http.get(`/handovers${qs({ status })}`)),
  get: (id: number) => d<Handover>(http.get(`/handovers/${id}`)),
  create: (p: { tripId: number; horseId: number; destination?: string; receiverName: string; receiverContact?: string; condition: HorseCondition; documentsHandedOver: boolean; notes?: string }) =>
    d<Handover>(http.post('/handovers', p)),
  update: (id: number, p: { tripId: number; horseId: number; destination?: string; receiverName: string; receiverContact?: string; condition: HorseCondition; documentsHandedOver: boolean; notes?: string }) =>
    d<Handover>(http.put(`/handovers/${id}`, p)),
  confirm: (id: number, accepted: boolean, comment?: string) => d<Handover>(http.post(`/handovers/${id}/confirm`, { accepted, comment })),
};

// ---------------------------------------------------------------- claims
export const claimApi = {
  list: (p: { status?: ClaimStatus[]; page?: number; size?: number } = {}) => d<Page<ClaimSummary>>(http.get(`/claims${qs(p)}`)),
  get: (id: number) => d<ClaimDetail>(http.get(`/claims/${id}`)),
  create: (p: { requestId: number; tripId?: number; incidentId?: number; horseId?: number; claimType: ClaimType; claimedAmount: number; description: string }) =>
    d<ClaimDetail>(http.post('/claims', p)),
  addEvidence: (id: number, file: File, evidenceType: EvidenceType, notes?: string) => {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('evidenceType', evidenceType);
    if (notes) fd.append('notes', notes);
    return d<ClaimDetail>(http.post(`/claims/${id}/evidence`, fd));
  },
  evidencePath: (id: number, evidenceId: number) => `/claims/${id}/evidence/${evidenceId}/file`,
  review: (id: number, p: { assessmentNote: string; recommendation: ClaimRecommendation; recommendedAmount?: number }) =>
    d<ClaimDetail>(http.post(`/claims/${id}/review`, p)),
  approve: (id: number, approvedAmount: number, decisionNote: string) =>
    d<ClaimDetail>(http.post(`/claims/${id}/approve`, { approvedAmount, decisionNote })),
  reject: (id: number, decisionNote: string) => d<ClaimDetail>(http.post(`/claims/${id}/reject`, { decisionNote })),
  settle: (id: number, amount: number, method?: PaymentMethod, note?: string) =>
    d<ClaimDetail>(http.post(`/claims/${id}/settlement`, { amount, method, note })),
  close: (id: number, note?: string) => d<ClaimDetail>(http.post(`/claims/${id}/close`, { note })),
};

// ---------------------------------------------------------------- notifications & reports
export const notificationApi = {
  list: (p: { unreadOnly?: boolean; page?: number; size?: number } = {}) => d<Page<NotificationItem>>(http.get(`/notifications${qs(p)}`)),
  unreadCount: () => d<{ count: number }>(http.get('/notifications/unread-count')),
  markRead: (id: number) => d<NotificationItem>(http.patch(`/notifications/${id}/read`)),
  readAll: () => d<{ updated: number }>(http.post('/notifications/read-all')),
};
export const reportApi = {
  dashboard: () => d<Dashboard>(http.get('/dashboard')),
  revenue: () => d<RevenueReport>(http.get('/reports/revenue')),
  transport: () => d<TransportReport>(http.get('/reports/transport')),
  incidents: () => d<IncidentReport>(http.get('/reports/incidents')),
  claims: () => d<ClaimReport>(http.get('/reports/claims')),
  kpi: () => d<KpiReport>(http.get('/reports/kpi')),
};

export type { HorseSex };
