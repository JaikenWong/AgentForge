CREATE TABLE IF NOT EXISTS agents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    status          VARCHAR(20) DEFAULT 'defined',
    system_prompt   TEXT NOT NULL,
    tools           JSONB DEFAULT '[]',
    channels        JSONB DEFAULT '[]',
    lifecycle       JSONB DEFAULT '{}',
    model           VARCHAR(50) DEFAULT 'astron-code-latest',
    version         INT DEFAULT 1,
    parent_id       UUID REFERENCES agents(id),
    owner_id        UUID NOT NULL,
    trigger_count   INT DEFAULT 0,
    last_triggered_at TIMESTAMPTZ,
    expire_notified BOOLEAN DEFAULT false,
    created_at      TIMESTAMPTZ DEFAULT now(),
    expires_at      TIMESTAMPTZ,
    archived_at     TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_agents_tenant ON agents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agents_status ON agents(status);
CREATE INDEX IF NOT EXISTS idx_agents_owner ON agents(owner_id);