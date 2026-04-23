--liquibase formatted sql

--changeset sell-glass:004-vouchers
CREATE TABLE vouchers (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(50)  NOT NULL UNIQUE,
    type             VARCHAR(20)  NOT NULL,
    value            NUMERIC(12, 2) NOT NULL,
    max_discount_amount NUMERIC(12, 2),
    min_order_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    usage_limit      INT,
    times_used       INT          NOT NULL DEFAULT 0,
    expires_at       TIMESTAMP,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

ALTER TABLE orders ADD COLUMN voucher_code     VARCHAR(50);
ALTER TABLE orders ADD COLUMN discount_amount  NUMERIC(12, 2) NOT NULL DEFAULT 0;

--rollback DROP TABLE vouchers;
--rollback ALTER TABLE orders DROP COLUMN voucher_code;
--rollback ALTER TABLE orders DROP COLUMN discount_amount;
