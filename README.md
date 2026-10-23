# IRTMS — Hệ thống quản lý vận chuyển ngựa đua quốc tế

Monorepo: `backend/` (Java 21, Spring Boot 3.5), `frontend/` (React + TypeScript + Vite), `database/` (script SQL Server), `docs/`.

## Yêu cầu môi trường
- JDK 21, Maven 3.9+
- Node.js ≥ 20.19, npm
- SQL Server 2019+ (có `sqlcmd`)

## 1. Cấu hình
```bash
cp .env.example .env            # tham khảo; backend đọc biến môi trường
cp backend/.env.example backend/.env   # Spring không tự đọc .env: nạp vào shell (set -a; source backend/.env; set +a)
```
Đặt tối thiểu: `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` và **`JWT_SECRET`** (chuỗi ngẫu nhiên ≥ 32 ký tự). Không commit file `.env`.
Ví dụ sinh secret: `openssl rand -base64 48`.

## 2. Tạo cơ sở dữ liệu
```bash
sqlcmd -S localhost -U sa -P '<sa-password>' -v APP_PASSWORD='<mat-khau-irtms_app>' -i database/create-database.sql
```
Tạo DB, collation `Vietnamese_100_CI_AS` và login `irtms_app`. Bảng được tạo bởi **Flyway** khi backend khởi động (`backend/src/main/resources/db/migration`). Xoá sạch dữ liệu dev: `database/reset-database.sql`.

## 3. Chạy backend
```bash
cd backend
export JWT_SECRET=...  DB_PASSWORD=...        # hoặc nạp từ .env
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```
- API: http://localhost:8080 · Swagger UI: http://localhost:8080/swagger-ui.html
- Profile `dev` nạp dữ liệu demo (`DemoDataSeeder`).

## 4. Chạy frontend
```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```
`VITE_API_BASE_URL` (mặc định trỏ backend local) cấu hình trong `frontend/.env`.

## 5. Tài khoản demo (mật khẩu `Demo@123`, chỉ profile dev)
| Vai trò | Email |
|---|---|
| Khách hàng | customer@irtms.vn, customer2@irtms.vn |
| Quản lý | manager@irtms.vn |
| Chuyên viên hồ sơ | specialist@irtms.vn |
| Điều phối viên | coordinator@irtms.vn |
| Tài xế / hộ tống | driver@, driver2@, escort@, escort2@, escort3@ `irtms.vn` |

Kịch bản demo đầy đủ: `docs/TEST_PLAN.md` §5. Thẻ mô phỏng kết thúc `0002` luôn bị từ chối.

## 6. Kiểm thử
```bash
cd backend && mvn test
cd frontend && npm test && npm run build
```

## 7. Quy tắc cần nhớ
- Thanh toán chỉ mô phỏng. Tổng tiền tính ở backend. Giá trị ngựa khai báo không phải giá vận chuyển.
- Múi giờ mặc định `Asia/Ho_Chi_Minh` (`APP_TIME_ZONE`).
- Tệp tải lên: ≤ 10 MB, PDF/JPEG/PNG/WebP, lưu tại `UPLOAD_DIR`.

## 8. Xử lý sự cố
- *Backend báo thiếu JWT_SECRET*: đặt biến môi trường `JWT_SECRET`.
- *Không kết nối được SQL Server*: kiểm tra TCP/IP đã bật, cổng 1433, `DB_URL` có `encrypt=true;trustServerCertificate=true` khi dev.
- *Flyway lỗi checksum*: không sửa V1/V2; dùng `reset-database.sql` ở môi trường dev.
- *Trình duyệt lỗi CORS*: đặt `CORS_ALLOWED_ORIGINS` đúng origin frontend.

## 9. Tài liệu
`docs/ARCHITECTURE.md`, `API_CONTRACT.md`, `DATABASE.md`, `BUSINESS_WORKFLOWS.md`, `ASSUMPTIONS.md`, `TEST_PLAN.md`, `TEAM_MODULES.md`.

## 10. Trạng thái kiểm chứng (trung thực)
Frontend đã build và chạy 48 test thật. Backend **chưa** được `mvn test`/khởi động trên SQL Server thật trong môi trường tạo mã (không có Maven); xem `docs/TEST_PLAN.md`. Hãy chạy §3 và §6 trên máy của bạn và báo lỗi nếu có.


## Làm việc nhóm trên Git
`main` chỉ gồm **đăng nhập / đăng ký + khung dùng chung** (các tệp module nghiệp vụ để rỗng); 5 nhánh `user1`…`user5` chứa code của từng thành viên. Chi tiết, thứ tự merge và cách tránh xung đột: [`docs/GIT_WORKFLOW.md`](docs/GIT_WORKFLOW.md).
