// DTO TypeScript – đồng bộ với các record DTO của backend (xem docs/API_CONTRACT.md).
import type {
  AttachmentRefType, ChecklistItemStatus, ClaimRecommendation, ClaimStatus, ClaimType, ComplianceCategory,
  CostCategory, CustomerType, DocumentReviewStatus, DocumentType, EvidenceType, HandoverStatus, HorseBehavior,
  HorseCondition, HorseSex, HorseStatus, IncidentActionType, IncidentSeverity, IncidentStatus, IncidentType,
  IntakeLevel, InvoiceStatus, MovementCondition, MovementType, NotificationType, ObservationSeverity,
  OperationalDecision, PaymentMethod, PaymentStatus, PricingUnit, QuoteStatus, QuoteType, RequestStatus, Role,
  RouteStatus, SettlementStatus, StaffRole, StopType, StressLevel, TrackingEventType, TransportMode,
  TransportPurpose, TripStatus, VehicleStatus, VehicleType,
} from './enums';

export type ID = number;
/** BigDecimal được Jackson serialize thành số JSON. */
export type Money = number;

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors?: { field: string; message: string }[];
}

// ---------------------------------------------------------------- auth / user
export interface UserSummary {
  id: ID; email: string; fullName: string; phone?: string; role: Role; enabled: boolean;
  createdAt?: string; lastLoginAt?: string;
}
export interface AuthResponse { accessToken: string; tokenType: string; expiresIn: number; user: UserSummary }
export interface RegisterPayload {
  email: string; password: string; fullName: string; phone?: string; companyName?: string;
  customerType?: CustomerType; country?: string; acceptTerms: boolean;
}
export interface CustomerProfile {
  userId: ID; email: string; fullName: string; phone?: string; companyName?: string; customerType: CustomerType;
  address?: string; country?: string; taxCode?: string; preferredLanguage: string; createdAt?: string;
}
export interface Employee {
  userId: ID; email: string; fullName: string; phone?: string; role: Role; enabled: boolean; employeeCode?: string;
  department?: string; staffRole?: StaffRole; licenseNumber?: string; licenseExpiry?: string;
  airEscortCertified: boolean; emergencyContact?: string; baseLocation?: string; activeAssignments: number;
}

// ---------------------------------------------------------------- horse
export interface Horse {
  id: ID; ownerId: ID; ownerName: string; registeredName: string; breed?: string; dateOfBirth?: string;
  ageYears?: number; sex?: HorseSex; color?: string; heightCm?: number; microchipNumber: string;
  passportNumber: string; passportCountry?: string; specialRequirements?: string; notes?: string;
  status: HorseStatus; createdAt?: string; updatedAt?: string;
}
export interface HorsePayload {
  registeredName: string; breed?: string; dateOfBirth?: string; sex?: HorseSex; color?: string; heightCm?: number;
  microchipNumber: string; passportNumber: string; passportCountry?: string; specialRequirements?: string; notes?: string;
}
export interface StallType {
  id: ID; code: string; name: string; widthCm: number; description?: string; surcharge: Money; currency: string;
}

// ---------------------------------------------------------------- request
export interface RequestHorseInput { horseId: ID; stallTypeId: ID; declaredValue: number; notes?: string }
export interface RequestPayload {
  originCountry: string; originAddress: string; destinationCountry: string; destinationAddress: string;
  requestedDepartureDate: string; returnDate?: string | null; movementType: MovementType; purpose: TransportPurpose;
  transportMode: TransportMode; currency?: string; specialRequirements?: string; notes?: string;
  horses: RequestHorseInput[];
}
export interface RequestHorse {
  id: ID; horseId: ID; horseName: string; microchipNumber: string; passportNumber: string; breed?: string;
  stallTypeId: ID; stallTypeCode: string; stallTypeName: string; stallWidthCm: number; stallSurcharge: Money;
  declaredValue: Money; notes?: string;
}
export interface StatusHistory {
  fromStatus?: RequestStatus; toStatus: RequestStatus; changedBy: string; changedByRole: Role; note?: string;
  changedAt: string;
}
export interface ComplianceSummary {
  total: number; approved: number; pending: number; missing: number; problems: number; complete: boolean;
}
export interface QuoteRef { id: ID; quoteNumber: string; versionNo: number; status: QuoteStatus; totalAmount: Money; currency: string }
export interface TripRef { id: ID; tripNumber: string; status: TripStatus; plannedDeparture: string }
export interface RequestSummary {
  id: ID; requestNumber: string; customerId: ID; customerName: string; originCountry: string; originAddress: string;
  destinationCountry: string; destinationAddress: string; requestedDepartureDate: string; movementType: MovementType;
  purpose: TransportPurpose; transportMode: TransportMode; status: RequestStatus; horseCount: number;
  horseNames: string[]; declaredValueTotal: Money; currency: string; createdAt?: string; submittedAt?: string;
}
export interface RequestDetail {
  id: ID; requestNumber: string; customerId: ID; customerName: string; customerEmail: string; customerPhone?: string;
  originCountry: string; originAddress: string; destinationCountry: string; destinationAddress: string;
  requestedDepartureDate: string; returnDate?: string; movementType: MovementType; purpose: TransportPurpose;
  transportMode: TransportMode; declaredValueTotal: Money; currency: string; specialRequirements?: string;
  notes?: string; status: RequestStatus; lastDecisionNote?: string; createdAt?: string; submittedAt?: string;
  approvedAt?: string; completedAt?: string; horses: RequestHorse[]; history: StatusHistory[];
  compliance: ComplianceSummary; estimate?: QuoteRef; latestQuote?: QuoteRef; invoiceId?: ID; trips: TripRef[];
  allowedActions: string[];
}

// ---------------------------------------------------------------- documents / compliance
export interface DocumentItem {
  id: ID; documentType: DocumentType; title?: string; documentNumber?: string; issuingCountry?: string;
  issueDate?: string; expiryDate?: string; reviewStatus: DocumentReviewStatus; reviewNote?: string;
  reviewerName?: string; reviewedAt?: string; requestId?: ID; requestNumber?: string; horseId?: ID;
  horseName?: string; customerName?: string; fileName: string; contentType: string; sizeBytes: number;
  uploadedByName: string; uploadedAt?: string; daysToExpiry?: number; expired: boolean;
}
export interface Requirement {
  id: ID; code: string; originCountry?: string; destinationCountry?: string; movementType?: MovementType;
  purpose?: TransportPurpose; category: ComplianceCategory; documentType: DocumentType; title: string;
  description?: string; validityDays?: number; perHorse: boolean; mandatory: boolean; effectiveFrom: string;
  effectiveTo?: string; active: boolean;
}
export type RequirementPayload = Omit<Requirement, 'id' | 'perHorse' | 'mandatory' | 'active'> & {
  perHorse?: boolean; mandatory?: boolean; active?: boolean;
};
export interface ChecklistItem {
  id: ID; requirementId: ID; requirementCode: string; title: string; description?: string;
  category: ComplianceCategory; documentType: DocumentType; mandatory: boolean; validityDays?: number;
  horseId?: ID; horseName?: string; status: ChecklistItemStatus; documentId?: ID; documentFileName?: string;
  documentStatus?: DocumentReviewStatus; documentExpiryDate?: string; reviewerNote?: string; checkedBy?: string;
  checkedAt?: string; dueDate?: string;
}
export interface Checklist {
  requestId: ID; requestNumber: string; requestStatus: RequestStatus; route: string; requestedDepartureDate: string;
  summary: ComplianceSummary; items: ChecklistItem[]; disclaimer: string;
}
export interface OpenItem {
  itemId: ID; requestId: ID; requestNumber: string; customerName: string; requestedDepartureDate: string;
  title: string; documentType: DocumentType; category: ComplianceCategory; horseName?: string;
  status: ChecklistItemStatus; reviewerNote?: string; dueDate?: string;
}
export interface Attachment {
  id: ID; refType: AttachmentRefType; refId: ID; caption?: string; fileName: string; contentType: string;
  sizeBytes: number; createdAt?: string;
}
export interface AttachmentRef { id: ID; fileName: string; contentType: string }

// ---------------------------------------------------------------- quotes / payments
export interface QuoteItem {
  id: ID; category: CostCategory; description: string; quantity: number; unitPrice: Money; amount: Money;
  estimatedAmount?: Money; confirmed: boolean; source?: string; sortOrder: number;
}
export interface Quote {
  id: ID; quoteNumber: string; requestId: ID; requestNumber: string; requestStatus: RequestStatus; customerId: ID;
  customerName: string; route: string; versionNo: number; quoteType: QuoteType; status: QuoteStatus;
  currency: string; subtotal: Money; discountTotal: Money; totalAmount: Money; estimatedTotal?: Money;
  declaredValueTotal: Money; validUntil?: string; expired: boolean; notes?: string; createdBy: string;
  createdAt?: string; updatedAt?: string; sentAt?: string; acceptedAt?: string; rejectedAt?: string;
  customerResponseNote?: string; invoiceId?: ID; items: QuoteItem[]; allowedActions: string[];
}
export interface QuoteItemInput {
  category: CostCategory; description: string; quantity: number; unitPrice: number; confirmed?: boolean;
  source?: string; estimatedAmount?: number | null;
}
export interface PricingRate {
  id: ID; code: string; category: CostCategory; description: string; unit: PricingUnit; unitPrice: number;
  currency: string; transportMode?: TransportMode; destinationCountry?: string; active: boolean;
}
export interface PaymentItem {
  id: ID; paymentReference: string; invoiceId: ID; invoiceNumber: string; requestNumber: string; customerName: string;
  amount: Money; currency: string; method: PaymentMethod; status: PaymentStatus; cardLast4?: string;
  paidAt?: string; note?: string; createdAt?: string;
}
export interface Invoice {
  id: ID; invoiceNumber: string; quoteId: ID; quoteNumber: string; requestId: ID; requestNumber: string;
  customerId: ID; customerName: string; issueDate: string; dueDate: string; currency: string; totalAmount: Money;
  paidAmount: Money; outstandingAmount: Money; depositPercent: number; depositAmount: Money;
  minimumNextPayment: Money; status: InvoiceStatus; overdue: boolean; payments: PaymentItem[];
}

// ---------------------------------------------------------------- fleet / route / trip
export interface VehicleTripRef {
  tripId: ID; tripNumber: string; status: TripStatus; plannedDeparture: string; plannedArrival: string; routeName: string;
}
export interface Vehicle {
  id: ID; vehicleCode: string; plateNumber: string; vehicleType: VehicleType; model?: string; capacityHorses: number;
  status: VehicleStatus; currentLocation?: string; climateControlled: boolean; cameraMonitoring: boolean;
  lastMaintenanceDate?: string; nextMaintenanceDate?: string; maintenanceDue: boolean; notes?: string;
  archived: boolean; updatedAt?: string; upcomingTrips: VehicleTripRef[];
}
export interface VehiclePayload {
  vehicleCode: string; plateNumber: string; vehicleType: VehicleType; model?: string; capacityHorses: number;
  currentLocation?: string; climateControlled?: boolean; cameraMonitoring?: boolean;
  lastMaintenanceDate?: string; nextMaintenanceDate?: string; notes?: string;
}
export interface RouteStop {
  id: ID; stopOrder: number; stopType: StopType; name: string; address?: string; country?: string;
  latitude?: number; longitude?: number; plannedArrivalOffsetMin?: number; plannedDurationMin?: number;
  instructions?: string;
}
export type RouteStopPayload = Omit<RouteStop, 'id' | 'stopOrder'> & { stopOrder?: number };
export interface RouteSummary {
  id: ID; routeCode: string; name: string; originCountry: string; originLocation: string; destinationCountry: string;
  destinationLocation: string; transportMode: TransportMode; estimatedDistanceKm?: number;
  estimatedDurationHours?: number; status: RouteStatus; alternativeOfId?: ID; stopCount: number; updatedAt?: string;
}
export interface RouteDetail extends Omit<RouteSummary, 'stopCount'> {
  alternativeOfName?: string; notes?: string; stops: RouteStop[]; alternatives: RouteSummary[]; tripCount: number;
}
export interface RoutePayload {
  routeCode?: string; name: string; originCountry: string; originLocation: string; destinationCountry: string;
  destinationLocation: string; transportMode: TransportMode; estimatedDistanceKm?: number;
  estimatedDurationHours?: number; status?: RouteStatus; alternativeOfId?: ID | null; notes?: string;
  stops?: RouteStopPayload[];
}
export interface StaffMember { assignmentId: ID; staffId: ID; fullName: string; phone?: string; role: StaffRole; assignedAt: string }
export interface TripHorse {
  horseId: ID; name: string; microchipNumber: string; breed?: string; stallTypeName: string; stallWidthCm: number;
  specialRequirements?: string; preCheckDone: boolean; fitForTravelObserved?: boolean; arrivalCheckDone: boolean;
  arrivalCondition?: HorseCondition; handoverId?: ID; handoverStatus?: HandoverStatus;
}
export interface VehicleRef { id: ID; vehicleCode: string; plateNumber: string; vehicleType: VehicleType; capacityHorses: number; climateControlled: boolean }
export interface IncidentRef { id: ID; incidentNumber: string; title: string; severity: IncidentSeverity; status: IncidentStatus; reportedAt: string }
export interface TripSummary {
  id: ID; tripNumber: string; requestId: ID; requestNumber: string; customerName: string; routeId: ID;
  routeName: string; originLocation: string; destinationLocation: string; transportMode: TransportMode;
  vehicleCode?: string; plannedDeparture: string; plannedArrival: string; actualDeparture?: string;
  actualArrival?: string; status: TripStatus; currentLocation?: string; operationBlocked: boolean;
  staffNames: string[];
}
export interface TripDetail {
  id: ID; tripNumber: string; requestId: ID; requestNumber: string; customerId: ID; customerName: string;
  customerPhone?: string; routeId: ID; routeCode: string; routeName: string; transportMode: TransportMode;
  originLocation: string; destinationLocation: string; destinationAddress: string; plannedDeparture: string;
  plannedArrival: string; actualDeparture?: string; actualArrival?: string; status: TripStatus;
  currentLocation?: string; currentStopId?: ID; operationBlocked: boolean; transportInstructions?: string;
  notes?: string; coordinatorName: string; vehicle?: VehicleRef; staff: StaffMember[]; horses: TripHorse[];
  stops: RouteStop[]; incidents: IncidentRef[]; totalDelayMinutes: number; nextStatuses: TripStatus[];
  allowedActions: string[]; createdAt?: string;
}
export interface CreateTripPayload {
  requestId: ID; routeId: ID; plannedDeparture: string; plannedArrival: string; vehicleId?: ID | null;
  transportInstructions?: string; notes?: string; staff?: { staffId: ID; role: StaffRole }[];
}
export interface StopProgress {
  stop: RouteStop; plannedArrival?: string; actualArrival?: string; actualDeparture?: string;
  state: 'DONE' | 'CURRENT' | 'UPCOMING';
}
export interface TrackingEventItem {
  id: ID; eventType: TrackingEventType; eventTime: string; location?: string; latitude?: number; longitude?: number;
  tripStatus: TripStatus; delayMinutes?: number; note?: string; recordedBy: string; routeStopId?: ID;
  routeStopName?: string;
}
export interface Tracking {
  tripId: ID; tripNumber: string; status: TripStatus; currentLocation?: string; currentStopId?: ID;
  progressPercent: number; plannedArrival: string; estimatedArrival: string; totalDelayMinutes: number;
  lastUpdate?: string; operationBlocked: boolean; stops: StopProgress[]; events: TrackingEventItem[];
}
export interface TimelineItem {
  time: string; kind: string; title: string; description?: string; status?: string; tone: string; actor?: string; refId?: ID;
}

// ---------------------------------------------------------------- health
export interface PreCheck {
  id: ID; tripId: ID; horseId: ID; horseName: string; temperatureC?: number; generalCondition: HorseCondition;
  identityVerified: boolean; documentsPresent: boolean; loadingOk: boolean; fitForTravelObserved: boolean;
  notes?: string; checkedBy: string; checkedAt: string;
}
export interface Observation {
  id: ID; tripId: ID; tripNumber: string; horseId: ID; horseName: string; author: string; observedAt: string;
  location?: string; appetite: IntakeLevel; waterIntake: IntakeLevel; behavior: HorseBehavior;
  stressLevel: StressLevel; movement: MovementCondition; visibleInjury: boolean; injuryDescription?: string;
  temperatureC?: number; severity: ObservationSeverity; notes?: string; incidentId?: ID; photos: AttachmentRef[];
}
export interface ArrivalCheck {
  id: ID; tripId: ID; horseId: ID; horseName: string; condition: HorseCondition; visibleInjury: boolean;
  notes?: string; checkedBy: string; checkedAt: string;
}
export interface VetConsultation {
  id: ID; incidentId?: ID; incidentNumber?: string; tripId?: ID; tripNumber?: string; horseId?: ID;
  horseName?: string; veterinarianName: string; clinic?: string; contact?: string; consultedAt: string;
  advice: string; recommendedAction?: OperationalDecision; reportFileId?: ID; reportFileName?: string;
  recordedBy: string; createdAt?: string;
}
export interface HorseHealthHistory {
  horseId: ID; horseName: string; preTransportChecks: PreCheck[]; observations: Observation[];
  arrivalChecks: ArrivalCheck[]; consultations: VetConsultation[]; disclaimer: string;
}

// ---------------------------------------------------------------- incident / handover / claim
export interface IncidentAction {
  id: ID; actionType: IncidentActionType; decision?: OperationalDecision; note: string; performedBy: string;
  performedByRole: Role; performedAt: string; consultationId?: ID; newRouteId?: ID; newRouteName?: string;
  extraCost?: Money; currency?: string;
}
export interface IncidentSummary {
  id: ID; incidentNumber: string; tripId: ID; tripNumber: string; requestId: ID; requestNumber: string;
  customerName: string; horseId?: ID; horseName?: string; incidentType: IncidentType; severity: IncidentSeverity;
  title: string; status: IncidentStatus; decision?: OperationalDecision; location?: string; reportedAt: string;
  reportedBy: string; tripBlocked: boolean;
}
export interface IncidentDetail extends IncidentSummary {
  tripStatus: TripStatus; tripLocation?: string; routeName: string; routeId: ID; description: string;
  decidedBy?: string; decidedAt?: string; resolvedAt?: string; resolutionNote?: string;
  actions: IncidentAction[]; consultations: VetConsultation[]; attachments: AttachmentRef[];
  rerouteOptions: { id: ID; routeCode: string; name: string }[]; allowedActions: string[];
}
export interface Handover {
  id: ID; handoverNumber: string; tripId: ID; tripNumber: string; tripStatus: TripStatus; requestId: ID;
  requestNumber: string; horseId: ID; horseName: string; microchipNumber: string; destination: string;
  handoverTime: string; receiverName: string; receiverContact?: string; condition: HorseCondition;
  documentsHandedOver: boolean; notes?: string; status: HandoverStatus; arrivalCheckId?: ID;
  arrivalCondition?: HorseCondition; createdBy: string; confirmedBy?: string; confirmedAt?: string;
  customerComment?: string; evidence: AttachmentRef[]; allowedActions: string[]; updatedAt?: string;
}
export interface ClaimSummary {
  id: ID; claimNumber: string; requestId: ID; requestNumber: string; customerName: string; horseName?: string;
  incidentNumber?: string; claimType: ClaimType; claimedAmount: Money; approvedAmount?: Money; currency: string;
  status: ClaimStatus; submittedAt: string; decidedAt?: string;
}
export interface ClaimDetail {
  id: ID; claimNumber: string; requestId: ID; requestNumber: string; tripId?: ID; tripNumber?: string;
  incidentId?: ID; incidentNumber?: string; incidentTitle?: string; incidentSeverity?: IncidentSeverity;
  horseId?: ID; horseName?: string; declaredValueReference?: Money; customerId: ID; customerName: string;
  claimType: ClaimType; claimedAmount: Money; approvedAmount?: Money; settledAmount: Money; currency: string;
  description: string; status: ClaimStatus; submittedAt: string; reviewedBy?: string; decidedAt?: string;
  decisionNote?: string; closedAt?: string;
  evidence: { id: ID; evidenceType: EvidenceType; notes?: string; fileName: string; contentType: string; sizeBytes: number; uploadedBy: string; uploadedAt: string }[];
  assessments: { id: ID; assessmentNote: string; recommendation: ClaimRecommendation; recommendedAmount?: Money; assessedBy: string; assessedAt: string }[];
  settlements: { id: ID; settlementReference: string; amount: Money; currency: string; status: SettlementStatus; settledAt: string; note?: string; settledBy: string }[];
  allowedActions: string[];
}

// ---------------------------------------------------------------- notifications / reports
export interface NotificationItem {
  id: ID; title: string; message: string; type: NotificationType; referenceType?: string; referenceId?: ID;
  read: boolean; readAt?: string; createdAt: string;
}
export interface Point { label: string; value: number; value2?: number }
export interface DashboardCard { key: string; label: string; value: string; hint?: string; tone: string; link?: string }
export interface DashboardItem { id: ID; title: string; subtitle?: string; status?: string; meta?: string; link?: string }
export interface DashboardSection { key: string; title: string; items: DashboardItem[]; emptyText: string; link?: string }
export interface Dashboard {
  role: Role; greeting: string; cards: DashboardCard[]; sections: DashboardSection[]; chart: Point[]; chartTitle?: string;
}
export interface RevenueReport {
  currency: string; invoicedTotal: Money; collectedTotal: Money; outstandingTotal: Money; acceptedQuoteTotal: Money;
  invoiceCount: number; paidInvoiceCount: number; monthly: Point[]; costBreakdown: Point[]; claimSettledTotal: Money;
  note: string;
}
export interface TransportReport {
  totalTrips: number; activeTrips: number; deliveredTrips: number; cancelledTrips: number; horsesTransported: number;
  tripsByStatus: Point[]; tripsByMonth: Point[]; requestsByStatus: Point[]; topRoutes: Point[]; byTransportMode: Point[];
}
export interface IncidentReport {
  total: number; open: number; critical: number; avgResolutionHours: number; bySeverity: Point[]; byType: Point[];
  byMonth: Point[]; byDecision: Point[]; totalDelayMinutes: number;
}
export interface ClaimReport {
  total: number; open: number; claimedTotal: Money; approvedTotal: Money; settledTotal: Money;
  approvalRatioPercent: number; byStatus: Point[]; byType: Point[];
}
export interface KpiReport { kpis: { key: string; label: string; value: number; unit: string; description: string }[]; note: string }
