import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../services/api';
import AgentChatPanel from '../components/AgentChatPanel';

interface Agent {
  id: number;
  name: string;
  status: string;
}

export default function EmbedAgentPage() {
  const { id } = useParams<{ id: string }>();
  const [agent, setAgent] = useState<Agent | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!id) return;
    (async () => {
      try {
        const data = await api.get<Agent>(`/agents/${id}`);
        setAgent(data);
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : '加载失败');
      }
    })();
  }, [id]);

  if (error) {
    return (
      <div className="min-h-screen bg-[#0a0a12] flex items-center justify-center text-gray-400 p-4">
        {error} · <Link to="/login" className="text-cyan-400 ml-2">登录</Link>
      </div>
    );
  }

  if (!agent) {
    return <div className="min-h-screen bg-[#0a0a12] flex items-center justify-center text-cyan-400">加载中…</div>;
  }

  return (
    <div className="min-h-screen bg-[#0a0a12] p-4 max-w-lg mx-auto">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-lg font-medium text-gray-100">{agent.name}</h1>
        <Link to={`/agents/${agent.id}`} className="text-xs text-cyan-400 hover:underline">
          完整详情
        </Link>
      </div>
      <AgentChatPanel
        agentId={agent.id}
        agentName={agent.name}
        disabled={agent.status === 'archived'}
      />
      <p className="mt-3 text-xs text-gray-600 text-center">
        Widget 试聊 · 需登录 · 对外嵌入 API 见 POST /api/agents/{'{id}'}/invoke
      </p>
    </div>
  );
}
