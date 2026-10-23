# TEST_PLAN — Kế hoạch và kết quả kiểm thử

> **Trung thực về kết quả:** phần dưới tách rõ *đã chạy thật*, *chỉ kiểm tra kiểu/cú pháp* và *chưa chạy*.
> Môi trường phát triển hệ thống này không có Maven và không truy cập được Maven Central, nên backend **chưa được build/chạy bằng Maven**.

## 1. Tóm tắt trạng thái

| Hạng mục | Kết quả | Ghi chú |
|---|---|---|
| Frontend `tsc --noEmit` | ✅ 0 lỗi | đã chạy |
| Frontend `vite build` | ✅ thành công | đã chạy (cảnh báo chunk lớn ~917 kB) |
| Frontend Vitest | ✅ 48/48 test đạt | đã chạy |
| Smoke test trình duyệt (Chromium + mock API) | ✅ không lỗi console | đã chạy; **không phải** backend thật |
| Đối chiếu endpoint frontend ↔ backend | ✅ khớp (134 endpoint) | bằng script tĩnh |
| Backend unit test (82) | ⚠️ đạt 82/82 **với stub thư viện** trong sandbox | không phải `mvn test` thật |
| Backend integration test (11) | ⚠️ chỉ kiểm tra biên dịch/kiểu | chưa chạy |
| `mvn test` thật, khởi động Spring Boot thật | ❌ chưa chạy | cần chạy trên máy có Maven |
| Flyway V1/V2 trên SQL Server | ❌ chưa chạy | chỉ parse cú pháp T-SQL (217 + 44 câu lệnh) |
| End-to-end frontend ↔ backend thật | ❌ chưa chạy | dùng checklist §5 |

Tổng số `@Test` backend: 93 (82 unit + 11 integration).

## 2. Backend
- **Unit (JUnit 5 + Mockito)**: máy trạng thái request/trip, tính báo giá, thanh toán mô phỏng, tính mức nghiêm trọng sức khoẻ, quy tắc claim (giới hạn TOTAL_LOSS), kiểm tra tệp (magic bytes, tên an toàn), phân quyền sở hữu.
- **Integration (Spring Boot Test + H2 chế độ MSSQL)**: bảo mật JWT, vai trò, luồng chính. Cần `mvn test` để xác nhận.
- Lệnh: `cd backend && mvn test`

## 3. Frontend
- Lệnh: `cd frontend && npm test` (Vitest + Testing Library + jsdom), `npm run build`, `npx tsc --noEmit`.
- Phạm vi: validate form, định dạng, bảo vệ route/vai trò (`ROUTE_ACCESS`), trang đăng nhập (lỗi 401), thành phần dùng chung, các trang chính.

## 4. Kiểm thử bảo mật cần xác nhận khi chạy thật
- Đăng ký công khai không thể tạo vai trò khác CUSTOMER.
- Customer không đọc được dữ liệu của khách khác (404/403).
- Upload tệp sai loại/quá 10 MB/tên có `../` bị từ chối.
- Phản hồi lỗi không chứa stack trace.
- CORS từ origin lạ bị chặn.
- Không có secret trong kho mã (`JWT_SECRET` chỉ từ biến môi trường).

## 5. Checklist demo end-to-end (kịch bản §16) — chạy thủ công
Mật khẩu mọi tài khoản demo: `Demo@123` (profile `dev`).

1. [ ] `customer@irtms.vn` đăng nhập, tạo ngựa và yêu cầu, tải tài liệu, gửi yêu cầu.
2. [ ] `specialist@irtms.vn` duyệt tài liệu, tick tuân thủ, chuyển Manager.
3. [ ] `manager@irtms.vn` duyệt, lập và gửi báo giá (kiểm tra tổng do backend tính).
4. [ ] Customer chấp nhận báo giá, thanh toán cọc (thử thẻ `...0002` → thất bại; thẻ khác → thành công) → READY_FOR_PLANNING.
5. [ ] `coordinator@irtms.vn` tạo tuyến/chuyến, gán xe, tài xế, hộ tống.
6. [ ] `driver@irtms.vn` pre-check, bắt đầu chuyến, cập nhật checkpoint, ghi nhận sức khoẻ.
7. [ ] Báo sự cố HIGH/CRITICAL → chuyến bị chặn; Manager quyết định → chuyến tiếp tục.
8. [ ] Đến nơi, kiểm tra khi đến, bàn giao; người nhận xác nhận → DELIVERED.
9. [ ] Tạo claim, Manager xét duyệt, quyết toán, chi trả, đóng.
10. [ ] Dashboard và Báo cáo hiển thị số liệu phù hợp; kiểm tra 403 khi truy cập sai vai trò.

Mỗi bước ghi lại kết quả (đạt/không đạt, ngày, người chạy) khi thực hiện.

## 6. Rủi ro đã biết chưa được kiểm chứng
- Khác biệt H2 (MSSQL mode) so với SQL Server thật (collation, kiểu dữ liệu, chỉ mục lọc).
- Phụ thuộc Maven chưa được giải quyết/khoá phiên bản trong môi trường này.
- Hiệu năng truy vấn với dữ liệu lớn chưa đo.
