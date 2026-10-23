# BUSINESS_WORKFLOWS — Quy trình nghiệp vụ IRTMS

Tài liệu này mô tả các quy trình A–H và máy trạng thái. Bảng chuyển trạng thái được lấy trực tiếp từ mã nguồn
(`RequestWorkflow`, `TripStateMachine`) — nếu sửa mã, phải sửa lại tài liệu này.

Vai trò: **CUSTOMER** (Khách hàng), **MANAGER** (Quản lý), **SPECIALIST** (Chuyên viên hồ sơ), **COORDINATOR** (Điều phối viên), **DRIVER** (Tài xế/Hộ tống).

## A. Khách hàng gửi yêu cầu vận chuyển
1. Customer đăng ký (chỉ tạo role CUSTOMER) và đăng nhập.
2. Customer tạo hồ sơ ngựa (`horses`), khai báo giá trị ngựa (**chỉ để tham chiếu bảo hiểm/bồi thường, không phải giá vận chuyển**).
3. Customer tạo yêu cầu (DRAFT), chọn ngựa, điểm đi/đến, ngày mong muốn, loại chuồng; tải tài liệu (hộ chiếu, giấy tiêm chủng, xét nghiệm…).
4. Customer gửi yêu cầu → SUBMITTED.

### Máy trạng thái Request
| Từ | Sang (được phép) |
|---|---|
| DRAFT | SUBMITTED, CANCELLED |
| SUBMITTED | UNDER_DOCUMENT_REVIEW, INFORMATION_REQUIRED, REJECTED, CANCELLED |
| UNDER_DOCUMENT_REVIEW | COMPLIANCE_REVIEW, INFORMATION_REQUIRED, REJECTED, CANCELLED |
| INFORMATION_REQUIRED | SUBMITTED, REJECTED, CANCELLED |
| COMPLIANCE_REVIEW | PENDING_MANAGER_APPROVAL, INFORMATION_REQUIRED, REJECTED, CANCELLED |
| PENDING_MANAGER_APPROVAL | APPROVED, REJECTED, INFORMATION_REQUIRED, CANCELLED |
| APPROVED | QUOTE_SENT, CANCELLED |
| QUOTE_SENT | QUOTE_SENT (báo giá phiên bản mới), QUOTE_ACCEPTED, QUOTE_REJECTED, CANCELLED |
| QUOTE_REJECTED | QUOTE_SENT, CANCELLED |
| QUOTE_ACCEPTED | READY_FOR_PLANNING |
| READY_FOR_PLANNING | TRIP_PLANNED |
| TRIP_PLANNED | TRIP_PLANNED (thêm chuyến), COMPLETED, READY_FOR_PLANNING (huỷ chuyến) |
| REJECTED / CANCELLED / COMPLETED | (trạng thái kết thúc) |

Chỉ DRAFT và INFORMATION_REQUIRED cho khách sửa nội dung. Mọi chuyển trạng thái ghi `request_status_history`.

## B. Thẩm định hồ sơ và tuân thủ (Specialist)
1. Specialist nhận yêu cầu SUBMITTED → UNDER_DOCUMENT_REVIEW; duyệt/từ chối từng tài liệu (lý do bắt buộc khi từ chối).
2. Chuyển COMPLIANCE_REVIEW: tick danh mục yêu cầu tuân thủ theo tuyến/quốc gia (`compliance_requirements`) — dữ liệu demo, xem ASSUMPTIONS.
3. Thiếu thông tin → INFORMATION_REQUIRED (khách bổ sung rồi gửi lại). Đạt → PENDING_MANAGER_APPROVAL.
4. Hồ sơ sức khoẻ ngựa (`health_records`) do Specialist quản lý.

## C. Quản lý duyệt và báo giá
1. Manager duyệt (APPROVED) hoặc từ chối (lý do bắt buộc).
2. Manager lập báo giá từ bảng giá (`pricing_rates`) + phụ phí. **Tổng tiền do backend tính**, frontend chỉ hiển thị.
3. Gửi báo giá → QUOTE_SENT; hiệu lực 14 ngày. Gửi phiên bản mới → bản cũ thành SUPERSEDED (`version_no` tăng).
4. Giá vận chuyển **không** lấy từ Declared Horse Value.

## D. Khách chấp nhận và thanh toán (mô phỏng)
1. Customer chấp nhận → QUOTE_ACCEPTED, sinh hoá đơn (hạn 7 ngày) với đặt cọc 50%.
2. Thanh toán mô phỏng (không xử lý thẻ thật). Thẻ kết thúc `0002` bị từ chối (HTTP 200, `status=FAILED`) để demo thất bại.
3. Thanh toán ≥ tiền cọc → READY_FOR_PLANNING. Có thể thanh toán nhiều lần cho đến đủ.

## E. Lập kế hoạch chuyến (Coordinator)
1. Tạo tuyến (`routes`) với các điểm dừng/biên giới/hải quan; tạo chuyến (TRIP-…) → request TRIP_PLANNED.
2. Gán xe (kiểm tra sức chứa/loại chuồng và xung đột lịch), gán tài xế và người hộ tống (kiểm tra xung đột lịch).
3. Chuyến mới ở SCHEDULED.

### Máy trạng thái Trip
| Từ | Sang |
|---|---|
| SCHEDULED | PREPARING, CANCELLED |
| PREPARING | READY_FOR_DEPARTURE, CANCELLED |
| READY_FOR_DEPARTURE | IN_TRANSIT, PREPARING, CANCELLED |
| IN_TRANSIT / AT_CHECKPOINT / AT_BORDER / CUSTOMS_CLEARANCE / QUARANTINE / DELAYED | bất kỳ trạng thái "đang đi" khác, hoặc ARRIVED |
| ARRIVED | HANDOVER_PENDING |
| HANDOVER_PENDING | DELIVERED |
| DELIVERED / CANCELLED | (kết thúc) |

`TripStatusService` là nơi duy nhất đổi trạng thái chuyến; mọi thay đổi sinh sự kiện timeline.

## F. Thực hiện chuyến (Driver)
1. Kiểm tra trước khởi hành (pre-check) → bắt đầu chuyến → IN_TRANSIT.
2. Cập nhật vị trí/checkpoint **nhập thủ công** (không GPS); bản đồ là sơ đồ SVG.
3. Ghi nhận sức khoẻ ngựa định kỳ; **mức độ nghiêm trọng do backend tính**.
4. Đến nơi: kiểm tra khi đến (arrival check).

## G. Sự cố và quyết định
1. Driver/Coordinator báo sự cố (loại, mức LOW/MEDIUM/HIGH/CRITICAL).
2. **HIGH/CRITICAL tự động chặn chuyến** (DELAYED + `operationBlocked`) cho đến khi Manager quyết định: CONTINUE / MONITOR / STOP / REROUTE (có thể kèm chi phí phát sinh, tư vấn thú y bên ngoài).
3. Sự cố **không tự tạo Claim**.
4. Trạng thái xử lý sự cố được cập nhật tới khi đóng.

## H. Bàn giao, bồi thường, quyết toán
1. **Handover** theo từng cặp (chuyến, ngựa): tình trạng, người nhận, ảnh/biên bản. Người nhận xác nhận hoặc tranh chấp.
2. Khi mọi ngựa trong chuyến được xác nhận → chuyến DELIVERED → request COMPLETED (khi không còn chuyến hoạt động).
3. **Claim** (khách hoặc nhân viên lập): SUBMITTED → UNDER_REVIEW / EVIDENCE_REQUIRED → APPROVED / PARTIALLY_APPROVED / REJECTED → PAID → CLOSED.
4. Loại TOTAL_LOSS (MORTALITY, HUMANE_EUTHANASIA, THEFT_OR_LOSS) bị giới hạn ≤ giá trị khai báo. Giá trị khai báo **không tự điền** số tiền yêu cầu.
5. Manager quyết toán (`settlements`) và ghi nhận chi trả.

## Quy tắc chéo
- Mã nghiệp vụ: `PREFIX-YYYY-NNNN` (REQ, TRIP, QT, INV, PAY, INC, HO, CLM, STL, RT).
- Phân quyền kiểm tra ở backend (`@PreAuthorize`) và có kiểm tra quyền sở hữu (khách chỉ thấy dữ liệu của mình).
- Tệp tải lên: giới hạn dung lượng/loại, kiểm tra magic bytes, lưu tên UUID, SHA-256, chống path traversal.
- Mọi thao tác ghi dùng giao dịch; xung đột đồng thời trả `CONCURRENT_UPDATE`.
