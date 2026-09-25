INSERT INTO tb_users (id, name, email, password, role, created_at)
VALUES (
           1,
           'Hanrry',
           'hanrry@email.com',
           '$2y$10$AFQWTRZsXPqDnUG.blACT.8HKHJLuXebJGoArXuSUy4mlxLpkc4a.',
           'ADMIN',
           CURRENT_TIMESTAMP
       );

SELECT setval('tb_users_id_seq', (SELECT MAX(id) FROM tb_users));