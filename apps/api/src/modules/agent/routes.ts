import { Type, Static } from '@sinclair/typebox';
import { FastifyInstance } from 'fastify';
import { prisma } from '../../db/client.js';
import { redis } from '../../config/redis.js';

const AgentSchema = Type.Object({
  id: Type.String(),
  name: Type.String(),
  description: Type.Optional(Type.String()),
  status: Type.String(),
  systemPrompt: Type.String(),
  tools: Type.Array(Type.String()),
  channels: Type.Array(Type.Any()),
  lifecycle: Type.Any(),
  model: Type.String(),
  createdAt: Type.String(),
});

const CreateAgentSchema = Type.Object({
  name: Type.String({ maxLength: 100 }),
  description: Type.Optional(Type.String()),
  systemPrompt: Type.String(),
  tools: Type.Optional(Type.Array(Type.String())),
  channels: Type.Optional(Type.Array(Type.Any())),
  lifecycle: Type.Optional(Type.Any()),
  tenantId: Type.String(),
  ownerId: Type.String(),
});

type CreateAgentInput = Static<typeof CreateAgentSchema>;

export async function agentRoutes(fastify: FastifyInstance) {
  // List agents
  fastify.get('/api/agents', {
    schema: {
      response: { 200: Type.Array(AgentSchema) }
    }
  }, async () => {
    return prisma.agent.findMany({
      orderBy: { createdAt: 'desc' },
      select: {
        id: true, name: true, description: true, status: true,
        systemPrompt: true, tools: true, channels: true,
        lifecycle: true, model: true, createdAt: true
      }
    });
  });

  // Get agent by id
  fastify.get('/api/agents/:id', {
    schema: {
      params: Type.Object({ id: Type.String() }),
      response: { 200: AgentSchema }
    }
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const agent = await prisma.agent.findUnique({ where: { id } });
    if (!agent) return reply.status(404).send({ error: 'Agent not found' });
    return agent;
  });

  // Create agent
  fastify.post('/api/agents', {
    schema: {
      body: CreateAgentSchema,
      response: { 201: AgentSchema }
    }
  }, async (request, reply) => {
    const data = request.body as CreateAgentInput;
    const agent = await prisma.agent.create({
      data: {
        tenantId: data.tenantId,
        ownerId: data.ownerId,
        name: data.name,
        description: data.description,
        systemPrompt: data.systemPrompt,
        tools: data.tools || [],
        channels: data.channels || [],
        lifecycle: data.lifecycle || {},
      }
    });
    return reply.status(201).send(agent);
  });

  // Update agent
  fastify.put('/api/agents/:id', {
    schema: {
      params: Type.Object({ id: Type.String() }),
      body: Type.Partial(CreateAgentSchema),
      response: { 200: AgentSchema }
    }
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const data = request.body as Partial<CreateAgentInput>;
    const agent = await prisma.agent.update({ where: { id }, data });
    await redis.del(`agent:config:${id}`);
    return agent;
  });

  // Deploy agent (defined -> active)
  fastify.post('/api/agents/:id/deploy', {
    schema: {
      params: Type.Object({ id: Type.String() }),
      response: { 200: Type.Object({ id: Type.String(), status: Type.String() }) }
    }
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const agent = await prisma.agent.findUnique({ where: { id } });
    if (!agent) return reply.status(404).send({ error: 'Agent not found' });
    if (agent.status !== 'defined') return reply.status(400).send({ error: 'Agent must be in defined status' });

    await prisma.agent.update({ where: { id }, data: { status: 'active' } });
    await redis.del(`agent:config:${id}`);

    // Set idle timer
    const idleSeconds = 24 * 60 * 60;
    await redis.setex(`agent:idle-watch:${id}`, idleSeconds, id);

    return { id, status: 'active' };
  });

  // Archive agent
  fastify.post('/api/agents/:id/archive', {
    schema: {
      params: Type.Object({ id: Type.String() }),
      response: { 200: Type.Object({ id: Type.String(), status: Type.String() }) }
    }
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    await prisma.agent.update({
      where: { id },
      data: { status: 'archived', archivedAt: new Date() }
    });
    await redis.del(`agent:config:${id}`);
    await redis.del(`agent:idle-watch:${id}`);
    return { id, status: 'archived' };
  });

  // Revive agent (archived -> active)
  fastify.post('/api/agents/:id/revive', {
    schema: {
      params: Type.Object({ id: Type.String() }),
      response: { 200: Type.Object({ id: Type.String(), status: Type.String() }) }
    }
  }, async (request, reply) => {
    const { id } = request.params as { id: string };
    const agent = await prisma.agent.findUnique({ where: { id } });
    if (!agent) return reply.status(404).send({ error: 'Agent not found' });
    if (agent.status !== 'archived') return reply.status(400).send({ error: 'Agent must be archived' });

    await prisma.agent.update({
      where: { id },
      data: { status: 'active', archivedAt: null }
    });
    await redis.del(`agent:config:${id}`);
    await redis.setex(`agent:idle-watch:${id}`, 24 * 60 * 60, id);

    return { id, status: 'active' };
  });
}