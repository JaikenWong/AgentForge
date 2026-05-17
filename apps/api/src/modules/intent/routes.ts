import { Type, Static } from '@sinclair/typebox';
import { FastifyInstance } from 'fastify';
import Anthropic from '@anthropic-ai/sdk';
import { extractJSON } from '../../utils/extractJSON.js';

const ClarifyRequestSchema = Type.Object({
  message: Type.String(),
  history: Type.Optional(Type.Array(Type.Object({
    role: Type.String(),
    content: Type.String()
  }))),
});

const ClarifyResponseSchema = Type.Object({
  goal: Type.String(),
  confidence: Type.Number(),
  missing_dims: Type.Array(Type.String()),
  questions: Type.Array(Type.String()),
  extracted: Type.Object({
    domain: Type.Optional(Type.String()),
    trigger: Type.Optional(Type.String()),
    behavior: Type.Optional(Type.String()),
  }),
});

type ClarifyRequest = Static<typeof ClarifyRequestSchema>;

const CLARIFICATION_PROMPT = `你是一个 Agent 需求分析师。分析用户目标，输出 JSON。

分析用户输入，判断是否需要进一步澄清：
1. 如果置信度 >= 0.7 或已进行 2 轮澄清，输出空的 questions 数组
2. 否则，输出最多 2 个最关键的澄清问题

输出格式（严格 JSON）：
{
  "goal": "结构化目标描述",
  "confidence": 0.0~1.0,
  "missing_dims": ["缺失维度1", "缺失维度2"],
  "questions": ["需要澄清的问题"],
  "extracted": {
    "domain": "回答领域",
    "trigger": "触发方式",
    "behavior": "不确定时行为"
  }
}`;

const client = new Anthropic({
  apiKey: process.env.LLM_API_KEY,
  baseURL: process.env.LLM_BASE_URL,
});

export async function intentRoutes(fastify: FastifyInstance) {
  fastify.post('/api/intent/clarify', {
    schema: {
      body: ClarifyRequestSchema,
      response: { 200: ClarifyResponseSchema }
    }
  }, async (request) => {
    const { message, history = [] } = request.body as ClarifyRequest;

    const messages: Anthropic.MessageParam[] = [
      ...history.map(h => ({ role: h.role as 'user' | 'assistant', content: h.content })),
      { role: 'user', content: message }
    ];

    const response = await client.messages.create({
      model: process.env.LLM_MODEL || 'astron-code-latest',
      max_tokens: 1024,
      system: CLARIFICATION_PROMPT,
      messages,
    });

    const text = response.content[0].type === 'text' ? response.content[0].text : '';
    const result = JSON.parse(extractJSON(text));

    // Termination condition
    const historyLength = history.length;
    if (result.confidence >= 0.7 || historyLength >= 4) {
      return { ...result, questions: [] };
    }

    return result;
  });
}