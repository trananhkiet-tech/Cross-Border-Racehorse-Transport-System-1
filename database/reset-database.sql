/* =====================================================================
   IRTMS – XÓA TOÀN BỘ dữ liệu và schema (chỉ dùng ở môi trường DEV).
   Sau khi chạy, khởi động lại backend: Flyway dựng lại schema từ đầu và (profile dev)
   seed lại dữ liệu demo vì bảng app_user trống.

     sqlcmd -S localhost -U sa -P "<mat-khau-sa>" -C -i database/reset-database.sql

   ⚠ KHÔNG chạy trên môi trường thật. Dữ liệu bị mất vĩnh viễn.
   ===================================================================== */
USE master;
GO

IF DB_ID(N'irtms') IS NOT NULL
BEGIN
    ALTER DATABASE irtms SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE irtms;
    PRINT N'Đã xóa database irtms.';
END
GO

CREATE DATABASE irtms COLLATE Vietnamese_100_CI_AS;
GO

USE irtms;
GO

IF USER_ID(N'irtms_app') IS NULL AND SUSER_ID(N'irtms_app') IS NOT NULL
    CREATE USER irtms_app FOR LOGIN irtms_app;
GO

IF USER_ID(N'irtms_app') IS NOT NULL
    ALTER ROLE db_owner ADD MEMBER irtms_app;
GO

PRINT N'Đã dựng lại database irtms rỗng. Khởi động backend để Flyway tạo lại schema.';
GO
