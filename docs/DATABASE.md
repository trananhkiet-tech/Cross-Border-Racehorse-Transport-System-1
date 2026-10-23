# Database – IRTMS

Hệ quản trị: **Microsoft SQL Server 2019+** (database `irtms`, collation `Vietnamese_100_CI_AS`). Schema do **Flyway** quản lý
(`backend/src/main/resources/db/migration/V1__init_schema.sql`, `V2__reference_data.sql`); Hibernate để `ddl-auto: none`.
Tạo database bằng `database/create-database.sql` (xem `database/README.md` và README gốc).

> **Trạng thái kiểm chứng:** cú pháp T-SQL của hai file migration đã parse thành công bằng `sqlglot`; schema và entity JPA được sinh từ cùng một mô hình dữ liệu.
> Migration **chưa** được chạy trên SQL Server thật trong môi trường phát triển (không có SQL Server/Maven Central). Cần chạy theo README trên máy có SQL Server để xác nhận.

## 1. Tổng quan

* **38 bảng**, 45 enum, 93 chỉ mục (gồm 2 chỉ mục unique có điều kiện `WHERE` cho checklist tuân thủ).
* Khóa chính `BIGINT IDENTITY`. Thời gian `DATETIME2(3)` **không** kèm múi giờ (giờ địa phương theo `APP_TIME_ZONE`).
* Tiền `DECIMAL(18,2)` kèm cột `currency`. Chuỗi là `NVARCHAR` để lưu tiếng Việt.
* 5 role nằm trong `app_user.role` (ràng buộc `CHECK`); **không có role thú y**.

## 2. Quyết định thiết kế

| Chủ đề | Quyết định | Lý do |
|---|---|---|
| Enum | Lưu `NVARCHAR` + ràng buộc `CHECK (... IN (...))`, map `@Enumerated(STRING)` | Danh sách hữu hạn, ít thay đổi; DB tự chặn giá trị sai; dễ đọc khi truy vấn. |
| Dữ liệu thay đổi theo quản trị | Bảng riêng: `stall_type`, `pricing_rate`, `compliance_requirement` | Manager/Specialist chỉnh được mà không sửa code. |
| Hồ sơ người dùng | `app_user` + `customer_profile` / `employee_profile` (1–1) | Một tài khoản – một role; thông tin riêng theo loại người dùng. |
| Yêu cầu nhiều ngựa | `transportation_request` – `request_horse` (N–N có thuộc tính: chuồng, giá trị khai báo) | Mỗi ngựa trong yêu cầu có loại chuồng và giá trị khai báo riêng. |
| Giá trị khai báo | Cột `request_horse.declared_value` chỉ để tham chiếu / bảo hiểm / trần bồi thường tổn thất toàn phần | Không phải giá vận chuyển, không tự quyết định số tiền bồi thường. |
| Tài liệu & tệp | `stored_file` (siêu dữ liệu: tên lưu UUID, loại thật, SHA-256, kích thước) tách khỏi `document`, `attachment` | Một tệp vật lý, nhiều nơi tham chiếu; tên người dùng không bao giờ là đường dẫn. |
| Checklist tuân thủ | `compliance_requirement` (danh mục theo tuyến/loại/mục đích) → `compliance_checklist_item` sinh cho từng yêu cầu (và từng ngựa nếu cần) | Không hard-code mọi quốc gia như nhau; unique có điều kiện ngăn trùng mục. |
| Báo giá | `quote` có `quote_type` (ESTIMATED/FINAL), `version_no`, `status`, unique `(request_id, version_no)`; dòng chi phí ở `quote_item` | Sửa sau khi gửi tạo **phiên bản mới**, bản cũ → `SUPERSEDED` (giữ lịch sử, không ghi đè). |
| Hóa đơn/thanh toán | `invoice` (1–1 với báo giá đã chấp nhận) – `payment` (nhiều lần, kết quả mô phỏng) | Số tiền lấy từ báo giá ở backend; chỉ lưu 4 số cuối mô phỏng (`card_last4`), không lưu số thẻ. |
| Chuyến đi | `transportation_trip` – `trip_staff_assignment` (vai trò DRIVER/ESCORT/TEAM_LEADER) – `trip_status_history` – `tracking_event` | Mọi đổi trạng thái đều có lịch sử; sự kiện hành trình tách khỏi trạng thái. |
| Ba loại dữ liệu sức khỏe | (1) `document` loại sức khỏe; (2) `pre_transport_health_check`, `health_observation`, `arrival_condition_check` do Driver/Escort quan sát; (3) `veterinary_consultation` do thú y bên ngoài | Tránh nhầm "quan sát" với "chẩn đoán" và với "giấy tờ". |
| Sự cố vs bồi thường | `incident` + `incident_action` tách hẳn `claim` + `claim_evidence/assessment/settlement` | Sự cố là vận hành; bồi thường là tài chính – Incident không tự thành Claim. |
| Bàn giao | `handover` mỗi (chuyến, ngựa) một bản; tham chiếu `arrival_condition_check` | Chuyến chỉ `DELIVERED` khi mọi ngựa đã được khách xác nhận. |
| Mã nghiệp vụ | `code_sequence` (khóa bi quan) sinh `REQ-2026-0001`, `TRIP-…`, `INC-…`, `CLM-…`… | Mã ổn định, đọc được, không phụ thuộc IDENTITY. |
| Đồng thời | Cột `version` (`@Version`) ở bảng hay bị sửa đồng thời → lỗi 409 `CONCURRENT_UPDATE` | Tránh ghi đè âm thầm khi hai người cùng thao tác. |
| Xóa dữ liệu | Không xóa cứng dữ liệu nghiệp vụ; ngựa/xe dùng `status`/`archived` | Giữ lịch sử và khóa ngoại. |

## 3. Sơ đồ quan hệ (ERD)

```mermaid
erDiagram
    app_user ||--o| customer_profile : "user_id"
    app_user ||--o| employee_profile : "user_id"
    app_user ||--o{ notification : "recipient_id"
    app_user ||--o{ horse : "owner_id"
    app_user ||--o{ transportation_request : "customer_id"
    transportation_request ||--o{ request_horse : "request_id"
    horse ||--o{ request_horse : "horse_id"
    stall_type ||--o{ request_horse : "stall_type_id"
    transportation_request ||--o{ request_status_history : "request_id"
    app_user ||--o{ request_status_history : "changed_by_id"
    app_user ||--o{ stored_file : "uploaded_by_id"
    transportation_request ||--o{ document : "request_id"
    horse ||--o{ document : "horse_id"
    stored_file ||--o{ document : "file_id"
    app_user ||--o{ document : "reviewer_id"
    app_user ||--o{ document : "uploaded_by_id"
    stored_file ||--o{ attachment : "file_id"
    transportation_request ||--o{ compliance_checklist_item : "request_id"
    compliance_requirement ||--o{ compliance_checklist_item : "requirement_id"
    horse ||--o{ compliance_checklist_item : "horse_id"
    document ||--o{ compliance_checklist_item : "document_id"
    app_user ||--o{ compliance_checklist_item : "checked_by_id"
    route ||--o{ route : "alternative_of_id"
    route ||--o{ route_stop : "route_id"
    transportation_request ||--o{ transportation_trip : "request_id"
    route ||--o{ transportation_trip : "route_id"
    vehicle ||--o{ transportation_trip : "vehicle_id"
    app_user ||--o{ transportation_trip : "coordinator_id"
    route_stop ||--o{ transportation_trip : "current_stop_id"
    transportation_trip ||--o{ trip_staff_assignment : "trip_id"
    app_user ||--o{ trip_staff_assignment : "staff_id"
    app_user ||--o{ trip_staff_assignment : "assigned_by_id"
    transportation_trip ||--o{ trip_status_history : "trip_id"
    app_user ||--o{ trip_status_history : "changed_by_id"
    transportation_trip ||--o{ tracking_event : "trip_id"
    route_stop ||--o{ tracking_event : "route_stop_id"
    app_user ||--o{ tracking_event : "recorded_by_id"
    transportation_trip ||--o{ pre_transport_health_check : "trip_id"
    horse ||--o{ pre_transport_health_check : "horse_id"
    app_user ||--o{ pre_transport_health_check : "checked_by_id"
    transportation_trip ||--o{ health_observation : "trip_id"
    horse ||--o{ health_observation : "horse_id"
    app_user ||--o{ health_observation : "author_id"
    incident ||--o{ health_observation : "incident_id"
    transportation_trip ||--o{ arrival_condition_check : "trip_id"
    horse ||--o{ arrival_condition_check : "horse_id"
    app_user ||--o{ arrival_condition_check : "checked_by_id"
    incident ||--o{ veterinary_consultation : "incident_id"
    transportation_trip ||--o{ veterinary_consultation : "trip_id"
    horse ||--o{ veterinary_consultation : "horse_id"
    stored_file ||--o{ veterinary_consultation : "report_file_id"
    app_user ||--o{ veterinary_consultation : "recorded_by_id"
    transportation_trip ||--o{ incident : "trip_id"
    horse ||--o{ incident : "horse_id"
    app_user ||--o{ incident : "reported_by_id"
    app_user ||--o{ incident : "decided_by_id"
    incident ||--o{ incident_action : "incident_id"
    app_user ||--o{ incident_action : "performed_by_id"
    veterinary_consultation ||--o{ incident_action : "consultation_id"
    route ||--o{ incident_action : "new_route_id"
    transportation_trip ||--o{ handover : "trip_id"
    horse ||--o{ handover : "horse_id"
    arrival_condition_check ||--o{ handover : "arrival_check_id"
    app_user ||--o{ handover : "created_by_id"
    app_user ||--o{ handover : "confirmed_by_id"
    transportation_request ||--o{ quote : "request_id"
    app_user ||--o{ quote : "created_by_id"
    quote ||--o{ quote_item : "quote_id"
    quote ||--o| invoice : "quote_id"
    transportation_request ||--o{ invoice : "request_id"
    app_user ||--o{ invoice : "customer_id"
    invoice ||--o{ payment : "invoice_id"
    app_user ||--o{ payment : "paid_by_id"
    transportation_request ||--o{ claim : "request_id"
    transportation_trip ||--o{ claim : "trip_id"
    incident ||--o{ claim : "incident_id"
    horse ||--o{ claim : "horse_id"
    app_user ||--o{ claim : "customer_id"
    app_user ||--o{ claim : "reviewed_by_id"
    claim ||--o{ claim_evidence : "claim_id"
    stored_file ||--o{ claim_evidence : "file_id"
    app_user ||--o{ claim_evidence : "uploaded_by_id"
    claim ||--o{ claim_assessment : "claim_id"
    app_user ||--o{ claim_assessment : "assessed_by_id"
    claim ||--o{ claim_settlement : "claim_id"
    app_user ||--o{ claim_settlement : "settled_by_id"
    app_user {
        bigint id PK
        nvarchar email UK
        nvarchar password_hash
        nvarchar full_name
        nvarchar phone
        nvarchar role
        bit enabled
        datetime2 last_login_at
        datetime2 created_at
        datetime2 updated_at
    }
    customer_profile {
        bigint id PK
        bigint user_id FK
        nvarchar company_name
        nvarchar customer_type
        nvarchar address
        nvarchar country
        nvarchar tax_code
        nvarchar preferred_language
        datetime2 created_at
        datetime2 updated_at
    }
    employee_profile {
        bigint id PK
        bigint user_id FK
        nvarchar employee_code UK
        nvarchar department
        nvarchar staff_role
        nvarchar license_number
        date license_expiry
        bit air_escort_certified
        nvarchar emergency_contact
        nvarchar base_location
        datetime2 created_at
        datetime2 updated_at
    }
    code_sequence {
        bigint id PK
        nvarchar seq_key UK
        bigint next_value
        bigint version
    }
    notification {
        bigint id PK
        bigint recipient_id FK
        nvarchar title
        nvarchar message
        nvarchar type
        nvarchar reference_type
        bigint reference_id
        datetime2 read_at
        datetime2 created_at
    }
    horse {
        bigint id PK
        bigint owner_id FK
        nvarchar registered_name
        nvarchar breed
        date date_of_birth
        nvarchar sex
        nvarchar color
        int height_cm
        nvarchar microchip_number UK
        nvarchar passport_number UK
        nvarchar passport_country
        nvarchar special_requirements
        nvarchar notes
        nvarchar status
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    stall_type {
        bigint id PK
        nvarchar code UK
        nvarchar name
        int width_cm
        nvarchar description
        decimal surcharge
        nvarchar currency
        bit active
        int sort_order
    }
    transportation_request {
        bigint id PK
        nvarchar request_number UK
        bigint customer_id FK
        nvarchar origin_country
        nvarchar origin_address
        nvarchar destination_country
        nvarchar destination_address
        date requested_departure_date
        date return_date
        nvarchar movement_type
        nvarchar purpose
        nvarchar transport_mode
        decimal declared_value_total
        nvarchar currency
        nvarchar special_requirements
        nvarchar notes
        nvarchar status
        nvarchar last_decision_note
        datetime2 submitted_at
        datetime2 approved_at
        datetime2 completed_at
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    request_horse {
        bigint id PK
        bigint request_id FK
        bigint horse_id FK
        bigint stall_type_id FK
        decimal declared_value
        nvarchar notes
    }
    request_status_history {
        bigint id PK
        bigint request_id FK
        nvarchar from_status
        nvarchar to_status
        bigint changed_by_id FK
        nvarchar note
        datetime2 changed_at
    }
    stored_file {
        bigint id PK
        nvarchar original_filename
        nvarchar stored_filename UK
        nvarchar content_type
        bigint size_bytes
        nvarchar sha256
        bigint uploaded_by_id FK
        datetime2 created_at
    }
    document {
        bigint id PK
        nvarchar document_type
        bigint request_id FK
        bigint horse_id FK
        bigint file_id FK
        nvarchar title
        nvarchar document_number
        nvarchar issuing_country
        date issue_date
        date expiry_date
        nvarchar review_status
        bigint reviewer_id FK
        datetime2 reviewed_at
        nvarchar review_note
        bigint uploaded_by_id FK
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    attachment {
        bigint id PK
        bigint file_id FK
        nvarchar ref_type
        bigint ref_id
        nvarchar caption
        datetime2 created_at
    }
    compliance_requirement {
        bigint id PK
        nvarchar code UK
        nvarchar origin_country
        nvarchar destination_country
        nvarchar movement_type
        nvarchar purpose
        nvarchar category
        nvarchar document_type
        nvarchar title
        nvarchar description
        int validity_days
        bit per_horse
        bit mandatory
        date effective_from
        date effective_to
        bit active
        datetime2 created_at
        datetime2 updated_at
    }
    compliance_checklist_item {
        bigint id PK
        bigint request_id FK
        bigint requirement_id FK
        bigint horse_id FK
        nvarchar status
        bigint document_id FK
        nvarchar reviewer_note
        bigint checked_by_id FK
        datetime2 checked_at
        date due_date
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    vehicle {
        bigint id PK
        nvarchar vehicle_code UK
        nvarchar plate_number UK
        nvarchar vehicle_type
        nvarchar model
        int capacity_horses
        nvarchar status
        nvarchar current_location
        bit climate_controlled
        bit camera_monitoring
        date last_maintenance_date
        date next_maintenance_date
        nvarchar notes
        bit archived
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    route {
        bigint id PK
        nvarchar route_code UK
        nvarchar name
        nvarchar origin_country
        nvarchar origin_location
        nvarchar destination_country
        nvarchar destination_location
        nvarchar transport_mode
        decimal estimated_distance_km
        decimal estimated_duration_hours
        nvarchar status
        bigint alternative_of_id FK
        nvarchar notes
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    route_stop {
        bigint id PK
        bigint route_id FK
        int stop_order
        nvarchar stop_type
        nvarchar name
        nvarchar address
        nvarchar country
        decimal latitude
        decimal longitude
        int planned_arrival_offset_min
        int planned_duration_min
        nvarchar instructions
    }
    transportation_trip {
        bigint id PK
        nvarchar trip_number UK
        bigint request_id FK
        bigint route_id FK
        bigint vehicle_id FK
        bigint coordinator_id FK
        datetime2 planned_departure
        datetime2 planned_arrival
        datetime2 actual_departure
        datetime2 actual_arrival
        nvarchar status
        nvarchar current_location
        bigint current_stop_id FK
        bit operation_blocked
        nvarchar transport_instructions
        nvarchar notes
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    trip_staff_assignment {
        bigint id PK
        bigint trip_id FK
        bigint staff_id FK
        nvarchar assignment_role
        bigint assigned_by_id FK
        datetime2 assigned_at
    }
    trip_status_history {
        bigint id PK
        bigint trip_id FK
        nvarchar previous_status
        nvarchar new_status
        bigint changed_by_id FK
        datetime2 changed_at
        nvarchar note
    }
    tracking_event {
        bigint id PK
        bigint trip_id FK
        bigint route_stop_id FK
        nvarchar event_type
        datetime2 event_time
        nvarchar location
        decimal latitude
        decimal longitude
        nvarchar trip_status
        int delay_minutes
        nvarchar note
        bigint recorded_by_id FK
        datetime2 created_at
    }
    pre_transport_health_check {
        bigint id PK
        bigint trip_id FK
        bigint horse_id FK
        bigint checked_by_id FK
        datetime2 checked_at
        decimal temperature_c
        nvarchar general_condition
        bit identity_verified
        bit documents_present
        bit loading_ok
        bit fit_for_travel_observed
        nvarchar notes
        datetime2 created_at
    }
    health_observation {
        bigint id PK
        bigint trip_id FK
        bigint horse_id FK
        bigint author_id FK
        datetime2 observed_at
        nvarchar location
        nvarchar appetite
        nvarchar water_intake
        nvarchar behavior
        nvarchar stress_level
        nvarchar movement
        bit visible_injury
        nvarchar injury_description
        decimal temperature_c
        nvarchar severity
        nvarchar notes
        bigint incident_id FK
        datetime2 created_at
    }
    arrival_condition_check {
        bigint id PK
        bigint trip_id FK
        bigint horse_id FK
        bigint checked_by_id FK
        datetime2 checked_at
        nvarchar condition
        bit visible_injury
        nvarchar notes
        datetime2 created_at
    }
    veterinary_consultation {
        bigint id PK
        bigint incident_id FK
        bigint trip_id FK
        bigint horse_id FK
        nvarchar veterinarian_name
        nvarchar clinic
        nvarchar contact
        datetime2 consulted_at
        nvarchar advice
        nvarchar recommended_action
        bigint report_file_id FK
        bigint recorded_by_id FK
        datetime2 created_at
    }
    incident {
        bigint id PK
        nvarchar incident_number UK
        bigint trip_id FK
        bigint horse_id FK
        bigint reported_by_id FK
        nvarchar incident_type
        nvarchar severity
        nvarchar title
        nvarchar description
        nvarchar location
        nvarchar status
        datetime2 reported_at
        nvarchar decision
        bigint decided_by_id FK
        datetime2 decided_at
        datetime2 resolved_at
        nvarchar resolution_note
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    incident_action {
        bigint id PK
        bigint incident_id FK
        nvarchar action_type
        nvarchar decision
        nvarchar note
        bigint performed_by_id FK
        datetime2 performed_at
        bigint consultation_id FK
        bigint new_route_id FK
        decimal extra_cost
        nvarchar currency
        datetime2 created_at
    }
    handover {
        bigint id PK
        nvarchar handover_number UK
        bigint trip_id FK
        bigint horse_id FK
        bigint arrival_check_id FK
        nvarchar destination
        datetime2 handover_time
        nvarchar receiver_name
        nvarchar receiver_contact
        nvarchar condition
        bit documents_handed_over
        nvarchar notes
        nvarchar status
        bigint created_by_id FK
        bigint confirmed_by_id FK
        datetime2 confirmed_at
        nvarchar customer_comment
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    pricing_rate {
        bigint id PK
        nvarchar code UK
        nvarchar category
        nvarchar description
        nvarchar unit
        decimal unit_price
        nvarchar currency
        nvarchar transport_mode
        nvarchar destination_country
        bit active
        datetime2 created_at
        datetime2 updated_at
    }
    quote {
        bigint id PK
        nvarchar quote_number UK
        bigint request_id FK
        int version_no
        nvarchar quote_type
        nvarchar status
        nvarchar currency
        decimal subtotal
        decimal discount_total
        decimal total_amount
        decimal estimated_total
        date valid_until
        nvarchar notes
        bigint created_by_id FK
        datetime2 sent_at
        datetime2 accepted_at
        datetime2 rejected_at
        nvarchar customer_response_note
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    quote_item {
        bigint id PK
        bigint quote_id FK
        nvarchar category
        nvarchar description
        decimal quantity
        decimal unit_price
        decimal amount
        decimal estimated_amount
        bit confirmed
        nvarchar source
        int sort_order
    }
    invoice {
        bigint id PK
        nvarchar invoice_number UK
        bigint quote_id FK
        bigint request_id FK
        bigint customer_id FK
        date issue_date
        date due_date
        nvarchar currency
        decimal total_amount
        decimal paid_amount
        decimal deposit_percent
        nvarchar status
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    payment {
        bigint id PK
        nvarchar payment_reference UK
        bigint invoice_id FK
        decimal amount
        nvarchar currency
        nvarchar method
        nvarchar status
        nvarchar card_last4
        datetime2 paid_at
        nvarchar note
        bigint paid_by_id FK
        datetime2 created_at
    }
    claim {
        bigint id PK
        nvarchar claim_number UK
        bigint request_id FK
        bigint trip_id FK
        bigint incident_id FK
        bigint horse_id FK
        bigint customer_id FK
        nvarchar claim_type
        decimal claimed_amount
        decimal approved_amount
        nvarchar currency
        nvarchar description
        nvarchar status
        datetime2 submitted_at
        bigint reviewed_by_id FK
        datetime2 decided_at
        nvarchar decision_note
        datetime2 closed_at
        bigint version
        datetime2 created_at
        datetime2 updated_at
    }
    claim_evidence {
        bigint id PK
        bigint claim_id FK
        bigint file_id FK
        nvarchar evidence_type
        nvarchar notes
        bigint uploaded_by_id FK
        datetime2 uploaded_at
    }
    claim_assessment {
        bigint id PK
        bigint claim_id FK
        nvarchar assessment_note
        nvarchar recommendation
        decimal recommended_amount
        bigint assessed_by_id FK
        datetime2 assessed_at
    }
    claim_settlement {
        bigint id PK
        bigint claim_id FK
        nvarchar settlement_reference UK
        decimal amount
        nvarchar currency
        nvarchar status
        datetime2 settled_at
        nvarchar note
        bigint settled_by_id FK
    }
```

## 4. Enum

| Enum | Module | Giá trị |
|---|---|---|
| `Role` | user | `CUSTOMER`, `LOGISTICS_MANAGER`, `TRANSPORT_SPECIALIST`, `FLEET_ROUTE_COORDINATOR`, `DRIVER_ESCORT` |
| `CustomerType` | user | `INDIVIDUAL`, `STABLE`, `COMPANY`, `AGENT` |
| `StaffRole` | user | `DRIVER`, `ESCORT`, `TEAM_LEADER` |
| `HorseSex` | horse | `STALLION`, `MARE`, `GELDING`, `COLT`, `FILLY` |
| `HorseStatus` | horse | `ACTIVE`, `IN_TRANSIT`, `ARCHIVED` |
| `MovementType` | transportationrequest | `TEMPORARY`, `PERMANENT` |
| `TransportPurpose` | transportationrequest | `RACING`, `COMPETITION`, `TRAINING`, `BREEDING`, `RELOCATION`, `OTHER` |
| `TransportMode` | transportationrequest | `ROAD`, `AIR`, `ROAD_AIR` |
| `RequestStatus` | transportationrequest | `DRAFT`, `SUBMITTED`, `UNDER_DOCUMENT_REVIEW`, `INFORMATION_REQUIRED`, `COMPLIANCE_REVIEW`, `PENDING_MANAGER_APPROVAL`, `APPROVED`, `REJECTED`, `QUOTE_SENT`, `QUOTE_ACCEPTED`, `QUOTE_REJECTED`, `CANCELLED`, `READY_FOR_PLANNING`, `TRIP_PLANNED`, `COMPLETED` |
| `DocumentType` | document | `HORSE_PASSPORT`, `MICROCHIP_CERTIFICATE`, `VACCINATION_RECORD`, `HEALTH_CERTIFICATE`, `LAB_TEST_REPORT`, `IMPORT_PERMIT`, `EXPORT_PERMIT`, `QUARANTINE_CERTIFICATE`, `CUSTOMS_DECLARATION`, `OWNERSHIP_PROOF`, `INSURANCE_CERTIFICATE`, `VET_REPORT`, `OTHER` |
| `DocumentReviewStatus` | document | `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `NEEDS_UPDATE`, `EXPIRED` |
| `ComplianceCategory` | compliance | `IDENTIFICATION`, `VACCINATION`, `HEALTH`, `LABORATORY`, `IMPORT_EXPORT`, `QUARANTINE`, `CUSTOMS`, `INSURANCE` |
| `ChecklistItemStatus` | compliance | `MISSING`, `SUBMITTED`, `APPROVED`, `REJECTED`, `NEEDS_UPDATE`, `EXPIRED`, `WAIVED` |
| `VehicleType` | fleet | `HORSE_TRUCK`, `HORSE_TRAILER`, `AIR_STALL`, `SUPPORT_VEHICLE` |
| `VehicleStatus` | fleet | `AVAILABLE`, `IN_USE`, `MAINTENANCE`, `UNAVAILABLE`, `RETIRED` |
| `RouteStatus` | route | `DRAFT`, `ACTIVE`, `INACTIVE` |
| `StopType` | route | `PICKUP`, `REST_STOP`, `CHECKPOINT`, `AIRPORT`, `BORDER`, `CUSTOMS`, `QUARANTINE`, `STABLE`, `DESTINATION` |
| `TripStatus` | trip | `SCHEDULED`, `PREPARING`, `READY_FOR_DEPARTURE`, `IN_TRANSIT`, `AT_CHECKPOINT`, `AT_BORDER`, `CUSTOMS_CLEARANCE`, `QUARANTINE`, `DELAYED`, `ARRIVED`, `HANDOVER_PENDING`, `DELIVERED`, `CANCELLED` |
| `TrackingEventType` | tracking | `DEPARTED`, `CHECKPOINT_ARRIVED`, `CHECKPOINT_DEPARTED`, `LOCATION_UPDATE`, `STATUS_CHANGE`, `DELAY`, `ARRIVED`, `NOTE` |
| `IntakeLevel` | health | `NORMAL`, `REDUCED`, `POOR`, `NONE` |
| `HorseBehavior` | health | `CALM`, `ALERT`, `RESTLESS`, `AGITATED`, `LETHARGIC` |
| `StressLevel` | health | `LOW`, `MEDIUM`, `HIGH` |
| `MovementCondition` | health | `NORMAL`, `STIFF`, `LAME`, `REFUSES_TO_MOVE` |
| `ObservationSeverity` | health | `NORMAL`, `ATTENTION`, `URGENT` |
| `HorseCondition` | health | `GOOD`, `FAIR`, `POOR`, `INJURED` |
| `OperationalDecision` | incident | `CONTINUE`, `MONITOR`, `STOP`, `REROUTE` |
| `IncidentType` | incident | `HORSE_HEALTH`, `HORSE_INJURY`, `VEHICLE_BREAKDOWN`, `TRAFFIC_DELAY`, `BORDER_DELAY`, `DOCUMENT_ISSUE`, `WEATHER`, `SECURITY`, `OTHER` |
| `IncidentSeverity` | incident | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `IncidentStatus` | incident | `OPEN`, `UNDER_REVIEW`, `ACTION_REQUIRED`, `MONITORING`, `RESOLVED`, `CLOSED` |
| `IncidentActionType` | incident | `NOTE`, `VET_CONTACTED`, `VET_ADVICE_RECORDED`, `DECISION`, `STATUS_CHANGE`, `ESCALATION`, `FIELD_ACTION` |
| `HandoverStatus` | handover | `PENDING_CONFIRMATION`, `CONFIRMED`, `DISPUTED` |
| `QuoteType` | quotation | `ESTIMATE`, `FINAL` |
| `QuoteStatus` | quotation | `ESTIMATED`, `DRAFT`, `SENT`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `SUPERSEDED` |
| `CostCategory` | quotation | `GROUND_TRANSPORT`, `AIR_TRANSPORT`, `HANDLING`, `VETERINARY_TESTING`, `DOCUMENTS`, `CUSTOMS`, `QUARANTINE_STABLING`, `ESCORT`, `INSURANCE`, `SURCHARGE`, `DISCOUNT` |
| `PricingUnit` | quotation | `PER_KM`, `PER_HORSE`, `PER_TRIP`, `PER_HORSE_DAY`, `PERCENT_OF_DECLARED_VALUE` |
| `InvoiceStatus` | payment | `ISSUED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED` |
| `PaymentMethod` | payment | `SIMULATED_CARD`, `SIMULATED_BANK_TRANSFER`, `SIMULATED_EWALLET` |
| `PaymentStatus` | payment | `SUCCEEDED`, `FAILED` |
| `ClaimType` | claim | `INJURY_MEDICAL_EXPENSE`, `MORTALITY`, `HUMANE_EUTHANASIA`, `THEFT_OR_LOSS`, `PROPERTY_DAMAGE`, `SERVICE_DELAY`, `OTHER_DIRECT_LOSS` |
| `ClaimStatus` | claim | `SUBMITTED`, `UNDER_REVIEW`, `EVIDENCE_REQUIRED`, `APPROVED`, `PARTIALLY_APPROVED`, `REJECTED`, `PAID`, `CLOSED` |
| `EvidenceType` | claim | `PHOTO`, `VET_REPORT`, `RECEIPT`, `OFFICIAL_REPORT`, `OTHER` |
| `ClaimRecommendation` | claim | `APPROVE`, `PARTIALLY_APPROVE`, `REJECT`, `REQUEST_EVIDENCE` |
| `SettlementStatus` | claim | `PAID`, `FAILED` |
| `NotificationType` | notification | `INFO`, `SUCCESS`, `WARNING`, `ACTION_REQUIRED`, `CRITICAL` |
| `AttachmentRefType` | document | `HEALTH_OBSERVATION`, `INCIDENT`, `HANDOVER`, `ARRIVAL_CHECK`, `VET_CONSULTATION`, `TRACKING_EVENT` |

## 5. Chi tiết bảng

### Nhóm: Tài khoản

#### `app_user` → entity `AppUser` (module `user`)

Tài khoản đăng nhập. Một user có đúng một role.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `email` | NVARCHAR(150) | NOT NULL | UNIQUE |
| `password_hash` | NVARCHAR(100) | NOT NULL |  |
| `full_name` | NVARCHAR(150) | NOT NULL |  |
| `phone` | NVARCHAR(30) | NULL |  |
| `role` | NVARCHAR(40) | NOT NULL | enum `Role` |
| `enabled` | BIT | NOT NULL | default `1` |
| `last_login_at` | DATETIME2(3) | NULL |  |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `customer_profile` → entity `CustomerProfile` (module `user`)

Thông tin riêng của khách hàng (1:1 với app_user role CUSTOMER).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `user_id` | BIGINT | NOT NULL | FK → `app_user.id`; UNIQUE |
| `company_name` | NVARCHAR(150) | NULL |  |
| `customer_type` | NVARCHAR(40) | NOT NULL | enum `CustomerType` |
| `address` | NVARCHAR(255) | NULL |  |
| `country` | NVARCHAR(2) | NULL |  |
| `tax_code` | NVARCHAR(30) | NULL |  |
| `preferred_language` | NVARCHAR(5) | NOT NULL | default `'vi'` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `employee_profile` → entity `EmployeeProfile` (module `user`)

Thông tin nhân viên (4 role nội bộ).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `user_id` | BIGINT | NOT NULL | FK → `app_user.id`; UNIQUE |
| `employee_code` | NVARCHAR(20) | NOT NULL | UNIQUE |
| `department` | NVARCHAR(100) | NULL |  |
| `staff_role` | NVARCHAR(40) | NULL | enum `StaffRole`; Chỉ dùng cho DRIVER_ESCORT |
| `license_number` | NVARCHAR(50) | NULL |  |
| `license_expiry` | DATE | NULL |  |
| `air_escort_certified` | BIT | NOT NULL | default `0` |
| `emergency_contact` | NVARCHAR(150) | NULL |  |
| `base_location` | NVARCHAR(150) | NULL |  |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

### Nhóm: Hệ thống

#### `code_sequence` → entity `CodeSequence` (module `common`)

Bộ đếm sinh mã nghiệp vụ (REQ-2026-0159…) có khóa bi quan.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `seq_key` | NVARCHAR(30) | NOT NULL | UNIQUE; VD: REQ-2026 |
| `next_value` | BIGINT | NOT NULL |  |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |

#### `notification` → entity `Notification` (module `notification`)

Thông báo trong ứng dụng.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `recipient_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `title` | NVARCHAR(200) | NOT NULL |  |
| `message` | NVARCHAR(1000) | NOT NULL |  |
| `type` | NVARCHAR(40) | NOT NULL | enum `NotificationType` |
| `reference_type` | NVARCHAR(40) | NULL |  |
| `reference_id` | BIGINT | NULL |  |
| `read_at` | DATETIME2(3) | NULL |  |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

### Nhóm: Ngựa & yêu cầu

#### `horse` → entity `Horse` (module `horse`)

Hồ sơ ngựa. Lưu trữ (ARCHIVED) thay vì xóa cứng khi đã có lịch sử.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `owner_id` | BIGINT | NOT NULL | FK → `app_user.id`; Khách hàng sở hữu |
| `registered_name` | NVARCHAR(150) | NOT NULL |  |
| `breed` | NVARCHAR(100) | NULL |  |
| `date_of_birth` | DATE | NULL |  |
| `sex` | NVARCHAR(40) | NULL | enum `HorseSex` |
| `color` | NVARCHAR(60) | NULL |  |
| `height_cm` | INT | NULL |  |
| `microchip_number` | NVARCHAR(30) | NOT NULL | UNIQUE |
| `passport_number` | NVARCHAR(50) | NOT NULL | UNIQUE |
| `passport_country` | NVARCHAR(2) | NULL |  |
| `special_requirements` | NVARCHAR(1000) | NULL |  |
| `notes` | NVARCHAR(1000) | NULL |  |
| `status` | NVARCHAR(40) | NOT NULL | enum `HorseStatus` |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `stall_type` → entity `StallType` (module `horse`)

Loại chuồng/ngăn vận chuyển khách chọn cho từng ngựa (theo Figma: tiêu chuẩn 75cm, rộng 105cm, hộp 150cm).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `code` | NVARCHAR(30) | NOT NULL | UNIQUE |
| `name` | NVARCHAR(100) | NOT NULL |  |
| `width_cm` | INT | NOT NULL |  |
| `description` | NVARCHAR(500) | NULL |  |
| `surcharge` | DECIMAL(18,2) | NOT NULL |  |
| `currency` | NVARCHAR(3) | NOT NULL | default `'USD'` |
| `active` | BIT | NOT NULL | default `1` |
| `sort_order` | INT | NOT NULL | default `0` |

#### `transportation_request` → entity `TransportationRequest` (module `transportationrequest`)

Yêu cầu vận chuyển của khách hàng.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `request_number` | NVARCHAR(30) | NOT NULL | UNIQUE |
| `customer_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `origin_country` | NVARCHAR(2) | NOT NULL |  |
| `origin_address` | NVARCHAR(255) | NOT NULL |  |
| `destination_country` | NVARCHAR(2) | NOT NULL |  |
| `destination_address` | NVARCHAR(255) | NOT NULL |  |
| `requested_departure_date` | DATE | NOT NULL |  |
| `return_date` | DATE | NULL | Chỉ áp dụng TEMPORARY |
| `movement_type` | NVARCHAR(40) | NOT NULL | enum `MovementType` |
| `purpose` | NVARCHAR(40) | NOT NULL | enum `TransportPurpose` |
| `transport_mode` | NVARCHAR(40) | NOT NULL | enum `TransportMode` |
| `declared_value_total` | DECIMAL(18,2) | NOT NULL | default `0`; Tổng giá trị khai báo – backend tự cộng từ request_horse |
| `currency` | NVARCHAR(3) | NOT NULL | default `'USD'` |
| `special_requirements` | NVARCHAR(1000) | NULL |  |
| `notes` | NVARCHAR(1000) | NULL |  |
| `status` | NVARCHAR(40) | NOT NULL | enum `RequestStatus` |
| `last_decision_note` | NVARCHAR(1000) | NULL |  |
| `submitted_at` | DATETIME2(3) | NULL |  |
| `approved_at` | DATETIME2(3) | NULL |  |
| `completed_at` | DATETIME2(3) | NULL |  |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `request_horse` → entity `RequestHorse` (module `transportationrequest`)

Quan hệ N:N yêu cầu – ngựa, kèm loại chuồng và giá trị khai báo.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `request_id` | BIGINT | NOT NULL | FK → `transportation_request.id` |
| `horse_id` | BIGINT | NOT NULL | FK → `horse.id` |
| `stall_type_id` | BIGINT | NOT NULL | FK → `stall_type.id` |
| `declared_value` | DECIMAL(18,2) | NOT NULL | Giá trị khai báo – chỉ tham chiếu bảo hiểm/hải quan/claim |
| `notes` | NVARCHAR(500) | NULL |  |

Unique tổ hợp: (request_id, horse_id)

#### `request_status_history` → entity `RequestStatusHistory` (module `transportationrequest`)

Lịch sử chuyển trạng thái yêu cầu (bao gồm quyết định duyệt/từ chối/yêu cầu bổ sung).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `request_id` | BIGINT | NOT NULL | FK → `transportation_request.id` |
| `from_status` | NVARCHAR(40) | NULL | enum `RequestStatus` |
| `to_status` | NVARCHAR(40) | NOT NULL | enum `RequestStatus` |
| `changed_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `note` | NVARCHAR(1000) | NULL |  |
| `changed_at` | DATETIME2(3) | NOT NULL |  |

### Nhóm: Tài liệu & tuân thủ

#### `stored_file` → entity `StoredFile` (module `document`)

Metadata tệp lưu trên đĩa (thư mục uploads).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `original_filename` | NVARCHAR(255) | NOT NULL |  |
| `stored_filename` | NVARCHAR(80) | NOT NULL | UNIQUE; UUID + phần mở rộng, không dùng tên người dùng |
| `content_type` | NVARCHAR(100) | NOT NULL |  |
| `size_bytes` | BIGINT | NOT NULL |  |
| `sha256` | NVARCHAR(64) | NOT NULL |  |
| `uploaded_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `document` → entity `Document` (module `document`)

Tài liệu pháp lý / thú y / hải quan gắn với yêu cầu và/hoặc ngựa.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `document_type` | NVARCHAR(40) | NOT NULL | enum `DocumentType` |
| `request_id` | BIGINT | NULL | FK → `transportation_request.id` |
| `horse_id` | BIGINT | NULL | FK → `horse.id` |
| `file_id` | BIGINT | NOT NULL | FK → `stored_file.id` |
| `title` | NVARCHAR(200) | NULL |  |
| `document_number` | NVARCHAR(80) | NULL |  |
| `issuing_country` | NVARCHAR(2) | NULL |  |
| `issue_date` | DATE | NULL |  |
| `expiry_date` | DATE | NULL |  |
| `review_status` | NVARCHAR(40) | NOT NULL | enum `DocumentReviewStatus` |
| `reviewer_id` | BIGINT | NULL | FK → `app_user.id` |
| `reviewed_at` | DATETIME2(3) | NULL |  |
| `review_note` | NVARCHAR(1000) | NULL |  |
| `uploaded_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

CHECK: `request_id IS NOT NULL OR horse_id IS NOT NULL`

#### `attachment` → entity `Attachment` (module `document`)

Ảnh/tệp đính kèm cho quan sát sức khỏe, sự cố, bàn giao, tư vấn thú y.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `file_id` | BIGINT | NOT NULL | FK → `stored_file.id` |
| `ref_type` | NVARCHAR(40) | NOT NULL | enum `AttachmentRefType` |
| `ref_id` | BIGINT | NOT NULL |  |
| `caption` | NVARCHAR(255) | NULL |  |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `compliance_requirement` → entity `ComplianceRequirement` (module `compliance`)

Danh mục yêu cầu tuân thủ theo tuyến/loại di chuyển/mục đích (dữ liệu demo, không phải quy định pháp lý chính thức).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `code` | NVARCHAR(40) | NOT NULL | UNIQUE |
| `origin_country` | NVARCHAR(2) | NULL | NULL = mọi quốc gia |
| `destination_country` | NVARCHAR(2) | NULL | NULL = mọi quốc gia |
| `movement_type` | NVARCHAR(40) | NULL | enum `MovementType`; NULL = mọi loại |
| `purpose` | NVARCHAR(40) | NULL | enum `TransportPurpose`; NULL = mọi mục đích |
| `category` | NVARCHAR(40) | NOT NULL | enum `ComplianceCategory` |
| `document_type` | NVARCHAR(40) | NOT NULL | enum `DocumentType` |
| `title` | NVARCHAR(200) | NOT NULL |  |
| `description` | NVARCHAR(1000) | NULL |  |
| `validity_days` | INT | NULL | Tài liệu phải còn hiệu lực/được cấp trong N ngày trước khởi hành |
| `per_horse` | BIT | NOT NULL | default `0` |
| `mandatory` | BIT | NOT NULL | default `1` |
| `effective_from` | DATE | NOT NULL |  |
| `effective_to` | DATE | NULL |  |
| `active` | BIT | NOT NULL | default `1` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `compliance_checklist_item` → entity `ComplianceChecklistItem` (module `compliance`)

Checklist tuân thủ sinh cho từng yêu cầu.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `request_id` | BIGINT | NOT NULL | FK → `transportation_request.id` |
| `requirement_id` | BIGINT | NOT NULL | FK → `compliance_requirement.id` |
| `horse_id` | BIGINT | NULL | FK → `horse.id`; NULL nếu yêu cầu áp dụng cho cả lô |
| `status` | NVARCHAR(40) | NOT NULL | enum `ChecklistItemStatus` |
| `document_id` | BIGINT | NULL | FK → `document.id`; Tài liệu minh chứng hiện hành |
| `reviewer_note` | NVARCHAR(1000) | NULL |  |
| `checked_by_id` | BIGINT | NULL | FK → `app_user.id` |
| `checked_at` | DATETIME2(3) | NULL |  |
| `due_date` | DATE | NULL |  |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

Unique tổ hợp: (request_id, requirement_id, horse_id)

### Nhóm: Đội xe & tuyến

#### `vehicle` → entity `Vehicle` (module `fleet`)

Phương tiện vận chuyển.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `vehicle_code` | NVARCHAR(20) | NOT NULL | UNIQUE |
| `plate_number` | NVARCHAR(30) | NOT NULL | UNIQUE |
| `vehicle_type` | NVARCHAR(40) | NOT NULL | enum `VehicleType` |
| `model` | NVARCHAR(100) | NULL |  |
| `capacity_horses` | INT | NOT NULL |  |
| `status` | NVARCHAR(40) | NOT NULL | enum `VehicleStatus` |
| `current_location` | NVARCHAR(150) | NULL |  |
| `climate_controlled` | BIT | NOT NULL | default `0` |
| `camera_monitoring` | BIT | NOT NULL | default `0` |
| `last_maintenance_date` | DATE | NULL |  |
| `next_maintenance_date` | DATE | NULL |  |
| `notes` | NVARCHAR(1000) | NULL |  |
| `archived` | BIT | NOT NULL | default `0` |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `route` → entity `Route` (module `route`)

Tuyến đường mẫu, dùng lại cho nhiều chuyến.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `route_code` | NVARCHAR(20) | NOT NULL | UNIQUE |
| `name` | NVARCHAR(150) | NOT NULL |  |
| `origin_country` | NVARCHAR(2) | NOT NULL |  |
| `origin_location` | NVARCHAR(150) | NOT NULL |  |
| `destination_country` | NVARCHAR(2) | NOT NULL |  |
| `destination_location` | NVARCHAR(150) | NOT NULL |  |
| `transport_mode` | NVARCHAR(40) | NOT NULL | enum `TransportMode` |
| `estimated_distance_km` | DECIMAL(10,1) | NULL |  |
| `estimated_duration_hours` | DECIMAL(8,1) | NULL |  |
| `status` | NVARCHAR(40) | NOT NULL | enum `RouteStatus` |
| `alternative_of_id` | BIGINT | NULL | FK → `route.id`; Tuyến thay thế cho tuyến gốc |
| `notes` | NVARCHAR(1000) | NULL |  |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `route_stop` → entity `RouteStop` (module `route`)

Điểm dừng theo thứ tự trên tuyến.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `route_id` | BIGINT | NOT NULL | FK → `route.id` |
| `stop_order` | INT | NOT NULL |  |
| `stop_type` | NVARCHAR(40) | NOT NULL | enum `StopType` |
| `name` | NVARCHAR(150) | NOT NULL |  |
| `address` | NVARCHAR(255) | NULL |  |
| `country` | NVARCHAR(2) | NULL |  |
| `latitude` | DECIMAL(9,6) | NULL |  |
| `longitude` | DECIMAL(9,6) | NULL |  |
| `planned_arrival_offset_min` | INT | NULL | Phút kể từ giờ khởi hành dự kiến |
| `planned_duration_min` | INT | NULL |  |
| `instructions` | NVARCHAR(1000) | NULL |  |

Unique tổ hợp: (route_id, stop_order)

### Nhóm: Chuyến đi

#### `transportation_trip` → entity `TransportationTrip` (module `trip`)

Chuyến vận chuyển. Một yêu cầu có thể có nhiều chuyến (nhiều chặng).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `trip_number` | NVARCHAR(30) | NOT NULL | UNIQUE |
| `request_id` | BIGINT | NOT NULL | FK → `transportation_request.id` |
| `route_id` | BIGINT | NOT NULL | FK → `route.id` |
| `vehicle_id` | BIGINT | NULL | FK → `vehicle.id` |
| `coordinator_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `planned_departure` | DATETIME2(3) | NOT NULL |  |
| `planned_arrival` | DATETIME2(3) | NOT NULL |  |
| `actual_departure` | DATETIME2(3) | NULL |  |
| `actual_arrival` | DATETIME2(3) | NULL |  |
| `status` | NVARCHAR(40) | NOT NULL | enum `TripStatus` |
| `current_location` | NVARCHAR(150) | NULL |  |
| `current_stop_id` | BIGINT | NULL | FK → `route_stop.id` |
| `operation_blocked` | BIT | NOT NULL | default `0`; Bị chặn do sự cố khẩn cấp chờ quyết định Manager |
| `transport_instructions` | NVARCHAR(2000) | NULL |  |
| `notes` | NVARCHAR(1000) | NULL |  |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `trip_staff_assignment` → entity `TripStaffAssignment` (module `trip`)

Phân công tài xế/áp tải/trưởng nhóm cho chuyến.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `trip_id` | BIGINT | NOT NULL | FK → `transportation_trip.id` |
| `staff_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `assignment_role` | NVARCHAR(40) | NOT NULL | enum `StaffRole` |
| `assigned_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `assigned_at` | DATETIME2(3) | NOT NULL |  |

Unique tổ hợp: (trip_id, staff_id)

#### `trip_status_history` → entity `TripStatusHistory` (module `tracking`)

Lịch sử mọi thay đổi trạng thái chuyến.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `trip_id` | BIGINT | NOT NULL | FK → `transportation_trip.id` |
| `previous_status` | NVARCHAR(40) | NULL | enum `TripStatus` |
| `new_status` | NVARCHAR(40) | NOT NULL | enum `TripStatus` |
| `changed_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `changed_at` | DATETIME2(3) | NOT NULL |  |
| `note` | NVARCHAR(1000) | NULL |  |

#### `tracking_event` → entity `TrackingEvent` (module `tracking`)

Sự kiện theo dõi hành trình (checkpoint, vị trí, trễ).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `trip_id` | BIGINT | NOT NULL | FK → `transportation_trip.id` |
| `route_stop_id` | BIGINT | NULL | FK → `route_stop.id` |
| `event_type` | NVARCHAR(40) | NOT NULL | enum `TrackingEventType` |
| `event_time` | DATETIME2(3) | NOT NULL |  |
| `location` | NVARCHAR(150) | NULL |  |
| `latitude` | DECIMAL(9,6) | NULL |  |
| `longitude` | DECIMAL(9,6) | NULL |  |
| `trip_status` | NVARCHAR(40) | NOT NULL | enum `TripStatus` |
| `delay_minutes` | INT | NULL |  |
| `note` | NVARCHAR(1000) | NULL |  |
| `recorded_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

### Nhóm: Sức khỏe

#### `pre_transport_health_check` → entity `PreTransportHealthCheck` (module `health`)

Kiểm tra (quan sát) trước khi xếp ngựa lên xe.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `trip_id` | BIGINT | NOT NULL | FK → `transportation_trip.id` |
| `horse_id` | BIGINT | NOT NULL | FK → `horse.id` |
| `checked_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `checked_at` | DATETIME2(3) | NOT NULL |  |
| `temperature_c` | DECIMAL(4,1) | NULL |  |
| `general_condition` | NVARCHAR(40) | NOT NULL | enum `HorseCondition` |
| `identity_verified` | BIT | NOT NULL | default `0` |
| `documents_present` | BIT | NOT NULL | default `0` |
| `loading_ok` | BIT | NOT NULL | default `0` |
| `fit_for_travel_observed` | BIT | NOT NULL | default `0`; Quan sát của đội vận hành, không phải chẩn đoán |
| `notes` | NVARCHAR(1000) | NULL |  |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

Unique tổ hợp: (trip_id, horse_id)

#### `health_observation` → entity `HealthObservation` (module `health`)

Quan sát tình trạng ngựa trong hành trình (Driver/Escort).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `trip_id` | BIGINT | NOT NULL | FK → `transportation_trip.id` |
| `horse_id` | BIGINT | NOT NULL | FK → `horse.id` |
| `author_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `observed_at` | DATETIME2(3) | NOT NULL |  |
| `location` | NVARCHAR(150) | NULL |  |
| `appetite` | NVARCHAR(40) | NOT NULL | enum `IntakeLevel` |
| `water_intake` | NVARCHAR(40) | NOT NULL | enum `IntakeLevel` |
| `behavior` | NVARCHAR(40) | NOT NULL | enum `HorseBehavior` |
| `stress_level` | NVARCHAR(40) | NOT NULL | enum `StressLevel` |
| `movement` | NVARCHAR(40) | NOT NULL | enum `MovementCondition` |
| `visible_injury` | BIT | NOT NULL | default `0` |
| `injury_description` | NVARCHAR(500) | NULL |  |
| `temperature_c` | DECIMAL(4,1) | NULL |  |
| `severity` | NVARCHAR(40) | NOT NULL | enum `ObservationSeverity` |
| `notes` | NVARCHAR(1000) | NULL |  |
| `incident_id` | BIGINT | NULL | FK → `incident.id`; Sự cố được tạo từ quan sát (nếu có) |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `arrival_condition_check` → entity `ArrivalConditionCheck` (module `health`)

Kiểm tra tình trạng khi đến nơi, trước bàn giao.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `trip_id` | BIGINT | NOT NULL | FK → `transportation_trip.id` |
| `horse_id` | BIGINT | NOT NULL | FK → `horse.id` |
| `checked_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `checked_at` | DATETIME2(3) | NOT NULL |  |
| `condition` | NVARCHAR(40) | NOT NULL | enum `HorseCondition` |
| `visible_injury` | BIT | NOT NULL | default `0` |
| `notes` | NVARCHAR(1000) | NULL |  |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

Unique tổ hợp: (trip_id, horse_id)

#### `veterinary_consultation` → entity `VeterinaryConsultation` (module `health`)

Bản ghi tư vấn từ bác sĩ thú y bên ngoài (không phải tài khoản hệ thống).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `incident_id` | BIGINT | NULL | FK → `incident.id` |
| `trip_id` | BIGINT | NULL | FK → `transportation_trip.id` |
| `horse_id` | BIGINT | NULL | FK → `horse.id` |
| `veterinarian_name` | NVARCHAR(150) | NOT NULL |  |
| `clinic` | NVARCHAR(150) | NULL |  |
| `contact` | NVARCHAR(150) | NULL |  |
| `consulted_at` | DATETIME2(3) | NOT NULL |  |
| `advice` | NVARCHAR(2000) | NOT NULL |  |
| `recommended_action` | NVARCHAR(40) | NULL | enum `OperationalDecision` |
| `report_file_id` | BIGINT | NULL | FK → `stored_file.id` |
| `recorded_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

### Nhóm: Sự cố & bàn giao

#### `incident` → entity `Incident` (module `incident`)

Sự cố phát sinh trên chuyến. Khác với Claim (bồi thường).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `incident_number` | NVARCHAR(30) | NOT NULL | UNIQUE |
| `trip_id` | BIGINT | NOT NULL | FK → `transportation_trip.id` |
| `horse_id` | BIGINT | NULL | FK → `horse.id` |
| `reported_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `incident_type` | NVARCHAR(40) | NOT NULL | enum `IncidentType` |
| `severity` | NVARCHAR(40) | NOT NULL | enum `IncidentSeverity` |
| `title` | NVARCHAR(200) | NOT NULL |  |
| `description` | NVARCHAR(2000) | NOT NULL |  |
| `location` | NVARCHAR(150) | NULL |  |
| `status` | NVARCHAR(40) | NOT NULL | enum `IncidentStatus` |
| `reported_at` | DATETIME2(3) | NOT NULL |  |
| `decision` | NVARCHAR(40) | NULL | enum `OperationalDecision` |
| `decided_by_id` | BIGINT | NULL | FK → `app_user.id` |
| `decided_at` | DATETIME2(3) | NULL |  |
| `resolved_at` | DATETIME2(3) | NULL |  |
| `resolution_note` | NVARCHAR(2000) | NULL |  |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `incident_action` → entity `IncidentAction` (module `incident`)

Nhật ký hành động xử lý sự cố.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `incident_id` | BIGINT | NOT NULL | FK → `incident.id` |
| `action_type` | NVARCHAR(40) | NOT NULL | enum `IncidentActionType` |
| `decision` | NVARCHAR(40) | NULL | enum `OperationalDecision` |
| `note` | NVARCHAR(2000) | NOT NULL |  |
| `performed_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `performed_at` | DATETIME2(3) | NOT NULL |  |
| `consultation_id` | BIGINT | NULL | FK → `veterinary_consultation.id` |
| `new_route_id` | BIGINT | NULL | FK → `route.id` |
| `extra_cost` | DECIMAL(18,2) | NULL |  |
| `currency` | NVARCHAR(3) | NULL |  |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `handover` → entity `Handover` (module `handover`)

Biên bản bàn giao từng ngựa tại điểm đến; khách hàng xác nhận.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `handover_number` | NVARCHAR(30) | NOT NULL | UNIQUE |
| `trip_id` | BIGINT | NOT NULL | FK → `transportation_trip.id` |
| `horse_id` | BIGINT | NOT NULL | FK → `horse.id` |
| `arrival_check_id` | BIGINT | NULL | FK → `arrival_condition_check.id` |
| `destination` | NVARCHAR(255) | NOT NULL |  |
| `handover_time` | DATETIME2(3) | NOT NULL |  |
| `receiver_name` | NVARCHAR(150) | NOT NULL |  |
| `receiver_contact` | NVARCHAR(150) | NULL |  |
| `condition` | NVARCHAR(40) | NOT NULL | enum `HorseCondition` |
| `documents_handed_over` | BIT | NOT NULL | default `0` |
| `notes` | NVARCHAR(2000) | NULL |  |
| `status` | NVARCHAR(40) | NOT NULL | enum `HandoverStatus` |
| `created_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `confirmed_by_id` | BIGINT | NULL | FK → `app_user.id` |
| `confirmed_at` | DATETIME2(3) | NULL |  |
| `customer_comment` | NVARCHAR(1000) | NULL |  |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

Unique tổ hợp: (trip_id, horse_id)

### Nhóm: Tài chính

#### `pricing_rate` → entity `PricingRate` (module `quotation`)

Bảng giá demo dùng để tính Estimated Cost.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `code` | NVARCHAR(40) | NOT NULL | UNIQUE |
| `category` | NVARCHAR(40) | NOT NULL | enum `CostCategory` |
| `description` | NVARCHAR(200) | NOT NULL |  |
| `unit` | NVARCHAR(40) | NOT NULL | enum `PricingUnit` |
| `unit_price` | DECIMAL(18,4) | NOT NULL |  |
| `currency` | NVARCHAR(3) | NOT NULL | default `'USD'` |
| `transport_mode` | NVARCHAR(40) | NULL | enum `TransportMode`; NULL = mọi phương thức |
| `destination_country` | NVARCHAR(2) | NULL | NULL = mọi quốc gia |
| `active` | BIT | NOT NULL | default `1` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `quote` → entity `Quote` (module `quotation`)

Chi phí ước tính (ESTIMATE) và các phiên bản báo giá cuối (FINAL).

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `quote_number` | NVARCHAR(30) | NOT NULL | UNIQUE |
| `request_id` | BIGINT | NOT NULL | FK → `transportation_request.id` |
| `version_no` | INT | NOT NULL |  |
| `quote_type` | NVARCHAR(40) | NOT NULL | enum `QuoteType` |
| `status` | NVARCHAR(40) | NOT NULL | enum `QuoteStatus` |
| `currency` | NVARCHAR(3) | NOT NULL | default `'USD'` |
| `subtotal` | DECIMAL(18,2) | NOT NULL | default `0` |
| `discount_total` | DECIMAL(18,2) | NOT NULL | default `0` |
| `total_amount` | DECIMAL(18,2) | NOT NULL | default `0`; Backend tính = subtotal - discount |
| `estimated_total` | DECIMAL(18,2) | NULL | Tổng ước tính tham chiếu khi lập báo giá cuối |
| `valid_until` | DATE | NULL |  |
| `notes` | NVARCHAR(2000) | NULL |  |
| `created_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `sent_at` | DATETIME2(3) | NULL |  |
| `accepted_at` | DATETIME2(3) | NULL |  |
| `rejected_at` | DATETIME2(3) | NULL |  |
| `customer_response_note` | NVARCHAR(1000) | NULL |  |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

Unique tổ hợp: (request_id, version_no)

#### `quote_item` → entity `QuoteItem` (module `quotation`)

Dòng chi phí của báo giá.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `quote_id` | BIGINT | NOT NULL | FK → `quote.id` |
| `category` | NVARCHAR(40) | NOT NULL | enum `CostCategory` |
| `description` | NVARCHAR(255) | NOT NULL |  |
| `quantity` | DECIMAL(12,2) | NOT NULL |  |
| `unit_price` | DECIMAL(18,2) | NOT NULL |  |
| `amount` | DECIMAL(18,2) | NOT NULL | Backend tính; DISCOUNT luôn âm |
| `estimated_amount` | DECIMAL(18,2) | NULL | Số ước tính ban đầu để so sánh |
| `confirmed` | BIT | NOT NULL | default `0`; Manager đã xác nhận chi phí thực |
| `source` | NVARCHAR(150) | NULL |  |
| `sort_order` | INT | NOT NULL | default `0` |

#### `invoice` → entity `Invoice` (module `payment`)

Hóa đơn phát hành khi khách chấp nhận báo giá cuối.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `invoice_number` | NVARCHAR(30) | NOT NULL | UNIQUE |
| `quote_id` | BIGINT | NOT NULL | FK → `quote.id`; UNIQUE |
| `request_id` | BIGINT | NOT NULL | FK → `transportation_request.id` |
| `customer_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `issue_date` | DATE | NOT NULL |  |
| `due_date` | DATE | NOT NULL |  |
| `currency` | NVARCHAR(3) | NOT NULL | default `'USD'` |
| `total_amount` | DECIMAL(18,2) | NOT NULL |  |
| `paid_amount` | DECIMAL(18,2) | NOT NULL | default `0` |
| `deposit_percent` | DECIMAL(5,2) | NOT NULL | default `50` |
| `status` | NVARCHAR(40) | NOT NULL | enum `InvoiceStatus` |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

#### `payment` → entity `Payment` (module `payment`)

Thanh toán MÔ PHỎNG, không xử lý thẻ thật.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `payment_reference` | NVARCHAR(40) | NOT NULL | UNIQUE |
| `invoice_id` | BIGINT | NOT NULL | FK → `invoice.id` |
| `amount` | DECIMAL(18,2) | NOT NULL |  |
| `currency` | NVARCHAR(3) | NOT NULL | default `'USD'` |
| `method` | NVARCHAR(40) | NOT NULL | enum `PaymentMethod` |
| `status` | NVARCHAR(40) | NOT NULL | enum `PaymentStatus` |
| `card_last4` | NVARCHAR(4) | NULL | Chỉ 4 số cuối mô phỏng, không lưu số thẻ |
| `paid_at` | DATETIME2(3) | NULL |  |
| `note` | NVARCHAR(500) | NULL |  |
| `paid_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

### Nhóm: Bồi thường

#### `claim` → entity `Claim` (module `claim`)

Yêu cầu bồi thường tài chính. Không tự động bằng giá trị khai báo.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `claim_number` | NVARCHAR(30) | NOT NULL | UNIQUE |
| `request_id` | BIGINT | NOT NULL | FK → `transportation_request.id` |
| `trip_id` | BIGINT | NULL | FK → `transportation_trip.id` |
| `incident_id` | BIGINT | NULL | FK → `incident.id` |
| `horse_id` | BIGINT | NULL | FK → `horse.id` |
| `customer_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `claim_type` | NVARCHAR(40) | NOT NULL | enum `ClaimType` |
| `claimed_amount` | DECIMAL(18,2) | NOT NULL |  |
| `approved_amount` | DECIMAL(18,2) | NULL |  |
| `currency` | NVARCHAR(3) | NOT NULL | default `'USD'` |
| `description` | NVARCHAR(2000) | NOT NULL |  |
| `status` | NVARCHAR(40) | NOT NULL | enum `ClaimStatus` |
| `submitted_at` | DATETIME2(3) | NOT NULL |  |
| `reviewed_by_id` | BIGINT | NULL | FK → `app_user.id` |
| `decided_at` | DATETIME2(3) | NULL |  |
| `decision_note` | NVARCHAR(2000) | NULL |  |
| `closed_at` | DATETIME2(3) | NULL |  |
| `version` | BIGINT | NOT NULL | optimistic lock (@Version); default `0` |
| `created_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |
| `updated_at` | DATETIME2(3) | NOT NULL | default `SYSDATETIME()` |

CHECK: `approved_amount IS NULL OR approved_amount <= claimed_amount`

#### `claim_evidence` → entity `ClaimEvidence` (module `claim`)

Bằng chứng cho claim.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `claim_id` | BIGINT | NOT NULL | FK → `claim.id` |
| `file_id` | BIGINT | NOT NULL | FK → `stored_file.id` |
| `evidence_type` | NVARCHAR(40) | NOT NULL | enum `EvidenceType` |
| `notes` | NVARCHAR(500) | NULL |  |
| `uploaded_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `uploaded_at` | DATETIME2(3) | NOT NULL |  |

#### `claim_assessment` → entity `ClaimAssessment` (module `claim`)

Nhận định thẩm định claim.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `claim_id` | BIGINT | NOT NULL | FK → `claim.id` |
| `assessment_note` | NVARCHAR(2000) | NOT NULL |  |
| `recommendation` | NVARCHAR(40) | NOT NULL | enum `ClaimRecommendation` |
| `recommended_amount` | DECIMAL(18,2) | NULL |  |
| `assessed_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |
| `assessed_at` | DATETIME2(3) | NOT NULL |  |

#### `claim_settlement` → entity `ClaimSettlement` (module `claim`)

Chi trả bồi thường (mô phỏng). 1:N cho phép chi trả nhiều đợt.

| Cột | Kiểu SQL Server | Null | Ràng buộc / ghi chú |
|---|---|---|---|
| `id` | BIGINT IDENTITY | NOT NULL | PK, IDENTITY |
| `claim_id` | BIGINT | NOT NULL | FK → `claim.id` |
| `settlement_reference` | NVARCHAR(40) | NOT NULL | UNIQUE |
| `amount` | DECIMAL(18,2) | NOT NULL |  |
| `currency` | NVARCHAR(3) | NOT NULL | default `'USD'` |
| `status` | NVARCHAR(40) | NOT NULL | enum `SettlementStatus` |
| `settled_at` | DATETIME2(3) | NOT NULL |  |
| `note` | NVARCHAR(500) | NULL |  |
| `settled_by_id` | BIGINT | NOT NULL | FK → `app_user.id` |

## 6. Dữ liệu tham chiếu (V2)

* `stall_type`: 3 loại chuồng (STANDARD, WIDE, BOX).
* `pricing_rate`: 11 dòng **bảng giá DEMO** – đường bộ theo km, hàng không theo ngựa, bốc xếp, khám/xét nghiệm, giấy tờ, hải quan, cách ly/lưu chuồng (SG/HK/MY), áp tải, bảo hiểm 0,6% giá trị khai báo. Số liệu minh họa, không phải bảng giá thật.
* `compliance_requirement`: 30 yêu cầu tuân thủ **DEMO** theo cặp nước đi/đến và loại di chuyển. Không phản ánh quy định thật của từng quốc gia.

Danh mục quốc gia hiển thị (`GET /api/reference/countries`) nằm trong mã nguồn, không phải bảng.
