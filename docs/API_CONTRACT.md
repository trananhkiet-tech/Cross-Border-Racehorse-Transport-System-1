# API Contract – IRTMS

Tài liệu này **được sinh từ mã nguồn** (các `@RestController`, `@PreAuthorize`, `@Operation`) nên khớp với backend hiện tại: 134 endpoint.
Swagger UI (profile `dev`): `http://localhost:8080/swagger-ui.html` – OpenAPI JSON: `/v3/api-docs`.

## Quy ước chung

* **Base URL:** `http://localhost:8080/api` (frontend đọc từ `VITE_API_BASE_URL`).
* **Xác thực:** `Authorization: Bearer <JWT>` (lấy từ `POST /api/auth/login`). Không có token → `401`; sai quyền → `403`.
* **Công khai (không cần token):** `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/reference/**`, Swagger ở profile dev.
* **Phân trang:** query `page` (bắt đầu từ **0**), `size`; trả về `{ content, page, size, totalElements, totalPages }`.
* **Lọc nhiều trạng thái:** lặp tham số, ví dụ `?status=SUBMITTED&status=COMPLIANCE_REVIEW`.
* **Thời gian:** ISO-8601 `LocalDateTime` **không kèm múi giờ**, hiểu theo `APP_TIME_ZONE` (mặc định `Asia/Ho_Chi_Minh`). Frontend hiển thị nguyên giá trị.
* **Tiền:** số thập phân 2 chữ số + mã tiền tệ (`currency`, mặc định USD). Mọi tổng tiền tính ở backend.
* **Tải tệp:** `multipart/form-data`, tối đa 10 MB, chỉ PDF/JPEG/PNG/WEBP (kiểm tra magic bytes).
* **`allowedActions`:** nhiều response chi tiết (yêu cầu, chuyến, sự cố, bàn giao, bồi thường, báo giá) kèm danh sách hành động mà người dùng hiện tại được phép; frontend dùng danh sách này để hiện nút. Backend vẫn kiểm tra lại mọi thao tác.

### Định dạng lỗi

Mọi lỗi trả JSON thống nhất (`ApiError`), **không bao giờ** kèm stack trace; lỗi không lường trước chỉ được ghi log phía server.

```json
{
  "timestamp": "2026-10-09T10:15:30",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu nhập chưa hợp lệ",
  "path": "/api/horses",
  "fieldErrors": [ { "field": "microchipNumber", "message": "Microchip gồm 9–20 ký tự chữ/số" } ]
}
```

| HTTP | `code` | Ý nghĩa |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Bean Validation thất bại; có `fieldErrors` |
| 400 | `BAD_REQUEST` | Sai định dạng JSON/tham số, hoặc dữ liệu nghiệp vụ không hợp lệ |
| 400 | `FILE_TOO_LARGE` | Tệp vượt 10 MB |
| 401 | `UNAUTHORIZED` | Thiếu hoặc hết hạn token |
| 401 | `INVALID_CREDENTIALS`, `ACCOUNT_DISABLED` | Đăng nhập sai / tài khoản bị khóa |
| 403 | `FORBIDDEN` | Không đủ quyền hoặc không sở hữu / không được phân công |
| 404 | `NOT_FOUND` | Không tồn tại (hoặc ngoài phạm vi được xem) |
| 405 | `METHOD_NOT_ALLOWED` | Sai phương thức HTTP |
| 409 | `INVALID_STATE` | Sai trạng thái nghiệp vụ (ví dụ chuyển trạng thái không hợp lệ) |
| 409 | `CONFLICT`, `DATA_CONFLICT` | Trùng dữ liệu / vi phạm ràng buộc (microchip, email, mã…) |
| 409 | `CONCURRENT_UPDATE` | Dữ liệu vừa bị người khác sửa (optimistic locking) |
| 500 | `INTERNAL_ERROR` | Lỗi hệ thống |

### Ghi chú nghiệp vụ liên quan đến API

* Đăng ký công khai **chỉ tạo `CUSTOMER`**; tài khoản nhân viên do Manager tạo qua `POST /api/employees`.
* Thanh toán là **mô phỏng**: thẻ có 4 số cuối `0002` luôn bị từ chối (HTTP 200, `status = FAILED`). Lần thanh toán đầu phải ≥ tiền cọc (50%).
* Giá trị khai báo của ngựa **không** là giá vận chuyển và **không** tự quyết định số tiền bồi thường.
* `Incident` không tự tạo `Claim`; khách hàng tự nộp hồ sơ bồi thường.

## Danh sách endpoint

Cột *Quyền* liệt kê role được gọi. "Đã đăng nhập" nghĩa là mọi role, kèm kiểm tra quyền sở hữu / được phân công ở tầng service. Một số endpoint có điều kiện bổ sung theo trạng thái nghiệp vụ (xem `BUSINESS_WORKFLOWS.md`).

### Xác thực

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `POST` | `/api/auth/register` | Công khai | Đăng ký (chỉ tạo CUSTOMER) |
| `POST` | `/api/auth/login` | Công khai | Đăng nhập, nhận JWT |
| `GET` | `/api/auth/me` | Đã đăng nhập | Thông tin của tôi |

### Người dùng & hồ sơ

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `PUT` | `/api/users/me` | Đã đăng nhập | Cập nhật |
| `POST` | `/api/users/me/password` | Đã đăng nhập | Đổi mật khẩu |
| `GET` | `/api/customers/me` | Customer | Thông tin của tôi |
| `PUT` | `/api/customers/me` | Customer | Cập nhật |
| `GET` | `/api/employees` | Manager, Coordinator | Danh sách |
| `GET` | `/api/employees/{userId}` | Manager, Coordinator | Chi tiết |
| `POST` | `/api/employees` | Manager | Tạo mới |
| `PATCH` | `/api/employees/{userId}/status` | Manager | Khóa / mở tài khoản |

### Tham chiếu & tệp đính kèm

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/api/reference/countries` | Công khai | Danh mục quốc gia |
| `GET` | `/api/reference/stall-types` | Công khai | Danh mục loại chuồng |

### Ngựa

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/api/horses` | Customer, Manager, Specialist, Coordinator | Danh sách |
| `POST` | `/api/horses` | Customer | Tạo mới |
| `GET` | `/api/horses/{id}` | Customer, Manager, Specialist, Coordinator | Chi tiết |
| `PUT` | `/api/horses/{id}` | Customer | Cập nhật |
| `DELETE` | `/api/horses/{id}` | Customer | Lưu trữ hồ sơ ngựa (soft delete, giữ lịch sử) |

### Yêu cầu vận chuyển

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/api/transportation-requests` | Customer, Manager, Specialist, Coordinator | Danh sách yêu cầu (Customer chỉ thấy của mình) |
| `GET` | `/api/transportation-requests/{id}` | Đã đăng nhập | Chi tiết |
| `POST` | `/api/transportation-requests` | Customer | Tạo yêu cầu ở trạng thái DRAFT |
| `PUT` | `/api/transportation-requests/{id}` | Customer | Cập nhật |
| `POST` | `/api/transportation-requests/{id}/submit` | Customer | Gửi yêu cầu: sinh checklist tuân thủ và chi phí ước tính |
| `POST` | `/api/transportation-requests/{id}/cancel` | Customer, Manager | Hủy |
| `POST` | `/api/transportation-requests/{id}/start-review` | Specialist | Bắt đầu rà soát hồ sơ |
| `POST` | `/api/transportation-requests/{id}/complete-document-review` | Specialist | Hoàn tất rà soát tài liệu |
| `POST` | `/api/transportation-requests/{id}/complete-compliance` | Specialist | Xác nhận hồ sơ tuân thủ đầy đủ → chờ Manager phê duyệt |
| `POST` | `/api/transportation-requests/{id}/request-information` | Manager, Specialist | Yêu cầu bổ sung thông tin |
| `POST` | `/api/transportation-requests/{id}/approve` | Manager | Phê duyệt |
| `POST` | `/api/transportation-requests/{id}/reject` | Manager | Từ chối |

### Tài liệu

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `POST` | `/api/attachments` | Đã đăng nhập | Tải tài liệu lên |
| `GET` | `/api/attachments` | Đã đăng nhập | Danh sách |
| `GET` | `/api/attachments/{id}/file` | Đã đăng nhập | Tải tệp |
| `POST` | `/api/documents/upload` | Customer, Specialist | Tải tài liệu (PDF/JPG/PNG/WEBP ≤ 10MB) cho yêu cầu và/hoặc ngựa |
| `GET` | `/api/documents` | Customer, Manager, Specialist, Coordinator | Tìm kiếm tài liệu |
| `GET` | `/api/documents/expiring` | Manager, Specialist | Tài liệu sắp hết hạn |
| `POST` | `/api/documents/mark-expired` | Specialist | Đánh dấu tài liệu hết hạn |
| `GET` | `/api/documents/{id}` | Đã đăng nhập | Chi tiết |
| `GET` | `/api/documents/{id}/file` | Đã đăng nhập | Tải tệp |
| `GET` | `/api/transportation-requests/{requestId}/documents` | Đã đăng nhập | Theo yêu cầu vận chuyển |
| `GET` | `/api/horses/{horseId}/documents` | Đã đăng nhập | Theo ngựa |
| `POST` | `/api/documents/{id}/review` | Specialist | Rà soát / thẩm định |

### Tuân thủ

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/api/compliance/requirements` | Đã đăng nhập | Danh mục yêu cầu tuân thủ DEMO (lọc theo nhóm và nước đến) |
| `POST` | `/api/compliance/requirements` | Specialist | Tạo yêu cầu tuân thủ (danh mục) |
| `PUT` | `/api/compliance/requirements/{id}` | Specialist | Cập nhật yêu cầu tuân thủ |
| `GET` | `/api/compliance/checklists/{requestId}` | Đã đăng nhập | Checklist tuân thủ của yêu cầu |
| `POST` | `/api/compliance/checklists/{requestId}/generate` | Specialist, Manager | Sinh / đồng bộ lại checklist |
| `PATCH` | `/api/compliance/checklists/{itemId}` | Specialist | Cập nhật một mục checklist |
| `POST` | `/api/compliance/checklists/{requestId}/notify-missing` | Specialist | Nhắc khách hàng bổ sung tài liệu thiếu |
| `GET` | `/api/compliance/open-items` | Customer, Specialist, Manager | Tài liệu còn thiếu / cần cập nhật (Missing Documents) |

### Báo giá & bảng giá

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `POST` | `/api/quotes` | Manager | Tạo bản nháp Final Quote (sao chép từ ước tính hoặc phiên bản trước) |
| `GET` | `/api/quotes` | Customer, Manager | Danh sách |
| `GET` | `/api/quotes/{id}` | Đã đăng nhập | Chi tiết |
| `PUT` | `/api/quotes/{id}` | Manager | Sửa dòng chi phí (chỉ bản nháp). Tổng do backend tính |
| `POST` | `/api/quotes/{id}/send` | Manager | Gửi báo giá cho khách |
| `POST` | `/api/quotes/{id}/accept` | Customer | Khách hàng chấp nhận báo giá |
| `POST` | `/api/quotes/{id}/reject` | Customer | Khách hàng từ chối báo giá |
| `GET` | `/api/transportation-requests/{requestId}/quotes` | Đã đăng nhập | Theo yêu cầu vận chuyển |
| `GET` | `/api/pricing-rates` | Manager | Bảng giá (demo) |

### Hóa đơn & thanh toán (mô phỏng)

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `POST` | `/api/invoices` | Manager | Phát hành hóa đơn |
| `GET` | `/api/invoices` | Customer, Manager | Danh sách hóa đơn |
| `GET` | `/api/invoices/{id}` | Customer, Manager | Chi tiết hóa đơn |
| `POST` | `/api/payments/simulate` | Customer | Thanh toán MÔ PHỎNG. Thẻ kết thúc 0002 = bị từ chối |
| `GET` | `/api/payments` | Customer, Manager | Danh sách thanh toán |
| `GET` | `/api/payments/{id}` | Customer, Manager | Chi tiết thanh toán |

### Phương tiện

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/api/vehicles` | Coordinator, Manager | Danh sách |
| `GET` | `/api/vehicles/available` | Coordinator, Manager | Xe sẵn sàng, không trùng lịch, đủ sức chứa |
| `GET` | `/api/vehicles/{id}` | Coordinator, Manager | Chi tiết |
| `POST` | `/api/vehicles` | Coordinator | Tạo mới |
| `PUT` | `/api/vehicles/{id}` | Coordinator | Cập nhật |
| `PATCH` | `/api/vehicles/{id}/status` | Coordinator | Cập nhật trạng thái |
| `DELETE` | `/api/vehicles/{id}` | Coordinator | Lưu trữ |

### Tuyến đường

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/api/routes` | Coordinator, Manager | Danh sách |
| `GET` | `/api/routes/{id}` | Coordinator, Manager | Chi tiết |
| `POST` | `/api/routes` | Coordinator | Tạo mới |
| `PUT` | `/api/routes/{id}` | Coordinator | Cập nhật |
| `POST` | `/api/routes/{id}/stops` | Coordinator | Thêm điểm dừng |
| `PUT` | `/api/routes/{routeId}/stops/{stopId}` | Coordinator | Sửa điểm dừng |
| `DELETE` | `/api/routes/{routeId}/stops/{stopId}` | Coordinator | Xóa điểm dừng |

### Chuyến đi, theo dõi hành trình

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/api/trips` | Đã đăng nhập | Customer: chuyến của mình; Driver/Escort: chuyến được giao; nhân viên khác: tất cả |
| `POST` | `/api/trips` | Coordinator | Tạo mới |
| `GET` | `/api/trips/{id}` | Đã đăng nhập | Chi tiết |
| `PUT` | `/api/trips/{id}` | Coordinator | Sửa kế hoạch chuyến |
| `POST` | `/api/trips/{id}/assign-vehicle` | Coordinator | Gán phương tiện |
| `POST` | `/api/trips/{id}/assign-staff` | Coordinator | Phân công nhân sự |
| `DELETE` | `/api/trips/{id}/staff/{staffId}` | Coordinator | Gỡ nhân sự khỏi chuyến |
| `POST` | `/api/trips/{id}/cancel` | Coordinator, Manager | Hủy chuyến |
| `POST` | `/api/trips/{id}/start` | Driver/Escort | Bắt đầu chuyến |
| `PATCH` | `/api/trips/{id}/status` | Driver/Escort, Manager | Cập nhật trạng thái chuyến |
| `POST` | `/api/trips/{id}/checkpoints` | Driver/Escort | Ghi checkpoint / vị trí / trễ |
| `GET` | `/api/trips/{id}/tracking` | Đã đăng nhập | Theo dõi hành trình |
| `GET` | `/api/trips/{id}/timeline` | Đã đăng nhập | Dòng thời gian của chuyến |

### Sức khỏe & tư vấn thú y

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `POST` | `/api/health-checks/pre-transport` | Driver/Escort | Kiểm tra (quan sát) trước vận chuyển cho từng ngựa |
| `GET` | `/api/trips/{tripId}/pre-transport-checks` | Đã đăng nhập | Kiểm tra trước vận chuyển của chuyến |
| `POST` | `/api/health-observations` | Driver/Escort | Ghi quan sát sức khỏe (không chẩn đoán); reportIncident=true để báo sự cố ngay |
| `GET` | `/api/trips/{tripId}/health-observations` | Đã đăng nhập | Quan sát sức khỏe của chuyến |
| `POST` | `/api/health-checks/arrival` | Driver/Escort | Kiểm tra khi đến nơi |
| `GET` | `/api/trips/{tripId}/arrival-checks` | Đã đăng nhập | Kiểm tra khi đến của chuyến |
| `POST` | `/api/veterinary-consultations` | Manager, Specialist, Driver/Escort | Ghi nhận tư vấn từ bác sĩ thú y BÊN NGOÀI (không phải tài khoản hệ thống) |
| `GET` | `/api/veterinary-consultations/{id}` | Đã đăng nhập | Chi tiết tư vấn thú y |
| `POST` | `/api/veterinary-consultations/{id}/report` | Manager, Specialist, Driver/Escort | Tải báo cáo thú y |
| `GET` | `/api/veterinary-consultations/{id}/report` | Đã đăng nhập | Báo cáo mới |
| `GET` | `/api/horses/{horseId}/health-history` | Đã đăng nhập | Lịch sử sức khỏe của ngựa |

### Sự cố

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `POST` | `/api/incidents` | Driver/Escort, Manager | Báo sự cố. Mức Cao/Nghiêm trọng tự tạm dừng chuyến chờ Manager quyết định |
| `GET` | `/api/incidents` | Đã đăng nhập | Danh sách |
| `GET` | `/api/incidents/{id}` | Đã đăng nhập | Chi tiết |
| `POST` | `/api/incidents/{id}/actions` | Manager, Specialist, Driver/Escort | Ghi hành động xử lý; actionType=DECISION (chỉ Manager) với Continue/Monitor/Stop/Reroute |
| `PATCH` | `/api/incidents/{id}/status` | Manager | Cập nhật trạng thái |

### Bàn giao

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `POST` | `/api/handovers` | Driver/Escort | Tạo mới |
| `GET` | `/api/handovers` | Đã đăng nhập | Danh sách |
| `GET` | `/api/handovers/{id}` | Đã đăng nhập | Chi tiết |
| `PUT` | `/api/handovers/{id}` | Driver/Escort | Cập nhật |
| `POST` | `/api/handovers/{id}/confirm` | Customer | Khách hàng xác nhận / phản hồi vấn đề |
| `GET` | `/api/trips/{tripId}/handovers` | Đã đăng nhập | Theo chuyến |

### Bồi thường

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `POST` | `/api/claims` | Customer | Nộp hồ sơ bồi thường |
| `GET` | `/api/claims` | Customer, Manager | Danh sách |
| `GET` | `/api/claims/{id}` | Customer, Manager | Chi tiết |
| `POST` | `/api/claims/{id}/evidence` | Customer, Manager | Bổ sung bằng chứng (tải tệp) |
| `GET` | `/api/claims/{id}/evidence/{evidenceId}/file` | Customer, Manager | Tải tệp bằng chứng |
| `POST` | `/api/claims/{id}/review` | Manager | Ghi nhận thẩm định |
| `POST` | `/api/claims/{id}/approve` | Manager | Phê duyệt (số tiền do Manager quyết định) |
| `POST` | `/api/claims/{id}/reject` | Manager | Từ chối hồ sơ |
| `POST` | `/api/claims/{id}/settlement` | Manager | Chi trả (mô phỏng) |
| `POST` | `/api/claims/{id}/close` | Manager | Đóng hồ sơ |

### Thông báo

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/api/notifications` | Đã đăng nhập | Danh sách |
| `GET` | `/api/notifications/unread-count` | Đã đăng nhập | Số thông báo chưa đọc |
| `PATCH` | `/api/notifications/{id}/read` | Đã đăng nhập | Đánh dấu đã đọc |
| `POST` | `/api/notifications/read-all` | Đã đăng nhập | Đánh dấu tất cả đã đọc |

### Dashboard & báo cáo

| Method | Đường dẫn | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/api/dashboard` | Đã đăng nhập | Dashboard theo role của người dùng hiện tại |
| `GET` | `/api/reports/revenue` | Manager | Báo cáo doanh thu |
| `GET` | `/api/reports/transport` | Manager | Báo cáo vận chuyển |
| `GET` | `/api/reports/incidents` | Manager | Báo cáo sự cố |
| `GET` | `/api/reports/claims` | Manager | Báo cáo bồi thường |
| `GET` | `/api/reports/kpi` | Manager | KPI vận hành |
