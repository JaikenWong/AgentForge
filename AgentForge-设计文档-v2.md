# AgentForge 平台设计文档

> 版本：v0.1  
> 定位：企业内部 Agent 工厂平台，用户通过自然语言描述目标，平台自动生成、部署、运行 Agent，无需技术配置。

---

## 目录

1. [产品定位](#1-产品定位)
2. [核心概念](#2-核心概念)
3. [系统架构](#3-系统架构)
4. [核心数据模型](#4-核心数据模型)
5. [Agent 生命周期状态机](#5-agent-生命周期状态机)
6. [核心模块详设](#6-核心模块详设)
7. [渠道接入层](#7-渠道接入层)
8. [Runtime 执行层](#8-runtime-执行层)
9. [大规模并发设计](#9-大规模并发设计)
10. [私有化部署方案](#10-私有化部署方案)
11. [技术栈选型](#11-技术栈选型)
12. [桌宠客户端](#12-桌宠客户端)
13. [开发优先级](#13-开发优先级)

---

## 1. 产品定位

### 核心价值

AgentForge 是一个**对话驱动的 Agent 工厂平台**。用户只需用自然语言描述目标，平台自动完成：

- 分析意图，提炼上下文知识
- 生成 Agent 定义（角色、工具、权限、触发条件）
- 部署到目标渠道（飞书群、网站 Widget 等）
- 持续运行、自动评估、迭代优化

**用户感知不到"在配置 Agent"，只感知到"目标被达成"。**

### 与现有产品的本质差异

| 维度 | 现有 No-Code 产品 | AgentForge |
|------|------------------|------------|
| 用户操作 | 拖拽配置流程 | 描述目标 |
| 心智模型 | "我在搭建一个机器人" | "我在说我要什么" |
| Agent 来源 | 用户手动创建 | 平台自动生成 |
| 生命周期 | 永久运行 | 按需存在，到期归档 |
| 知识来源 | 手动上传 | 从上下文自动提炼 |

### 第一期目标用户

企业内部用户，包括技术人员和非技术人员，主要场景：

- 技术支持群的答疑机器人（飞书）
- 客户服务群的自动回复（飞书）
- 企业官网的智能问答（网站 Widget）

---

## 2. 核心概念

### Agent

Agent 是平台的基本执行单元，本质是一份存在数据库里的**配置 + 知识的组合体**，包含：

- `system_prompt`：角色定义与行为规范
- `tools`：工具白名单（最小权限原则）
- `memory`：从上下文提炼的知识
- `lifecycle`：生命周期配置（触发方式、过期时间）
- `channels`：部署的渠道列表

Agent **不是一个长期运行的进程**，它是数据库里的一行记录，被触发时由 Runtime 按需实例化。

### Runtime

Runtime 是平台的执行引擎，一组长期运行的 Worker 进程。它本身无状态，负责：

1. 监听消息队列
2. 收到消息时从数据库加载 Agent 配置
3. 调用 LLM 执行任务
4. 将结果发回渠道
5. 写入执行日志

### 生命周期

Agent 有明确的生命周期，到期归档而非删除。知识永久沉淀，Agent 可随时复活或克隆。

```
临时 ≠ 消失
归档 ≠ 删除
```

---

## 3. 系统架构

### 整体分层

```
┌─────────────────────────────────────────────┐
│               管理控制台（Web）               │  用户入口
└─────────────────────────────────────────────┘
         │
┌────────┴──────────────────────────────────┐
│                 平台核心                    │
│  ┌───────────┐ ┌──────────┐ ┌──────────┐  │
│  │ 意图理解  │ │Agent生成 │ │生命周期  │  │
│  │   引擎    │ │  引擎    │ │  管理    │  │
│  └───────────┘ └──────────┘ └──────────┘  │
│  ┌───────────────────────────────────────┐ │
│  │         上下文处理引擎                 │ │
│  │  飞书消息摘取·网站内容抽取·知识压缩   │ │
│  └───────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
         │
┌────────┴──────────────────────────────────┐
│           渠道适配层（统一消息总线）         │
│     标准化收发 · 权限校验 · 消息队列        │
└─────────────────────────────────────────────┘
         │
┌────────┴──────────────────────────────────┐
│              执行层                         │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐  │
│  │飞书机器人│ │网站Widget│ │定时/Hook  │  │
│  └──────────┘ └──────────┘ └───────────┘  │
└─────────────────────────────────────────────┘
         │
┌────────┴──────────────────────────────────┐
│    Agent Runtime（Serverless 按需实例化）   │
│   加载配置 · 调用 LLM · 执行工具 · 写日志  │
└─────────────────────────────────────────────┘
         │
┌────────┴──────────────────────────────────┐
│                 存储层                      │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐  │
│  │Agent定义 │ │知识/记忆 │ │执行日志   │  │
│  │    库    │ │    库    │ │   库      │  │
│  └──────────┘ └──────────┘ └───────────┘  │
└─────────────────────────────────────────────┘
```

### 消息流向（以飞书群为例）

```
用户在飞书群发消息
  → 飞书 Webhook 推送到平台
  → 渠道适配层标准化消息格式
  → 写入 Redis 消息队列
  → Worker 从队列取消息
  → 查 Redis 缓存加载 Agent 配置（缓存未命中则查 DB）
  → 组装 system_prompt + 历史记录 + 知识记忆
  → 调用 Claude API
  → 执行工具调用（如有）
  → 通过飞书 API 发送回复
  → 写入执行日志
  → 更新 Agent 状态
```

---

## 4. 核心数据模型

### agents 表

```sql
CREATE TABLE agents (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id         UUID NOT NULL,                    -- 租户隔离
  name              VARCHAR(100) NOT NULL,
  description       TEXT,                             -- 用于自动路由匹配
  status            VARCHAR(20) NOT NULL DEFAULT 'defined',
                    -- defined | active | running | idle | archived
  system_prompt     TEXT NOT NULL,
  tools             JSONB NOT NULL DEFAULT '[]',      -- 工具白名单
  channels          JSONB NOT NULL DEFAULT '[]',      -- 部署的渠道
  lifecycle         JSONB NOT NULL DEFAULT '{}',      -- 生命周期配置
  model             VARCHAR(50) DEFAULT 'claude-sonnet-4-6',
  version           INTEGER NOT NULL DEFAULT 1,
  parent_id         UUID REFERENCES agents(id),       -- 克隆来源
  owner_id          UUID NOT NULL,                    -- 创建者
  trigger_count     INTEGER NOT NULL DEFAULT 0,
  last_triggered_at TIMESTAMPTZ,
  expire_notified   BOOLEAN DEFAULT FALSE,
  created_at        TIMESTAMPTZ DEFAULT NOW(),
  expires_at        TIMESTAMPTZ,
  archived_at       TIMESTAMPTZ
);

-- lifecycle 字段结构示例：
-- {
--   "trigger": "message",           触发方式：message | cron | webhook
--   "idle_timeout": "24h",          无触发多久后进入 IDLE
--   "auto_archive": true,           到期自动归档
--   "notify_before_expire": "1d"    到期前提前通知
-- }

-- tools 字段示例：
-- ["feishu_send", "web_search", "knowledge_query", "http_request"]
```

### agent_memory 表

```sql
CREATE TABLE agent_memory (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  agent_id     UUID NOT NULL REFERENCES agents(id),
  type         VARCHAR(20) NOT NULL,
               -- knowledge | episodic | working | archived
  content      TEXT NOT NULL,
  embedding    vector(1536),                          -- pgvector 向量
  source       VARCHAR(50),                           -- feishu | website | manual
  importance   FLOAT DEFAULT 0.5,
  access_count INTEGER DEFAULT 0,
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  expires_at   TIMESTAMPTZ
);

CREATE INDEX ON agent_memory USING ivfflat (embedding vector_cosine_ops);
```

### execution_logs 表

```sql
CREATE TABLE execution_logs (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  agent_id      UUID NOT NULL REFERENCES agents(id),
  session_id    UUID,                                 -- 多轮对话 session
  channel       VARCHAR(50),                          -- feishu | website | webhook
  trigger_type  VARCHAR(50),
  input         TEXT,
  output        TEXT,
  tools_used    JSONB DEFAULT '[]',
  quality_score FLOAT,                                -- 自动评分 0~1
  tokens_used   INTEGER,
  latency_ms    INTEGER,
  user_feedback SMALLINT DEFAULT 0,                   -- -1差 | 0无 | 1好
  error         TEXT,
  created_at    TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 5. Agent 生命周期状态机

### 状态定义

| 状态 | 含义 | 资源消耗 |
|------|------|---------|
| `defined` | 已定义，尚未部署 | 零 |
| `active` | 已部署，待触发 | 零（仅占 DB 一行） |
| `running` | 正在执行 LLM 调用 | 按 token 计费 |
| `idle` | 超过 idle_timeout 未被触发 | 零 |
| `archived` | 已归档，知识保留 | 仅存储费用 |

### 状态转换规则

```
defined  → active    部署操作触发（平台写入）
active   → running   收到消息触发（Runtime 写入）
running  → active    LLM 执行完成（Runtime 写入，finally 保证）
active   → idle      idle_timeout 到期（Redis TTL 过期事件触发）
idle     → active    再次收到消息（Runtime 写入）
*        → archived  expires_at 到期 或 用户手动归档
archived → active    用户点击"复活"（平台写入）
```

### 状态维护实现

**核心原则：状态存数据库，触发靠 Redis TTL 事件，不做全表扫描。**

```typescript
// 部署时：注册 idle 监控
async function deployAgent(agentId: string) {
  const agent = await db.getAgent(agentId)
  await db.updateStatus(agentId, 'active')

  // 用 Redis TTL 代替定时器
  const idleSeconds = parseDuration(agent.lifecycle.idle_timeout)
  await redis.setex(`agent:idle-watch:${agentId}`, idleSeconds, agentId)

  // 注册到期监控
  if (agent.expires_at) {
    const expiresIn = Math.floor((new Date(agent.expires_at).getTime() - Date.now()) / 1000)
    await redis.setex(`agent:expire-watch:${agentId}`, expiresIn, agentId)
  }
}

// 每次触发时：重置 idle 计时器
async function onAgentTriggered(agentId: string) {
  const agent = await loadAgent(agentId)
  const idleSeconds = parseDuration(agent.lifecycle.idle_timeout)
  await redis.setex(`agent:idle-watch:${agentId}`, idleSeconds, agentId)
}

// 监听 Redis key 过期事件
redis.subscribe('__keyevent@0__:expired', async (key: string) => {
  if (key.startsWith('agent:idle-watch:')) {
    const agentId = key.replace('agent:idle-watch:', '')
    await db.updateStatus(agentId, 'idle')
  }

  if (key.startsWith('agent:expire-watch:')) {
    const agentId = key.replace('agent:expire-watch:', '')
    await archiveAgent(agentId)
    await notifyOwner(agentId, 'expired')
  }
})

// 归档：保留知识，清除执行状态
async function archiveAgent(agentId: string) {
  await db.updateStatus(agentId, 'archived', { archived_at: new Date() })
  await redis.del(`agent:config:${agentId}`)  // 清配置缓存
  // 注意：agent_memory 表数据保留，知识不删除
}
```

---

## 6. 核心模块详设

### 6.1 意图理解引擎

负责将用户的自然语言目标转化为结构化的 Agent 需求。

**设计原则：**
- 多轮澄清，不追求一次完美，先跑起来再迭代
- 只问最关键的 1~2 个问题，不做问卷式轰炸
- 识别隐含需求（用户没说但需要的能力）

**澄清终止条件：**

满足以下任一条件时停止澄清，直接生成 Agent：

| 条件 | 阈值 | 说明 |
|------|------|------|
| 置信度 | ≥ 0.7 | LLM 对需求理解的结构化输出置信度 |
| 轮次 | ≥ 2 | 最多 2 轮澄清，避免用户疲劳 |
| 用户确认 | "就这样" / "可以了" | 用户主动确认 |

**置信度计算：**

```typescript
interface ClarificationResult {
  goal: string              // 结构化目标
  confidence: number        // 0~1，由 LLM 自评
  missing_dims: string[]    // 缺失的关键维度
  questions: string[]       // 待澄清问题列表
}

async function clarify(userInput: string, history: Message[]): Promise<ClarificationResult> {
  const result = await callLLM({
    prompt: `
      分析用户目标，输出 JSON：
      {
        "goal": "结构化目标描述",
        "confidence": 0.0~1.0,
        "missing_dims": ["缺失维度1", "缺失维度2"],
        "questions": ["需要澄清的问题"]
      }

      用户输入：${userInput}
      对话历史：${JSON.stringify(history)}
    `
  })

  // 终止判断
  if (result.confidence >= 0.7 || history.length >= 2) {
    return { ...result, questions: [] }  // 停止澄清
  }

  return result
}
```

**用户回答模糊时的处理：**

```typescript
// 模糊回答检测
const VAGUE_PATTERNS = ['随便', '都行', '你定', '不知道', '不确定']

function handleVagueAnswer(answer: string, dim: string): string | null {
  if (VAGUE_PATTERNS.some(p => answer.includes(p))) {
    // 根据场景类型选择默认值
    return getDefaultForDimension(dim)
  }
  return answer
}

function getDefaultForDimension(dim: string): string {
  const defaults = {
    '触发方式': 'mention',           // 默认 @ 触发
    '不确定时行为': 'admit',         // 默认承认不确定
    '生命周期': '7d',               // 默认 7 天
    '回答领域': 'auto_detect'       // 从上下文自动检测
  }
  return defaults[dim] || 'auto'
}
```

**澄清维度示例（飞书群答疑场景）：**

```
用户说："帮我做一个答疑机器人"

需要澄清的维度：
  - 主要回答什么领域的问题？（决定知识库构建方式）
  - 触发方式：@ 机器人 还是 所有消息都响应？
  - 不确定时的行为：说不确定 还是 尝试回答？

不需要用户回答的（平台自动决定）：
  - 工具权限（根据场景自动分配最小权限）
  - 生命周期（默认 7 天，可修改）
  - 模型选择（默认 claude-sonnet）
```

### 6.2 上下文处理引擎

将原始上下文（群聊记录、网站内容）压缩为结构化知识注入 Agent 记忆。

**敏感信息过滤策略：**

所有来源的上下文在进入处理流程前，必须经过敏感信息过滤。

| 来源 | 过滤时机 | 过滤方式 | 过滤内容 |
|------|---------|---------|---------|
| 飞书群消息 | 平台接收时 | 正则 + LLM | 手机号、身份证、银行卡、密码、Token、API Key |
| 网站内容 | 抓取后 | 正则 + LLM | 同上 + 内网 IP、员工姓名 |
| 文档上传 | 解析后 | 正则 + LLM | 同上 |
| 桌宠本地 | 本地预处理 | 正则 | 同上 + 文件绝对路径中的用户名 |

**过滤实现：**

```typescript
// 正则规则（快速匹配）
const SENSITIVE_PATTERNS = [
  { pattern: /1[3-9]\d{9}/g, replacement: '[PHONE]' },
  { pattern: /\d{17}[\dXx]/g, replacement: '[ID_CARD]' },
  { pattern: /\d{16,19}/g, replacement: '[BANK_CARD]' },
  { pattern: /password\s*[=:]\s*\S+/gi, replacement: 'password=[REDACTED]' },
  { pattern: /api[_-]?key\s*[=:]\s*\S+/gi, replacement: 'api_key=[REDACTED]' },
  { pattern: /token\s*[=:]\s*\S+/gi, replacement: 'token=[REDACTED]' },
  { pattern: /sk-[a-zA-Z0-9]{20,}/g, replacement: '[API_KEY]' },
  { pattern: /10\.\d{1,3}\.\d{1,3}\.\d{1,3}/g, replacement: '[INTERNAL_IP]' },
  { pattern: /192\.168\.\d{1,3}\.\d{1,3}/g, replacement: '[INTERNAL_IP]' },
]

function quickFilter(text: string): string {
  let filtered = text
  for (const { pattern, replacement } of SENSITIVE_PATTERNS) {
    filtered = filtered.replace(pattern, replacement)
  }
  return filtered
}

// LLM 深度过滤（处理正则无法覆盖的场景）
async function deepFilter(text: string): Promise<string> {
  const result = await callSubAgent({
    prompt: `
      检查以下文本中是否包含敏感信息，如有则替换为 [REDACTED]。
      敏感信息包括：真实姓名、员工工号、内部系统名称、未脱敏的日志内容。

      输出处理后的文本，不要解释。

      文本：${text}
    `
  })
  return result
}

// 组合过滤流程
async function filterSensitiveInfo(text: string, source: string): Promise<string> {
  // 1. 正则快速过滤
  let filtered = quickFilter(text)

  // 2. 飞书消息和网站内容额外走 LLM 深度过滤
  if (['feishu_messages', 'website_url'].includes(source)) {
    filtered = await deepFilter(filtered)
  }

  return filtered
}
```

**处理流程：**

```typescript
async function processContext(source: ContextSource): Promise<AgentKnowledge> {

  // 1. 提取原始内容
  const rawContent = await extract(source)
  // source.type === 'feishu_messages' | 'website_url' | 'document'

  // 2. 敏感信息过滤（新增）
  const filteredContent = await filterSensitiveInfo(rawContent, source.type)

  // 3. 用 SubAgent 分析（不污染主对话上下文）
  const analysis = await callSubAgent({
    prompt: `
      分析以下内容，提取：
      1. 主要领域和话题
      2. 高频问题列表（最多20条）
      3. 回答风格特征
      4. 专有名词和术语表
      5. 明确不应该回答的内容

      内容：${filteredContent}

      输出严格的 JSON 格式。
    `
  })

  // 4. 向量化存储
  const chunks = splitIntoChunks(filteredContent)
  for (const chunk of chunks) {
    const embedding = await embed(chunk)
    await db.insertMemory({
      content: chunk,
      embedding,
      source: source.type,
      type: 'knowledge'
    })
  }

  return analysis
}
```

### 6.3 Agent 生成引擎

核心 meta-prompt，调用 LLM 生成 Agent 定义文件。

```typescript
const META_PROMPT = `
你是一个 Agent 工厂。根据以下信息生成一个 Agent 定义。

[用户目标]
{{user_goal}}

[上下文分析结果]
{{context_summary}}

[可用工具列表]
{{available_tools}}

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
}
`
```

### 6.4 执行质量评分引擎

每次 Agent 执行后自动评分，低分触发优化流程。

**评分维度与权重：**

| 维度 | 权重 | 数据来源 | 说明 |
|------|------|---------|------|
| 响应相关性 | 30% | LLM 自评 | 回答是否切题 |
| 知识利用率 | 25% | 工具调用记录 | 是否使用了知识库/上下文 |
| 响应时长 | 15% | 系统记录 | latency_ms，越低越好 |
| 用户反馈 | 20% | 用户点赞/点踩 | 显式反馈 |
| 格式规范 | 10% | LLM 自评 | 是否符合预设输出格式 |

**评分公式：**

```typescript
interface ExecutionMetrics {
  relevance_score: number      // 0~1，LLM 自评
  knowledge_used: boolean      // 是否调用 knowledge_query
  latency_ms: number           // 响应时长
  user_feedback: -1 | 0 | 1    // -1差 0无 1好
  format_score: number         // 0~1，LLM 自评
}

function calculateQualityScore(m: ExecutionMetrics): number {
  // 响应时长归一化（<2s = 1, >10s = 0, 线性插值）
  const latency_score = Math.max(0, Math.min(1, (10000 - m.latency_ms) / 8000))

  // 知识利用得分
  const knowledge_score = m.knowledge_used ? 1 : 0.5

  // 用户反馈归一化
  const feedback_score = m.user_feedback === 1 ? 1
                       : m.user_feedback === -1 ? 0
                       : 0.7  // 无反馈时给中等分

  // 加权求和
  const score =
    m.relevance_score * 0.30 +
    knowledge_score * 0.25 +
    latency_score * 0.15 +
    feedback_score * 0.20 +
    m.format_score * 0.10

  return Math.round(score * 100) / 100  // 保留两位小数
}
```

**LLM 自评 Prompt：**

```typescript
async function selfEvaluate(input: string, output: string, systemPrompt: string): Promise<{
  relevance: number
  format: number
}> {
  const result = await callLLM({
    prompt: `
      评估以下 Agent 回答的质量，输出 JSON：
      {
        "relevance": 0.0~1.0,  // 回答是否切题
        "format": 0.0~1.0      // 是否符合预设格式
      }

      用户问题：${input}
      Agent 回答：${output}
      角色定义：${systemPrompt}
    `
  })
  return JSON.parse(result)
}
```

**低分触发优化：**

| 条件 | 触发动作 | 说明 |
|------|---------|------|
| 单次评分 < 0.5 | 记录 + 通知 owner | 异常执行，需人工关注 |
| 连续 3 次评分 < 0.7 | 自动触发 Agent 重生成 | 知识库或 prompt 需优化 |
| 用户点踩 | 立即记录 + 知识库标记 | 该问答对需人工审核 |

**优化触发逻辑：**

```typescript
async function checkAndOptimize(agentId: string): Promise<void> {
  // 获取最近 10 次执行记录
  const recentLogs = await db.query(`
    SELECT quality_score FROM execution_logs
    WHERE agent_id = $1
    ORDER BY created_at DESC
    LIMIT 10
  `, [agentId])

  // 计算连续低分次数
  let lowScoreStreak = 0
  for (const log of recentLogs) {
    if (log.quality_score < 0.7) lowScoreStreak++
    else break
  }

  // 触发优化
  if (lowScoreStreak >= 3) {
    await triggerAgentOptimization(agentId, 'consecutive_low_scores')
  }
}

async function triggerAgentOptimization(agentId: string, reason: string): Promise<void> {
  // 1. 分析低分原因
  const analysis = await analyzeLowScoreReasons(agentId)

  // 2. 重新生成 Agent
  const newConfig = await regenerateAgent(agentId, analysis)

  // 3. 通知 owner 确认
  await notifyOwner(agentId, {
    type: 'optimization_suggested',
    reason,
    changes: newConfig.changes,
    action: 'review_and_confirm'
  })
}
```

---

## 7. 渠道接入层

### 统一消息格式

所有渠道的消息在进入系统时统一转换为标准格式：

```typescript
interface UnifiedMessage {
  id:          string
  agent_id:    string
  channel:     'feishu' | 'website' | 'webhook'
  channel_ctx: {
    group_id?:    string    // 飞书群 ID
    message_id?:  string    // 飞书消息 ID（用于回复）
    session_id?:  string    // 网站访客 session
    user_id?:     string
    user_name?:   string
  }
  content:     string
  role:        'user' | 'agent'
  created_at:  Date
}
```

### 渠道适配器接口

```typescript
interface ChannelAdapter {
  receive(raw: any): UnifiedMessage        // 原始消息 → 统一格式
  send(msg: UnifiedMessage, reply: string): Promise<void>  // 发送回复
  verify(signature: string, body: string): boolean         // Webhook 验签
}
```

### 飞书适配器要点

- 接收 Webhook：`POST /webhook/feishu`
- 验签：使用飞书 Encrypt Key 验证请求合法性
- 消息类型过滤：只处理 `im.message.receive_v1` 事件
- 防重放：记录已处理的 `message_id`，避免重复处理
- @ 过滤：检查消息中是否包含 `@机器人`（可配置为全量响应）
- 回复方式：使用飞书消息 ID 回复到对应消息线程

### 网站 Widget 接入

提供一段嵌入代码，客户复制到网页 `<head>` 中即可：

```html
<script>
  window.AgentForgeConfig = {
    agentId: 'agent_abc123',
    token:   'pk_live_xxxx'
  }
</script>
<script src="https://cdn.agentforge.com/widget.js" async></script>
```

Widget 特性：
- iframe 隔离，不影响宿主页面
- 支持自定义主题色、欢迎语
- session 基于 localStorage，多轮对话连续
- 访客无需登录

---

## 8. Runtime 执行层

### Worker 执行流程

```typescript
async function handleMessage(msg: UnifiedMessage): Promise<void> {

  // 1. 加载 Agent 配置（优先读缓存）
  const agent = await loadAgent(msg.agent_id)
  if (!agent || agent.status === 'archived') return

  // 2. 更新状态为 running
  await updateAgentStatus(agent.id, 'running')
  await onAgentTriggered(agent.id)  // 重置 idle 计时器

  const startTime = Date.now()

  try {
    // 3. 检索相关记忆
    const memories = await retrieveRelevantMemory(agent.id, msg.content, 5)

    // 4. 加载会话历史（最近 10 轮）
    const history = await getSessionHistory(msg.channel_ctx.session_id, 10)

    // 5. 组装 prompt
    const systemPrompt = buildSystemPrompt(agent.system_prompt, memories)

    // 6. 调用 LLM（带速率限制）
    const response = await llmRateLimiter.call(agent.id, {
      model:    agent.model,
      system:   systemPrompt,
      messages: [...history, { role: 'user', content: msg.content }],
      tools:    resolveTools(agent.tools),
      max_tokens: 1000
    })

    // 7. 执行工具调用（如有）
    let finalOutput = response.content
    if (response.stop_reason === 'tool_use') {
      finalOutput = await executeToolCalls(response, agent.tools)
    }

    // 8. 发送回复
    const adapter = getChannelAdapter(msg.channel)
    await adapter.send(msg, finalOutput)

    // 9. 记录执行日志
    await logExecution({
      agent_id:     agent.id,
      session_id:   msg.channel_ctx.session_id,
      channel:      msg.channel,
      input:        msg.content,
      output:       finalOutput,
      tokens_used:  response.usage.input_tokens + response.usage.output_tokens,
      latency_ms:   Date.now() - startTime,
      quality_score: await autoScore(msg.content, finalOutput)
    })

  } finally {
    // 无论成功失败，都要恢复状态
    await updateAgentStatus(agent.id, 'active')
  }
}
```

### Agent 配置缓存

```typescript
async function loadAgent(agentId: string): Promise<Agent> {
  // 先查 Redis 缓存
  const cached = await redis.get(`agent:config:${agentId}`)
  if (cached) return JSON.parse(cached)

  // 缓存未命中，查数据库
  const agent = await db.query(
    `SELECT * FROM agents WHERE id = $1 AND status != 'archived'`,
    [agentId]
  )
  if (!agent) return null

  // 写入缓存，10 分钟过期
  await redis.setex(`agent:config:${agentId}`, 600, JSON.stringify(agent))
  return agent
}

// Agent 更新时主动清缓存
async function updateAgent(agentId: string, updates: Partial<Agent>) {
  await db.update('agents', agentId, updates)
  await redis.del(`agent:config:${agentId}`)
}
```

### 工具执行沙箱

```typescript
// 普通工具直接执行
const SAFE_TOOLS = {
  web_search:       (params) => webSearch(params.query),
  knowledge_query:  (params) => queryMemory(params.agent_id, params.query),
  feishu_send:      (params) => feishuClient.sendMessage(params),
  http_request:     (params) => safeHttpRequest(params),  // 有域名白名单
}

// 高风险工具走 Docker 沙箱
const SANDBOXED_TOOLS = {
  execute_code: async (params) => {
    const container = await docker.createContainer({
      Image:      'python:3.11-slim',
      Cmd:        ['python', '-c', params.code],
      HostConfig: {
        Memory:          128 * 1024 * 1024,
        NetworkMode:     'none',
        ReadonlyRootfs:  true,
        AutoRemove:      true,
      }
    })
    await container.start()
    const result = await container.wait()
    return result.output
  }
}

function resolveTools(toolNames: string[]) {
  return toolNames.map(name => ({
    ...(SAFE_TOOLS[name] || SANDBOXED_TOOLS[name]),
    name
  }))
}
```

---

## 9. 大规模并发设计

### 消息队列 + 多 Worker

```
单进程串行（不可扩展）：
  消息1 → 处理 → 消息2 → 处理 → ...

多 Worker 并行（可水平扩展）：
  消息1 ─┐
  消息2 ─┤→ Redis Queue → Worker1
  消息3 ─┤             → Worker2
  消息N ─┘             → Worker3 ... WorkerN
```

```typescript
// 启动多个 Worker 实例
async function startWorkers(count: number) {
  for (let i = 0; i < count; i++) {
    startWorker(i)
  }
}

async function startWorker(workerId: number) {
  while (true) {
    // 阻塞等待消息，0 表示永久等待
    const [, rawMsg] = await redis.brpop('agent:queue', 0)
    const msg: UnifiedMessage = JSON.parse(rawMsg)

    // 异步处理，不等待，立即取下一条
    handleMessage(msg).catch(err => {
      logger.error(`Worker ${workerId} error for agent ${msg.agent_id}:`, err)
    })
  }
}
```

### LLM 速率限制

```typescript
class LLMRateLimiter {
  private queues = {
    high:   [] as Task[],   // 用户实时等待
    normal: [] as Task[],   // 普通触发
    low:    [] as Task[],   // 后台任务
  }
  private tokens = 60        // 令牌桶容量（每分钟请求数）

  async call(agentId: string, params: any, priority = 'normal') {
    return new Promise((resolve, reject) => {
      this.queues[priority].push({ agentId, params, resolve, reject })
      this.process()
    })
  }

  private async process() {
    if (this.tokens <= 0) return

    // 优先消费 high 队列
    const queue = this.queues.high.length > 0 ? this.queues.high
                : this.queues.normal.length > 0 ? this.queues.normal
                : this.queues.low

    if (queue.length === 0) return

    this.tokens--
    const task = queue.shift()!

    try {
      const result = await anthropic.messages.create(task.params)
      task.resolve(result)
    } catch (err) {
      task.reject(err)
    }
  }
}
```

### 各规模阶段配置建议

| 阶段 | 活跃 Agent 数 | Worker 数 | Redis | 数据库 |
|------|--------------|-----------|-------|--------|
| MVP | < 50 | 2 | 单机 | 单机 PG |
| 成长期 | 50~500 | 5~10 | 主从复制 | 读写分离 |
| 规模化 | 500~5000 | 20~50（弹性） | 集群 | 分库 + pgvector 独立 |

---

## 10. 私有化部署方案

当客户要求数据不出内网时，平台支持生成独立部署包。

### 配置注入方式（改进版）

**问题**：原方案将 Agent 配置直接写入环境变量，存在以下风险：
- 配置更新需重启容器
- 敏感信息暴露在环境变量中
- JSON 配置过长可能超出环境变量长度限制

**改进方案**：配置文件挂载 + 环境变量仅注入敏感信息。

```yaml
# docker-compose.yml（由平台自动生成）
version: '3.8'
services:
  agent-runtime:
    image: agentforge/runtime:latest
    volumes:
      - ./config:/app/config:ro        # 配置文件挂载
      - ./data:/app/data               # 数据持久化
    environment:
      # 仅敏感信息通过环境变量注入
      LLM_API_KEY:       "${LLM_API_KEY}"
      FEISHU_APP_ID:     "${FEISHU_APP_ID}"
      FEISHU_APP_SECRET: "${FEISHU_APP_SECRET}"
      FEISHU_ENCRYPT_KEY: "${FEISHU_ENCRYPT_KEY}"
      # 配置文件路径
      AGENT_CONFIG_PATH: "/app/config/agent.json"
    ports:
      - "8080:8080"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes  # 持久化

volumes:
  redis_data:
```

**配置文件结构：**

```json
// config/agent.json（非敏感配置）
{
  "agent_id": "agent_abc123",
  "name": "Java技术答疑机器人",
  "system_prompt": "...",
  "tools": ["knowledge_query", "web_search"],
  "lifecycle": {
    "trigger": "message",
    "idle_timeout": "24h"
  },
  "channels": [
    {
      "type": "feishu",
      "group_id": "oc_xxx"
    }
  ]
}
```

```env
# .env（敏感信息，客户自行填写）
LLM_API_KEY=sk-ant-xxx
FEISHU_APP_ID=cli_xxx
FEISHU_APP_SECRET=xxx
FEISHU_ENCRYPT_KEY=xxx
```

**客户操作：**

```bash
# 1. 解压部署包
tar -xzf agentforge-agent-abc123.tar.gz
cd agentforge-agent-abc123

# 2. 填写 .env 文件
cp .env.example .env
vim .env

# 3. 启动
docker-compose up -d

# 4. 查看状态
docker-compose logs -f agent-runtime

# 5. 更新配置（无需重启）
# 编辑 config/agent.json 后，Runtime 自动检测文件变化并热加载
vim config/agent.json
```

**配置热加载机制：**

```typescript
// Runtime 内部实现
class ConfigWatcher {
  private configPath: string
  private lastConfig: AgentConfig

  start() {
    // 使用 fs.watch 监听配置文件变化
    fs.watch(this.configPath, (event) => {
      if (event === 'change') {
        this.reloadConfig()
      }
    })
  }

  private async reloadConfig() {
    const newConfig = JSON.parse(fs.readFileSync(this.configPath, 'utf-8'))

    // 校验配置合法性
    if (!this.validateConfig(newConfig)) {
      logger.error('Invalid config, keeping old one')
      return
    }

    this.lastConfig = newConfig
    logger.info('Config reloaded successfully')
  }
}
```

**生成部署包的 API：**

```typescript
async function generateDeployPackage(agentId: string): Promise<string> {
  const agent = await db.getAgent(agentId)

  // 分离敏感和非敏感配置
  const safeConfig = {
    agent_id: agent.id,
    name: agent.name,
    system_prompt: agent.system_prompt,
    tools: agent.tools,
    lifecycle: agent.lifecycle,
    channels: agent.channels.map(c => ({
      type: c.type,
      group_id: c.group_id  // 不包含 app_id/secret
    }))
  }

  // 生成文件结构
  const packageDir = `/tmp/deploy-${agentId}`
  fs.mkdirSync(`${packageDir}/config`, { recursive: true })
  fs.writeFileSync(`${packageDir}/config/agent.json`, JSON.stringify(safeConfig, null, 2))
  fs.writeFileSync(`${packageDir}/docker-compose.yml`, generateDockerCompose())
  fs.writeFileSync(`${packageDir}/.env.example`, generateEnvExample())

  // 打包
  const tarPath = `${packageDir}.tar.gz`
  execSync(`tar -czf ${tarPath} -C ${packageDir} .`)

  return tarPath
}
```

---

## 11. 技术栈选型

| 层 | 技术选型 | 理由 |
|----|---------|------|
| 后端 API | Node.js (TypeScript) | 异步 IO 适合大量并发消息处理 |
| 消息队列 | Redis (BullMQ) | 轻量、可靠、内置重试和优先级 |
| 数据库 | PostgreSQL | 成熟稳定，pgvector 支持向量检索 |
| 向量检索 | pgvector | 早期省事，数据量大时迁移 Milvus |
| LLM | Claude API (claude-sonnet-4-6) | 长上下文、工具调用能力强 |
| 飞书接入 | 飞书开放平台 Bot SDK | 官方 SDK，Webhook 事件处理 |
| 网站 Widget | 原生 JS + iframe 隔离 | 零依赖，客户接入成本低 |
| 沙箱执行 | Docker SDK | 高风险工具隔离执行 |
| 桌宠客户端 | Tauri 2.0 (Rust + WebView) | 体积 8MB vs Electron 150MB，Rust 系统调用安全 |
| 本地通信协议 | MCP (Stdio / HTTP 双模式) | 标准化本地工具暴露，与云端 Agent 无缝协同 |
| 桌宠本地存储 | SQLite | 轻量零配置，审计日志与配置持久化 |
| 监控 | 自建日志 + Prometheus | 初期够用，后期接入 Grafana |

---

## 12. 桌宠客户端

### 12.1 定位与价值

桌宠客户端是 AgentForge 的**第五种接入渠道**，也是最具差异性的一种。前四种渠道（飞书、网站 Widget、Webhook、定时任务）均只能处理消息和网络资源；桌宠客户端通过常驻用户桌面，突破了这一边界，让 Agent 能够感知和操作用户的本地环境。

```
现有渠道盲区                  桌宠解锁的能力
──────────────────────        ──────────────────────
只能处理网络消息               本地文件读写与分析
无法感知用户上下文             屏幕 / 剪贴板实时感知
数据必须经过云端               敏感数据本地处理不上云
用户需主动触发                 上下文感知主动提供帮助
```

**设计原则：像安静的同事，而不是不停弹窗的工具。** 桌宠只在三种情况主动出现：用户拖文件或截图给它、用户预设的触发规则被满足、云端 Agent 有消息推送。其他时候安静待命，不打断工作流。

### 12.2 系统架构（五层）

```
┌─────────────────────────────────────┐
│  UI 层（Tauri + React）              │  悬浮窗口 · 系统托盘 · 拖拽区域
├─────────────────────────────────────┤
│  感知层                              │  文件系统监听 · 剪贴板轮询
│                                     │  截图捕获 · 系统通知订阅
├─────────────────────────────────────┤
│  本地 MCP Server                     │  暴露本地工具给云端 Agent
│                                     │  Stdio / HTTP 双模式
│                                     │  工具白名单强制隔离
├─────────────────────────────────────┤
│  安全沙箱层                           │  目录权限声明 · 操作审计日志
│                                     │  敏感路径永久禁止访问
├─────────────────────────────────────┤
│  云端通信层                           │  WebSocket 长连接
│                                     │  Agent 指令接收 · 本地结果上传
│                                     │  断线自动重连
├─────────────────────────────────────┤
│  本地存储（SQLite）                   │  配置 · 操作历史 · 加密 Token
└─────────────────────────────────────┘
```

### 12.3 MCP 工具白名单（三级权限）

桌宠内置 MCP Server，将本地能力以工具形式暴露给云端 Agent 调用。所有工具按风险分为三级：

#### 一级：默认开启（无需额外授权）

| 工具名 | 说明 |
|--------|------|
| `clipboard_read` | 读取剪贴板文本内容 |
| `clipboard_write` | 向剪贴板写入文本 |
| `notify_send` | 发送系统桌面通知 |
| `app_status` | 查询当前前台应用名称 |

#### 二级：首次使用需用户授权

| 工具名 | 说明 |
|--------|------|
| `file_read` | 读取指定路径文件内容 |
| `file_write` | 在授权目录内写入文件 |
| `screenshot` | 截取当前屏幕或指定窗口 |
| `folder_watch` | 监听指定目录文件变动事件 |

#### 三级：每次执行需用户确认（高风险）

| 工具名 | 说明 |
|--------|------|
| `shell_exec` | 在沙箱内执行 Shell 命令 |
| `app_launch` | 启动本地应用程序 |
| `file_delete` | 删除文件（需二次确认弹窗） |
| `network_request` | 代理发起本地网络请求 |

**永久禁止访问（硬编码，不可被 Agent 覆盖）：**
`.ssh` 目录、密钥文件（`.pem` `.key`）、浏览器密码存储、加密钱包、`/etc`、`/proc`

### 12.4 权限授权管理

**授权流程：**

```
┌─────────────────────────────────────────────────────────────┐
│  Agent 请求调用二级/三级工具                                  │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│  检查本地授权缓存（SQLite）                                   │
│  - 已授权 → 直接执行                                         │
│  - 未授权 → 弹出授权弹窗                                     │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│  授权弹窗                                                    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  🔐 Agent 请求权限                                   │    │
│  │                                                     │    │
│  │  "Java答疑机器人" 想要读取文件：                      │    │
│  │  /Users/xxx/Documents/project/README.md             │    │
│  │                                                     │    │
│  │  [仅本次允许]  [始终允许此目录]  [拒绝]               │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
         │
         ├─ [仅本次允许] → 执行一次，不缓存
         ├─ [始终允许此目录] → 写入授权记录，后续同目录自动通过
         └─ [拒绝] → 拒绝执行，返回错误给 Agent
```

**授权记录存储（SQLite）：**

```sql
CREATE TABLE tool_permissions (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  agent_id     TEXT NOT NULL,
  tool_name    TEXT NOT NULL,
  resource     TEXT,                    -- 授权的资源路径（如目录）
  permission   TEXT NOT NULL,           -- 'once' | 'always' | 'denied'
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at   TIMESTAMP,               -- 可选：授权过期时间

  UNIQUE(agent_id, tool_name, resource)
);

-- 示例数据
INSERT INTO tool_permissions (agent_id, tool_name, resource, permission)
VALUES
  ('agent_abc', 'file_read', '/Users/xxx/Documents/project', 'always'),
  ('agent_abc', 'screenshot', NULL, 'once'),
  ('agent_xyz', 'shell_exec', NULL, 'denied');
```

**授权检查逻辑：**

```typescript
interface PermissionCheck {
  agentId: string
  toolName: string
  resource?: string  // 文件路径等
}

async function checkPermission(check: PermissionCheck): Promise<'allowed' | 'denied' | 'prompt'> {
  const db = await getLocalDb()

  // 1. 检查精确匹配
  const exact = await db.get(`
    SELECT permission FROM tool_permissions
    WHERE agent_id = ? AND tool_name = ? AND resource = ?
  `, [check.agentId, check.toolName, check.resource])

  if (exact) {
    return exact.permission === 'always' ? 'allowed' : 'denied'
  }

  // 2. 检查目录通配（file_read/file_write）
  if (check.resource) {
    const parentPerms = await db.all(`
      SELECT resource, permission FROM tool_permissions
      WHERE agent_id = ? AND tool_name = ? AND permission = 'always'
    `, [check.agentId, check.toolName])

    for (const perm of parentPerms) {
      if (check.resource.startsWith(perm.resource)) {
        return 'allowed'
      }
    }
  }

  // 3. 未找到授权记录，需要弹窗
  return 'prompt'
}
```

**授权弹窗 UI 组件：**

```tsx
// React 组件
function PermissionDialog({ request, onDecision }: Props) {
  const { agentName, toolName, resource } = request

  return (
    <div className="permission-dialog">
      <div className="header">🔐 权限请求</div>
      <div className="body">
        <p><strong>{agentName}</strong> 想要：</p>
        <p className="action">{getToolDescription(toolName)}</p>
        {resource && <p className="resource">{resource}</p>}
      </div>
      <div className="actions">
        <button onClick={() => onDecision('once')}>仅本次允许</button>
        <button onClick={() => onDecision('always')}>始终允许</button>
        <button onClick={() => onDecision('denied')} className="danger">拒绝</button>
      </div>
    </div>
  )
}
```

**撤销授权：**

用户可在设置页面管理已授权的权限：

```tsx
// 设置页面 - 权限管理
function PermissionManager() {
  const [permissions, setPermissions] = useState<Permission[]>([])

  const revokePermission = async (id: number) => {
    await db.run(`DELETE FROM tool_permissions WHERE id = ?`, [id])
    setPermissions(permissions.filter(p => p.id !== id))
  }

  return (
    <div className="permission-manager">
      <h3>已授权的权限</h3>
      <table>
        <thead>
          <tr><th>Agent</th><th>工具</th><th>资源</th><th>操作</th></tr>
        </thead>
        <tbody>
          {permissions.map(p => (
            <tr key={p.id}>
              <td>{p.agentName}</td>
              <td>{p.toolName}</td>
              <td>{p.resource || '全局'}</td>
              <td>
                <button onClick={() => revokePermission(p.id)}>撤销</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
```

**三级工具的额外确认：**

对于三级工具（shell_exec、file_delete 等），即使有授权记录，每次执行仍需二次确认：

```typescript
async function executeHighRiskTool(toolName: string, params: any): Promise<any> {
  // 即使有授权，也要二次确认
  const confirmed = await showConfirmDialog({
    title: '⚠️ 高风险操作确认',
    message: `即将执行：${toolName}\n参数：${JSON.stringify(params, null, 2)}`,
    confirmText: '确认执行',
    cancelText: '取消'
  })

  if (!confirmed) {
    throw new Error('User cancelled high-risk operation')
  }

  return await executeTool(toolName, params)
}
```

### 12.5 本地 ↔ 云端协同链路

以"用户复制报错文本，桌宠自动触发分析"为例：

```
① 感知触发
   用户复制报错文本
   → 剪贴板感知层检测到变化
   → 匹配预设规则："包含 Exception → 触发错误分析 Agent"

② 本地预处理
   桌宠本地 MCP Server 执行 clipboard_read
   → 本地过滤敏感信息（IP、密钥、用户名）
   → 封装为标准 UnifiedMessage 格式

③ 云端 Agent 处理
   通过 WebSocket 推送给平台 Runtime
   → Agent 调用 LLM 分析错误原因
   → 可能回调本地工具（如 file_read 读取相关代码文件）

④ 结果推送
   平台通过 WebSocket 将结果推回桌宠
   → 悬浮卡片展示分析结果
   → 用户一键复制修复方案
   → 可选：自动写入剪贴板 或 发送到飞书群

⑤ 日志归档
   本地操作写入 SQLite 审计日志
   云端同步执行日志与质量评分
   用户可在管理控制台查看所有桌宠操作记录
```

### 12.5 技术选型详解

**为什么选 Tauri 而非 Electron：**

```
对比维度        Electron        Tauri
─────────────────────────────────────────
安装包体积      ~150MB          ~8MB
内存占用        ~200MB          ~30MB
系统调用安全    JS（较弱）       Rust（强）
启动速度        慢（2-4秒）      快（<1秒）
跨平台          ✓               ✓
社区活跃度      成熟             快速增长
```

Claude Desktop 从 Electron 迁移到 Tauri 的趋势印证了这一选择。

**本地通信：MCP Stdio 模式 vs HTTP 模式**

```typescript
// Stdio 模式（开发调试时使用）
const server = new McpServer({ transport: 'stdio' })

// HTTP 模式（生产环境，支持多进程调用）
const server = new McpServer({
  transport: 'http',
  port: 3721,           // 本机回环，不对外暴露
  auth: localToken      // 本地随机 Token，防止其他程序调用
})
```

**WebSocket 长连接管理：**

```typescript
class CloudConnector {
  private ws: WebSocket
  private reconnectDelay = 1000

  connect() {
    this.ws = new WebSocket(`wss://api.agentforge.com/desktop/${deviceId}`)
    this.ws.on('message', this.handleAgentInstruction)
    this.ws.on('close', () => {
      // 指数退避重连，最大 30 秒
      setTimeout(() => this.connect(), Math.min(this.reconnectDelay *= 2, 30000))
    })
  }
}
```

### 12.6 安全设计

```
权限声明（tauri.conf.json）：
{
  "allowlist": {
    "fs": { "readFile": true, "writeFile": true, "scope": ["$DOCUMENT/*"] },
    "clipboard": { "all": true },
    "notification": { "all": true },
    "shell": { "execute": false }  // shell 默认关闭，需用户单独授权
  }
}

敏感路径黑名单（Rust 层强制拦截，JS 层无法绕过）：
- ~/.ssh
- ~/.*wallet, ~/.bitcoin
- 浏览器 Profile 目录（Keychain、Login Data）
- /etc, /proc, /sys（Linux/macOS）
- C:\Windows\System32（Windows）
```

---

## 13. 开发优先级

### P0：MVP（第一期上线必须）

- [ ] 管理控制台基础页面（创建 Agent 的对话式交互）
- [ ] 意图理解引擎（多轮澄清 + 需求结构化）
- [ ] Agent 生成引擎（meta-prompt 生成 Agent 定义）
- [ ] 飞书 Webhook 接入（消息收发 + 验签）
- [ ] Agent Runtime（Worker + 队列 + LLM 调用）
- [ ] 基础状态机（defined → active → running → active）
- [ ] 配置缓存（Redis 缓存 Agent 定义）
- [ ] 执行日志记录

### P1：第一个迭代

- [ ] 网站 Widget 嵌入（JS SDK + iframe）
- [ ] 向量知识库（pgvector + 上下文压缩 SubAgent）
- [ ] 完整状态机（idle → archived → 复活）
- [ ] 到期通知 + 一键续期
- [ ] 执行质量自动评分

### P2：第二个迭代

- [ ] Agent 克隆与模板化
- [ ] 管理控制台数据看板（执行量、质量趋势）
- [ ] 多租户权限隔离
- [ ] LLM 速率限制器（优先级队列 + 令牌桶）
- [ ] 私有化部署包生成（docker-compose 导出）
- [ ] **桌宠客户端 MVP**（Tauri + 剪贴板感知 + 本地 MCP Server + WebSocket 云端通信）

### P3：规模化

- [ ] Worker 弹性扩缩容
- [ ] Agent 协作（多 Agent 互相调用）
- [ ] 更多渠道（钉钉、企微、Slack）
- [ ] Agent 市场（团队内部共享模板）
- [ ] **桌宠能力扩展**（文件拖拽分析 · 截图感知 · 本地自动化脚本 · 文件夹监控触发）
- [ ] **桌宠企业版**（管理员统一下发规则 · 操作审计上报 · 与企业 SSO 集成）

---

## 附录

### Agent 定义示例（飞书技术答疑机器人）

```json
{
  "name": "Java技术答疑机器人",
  "description": "回答Java、Spring相关技术问题，被@或提问技术问题时触发",
  "system_prompt": "你是一个专业的Java和Spring技术专家，在飞书群里帮助团队成员解答技术问题。\n\n行为规范：\n- 回答要简洁直接，给出可运行的代码示例\n- 对于不确定的问题，明确说不确定，不要编造\n- 只回答Java、Spring、Maven、数据库相关的技术问题\n- 非技术问题礼貌拒绝并引导找相关人员\n\n回答格式：\n- 简要说明原因（1-2句）\n- 提供解决代码\n- 如有需要，说明注意事项",
  "tools": ["knowledge_query", "web_search"],
  "channels": [
    {
      "type": "feishu",
      "group_id": "oc_xxx",
      "trigger": "mention_or_question"
    }
  ],
  "lifecycle": {
    "trigger": "message",
    "idle_timeout": "48h",
    "auto_archive": true,
    "notify_before_expire": "1d"
  },
  "model": "claude-sonnet-4-6",
  "quality_criteria": "回答准确、有代码示例、不超过300字"
}
```

### 环境变量配置

```env
# LLM
CLAUDE_API_KEY=sk-ant-xxx

# 数据库
DATABASE_URL=postgresql://user:pass@localhost:5432/agentforge

# Redis
REDIS_URL=redis://localhost:6379

# 飞书
FEISHU_APP_ID=cli_xxx
FEISHU_APP_SECRET=xxx
FEISHU_ENCRYPT_KEY=xxx
FEISHU_VERIFICATION_TOKEN=xxx

# 平台
PORT=3000
WORKER_COUNT=4
LOG_LEVEL=info

# 桌宠客户端（客户端本地 .env，不上传云端）
DESKTOP_DEVICE_ID=<自动生成的设备唯一ID>
DESKTOP_API_ENDPOINT=wss://api.agentforge.com/desktop
DESKTOP_LOCAL_MCP_PORT=3721
DESKTOP_LOCAL_MCP_TOKEN=<本地随机Token>
DESKTOP_ALLOWED_DIRS=$HOME/Documents,$HOME/Downloads
```
