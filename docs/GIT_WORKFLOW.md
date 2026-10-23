# GIT_WORKFLOW — `main` + 5 nhánh thành viên

## Cấu trúc nhánh

| Nhánh | Nội dung |
|---|---|
| `main` | **Đăng nhập / đăng ký + khung dùng chung** (security, JWT, common, config, user entity/repo/service, migration Flyway, API client, design system, types/enums, routes/menu/access, docs). Mọi tệp của module nghiệp vụ có mặt nhưng **RỖNG** (0 byte) để mỗi người điền vào đúng chỗ của mình. |
| `user1` | M1 — Ngựa, Yêu cầu vận chuyển, Hồ sơ cá nhân, API người dùng/khách hàng/nhân viên, `ReferenceController` |
| `user2` | M2 — Tài liệu, Tuân thủ, Hồ sơ sức khoẻ (backend `health`) |
| `user3` | M3 — Đội xe, Tuyến đường, Lập chuyến (`trip`, trừ `TripExecutionService`) + khôi phục đếm chuyến đang phụ trách trong `UserService` |
| `user4` | M4 — Thực hiện chuyến (`TripExecutionService`), Theo dõi, Sự cố, Bàn giao, màn hình sức khoẻ trong chuyến |
| `user5` | M5 — Báo giá, Hoá đơn, Thanh toán, Bồi thường, Báo cáo, Thông báo, Dashboard, seed dữ liệu demo, test tích hợp |

Mỗi nhánh `userN` = `main` + **đúng 1 commit** chứa phần việc của người đó (các tệp rỗng ở main được điền nội dung thật).

## Quy trình
```bash
git clone <url-repo> && cd <repo>
git checkout userN            # N = số của bạn; có thể sửa tiếp / chia nhỏ commit
# ... code, commit ...
git push origin userN
# mở Pull Request: userN -> main
```
Chỉ sửa **các tệp thuộc module của mình** (đang rỗng ở main). Với các tệp dùng chung, xem mục dưới.

## Thứ tự merge vào main
`user1 → user2 → user3 → user4 → user5` (mỗi PR merge vào `main` theo thứ tự này).
Backend của các module **phụ thuộc chéo nhau** (ví dụ `horse` ↔ `transportationrequest` ↔ `document` ↔ `trip`), nên **từng nhánh riêng lẻ chưa biên dịch được**;
chỉ khi đã merge đủ 5 nhánh thì `mvn test` / `mvn spring-boot:run` mới chạy trọn. Riêng `main` luôn biên dịch được (chỉ có đăng nhập).
Frontend của `main` build được (`npm run build`) và chạy các test của phần đăng nhập/khung.

## Tệp dùng chung — cách tránh xung đột
- `frontend/src/routes/AppRoutes.tsx`: mỗi người **chỉ thay dòng** `// [userN:imports]` và `// [userN:routes]` của mình bằng import / route module mình. Không sửa dòng của người khác.
- `api/endpoints.ts`, `types/api.ts`, `types/enums.ts`, `routes/access.ts`, `routes/menu.ts`, `utils/status.ts`: đã đầy đủ ở `main` (hợp đồng chung FE ↔ BE). Cần đổi → báo nhóm, sửa trong PR nhỏ riêng vào `main`.
- Migration Flyway `V1`, `V2` đã đủ 38 bảng: không sửa; thay đổi schema = thêm `V<n>__...sql` mới.
- `DemoAccountSeeder` (main) tạo 9 tài khoản demo để đăng nhập thử. Khi merge `user5` tệp này bị xoá vì `DemoDataSeeder` thay thế.
- `UserService.toEmployeeResponse` ở main đặt `activeTrips = 0`; nhánh `user3` trả lại cách đếm theo phân công chuyến.
- Tệp test rỗng ở `frontend/src/test` được vitest bỏ qua tự động.

## Tài khoản demo (profile dev, mật khẩu `Demo@123`)
customer@irtms.vn · manager@irtms.vn · specialist@irtms.vn · coordinator@irtms.vn · driver@irtms.vn
