--liquibase formatted sql

--changeset sell-glass:005-reviews
CREATE TABLE reviews (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID         NOT NULL REFERENCES customers(id),
    product_id  UUID         NOT NULL REFERENCES products(id),
    rating      SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_customer_product UNIQUE (customer_id, product_id)
);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);

--rollback DROP TABLE reviews;
