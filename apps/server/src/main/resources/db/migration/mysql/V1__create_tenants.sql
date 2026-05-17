CREATE TABLE IF NOT EXISTS tenants (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    plan        VARCHAR(20) DEFAULT 'free',
    max_agents  INT DEFAULT 10,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tenants (id, name, plan, max_agents) VALUES (1, 'Default', 'free', 10);