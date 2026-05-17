CREATE TABLE IF NOT EXISTS users (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    username       VARCHAR(50) UNIQUE,
    password_hash  VARCHAR(255),
    display_name   VARCHAR(100),
    email          VARCHAR(255),
    avatar_url     VARCHAR(500),
    oauth_provider VARCHAR(20),
    oauth_id       VARCHAR(100),
    role           VARCHAR(20) DEFAULT 'user',
    tenant_id      BIGINT DEFAULT 1,
    status         VARCHAR(20) DEFAULT 'active',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- 默认管理员
INSERT INTO users (username, password_hash, display_name, role, tenant_id)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', 'admin', 1);