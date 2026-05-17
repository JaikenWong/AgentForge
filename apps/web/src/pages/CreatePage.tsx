import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  type?: 'question' | 'confirmation' | 'result';
}

interface ClarifyResult {
  goal: string;
  confidence: number;
  questions: string[];
  extracted: Record<string, string>;
}

interface AgentPreview {
  name: string;
  description: string;
  system_prompt: string;
  tools: string[];
}

interface ConversationDto {
  id: number;
  messages?: Array<{ id: number; role: string; content: string }>;
}

const WELCOME: Message = {
  id: 'welcome',
  role: 'assistant',
  content: '你好！我是 AgentForge。请告诉我你想创建什么样的 Agent？\n\n例如：\n• "我要一个回答售前问题的飞书机器人"\n• "帮我做个网站智能客服，能识别意图并升级到人工"',
};

export default function CreatePage() {
  const [messages, setMessages] = useState<Message[]>([WELCOME]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [agentPreview, setAgentPreview] = useState<AgentPreview | null>(null);
  const [step, setStep] = useState<'clarify' | 'confirm' | 'done'>('clarify');
  const [conversationId, setConversationId] = useState<number | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    (async () => {
      try {
        const conv = await api.post<ConversationDto>('/conversations', { type: 'create_agent' });
        setConversationId(conv.id);
      } catch (e) {
        console.error('init conversation failed', e);
      }
    })();
  }, []);

  const addMessage = (role: 'user' | 'assistant', content: string, type?: Message['type']) => {
    setMessages((prev) => [...prev, { id: Date.now().toString() + Math.random(), role, content, type }]);
  };

  const persistMessage = async (role: 'user' | 'assistant', content: string) => {
    if (!conversationId) return;
    try {
      await api.post(`/conversations/${conversationId}/messages`, { role, content });
    } catch (e) {
      console.error('persist message failed', e);
    }
  };

  const sendMessage = async () => {
    if (!input.trim() || loading) return;

    const userText = input;
    addMessage('user', userText);
    persistMessage('user', userText);
    setInput('');
    setLoading(true);

    try {
      const history = messages.slice(1).map((m) => ({ role: m.role, content: m.content }));

      if (step === 'clarify') {
        const result = await api.post<ClarifyResult>('/intent/clarify', {
          message: userText,
          history,
        });

        if (result.questions.length === 0) {
          setStep('confirm');
          const msg = `我理解了你的需求：\n\n**目标**：${result.goal}\n\n正在生成 Agent 配置...`;
          addMessage('assistant', msg, 'confirmation');
          persistMessage('assistant', msg);

          try {
            const generated = await api.post<AgentPreview>('/generator/generate', {
              goal: result.goal,
              extracted: result.extracted,
            });
            setAgentPreview(generated);
            const reply = `Agent 已生成！\n\n**名称**：${generated.name}\n**描述**：${generated.description}\n\n请确认是否创建此 Agent？`;
            addMessage('assistant', reply, 'result');
            persistMessage('assistant', reply);
          } catch (genErr) {
            const err = `生成失败：${genErr instanceof Error ? genErr.message : '未知错误'}，请重试。`;
            addMessage('assistant', err);
            persistMessage('assistant', err);
          }
        } else {
          const q = result.questions[0];
          addMessage('assistant', q, 'question');
          persistMessage('assistant', q);
        }
      }
    } catch (err) {
      const e = `请求失败：${err instanceof Error ? err.message : '未知错误'}`;
      addMessage('assistant', e);
      persistMessage('assistant', e);
    } finally {
      setLoading(false);
    }
  };

  const createAgent = async () => {
    if (!agentPreview) return;
    setLoading(true);
    try {
      const config = JSON.stringify({ tools: agentPreview.tools });
      const created = await api.post<{ id: number; uuid: string }>('/agents', {
        name: agentPreview.name,
        description: agentPreview.description,
        prompt: agentPreview.system_prompt,
        config,
      });
      if (conversationId) {
        await api.post(`/conversations/${conversationId}/attach-agent`, { agentUuid: created.uuid });
        await api.post(`/conversations/${conversationId}/complete`);
      }
      setStep('done');
      const ok = '✅ Agent 创建成功！3 秒后跳转到 Agent 列表...';
      addMessage('assistant', ok, 'result');
      persistMessage('assistant', ok);
      setTimeout(() => navigate('/agents'), 3000);
    } catch (err) {
      const e = `创建失败：${err instanceof Error ? err.message : '未知错误'}`;
      addMessage('assistant', e);
      persistMessage('assistant', e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-6">
      <div className="flex h-[calc(100vh-10rem)] gap-4">
        <div className={`flex flex-col rounded-2xl border border-[#1f1f2e] bg-[#11111a]/80 backdrop-blur-sm shadow-[0_0_30px_rgba(6,182,212,0.05)] ${agentPreview ? 'flex-1' : 'flex-1 max-w-4xl mx-auto'}`}>
          <div className="px-6 py-4 border-b border-[#1f1f2e] flex justify-between items-center">
            <div className="flex items-center space-x-3">
              <div className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse shadow-[0_0_10px_rgba(6,182,212,0.8)]"></div>
              <span className="text-sm font-medium text-gray-200">AgentForge · 对话生成</span>
            </div>
            <div className="text-xs text-gray-500 font-mono">
              {conversationId ? `CONV#${conversationId}` : 'INIT...'} · {step.toUpperCase()}
            </div>
          </div>

          <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[80%] rounded-2xl px-5 py-3 ${
                    message.role === 'user'
                      ? 'bg-gradient-to-r from-cyan-600 to-purple-600 text-white shadow-lg shadow-cyan-900/30'
                      : 'bg-[#1f1f2e]/70 text-gray-200 border border-[#333]'
                  }`}
                >
                  <div className="whitespace-pre-wrap text-sm leading-relaxed">{message.content}</div>
                </div>
              </div>
            ))}
            {loading && (
              <div className="flex justify-start">
                <div className="bg-[#1f1f2e]/70 border border-[#333] rounded-2xl px-5 py-3 text-cyan-400 text-sm flex items-center space-x-2">
                  <div className="flex space-x-1">
                    <div className="w-2 h-2 rounded-full bg-cyan-400 animate-bounce" style={{ animationDelay: '0ms' }}></div>
                    <div className="w-2 h-2 rounded-full bg-cyan-400 animate-bounce" style={{ animationDelay: '150ms' }}></div>
                    <div className="w-2 h-2 rounded-full bg-cyan-400 animate-bounce" style={{ animationDelay: '300ms' }}></div>
                  </div>
                  <span>思考中</span>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          <div className="border-t border-[#1f1f2e] px-6 py-4">
            <div className="flex space-x-3">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && sendMessage()}
                placeholder="描述你想要的 Agent..."
                className="flex-1 px-4 py-3 rounded-lg bg-[#0a0a12] border border-[#333] text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-cyan-500/50 focus:border-cyan-500 transition-all"
                disabled={loading || step === 'done'}
              />
              {step === 'confirm' && agentPreview ? (
                <button
                  onClick={createAgent}
                  disabled={loading}
                  className="px-6 py-3 rounded-lg bg-gradient-to-r from-green-600 to-emerald-600 text-white font-medium hover:from-green-500 hover:to-emerald-500 disabled:opacity-50 shadow-lg shadow-green-900/30"
                >
                  确认创建
                </button>
              ) : (
                <button
                  onClick={sendMessage}
                  disabled={loading || step === 'done'}
                  className="px-6 py-3 rounded-lg bg-gradient-to-r from-cyan-600 to-purple-600 text-white font-medium hover:from-cyan-500 hover:to-purple-500 disabled:opacity-50 shadow-lg shadow-cyan-900/30"
                >
                  发送
                </button>
              )}
            </div>
          </div>
        </div>

        {agentPreview && (
          <div className="w-[380px] rounded-2xl border border-[#1f1f2e] bg-[#11111a]/80 backdrop-blur-sm p-6 overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-medium bg-gradient-to-r from-cyan-400 to-purple-500 bg-clip-text text-transparent">
                Agent 预览
              </h3>
              <span className="text-xs px-2 py-1 rounded bg-cyan-900/30 border border-cyan-500/30 text-cyan-400 font-mono">
                DRAFT
              </span>
            </div>
            <div className="space-y-5">
              <div>
                <label className="text-xs text-gray-500 uppercase tracking-wider mb-1 block">名称</label>
                <p className="font-medium text-gray-100">{agentPreview.name}</p>
              </div>
              <div>
                <label className="text-xs text-gray-500 uppercase tracking-wider mb-1 block">描述</label>
                <p className="text-gray-300 text-sm leading-relaxed">{agentPreview.description}</p>
              </div>
              <div>
                <label className="text-xs text-gray-500 uppercase tracking-wider mb-2 block">工具白名单</label>
                <div className="flex flex-wrap gap-2">
                  {agentPreview.tools.map((tool) => (
                    <span
                      key={tool}
                      className="px-2 py-1 rounded text-xs font-mono bg-cyan-900/30 border border-cyan-500/30 text-cyan-300"
                    >
                      {tool}
                    </span>
                  ))}
                </div>
              </div>
              <div>
                <label className="text-xs text-gray-500 uppercase tracking-wider mb-1 block">System Prompt</label>
                <pre className="text-gray-300 text-xs whitespace-pre-wrap bg-[#0a0a12] border border-[#1f1f2e] p-3 rounded-lg max-h-64 overflow-y-auto font-mono leading-relaxed">
                  {agentPreview.system_prompt}
                </pre>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}