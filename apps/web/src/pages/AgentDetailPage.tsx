import { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api from '../services/api';
import AgentChatPanel from '../components/AgentChatPanel';
import AgentFeishuDeploy from '../components/AgentFeishuDeploy';

interface ExecutionLog {
  id: number;
  channel: string;
  inputText: string;
  outputText?: string;
  status: string;
  latencyMs?: number;
  errorMessage?: string;
  createdAt: string;
}

interface Agent {
  id: number;
  uuid: string;
  userId: number;
  username?: string;
  tenantId: number;
  name: string;
  description?: string;
  prompt: string;
  config?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

const STATUS_STYLES: Record<string, { dot: string; text: string; bg: string; border: string }> = {
  draft: { dot: 'bg-gray-400', text: 'text-gray-400', bg: 'bg-gray-900/30', border: 'border-gray-500/30' },
  active: { dot: 'bg-green-400', text: 'text-green-400', bg: 'bg-green-900/30', border: 'border-green-500/30' },
  running: { dot: 'bg-cyan-400', text: 'text-cyan-400', bg: 'bg-cyan-900/30', border: 'border-cyan-500/30' },
  idle: { dot: 'bg-yellow-400', text: 'text-yellow-400', bg: 'bg-yellow-900/30', border: 'border-yellow-500/30' },
  archived: { dot: 'bg-red-400', text: 'text-red-400', bg: 'bg-red-900/30', border: 'border-red-500/30' },
};

export default function AgentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [agent, setAgent] = useState<Agent | null>(null);
  const [logs, setLogs] = useState<ExecutionLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const loadLogs = useCallback(async (agentId: string) => {
    try {
      const data = await api.get<ExecutionLog[]>(`/agents/${agentId}/logs?limit=20`);
      setLogs(data);
    } catch {
      setLogs([]);
    }
  }, []);

  useEffect(() => {
    if (id) loadAgent(id);
  }, [id]);

  const loadAgent = async (agentId: string) => {
    try {
      const data = await api.get<Agent>(`/agents/${agentId}`);
      setAgent(data);
      await loadLogs(agentId);
    } catch (err: any) {
      setError(err.message || '加载失败');
    } finally {
      setLoading(false);
    }
  };

  const setStatus = async (status: string) => {
    if (!id || !agent) return;
    try {
      const updated = await api.put<Agent>(`/agents/${id}`, {
        name: agent.name,
        description: agent.description,
        prompt: agent.prompt,
        config: agent.config,
        status,
      });
      setAgent(updated);
    } catch (err: any) {
      alert(err.message || '操作失败');
    }
  };

  const cloneAgent = async () => {
    if (!id) return;
    try {
      const cloned = await api.post<Agent>(`/agents/${id}/clone`);
      navigate(`/agents/${cloned.id}`);
    } catch (err: any) {
      alert(err.message || '克隆失败');
    }
  };

  const remove = async () => {
    if (!id) return;
    if (!confirm('确认删除该 Agent？此操作不可恢复。')) return;
    try {
      await api.delete(`/agents/${id}`);
      navigate('/agents');
    } catch (err: any) {
      alert(err.message || '删除失败');
    }
  };

  if (loading) return <div className="text-center py-12 text-cyan-400">加载中...</div>;
  if (error || !agent) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12 text-center">
        <div className="text-gray-500 mb-4">{error || 'Agent 不存在'}</div>
        <Link to="/agents" className="text-cyan-400 hover:text-cyan-300">← 返回列表</Link>
      </div>
    );
  }

  const style = STATUS_STYLES[agent.status] || STATUS_STYLES.draft;
  let tools: string[] = [];
  try {
    if (agent.config) tools = JSON.parse(agent.config).tools || [];
  } catch {}

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center space-x-4">
          <Link to="/agents" className="text-gray-500 hover:text-cyan-400 transition-colors">
            ← 返回
          </Link>
          <div>
            <div className="flex items-center space-x-3 mb-1">
              <h1 className="text-2xl font-bold text-gray-100">{agent.name}</h1>
              <span className={`flex items-center space-x-1.5 text-xs px-2 py-1 rounded ${style.bg} ${style.border} border ${style.text}`}>
                <span className={`w-1.5 h-1.5 rounded-full ${style.dot} animate-pulse`}></span>
                <span className="font-mono">{agent.status}</span>
              </span>
            </div>
            <div className="text-xs text-gray-500 font-mono">UUID · {agent.uuid}</div>
          </div>
        </div>

        <div className="flex space-x-2">
          {agent.status === 'draft' && (
            <button
              onClick={() => setStatus('active')}
              className="px-4 py-2 rounded-lg bg-gradient-to-r from-green-600 to-emerald-600 text-white text-sm hover:from-green-500 hover:to-emerald-500"
            >
              激活
            </button>
          )}
          {agent.status === 'active' && (
            <button
              onClick={() => setStatus('archived')}
              className="px-4 py-2 rounded-lg border border-yellow-500/40 text-yellow-400 text-sm hover:bg-yellow-500/10"
            >
              归档
            </button>
          )}
          {agent.status === 'archived' && (
            <button
              onClick={() => setStatus('active')}
              className="px-4 py-2 rounded-lg bg-gradient-to-r from-cyan-600 to-purple-600 text-white text-sm hover:from-cyan-500 hover:to-purple-500"
            >
              复活
            </button>
          )}
          <button
            onClick={cloneAgent}
            className="px-4 py-2 rounded-lg border border-cyan-500/40 text-cyan-400 text-sm hover:bg-cyan-500/10"
          >
            克隆
          </button>
          <Link
            to={`/embed/${id}`}
            className="px-4 py-2 rounded-lg border border-purple-500/40 text-purple-300 text-sm hover:bg-purple-500/10"
          >
            Widget
          </Link>
          <button
            onClick={remove}
            className="px-4 py-2 rounded-lg border border-red-500/40 text-red-400 text-sm hover:bg-red-500/10"
          >
            删除
          </button>
        </div>
      </div>

      <AgentChatPanel
        agentId={agent.id}
        agentName={agent.name}
        disabled={agent.status === 'archived'}
      />

      <AgentFeishuDeploy
        agentId={agent.id}
        archived={agent.status === 'archived'}
        onDeployed={() => id && loadAgent(id)}
      />

      <div className="rounded-xl border border-[#1f1f2e] bg-[#11111a]/60 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-medium text-gray-200">执行日志</h2>
          <button type="button" onClick={() => id && loadLogs(id)} className="text-xs text-cyan-400 hover:underline">
            刷新
          </button>
        </div>
        {logs.length === 0 ? (
          <p className="text-sm text-gray-500">暂无执行记录，试聊后将在此留痕。</p>
        ) : (
          <ul className="space-y-3 max-h-64 overflow-y-auto">
            {logs.map((log) => (
              <li key={log.id} className="text-xs border border-[#1f1f2e] rounded-lg p-3 bg-[#0a0a12]">
                <div className="flex justify-between text-gray-500 mb-1">
                  <span>{log.channel} · {log.status}</span>
                  <span>{log.latencyMs != null ? `${log.latencyMs}ms` : ''}</span>
                </div>
                <p className="text-gray-400 truncate">→ {log.inputText}</p>
                {log.outputText && <p className="text-gray-500 truncate mt-1">← {log.outputText}</p>}
                {log.errorMessage && <p className="text-red-400 mt-1">{log.errorMessage}</p>}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-xl border border-[#1f1f2e] bg-[#11111a]/60 backdrop-blur-sm p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-medium text-gray-200">System Prompt</h2>
              <span className="text-xs text-gray-500 font-mono">{agent.prompt?.length || 0} chars</span>
            </div>
            <pre className="text-sm text-gray-300 bg-[#0a0a12] border border-[#1f1f2e] p-4 rounded-lg whitespace-pre-wrap font-mono leading-relaxed max-h-[500px] overflow-y-auto">
              {agent.prompt}
            </pre>
          </div>

          {tools.length > 0 && (
            <div className="rounded-xl border border-[#1f1f2e] bg-[#11111a]/60 backdrop-blur-sm p-6">
              <h2 className="text-lg font-medium text-gray-200 mb-4">工具白名单</h2>
              <div className="flex flex-wrap gap-2">
                {tools.map((t) => (
                  <span
                    key={t}
                    className="px-3 py-1.5 rounded-lg font-mono text-sm bg-cyan-900/20 border border-cyan-500/30 text-cyan-300"
                  >
                    {t}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="space-y-6">
          <div className="rounded-xl border border-[#1f1f2e] bg-[#11111a]/60 backdrop-blur-sm p-6">
            <h2 className="text-lg font-medium text-gray-200 mb-4">元信息</h2>
            <dl className="space-y-3 text-sm">
              <div>
                <dt className="text-xs text-gray-500 uppercase tracking-wider mb-0.5">描述</dt>
                <dd className="text-gray-300">{agent.description || '-'}</dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase tracking-wider mb-0.5">创建人</dt>
                <dd className="text-gray-300">{agent.username || `user#${agent.userId}`}</dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase tracking-wider mb-0.5">租户 ID</dt>
                <dd className="text-gray-300 font-mono">{agent.tenantId}</dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase tracking-wider mb-0.5">创建时间</dt>
                <dd className="text-gray-300">{new Date(agent.createdAt).toLocaleString()}</dd>
              </div>
              <div>
                <dt className="text-xs text-gray-500 uppercase tracking-wider mb-0.5">更新时间</dt>
                <dd className="text-gray-300">{new Date(agent.updatedAt).toLocaleString()}</dd>
              </div>
            </dl>
          </div>

          {agent.config && (
            <div className="rounded-xl border border-[#1f1f2e] bg-[#11111a]/60 backdrop-blur-sm p-6">
              <h2 className="text-lg font-medium text-gray-200 mb-4">配置 (JSON)</h2>
              <pre className="text-xs text-gray-300 bg-[#0a0a12] border border-[#1f1f2e] p-3 rounded font-mono overflow-x-auto">
                {(() => {
                  try {
                    return JSON.stringify(JSON.parse(agent.config), null, 2);
                  } catch {
                    return agent.config;
                  }
                })()}
              </pre>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
