-- CreateTable
CREATE TABLE "agents" (
    "id" UUID NOT NULL,
    "tenant_id" UUID NOT NULL,
    "name" VARCHAR(100) NOT NULL,
    "description" TEXT,
    "status" VARCHAR(20) NOT NULL DEFAULT 'defined',
    "system_prompt" TEXT NOT NULL,
    "tools" JSONB NOT NULL DEFAULT '[]',
    "channels" JSONB NOT NULL DEFAULT '[]',
    "lifecycle" JSONB NOT NULL DEFAULT '{}',
    "model" VARCHAR(50) NOT NULL DEFAULT 'claude-sonnet-4-6',
    "version" INTEGER NOT NULL DEFAULT 1,
    "parent_id" UUID,
    "owner_id" UUID NOT NULL,
    "trigger_count" INTEGER NOT NULL DEFAULT 0,
    "last_triggered_at" TIMESTAMPTZ,
    "expire_notified" BOOLEAN NOT NULL DEFAULT false,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "expires_at" TIMESTAMPTZ,
    "archived_at" TIMESTAMPTZ,

    CONSTRAINT "agents_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "agent_memory" (
    "id" UUID NOT NULL,
    "agent_id" UUID NOT NULL,
    "type" VARCHAR(20) NOT NULL,
    "content" TEXT NOT NULL,
    "embedding" BYTEA,
    "source" VARCHAR(50),
    "importance" DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    "access_count" INTEGER NOT NULL DEFAULT 0,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "expires_at" TIMESTAMPTZ,

    CONSTRAINT "agent_memory_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "execution_logs" (
    "id" UUID NOT NULL,
    "agent_id" UUID NOT NULL,
    "session_id" UUID,
    "channel" VARCHAR(50),
    "trigger_type" VARCHAR(50),
    "input" TEXT,
    "output" TEXT,
    "tools_used" JSONB NOT NULL DEFAULT '[]',
    "quality_score" DOUBLE PRECISION,
    "tokens_used" INTEGER,
    "latency_ms" INTEGER,
    "user_feedback" INTEGER NOT NULL DEFAULT 0,
    "error" TEXT,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "execution_logs_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "session_history" (
    "id" UUID NOT NULL,
    "session_id" UUID NOT NULL,
    "agent_id" UUID NOT NULL,
    "role" VARCHAR(20) NOT NULL,
    "content" TEXT NOT NULL,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "session_history_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "agents_tenant_id_idx" ON "agents"("tenant_id");

-- CreateIndex
CREATE INDEX "agents_status_idx" ON "agents"("status");

-- CreateIndex
CREATE INDEX "agents_owner_id_idx" ON "agents"("owner_id");

-- CreateIndex
CREATE INDEX "agent_memory_agent_id_idx" ON "agent_memory"("agent_id");

-- CreateIndex
CREATE INDEX "agent_memory_type_idx" ON "agent_memory"("type");

-- CreateIndex
CREATE INDEX "execution_logs_agent_id_idx" ON "execution_logs"("agent_id");

-- CreateIndex
CREATE INDEX "execution_logs_session_id_idx" ON "execution_logs"("session_id");

-- CreateIndex
CREATE INDEX "execution_logs_created_at_idx" ON "execution_logs"("created_at");

-- CreateIndex
CREATE INDEX "session_history_session_id_idx" ON "session_history"("session_id");

-- CreateIndex
CREATE INDEX "session_history_agent_id_idx" ON "session_history"("agent_id");

-- AddForeignKey
ALTER TABLE "agents" ADD CONSTRAINT "agents_parent_id_fkey" FOREIGN KEY ("parent_id") REFERENCES "agents"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "agent_memory" ADD CONSTRAINT "agent_memory_agent_id_fkey" FOREIGN KEY ("agent_id") REFERENCES "agents"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "execution_logs" ADD CONSTRAINT "execution_logs_agent_id_fkey" FOREIGN KEY ("agent_id") REFERENCES "agents"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "session_history" ADD CONSTRAINT "session_history_agent_id_fkey" FOREIGN KEY ("agent_id") REFERENCES "agents"("id") ON DELETE CASCADE ON UPDATE CASCADE;
