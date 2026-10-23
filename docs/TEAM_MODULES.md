# TEAM_MODULES — Phân chia module cho 5 thành viên

Mỗi module gồm backend (`com.example.racetransport.<module>`) và frontend (`frontend/src/features/<module>`).

| TV | Module | Backend (`com.example.racetransport.*`) | Frontend (`features/*`) |
|---|---|---|---|
| M1 | Khách hàng / Ngựa / Yêu cầu | `user` (controllers), `horse`, `transportationrequest` (đăng nhập `auth` + `user` entity/service nằm ở `main`) | `auth/ProfilePage`, `horses`, `transportation-requests` (Login/Register nằm ở `main`) |
| M2 | Tài liệu / Tuân thủ / Hồ sơ sức khoẻ | `document`, `compliance`, `health` | `documents`, `compliance` |
| M3 | Đội xe / Tuyến / Lập chuyến | `fleet`, `route`, `trip` | `fleet`, `routes`, `trips` |
| M4 | Thực hiện chuyến / Theo dõi / Sự cố / Bàn giao | `tracking`, `incident`, `handover`, `trip/service/TripExecutionService` | `tracking`, `health-observations`, `incidents`, `handovers` |
| M5 | Báo giá / Hoá đơn / Thanh toán / Bồi thường / Dashboard / Báo cáo | `quotation`, `payment`, `claim`, `report`, `notification` | `quotations`, `payments`, `claims`, `dashboard`, `reports`, `notifications` |

Dùng chung: `common`, `config`, `security` (backend); `common`, `api`, `components` (frontend).

## Tệp dùng chung — thay đổi cần báo cả nhóm
- Backend: `common/*` (exception, response, audit), `config/*` (security, CORS, clock), `security/*`, migration Flyway.
- Frontend: `api/client.ts`, `auth/*`, `components/*` (design system), `routes/access.ts`, `routes/AppRoutes.tsx`, `styles/*`.
- Docs: `API_CONTRACT.md`, `DATABASE.md`.

## Quy tắc tích hợp
1. **Migration**: không sửa V1/V2 đã chạy; thay đổi schema = thêm `V<n>__mo_ta.sql` mới, đánh số theo thứ tự merge, báo nhóm trước.
2. **API contract**: đổi endpoint/DTO phải cập nhật `API_CONTRACT.md` và kiểu TypeScript tương ứng trong cùng PR.
3. **Phân quyền**: thêm/đổi `@PreAuthorize` ở backend phải đồng bộ `routes/access.ts` (một nguồn duy nhất cho vai trò truy cập route ở UI).
4. **Trạng thái**: đổi trạng thái request chỉ qua `RequestWorkflow`; trạng thái chuyến chỉ qua `TripStatusService`. Không cập nhật trực tiếp `status` ở nơi khác.
5. **Phụ thuộc giữa module**: M5 (báo giá, `quotation`) cần request đã APPROVED từ M1/M2; M3 cần request READY_FOR_PLANNING từ M5; M4 cần chuyến từ M3; claim (M5) có thể tham chiếu sự cố/bàn giao (M4).
6. **Nhánh/PR**: `main` (đăng nhập + khung) và 5 nhánh `user1`…`user5` tương ứng M1…M5 (xem `GIT_WORKFLOW.md`); PR nhỏ, có test; không commit secret (`.env`, `JWT_SECRET`).
7. **Test**: mỗi module có unit test service/validator và test giao diện cho trang chính; chạy `mvn test` và `npm test` trước khi merge.
8. **Mã nghiệp vụ**: sinh mã qua bộ sinh mã chung, không tự ghép chuỗi.

Xem thêm [`GIT_WORKFLOW.md`](GIT_WORKFLOW.md) về cấu trúc nhánh `main` / `user1`…`user5` và thứ tự merge.
