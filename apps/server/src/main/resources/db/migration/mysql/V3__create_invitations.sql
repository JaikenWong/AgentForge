CREATE TABLE IF NOT EXISTS invitations (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(32) UNIQUE NOT NULL,
    tenant_id   BIGINT NOT NULL,
    created_by  BIGINT NOT NULL,
    used_by     BIGINT,
    expires_at  TIMESTAMP,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (used_by) REFERENCES users(id)
);