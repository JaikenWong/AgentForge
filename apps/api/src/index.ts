import 'dotenv/config';
import Fastify from 'fastify';
import cors from '@fastify/cors';
import helmet from '@fastify/helmet';
import { agentRoutes } from './modules/agent/routes.js';
import { intentRoutes } from './modules/intent/routes.js';
import { generatorRoutes } from './modules/generator/routes.js';
import { logRoutes } from './modules/log/routes.js';
import { feishuWebhook } from './webhook/feishu.js';

const fastify = Fastify({
  logger: {
    transport: {
      target: 'pino-pretty',
      options: { colorize: true }
    }
  }
});

await fastify.register(helmet);
await fastify.register(cors, { origin: true });

// Health check
fastify.get('/health', async () => ({ status: 'ok', timestamp: new Date().toISOString() }));

// Routes
await fastify.register(agentRoutes);
await fastify.register(intentRoutes);
await fastify.register(generatorRoutes);
await fastify.register(logRoutes);

// Webhooks
await fastify.register(feishuWebhook);

const start = async () => {
  try {
    const port = Number(process.env.PORT) || 3000;
    await fastify.listen({ port, host: '0.0.0.0' });
    console.log(`Server running on http://localhost:${port}`);
  } catch (err) {
    fastify.log.error(err);
    process.exit(1);
  }
};

start();