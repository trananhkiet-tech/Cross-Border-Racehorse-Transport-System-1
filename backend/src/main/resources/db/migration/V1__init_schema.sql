-- =====================================================================
-- IRTMS – International Racehorse Transportation Management System
-- V1: schema khởi tạo cho Microsoft SQL Server 2019+
-- Sinh từ mô hình dữ liệu thống nhất với JPA entity. KHÔNG sửa file đã chạy;
-- thay đổi schema = thêm file V{n}__mo_ta.sql mới.
-- =====================================================================

CREATE TABLE app_user (
    id BIGINT IDENTITY(1,1) NOT NULL,
    email NVARCHAR(150) NOT NULL,
    password_hash NVARCHAR(100) NOT NULL,
    full_name NVARCHAR(150) NOT NULL,
    phone NVARCHAR(30) NULL,
    role NVARCHAR(40) NOT NULL,
    enabled BIT NOT NULL CONSTRAINT df_app_user_enabled DEFAULT 1,
    last_login_at DATETIME2(3) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_app_user_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_app_user_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_role CHECK (role IN (N'CUSTOMER', N'LOGISTICS_MANAGER', N'TRANSPORT_SPECIALIST', N'FLEET_ROUTE_COORDINATOR', N'DRIVER_ESCORT'))
);

CREATE TABLE customer_profile (
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    company_name NVARCHAR(150) NULL,
    customer_type NVARCHAR(40) NOT NULL,
    address NVARCHAR(255) NULL,
    country NVARCHAR(2) NULL,
    tax_code NVARCHAR(30) NULL,
    preferred_language NVARCHAR(5) NOT NULL CONSTRAINT df_customer_profile_preferred_language DEFAULT 'vi',
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_customer_profile_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_customer_profile_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_customer_profile PRIMARY KEY (id),
    CONSTRAINT uq_customer_profile_user_id UNIQUE (user_id),
    CONSTRAINT ck_customer_profile_customer_type CHECK (customer_type IN (N'INDIVIDUAL', N'STABLE', N'COMPANY', N'AGENT'))
);

CREATE TABLE employee_profile (
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    employee_code NVARCHAR(20) NOT NULL,
    department NVARCHAR(100) NULL,
    staff_role NVARCHAR(40) NULL,
    license_number NVARCHAR(50) NULL,
    license_expiry DATE NULL,
    air_escort_certified BIT NOT NULL CONSTRAINT df_employee_profile_air_escort_certified DEFAULT 0,
    emergency_contact NVARCHAR(150) NULL,
    base_location NVARCHAR(150) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_employee_profile_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_employee_profile_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_employee_profile PRIMARY KEY (id),
    CONSTRAINT uq_employee_profile_user_id UNIQUE (user_id),
    CONSTRAINT uq_employee_profile_employee_code UNIQUE (employee_code),
    CONSTRAINT ck_employee_profile_staff_role CHECK (staff_role IN (N'DRIVER', N'ESCORT', N'TEAM_LEADER'))
);

CREATE TABLE code_sequence (
    id BIGINT IDENTITY(1,1) NOT NULL,
    seq_key NVARCHAR(30) NOT NULL,
    next_value BIGINT NOT NULL,
    version BIGINT NOT NULL CONSTRAINT df_code_sequence_version DEFAULT 0,
    CONSTRAINT pk_code_sequence PRIMARY KEY (id),
    CONSTRAINT uq_code_sequence_seq_key UNIQUE (seq_key)
);

CREATE TABLE notification (
    id BIGINT IDENTITY(1,1) NOT NULL,
    recipient_id BIGINT NOT NULL,
    title NVARCHAR(200) NOT NULL,
    message NVARCHAR(1000) NOT NULL,
    type NVARCHAR(40) NOT NULL,
    reference_type NVARCHAR(40) NULL,
    reference_id BIGINT NULL,
    read_at DATETIME2(3) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_notification_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_notification PRIMARY KEY (id),
    CONSTRAINT ck_notification_type CHECK (type IN (N'INFO', N'SUCCESS', N'WARNING', N'ACTION_REQUIRED', N'CRITICAL'))
);

CREATE TABLE horse (
    id BIGINT IDENTITY(1,1) NOT NULL,
    owner_id BIGINT NOT NULL,
    registered_name NVARCHAR(150) NOT NULL,
    breed NVARCHAR(100) NULL,
    date_of_birth DATE NULL,
    sex NVARCHAR(40) NULL,
    color NVARCHAR(60) NULL,
    height_cm INT NULL,
    microchip_number NVARCHAR(30) NOT NULL,
    passport_number NVARCHAR(50) NOT NULL,
    passport_country NVARCHAR(2) NULL,
    special_requirements NVARCHAR(1000) NULL,
    notes NVARCHAR(1000) NULL,
    status NVARCHAR(40) NOT NULL,
    version BIGINT NOT NULL CONSTRAINT df_horse_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_horse_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_horse_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_horse PRIMARY KEY (id),
    CONSTRAINT uq_horse_microchip_number UNIQUE (microchip_number),
    CONSTRAINT uq_horse_passport_number UNIQUE (passport_number),
    CONSTRAINT ck_horse_sex CHECK (sex IN (N'STALLION', N'MARE', N'GELDING', N'COLT', N'FILLY')),
    CONSTRAINT ck_horse_status CHECK (status IN (N'ACTIVE', N'IN_TRANSIT', N'ARCHIVED'))
);

CREATE TABLE stall_type (
    id BIGINT IDENTITY(1,1) NOT NULL,
    code NVARCHAR(30) NOT NULL,
    name NVARCHAR(100) NOT NULL,
    width_cm INT NOT NULL,
    description NVARCHAR(500) NULL,
    surcharge DECIMAL(18,2) NOT NULL,
    currency NVARCHAR(3) NOT NULL CONSTRAINT df_stall_type_currency DEFAULT 'USD',
    active BIT NOT NULL CONSTRAINT df_stall_type_active DEFAULT 1,
    sort_order INT NOT NULL CONSTRAINT df_stall_type_sort_order DEFAULT 0,
    CONSTRAINT pk_stall_type PRIMARY KEY (id),
    CONSTRAINT uq_stall_type_code UNIQUE (code)
);

CREATE TABLE transportation_request (
    id BIGINT IDENTITY(1,1) NOT NULL,
    request_number NVARCHAR(30) NOT NULL,
    customer_id BIGINT NOT NULL,
    origin_country NVARCHAR(2) NOT NULL,
    origin_address NVARCHAR(255) NOT NULL,
    destination_country NVARCHAR(2) NOT NULL,
    destination_address NVARCHAR(255) NOT NULL,
    requested_departure_date DATE NOT NULL,
    return_date DATE NULL,
    movement_type NVARCHAR(40) NOT NULL,
    purpose NVARCHAR(40) NOT NULL,
    transport_mode NVARCHAR(40) NOT NULL,
    declared_value_total DECIMAL(18,2) NOT NULL CONSTRAINT df_transportation_request_declared_value_total DEFAULT 0,
    currency NVARCHAR(3) NOT NULL CONSTRAINT df_transportation_request_currency DEFAULT 'USD',
    special_requirements NVARCHAR(1000) NULL,
    notes NVARCHAR(1000) NULL,
    status NVARCHAR(40) NOT NULL,
    last_decision_note NVARCHAR(1000) NULL,
    submitted_at DATETIME2(3) NULL,
    approved_at DATETIME2(3) NULL,
    completed_at DATETIME2(3) NULL,
    version BIGINT NOT NULL CONSTRAINT df_transportation_request_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_transportation_request_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_transportation_request_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_transportation_request PRIMARY KEY (id),
    CONSTRAINT uq_transportation_request_request_number UNIQUE (request_number),
    CONSTRAINT ck_transportation_request_movement_type CHECK (movement_type IN (N'TEMPORARY', N'PERMANENT')),
    CONSTRAINT ck_transportation_request_purpose CHECK (purpose IN (N'RACING', N'COMPETITION', N'TRAINING', N'BREEDING', N'RELOCATION', N'OTHER')),
    CONSTRAINT ck_transportation_request_transport_mode CHECK (transport_mode IN (N'ROAD', N'AIR', N'ROAD_AIR')),
    CONSTRAINT ck_transportation_request_status CHECK (status IN (N'DRAFT', N'SUBMITTED', N'UNDER_DOCUMENT_REVIEW', N'INFORMATION_REQUIRED', N'COMPLIANCE_REVIEW', N'PENDING_MANAGER_APPROVAL', N'APPROVED', N'REJECTED', N'QUOTE_SENT', N'QUOTE_ACCEPTED', N'QUOTE_REJECTED', N'CANCELLED', N'READY_FOR_PLANNING', N'TRIP_PLANNED', N'COMPLETED'))
);

CREATE TABLE request_horse (
    id BIGINT IDENTITY(1,1) NOT NULL,
    request_id BIGINT NOT NULL,
    horse_id BIGINT NOT NULL,
    stall_type_id BIGINT NOT NULL,
    declared_value DECIMAL(18,2) NOT NULL,
    notes NVARCHAR(500) NULL,
    CONSTRAINT pk_request_horse PRIMARY KEY (id),
    CONSTRAINT uq_request_horse_request_id_horse_id UNIQUE (request_id, horse_id)
);

CREATE TABLE request_status_history (
    id BIGINT IDENTITY(1,1) NOT NULL,
    request_id BIGINT NOT NULL,
    from_status NVARCHAR(40) NULL,
    to_status NVARCHAR(40) NOT NULL,
    changed_by_id BIGINT NOT NULL,
    note NVARCHAR(1000) NULL,
    changed_at DATETIME2(3) NOT NULL,
    CONSTRAINT pk_request_status_history PRIMARY KEY (id),
    CONSTRAINT ck_request_status_history_from_status CHECK (from_status IN (N'DRAFT', N'SUBMITTED', N'UNDER_DOCUMENT_REVIEW', N'INFORMATION_REQUIRED', N'COMPLIANCE_REVIEW', N'PENDING_MANAGER_APPROVAL', N'APPROVED', N'REJECTED', N'QUOTE_SENT', N'QUOTE_ACCEPTED', N'QUOTE_REJECTED', N'CANCELLED', N'READY_FOR_PLANNING', N'TRIP_PLANNED', N'COMPLETED')),
    CONSTRAINT ck_request_status_history_to_status CHECK (to_status IN (N'DRAFT', N'SUBMITTED', N'UNDER_DOCUMENT_REVIEW', N'INFORMATION_REQUIRED', N'COMPLIANCE_REVIEW', N'PENDING_MANAGER_APPROVAL', N'APPROVED', N'REJECTED', N'QUOTE_SENT', N'QUOTE_ACCEPTED', N'QUOTE_REJECTED', N'CANCELLED', N'READY_FOR_PLANNING', N'TRIP_PLANNED', N'COMPLETED'))
);

CREATE TABLE stored_file (
    id BIGINT IDENTITY(1,1) NOT NULL,
    original_filename NVARCHAR(255) NOT NULL,
    stored_filename NVARCHAR(80) NOT NULL,
    content_type NVARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 NVARCHAR(64) NOT NULL,
    uploaded_by_id BIGINT NOT NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_stored_file_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_stored_file PRIMARY KEY (id),
    CONSTRAINT uq_stored_file_stored_filename UNIQUE (stored_filename)
);

CREATE TABLE document (
    id BIGINT IDENTITY(1,1) NOT NULL,
    document_type NVARCHAR(40) NOT NULL,
    request_id BIGINT NULL,
    horse_id BIGINT NULL,
    file_id BIGINT NOT NULL,
    title NVARCHAR(200) NULL,
    document_number NVARCHAR(80) NULL,
    issuing_country NVARCHAR(2) NULL,
    issue_date DATE NULL,
    expiry_date DATE NULL,
    review_status NVARCHAR(40) NOT NULL,
    reviewer_id BIGINT NULL,
    reviewed_at DATETIME2(3) NULL,
    review_note NVARCHAR(1000) NULL,
    uploaded_by_id BIGINT NOT NULL,
    version BIGINT NOT NULL CONSTRAINT df_document_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_document_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_document_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_document PRIMARY KEY (id),
    CONSTRAINT ck_document_document_type CHECK (document_type IN (N'HORSE_PASSPORT', N'MICROCHIP_CERTIFICATE', N'VACCINATION_RECORD', N'HEALTH_CERTIFICATE', N'LAB_TEST_REPORT', N'IMPORT_PERMIT', N'EXPORT_PERMIT', N'QUARANTINE_CERTIFICATE', N'CUSTOMS_DECLARATION', N'OWNERSHIP_PROOF', N'INSURANCE_CERTIFICATE', N'VET_REPORT', N'OTHER')),
    CONSTRAINT ck_document_review_status CHECK (review_status IN (N'PENDING_REVIEW', N'APPROVED', N'REJECTED', N'NEEDS_UPDATE', N'EXPIRED')),
    CONSTRAINT ck_document_1 CHECK (request_id IS NOT NULL OR horse_id IS NOT NULL)
);

CREATE TABLE attachment (
    id BIGINT IDENTITY(1,1) NOT NULL,
    file_id BIGINT NOT NULL,
    ref_type NVARCHAR(40) NOT NULL,
    ref_id BIGINT NOT NULL,
    caption NVARCHAR(255) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_attachment_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_attachment PRIMARY KEY (id),
    CONSTRAINT ck_attachment_ref_type CHECK (ref_type IN (N'HEALTH_OBSERVATION', N'INCIDENT', N'HANDOVER', N'ARRIVAL_CHECK', N'VET_CONSULTATION', N'TRACKING_EVENT'))
);

CREATE TABLE compliance_requirement (
    id BIGINT IDENTITY(1,1) NOT NULL,
    code NVARCHAR(40) NOT NULL,
    origin_country NVARCHAR(2) NULL,
    destination_country NVARCHAR(2) NULL,
    movement_type NVARCHAR(40) NULL,
    purpose NVARCHAR(40) NULL,
    category NVARCHAR(40) NOT NULL,
    document_type NVARCHAR(40) NOT NULL,
    title NVARCHAR(200) NOT NULL,
    description NVARCHAR(1000) NULL,
    validity_days INT NULL,
    per_horse BIT NOT NULL CONSTRAINT df_compliance_requirement_per_horse DEFAULT 0,
    mandatory BIT NOT NULL CONSTRAINT df_compliance_requirement_mandatory DEFAULT 1,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    active BIT NOT NULL CONSTRAINT df_compliance_requirement_active DEFAULT 1,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_compliance_requirement_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_compliance_requirement_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_compliance_requirement PRIMARY KEY (id),
    CONSTRAINT uq_compliance_requirement_code UNIQUE (code),
    CONSTRAINT ck_compliance_requirement_movement_type CHECK (movement_type IN (N'TEMPORARY', N'PERMANENT')),
    CONSTRAINT ck_compliance_requirement_purpose CHECK (purpose IN (N'RACING', N'COMPETITION', N'TRAINING', N'BREEDING', N'RELOCATION', N'OTHER')),
    CONSTRAINT ck_compliance_requirement_category CHECK (category IN (N'IDENTIFICATION', N'VACCINATION', N'HEALTH', N'LABORATORY', N'IMPORT_EXPORT', N'QUARANTINE', N'CUSTOMS', N'INSURANCE')),
    CONSTRAINT ck_compliance_requirement_document_type CHECK (document_type IN (N'HORSE_PASSPORT', N'MICROCHIP_CERTIFICATE', N'VACCINATION_RECORD', N'HEALTH_CERTIFICATE', N'LAB_TEST_REPORT', N'IMPORT_PERMIT', N'EXPORT_PERMIT', N'QUARANTINE_CERTIFICATE', N'CUSTOMS_DECLARATION', N'OWNERSHIP_PROOF', N'INSURANCE_CERTIFICATE', N'VET_REPORT', N'OTHER'))
);

CREATE TABLE compliance_checklist_item (
    id BIGINT IDENTITY(1,1) NOT NULL,
    request_id BIGINT NOT NULL,
    requirement_id BIGINT NOT NULL,
    horse_id BIGINT NULL,
    status NVARCHAR(40) NOT NULL,
    document_id BIGINT NULL,
    reviewer_note NVARCHAR(1000) NULL,
    checked_by_id BIGINT NULL,
    checked_at DATETIME2(3) NULL,
    due_date DATE NULL,
    version BIGINT NOT NULL CONSTRAINT df_compliance_checklist_item_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_compliance_checklist_item_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_compliance_checklist_item_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_compliance_checklist_item PRIMARY KEY (id),
    CONSTRAINT ck_compliance_checklist_item_status CHECK (status IN (N'MISSING', N'SUBMITTED', N'APPROVED', N'REJECTED', N'NEEDS_UPDATE', N'EXPIRED', N'WAIVED'))
);

CREATE TABLE vehicle (
    id BIGINT IDENTITY(1,1) NOT NULL,
    vehicle_code NVARCHAR(20) NOT NULL,
    plate_number NVARCHAR(30) NOT NULL,
    vehicle_type NVARCHAR(40) NOT NULL,
    model NVARCHAR(100) NULL,
    capacity_horses INT NOT NULL,
    status NVARCHAR(40) NOT NULL,
    current_location NVARCHAR(150) NULL,
    climate_controlled BIT NOT NULL CONSTRAINT df_vehicle_climate_controlled DEFAULT 0,
    camera_monitoring BIT NOT NULL CONSTRAINT df_vehicle_camera_monitoring DEFAULT 0,
    last_maintenance_date DATE NULL,
    next_maintenance_date DATE NULL,
    notes NVARCHAR(1000) NULL,
    archived BIT NOT NULL CONSTRAINT df_vehicle_archived DEFAULT 0,
    version BIGINT NOT NULL CONSTRAINT df_vehicle_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_vehicle_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_vehicle_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_vehicle PRIMARY KEY (id),
    CONSTRAINT uq_vehicle_vehicle_code UNIQUE (vehicle_code),
    CONSTRAINT uq_vehicle_plate_number UNIQUE (plate_number),
    CONSTRAINT ck_vehicle_vehicle_type CHECK (vehicle_type IN (N'HORSE_TRUCK', N'HORSE_TRAILER', N'AIR_STALL', N'SUPPORT_VEHICLE')),
    CONSTRAINT ck_vehicle_status CHECK (status IN (N'AVAILABLE', N'IN_USE', N'MAINTENANCE', N'UNAVAILABLE', N'RETIRED'))
);

CREATE TABLE route (
    id BIGINT IDENTITY(1,1) NOT NULL,
    route_code NVARCHAR(20) NOT NULL,
    name NVARCHAR(150) NOT NULL,
    origin_country NVARCHAR(2) NOT NULL,
    origin_location NVARCHAR(150) NOT NULL,
    destination_country NVARCHAR(2) NOT NULL,
    destination_location NVARCHAR(150) NOT NULL,
    transport_mode NVARCHAR(40) NOT NULL,
    estimated_distance_km DECIMAL(10,1) NULL,
    estimated_duration_hours DECIMAL(8,1) NULL,
    status NVARCHAR(40) NOT NULL,
    alternative_of_id BIGINT NULL,
    notes NVARCHAR(1000) NULL,
    version BIGINT NOT NULL CONSTRAINT df_route_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_route_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_route_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_route PRIMARY KEY (id),
    CONSTRAINT uq_route_route_code UNIQUE (route_code),
    CONSTRAINT ck_route_transport_mode CHECK (transport_mode IN (N'ROAD', N'AIR', N'ROAD_AIR')),
    CONSTRAINT ck_route_status CHECK (status IN (N'DRAFT', N'ACTIVE', N'INACTIVE'))
);

CREATE TABLE route_stop (
    id BIGINT IDENTITY(1,1) NOT NULL,
    route_id BIGINT NOT NULL,
    stop_order INT NOT NULL,
    stop_type NVARCHAR(40) NOT NULL,
    name NVARCHAR(150) NOT NULL,
    address NVARCHAR(255) NULL,
    country NVARCHAR(2) NULL,
    latitude DECIMAL(9,6) NULL,
    longitude DECIMAL(9,6) NULL,
    planned_arrival_offset_min INT NULL,
    planned_duration_min INT NULL,
    instructions NVARCHAR(1000) NULL,
    CONSTRAINT pk_route_stop PRIMARY KEY (id),
    CONSTRAINT uq_route_stop_route_id_stop_order UNIQUE (route_id, stop_order),
    CONSTRAINT ck_route_stop_stop_type CHECK (stop_type IN (N'PICKUP', N'REST_STOP', N'CHECKPOINT', N'AIRPORT', N'BORDER', N'CUSTOMS', N'QUARANTINE', N'STABLE', N'DESTINATION'))
);

CREATE TABLE transportation_trip (
    id BIGINT IDENTITY(1,1) NOT NULL,
    trip_number NVARCHAR(30) NOT NULL,
    request_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    vehicle_id BIGINT NULL,
    coordinator_id BIGINT NOT NULL,
    planned_departure DATETIME2(3) NOT NULL,
    planned_arrival DATETIME2(3) NOT NULL,
    actual_departure DATETIME2(3) NULL,
    actual_arrival DATETIME2(3) NULL,
    status NVARCHAR(40) NOT NULL,
    current_location NVARCHAR(150) NULL,
    current_stop_id BIGINT NULL,
    operation_blocked BIT NOT NULL CONSTRAINT df_transportation_trip_operation_blocked DEFAULT 0,
    transport_instructions NVARCHAR(2000) NULL,
    notes NVARCHAR(1000) NULL,
    version BIGINT NOT NULL CONSTRAINT df_transportation_trip_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_transportation_trip_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_transportation_trip_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_transportation_trip PRIMARY KEY (id),
    CONSTRAINT uq_transportation_trip_trip_number UNIQUE (trip_number),
    CONSTRAINT ck_transportation_trip_status CHECK (status IN (N'SCHEDULED', N'PREPARING', N'READY_FOR_DEPARTURE', N'IN_TRANSIT', N'AT_CHECKPOINT', N'AT_BORDER', N'CUSTOMS_CLEARANCE', N'QUARANTINE', N'DELAYED', N'ARRIVED', N'HANDOVER_PENDING', N'DELIVERED', N'CANCELLED'))
);

CREATE TABLE trip_staff_assignment (
    id BIGINT IDENTITY(1,1) NOT NULL,
    trip_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    assignment_role NVARCHAR(40) NOT NULL,
    assigned_by_id BIGINT NOT NULL,
    assigned_at DATETIME2(3) NOT NULL,
    CONSTRAINT pk_trip_staff_assignment PRIMARY KEY (id),
    CONSTRAINT uq_trip_staff_assignment_trip_id_staff_id UNIQUE (trip_id, staff_id),
    CONSTRAINT ck_trip_staff_assignment_assignment_role CHECK (assignment_role IN (N'DRIVER', N'ESCORT', N'TEAM_LEADER'))
);

CREATE TABLE trip_status_history (
    id BIGINT IDENTITY(1,1) NOT NULL,
    trip_id BIGINT NOT NULL,
    previous_status NVARCHAR(40) NULL,
    new_status NVARCHAR(40) NOT NULL,
    changed_by_id BIGINT NOT NULL,
    changed_at DATETIME2(3) NOT NULL,
    note NVARCHAR(1000) NULL,
    CONSTRAINT pk_trip_status_history PRIMARY KEY (id),
    CONSTRAINT ck_trip_status_history_previous_status CHECK (previous_status IN (N'SCHEDULED', N'PREPARING', N'READY_FOR_DEPARTURE', N'IN_TRANSIT', N'AT_CHECKPOINT', N'AT_BORDER', N'CUSTOMS_CLEARANCE', N'QUARANTINE', N'DELAYED', N'ARRIVED', N'HANDOVER_PENDING', N'DELIVERED', N'CANCELLED')),
    CONSTRAINT ck_trip_status_history_new_status CHECK (new_status IN (N'SCHEDULED', N'PREPARING', N'READY_FOR_DEPARTURE', N'IN_TRANSIT', N'AT_CHECKPOINT', N'AT_BORDER', N'CUSTOMS_CLEARANCE', N'QUARANTINE', N'DELAYED', N'ARRIVED', N'HANDOVER_PENDING', N'DELIVERED', N'CANCELLED'))
);

CREATE TABLE tracking_event (
    id BIGINT IDENTITY(1,1) NOT NULL,
    trip_id BIGINT NOT NULL,
    route_stop_id BIGINT NULL,
    event_type NVARCHAR(40) NOT NULL,
    event_time DATETIME2(3) NOT NULL,
    location NVARCHAR(150) NULL,
    latitude DECIMAL(9,6) NULL,
    longitude DECIMAL(9,6) NULL,
    trip_status NVARCHAR(40) NOT NULL,
    delay_minutes INT NULL,
    note NVARCHAR(1000) NULL,
    recorded_by_id BIGINT NOT NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_tracking_event_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_tracking_event PRIMARY KEY (id),
    CONSTRAINT ck_tracking_event_event_type CHECK (event_type IN (N'DEPARTED', N'CHECKPOINT_ARRIVED', N'CHECKPOINT_DEPARTED', N'LOCATION_UPDATE', N'STATUS_CHANGE', N'DELAY', N'ARRIVED', N'NOTE')),
    CONSTRAINT ck_tracking_event_trip_status CHECK (trip_status IN (N'SCHEDULED', N'PREPARING', N'READY_FOR_DEPARTURE', N'IN_TRANSIT', N'AT_CHECKPOINT', N'AT_BORDER', N'CUSTOMS_CLEARANCE', N'QUARANTINE', N'DELAYED', N'ARRIVED', N'HANDOVER_PENDING', N'DELIVERED', N'CANCELLED'))
);

CREATE TABLE pre_transport_health_check (
    id BIGINT IDENTITY(1,1) NOT NULL,
    trip_id BIGINT NOT NULL,
    horse_id BIGINT NOT NULL,
    checked_by_id BIGINT NOT NULL,
    checked_at DATETIME2(3) NOT NULL,
    temperature_c DECIMAL(4,1) NULL,
    general_condition NVARCHAR(40) NOT NULL,
    identity_verified BIT NOT NULL CONSTRAINT df_pre_transport_health_check_identity_verified DEFAULT 0,
    documents_present BIT NOT NULL CONSTRAINT df_pre_transport_health_check_documents_present DEFAULT 0,
    loading_ok BIT NOT NULL CONSTRAINT df_pre_transport_health_check_loading_ok DEFAULT 0,
    fit_for_travel_observed BIT NOT NULL CONSTRAINT df_pre_transport_health_check_fit_for_travel_observed DEFAULT 0,
    notes NVARCHAR(1000) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_pre_transport_health_check_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_pre_transport_health_check PRIMARY KEY (id),
    CONSTRAINT uq_pre_transport_health_check_trip_id_horse_id UNIQUE (trip_id, horse_id),
    CONSTRAINT ck_pre_transport_health_check_general_condition CHECK (general_condition IN (N'GOOD', N'FAIR', N'POOR', N'INJURED'))
);

CREATE TABLE health_observation (
    id BIGINT IDENTITY(1,1) NOT NULL,
    trip_id BIGINT NOT NULL,
    horse_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    observed_at DATETIME2(3) NOT NULL,
    location NVARCHAR(150) NULL,
    appetite NVARCHAR(40) NOT NULL,
    water_intake NVARCHAR(40) NOT NULL,
    behavior NVARCHAR(40) NOT NULL,
    stress_level NVARCHAR(40) NOT NULL,
    movement NVARCHAR(40) NOT NULL,
    visible_injury BIT NOT NULL CONSTRAINT df_health_observation_visible_injury DEFAULT 0,
    injury_description NVARCHAR(500) NULL,
    temperature_c DECIMAL(4,1) NULL,
    severity NVARCHAR(40) NOT NULL,
    notes NVARCHAR(1000) NULL,
    incident_id BIGINT NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_health_observation_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_health_observation PRIMARY KEY (id),
    CONSTRAINT ck_health_observation_appetite CHECK (appetite IN (N'NORMAL', N'REDUCED', N'POOR', N'NONE')),
    CONSTRAINT ck_health_observation_water_intake CHECK (water_intake IN (N'NORMAL', N'REDUCED', N'POOR', N'NONE')),
    CONSTRAINT ck_health_observation_behavior CHECK (behavior IN (N'CALM', N'ALERT', N'RESTLESS', N'AGITATED', N'LETHARGIC')),
    CONSTRAINT ck_health_observation_stress_level CHECK (stress_level IN (N'LOW', N'MEDIUM', N'HIGH')),
    CONSTRAINT ck_health_observation_movement CHECK (movement IN (N'NORMAL', N'STIFF', N'LAME', N'REFUSES_TO_MOVE')),
    CONSTRAINT ck_health_observation_severity CHECK (severity IN (N'NORMAL', N'ATTENTION', N'URGENT'))
);

CREATE TABLE arrival_condition_check (
    id BIGINT IDENTITY(1,1) NOT NULL,
    trip_id BIGINT NOT NULL,
    horse_id BIGINT NOT NULL,
    checked_by_id BIGINT NOT NULL,
    checked_at DATETIME2(3) NOT NULL,
    condition NVARCHAR(40) NOT NULL,
    visible_injury BIT NOT NULL CONSTRAINT df_arrival_condition_check_visible_injury DEFAULT 0,
    notes NVARCHAR(1000) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_arrival_condition_check_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_arrival_condition_check PRIMARY KEY (id),
    CONSTRAINT uq_arrival_condition_check_trip_id_horse_id UNIQUE (trip_id, horse_id),
    CONSTRAINT ck_arrival_condition_check_condition CHECK (condition IN (N'GOOD', N'FAIR', N'POOR', N'INJURED'))
);

CREATE TABLE veterinary_consultation (
    id BIGINT IDENTITY(1,1) NOT NULL,
    incident_id BIGINT NULL,
    trip_id BIGINT NULL,
    horse_id BIGINT NULL,
    veterinarian_name NVARCHAR(150) NOT NULL,
    clinic NVARCHAR(150) NULL,
    contact NVARCHAR(150) NULL,
    consulted_at DATETIME2(3) NOT NULL,
    advice NVARCHAR(2000) NOT NULL,
    recommended_action NVARCHAR(40) NULL,
    report_file_id BIGINT NULL,
    recorded_by_id BIGINT NOT NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_veterinary_consultation_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_veterinary_consultation PRIMARY KEY (id),
    CONSTRAINT ck_veterinary_consultation_recommended_action CHECK (recommended_action IN (N'CONTINUE', N'MONITOR', N'STOP', N'REROUTE'))
);

CREATE TABLE incident (
    id BIGINT IDENTITY(1,1) NOT NULL,
    incident_number NVARCHAR(30) NOT NULL,
    trip_id BIGINT NOT NULL,
    horse_id BIGINT NULL,
    reported_by_id BIGINT NOT NULL,
    incident_type NVARCHAR(40) NOT NULL,
    severity NVARCHAR(40) NOT NULL,
    title NVARCHAR(200) NOT NULL,
    description NVARCHAR(2000) NOT NULL,
    location NVARCHAR(150) NULL,
    status NVARCHAR(40) NOT NULL,
    reported_at DATETIME2(3) NOT NULL,
    decision NVARCHAR(40) NULL,
    decided_by_id BIGINT NULL,
    decided_at DATETIME2(3) NULL,
    resolved_at DATETIME2(3) NULL,
    resolution_note NVARCHAR(2000) NULL,
    version BIGINT NOT NULL CONSTRAINT df_incident_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_incident_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_incident_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_incident PRIMARY KEY (id),
    CONSTRAINT uq_incident_incident_number UNIQUE (incident_number),
    CONSTRAINT ck_incident_incident_type CHECK (incident_type IN (N'HORSE_HEALTH', N'HORSE_INJURY', N'VEHICLE_BREAKDOWN', N'TRAFFIC_DELAY', N'BORDER_DELAY', N'DOCUMENT_ISSUE', N'WEATHER', N'SECURITY', N'OTHER')),
    CONSTRAINT ck_incident_severity CHECK (severity IN (N'LOW', N'MEDIUM', N'HIGH', N'CRITICAL')),
    CONSTRAINT ck_incident_status CHECK (status IN (N'OPEN', N'UNDER_REVIEW', N'ACTION_REQUIRED', N'MONITORING', N'RESOLVED', N'CLOSED')),
    CONSTRAINT ck_incident_decision CHECK (decision IN (N'CONTINUE', N'MONITOR', N'STOP', N'REROUTE'))
);

CREATE TABLE incident_action (
    id BIGINT IDENTITY(1,1) NOT NULL,
    incident_id BIGINT NOT NULL,
    action_type NVARCHAR(40) NOT NULL,
    decision NVARCHAR(40) NULL,
    note NVARCHAR(2000) NOT NULL,
    performed_by_id BIGINT NOT NULL,
    performed_at DATETIME2(3) NOT NULL,
    consultation_id BIGINT NULL,
    new_route_id BIGINT NULL,
    extra_cost DECIMAL(18,2) NULL,
    currency NVARCHAR(3) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_incident_action_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_incident_action PRIMARY KEY (id),
    CONSTRAINT ck_incident_action_action_type CHECK (action_type IN (N'NOTE', N'VET_CONTACTED', N'VET_ADVICE_RECORDED', N'DECISION', N'STATUS_CHANGE', N'ESCALATION', N'FIELD_ACTION')),
    CONSTRAINT ck_incident_action_decision CHECK (decision IN (N'CONTINUE', N'MONITOR', N'STOP', N'REROUTE'))
);

CREATE TABLE handover (
    id BIGINT IDENTITY(1,1) NOT NULL,
    handover_number NVARCHAR(30) NOT NULL,
    trip_id BIGINT NOT NULL,
    horse_id BIGINT NOT NULL,
    arrival_check_id BIGINT NULL,
    destination NVARCHAR(255) NOT NULL,
    handover_time DATETIME2(3) NOT NULL,
    receiver_name NVARCHAR(150) NOT NULL,
    receiver_contact NVARCHAR(150) NULL,
    condition NVARCHAR(40) NOT NULL,
    documents_handed_over BIT NOT NULL CONSTRAINT df_handover_documents_handed_over DEFAULT 0,
    notes NVARCHAR(2000) NULL,
    status NVARCHAR(40) NOT NULL,
    created_by_id BIGINT NOT NULL,
    confirmed_by_id BIGINT NULL,
    confirmed_at DATETIME2(3) NULL,
    customer_comment NVARCHAR(1000) NULL,
    version BIGINT NOT NULL CONSTRAINT df_handover_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_handover_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_handover_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_handover PRIMARY KEY (id),
    CONSTRAINT uq_handover_handover_number UNIQUE (handover_number),
    CONSTRAINT uq_handover_trip_id_horse_id UNIQUE (trip_id, horse_id),
    CONSTRAINT ck_handover_condition CHECK (condition IN (N'GOOD', N'FAIR', N'POOR', N'INJURED')),
    CONSTRAINT ck_handover_status CHECK (status IN (N'PENDING_CONFIRMATION', N'CONFIRMED', N'DISPUTED'))
);

CREATE TABLE pricing_rate (
    id BIGINT IDENTITY(1,1) NOT NULL,
    code NVARCHAR(40) NOT NULL,
    category NVARCHAR(40) NOT NULL,
    description NVARCHAR(200) NOT NULL,
    unit NVARCHAR(40) NOT NULL,
    unit_price DECIMAL(18,4) NOT NULL,
    currency NVARCHAR(3) NOT NULL CONSTRAINT df_pricing_rate_currency DEFAULT 'USD',
    transport_mode NVARCHAR(40) NULL,
    destination_country NVARCHAR(2) NULL,
    active BIT NOT NULL CONSTRAINT df_pricing_rate_active DEFAULT 1,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_pricing_rate_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_pricing_rate_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_pricing_rate PRIMARY KEY (id),
    CONSTRAINT uq_pricing_rate_code UNIQUE (code),
    CONSTRAINT ck_pricing_rate_category CHECK (category IN (N'GROUND_TRANSPORT', N'AIR_TRANSPORT', N'HANDLING', N'VETERINARY_TESTING', N'DOCUMENTS', N'CUSTOMS', N'QUARANTINE_STABLING', N'ESCORT', N'INSURANCE', N'SURCHARGE', N'DISCOUNT')),
    CONSTRAINT ck_pricing_rate_unit CHECK (unit IN (N'PER_KM', N'PER_HORSE', N'PER_TRIP', N'PER_HORSE_DAY', N'PERCENT_OF_DECLARED_VALUE')),
    CONSTRAINT ck_pricing_rate_transport_mode CHECK (transport_mode IN (N'ROAD', N'AIR', N'ROAD_AIR'))
);

CREATE TABLE quote (
    id BIGINT IDENTITY(1,1) NOT NULL,
    quote_number NVARCHAR(30) NOT NULL,
    request_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    quote_type NVARCHAR(40) NOT NULL,
    status NVARCHAR(40) NOT NULL,
    currency NVARCHAR(3) NOT NULL CONSTRAINT df_quote_currency DEFAULT 'USD',
    subtotal DECIMAL(18,2) NOT NULL CONSTRAINT df_quote_subtotal DEFAULT 0,
    discount_total DECIMAL(18,2) NOT NULL CONSTRAINT df_quote_discount_total DEFAULT 0,
    total_amount DECIMAL(18,2) NOT NULL CONSTRAINT df_quote_total_amount DEFAULT 0,
    estimated_total DECIMAL(18,2) NULL,
    valid_until DATE NULL,
    notes NVARCHAR(2000) NULL,
    created_by_id BIGINT NOT NULL,
    sent_at DATETIME2(3) NULL,
    accepted_at DATETIME2(3) NULL,
    rejected_at DATETIME2(3) NULL,
    customer_response_note NVARCHAR(1000) NULL,
    version BIGINT NOT NULL CONSTRAINT df_quote_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_quote_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_quote_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_quote PRIMARY KEY (id),
    CONSTRAINT uq_quote_quote_number UNIQUE (quote_number),
    CONSTRAINT uq_quote_request_id_version_no UNIQUE (request_id, version_no),
    CONSTRAINT ck_quote_quote_type CHECK (quote_type IN (N'ESTIMATE', N'FINAL')),
    CONSTRAINT ck_quote_status CHECK (status IN (N'ESTIMATED', N'DRAFT', N'SENT', N'ACCEPTED', N'REJECTED', N'EXPIRED', N'SUPERSEDED'))
);

CREATE TABLE quote_item (
    id BIGINT IDENTITY(1,1) NOT NULL,
    quote_id BIGINT NOT NULL,
    category NVARCHAR(40) NOT NULL,
    description NVARCHAR(255) NOT NULL,
    quantity DECIMAL(12,2) NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    estimated_amount DECIMAL(18,2) NULL,
    confirmed BIT NOT NULL CONSTRAINT df_quote_item_confirmed DEFAULT 0,
    source NVARCHAR(150) NULL,
    sort_order INT NOT NULL CONSTRAINT df_quote_item_sort_order DEFAULT 0,
    CONSTRAINT pk_quote_item PRIMARY KEY (id),
    CONSTRAINT ck_quote_item_category CHECK (category IN (N'GROUND_TRANSPORT', N'AIR_TRANSPORT', N'HANDLING', N'VETERINARY_TESTING', N'DOCUMENTS', N'CUSTOMS', N'QUARANTINE_STABLING', N'ESCORT', N'INSURANCE', N'SURCHARGE', N'DISCOUNT'))
);

CREATE TABLE invoice (
    id BIGINT IDENTITY(1,1) NOT NULL,
    invoice_number NVARCHAR(30) NOT NULL,
    quote_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    currency NVARCHAR(3) NOT NULL CONSTRAINT df_invoice_currency DEFAULT 'USD',
    total_amount DECIMAL(18,2) NOT NULL,
    paid_amount DECIMAL(18,2) NOT NULL CONSTRAINT df_invoice_paid_amount DEFAULT 0,
    deposit_percent DECIMAL(5,2) NOT NULL CONSTRAINT df_invoice_deposit_percent DEFAULT 50,
    status NVARCHAR(40) NOT NULL,
    version BIGINT NOT NULL CONSTRAINT df_invoice_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_invoice_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_invoice_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_invoice PRIMARY KEY (id),
    CONSTRAINT uq_invoice_invoice_number UNIQUE (invoice_number),
    CONSTRAINT uq_invoice_quote_id UNIQUE (quote_id),
    CONSTRAINT ck_invoice_status CHECK (status IN (N'ISSUED', N'PARTIALLY_PAID', N'PAID', N'OVERDUE', N'CANCELLED'))
);

CREATE TABLE payment (
    id BIGINT IDENTITY(1,1) NOT NULL,
    payment_reference NVARCHAR(40) NOT NULL,
    invoice_id BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    currency NVARCHAR(3) NOT NULL CONSTRAINT df_payment_currency DEFAULT 'USD',
    method NVARCHAR(40) NOT NULL,
    status NVARCHAR(40) NOT NULL,
    card_last4 NVARCHAR(4) NULL,
    paid_at DATETIME2(3) NULL,
    note NVARCHAR(500) NULL,
    paid_by_id BIGINT NOT NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_payment_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_payment PRIMARY KEY (id),
    CONSTRAINT uq_payment_payment_reference UNIQUE (payment_reference),
    CONSTRAINT ck_payment_method CHECK (method IN (N'SIMULATED_CARD', N'SIMULATED_BANK_TRANSFER', N'SIMULATED_EWALLET')),
    CONSTRAINT ck_payment_status CHECK (status IN (N'SUCCEEDED', N'FAILED'))
);

CREATE TABLE claim (
    id BIGINT IDENTITY(1,1) NOT NULL,
    claim_number NVARCHAR(30) NOT NULL,
    request_id BIGINT NOT NULL,
    trip_id BIGINT NULL,
    incident_id BIGINT NULL,
    horse_id BIGINT NULL,
    customer_id BIGINT NOT NULL,
    claim_type NVARCHAR(40) NOT NULL,
    claimed_amount DECIMAL(18,2) NOT NULL,
    approved_amount DECIMAL(18,2) NULL,
    currency NVARCHAR(3) NOT NULL CONSTRAINT df_claim_currency DEFAULT 'USD',
    description NVARCHAR(2000) NOT NULL,
    status NVARCHAR(40) NOT NULL,
    submitted_at DATETIME2(3) NOT NULL,
    reviewed_by_id BIGINT NULL,
    decided_at DATETIME2(3) NULL,
    decision_note NVARCHAR(2000) NULL,
    closed_at DATETIME2(3) NULL,
    version BIGINT NOT NULL CONSTRAINT df_claim_version DEFAULT 0,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_claim_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(3) NOT NULL CONSTRAINT df_claim_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT pk_claim PRIMARY KEY (id),
    CONSTRAINT uq_claim_claim_number UNIQUE (claim_number),
    CONSTRAINT ck_claim_claim_type CHECK (claim_type IN (N'INJURY_MEDICAL_EXPENSE', N'MORTALITY', N'HUMANE_EUTHANASIA', N'THEFT_OR_LOSS', N'PROPERTY_DAMAGE', N'SERVICE_DELAY', N'OTHER_DIRECT_LOSS')),
    CONSTRAINT ck_claim_status CHECK (status IN (N'SUBMITTED', N'UNDER_REVIEW', N'EVIDENCE_REQUIRED', N'APPROVED', N'PARTIALLY_APPROVED', N'REJECTED', N'PAID', N'CLOSED')),
    CONSTRAINT ck_claim_1 CHECK (approved_amount IS NULL OR approved_amount <= claimed_amount)
);

CREATE TABLE claim_evidence (
    id BIGINT IDENTITY(1,1) NOT NULL,
    claim_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    evidence_type NVARCHAR(40) NOT NULL,
    notes NVARCHAR(500) NULL,
    uploaded_by_id BIGINT NOT NULL,
    uploaded_at DATETIME2(3) NOT NULL,
    CONSTRAINT pk_claim_evidence PRIMARY KEY (id),
    CONSTRAINT ck_claim_evidence_evidence_type CHECK (evidence_type IN (N'PHOTO', N'VET_REPORT', N'RECEIPT', N'OFFICIAL_REPORT', N'OTHER'))
);

CREATE TABLE claim_assessment (
    id BIGINT IDENTITY(1,1) NOT NULL,
    claim_id BIGINT NOT NULL,
    assessment_note NVARCHAR(2000) NOT NULL,
    recommendation NVARCHAR(40) NOT NULL,
    recommended_amount DECIMAL(18,2) NULL,
    assessed_by_id BIGINT NOT NULL,
    assessed_at DATETIME2(3) NOT NULL,
    CONSTRAINT pk_claim_assessment PRIMARY KEY (id),
    CONSTRAINT ck_claim_assessment_recommendation CHECK (recommendation IN (N'APPROVE', N'PARTIALLY_APPROVE', N'REJECT', N'REQUEST_EVIDENCE'))
);

CREATE TABLE claim_settlement (
    id BIGINT IDENTITY(1,1) NOT NULL,
    claim_id BIGINT NOT NULL,
    settlement_reference NVARCHAR(40) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    currency NVARCHAR(3) NOT NULL CONSTRAINT df_claim_settlement_currency DEFAULT 'USD',
    status NVARCHAR(40) NOT NULL,
    settled_at DATETIME2(3) NOT NULL,
    note NVARCHAR(500) NULL,
    settled_by_id BIGINT NOT NULL,
    CONSTRAINT pk_claim_settlement PRIMARY KEY (id),
    CONSTRAINT uq_claim_settlement_settlement_reference UNIQUE (settlement_reference),
    CONSTRAINT ck_claim_settlement_status CHECK (status IN (N'PAID', N'FAILED'))
);

-- ------------------------------------------------------------ khóa ngoại
ALTER TABLE customer_profile ADD CONSTRAINT fk_customer_profile_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE employee_profile ADD CONSTRAINT fk_employee_profile_user_id FOREIGN KEY (user_id) REFERENCES app_user(id);
ALTER TABLE notification ADD CONSTRAINT fk_notification_recipient_id FOREIGN KEY (recipient_id) REFERENCES app_user(id);
ALTER TABLE horse ADD CONSTRAINT fk_horse_owner_id FOREIGN KEY (owner_id) REFERENCES app_user(id);
ALTER TABLE transportation_request ADD CONSTRAINT fk_transportation_request_customer_id FOREIGN KEY (customer_id) REFERENCES app_user(id);
ALTER TABLE request_horse ADD CONSTRAINT fk_request_horse_request_id FOREIGN KEY (request_id) REFERENCES transportation_request(id);
ALTER TABLE request_horse ADD CONSTRAINT fk_request_horse_horse_id FOREIGN KEY (horse_id) REFERENCES horse(id);
ALTER TABLE request_horse ADD CONSTRAINT fk_request_horse_stall_type_id FOREIGN KEY (stall_type_id) REFERENCES stall_type(id);
ALTER TABLE request_status_history ADD CONSTRAINT fk_request_status_history_request_id FOREIGN KEY (request_id) REFERENCES transportation_request(id);
ALTER TABLE request_status_history ADD CONSTRAINT fk_request_status_history_changed_by_id FOREIGN KEY (changed_by_id) REFERENCES app_user(id);
ALTER TABLE stored_file ADD CONSTRAINT fk_stored_file_uploaded_by_id FOREIGN KEY (uploaded_by_id) REFERENCES app_user(id);
ALTER TABLE document ADD CONSTRAINT fk_document_request_id FOREIGN KEY (request_id) REFERENCES transportation_request(id);
ALTER TABLE document ADD CONSTRAINT fk_document_horse_id FOREIGN KEY (horse_id) REFERENCES horse(id);
ALTER TABLE document ADD CONSTRAINT fk_document_file_id FOREIGN KEY (file_id) REFERENCES stored_file(id);
ALTER TABLE document ADD CONSTRAINT fk_document_reviewer_id FOREIGN KEY (reviewer_id) REFERENCES app_user(id);
ALTER TABLE document ADD CONSTRAINT fk_document_uploaded_by_id FOREIGN KEY (uploaded_by_id) REFERENCES app_user(id);
ALTER TABLE attachment ADD CONSTRAINT fk_attachment_file_id FOREIGN KEY (file_id) REFERENCES stored_file(id);
ALTER TABLE compliance_checklist_item ADD CONSTRAINT fk_compliance_checklist_item_request_id FOREIGN KEY (request_id) REFERENCES transportation_request(id);
ALTER TABLE compliance_checklist_item ADD CONSTRAINT fk_compliance_checklist_item_requirement_id FOREIGN KEY (requirement_id) REFERENCES compliance_requirement(id);
ALTER TABLE compliance_checklist_item ADD CONSTRAINT fk_compliance_checklist_item_horse_id FOREIGN KEY (horse_id) REFERENCES horse(id);
ALTER TABLE compliance_checklist_item ADD CONSTRAINT fk_compliance_checklist_item_document_id FOREIGN KEY (document_id) REFERENCES document(id);
ALTER TABLE compliance_checklist_item ADD CONSTRAINT fk_compliance_checklist_item_checked_by_id FOREIGN KEY (checked_by_id) REFERENCES app_user(id);
ALTER TABLE route ADD CONSTRAINT fk_route_alternative_of_id FOREIGN KEY (alternative_of_id) REFERENCES route(id);
ALTER TABLE route_stop ADD CONSTRAINT fk_route_stop_route_id FOREIGN KEY (route_id) REFERENCES route(id);
ALTER TABLE transportation_trip ADD CONSTRAINT fk_transportation_trip_request_id FOREIGN KEY (request_id) REFERENCES transportation_request(id);
ALTER TABLE transportation_trip ADD CONSTRAINT fk_transportation_trip_route_id FOREIGN KEY (route_id) REFERENCES route(id);
ALTER TABLE transportation_trip ADD CONSTRAINT fk_transportation_trip_vehicle_id FOREIGN KEY (vehicle_id) REFERENCES vehicle(id);
ALTER TABLE transportation_trip ADD CONSTRAINT fk_transportation_trip_coordinator_id FOREIGN KEY (coordinator_id) REFERENCES app_user(id);
ALTER TABLE transportation_trip ADD CONSTRAINT fk_transportation_trip_current_stop_id FOREIGN KEY (current_stop_id) REFERENCES route_stop(id);
ALTER TABLE trip_staff_assignment ADD CONSTRAINT fk_trip_staff_assignment_trip_id FOREIGN KEY (trip_id) REFERENCES transportation_trip(id);
ALTER TABLE trip_staff_assignment ADD CONSTRAINT fk_trip_staff_assignment_staff_id FOREIGN KEY (staff_id) REFERENCES app_user(id);
ALTER TABLE trip_staff_assignment ADD CONSTRAINT fk_trip_staff_assignment_assigned_by_id FOREIGN KEY (assigned_by_id) REFERENCES app_user(id);
ALTER TABLE trip_status_history ADD CONSTRAINT fk_trip_status_history_trip_id FOREIGN KEY (trip_id) REFERENCES transportation_trip(id);
ALTER TABLE trip_status_history ADD CONSTRAINT fk_trip_status_history_changed_by_id FOREIGN KEY (changed_by_id) REFERENCES app_user(id);
ALTER TABLE tracking_event ADD CONSTRAINT fk_tracking_event_trip_id FOREIGN KEY (trip_id) REFERENCES transportation_trip(id);
ALTER TABLE tracking_event ADD CONSTRAINT fk_tracking_event_route_stop_id FOREIGN KEY (route_stop_id) REFERENCES route_stop(id);
ALTER TABLE tracking_event ADD CONSTRAINT fk_tracking_event_recorded_by_id FOREIGN KEY (recorded_by_id) REFERENCES app_user(id);
ALTER TABLE pre_transport_health_check ADD CONSTRAINT fk_pre_transport_health_check_trip_id FOREIGN KEY (trip_id) REFERENCES transportation_trip(id);
ALTER TABLE pre_transport_health_check ADD CONSTRAINT fk_pre_transport_health_check_horse_id FOREIGN KEY (horse_id) REFERENCES horse(id);
ALTER TABLE pre_transport_health_check ADD CONSTRAINT fk_pre_transport_health_check_checked_by_id FOREIGN KEY (checked_by_id) REFERENCES app_user(id);
ALTER TABLE health_observation ADD CONSTRAINT fk_health_observation_trip_id FOREIGN KEY (trip_id) REFERENCES transportation_trip(id);
ALTER TABLE health_observation ADD CONSTRAINT fk_health_observation_horse_id FOREIGN KEY (horse_id) REFERENCES horse(id);
ALTER TABLE health_observation ADD CONSTRAINT fk_health_observation_author_id FOREIGN KEY (author_id) REFERENCES app_user(id);
ALTER TABLE health_observation ADD CONSTRAINT fk_health_observation_incident_id FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE arrival_condition_check ADD CONSTRAINT fk_arrival_condition_check_trip_id FOREIGN KEY (trip_id) REFERENCES transportation_trip(id);
ALTER TABLE arrival_condition_check ADD CONSTRAINT fk_arrival_condition_check_horse_id FOREIGN KEY (horse_id) REFERENCES horse(id);
ALTER TABLE arrival_condition_check ADD CONSTRAINT fk_arrival_condition_check_checked_by_id FOREIGN KEY (checked_by_id) REFERENCES app_user(id);
ALTER TABLE veterinary_consultation ADD CONSTRAINT fk_veterinary_consultation_incident_id FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE veterinary_consultation ADD CONSTRAINT fk_veterinary_consultation_trip_id FOREIGN KEY (trip_id) REFERENCES transportation_trip(id);
ALTER TABLE veterinary_consultation ADD CONSTRAINT fk_veterinary_consultation_horse_id FOREIGN KEY (horse_id) REFERENCES horse(id);
ALTER TABLE veterinary_consultation ADD CONSTRAINT fk_veterinary_consultation_report_file_id FOREIGN KEY (report_file_id) REFERENCES stored_file(id);
ALTER TABLE veterinary_consultation ADD CONSTRAINT fk_veterinary_consultation_recorded_by_id FOREIGN KEY (recorded_by_id) REFERENCES app_user(id);
ALTER TABLE incident ADD CONSTRAINT fk_incident_trip_id FOREIGN KEY (trip_id) REFERENCES transportation_trip(id);
ALTER TABLE incident ADD CONSTRAINT fk_incident_horse_id FOREIGN KEY (horse_id) REFERENCES horse(id);
ALTER TABLE incident ADD CONSTRAINT fk_incident_reported_by_id FOREIGN KEY (reported_by_id) REFERENCES app_user(id);
ALTER TABLE incident ADD CONSTRAINT fk_incident_decided_by_id FOREIGN KEY (decided_by_id) REFERENCES app_user(id);
ALTER TABLE incident_action ADD CONSTRAINT fk_incident_action_incident_id FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE incident_action ADD CONSTRAINT fk_incident_action_performed_by_id FOREIGN KEY (performed_by_id) REFERENCES app_user(id);
ALTER TABLE incident_action ADD CONSTRAINT fk_incident_action_consultation_id FOREIGN KEY (consultation_id) REFERENCES veterinary_consultation(id);
ALTER TABLE incident_action ADD CONSTRAINT fk_incident_action_new_route_id FOREIGN KEY (new_route_id) REFERENCES route(id);
ALTER TABLE handover ADD CONSTRAINT fk_handover_trip_id FOREIGN KEY (trip_id) REFERENCES transportation_trip(id);
ALTER TABLE handover ADD CONSTRAINT fk_handover_horse_id FOREIGN KEY (horse_id) REFERENCES horse(id);
ALTER TABLE handover ADD CONSTRAINT fk_handover_arrival_check_id FOREIGN KEY (arrival_check_id) REFERENCES arrival_condition_check(id);
ALTER TABLE handover ADD CONSTRAINT fk_handover_created_by_id FOREIGN KEY (created_by_id) REFERENCES app_user(id);
ALTER TABLE handover ADD CONSTRAINT fk_handover_confirmed_by_id FOREIGN KEY (confirmed_by_id) REFERENCES app_user(id);
ALTER TABLE quote ADD CONSTRAINT fk_quote_request_id FOREIGN KEY (request_id) REFERENCES transportation_request(id);
ALTER TABLE quote ADD CONSTRAINT fk_quote_created_by_id FOREIGN KEY (created_by_id) REFERENCES app_user(id);
ALTER TABLE quote_item ADD CONSTRAINT fk_quote_item_quote_id FOREIGN KEY (quote_id) REFERENCES quote(id);
ALTER TABLE invoice ADD CONSTRAINT fk_invoice_quote_id FOREIGN KEY (quote_id) REFERENCES quote(id);
ALTER TABLE invoice ADD CONSTRAINT fk_invoice_request_id FOREIGN KEY (request_id) REFERENCES transportation_request(id);
ALTER TABLE invoice ADD CONSTRAINT fk_invoice_customer_id FOREIGN KEY (customer_id) REFERENCES app_user(id);
ALTER TABLE payment ADD CONSTRAINT fk_payment_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoice(id);
ALTER TABLE payment ADD CONSTRAINT fk_payment_paid_by_id FOREIGN KEY (paid_by_id) REFERENCES app_user(id);
ALTER TABLE claim ADD CONSTRAINT fk_claim_request_id FOREIGN KEY (request_id) REFERENCES transportation_request(id);
ALTER TABLE claim ADD CONSTRAINT fk_claim_trip_id FOREIGN KEY (trip_id) REFERENCES transportation_trip(id);
ALTER TABLE claim ADD CONSTRAINT fk_claim_incident_id FOREIGN KEY (incident_id) REFERENCES incident(id);
ALTER TABLE claim ADD CONSTRAINT fk_claim_horse_id FOREIGN KEY (horse_id) REFERENCES horse(id);
ALTER TABLE claim ADD CONSTRAINT fk_claim_customer_id FOREIGN KEY (customer_id) REFERENCES app_user(id);
ALTER TABLE claim ADD CONSTRAINT fk_claim_reviewed_by_id FOREIGN KEY (reviewed_by_id) REFERENCES app_user(id);
ALTER TABLE claim_evidence ADD CONSTRAINT fk_claim_evidence_claim_id FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE claim_evidence ADD CONSTRAINT fk_claim_evidence_file_id FOREIGN KEY (file_id) REFERENCES stored_file(id);
ALTER TABLE claim_evidence ADD CONSTRAINT fk_claim_evidence_uploaded_by_id FOREIGN KEY (uploaded_by_id) REFERENCES app_user(id);
ALTER TABLE claim_assessment ADD CONSTRAINT fk_claim_assessment_claim_id FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE claim_assessment ADD CONSTRAINT fk_claim_assessment_assessed_by_id FOREIGN KEY (assessed_by_id) REFERENCES app_user(id);
ALTER TABLE claim_settlement ADD CONSTRAINT fk_claim_settlement_claim_id FOREIGN KEY (claim_id) REFERENCES claim(id);
ALTER TABLE claim_settlement ADD CONSTRAINT fk_claim_settlement_settled_by_id FOREIGN KEY (settled_by_id) REFERENCES app_user(id);

-- ------------------------------------------------------------ chỉ mục
CREATE INDEX ix_app_user_role ON app_user(role);
CREATE INDEX ix_notification_recipient_id ON notification(recipient_id);
CREATE INDEX ix_notification_recipient_id_read_at ON notification(recipient_id, read_at);
CREATE INDEX ix_horse_owner_id ON horse(owner_id);
CREATE INDEX ix_horse_status ON horse(status);
CREATE INDEX ix_transportation_request_customer_id ON transportation_request(customer_id);
CREATE INDEX ix_transportation_request_status ON transportation_request(status);
CREATE INDEX ix_request_horse_horse_id ON request_horse(horse_id);
CREATE INDEX ix_request_horse_stall_type_id ON request_horse(stall_type_id);
CREATE INDEX ix_request_status_history_request_id ON request_status_history(request_id);
CREATE INDEX ix_request_status_history_changed_by_id ON request_status_history(changed_by_id);
CREATE INDEX ix_stored_file_uploaded_by_id ON stored_file(uploaded_by_id);
CREATE INDEX ix_document_request_id ON document(request_id);
CREATE INDEX ix_document_horse_id ON document(horse_id);
CREATE INDEX ix_document_file_id ON document(file_id);
CREATE INDEX ix_document_review_status ON document(review_status);
CREATE INDEX ix_document_reviewer_id ON document(reviewer_id);
CREATE INDEX ix_document_uploaded_by_id ON document(uploaded_by_id);
CREATE INDEX ix_attachment_file_id ON attachment(file_id);
CREATE INDEX ix_attachment_ref_type_ref_id ON attachment(ref_type, ref_id);
CREATE INDEX ix_compliance_checklist_item_requirement_id ON compliance_checklist_item(requirement_id);
CREATE INDEX ix_compliance_checklist_item_horse_id ON compliance_checklist_item(horse_id);
CREATE INDEX ix_compliance_checklist_item_status ON compliance_checklist_item(status);
CREATE INDEX ix_compliance_checklist_item_document_id ON compliance_checklist_item(document_id);
CREATE INDEX ix_compliance_checklist_item_checked_by_id ON compliance_checklist_item(checked_by_id);
CREATE UNIQUE INDEX ux_compliance_checklist_item_request_id_requirement_id_horse_id ON compliance_checklist_item(request_id, requirement_id, horse_id) WHERE horse_id IS NOT NULL;
CREATE UNIQUE INDEX ux_compliance_checklist_item_request_id_requirement_id_null ON compliance_checklist_item(request_id, requirement_id) WHERE horse_id IS NULL;
CREATE INDEX ix_vehicle_status ON vehicle(status);
CREATE INDEX ix_route_alternative_of_id ON route(alternative_of_id);
CREATE INDEX ix_transportation_trip_request_id ON transportation_trip(request_id);
CREATE INDEX ix_transportation_trip_route_id ON transportation_trip(route_id);
CREATE INDEX ix_transportation_trip_vehicle_id ON transportation_trip(vehicle_id);
CREATE INDEX ix_transportation_trip_coordinator_id ON transportation_trip(coordinator_id);
CREATE INDEX ix_transportation_trip_status ON transportation_trip(status);
CREATE INDEX ix_transportation_trip_current_stop_id ON transportation_trip(current_stop_id);
CREATE INDEX ix_transportation_trip_planned_departure_planned_arrival ON transportation_trip(planned_departure, planned_arrival);
CREATE INDEX ix_trip_staff_assignment_staff_id ON trip_staff_assignment(staff_id);
CREATE INDEX ix_trip_staff_assignment_assigned_by_id ON trip_staff_assignment(assigned_by_id);
CREATE INDEX ix_trip_status_history_trip_id ON trip_status_history(trip_id);
CREATE INDEX ix_trip_status_history_changed_by_id ON trip_status_history(changed_by_id);
CREATE INDEX ix_tracking_event_trip_id ON tracking_event(trip_id);
CREATE INDEX ix_tracking_event_route_stop_id ON tracking_event(route_stop_id);
CREATE INDEX ix_tracking_event_recorded_by_id ON tracking_event(recorded_by_id);
CREATE INDEX ix_tracking_event_trip_id_event_time ON tracking_event(trip_id, event_time);
CREATE INDEX ix_pre_transport_health_check_horse_id ON pre_transport_health_check(horse_id);
CREATE INDEX ix_pre_transport_health_check_checked_by_id ON pre_transport_health_check(checked_by_id);
CREATE INDEX ix_health_observation_trip_id ON health_observation(trip_id);
CREATE INDEX ix_health_observation_horse_id ON health_observation(horse_id);
CREATE INDEX ix_health_observation_author_id ON health_observation(author_id);
CREATE INDEX ix_health_observation_incident_id ON health_observation(incident_id);
CREATE INDEX ix_health_observation_trip_id_observed_at ON health_observation(trip_id, observed_at);
CREATE INDEX ix_arrival_condition_check_horse_id ON arrival_condition_check(horse_id);
CREATE INDEX ix_arrival_condition_check_checked_by_id ON arrival_condition_check(checked_by_id);
CREATE INDEX ix_veterinary_consultation_incident_id ON veterinary_consultation(incident_id);
CREATE INDEX ix_veterinary_consultation_trip_id ON veterinary_consultation(trip_id);
CREATE INDEX ix_veterinary_consultation_horse_id ON veterinary_consultation(horse_id);
CREATE INDEX ix_veterinary_consultation_report_file_id ON veterinary_consultation(report_file_id);
CREATE INDEX ix_veterinary_consultation_recorded_by_id ON veterinary_consultation(recorded_by_id);
CREATE INDEX ix_incident_trip_id ON incident(trip_id);
CREATE INDEX ix_incident_horse_id ON incident(horse_id);
CREATE INDEX ix_incident_reported_by_id ON incident(reported_by_id);
CREATE INDEX ix_incident_status ON incident(status);
CREATE INDEX ix_incident_decided_by_id ON incident(decided_by_id);
CREATE INDEX ix_incident_action_incident_id ON incident_action(incident_id);
CREATE INDEX ix_incident_action_performed_by_id ON incident_action(performed_by_id);
CREATE INDEX ix_incident_action_consultation_id ON incident_action(consultation_id);
CREATE INDEX ix_incident_action_new_route_id ON incident_action(new_route_id);
CREATE INDEX ix_handover_horse_id ON handover(horse_id);
CREATE INDEX ix_handover_arrival_check_id ON handover(arrival_check_id);
CREATE INDEX ix_handover_created_by_id ON handover(created_by_id);
CREATE INDEX ix_handover_confirmed_by_id ON handover(confirmed_by_id);
CREATE INDEX ix_quote_status ON quote(status);
CREATE INDEX ix_quote_created_by_id ON quote(created_by_id);
CREATE INDEX ix_quote_item_quote_id ON quote_item(quote_id);
CREATE INDEX ix_invoice_request_id ON invoice(request_id);
CREATE INDEX ix_invoice_customer_id ON invoice(customer_id);
CREATE INDEX ix_invoice_status ON invoice(status);
CREATE INDEX ix_payment_invoice_id ON payment(invoice_id);
CREATE INDEX ix_payment_paid_by_id ON payment(paid_by_id);
CREATE INDEX ix_claim_request_id ON claim(request_id);
CREATE INDEX ix_claim_trip_id ON claim(trip_id);
CREATE INDEX ix_claim_incident_id ON claim(incident_id);
CREATE INDEX ix_claim_horse_id ON claim(horse_id);
CREATE INDEX ix_claim_customer_id ON claim(customer_id);
CREATE INDEX ix_claim_status ON claim(status);
CREATE INDEX ix_claim_reviewed_by_id ON claim(reviewed_by_id);
CREATE INDEX ix_claim_evidence_claim_id ON claim_evidence(claim_id);
CREATE INDEX ix_claim_evidence_file_id ON claim_evidence(file_id);
CREATE INDEX ix_claim_evidence_uploaded_by_id ON claim_evidence(uploaded_by_id);
CREATE INDEX ix_claim_assessment_claim_id ON claim_assessment(claim_id);
CREATE INDEX ix_claim_assessment_assessed_by_id ON claim_assessment(assessed_by_id);
CREATE INDEX ix_claim_settlement_claim_id ON claim_settlement(claim_id);
CREATE INDEX ix_claim_settlement_settled_by_id ON claim_settlement(settled_by_id);
