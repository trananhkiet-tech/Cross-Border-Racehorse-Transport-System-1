-- =====================================================================
-- V3: cờ "lưu trữ" (xóa mềm) cho chuyến, yêu cầu vận chuyển và hồ sơ nhân sự.
-- Dữ liệu vẫn được giữ nguyên cho báo cáo/lịch sử; chỉ ẩn khỏi danh sách làm việc.
-- =====================================================================
ALTER TABLE transportation_trip ADD archived BIT NOT NULL CONSTRAINT df_transportation_trip_archived DEFAULT 0;
ALTER TABLE transportation_trip ADD archived_at DATETIME2(3) NULL;
ALTER TABLE transportation_request ADD archived BIT NOT NULL CONSTRAINT df_transportation_request_archived DEFAULT 0;
ALTER TABLE transportation_request ADD archived_at DATETIME2(3) NULL;
ALTER TABLE employee_profile ADD archived BIT NOT NULL CONSTRAINT df_employee_profile_archived DEFAULT 0;
ALTER TABLE employee_profile ADD archived_at DATETIME2(3) NULL;
