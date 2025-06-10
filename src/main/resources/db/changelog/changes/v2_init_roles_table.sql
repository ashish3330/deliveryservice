CREATE TABLE IF NOT EXISTS roles (
    role_id BIGINT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(255)
);

INSERT INTO roles (role_id, role_name, description) VALUES
    (1, 'ROLE_ADMIN', 'Administrator role with full access'),
    (2, 'ROLE_USER', 'Standard user role for customers'),
    (3, 'ROLE_VENDOR', 'Vendor role for managing products');
