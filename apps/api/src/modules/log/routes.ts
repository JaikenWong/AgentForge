import { Type } from '@sinclair/typebox';
import { FastifyInstance } from 'fastify';
import { prisma } from '../../db/client.js';

export async function logRoutes(fastify: FastifyInstance) {
  // List execution logs
  fastify.get('/api/logs', {
    schema: {
      querystring: Type.Object({
        agentId: Type.Optional(Type.String()),
        limit: Type.Optional(Type.Number()),
      }),
      response: {
        200: Type.Array(Type.Object({
          id: Type.String(),
          agentId: Type.String(),
          sessionId: Type.Optional(Type.String()),
          channel: Type.Optional(Type.String()),
          input: Type.Optional(Type.String()),
          output: Type.Optional(Type.String()),
          tokensUsed: Type.Optional(Type.Number()),
          latencyMs: Type.Optional(Type.Number()),
          createdAt: Type.String(),
        }))
      }
    }
  }, async (request) => {
    const { agentId, limit = 50 } = request.query as { agentId?: string; limit?: number };

    return prisma.executionLog.findMany({
      where: agentId ? { agentId } : undefined,
      orderBy: { createdAt: 'desc' },
      take: limit,
    });
  });

  // Get logs for specific agent
  fastify.get('/api/agents/:id/logs', {
    schema: {
      params: Type.Object({ id: Type.String() }),
      response: {
        200: Type.Array(Type.Object({
          id: Type.String(),
          agentId: Type.String(),
          sessionId: Type.Optional(Type.String()),
          channel: Type.Optional(Type.String()),
          input: Type.Optional(Type.String()),
          output: Type.Optional(Type.String()),
          tokensUsed: Type.Optional(Type.Number()),
          latencyMs: Type.Optional(Type.Number()),
          createdAt: Type.String(),
        }))
      }
    }
  }, async (request) => {
    const { id } = request.params as { id: string };
    return prisma.executionLog.findMany({
      where: { agentId: id },
      orderBy: { createdAt: 'desc' },
      take: 100,
    });
  });
}