INSERT INTO tb_categories (name, description) VALUES
('Mercearia Seca', 'Arroz, feijão, massas, óleos e farináceos'),
('Laticínios e Frios', 'Leite, queijos, iogurtes e manteigas'),
('Bebidas', 'Sucos, refrigerantes, águas e chás'),
('Higiene Pessoal', 'Shampoos, sabonetes, cremes dentais e desodorantes'),
('Limpeza', 'Detergentes, amaciantes, sabão em pó e desinfetantes');

INSERT INTO tb_products (name, sku, min_stock, category_id) VALUES
('Arroz Agulhinha 5kg', 'ARR-001', 10, 1),
('Feijão Carioca 1kg', 'FEI-001', 15, 1),
('Macarrão Espaguete 500g', 'MAC-001', 20, 1),
('Óleo de Soja 900ml', 'OLE-001', 12, 1),
('Leite Integral UHT 1L', 'LEI-001', 24, 2),
('Iogurte Morango 800g', 'IOG-001', 8, 2),
('Manteiga com Sal 200g', 'MAN-001', 10, 2),
('Suco de Laranja 1L', 'SUC-001', 10, 3),
('Água Mineral 500ml', 'AGU-001', 50, 3),
('Sabonete Barra 90g', 'SAB-001', 30, 4),
('Creme Dental 90g', 'DEN-001', 15, 4),
('Detergente Neutro 500ml', 'DET-001', 20, 5),
('Amaciante Blue 2L', 'AMA-001', 5, 5);

INSERT INTO tb_batches (batch_number, quantity, manufacturing_date, expiry_date, price, product_id) VALUES
('LEITE-JUN-001', 100, '2026-03-01', '2026-07-01', 5.40, 5),
('LEITE-ABR-002', 50, '2026-02-15', '2026-04-10', 5.10, 5),
('ARROZ-JAN-27', 40, '2026-01-10', '2027-01-10', 28.90, 1),
('IOG-AGO-22', 12, '2026-01-01', '2026-08-01', 12.50, 6),
('SUCO-JUN-27', 20, '2026-02-01', '2026-07-15', 14.00, 8),
('SAB-JUL-28', 60, '2026-01-01', '2028-07-01', 2.50, 10),
('AMA-AGO-27', 15, '2026-02-01', '2027-08-01', 18.50, 13);
('FEIJAO-AGO-28', 12, '2026-01-01', '2028-08-01', 10.50, 2),
('MAC-JUN-27', 21, '2026-02-01', '2027-07-15', 6.99, 3,
('OLEO-JUL-28', 60, '2026-01-01', '2028-07-01', 5.00, 4),
('MANT-AGO-27', 15, '2026-02-01', '2027-08-01', 4.50, 7);
('AGUA-AGO-27', 42, '2023-02-01', '2027-08-01', 4.50, 9);
('CREM-AGO-29', 18, '2023-02-01', '2029-08-01', 4.50, 11);
('DET-AGO-29', 15, '2023-02-01', '2029-08-01', 4.50, 12);

INSERT INTO tb_inventory_logs (type, quantity, product_id, batch_id) VALUES
('ENTRY', 40, 1, 3),
('ENTRY', 20, 8, 5);

UPDATE tb_batches SET quantity = 2 WHERE batch_number = 'SUCO-CRITICO';