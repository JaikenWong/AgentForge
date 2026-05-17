import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

interface Agent {
  id: number;
  uuid: string;
  name: string;
  description?: string;
  status: string;
  createdAt: string;
}

const STATUS_STYLES: Record<string, { dot: string; text: string; bg: string; border: string }> = {
  draft: { dot: 'bg-gray-400', text: 'text-gray-400', bg: 'bg-gray-900/30', border: 'border-gray-500/30' },
  active: { dot: 'bg-green-400', text: 'text-green-400', bg: 'bg-green-900/30', border: 'border-green-500/30' },
  running: { dot: 'bg-cyan-400', text: 'text-cyan-400', bg: 'bg-cyan-900/30', border: 'border-cyan-500/30' },
  idle: { dot: 'bg-yellow-400', text: 'text-yellow-400', bg: 'bg-yellow-900/30', border: 'border-yellow-500/30' },
  archived: { dot: 'bg-red-400', text: 'text-red-400', bg: 'bg-red-900/30', border: 'border-red-500/30' },
};

export default function AgentListPage() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string>('all');

  useEffect(() => {
    loadAgents();
  }, []);

  const loadAgents = async () => {
    try {
      const data = await api.get<Agent[]>('/agents');
      setAgents(data);
    } catch (error) {
      console.error('Failed to load agents:', error);
    } finally {
      setLoading(false);
    }
  };

  const filtered = filter === 'all' ? agents : agents.filter((a) => a.status === filter);
  const statuses = ['all', ...Array.from(new Set(agents.map((a) => a.status)))];

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex flex-wrap justify-between items-center gap-4 mb-8">
        <div>
          <div className="text-xs text-cyan-400 font-mono tracking-widest mb-1">AGENT REGISTRY</div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-purple-500 bg-clip-text text-transparent">
            Agent 列表
          </h1>
        </div>
        <Link
          to="/create"
          className="px-5 py-2.5 rounded-lg bg-gradient-to-r from-cyan-600 to-purple-600 text-white text-sm font-medium hover:from-cyan-500 hover:to-purple-500 shadow-lg shadow-cyan-900/30 transition-all flex items-center space-x-2"
        >
          <span>+</span>
          <span>新建 Agent</span>
        </Link>
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        {statuses.map((s) => (
          <button
            key={s}
            onClick={() => setFilter(s)}
            className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-all ${
              filter === s
                ? 'bg-cyan-500/20 border border-cyan-500/40 text-cyan-300'
                : 'border border-[#1f1f2e] text-gray-500 hover:text-gray-300 hover:border-[#333]'
            }`}
          >
            {s.toUpperCase()} {s !== 'all' && `(${agents.filter((a) => a.status === s).length})`}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="text-center py-20 text-cyan-400">加载中...</div>
      ) : filtered.length === 0 ? (
        <div className="rounded-xl border border-dashed border-[#1f1f2e] p-16 text-center">
          <div className="text-6xl mb-4 opacity-30">🤖</div>
          <div className="text-gray-500 mb-4">{agents.length === 0 ? '还没有创建任何 Agent' : '当前筛选下无 Agent'}</div>
          <Link to="/create" className="text-cyan-400 hover:text-cyan-300 text-sm">
            → 立即创建第一个 Agent
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((agent) => {
            const style = STATUS_STYLES[agent.status] || STATUS_STYLES.draft;
            return (
              <Link
                key={agent.id}
                to={`/agents/${agent.id}`}
                className="group relative rounded-xl border border-[#1f1f2e] bg-[#11111a]/60 backdrop-blur-sm p-5 hover:border-cyan-500/40 transition-all overflow-hidden"
              >
                <div className="absolute -top-20 -right-20 w-40 h-40 rounded-full bg-cyan-500/0 group-hover:bg-cyan-500/10 blur-3xl transition-all"></div>
                <div className="relative">
                  <div className="flex justify-between items-start mb-3">
                    <h3 className="text-lg font-medium text-gray-100 group-hover:text-cyan-300 transition-colors">
                      {agent.name}
                    </h3>
                    <span className={`flex items-center space-x-1.5 text-xs px-2 py-0.5 rounded ${style.bg} ${style.border} border ${style.text}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${style.dot} animate-pulse`}></span>
                      <span className="font-mono">{agent.status}</span>
                    </span>
                  </div>
                  {agent.description && (
                    <p className="text-gray-400 text-sm leading-relaxed line-clamp-2 mb-3">
                      {agent.description}
                    </p>
                  )}
                  <div className="flex justify-between items-center text-xs text-gray-600 font-mono">
                    <span className="truncate">{agent.uuid?.substring(0, 8)}</span>
                    <span>{new Date(agent.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}