DROP INDEX IF EXISTS uq_category_owner_name_ci;
DROP INDEX IF EXISTS uq_product_owner_sku_ci;

CREATE UNIQUE INDEX uq_category_owner_name_ci
    ON tb_categories (owner_id, LOWER(name));

CREATE UNIQUE INDEX uq_product_owner_sku_ci
    ON tb_products (owner_id, LOWER(sku));
