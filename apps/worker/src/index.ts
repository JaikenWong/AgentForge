import 'dotenv/config';
import { Worker, Queue } from 'bullmq';
import Redis from 'ioredis';
import Anthropic from '@anthropic-ai/sdk';
import { PrismaClient } from '@prisma/client';
import { pino } from 'pino';

const logger = pino({
  transport: { target: 'pino-pretty', options: { colorize: true } }
});

const redis = new Redis(process.env.REDIS_URL || 'redis://localhost:6379', {
  maxRetriesPerRequest: null,
});
const prisma = new PrismaClient();
const anthropic = new Anthropic({
  apiKey: process.env.LLM_API_KEY,
  baseURL: process.env.LLM_BASE_URL,
});

interface MessageJob {
  id: string;
  agent_id: string;
  channel: string;
  channel_ctx: {
    group_id?: string;
    message_id?: string;
    user_id?: string;
    session_id?: string;
  };
  content: string;
  role: string;
  created_at: string;
}

async function loadAgent(agentId: string) {
  const cached = await redis.get(`agent:config:${agentId}`);
  if (cached) return JSON.parse(cached);

  const agent = await prisma.agent.findUnique({ where: { id: agentId } });
  if (agent) {
    await redis.setex(`agent:config:${agentId}`, 600, JSON.stringify(agent));
  }
  return agent;
}

async function updateStatus(agentId: string, status: string) {
  await prisma.agent.update({ where: { id: agentId }, data: { status } });
  await redis.del(`agent:config:${agentId}`);
}

async function resetIdleTimer(agentId: string) {
  await redis.setex(`agent:idle-watch:${agentId}`, 24 * 60 * 60, agentId);
}

async function getSessionHistory(sessionId: string | undefined) {
  if (!sessionId) return [];
  const history = await prisma.sessionHistory.findMany({
    where: { sessionId },
    orderBy: { createdAt: 'asc' },
    take: 20,
  });
  return history.map(h => ({ role: h.role as 'user' | 'assistant', content: h.content }));
}

async function processMessage(job: { data: MessageJob }) {
  const { agent_id, content, channel, channel_ctx } = job.data;
  const startTime = Date.now();

  try {
    const agent = await loadAgent(agent_id);
    if (!agent || agent.status === 'archived') {
      logger.info({ agent_id }, 'Agent not found or archived, skipping');
      return;
    }

    await updateStatus(agent_id, 'running');
    await resetIdleTimer(agent_id);

    const history = await getSessionHistory(channel_ctx.session_id);

    const response = await anthropic.messages.create({
      model: agent.model,
      system: agent.systemPrompt,
      messages: [...history, { role: 'user', content }],
      max_tokens: 1000,
    });

    const output = response.content[0].type === 'text' ? response.content[0].text : '';
    const tokensUsed = response.usage.input_tokens + response.usage.output_tokens;
    const latencyMs = Date.now() - startTime;

    // Log execution
    await prisma.executionLog.create({
      data: {
        agentId: agent_id,
        sessionId: channel_ctx.session_id,
        channel,
        input: content,
        output,
        tokensUsed,
        latencyMs,
      }
    });

    // Update session history
    if (channel_ctx.session_id) {
      await prisma.sessionHistory.createMany({
        data: [
          { sessionId: channel_ctx.session_id, agentId: agent_id, role: 'user', content },
          { sessionId: channel_ctx.session_id, agentId: agent_id, role: 'assistant', content: output },
        ]
      });
    }

    // Update agent stats
    await prisma.agent.update({
      where: { id: agent_id },
      data: {
        triggerCount: { increment: 1 },
        lastTriggeredAt: new Date(),
      }
    });

    // Send reply (for feishu, push to reply queue)
    if (channel === 'feishu' && channel_ctx.group_id) {
      await redis.lpush('feishu:reply', JSON.stringify({
        chat_id: channel_ctx.group_id,
        message_id: channel_ctx.message_id,
        content: output,
      }));
    }

    logger.info({ agent_id, latencyMs, tokensUsed }, 'Message processed');

  } catch (error) {
    logger.error({ agent_id, error }, 'Error processing message');
    await prisma.executionLog.create({
      data: {
        agentId: agent_id,
        channel,
        input: content,
        error: String(error),
      }
    });
  } finally {
    await updateStatus(agent_id, 'active');
  }
}

const worker = new Worker<MessageJob>('agent-messages', processMessage, {
  connection: redis,
  concurrency: Number(process.env.WORKER_CONCURRENCY) || 5,
});

worker.on('completed', (job) => {
  logger.info({ jobId: job.id }, 'Job completed');
});

worker.on('failed', (job, err) => {
  logger.error({ jobId: job?.id, err }, 'Job failed');
});

logger.info('Worker started, waiting for messages...');

// Subscribe to Redis expiry events for state machine
const subscriber = redis.duplicate();
subscriber.subscribe('__keyevent@0__:expired');

subscriber.on('message', async (channel, key) => {
  if (key.startsWith('agent:idle-watch:')) {
    const agentId = key.replace('agent:idle-watch:', '');
    await updateStatus(agentId, 'idle');
    logger.info({ agentId }, 'Agent moved to idle');
  }

  if (key.startsWith('agent:expire-watch:')) {
    const agentId = key.replace('agent:expire-watch:', '');
    await prisma.agent.update({
      where: { id: agentId },
      data: { status: 'archived', archivedAt: new Date() }
    });
    await redis.del(`agent:config:${agentId}`);
    logger.info({ agentId }, 'Agent archived');
  }
});