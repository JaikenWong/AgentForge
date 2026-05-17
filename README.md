# AgentForge

对话驱动的企业 Agent 工厂平台。

## 快速开始

### 1. 启动开发环境

```bash
# 启动 Docker (PostgreSQL + Redis)
cd docker && docker compose up -d && cd ..

# 安装依赖
pnpm install

# 运行数据库迁移
cd apps/api && pnpm prisma migrate dev && cd ../..

# 启动所有服务
pnpm dev
```

### 2. 访问服务

- **管理控制台**: http://localhost:5173
- **API**: http://localhost:3000
- **API 文档**: http://localhost:3000/health

## 项目结构

```
AgentForge/
├── apps/
│   ├── api/          # 后端 API 服务 (Fastify)
│   ├── worker/       # Agent Runtime Worker (BullMQ)
│   └── web/          # 管理控制台 (React + Vite)
├── packages/
│   └── shared/       # 共享类型定义
└── docker/           # Docker Compose 配置
```

## 功能特性

- **对话即部署**: 描述目标，自动生成 Agent
- **上下文变知识**: 群聊记录自动提炼为知识
- **有生命周期**: 到期归档，知识沉淀
- **多渠道支持**: 飞书、网站 Widget

## 环境变量

复制 `.env.example` 为 `.env` 并填写：

```env
DATABASE_URL=postgresql://postgres:password@localhost:5432/agentforge
REDIS_URL=redis://localhost:6379
ANTHROPIC_API_KEY=sk-ant-xxx
FEISHU_APP_ID=cli_xxx
FEISHU_APP_SECRET=xxx
```

## API 路由

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/agents` | 创建 Agent |
| GET | `/api/agents` | Agent 列表 |
| POST | `/api/agents/:id/deploy` | 部署 Agent |
| POST | `/api/intent/clarify` | 意图澄清 |
| POST | `/api/generator/generate` | 生成 Agent |
| POST | `/webhook/feishu` | 飞书回调 |

## 技术栈

- **后端**: Node.js + TypeScript + Fastify + Prisma
- **前端**: React 19 + Vite + Tailwind CSS
- **数据库**: PostgreSQL + Redis
- **消息队列**: BullMQ
- **LLM**: Claude API