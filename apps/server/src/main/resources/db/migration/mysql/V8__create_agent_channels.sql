CREATE TABLE IF NOT EXISTS agent_channels (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id   BIGINT NOT NULL,
    tenant_id  BIGINT NOT NULL,
    channel    VARCHAR(32) NOT NULL,
    chat_id    VARCHAR(128) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_channel_chat (channel, chat_id),
    FOREIGN KEY (agent_id) REFERENCES agents(id)
);
