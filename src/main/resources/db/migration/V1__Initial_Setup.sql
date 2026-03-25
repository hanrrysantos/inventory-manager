-- 1. Tabela de Categorias
CREATE TABLE category (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(100) NOT NULL UNIQUE,
                          description TEXT
);

-- 2. Tabela de Produtos
CREATE TABLE product (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         sku VARCHAR(50) NOT NULL UNIQUE,
                         min_stock BIGINT NOT NULL DEFAULT 0,
                         category_id BIGINT NOT NULL,
                         CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE batch (
                       id BIGSERIAL PRIMARY KEY,
                       batch_number VARCHAR(100) NOT NULL UNIQUE, -- Certifique-se de que é VARCHAR
                       quantity BIGINT NOT NULL CHECK (quantity >= 0),
                       manufacturing_date DATE,
                       expiry_date DATE,
                       price NUMERIC(38,2) NOT NULL DEFAULT 0, -- Coluna incluída aqui
                       product_id BIGINT NOT NULL,
                       CONSTRAINT fk_batch_product FOREIGN KEY (product_id) REFERENCES product(id)
);

-- 4. Tabela de Logs de Inventário (Auditoria)
CREATE TABLE inventory_log (
                               id BIGSERIAL PRIMARY KEY,
                               type VARCHAR(20) NOT NULL, -- INPUT, OUTPUT, LOSS
                               quantity BIGINT NOT NULL,
                               timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               batch_id BIGINT,
                               product_id BIGINT NOT NULL,
                               CONSTRAINT fk_log_batch FOREIGN KEY (batch_id) REFERENCES batch(id) ON DELETE SET NULL,
                               CONSTRAINT fk_log_product FOREIGN KEY (product_id) REFERENCES product(id)
);