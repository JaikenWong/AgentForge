export type AgentStatus = 'defined' | 'active' | 'running' | 'idle' | 'archived';

export interface Agent {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  status: AgentStatus;
  systemPrompt: string;
  tools: string[];
  channels: AgentChannel[];
  lifecycle: AgentLifecycle;
  model: string;
  version: number;
  parentId?: string;
  ownerId: string;
  triggerCount: number;
  lastTriggeredAt?: Date;
  createdAt: Date;
  expiresAt?: Date;
  archivedAt?: Date;
}

export interface AgentChannel {
  type: 'feishu' | 'website' | 'webhook';
  group_id?: string;
  trigger?: 'mention' | 'all' | 'mention_or_question';
}

export interface AgentLifecycle {
  trigger: 'message' | 'cron' | 'webhook';
  idle_timeout?: string;
  auto_archive?: boolean;
  notify_before_expire?: string;
}

export interface UnifiedMessage {
  id: string;
  agent_id: string;
  channel: 'feishu' | 'website' | 'webhook';
  channel_ctx: {
    group_id?: string;
    message_id?: string;
    session_id?: string;
    user_id?: string;
    user_name?: string;
  };
  content: string;
  role: 'user' | 'assistant';
  created_at: Date;
}