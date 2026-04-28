INSERT INTO tb_users
    (id, name, email, password, role, active, created_at, updated_at)
VALUES 
    (
        gen_random_uuid(),
        'Bruno Mota',
        'bruno7motadev@gmail.com',
        '$2a$10$uclfnNOuJRsH8y4G51CPdeNAXjKXLiQFK7eERYW0bTH95cRq6r8o2',
        'ROLE_ADMIN',
        true,
        now(),
        now()
    );