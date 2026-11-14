-- user4: lịch nghỉ phép của tài xế / áp tải
CREATE TABLE staff_leave (
    id BIGINT IDENTITY(1,1) NOT NULL,
    staff_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason NVARCHAR(300) NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT df_sl_status DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_sl_created DEFAULT SYSDATETIME(),
    CONSTRAINT pk_staff_leave PRIMARY KEY (id),
    CONSTRAINT fk_sl_staff FOREIGN KEY (staff_id) REFERENCES app_user (id),
    CONSTRAINT fk_sl_created_by FOREIGN KEY (created_by) REFERENCES app_user (id),
    CONSTRAINT ck_sl_dates CHECK (end_date >= start_date)
);
CREATE INDEX ix_sl_staff ON staff_leave (staff_id, start_date, end_date);
