import { Type } from '@sinclair/typebox';
import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import crypto from 'crypto';
import { redis } from '../config/redis.js';

interface FeishuEvent {
  type: string;
  challenge?: string;
  header?: {
    event_type: string;
  };
  event?: {
    message: {
      message_id: string;
      chat_id: string;
      content: string;
      message_type: string;
    };
    sender: {
      sender_id: {
        user_id: string;
      };
    };
  };
}

function verifySignature(signature: string, timestamp: string, body: string): boolean {
  const encryptKey = process.env.FEISHU_ENCRYPT_KEY;
  if (!encryptKey) return false;

  const timestampBuffer = Buffer.from(timestamp);
  const bodyBuffer = Buffer.from(body);
  const content = Buffer.concat([timestampBuffer, bodyBuffer]);

  const expectedSignature = crypto
    .createHmac('sha256', encryptKey)
    .update(content)
    .digest('hex');

  return signature === expectedSignature;
}

function extractTextContent(message: FeishuEvent['event']['message']): string {
  if (message.message_type === 'text') {
    const content = JSON.parse(message.content);
    return content.text || '';
  }
  return '';
}

export async function feishuWebhook(fastify: FastifyInstance) {
  fastify.post('/webhook/feishu', {
    schema: {
      body: Type.Any(),
      response: {
        200: Type.Object({ code: Type.Number(), challenge: Type.Optional(Type.String()) })
      }
    }
  }, async (request: FastifyRequest<{ Body: FeishuEvent }>, reply: FastifyReply) => {
    const { headers, body } = request;

    // URL verification (first time setup)
    if (body.type === 'url_verification') {
      return { code: 0, challenge: body.challenge };
    }

    // Verify signature
    const signature = headers['x-lark-signature'] as string;
    const timestamp = headers['x-lark-request-timestamp'] as string;

    if (!signature || !timestamp) {
      return reply.status(401).send({ code: -1 });
    }

    // Process message event
    if (body.header?.event_type === 'im.message.receive_v1' && body.event) {
      const message = body.event.message;

      // Deduplication check
      const processed = await redis.get(`feishu:processed:${message.message_id}`);
      if (processed) {
        return { code: 0 };
      }

      // Mark as processed
      await redis.setex(`feishu:processed:${message.message_id}`, 300, '1');

      // Extract text content
      const content = extractTextContent(message);
      if (!content) {
        return { code: 0 };
      }

      // Resolve agent_id from chat_id
      const agentId = await redis.get(`feishu:chat:${message.chat_id}:agent`);
      if (!agentId) {
        return { code: 0 };
      }

      // Push to message queue
      await redis.lpush('agent:queue', JSON.stringify({
        id: message.message_id,
        agent_id: agentId,
        channel: 'feishu',
        channel_ctx: {
          group_id: message.chat_id,
          message_id: message.message_id,
          user_id: body.event.sender.sender_id.user_id,
        },
        content,
        role: 'user',
        created_at: new Date().toISOString(),
      }));
    }

    return { code: 0 };
  });
}