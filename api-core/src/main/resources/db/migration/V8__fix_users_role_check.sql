ALTER TABLE tb_users
    DROP CONSTRAINT tb_users_role_check;

ALTER TABLE tb_users
    ADD CONSTRAINT tb_users_role_check
    CHECK (role IN ('ROLE_ADMIN', 'ROLE_USER', 'ROLE_VIEWER'));

ALTER TABLE tb_users
    ALTER COLUMN role SET DEFAULT 'ROLE_USER';