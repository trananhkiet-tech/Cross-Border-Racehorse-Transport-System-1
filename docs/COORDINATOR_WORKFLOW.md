# Fleet & Route Coordinator — Hướng dẫn nghiệp vụ

> **Vai trò:** `FLEET_ROUTE_COORDINATOR`  
> **Mô-đun phụ trách:** `fleet`, `route`, `trip`, `staffleave` (xem lịch nghỉ)

---

## 1. Trách nhiệm chính

| Nhiệm vụ | Mô-đun | API |
|---|---|---|
| Quản lý đội xe (thêm, sửa, đổi trạng thái, lưu trữ) | `fleet` | `POST/PUT/PATCH/DELETE /api/vehicles` |
| Truy vấn xe sẵn sàng theo khoảng thời gian | `fleet` | `GET /api/vehicles/available` |
| Tạo và quản lý tuyến đường + điểm dừng | `route` | `POST/PUT/DELETE /api/routes` |
| Kích hoạt / vô hiệu hóa tuyến | `route` | `PATCH /api/routes/{id}/activate` |
| Lập chuyến vận chuyển (gán xe, tuyến, nhân viên) | `trip` | `POST /api/trips` |
| Phân công và hủy phân công nhân viên cho chuyến | `trip` | `POST/DELETE /api/trips/{id}/staff` |
| Xem lịch nghỉ phép của tài xế / áp tải | `staffleave` | `GET /api/staff-leaves` |

---

## 2. Quy trình lập chuyến vận chuyển

```
Yêu cầu READY_FOR_PLANNING (từ Logistics Manager)
        │
        ▼
[1] Coordinator kiểm tra xe sẵn sàng
    GET /api/vehicles/available?from=...&to=...&minCapacity=2
        │
        ▼
[2] Coordinator chọn tuyến phù hợp
    GET /api/routes?status=ACTIVE&origin=VN&destination=SG
        │
        ▼
[3] Kiểm tra lịch nghỉ của tài xế / áp tải
    GET /api/staff-leaves  (xem ai đang nghỉ trong khoảng thời gian chuyến)
        │
        ▼
[4] Tạo chuyến (gán xe + tuyến)
    POST /api/trips  { vehicleId, routeId, requestId, departureTime, ... }
        │
        ▼
[5] Phân công tài xế và áp tải
    POST /api/trips/{id}/staff  { staffId, role: DRIVER | ESCORT }
        │
        ▼
[6] Chuyến chuyển sang trạng thái SCHEDULED
    → Logistics Manager có thể xem và phê duyệt
```

---

## 3. Vòng đời trạng thái xe (`VehicleStatus`)

```
         ┌──────────────────────────────────────────┐
         │                                          │
    AVAILABLE ──────────────────────► IN_USE        │
         ▲                                │         │
         │◄───── (chuyến kết thúc) ───────┘         │
         │                                          │
         ├─────► MAINTENANCE ─────► AVAILABLE        │
         │                                          │
         └─────► RETIRED (ẩn, giữ lịch sử)◄─────────┘
```

**Quy tắc:**
- Chỉ xe **AVAILABLE** mới được phân công cho chuyến mới.
- Xe **IN_USE** không được xóa hay chuyển sang RETIRED.
- Xe **RETIRED** / **archived=true** bị ẩn khỏi danh sách mặc định.

---

## 4. Vòng đời trạng thái tuyến (`RouteStatus`)

```
DRAFT ──► ACTIVE ──► INACTIVE
  │          │
  │          └──► (tuyến đang có chuyến IN_TRANSIT → không được INACTIVE)
  │
  └──► (xóa được nếu chưa có chuyến nào dùng tuyến này)
```

**Quy tắc:**
- Tuyến **DRAFT** phải có ít nhất **2 điểm dừng** mới kích hoạt được.
- Tuyến **ACTIVE** đang có chuyến `IN_TRANSIT` → không sửa điểm dừng.
- Xóa tuyến: nếu không có chuyến nào tham chiếu → xóa hẳn; ngược lại → lưu trữ (archived).

---

## 5. Quy tắc phân công nhân viên

- Mỗi chuyến cần **ít nhất 1 DRIVER** (tài xế) và **ít nhất 1 ESCORT** (áp tải).
- Nhân viên **không được phân công** nếu:
  - Đang có chuyến `OPEN_TRIP` trùng lịch khởi hành.
  - Đang có lịch nghỉ phép (`StaffLeave.status = ACTIVE`) trùng ngày chuyến.
- Hủy phân công chỉ được khi chuyến **chưa** chuyển sang `IN_TRANSIT`.

---

## 6. Tài khoản demo (môi trường dev)

| Email | Mật khẩu | Vai trò |
|---|---|---|
| `coordinator@irtms.vn` | `Demo@123` | `FLEET_ROUTE_COORDINATOR` |
| `manager@irtms.vn` | `Demo@123` | `LOGISTICS_MANAGER` |
| `driver@irtms.vn` | `Demo@123` | `DRIVER_ESCORT` |

---

*Cập nhật lần cuối: 2026-11-14 — dangbao-hub*
