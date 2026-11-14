# EXCEPTION_FLOWS — Các tình huống ngoại lệ ("happy case" mở rộng) và luồng xử lý

Tài liệu này mô tả **cách hệ thống IRTMS hiện tại xử lý** các tình huống bất thường, bước nào do vai trò nào làm, dùng màn hình/API nào.
Mỗi luồng ghi rõ **Hệ thống đã hỗ trợ** và **Khoảng trống** (việc phải làm ngoài hệ thống hoặc cần phát triển thêm) — không giả định chức năng chưa có.

Vai trò: CUSTOMER (khách), MANAGER (Logistics Manager), SPECIALIST (chuyên viên hồ sơ/thú y), COORDINATOR (điều phối), DRIVER (tài xế/hộ tống).

> Mức bồi thường ở các bảng dưới đây là **đề xuất chính sách để nhóm xác nhận**, không phải quy tắc đã cài trong code.
> Quy tắc đã cài: số tiền yêu cầu của loại tổn thất toàn bộ (ngựa chết, an tử, mất) **không vượt giá trị khai báo của ngựa**; số duyệt ≤ số yêu cầu; số chi trả ≤ số còn lại.

---

## 1. Ngựa chết trong lúc vận chuyển

| # | Bước | Ai | Màn hình / API | Ghi chú |
|---|---|---|---|---|
| 1 | Ngừng chuyến tại chỗ, bảo đảm an toàn cho các ngựa còn lại | DRIVER | Báo sự cố (`POST /incidents`) loại **Sức khỏe ngựa**, mức **CRITICAL**, chọn ngựa | Chưa có loại sự cố riêng "ngựa chết" → dùng HORSE_HEALTH/CRITICAL, mô tả rõ trong nội dung |
| 2 | Hệ thống chặn chuyến, báo Manager + khách | Hệ thống | `operationBlocked = true`; thông báo CRITICAL | Chuyến không đi tiếp cho đến khi có quyết định |
| 3 | Bác sĩ thú y tại chỗ xác nhận nguyên nhân; chụp ảnh, lấy biên bản | DRIVER/SPECIALIST | Thêm hành động/bằng chứng trên sự cố; ghi nhận sức khỏe (`health-observations`) | Giữ biên bản thú y + biên bản cơ quan địa phương (cần cho bồi thường) |
| 4 | Manager quyết định: **STOP** (dừng hẳn), **MONITOR** (đi tiếp có theo dõi – khi còn ngựa khác), **REROUTE** (đổi tuyến về cơ sở thú y/kiểm dịch gần) | MANAGER | `POST /incidents/{id}/actions` | HIGH/CRITICAL không đóng được nếu chưa có quyết định ≠ STOP |
| 5 | Xử lý xác ngựa theo quy định kiểm dịch nước sở tại (tiêu hủy/khám nghiệm) | SPECIALIST + đối tác | Ngoài hệ thống; ghi chú vào sự cố | Khai báo với cơ quan thú y; **không** tự ý chuyển xác qua biên giới |
| 6 | Khách nộp yêu cầu bồi thường loại **Ngựa chết** | CUSTOMER | `POST /claims` (cần yêu cầu ở TRIP_PLANNED/COMPLETED, chọn ngựa) | Số tiền ≤ giá trị khai báo của ngựa |
| 7 | Bổ sung bằng chứng: báo cáo thú y, ảnh, biên bản | CUSTOMER/MANAGER | `POST /claims/{id}/evidence` | Bắt buộc ≥ 1 bằng chứng trước khi duyệt |
| 8 | Thẩm định → duyệt/duyệt một phần/từ chối | MANAGER | Assessment → Decision | Cần ≥ 1 nhận định thẩm định |
| 9 | Chi trả (có thể chia nhiều đợt) → đóng hồ sơ | MANAGER | `POST /claims/{id}/settle`, `/close` | Chi trả ≤ số còn lại |
| 10 | Hoàn tất chuyến cho các ngựa còn lại hoặc kết thúc chuyến | COORDINATOR/MANAGER | Cập nhật trạng thái / bàn giao | Ngựa chết ghi rõ trong biên bản bàn giao |

**Bồi thường đề xuất:** tối đa = giá trị khai báo; trừ phần bảo hiểm bên thứ ba đã chi (nếu có); loại trừ khi nguyên nhân là bệnh có sẵn đã bị che giấu trong hồ sơ sức khỏe.
**Khoảng trống:** không có loại sự cố "ngựa chết"; không có bước tự động xác định ngựa khác trong chuyến phải khám lại; không tích hợp công ty bảo hiểm.

---

## 2. Khách muốn hủy chuyến giữa chừng

Hệ thống chia theo thời điểm:

| Thời điểm | Hỗ trợ trong hệ thống | Cách xử lý |
|---|---|---|
| Trước khi xuất phát (SCHEDULED / PREPARING / READY_FOR_DEPARTURE) | **Có** – `POST /trips/{id}/cancel` (cần lý do) → yêu cầu về READY_FOR_PLANNING, lên chuyến lại được | Coordinator/Manager hủy chuyến; giải phóng xe + nhân sự; khách nhận thông báo |
| Trước khi duyệt/báo giá | **Có** – khách hủy yêu cầu (CANCELLED) | Không phát sinh phí |
| **Đang trên đường** (IN_TRANSIT trở đi) | **Không** – máy trạng thái chặn hủy | Xử lý theo luồng thủ công dưới đây |

Luồng thủ công khi khách yêu cầu dừng/hủy đang trên đường:
1. Khách liên hệ Manager (ngoài hệ thống) → Manager tạo **sự cố loại Khác, mức HIGH**: "Khách yêu cầu dừng vận chuyển".
2. Manager chọn quyết định **STOP** để dừng chuyến tại điểm an toàn (chuyến chuyển DELAYED, bị chặn).
3. Quyết định hướng đi: (a) **đưa ngựa về điểm xuất phát** (REROUTE sang tuyến quay đầu, ghi `extraCost`), (b) **gửi tại trại/cơ sở kiểm dịch gần nhất** chờ khách xử lý, (c) **tiếp tục** nếu khách đổi ý (MONITOR/CONTINUE).
4. Tính phí thực tế (đề xuất): phí đã đi + chi phí phát sinh (đổi tuyến, lưu chuồng, kiểm dịch) – không hoàn phần đã sử dụng; phần dịch vụ chưa thực hiện hoàn theo hợp đồng.
5. Kết thúc: Manager đóng sự cố; chuyến kết thúc bằng bàn giao tại điểm thỏa thuận.

**Khoảng trống:** chưa có chức năng hủy giữa chặng, chưa có hoàn tiền/tính phí hủy, chưa có biên bản bàn giao "một phần". Nếu cần, bổ sung: trạng thái chuyến `ABORTED`, API `POST /trips/{id}/abort`, bảng phí hủy theo mốc (cần migration CSDL).

---

## 3. Bồi thường vận chuyển — quy trình chung

```
Khách nộp claim ──► SUBMITTED ──► UNDER_REVIEW ◄──► EVIDENCE_REQUIRED
                                      │
              ┌───────────────────────┼───────────────────────┐
         APPROVED               PARTIALLY_APPROVED         REJECTED
              └──────────┬────────────┘                        │
                       PAID (theo từng đợt)                    │
                         └──────────────► CLOSED ◄─────────────┘
```

| Loại bồi thường (ClaimType) | Khi nào | Mức tối đa đề xuất | Bằng chứng cần |
|---|---|---|---|
| Ngựa chết / An tử nhân đạo | Ngựa chết hoặc phải an tử do tai nạn/bệnh trong chuyến | = giá trị khai báo (đã cài) | Báo cáo thú y, biên bản, ảnh |
| Mất cắp / thất lạc | Ngựa bị mất, không bàn giao được | = giá trị khai báo (đã cài) | Biên bản công an/cơ quan chức năng |
| Chi phí điều trị chấn thương | Ngựa bị thương, đã điều trị | Chi phí thực tế có hóa đơn | Hóa đơn thú y, báo cáo điều trị |
| Hư hỏng tài sản | Hư yên cương, dụng cụ đi kèm | Giá trị sửa/thay thế | Ảnh, hóa đơn |
| Chậm trễ dịch vụ | Trễ vượt cam kết do lỗi nhà vận chuyển | Theo % phí vận chuyển (chính sách do nhóm đặt) | Nhật ký hành trình (`/tracking`) |
| Tổn thất trực tiếp khác | Khác | Theo thẩm định | Tùy trường hợp |

Điều kiện đã cài: chỉ nộp khi yêu cầu ở TRIP_PLANNED/COMPLETED; ≥ 1 nhận định + ≥ 1 bằng chứng mới duyệt được; chỉ chi trả hồ sơ đã duyệt; chỉ đóng hồ sơ đã chi trả hoặc bị từ chối.
**Tách biệt:** sự cố (vận hành) và bồi thường (tài chính) là hai luồng riêng; claim có thể gắn với sự cố.
**Loại trừ đề xuất:** bất khả kháng đã nêu trong hợp đồng, bệnh có sẵn không khai báo, khai sai thông tin ngựa.

---

## 4. Các tình huống hiếm gặp

| # | Tình huống | Hệ thống xử lý được | Việc cần làm | Khoảng trống |
|---|---|---|---|---|
| 4.1 | Hỏng xe giữa đường | Sự cố VEHICLE_BREAKDOWN; Manager STOP/REROUTE; phân xe khác trước xuất phát | DRIVER báo → Manager quyết định → điều xe thay thế, ghi trễ ở checkpoint | Không đổi xe khi đã xuất phát (chỉ ghi chú) |
| 4.2 | Bị từ chối tại cửa khẩu | Sự cố DOCUMENT_ISSUE/BORDER_DELAY; trạng thái AT_BORDER/CUSTOMS_CLEARANCE | SPECIALIST bổ sung hồ sơ; nếu không đạt → Manager REROUTE về điểm quay lại hoặc dừng | Không có trạng thái "bị từ chối nhập cảnh" |
| 4.3 | Ngựa bệnh/stress nặng, từ chối lên xe | Kiểm tra trước vận chuyển (`pre-check`), quan sát sức khỏe, sự cố HORSE_HEALTH | Ghi pre-check "không đủ điều kiện" → không xuất phát; Coordinator hủy chuyến trước khi đi | — |
| 4.4 | Tai nạn giao thông | Sự cố CRITICAL → chặn chuyến; claim chấn thương/ngựa chết/hư hỏng | Gọi cấp cứu thú y, Manager STOP; nộp claim | Không tích hợp bảo hiểm xe |
| 4.5 | Mất cắp / ngựa mất tích | Sự cố SECURITY CRITICAL; claim THEFT_OR_LOSS | Báo công an, lấy biên bản; Manager STOP | Không tích hợp cơ quan chức năng |
| 4.6 | Thiên tai, thời tiết xấu, đóng đường | Sự cố WEATHER; Manager MONITOR/REROUTE; chuyến DELAYED | Chọn tuyến thay thế ACTIVE (có chi phí thêm `extraCost`), báo khách | Không lấy dữ liệu thời tiết tự động |
| 4.7 | Giấy tờ hết hạn khi đang đi (tiêm chủng, giấy phép) | Sự cố DOCUMENT_ISSUE; SPECIALIST quản lý hồ sơ; tuân thủ theo tuyến | Gia hạn khẩn cấp; STOP đến khi hợp lệ | Chưa cảnh báo tự động hết hạn trên chuyến đang chạy |
| 4.8 | Tài xế mất khả năng lái / hết giờ lái | Phân công nhiều nhân sự; trạng thái nhân sự | Hộ tống/trưởng đoàn thay thế; Manager ghi sự cố OTHER | Không có kiểm soát giờ lái tự động |
| 4.9 | Kéo dài kiểm dịch | Trạng thái QUARANTINE; DELAY + ghi trễ; thông báo khách | Cập nhật hành trình + ETA; tính phí lưu chuồng | Chưa có biểu phí lưu chuồng |
| 4.10 | Khách không thanh toán / thanh toán thất bại | Hóa đơn/thanh toán; báo giá hết hiệu lực | Không lên chuyến khi chưa đặt cọc (theo chính sách nhóm) | Chưa có hoàn tiền/ghi nợ |
| 4.11 | Người nhận từ chối nhận ngựa | Trạng thái HANDOVER_PENDING; sự cố OTHER | Manager liên hệ khách; gửi chuồng tạm; phí lưu chuồng | Không có bàn giao "bị từ chối" |
| 4.12 | Mất GPS/mất liên lạc | Checkpoint thủ công; GPS thiết bị gắn vào checkpoint; bản đồ tự làm mới 15 giây | DRIVER cập nhật checkpoint khi có sóng; Coordinator gọi xác nhận | Không có theo dõi GPS liên tục tự động |

---

## 5. Bản đồ & thời gian thực (liên quan các luồng trên)

- Màn hình Theo dõi chuyến dùng **bản đồ thật** (OpenStreetMap, kéo/phóng như Google Maps, chuyển lớp vệ tinh), tuyến đường bộ dẫn theo đường thật qua OSRM, tự làm mới mỗi 15 giây.
- Vị trí xe = sự kiện hành trình mới nhất có tọa độ (tài xế bấm "Lấy vị trí GPS của thiết bị" trong *Cập nhật hành trình*); nếu chưa có thì lấy điểm dừng hiện tại.
- Giới hạn: đây là **cập nhật theo checkpoint (gần thời gian thực)**, chưa phải GPS truyền liên tục. Có thể nâng cấp bằng ứng dụng tài xế gửi tọa độ định kỳ hoặc WebSocket.
- Cấu hình dịch vụ bản đồ qua biến môi trường: `VITE_MAP_TILE_URL`, `VITE_MAP_SAT_URL`, `VITE_OSRM_URL`. Máy chủ OSRM công cộng chỉ phù hợp demo; chạy thật nên tự dựng.
