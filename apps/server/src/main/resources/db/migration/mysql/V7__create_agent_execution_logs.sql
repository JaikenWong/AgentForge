CREATE TABLE IF NOT EXISTS agent_execution_logs (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id      BIGINT NOT NULL,
    user_id       BIGINT,
    tenant_id     BIGINT NOT NULL,
    channel       VARCHAR(32) NOT NULL DEFAULT 'web',
    input_text    TEXT,
    output_text   TEXT,
    status        VARCHAR(20) NOT NULL,
    latency_ms    INT,
    error_message TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_created (agent_id, created_at DESC),
    FOREIGN KEY (agent_id) REFERENCES agents(id)
);
