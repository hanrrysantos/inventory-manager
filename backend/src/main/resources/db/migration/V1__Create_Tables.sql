CREATE TABLE tb_users (
                          id              BIGSERIAL PRIMARY KEY,
                          name            VARCHAR(100) NOT NULL,
                          email           VARCHAR(250) NOT NULL UNIQUE,
                          google_subject  VARCHAR(255) UNIQUE,
                          password        VARCHAR(250) NOT NULL,
                          role            VARCHAR(50)  NOT NULL,
                          created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tb_categories (
                               id          BIGSERIAL PRIMARY KEY,
                               name        VARCHAR(100) NOT NULL,
                               description TEXT,
                               owner_id    BIGINT       NOT NULL,

                               CONSTRAINT fk_category_owner
                                   FOREIGN KEY (owner_id) REFERENCES tb_users (id),
                               CONSTRAINT uq_category_owner_name
                                   UNIQUE (owner_id, name)
);

CREATE TABLE tb_products (
                             id          BIGSERIAL PRIMARY KEY,
                             name        VARCHAR(255) NOT NULL,
                             sku         VARCHAR(50)  NOT NULL,
                             min_stock   BIGINT       NOT NULL DEFAULT 0,
                             category_id BIGINT       NOT NULL,
                             owner_id    BIGINT       NOT NULL,

                             CONSTRAINT fk_product_category
                                 FOREIGN KEY (category_id) REFERENCES tb_categories (id),
                             CONSTRAINT fk_product_owner
                                 FOREIGN KEY (owner_id) REFERENCES tb_users (id),
                             CONSTRAINT uq_product_owner_sku
                                 UNIQUE (owner_id, sku)
);

CREATE TABLE tb_batches (
                            id                 BIGSERIAL PRIMARY KEY,
                            batch_number       VARCHAR(100)   NOT NULL UNIQUE,
                            quantity           BIGINT         NOT NULL CHECK (quantity >= 0),
                            manufacturing_date DATE           NOT NULL,
                            expiry_date        DATE           NOT NULL,
                            price              NUMERIC(38, 2) NOT NULL DEFAULT 0 CHECK (price >= 0),
                            product_id         BIGINT         NOT NULL,

                            CONSTRAINT fk_batch_product
                                FOREIGN KEY (product_id) REFERENCES tb_products (id)
);

CREATE TABLE tb_inventory_logs (
                                   id         BIGSERIAL PRIMARY KEY,
                                   type       VARCHAR(20) NOT NULL,
                                   quantity   BIGINT      NOT NULL CHECK (quantity > 0),
                                   timestamp  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   batch_id   BIGINT,
                                   product_id BIGINT      NOT NULL,

                                   CONSTRAINT fk_log_batch
                                       FOREIGN KEY (batch_id) REFERENCES tb_batches (id) ON DELETE SET NULL,
                                   CONSTRAINT fk_log_product
                                       FOREIGN KEY (product_id) REFERENCES tb_products (id)
);