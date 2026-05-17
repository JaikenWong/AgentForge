import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';

interface Template {
  id: string;
  name: string;
  description: string;
  prompt: string;
  config?: string;
}

interface AgentCreated {
  id: number;
}

export default function TemplatesPage() {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    (async () => {
      try {
        const data = await api.get<Template[]>('/agents/templates');
        setTemplates(data);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const useTemplate = async (templateId: string) => {
    setCreating(templateId);
    try {
      const agent = await api.post<AgentCreated>('/agents/from-template', { templateId });
      navigate(`/agents/${agent.id}`);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : '创建失败');
    } finally {
      setCreating(null);
    }
  };

  if (loading) {
    return <div className="text-center py-12 text-cyan-400">加载模板…</div>;
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-gray-100 mb-2">Agent 模板库</h1>
        <p className="text-gray-400 text-sm">
          从行业模板一键创建草稿 Agent，可在详情页试聊、激活或克隆。飞书等渠道部署后续接入。
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {templates.map((t) => (
          <article
            key={t.id}
            className="rounded-xl border border-[#1f1f2e] bg-[#11111a]/60 p-6 flex flex-col"
          >
            <h2 className="text-lg font-medium text-gray-100 mb-2">{t.name}</h2>
            <p className="text-sm text-gray-400 flex-1 mb-4">{t.description}</p>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={creating === t.id}
                onClick={() => useTemplate(t.id)}
                className="px-4 py-2 rounded-lg bg-gradient-to-r from-cyan-600 to-purple-600 text-white text-sm disabled:opacity-50"
              >
                {creating === t.id ? '创建中…' : '使用模板'}
              </button>
              <Link
                to="/create"
                className="px-4 py-2 rounded-lg border border-[#1f1f2e] text-gray-400 text-sm hover:text-cyan-400"
              >
                对话定制
              </Link>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
