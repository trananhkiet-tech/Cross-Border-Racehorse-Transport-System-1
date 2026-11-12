-- user5: chi phí phát sinh trên đường và hoàn ứng
CREATE TABLE trip_expense (
    id BIGINT IDENTITY(1,1) NOT NULL,
    trip_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    category NVARCHAR(20) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    currency NVARCHAR(3) NOT NULL CONSTRAINT df_te_cur DEFAULT 'USD',
    description NVARCHAR(500) NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT df_te_status DEFAULT 'PENDING',
    review_note NVARCHAR(500) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME2(3) NULL,
    created_at DATETIME2(3) NOT NULL CONSTRAINT df_te_created DEFAULT SYSDATETIME(),
    CONSTRAINT pk_trip_expense PRIMARY KEY (id),
    CONSTRAINT fk_te_trip FOREIGN KEY (trip_id) REFERENCES transportation_trip (id),
    CONSTRAINT fk_te_staff FOREIGN KEY (staff_id) REFERENCES app_user (id),
    CONSTRAINT fk_te_reviewer FOREIGN KEY (reviewed_by) REFERENCES app_user (id),
    CONSTRAINT ck_te_amount CHECK (amount > 0)
);
CREATE INDEX ix_te_trip ON trip_expense (trip_id, status);
