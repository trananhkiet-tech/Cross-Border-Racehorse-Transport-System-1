/* =====================================================================
   IRTMS – tạo database và tài khoản ứng dụng trên Microsoft SQL Server 2019+.

   Chạy MỘT LẦN bằng tài khoản quản trị (sa hoặc sysadmin), ví dụ:
     sqlcmd -S localhost -U sa -P "<mat-khau-sa>" -C -i database/create-database.sql ^
            -v APP_PASSWORD="<mat-khau-cho-irtms_app>"
   (-C: tin cậy chứng chỉ tự ký khi dev. Trên Linux/macOS dùng \ thay cho ^.)

   Script này CHỈ tạo database rỗng + login/user. Bảng và dữ liệu tham chiếu do Flyway
   tạo khi backend khởi động (backend/src/main/resources/db/migration/V1, V2).
   Mật khẩu không nằm trong repository: truyền qua biến sqlcmd APP_PASSWORD.
   ===================================================================== */
SET NOCOUNT ON;
GO

IF DB_ID(N'irtms') IS NULL
BEGIN
    CREATE DATABASE irtms COLLATE Vietnamese_100_CI_AS;
    PRINT N'Đã tạo database irtms.';
END
ELSE
    PRINT N'Database irtms đã tồn tại – bỏ qua.';
GO

IF SUSER_ID(N'irtms_app') IS NULL
BEGIN
    DECLARE @sql NVARCHAR(400) = N'CREATE LOGIN irtms_app WITH PASSWORD = N''$(APP_PASSWORD)'', CHECK_POLICY = ON;';
    EXEC (@sql);
    PRINT N'Đã tạo login irtms_app.';
END
GO

USE irtms;
GO

IF USER_ID(N'irtms_app') IS NULL
    CREATE USER irtms_app FOR LOGIN irtms_app;
GO

-- Flyway cần tạo/sửa bảng (DDL) → db_owner trong database irtms.
-- Với môi trường thật có thể tách tài khoản migration (db_owner) và tài khoản chạy ứng dụng (db_datareader/db_datawriter).
ALTER ROLE db_owner ADD MEMBER irtms_app;
GO

PRINT N'Hoàn tất. Tiếp theo: cấu hình backend/.env rồi chạy backend để Flyway tạo schema.';
GO
