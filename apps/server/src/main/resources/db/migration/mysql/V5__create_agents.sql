CREATE TABLE IF NOT EXISTS agents (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid          VARCHAR(36) UNIQUE NOT NULL,
    user_id       BIGINT NOT NULL,
    tenant_id     BIGINT NOT NULL,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    prompt        TEXT,
    config        JSON,
    status        VARCHAR(20) DEFAULT 'draft',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);
