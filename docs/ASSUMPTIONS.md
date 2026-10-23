# ASSUMPTIONS — Giả định và phạm vi

Những điều dưới đây là quyết định của dự án khi đề bài chưa nêu rõ. Đây là **dữ liệu/ cơ chế demo**, không dùng cho vận hành thật.

## Phạm vi và vai trò
1. Đúng 5 vai trò: CUSTOMER, MANAGER, SPECIALIST, COORDINATOR, DRIVER. Không có VETERINARIAN; tư vấn thú y là bản ghi "tư vấn thú y bên ngoài" do nhân viên nhập.
2. Đăng ký công khai chỉ tạo CUSTOMER. Tài khoản nhân viên do seed (dev) hoặc quản trị tạo trực tiếp.
3. Vai trò Driver gồm cả người hộ tống (escort); trong dữ liệu demo có tài khoản `driver@`, `driver2@`, `escort@`…

## Tiền và thanh toán
4. **Thanh toán chỉ là mô phỏng**; không có cổng thanh toán thật, không lưu số thẻ đầy đủ. Thẻ kết thúc `0002` luôn bị từ chối để demo; các thẻ khác thành công.
5. Tiền cọc 50%; báo giá hiệu lực 14 ngày; hoá đơn đến hạn sau 7 ngày. Đây là tham số demo.
6. Bảng giá (`pricing_rates`, 11 dòng) và loại chuồng (3 loại) là **số liệu minh hoạ**, không phải giá thị trường.
7. Đơn vị tiền tệ mặc định VND; số tiền dùng `DECIMAL`, tính ở backend. Không có thuế/ngoại tệ quy đổi.
8. Declared Horse Value chỉ là tham chiếu bảo hiểm/bồi thường; không quyết định giá vận chuyển và không tự điền số tiền claim. Claim mất toàn bộ bị giới hạn ≤ giá trị khai báo.

## Tuân thủ và hồ sơ
9. 30 yêu cầu tuân thủ (`compliance_requirements`) là **dữ liệu minh hoạ** theo tuyến/quốc gia, không thay thế quy định pháp lý hay tư vấn của cơ quan thú y/hải quan.
10. Việc "duyệt tài liệu" là kiểm tra thủ công của Specialist; hệ thống không xác thực chữ ký/giấy tờ với cơ quan ngoài.

## Vận hành chuyến
11. Không có GPS/telematics: vị trí do tài xế nhập thủ công tại checkpoint. Bản đồ là sơ đồ SVG minh hoạ, không phải bản đồ thật.
12. Không gửi email/SMS/push; thông báo chỉ hiển thị trong ứng dụng.
13. Sự cố HIGH/CRITICAL chặn chuyến cho đến khi Manager quyết định. Sự cố không tự tạo claim.
14. Mức nghiêm trọng của quan sát sức khoẻ do backend tính theo ngưỡng cấu hình trong mã (ngưỡng demo).
15. Mỗi ngựa trong chuyến có một biên bản bàn giao; chuyến DELIVERED khi tất cả được xác nhận.

## Kỹ thuật
16. Múi giờ mặc định `Asia/Ho_Chi_Minh` (`APP_TIME_ZONE`). Thời điểm là `LocalDateTime` theo múi giờ này và hiển thị nguyên giá trị.
17. Tệp tải lên lưu ở thư mục cục bộ `UPLOAD_DIR` (không dùng object storage); tối đa 10 MB; chỉ PDF/JPEG/PNG/WebP; kiểm tra magic bytes.
18. JWT lưu ở phía client (hết hạn mặc định 480 phút), không có refresh token/đăng xuất phía server.
19. CORS chỉ cho origin dev (`CORS_ALLOWED_ORIGINS`, mặc định `http://localhost:5173`).
20. Flyway là nguồn duy nhất tạo schema (`ddl-auto: none`). Dữ liệu seed demo chỉ chạy ở profile `dev`, mật khẩu `Demo@123`.
21. Ngôn ngữ giao diện: tiếng Việt; mã định danh/enum/API giữ tiếng Anh.
22. Phân trang 0-based; danh sách lớn dùng phân trang phía server.

## Ngoài phạm vi (không triển khai)
Cổng thanh toán thật, GPS thời gian thực, email/SMS, ký số, đa ngôn ngữ, đa tiền tệ, tích hợp hải quan/thú y, ứng dụng di động.
Các nút liên quan, nếu có, hiển thị rõ là ngoài phạm vi.
