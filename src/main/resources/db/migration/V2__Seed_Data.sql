-- Inserindo Categorias
INSERT INTO category (name, description) VALUES
                                             ('Suplementos', 'Proteínas, creatinas e aminoácidos'),
                                             ('Acessórios', 'Coqueteleiras e luvas');

-- Inserindo Produtos (O Whey tem estoque mínimo de 10)
INSERT INTO product (name, sku, min_stock, category_id) VALUES
                                                            ('Whey Protein 1kg', 'WHEY-001', 10, 1),
                                                            ('Creatina 300g', 'CREA-002', 5, 1);

-- Inserindo Lotes Iniciais
-- Whey: 15 unidades (Acima do mínimo de 10)
INSERT INTO batch (batch_number, quantity, manufacturing_date, expiry_date, product_id) VALUES
    ('LOT-WHEY-001', 15, '2026-01-01', '2027-01-01', 1);

-- Creatina: 4 unidades (Já nasce abaixo do mínimo de 5)
INSERT INTO batch (batch_number, quantity, manufacturing_date, expiry_date, product_id) VALUES
    ('LOT-CREA-002', 4, '2026-02-15', '2027-02-15', 2);

-- Registrando os Logs de Entrada (INPUT) para manter o histórico limpo
INSERT INTO inventory_log (type, quantity, batch_id, product_id) VALUES
                                                                     ('INPUT', 15, 1, 1),
                                                                     ('INPUT', 4, 2, 2);