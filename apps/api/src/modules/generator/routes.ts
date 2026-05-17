import { Type, Static } from '@sinclair/typebox';
import { FastifyInstance } from 'fastify';
import Anthropic from '@anthropic-ai/sdk';
import { extractJSON } from '../../utils/extractJSON.js';

const GenerateRequestSchema = Type.Object({
  goal: Type.String(),
  extracted: Type.Object({
    domain: Type.Optional(Type.String()),
    trigger: Type.Optional(Type.String()),
    behavior: Type.Optional(Type.String()),
  }),
});

const GenerateResponseSchema = Type.Object({
  name: Type.String(),
  description: Type.String(),
  system_prompt: Type.String(),
  tools: Type.Array(Type.String()),
  lifecycle: Type.Object({
    trigger: Type.String(),
    idle_timeout: Type.String(),
    auto_archive: Type.Boolean(),
  }),
  quality_criteria: Type.String(),
});

type GenerateRequest = Static<typeof GenerateRequestSchema>;

const META_PROMPT = `你是一个 Agent 工厂。根据以下信息生成一个 Agent 定义。

[用户目标]
{{user_goal}}

[上下文分析结果]
{{context_summary}}

[可用工具列表]
- knowledge_query: 查询知识库
- web_search: 网页搜索
- feishu_send: 发送飞书消息

[约束条件]
- 工具权限遵循最小权限原则，只给完成任务必须的工具
- system_prompt 必须包含：角色定义、行为规范、不回答的边界、回答格式
- description 用于自动路由匹配，要清晰描述"何时调用此 Agent"

严格输出以下 JSON，不要有其他内容：
{
  "name": "Agent 名称",
  "description": "一句话描述触发时机",
  "system_prompt": "完整角色定义",
  "tools": ["工具名称列表"],
  "lifecycle": {
    "trigger": "message",
    "idle_timeout": "24h",
    "auto_archive": true
  },
  "quality_criteria": "回答质量的评估标准"
}`;

const client = new Anthropic({
  apiKey: process.env.LLM_API_KEY,
  baseURL: process.env.LLM_BASE_URL,
});

export async function generatorRoutes(fastify: FastifyInstance) {
  fastify.post('/api/generator/generate', {
    schema: {
      body: GenerateRequestSchema,
      response: { 200: GenerateResponseSchema }
    }
  }, async (request) => {
    const { goal, extracted } = request.body as GenerateRequest;

    const prompt = META_PROMPT
      .replace('{{user_goal}}', goal)
      .replace('{{context_summary}}', JSON.stringify(extracted));

    const response = await client.messages.create({
      model: process.env.LLM_MODEL || 'astron-code-latest',
      max_tokens: 2048,
      messages: [{ role: 'user', content: prompt }],
    });

    const text = response.content[0].type === 'text' ? response.content[0].text : '';
    return JSON.parse(extractJSON(text));
  });
}