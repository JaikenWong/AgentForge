import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

interface Message {
  id: number;
  role: string;
  content: string;
  createdAt: string;
}

interface Conversation {
  id: number;
  type: string;
  status: string;
  agentId?: string;
  createdAt: string;
  updatedAt: string;
  messages?: Message[];
}

export default function ConversationsPage() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selected, setSelected] = useState<Conversation | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    load();
  }, []);

  const load = async () => {
    try {
      const data = await api.get<Conversation[]>('/conversations');
      setConversations(data);
    } finally {
      setLoading(false);
    }
  };

  const openConv = async (id: number) => {
    const conv = await api.get<Conversation>(`/conversations/${id}`);
    setSelected(conv);
  };

  if (loading) return <div className="text-center py-12 text-cyan-400">加载中...</div>;

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-purple-500 bg-clip-text text-transparent">
          对话历史
        </h1>
        <Link to="/create" className="px-4 py-2 rounded-lg bg-gradient-to-r from-cyan-600 to-purple-600 text-white text-sm font-medium hover:from-cyan-500 hover:to-purple-500 transition-all">
          新建对话
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-1 space-y-2">
          {conversations.length === 0 && (
            <div className="text-gray-500 text-sm p-4 rounded-lg border border-[#1f1f2e] bg-[#11111a]">
              暂无对话记录
            </div>
          )}
          {conversations.map((c) => (
            <button
              key={c.id}
              onClick={() => openConv(c.id)}
              className={`w-full text-left p-4 rounded-lg border transition-all ${
                selected?.id === c.id
                  ? 'border-cyan-500/60 bg-cyan-500/10 shadow-[0_0_15px_rgba(6,182,212,0.15)]'
                  : 'border-[#1f1f2e] bg-[#11111a] hover:border-cyan-500/30'
              }`}
            >
              <div className="flex justify-between items-center mb-1">
                <span className="text-sm font-medium text-gray-200">#{c.id}</span>
                <span
                  className={`text-xs px-2 py-0.5 rounded ${
                    c.status === 'completed'
                      ? 'text-green-400 bg-green-900/30'
                      : 'text-yellow-400 bg-yellow-900/30'
                  }`}
                >
                  {c.status}
                </span>
              </div>
              <div className="text-xs text-gray-500">{new Date(c.updatedAt).toLocaleString()}</div>
              {c.agentId && (
                <div className="mt-1 text-xs text-purple-400 truncate">Agent: {c.agentId}</div>
              )}
            </button>
          ))}
        </div>

        <div className="md:col-span-2">
          {selected ? (
            <div className="rounded-lg border border-[#1f1f2e] bg-[#11111a] p-6 max-h-[70vh] overflow-y-auto">
              <div className="mb-4 pb-4 border-b border-[#1f1f2e]">
                <h2 className="text-lg font-medium text-gray-200">对话 #{selected.id}</h2>
                <p className="text-xs text-gray-500 mt-1">
                  创建于 {new Date(selected.createdAt).toLocaleString()}
                </p>
              </div>
              <div className="space-y-4">
                {selected.messages?.map((m) => (
                  <div
                    key={m.id}
                    className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}
                  >
                    <div
                      className={`max-w-[80%] rounded-lg px-4 py-2 ${
                        m.role === 'user'
                          ? 'bg-gradient-to-r from-cyan-600 to-purple-600 text-white'
                          : 'bg-[#1f1f2e] text-gray-200 border border-[#333]'
                      }`}
                    >
                      <div className="whitespace-pre-wrap text-sm">{m.content}</div>
                      <div className="text-[10px] opacity-60 mt-1">
                        {new Date(m.createdAt).toLocaleTimeString()}
                      </div>
                    </div>
                  </div>
                ))}
                {(!selected.messages || selected.messages.length === 0) && (
                  <div className="text-center text-gray-500 text-sm">无消息记录</div>
                )}
              </div>
            </div>
          ) : (
            <div className="rounded-lg border border-dashed border-[#1f1f2e] p-12 text-center text-gray-500">
              选择左侧对话以查看详情
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
