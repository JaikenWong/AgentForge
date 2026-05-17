import { useState, useRef, useEffect } from 'react';
import api from '../services/api';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

interface ChatResult {
  reply: string;
  logId: number;
  latencyMs: number;
}

interface Props {
  agentId: number;
  agentName: string;
  disabled?: boolean;
  channel?: 'web' | 'api';
}

export default function AgentChatPanel({ agentId, agentName, disabled, channel = 'web' }: Props) {
  const [messages, setMessages] = useState<ChatMessage[]>([
    { role: 'assistant', content: `已连接「${agentName}」。输入消息试聊，回复会写入执行日志。` },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const send = async () => {
    const text = input.trim();
    if (!text || loading || disabled) return;

    const history = messages
      .filter((m) => m.role === 'user' || m.role === 'assistant')
      .slice(-10)
      .map((m) => ({ role: m.role, content: m.content }));

    setInput('');
    setMessages((prev) => [...prev, { role: 'user', content: text }]);
    setLoading(true);

    try {
      const path = channel === 'api' ? `/agents/${agentId}/invoke` : `/agents/${agentId}/chat`;
      const result = await api.post<ChatResult>(path, { message: text, history });
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: result.reply || '（空回复）' },
      ]);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '请求失败';
      setMessages((prev) => [...prev, { role: 'assistant', content: `错误：${msg}` }]);
    } finally {
      setLoading(false);
    }
  };

  const Box = 'div' as const;
  const Header = Box;
  const Loading = Box;

  return (
    <Box className="rounded-xl border border-[#1f1f2e] bg-[#11111a]/60 backdrop-blur-sm flex flex-col h-[420px]">
      <Header className="px-4 py-3 border-b border-[#1f1f2e] flex justify-between items-center">
        <span className="text-sm font-medium text-gray-200">试聊 · {channel === 'api' ? 'API 通道' : 'Web'}</span>
        {disabled && <span className="text-xs text-yellow-400">已归档，请先复活</span>}
      </Header>
      <Box className="flex-1 overflow-y-auto p-4 space-y-3">
        {messages.map((m, i) => (
          <Box
            key={i}
            className={`text-sm rounded-lg px-3 py-2 max-w-[90%] whitespace-pre-wrap ${
              m.role === 'user'
                ? 'ml-auto bg-cyan-900/40 border border-cyan-500/30 text-cyan-100'
                : 'bg-[#0a0a12] border border-[#1f1f2e] text-gray-300'
            }`}
          >
            {m.content}
          </Box>
        ))}
        {loading && <Loading className="text-xs text-gray-500">思考中…</Loading>}
        <Box ref={endRef as never} />
      </Box>
      <Box className="p-3 border-t border-[#1f1f2e] flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), send())}
          disabled={disabled || loading}
          placeholder={disabled ? 'Agent 已归档' : '输入消息…'}
          className="flex-1 bg-[#0a0a12] border border-[#1f1f2e] rounded-lg px-3 py-2 text-sm text-gray-200 placeholder-gray-600 focus:border-cyan-500/50 outline-none disabled:opacity-50"
        />
        <button
          type="button"
          onClick={send}
          disabled={disabled || loading || !input.trim()}
          className="px-4 py-2 rounded-lg bg-gradient-to-r from-cyan-600 to-purple-600 text-white text-sm disabled:opacity-40"
        >
          发送
        </button>
      </Box>
    </Box>
  );
}
