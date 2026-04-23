--liquibase formatted sql

--changeset sell-glass:003-stock-and-prescription
ALTER TABLE product_variants ADD COLUMN stock INT NOT NULL DEFAULT 0;

ALTER TABLE orders ADD COLUMN prescription_od_sph  NUMERIC(5, 2);
ALTER TABLE orders ADD COLUMN prescription_od_cyl  NUMERIC(5, 2);
ALTER TABLE orders ADD COLUMN prescription_od_axis SMALLINT;
ALTER TABLE orders ADD COLUMN prescription_os_sph  NUMERIC(5, 2);
ALTER TABLE orders ADD COLUMN prescription_os_cyl  NUMERIC(5, 2);
ALTER TABLE orders ADD COLUMN prescription_os_axis SMALLINT;
ALTER TABLE orders ADD COLUMN prescription_pd      NUMERIC(4, 1);
ALTER TABLE orders ADD COLUMN prescription_note    TEXT;

--rollback ALTER TABLE product_variants DROP COLUMN stock;
--rollback ALTER TABLE orders DROP COLUMN prescription_od_sph;
--rollback ALTER TABLE orders DROP COLUMN prescription_od_cyl;
--rollback ALTER TABLE orders DROP COLUMN prescription_od_axis;
--rollback ALTER TABLE orders DROP COLUMN prescription_os_sph;
--rollback ALTER TABLE orders DROP COLUMN prescription_os_cyl;
--rollback ALTER TABLE orders DROP COLUMN prescription_os_axis;
--rollback ALTER TABLE orders DROP COLUMN prescription_pd;
--rollback ALTER TABLE orders DROP COLUMN prescription_note;
