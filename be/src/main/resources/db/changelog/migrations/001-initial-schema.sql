--liquibase formatted sql

--changeset sell-glass:001-create-branches
CREATE TABLE branches (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    address     TEXT,
    phone       VARCHAR(20),
    open_time   TIME,
    close_time  TIME,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE branches;

--changeset sell-glass:002-create-users
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id     UUID REFERENCES branches(id),
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'BRANCH_MANAGER', 'STAFF')),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE users;

--changeset sell-glass:003-create-customers
CREATE TABLE customers (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone         VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE customers;

--changeset sell-glass:004-create-customer-addresses
CREATE TABLE customer_addresses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id   UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    receiver_name VARCHAR(100) NOT NULL,
    phone         VARCHAR(20) NOT NULL,
    address       TEXT NOT NULL,
    is_default    BOOLEAN NOT NULL DEFAULT FALSE
);
--rollback DROP TABLE customer_addresses;

--changeset sell-glass:005-create-categories
CREATE TABLE categories (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name      VARCHAR(100) NOT NULL,
    slug      VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
--rollback DROP TABLE categories;

--changeset sell-glass:006-create-brands
CREATE TABLE brands (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name      VARCHAR(100) NOT NULL,
    slug      VARCHAR(100) NOT NULL UNIQUE,
    logo_url  VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
--rollback DROP TABLE brands;

--changeset sell-glass:007-create-products
CREATE TABLE products (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES categories(id),
    brand_id    UUID NOT NULL REFERENCES brands(id),
    name        VARCHAR(200) NOT NULL,
    slug        VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    frame_shape VARCHAR(50),
    material    VARCHAR(50),
    gender      VARCHAR(10) NOT NULL CHECK (gender IN ('MEN', 'WOMEN', 'UNISEX')),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE products;

--changeset sell-glass:008-create-product-images
CREATE TABLE product_images (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url        VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE
);
--rollback DROP TABLE product_images;

--changeset sell-glass:009-create-product-variants
CREATE TABLE product_variants (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku        VARCHAR(100) NOT NULL UNIQUE,
    color      VARCHAR(50),
    size       VARCHAR(20),
    price      NUMERIC(12, 2) NOT NULL,
    is_active  BOOLEAN NOT NULL DEFAULT TRUE
);
--rollback DROP TABLE product_variants;

--changeset sell-glass:010-create-carts
CREATE TABLE carts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    session_id  VARCHAR(100),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE carts;

--changeset sell-glass:011-create-cart-items
CREATE TABLE cart_items (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id            UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_variant_id UUID NOT NULL REFERENCES product_variants(id),
    quantity           INT NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_variant_id)
);
--rollback DROP TABLE cart_items;

--changeset sell-glass:012-create-orders
CREATE TABLE orders (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id      UUID NOT NULL REFERENCES customers(id),
    branch_id        UUID NOT NULL REFERENCES branches(id),
    order_type       VARCHAR(10) NOT NULL CHECK (order_type IN ('PICKUP', 'DELIVERY')),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'READY', 'DELIVERING', 'COMPLETED', 'CANCELLED')),
    receiver_name    VARCHAR(100),
    receiver_phone   VARCHAR(20),
    delivery_address TEXT,
    subtotal         NUMERIC(12, 2) NOT NULL,
    shipping_fee     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total            NUMERIC(12, 2) NOT NULL,
    payment_status   VARCHAR(20) NOT NULL DEFAULT 'UNPAID'
                         CHECK (payment_status IN ('UNPAID', 'PENDING_VERIFY', 'PAID')),
    payment_proof_url VARCHAR(255),
    note             TEXT,
    cancelled_reason TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE orders;

--changeset sell-glass:013-create-order-items
CREATE TABLE order_items (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id           UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_variant_id UUID REFERENCES product_variants(id) ON DELETE SET NULL,
    product_name       VARCHAR(200) NOT NULL,
    variant_sku        VARCHAR(100) NOT NULL,
    unit_price         NUMERIC(12, 2) NOT NULL,
    quantity           INT NOT NULL CHECK (quantity > 0),
    subtotal           NUMERIC(12, 2) NOT NULL
);
--rollback DROP TABLE order_items;

--changeset sell-glass:014-create-appointments
CREATE TABLE appointments (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID NOT NULL REFERENCES customers(id),
    branch_id    UUID NOT NULL REFERENCES branches(id),
    staff_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    scheduled_at TIMESTAMP NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'CONFIRMED', 'DONE', 'CANCELLED')),
    note         TEXT,
    result_note  TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
--rollback DROP TABLE appointments;

--changeset sell-glass:015-create-indexes
CREATE INDEX idx_users_branch             ON users(branch_id);
CREATE INDEX idx_products_category        ON products(category_id);
CREATE INDEX idx_products_brand           ON products(brand_id);
CREATE INDEX idx_products_active          ON products(is_active);
CREATE INDEX idx_orders_branch_status     ON orders(branch_id, status);
CREATE INDEX idx_orders_customer          ON orders(customer_id);
CREATE INDEX idx_appointments_branch_date ON appointments(branch_id, scheduled_at);
--rollback DROP INDEX idx_users_branch, idx_products_category, idx_products_brand, idx_products_active, idx_orders_branch_status, idx_orders_customer, idx_appointments_branch_date;
