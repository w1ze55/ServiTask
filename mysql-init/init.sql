CREATE DATABASE IF NOT EXISTS servitask;

USE servitask;

ALTER DATABASE servitask CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'servitask_user'@'%' IDENTIFIED BY 'servitask_password';
GRANT ALL PRIVILEGES ON servitask.* TO 'servitask_user'@'%';
FLUSH PRIVILEGES;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    photo LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Adicionar coluna photo se ela não existir (para bancos existentes)
ALTER TABLE users ADD COLUMN IF NOT EXISTS photo LONGTEXT;

SELECT 'Database servitask created successfully!' as message;
SHOW DATABASES LIKE 'servitask'; 