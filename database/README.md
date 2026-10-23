# Thư mục `database/`

| Tệp | Mục đích |
|---|---|
| `create-database.sql` | Tạo database `irtms` (collation `Vietnamese_100_CI_AS`), login/user `irtms_app`. Chạy một lần bằng tài khoản quản trị. |
| `reset-database.sql` | **Chỉ dev**: xóa và tạo lại database rỗng để chạy migration từ đầu. |

## Migration (nguồn duy nhất)

Schema và dữ liệu tham chiếu do **Flyway** quản lý, nằm ở:

```
backend/src/main/resources/db/migration/
├── V1__init_schema.sql      -- 38 bảng, khóa, ràng buộc CHECK cho enum, chỉ mục
└── V2__reference_data.sql   -- 3 loại chuồng, 11 dòng bảng giá DEMO, 30 yêu cầu tuân thủ DEMO
```

Không sao chép các file này sang nơi khác để tránh lệch phiên bản. Muốn thay đổi schema: **thêm** file
`V3__mo_ta.sql` mới, không sửa file đã chạy (Flyway kiểm tra checksum, `validate-on-migrate: true`).

`spring.jpa.hibernate.ddl-auto` được đặt `none`; Hibernate không tự sinh hay sửa bảng.

## Chạy migration thủ công (không bắt buộc)

Backend tự chạy Flyway khi khởi động. Nếu muốn chạy riêng bằng `sqlcmd`:

```bash
sqlcmd -S localhost -U irtms_app -P "<mat-khau>" -C -d irtms -i backend/src/main/resources/db/migration/V1__init_schema.sql
sqlcmd -S localhost -U irtms_app -P "<mat-khau>" -C -d irtms -i backend/src/main/resources/db/migration/V2__reference_data.sql
```

(Khi đó Flyway sẽ báo bảng đã tồn tại vì không có `flyway_schema_history`; chỉ nên dùng cách này để kiểm tra nhanh trên database trống rồi xóa bằng `reset-database.sql`.)

## Trạng thái kiểm chứng

* Cú pháp T-SQL của V1/V2 được parse thành công bằng `sqlglot` (dialect `tsql`).
* **Chưa** chạy được trên SQL Server thật trong môi trường phát triển hiện tại. Hãy chạy theo README gốc trên máy có SQL Server và báo lỗi nếu có.
