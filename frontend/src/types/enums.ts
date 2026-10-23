// Sinh tự động từ mô hình dữ liệu backend – đồng bộ với các Java enum.
// Không sửa tay giá trị enum; nhãn tiếng Việt có thể chỉnh nếu cần.

export type Role = 'CUSTOMER' | 'LOGISTICS_MANAGER' | 'TRANSPORT_SPECIALIST' | 'FLEET_ROUTE_COORDINATOR' | 'DRIVER_ESCORT';
export const RoleValues: Role[] = ['CUSTOMER', 'LOGISTICS_MANAGER', 'TRANSPORT_SPECIALIST', 'FLEET_ROUTE_COORDINATOR', 'DRIVER_ESCORT'];
export const RoleLabels: Record<Role, string> = {
  CUSTOMER: 'Khách hàng',
  LOGISTICS_MANAGER: 'Quản lý Logistics',
  TRANSPORT_SPECIALIST: 'Chuyên viên vận chuyển',
  FLEET_ROUTE_COORDINATOR: 'Điều phối đội xe & tuyến',
  DRIVER_ESCORT: 'Tài xế / Người áp tải',
};

export type CustomerType = 'INDIVIDUAL' | 'STABLE' | 'COMPANY' | 'AGENT';
export const CustomerTypeValues: CustomerType[] = ['INDIVIDUAL', 'STABLE', 'COMPANY', 'AGENT'];
export const CustomerTypeLabels: Record<CustomerType, string> = {
  INDIVIDUAL: 'Cá nhân',
  STABLE: 'Trại ngựa / chuồng đua',
  COMPANY: 'Doanh nghiệp',
  AGENT: 'Đại lý',
};

export type StaffRole = 'DRIVER' | 'ESCORT' | 'TEAM_LEADER';
export const StaffRoleValues: StaffRole[] = ['DRIVER', 'ESCORT', 'TEAM_LEADER'];
export const StaffRoleLabels: Record<StaffRole, string> = {
  DRIVER: 'Tài xế',
  ESCORT: 'Người áp tải',
  TEAM_LEADER: 'Trưởng nhóm',
};

export type HorseSex = 'STALLION' | 'MARE' | 'GELDING' | 'COLT' | 'FILLY';
export const HorseSexValues: HorseSex[] = ['STALLION', 'MARE', 'GELDING', 'COLT', 'FILLY'];
export const HorseSexLabels: Record<HorseSex, string> = {
  STALLION: 'Ngựa đực giống',
  MARE: 'Ngựa cái',
  GELDING: 'Ngựa thiến',
  COLT: 'Ngựa đực non',
  FILLY: 'Ngựa cái non',
};

export type HorseStatus = 'ACTIVE' | 'IN_TRANSIT' | 'ARCHIVED';
export const HorseStatusValues: HorseStatus[] = ['ACTIVE', 'IN_TRANSIT', 'ARCHIVED'];
export const HorseStatusLabels: Record<HorseStatus, string> = {
  ACTIVE: 'Đang hoạt động',
  IN_TRANSIT: 'Đang vận chuyển',
  ARCHIVED: 'Đã lưu trữ',
};

export type MovementType = 'TEMPORARY' | 'PERMANENT';
export const MovementTypeValues: MovementType[] = ['TEMPORARY', 'PERMANENT'];
export const MovementTypeLabels: Record<MovementType, string> = {
  TEMPORARY: 'Tạm thời',
  PERMANENT: 'Vĩnh viễn',
};

export type TransportPurpose = 'RACING' | 'COMPETITION' | 'TRAINING' | 'BREEDING' | 'RELOCATION' | 'OTHER';
export const TransportPurposeValues: TransportPurpose[] = ['RACING', 'COMPETITION', 'TRAINING', 'BREEDING', 'RELOCATION', 'OTHER'];
export const TransportPurposeLabels: Record<TransportPurpose, string> = {
  RACING: 'Đua',
  COMPETITION: 'Thi đấu',
  TRAINING: 'Huấn luyện',
  BREEDING: 'Phối giống',
  RELOCATION: 'Chuyển nơi ở',
  OTHER: 'Khác',
};

export type TransportMode = 'ROAD' | 'AIR' | 'ROAD_AIR';
export const TransportModeValues: TransportMode[] = ['ROAD', 'AIR', 'ROAD_AIR'];
export const TransportModeLabels: Record<TransportMode, string> = {
  ROAD: 'Đường bộ',
  AIR: 'Hàng không',
  ROAD_AIR: 'Kết hợp bộ + hàng không',
};

export type RequestStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_DOCUMENT_REVIEW' | 'INFORMATION_REQUIRED' | 'COMPLIANCE_REVIEW' | 'PENDING_MANAGER_APPROVAL' | 'APPROVED' | 'REJECTED' | 'QUOTE_SENT' | 'QUOTE_ACCEPTED' | 'QUOTE_REJECTED' | 'CANCELLED' | 'READY_FOR_PLANNING' | 'TRIP_PLANNED' | 'COMPLETED';
export const RequestStatusValues: RequestStatus[] = ['DRAFT', 'SUBMITTED', 'UNDER_DOCUMENT_REVIEW', 'INFORMATION_REQUIRED', 'COMPLIANCE_REVIEW', 'PENDING_MANAGER_APPROVAL', 'APPROVED', 'REJECTED', 'QUOTE_SENT', 'QUOTE_ACCEPTED', 'QUOTE_REJECTED', 'CANCELLED', 'READY_FOR_PLANNING', 'TRIP_PLANNED', 'COMPLETED'];
export const RequestStatusLabels: Record<RequestStatus, string> = {
  DRAFT: 'Bản nháp',
  SUBMITTED: 'Đã gửi',
  UNDER_DOCUMENT_REVIEW: 'Đang rà soát tài liệu',
  INFORMATION_REQUIRED: 'Cần bổ sung thông tin',
  COMPLIANCE_REVIEW: 'Đang kiểm tra tuân thủ',
  PENDING_MANAGER_APPROVAL: 'Chờ quản lý phê duyệt',
  APPROVED: 'Đã phê duyệt',
  REJECTED: 'Bị từ chối',
  QUOTE_SENT: 'Đã gửi báo giá',
  QUOTE_ACCEPTED: 'Đã chấp nhận báo giá',
  QUOTE_REJECTED: 'Đã từ chối báo giá',
  CANCELLED: 'Đã hủy',
  READY_FOR_PLANNING: 'Sẵn sàng lập chuyến',
  TRIP_PLANNED: 'Đã lập chuyến',
  COMPLETED: 'Hoàn tất',
};

export type DocumentType = 'HORSE_PASSPORT' | 'MICROCHIP_CERTIFICATE' | 'VACCINATION_RECORD' | 'HEALTH_CERTIFICATE' | 'LAB_TEST_REPORT' | 'IMPORT_PERMIT' | 'EXPORT_PERMIT' | 'QUARANTINE_CERTIFICATE' | 'CUSTOMS_DECLARATION' | 'OWNERSHIP_PROOF' | 'INSURANCE_CERTIFICATE' | 'VET_REPORT' | 'OTHER';
export const DocumentTypeValues: DocumentType[] = ['HORSE_PASSPORT', 'MICROCHIP_CERTIFICATE', 'VACCINATION_RECORD', 'HEALTH_CERTIFICATE', 'LAB_TEST_REPORT', 'IMPORT_PERMIT', 'EXPORT_PERMIT', 'QUARANTINE_CERTIFICATE', 'CUSTOMS_DECLARATION', 'OWNERSHIP_PROOF', 'INSURANCE_CERTIFICATE', 'VET_REPORT', 'OTHER'];
export const DocumentTypeLabels: Record<DocumentType, string> = {
  HORSE_PASSPORT: 'Hộ chiếu ngựa',
  MICROCHIP_CERTIFICATE: 'Chứng nhận microchip / nhận dạng',
  VACCINATION_RECORD: 'Hồ sơ tiêm phòng',
  HEALTH_CERTIFICATE: 'Giấy chứng nhận sức khỏe',
  LAB_TEST_REPORT: 'Kết quả xét nghiệm',
  IMPORT_PERMIT: 'Giấy phép nhập khẩu',
  EXPORT_PERMIT: 'Giấy phép xuất khẩu',
  QUARANTINE_CERTIFICATE: 'Chứng nhận kiểm dịch / cách ly',
  CUSTOMS_DECLARATION: 'Tờ khai hải quan',
  OWNERSHIP_PROOF: 'Giấy tờ sở hữu',
  INSURANCE_CERTIFICATE: 'Giấy chứng nhận bảo hiểm',
  VET_REPORT: 'Báo cáo thú y bên ngoài',
  OTHER: 'Tài liệu khác',
};

export type DocumentReviewStatus = 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'NEEDS_UPDATE' | 'EXPIRED';
export const DocumentReviewStatusValues: DocumentReviewStatus[] = ['PENDING_REVIEW', 'APPROVED', 'REJECTED', 'NEEDS_UPDATE', 'EXPIRED'];
export const DocumentReviewStatusLabels: Record<DocumentReviewStatus, string> = {
  PENDING_REVIEW: 'Chờ rà soát',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Bị từ chối',
  NEEDS_UPDATE: 'Cần cập nhật',
  EXPIRED: 'Hết hạn',
};

export type ComplianceCategory = 'IDENTIFICATION' | 'VACCINATION' | 'HEALTH' | 'LABORATORY' | 'IMPORT_EXPORT' | 'QUARANTINE' | 'CUSTOMS' | 'INSURANCE';
export const ComplianceCategoryValues: ComplianceCategory[] = ['IDENTIFICATION', 'VACCINATION', 'HEALTH', 'LABORATORY', 'IMPORT_EXPORT', 'QUARANTINE', 'CUSTOMS', 'INSURANCE'];
export const ComplianceCategoryLabels: Record<ComplianceCategory, string> = {
  IDENTIFICATION: 'Nhận dạng',
  VACCINATION: 'Tiêm phòng',
  HEALTH: 'Sức khỏe',
  LABORATORY: 'Xét nghiệm',
  IMPORT_EXPORT: 'Xuất / nhập khẩu',
  QUARANTINE: 'Kiểm dịch',
  CUSTOMS: 'Hải quan',
  INSURANCE: 'Bảo hiểm',
};

export type ChecklistItemStatus = 'MISSING' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'NEEDS_UPDATE' | 'EXPIRED' | 'WAIVED';
export const ChecklistItemStatusValues: ChecklistItemStatus[] = ['MISSING', 'SUBMITTED', 'APPROVED', 'REJECTED', 'NEEDS_UPDATE', 'EXPIRED', 'WAIVED'];
export const ChecklistItemStatusLabels: Record<ChecklistItemStatus, string> = {
  MISSING: 'Chưa nộp',
  SUBMITTED: 'Đã nộp – chờ rà soát',
  APPROVED: 'Đạt',
  REJECTED: 'Không đạt',
  NEEDS_UPDATE: 'Cần cập nhật',
  EXPIRED: 'Hết hạn',
  WAIVED: 'Miễn trừ',
};

export type VehicleType = 'HORSE_TRUCK' | 'HORSE_TRAILER' | 'AIR_STALL' | 'SUPPORT_VEHICLE';
export const VehicleTypeValues: VehicleType[] = ['HORSE_TRUCK', 'HORSE_TRAILER', 'AIR_STALL', 'SUPPORT_VEHICLE'];
export const VehicleTypeLabels: Record<VehicleType, string> = {
  HORSE_TRUCK: 'Xe tải chở ngựa',
  HORSE_TRAILER: 'Rơ-moóc chở ngựa',
  AIR_STALL: 'Chuồng bay (air stall)',
  SUPPORT_VEHICLE: 'Xe hỗ trợ',
};

export type VehicleStatus = 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE' | 'UNAVAILABLE' | 'RETIRED';
export const VehicleStatusValues: VehicleStatus[] = ['AVAILABLE', 'IN_USE', 'MAINTENANCE', 'UNAVAILABLE', 'RETIRED'];
export const VehicleStatusLabels: Record<VehicleStatus, string> = {
  AVAILABLE: 'Sẵn sàng',
  IN_USE: 'Đang sử dụng',
  MAINTENANCE: 'Đang bảo dưỡng',
  UNAVAILABLE: 'Không khả dụng',
  RETIRED: 'Ngừng sử dụng',
};

export type RouteStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE';
export const RouteStatusValues: RouteStatus[] = ['DRAFT', 'ACTIVE', 'INACTIVE'];
export const RouteStatusLabels: Record<RouteStatus, string> = {
  DRAFT: 'Nháp',
  ACTIVE: 'Đang dùng',
  INACTIVE: 'Ngừng dùng',
};

export type StopType = 'PICKUP' | 'REST_STOP' | 'CHECKPOINT' | 'AIRPORT' | 'BORDER' | 'CUSTOMS' | 'QUARANTINE' | 'STABLE' | 'DESTINATION';
export const StopTypeValues: StopType[] = ['PICKUP', 'REST_STOP', 'CHECKPOINT', 'AIRPORT', 'BORDER', 'CUSTOMS', 'QUARANTINE', 'STABLE', 'DESTINATION'];
export const StopTypeLabels: Record<StopType, string> = {
  PICKUP: 'Điểm nhận ngựa',
  REST_STOP: 'Điểm nghỉ',
  CHECKPOINT: 'Điểm kiểm tra',
  AIRPORT: 'Sân bay',
  BORDER: 'Cửa khẩu',
  CUSTOMS: 'Hải quan',
  QUARANTINE: 'Kiểm dịch / cách ly',
  STABLE: 'Chuồng trung chuyển',
  DESTINATION: 'Điểm đến',
};

export type TripStatus = 'SCHEDULED' | 'PREPARING' | 'READY_FOR_DEPARTURE' | 'IN_TRANSIT' | 'AT_CHECKPOINT' | 'AT_BORDER' | 'CUSTOMS_CLEARANCE' | 'QUARANTINE' | 'DELAYED' | 'ARRIVED' | 'HANDOVER_PENDING' | 'DELIVERED' | 'CANCELLED';
export const TripStatusValues: TripStatus[] = ['SCHEDULED', 'PREPARING', 'READY_FOR_DEPARTURE', 'IN_TRANSIT', 'AT_CHECKPOINT', 'AT_BORDER', 'CUSTOMS_CLEARANCE', 'QUARANTINE', 'DELAYED', 'ARRIVED', 'HANDOVER_PENDING', 'DELIVERED', 'CANCELLED'];
export const TripStatusLabels: Record<TripStatus, string> = {
  SCHEDULED: 'Đã lên lịch',
  PREPARING: 'Đang chuẩn bị',
  READY_FOR_DEPARTURE: 'Sẵn sàng khởi hành',
  IN_TRANSIT: 'Đang vận chuyển',
  AT_CHECKPOINT: 'Tại điểm dừng',
  AT_BORDER: 'Tại cửa khẩu',
  CUSTOMS_CLEARANCE: 'Đang thông quan',
  QUARANTINE: 'Đang kiểm dịch',
  DELAYED: 'Bị trễ / tạm dừng',
  ARRIVED: 'Đã đến nơi',
  HANDOVER_PENDING: 'Chờ bàn giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
};

export type TrackingEventType = 'DEPARTED' | 'CHECKPOINT_ARRIVED' | 'CHECKPOINT_DEPARTED' | 'LOCATION_UPDATE' | 'STATUS_CHANGE' | 'DELAY' | 'ARRIVED' | 'NOTE';
export const TrackingEventTypeValues: TrackingEventType[] = ['DEPARTED', 'CHECKPOINT_ARRIVED', 'CHECKPOINT_DEPARTED', 'LOCATION_UPDATE', 'STATUS_CHANGE', 'DELAY', 'ARRIVED', 'NOTE'];
export const TrackingEventTypeLabels: Record<TrackingEventType, string> = {
  DEPARTED: 'Khởi hành',
  CHECKPOINT_ARRIVED: 'Đến điểm dừng',
  CHECKPOINT_DEPARTED: 'Rời điểm dừng',
  LOCATION_UPDATE: 'Cập nhật vị trí',
  STATUS_CHANGE: 'Đổi trạng thái',
  DELAY: 'Báo trễ',
  ARRIVED: 'Đến nơi',
  NOTE: 'Ghi chú',
};

export type IntakeLevel = 'NORMAL' | 'REDUCED' | 'POOR' | 'NONE';
export const IntakeLevelValues: IntakeLevel[] = ['NORMAL', 'REDUCED', 'POOR', 'NONE'];
export const IntakeLevelLabels: Record<IntakeLevel, string> = {
  NORMAL: 'Bình thường',
  REDUCED: 'Giảm',
  POOR: 'Kém',
  NONE: 'Không ăn / uống',
};

export type HorseBehavior = 'CALM' | 'ALERT' | 'RESTLESS' | 'AGITATED' | 'LETHARGIC';
export const HorseBehaviorValues: HorseBehavior[] = ['CALM', 'ALERT', 'RESTLESS', 'AGITATED', 'LETHARGIC'];
export const HorseBehaviorLabels: Record<HorseBehavior, string> = {
  CALM: 'Bình tĩnh',
  ALERT: 'Tỉnh táo',
  RESTLESS: 'Bồn chồn',
  AGITATED: 'Kích động',
  LETHARGIC: 'Lờ đờ',
};

export type StressLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export const StressLevelValues: StressLevel[] = ['LOW', 'MEDIUM', 'HIGH'];
export const StressLevelLabels: Record<StressLevel, string> = {
  LOW: 'Thấp',
  MEDIUM: 'Trung bình',
  HIGH: 'Cao',
};

export type MovementCondition = 'NORMAL' | 'STIFF' | 'LAME' | 'REFUSES_TO_MOVE';
export const MovementConditionValues: MovementCondition[] = ['NORMAL', 'STIFF', 'LAME', 'REFUSES_TO_MOVE'];
export const MovementConditionLabels: Record<MovementCondition, string> = {
  NORMAL: 'Bình thường',
  STIFF: 'Cứng / khó chịu',
  LAME: 'Đi khập khiễng',
  REFUSES_TO_MOVE: 'Không chịu di chuyển',
};

export type ObservationSeverity = 'NORMAL' | 'ATTENTION' | 'URGENT';
export const ObservationSeverityValues: ObservationSeverity[] = ['NORMAL', 'ATTENTION', 'URGENT'];
export const ObservationSeverityLabels: Record<ObservationSeverity, string> = {
  NORMAL: 'Bình thường',
  ATTENTION: 'Cần chú ý',
  URGENT: 'Khẩn cấp',
};

export type HorseCondition = 'GOOD' | 'FAIR' | 'POOR' | 'INJURED';
export const HorseConditionValues: HorseCondition[] = ['GOOD', 'FAIR', 'POOR', 'INJURED'];
export const HorseConditionLabels: Record<HorseCondition, string> = {
  GOOD: 'Tốt',
  FAIR: 'Khá',
  POOR: 'Kém',
  INJURED: 'Bị thương',
};

export type OperationalDecision = 'CONTINUE' | 'MONITOR' | 'STOP' | 'REROUTE';
export const OperationalDecisionValues: OperationalDecision[] = ['CONTINUE', 'MONITOR', 'STOP', 'REROUTE'];
export const OperationalDecisionLabels: Record<OperationalDecision, string> = {
  CONTINUE: 'Tiếp tục hành trình',
  MONITOR: 'Tiếp tục & theo dõi',
  STOP: 'Dừng tại chỗ',
  REROUTE: 'Đổi tuyến',
};

export type IncidentType = 'HORSE_HEALTH' | 'HORSE_INJURY' | 'VEHICLE_BREAKDOWN' | 'TRAFFIC_DELAY' | 'BORDER_DELAY' | 'DOCUMENT_ISSUE' | 'WEATHER' | 'SECURITY' | 'OTHER';
export const IncidentTypeValues: IncidentType[] = ['HORSE_HEALTH', 'HORSE_INJURY', 'VEHICLE_BREAKDOWN', 'TRAFFIC_DELAY', 'BORDER_DELAY', 'DOCUMENT_ISSUE', 'WEATHER', 'SECURITY', 'OTHER'];
export const IncidentTypeLabels: Record<IncidentType, string> = {
  HORSE_HEALTH: 'Sức khỏe ngựa',
  HORSE_INJURY: 'Ngựa bị thương',
  VEHICLE_BREAKDOWN: 'Sự cố phương tiện',
  TRAFFIC_DELAY: 'Giao thông / kẹt xe',
  BORDER_DELAY: 'Chậm trễ cửa khẩu',
  DOCUMENT_ISSUE: 'Vấn đề giấy tờ / tuân thủ',
  WEATHER: 'Thời tiết',
  SECURITY: 'An ninh',
  OTHER: 'Khác',
};

export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export const IncidentSeverityValues: IncidentSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
export const IncidentSeverityLabels: Record<IncidentSeverity, string> = {
  LOW: 'Thấp',
  MEDIUM: 'Trung bình',
  HIGH: 'Cao',
  CRITICAL: 'Nghiêm trọng',
};

export type IncidentStatus = 'OPEN' | 'UNDER_REVIEW' | 'ACTION_REQUIRED' | 'MONITORING' | 'RESOLVED' | 'CLOSED';
export const IncidentStatusValues: IncidentStatus[] = ['OPEN', 'UNDER_REVIEW', 'ACTION_REQUIRED', 'MONITORING', 'RESOLVED', 'CLOSED'];
export const IncidentStatusLabels: Record<IncidentStatus, string> = {
  OPEN: 'Mới mở',
  UNDER_REVIEW: 'Đang xem xét',
  ACTION_REQUIRED: 'Cần hành động',
  MONITORING: 'Đang theo dõi',
  RESOLVED: 'Đã xử lý',
  CLOSED: 'Đã đóng',
};

export type IncidentActionType = 'NOTE' | 'VET_CONTACTED' | 'VET_ADVICE_RECORDED' | 'DECISION' | 'STATUS_CHANGE' | 'ESCALATION' | 'FIELD_ACTION';
export const IncidentActionTypeValues: IncidentActionType[] = ['NOTE', 'VET_CONTACTED', 'VET_ADVICE_RECORDED', 'DECISION', 'STATUS_CHANGE', 'ESCALATION', 'FIELD_ACTION'];
export const IncidentActionTypeLabels: Record<IncidentActionType, string> = {
  NOTE: 'Ghi chú',
  VET_CONTACTED: 'Đã liên hệ thú y',
  VET_ADVICE_RECORDED: 'Ghi nhận tư vấn thú y',
  DECISION: 'Quyết định vận hành',
  STATUS_CHANGE: 'Đổi trạng thái',
  ESCALATION: 'Báo cáo cấp trên',
  FIELD_ACTION: 'Xử lý tại hiện trường',
};

export type HandoverStatus = 'PENDING_CONFIRMATION' | 'CONFIRMED' | 'DISPUTED';
export const HandoverStatusValues: HandoverStatus[] = ['PENDING_CONFIRMATION', 'CONFIRMED', 'DISPUTED'];
export const HandoverStatusLabels: Record<HandoverStatus, string> = {
  PENDING_CONFIRMATION: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  DISPUTED: 'Có khiếu nại',
};

export type QuoteType = 'ESTIMATE' | 'FINAL';
export const QuoteTypeValues: QuoteType[] = ['ESTIMATE', 'FINAL'];
export const QuoteTypeLabels: Record<QuoteType, string> = {
  ESTIMATE: 'Chi phí ước tính',
  FINAL: 'Báo giá cuối',
};

export type QuoteStatus = 'ESTIMATED' | 'DRAFT' | 'SENT' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | 'SUPERSEDED';
export const QuoteStatusValues: QuoteStatus[] = ['ESTIMATED', 'DRAFT', 'SENT', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'SUPERSEDED'];
export const QuoteStatusLabels: Record<QuoteStatus, string> = {
  ESTIMATED: 'Ước tính',
  DRAFT: 'Nháp',
  SENT: 'Đã gửi khách',
  ACCEPTED: 'Đã chấp nhận',
  REJECTED: 'Đã từ chối',
  EXPIRED: 'Hết hạn',
  SUPERSEDED: 'Đã thay thế bởi phiên bản mới',
};

export type CostCategory = 'GROUND_TRANSPORT' | 'AIR_TRANSPORT' | 'HANDLING' | 'VETERINARY_TESTING' | 'DOCUMENTS' | 'CUSTOMS' | 'QUARANTINE_STABLING' | 'ESCORT' | 'INSURANCE' | 'SURCHARGE' | 'DISCOUNT';
export const CostCategoryValues: CostCategory[] = ['GROUND_TRANSPORT', 'AIR_TRANSPORT', 'HANDLING', 'VETERINARY_TESTING', 'DOCUMENTS', 'CUSTOMS', 'QUARANTINE_STABLING', 'ESCORT', 'INSURANCE', 'SURCHARGE', 'DISCOUNT'];
export const CostCategoryLabels: Record<CostCategory, string> = {
  GROUND_TRANSPORT: 'Vận chuyển đường bộ',
  AIR_TRANSPORT: 'Vận chuyển hàng không',
  HANDLING: 'Bốc xếp / xử lý',
  VETERINARY_TESTING: 'Thú y / xét nghiệm',
  DOCUMENTS: 'Giấy tờ',
  CUSTOMS: 'Hải quan',
  QUARANTINE_STABLING: 'Kiểm dịch / lưu chuồng',
  ESCORT: 'Áp tải',
  INSURANCE: 'Bảo hiểm',
  SURCHARGE: 'Phụ phí',
  DISCOUNT: 'Giảm giá',
};

export type PricingUnit = 'PER_KM' | 'PER_HORSE' | 'PER_TRIP' | 'PER_HORSE_DAY' | 'PERCENT_OF_DECLARED_VALUE';
export const PricingUnitValues: PricingUnit[] = ['PER_KM', 'PER_HORSE', 'PER_TRIP', 'PER_HORSE_DAY', 'PERCENT_OF_DECLARED_VALUE'];
export const PricingUnitLabels: Record<PricingUnit, string> = {
  PER_KM: 'Theo km',
  PER_HORSE: 'Theo con ngựa',
  PER_TRIP: 'Theo chuyến',
  PER_HORSE_DAY: 'Theo ngựa / ngày',
  PERCENT_OF_DECLARED_VALUE: '% giá trị khai báo (phí bảo hiểm)',
};

export type InvoiceStatus = 'ISSUED' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'CANCELLED';
export const InvoiceStatusValues: InvoiceStatus[] = ['ISSUED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED'];
export const InvoiceStatusLabels: Record<InvoiceStatus, string> = {
  ISSUED: 'Đã phát hành',
  PARTIALLY_PAID: 'Đã thanh toán một phần',
  PAID: 'Đã thanh toán',
  OVERDUE: 'Quá hạn',
  CANCELLED: 'Đã hủy',
};

export type PaymentMethod = 'SIMULATED_CARD' | 'SIMULATED_BANK_TRANSFER' | 'SIMULATED_EWALLET';
export const PaymentMethodValues: PaymentMethod[] = ['SIMULATED_CARD', 'SIMULATED_BANK_TRANSFER', 'SIMULATED_EWALLET'];
export const PaymentMethodLabels: Record<PaymentMethod, string> = {
  SIMULATED_CARD: 'Thẻ (mô phỏng)',
  SIMULATED_BANK_TRANSFER: 'Chuyển khoản (mô phỏng)',
  SIMULATED_EWALLET: 'Ví điện tử (mô phỏng)',
};

export type PaymentStatus = 'SUCCEEDED' | 'FAILED';
export const PaymentStatusValues: PaymentStatus[] = ['SUCCEEDED', 'FAILED'];
export const PaymentStatusLabels: Record<PaymentStatus, string> = {
  SUCCEEDED: 'Thành công',
  FAILED: 'Thất bại',
};

export type ClaimType = 'INJURY_MEDICAL_EXPENSE' | 'MORTALITY' | 'HUMANE_EUTHANASIA' | 'THEFT_OR_LOSS' | 'PROPERTY_DAMAGE' | 'SERVICE_DELAY' | 'OTHER_DIRECT_LOSS';
export const ClaimTypeValues: ClaimType[] = ['INJURY_MEDICAL_EXPENSE', 'MORTALITY', 'HUMANE_EUTHANASIA', 'THEFT_OR_LOSS', 'PROPERTY_DAMAGE', 'SERVICE_DELAY', 'OTHER_DIRECT_LOSS'];
export const ClaimTypeLabels: Record<ClaimType, string> = {
  INJURY_MEDICAL_EXPENSE: 'Chi phí điều trị chấn thương',
  MORTALITY: 'Ngựa chết',
  HUMANE_EUTHANASIA: 'An tử nhân đạo',
  THEFT_OR_LOSS: 'Mất cắp / thất lạc',
  PROPERTY_DAMAGE: 'Hư hỏng tài sản',
  SERVICE_DELAY: 'Chậm trễ dịch vụ',
  OTHER_DIRECT_LOSS: 'Tổn thất trực tiếp khác',
};

export type ClaimStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'EVIDENCE_REQUIRED' | 'APPROVED' | 'PARTIALLY_APPROVED' | 'REJECTED' | 'PAID' | 'CLOSED';
export const ClaimStatusValues: ClaimStatus[] = ['SUBMITTED', 'UNDER_REVIEW', 'EVIDENCE_REQUIRED', 'APPROVED', 'PARTIALLY_APPROVED', 'REJECTED', 'PAID', 'CLOSED'];
export const ClaimStatusLabels: Record<ClaimStatus, string> = {
  SUBMITTED: 'Đã nộp',
  UNDER_REVIEW: 'Đang thẩm định',
  EVIDENCE_REQUIRED: 'Cần bổ sung bằng chứng',
  APPROVED: 'Được duyệt',
  PARTIALLY_APPROVED: 'Duyệt một phần',
  REJECTED: 'Bị từ chối',
  PAID: 'Đã chi trả',
  CLOSED: 'Đã đóng',
};

export type EvidenceType = 'PHOTO' | 'VET_REPORT' | 'RECEIPT' | 'OFFICIAL_REPORT' | 'OTHER';
export const EvidenceTypeValues: EvidenceType[] = ['PHOTO', 'VET_REPORT', 'RECEIPT', 'OFFICIAL_REPORT', 'OTHER'];
export const EvidenceTypeLabels: Record<EvidenceType, string> = {
  PHOTO: 'Ảnh',
  VET_REPORT: 'Báo cáo thú y',
  RECEIPT: 'Hóa đơn / chứng từ chi phí',
  OFFICIAL_REPORT: 'Biên bản',
  OTHER: 'Khác',
};

export type ClaimRecommendation = 'APPROVE' | 'PARTIALLY_APPROVE' | 'REJECT' | 'REQUEST_EVIDENCE';
export const ClaimRecommendationValues: ClaimRecommendation[] = ['APPROVE', 'PARTIALLY_APPROVE', 'REJECT', 'REQUEST_EVIDENCE'];
export const ClaimRecommendationLabels: Record<ClaimRecommendation, string> = {
  APPROVE: 'Đề xuất duyệt',
  PARTIALLY_APPROVE: 'Đề xuất duyệt một phần',
  REJECT: 'Đề xuất từ chối',
  REQUEST_EVIDENCE: 'Yêu cầu bổ sung bằng chứng',
};

export type SettlementStatus = 'PAID' | 'FAILED';
export const SettlementStatusValues: SettlementStatus[] = ['PAID', 'FAILED'];
export const SettlementStatusLabels: Record<SettlementStatus, string> = {
  PAID: 'Đã chi trả (mô phỏng)',
  FAILED: 'Thất bại',
};

export type NotificationType = 'INFO' | 'SUCCESS' | 'WARNING' | 'ACTION_REQUIRED' | 'CRITICAL';
export const NotificationTypeValues: NotificationType[] = ['INFO', 'SUCCESS', 'WARNING', 'ACTION_REQUIRED', 'CRITICAL'];
export const NotificationTypeLabels: Record<NotificationType, string> = {
  INFO: 'Thông tin',
  SUCCESS: 'Thành công',
  WARNING: 'Cảnh báo',
  ACTION_REQUIRED: 'Cần xử lý',
  CRITICAL: 'Khẩn cấp',
};

export type AttachmentRefType = 'HEALTH_OBSERVATION' | 'INCIDENT' | 'HANDOVER' | 'ARRIVAL_CHECK' | 'VET_CONSULTATION' | 'TRACKING_EVENT';
export const AttachmentRefTypeValues: AttachmentRefType[] = ['HEALTH_OBSERVATION', 'INCIDENT', 'HANDOVER', 'ARRIVAL_CHECK', 'VET_CONSULTATION', 'TRACKING_EVENT'];
export const AttachmentRefTypeLabels: Record<AttachmentRefType, string> = {
  HEALTH_OBSERVATION: 'Quan sát sức khỏe',
  INCIDENT: 'Sự cố',
  HANDOVER: 'Bàn giao',
  ARRIVAL_CHECK: 'Kiểm tra khi đến',
  VET_CONSULTATION: 'Tư vấn thú y',
  TRACKING_EVENT: 'Sự kiện hành trình',
};
