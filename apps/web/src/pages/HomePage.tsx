import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';

const STATS = [
  {
    label: 'Agent 创建',
    value: 12847,
    suffix: '+',
    text: 'text-cyan-400',
    glow: 'bg-cyan-500/10',
    glowHover: 'group-hover:bg-cyan-500/20',
    border: 'hover:border-cyan-500/40',
  },
  {
    label: '部署渠道',
    value: 36,
    suffix: '',
    text: 'text-purple-400',
    glow: 'bg-purple-500/10',
    glowHover: 'group-hover:bg-purple-500/20',
    border: 'hover:border-purple-500/40',
  },
  {
    label: '日处理消息',
    value: 248391,
    suffix: '+',
    text: 'text-pink-400',
    glow: 'bg-pink-500/10',
    glowHover: 'group-hover:bg-pink-500/20',
    border: 'hover:border-pink-500/40',
  },
  {
    label: '平均响应',
    value: 218,
    suffix: 'ms',
    text: 'text-emerald-400',
    glow: 'bg-emerald-500/10',
    glowHover: 'group-hover:bg-emerald-500/20',
    border: 'hover:border-emerald-500/40',
  },
];

const FEATURES = [
  {
    icon: '⚡',
    title: '对话即部署',
    desc: '描述目标，全自动生成 Agent 定义、配置工具权限、完成渠道部署',
    tags: ['NL → Agent', 'Auto Deploy'],
  },
  {
    icon: '🧠',
    title: '上下文变知识',
    desc: '群聊记录、网站内容自动提炼为结构化知识，注入 Agent 记忆',
    tags: ['RAG', 'Auto Knowledge'],
  },
  {
    icon: '🔄',
    title: '有生命周期',
    desc: '到期自动归档，知识永久沉淀，需要时一键复活，空闲时零成本',
    tags: ['Auto Archive', 'Zero Idle Cost'],
  },
  {
    icon: '🛡️',
    title: '最小权限原则',
    desc: '工具白名单 + 租户隔离 + JWT 鉴权，企业级安全可控',
    tags: ['JWT', 'Multi-Tenant'],
  },
  {
    icon: '🌐',
    title: '多渠道接入',
    desc: '飞书群、企业官网 Widget、桌宠客户端，一份配置多端运行',
    tags: ['Feishu', 'Web Widget'],
  },
  {
    icon: '📊',
    title: '可观测演进',
    desc: '执行日志全量留痕，对话效果实时评估，Agent 持续自我优化',
    tags: ['Tracing', 'Auto Eval'],
  },
];

const PIPELINE = [
  { step: '01', label: '描述目标', desc: '自然语言输入', icon: '💬' },
  { step: '02', label: '意图澄清', desc: 'LLM 多轮追问', icon: '🤖' },
  { step: '03', label: '配置生成', desc: 'Prompt + 工具', icon: '⚙️' },
  { step: '04', label: '渠道部署', desc: '飞书 / Web', icon: '🚀' },
  { step: '05', label: '运行进化', desc: '反馈学习', icon: '📈' },
];

const TECH_STACK = [
  'Java 21', 'Spring Boot 3', 'MySQL', 'PostgreSQL', 'Redis', 'Flyway',
  'React 18', 'TypeScript', 'Vite', 'Tailwind v4', 'JWT', 'BCrypt',
];

function CountUp({ target, suffix }: { target: number; suffix: string }) {
  const [value, setValue] = useState(0);
  useEffect(() => {
    const duration = 1600;
    const start = performance.now();
    let raf = 0;
    const tick = (t: number) => {
      const p = Math.min(1, (t - start) / duration);
      const eased = 1 - Math.pow(1 - p, 3);
      setValue(Math.floor(target * eased));
      if (p < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [target]);
  return (
    <span>
      {value.toLocaleString()}
      {suffix}
    </span>
  );
}

export default function HomePage() {
  return (
    <div className="relative">
      {/* Hero background grid */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute inset-0 bg-[linear-gradient(rgba(6,182,212,0.07)_1px,transparent_1px),linear-gradient(90deg,rgba(6,182,212,0.07)_1px,transparent_1px)] bg-[size:50px_50px] [mask-image:radial-gradient(ellipse_at_center,black_30%,transparent_70%)]"></div>
        <div className="absolute -top-40 left-1/2 -translate-x-1/2 w-[800px] h-[600px] rounded-full bg-cyan-500/10 blur-[120px]"></div>
        <div className="absolute top-[40%] -right-32 w-[500px] h-[500px] rounded-full bg-purple-500/10 blur-[120px]"></div>
        <div className="absolute bottom-0 -left-32 w-[400px] h-[400px] rounded-full bg-pink-500/10 blur-[120px]"></div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-12 relative">
        {/* HERO */}
        <section className="text-center py-20 relative">
          <div className="inline-flex items-center space-x-2 px-4 py-1.5 rounded-full border border-cyan-500/30 bg-cyan-500/5 mb-8">
            <div className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse shadow-[0_0_10px_rgba(6,182,212,0.8)]"></div>
            <span className="text-xs text-cyan-300 font-mono tracking-wider">SYSTEM ONLINE · v0.1.0</span>
          </div>

          <h1 className="text-6xl md:text-7xl font-bold mb-6 tracking-tight">
            <span className="bg-gradient-to-r from-cyan-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
              AgentForge
            </span>
          </h1>
          <p className="text-2xl text-gray-300 mb-4 font-light">
            对话驱动的企业 Agent 工厂平台
          </p>
          <p className="text-base text-gray-500 mb-10 max-w-2xl mx-auto leading-relaxed">
            描述目标 → 自动生成 Agent → 部署到飞书 / 网站 → 持续进化
            <br />
            <span className="text-gray-600">用户感知不到"在配置 Agent"，只感知到"目标被达成"。</span>
          </p>

          <div className="flex flex-wrap justify-center gap-4">
            <Link
              to="/create"
              className="group relative px-8 py-3.5 rounded-lg bg-gradient-to-r from-cyan-600 to-purple-600 text-white font-medium overflow-hidden shadow-lg shadow-cyan-900/40 hover:shadow-cyan-500/40 transition-all"
            >
              <span className="relative z-10 flex items-center space-x-2">
                <span>立即创建 Agent</span>
                <span>→</span>
              </span>
              <div className="absolute inset-0 bg-gradient-to-r from-purple-600 to-pink-600 opacity-0 group-hover:opacity-100 transition-opacity"></div>
            </Link>
            <Link
              to="/agents"
              className="px-8 py-3.5 rounded-lg border border-[#333] hover:border-cyan-500/50 text-gray-300 hover:text-cyan-400 transition-all"
            >
              查看 Agent 列表
            </Link>
          </div>
        </section>

        {/* STATS */}
        <section className="grid grid-cols-2 md:grid-cols-4 gap-4 my-16">
          {STATS.map((s) => (
            <div
              key={s.label}
              className={`group relative rounded-xl border border-[#1f1f2e] bg-[#11111a]/60 backdrop-blur-sm p-6 ${s.border} transition-all overflow-hidden`}
            >
              <div className={`absolute -top-20 -right-20 w-40 h-40 rounded-full ${s.glow} blur-3xl ${s.glowHover} transition-all`}></div>
              <div className="relative">
                <div className={`text-3xl md:text-4xl font-bold ${s.text} tabular-nums`}>
                  <CountUp target={s.value} suffix={s.suffix} />
                </div>
                <div className="text-sm text-gray-500 mt-2">{s.label}</div>
              </div>
            </div>
          ))}
        </section>

        {/* PIPELINE */}
        <section className="my-20">
          <div className="text-center mb-12">
            <div className="text-xs text-cyan-400 font-mono tracking-widest mb-2">PIPELINE</div>
            <h2 className="text-3xl font-bold text-gray-100">从一句话到上线 · 五步</h2>
          </div>
          <div className="relative">
            <div className="absolute top-12 left-[10%] right-[10%] h-px bg-gradient-to-r from-transparent via-cyan-500/40 to-transparent hidden md:block"></div>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-4 relative">
              {PIPELINE.map((p) => (
                <div key={p.step} className="text-center">
                  <div className="relative w-24 h-24 mx-auto mb-4">
                    <div className="absolute inset-0 rounded-full bg-gradient-to-br from-cyan-500/20 to-purple-500/20 blur-xl"></div>
                    <div className="relative w-full h-full rounded-full border border-cyan-500/30 bg-[#0a0a12] flex items-center justify-center text-3xl shadow-[0_0_20px_rgba(6,182,212,0.2)]">
                      {p.icon}
                    </div>
                  </div>
                  <div className="text-xs text-cyan-400 font-mono mb-1">{p.step}</div>
                  <div className="text-sm font-medium text-gray-200 mb-1">{p.label}</div>
                  <div className="text-xs text-gray-500">{p.desc}</div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* FEATURES */}
        <section className="my-20">
          <div className="text-center mb-12">
            <div className="text-xs text-purple-400 font-mono tracking-widest mb-2">CAPABILITIES</div>
            <h2 className="text-3xl font-bold text-gray-100">核心能力矩阵</h2>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {FEATURES.map((f) => (
              <div
                key={f.title}
                className="group relative rounded-xl border border-[#1f1f2e] bg-[#11111a]/60 backdrop-blur-sm p-6 hover:border-cyan-500/40 transition-all overflow-hidden"
              >
                <div className="absolute inset-0 bg-gradient-to-br from-cyan-500/0 to-purple-500/0 group-hover:from-cyan-500/5 group-hover:to-purple-500/5 transition-all"></div>
                <div className="relative">
                  <div className="text-4xl mb-3">{f.icon}</div>
                  <h3 className="text-lg font-medium text-gray-100 mb-2">{f.title}</h3>
                  <p className="text-sm text-gray-400 leading-relaxed mb-4">{f.desc}</p>
                  <div className="flex flex-wrap gap-1.5">
                    {f.tags.map((t) => (
                      <span
                        key={t}
                        className="px-2 py-0.5 rounded text-[10px] font-mono bg-cyan-900/20 border border-cyan-500/20 text-cyan-300"
                      >
                        {t}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* TERMINAL DEMO */}
        <section className="my-20">
          <div className="rounded-xl border border-[#1f1f2e] bg-[#0a0a12] overflow-hidden shadow-2xl">
            <div className="flex items-center px-4 py-2 bg-[#11111a] border-b border-[#1f1f2e]">
              <div className="flex space-x-2">
                <div className="w-3 h-3 rounded-full bg-red-500/60"></div>
                <div className="w-3 h-3 rounded-full bg-yellow-500/60"></div>
                <div className="w-3 h-3 rounded-full bg-green-500/60"></div>
              </div>
              <div className="ml-4 text-xs text-gray-500 font-mono">agentforge ~ live-session.log</div>
            </div>
            <div className="p-6 font-mono text-sm space-y-2 text-gray-300">
              <div><span className="text-cyan-400">user@forge</span><span className="text-gray-600">:~$</span> <span className="text-gray-200">forge create</span></div>
              <div className="text-gray-500">→ Initialize conversation #4821 · tenant=default · user=admin</div>
              <div className="text-purple-400">▌ "我要一个回答售前问题的飞书机器人"</div>
              <div className="text-gray-500">→ Intent: CREATE_AGENT · confidence=0.92</div>
              <div className="text-gray-500">→ Clarification needed · question="目标用户群是什么？"</div>
              <div className="text-purple-400">▌ "现有客户的售前咨询群"</div>
              <div className="text-cyan-400">✓ Generated agent · name="售前答疑助手" · tools=[search_doc, escalate_to_human]</div>
              <div className="text-emerald-400">✓ Deploy to feishu://group/abc123 · status=running</div>
              <div className="text-gray-500">→ Conversation #4821 · status=completed · 14.2s</div>
              <div className="flex items-center"><span className="text-cyan-400">user@forge</span><span className="text-gray-600">:~$</span> <span className="ml-1 w-2 h-4 bg-cyan-400 animate-pulse"></span></div>
            </div>
          </div>
        </section>

        {/* TECH STACK */}
        <section className="my-20">
          <div className="text-center mb-8">
            <div className="text-xs text-emerald-400 font-mono tracking-widest mb-2">TECH STACK</div>
            <h2 className="text-3xl font-bold text-gray-100">企业级技术栈</h2>
          </div>
          <div className="flex flex-wrap justify-center gap-2">
            {TECH_STACK.map((t) => (
              <span
                key={t}
                className="px-3 py-1.5 rounded-lg border border-[#1f1f2e] bg-[#11111a]/60 text-sm text-gray-300 hover:border-cyan-500/40 hover:text-cyan-400 transition-all font-mono"
              >
                {t}
              </span>
            ))}
          </div>
        </section>

        {/* CTA */}
        <section className="my-20">
          <div className="relative rounded-2xl border border-[#1f1f2e] bg-gradient-to-br from-[#11111a] to-[#0a0a12] p-12 text-center overflow-hidden">
            <div className="absolute -top-32 -right-32 w-96 h-96 rounded-full bg-cyan-500/10 blur-3xl"></div>
            <div className="absolute -bottom-32 -left-32 w-96 h-96 rounded-full bg-purple-500/10 blur-3xl"></div>
            <div className="relative">
              <h2 className="text-3xl md:text-4xl font-bold mb-4">
                <span className="bg-gradient-to-r from-cyan-400 to-purple-500 bg-clip-text text-transparent">
                  准备好让 AI 替你工作了吗？
                </span>
              </h2>
              <p className="text-gray-400 mb-8 max-w-lg mx-auto">
                注册账户、获取邀请码、用一句话生成属于你的 Agent。
              </p>
              <div className="flex flex-wrap justify-center gap-3">
                <Link
                  to="/register"
                  className="px-6 py-3 rounded-lg bg-gradient-to-r from-cyan-600 to-purple-600 text-white font-medium hover:from-cyan-500 hover:to-purple-500 shadow-lg shadow-cyan-900/30 transition-all"
                >
                  免费注册
                </Link>
                <Link
                  to="/login"
                  className="px-6 py-3 rounded-lg border border-[#333] hover:border-cyan-500/50 text-gray-300 hover:text-cyan-400 transition-all"
                >
                  已有账户登录
                </Link>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}