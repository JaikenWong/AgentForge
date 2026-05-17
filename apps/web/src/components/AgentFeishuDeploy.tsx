import { useCallback, useEffect, useState } from 'react';
import api from '../services/api';

interface Deployment {
  agentId: number;
  tenantId: number;
  channel: string;
  chatId: string;
}

interface BindCode {
  bindCode: string;
  command: string;
  agentId: number;
  agentName: string;
  expiresAt: string;
  expiresInMinutes: number;
}

interface RuntimeStatus {
  feishuAppConfigured: boolean;
  feishuEncryptKeyConfigured: boolean;
  queueDepth: number;
  webhookPath: string;
  webhookUrl?: string;
}

interface Props {
  agentId: number;
  archived: boolean;
  onDeployed?: () => void;
}

export default function AgentFeishuDeploy({ agentId, archived, onDeployed }: Props) {
  const [deployments, setDeployments] = useState<Deployment[]>([]);
  const [bindCode, setBindCode] = useState<BindCode | null>(null);
  const [runtime, setRuntime] = useState<RuntimeStatus | null>(null);
  const [loadingCode, setLoadingCode] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [manualChatId, setManualChatId] = useState('');
  const [manualLoading, setManualLoading] = useState(false);

  const webhookUrl = runtime?.webhookUrl || `${window.location.origin}/webhook/feishu`;

  const loadDeployments = useCallback(async () => {
    try {
      const data = await api.get<Deployment[]>(`/agents/${agentId}/deployments`);
      setDeployments(data);
      if (data.length > 0) {
        onDeployed?.();
      }
    } catch {
      setDeployments([]);
    }
  }, [agentId, onDeployed]);

  const loadRuntime = async () => {
    try {
      setRuntime(await api.get<RuntimeStatus>('/runtime/status'));
    } catch {
      setRuntime(null);
    }
  };

  const generateBindCode = async () => {
    setLoadingCode(true);
    try {
      const data = await api.post<BindCode>(`/agents/${agentId}/deploy/feishu/bind-code`);
      setBindCode(data);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : '生成绑定码失败');
    } finally {
      setLoadingCode(false);
    }
  };

  useEffect(() => {
    loadDeployments();
    loadRuntime();
    if (!archived) {
      generateBindCode();
    }
  }, [agentId, archived]);

  useEffect(() => {
    if (!bindCode || deployments.length > 0) return;
    const timer = setInterval(loadDeployments, 5000);
    return () => clearInterval(timer);
  }, [bindCode, deployments.length, loadDeployments]);

  const copyText = async (text: string, label: string) => {
    await navigator.clipboard.writeText(text);
    alert(`${label}已复制`);
  };

  const manualDeploy = async () => {
    if (!manualChatId.trim()) return;
    setManualLoading(true);
    try {
      await api.post(`/agents/${agentId}/deploy/feishu`, { chatId: manualChatId.trim() });
      setManualChatId('');
      await loadDeployments();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : '绑定失败');
    } finally {
      setManualLoading(false);
    }
  };

  const undeploy = async (chatId: string) => {
    try {
      await api.delete(`/agents/${agentId}/deploy/feishu?chatId=${encodeURIComponent(chatId)}`);
      await loadDeployments();
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : '解绑失败');
    }
  };

  return (
    <section className="rounded-xl border border-[#1f1f2e] bg-[#11111a]/60 p-6 space-y-5">
      <div>
        <h2 className="text-lg font-medium text-gray-200">渠道部署 · 飞书</h2>
        <p className="text-xs text-gray-500 mt-1">
          企业只需配置一次飞书机器人；你只需把机器人拉进群并发送绑定口令，无需填写任何群 ID。
        </p>
      </div>

      <ol className="text-sm text-gray-300 space-y-2 list-decimal list-inside">
        <li>请管理员确保飞书事件订阅已指向下方 Webhook</li>
        <li>将<strong className="text-gray-200">企业机器人</strong>拉入目标群</li>
        <li>在群内发送下方绑定口令（30 分钟内有效）</li>
        <li>绑定成功后，在群内 @机器人 即可对话</li>
      </ol>

      <div className="text-xs space-y-2 bg-[#0a0a12] border border-[#1f1f2e] rounded-lg p-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-gray-500 shrink-0">Webhook</span>
          <code className="text-cyan-300 break-all flex-1 text-[11px]">{webhookUrl}</code>
          <button type="button" onClick={() => copyText(webhookUrl, 'Webhook')} className="text-cyan-400 hover:underline text-xs">
            复制
          </button>
        </div>
        <div className="flex flex-wrap gap-3 text-gray-500">
          <span>应用 {runtime?.feishuAppConfigured ? '✓' : '✗'}</span>
          <span>Encrypt Key {runtime?.feishuEncryptKeyConfigured ? '✓' : '✗'}</span>
        </div>
      </div>

      {bindCode && (
        <div className="rounded-lg border border-cyan-500/30 bg-cyan-950/20 p-4 space-y-3">
          <p className="text-xs text-gray-500">在群内发送以下消息完成绑定</p>
          <div className="flex flex-wrap items-center gap-3">
            <code className="text-2xl font-bold tracking-widest text-cyan-300">{bindCode.command}</code>
            <button
              type="button"
              onClick={() => copyText(bindCode.command, '绑定口令')}
              className="px-3 py-1.5 rounded-lg border border-cyan-500/40 text-cyan-300 text-sm hover:bg-cyan-500/10"
            >
              复制口令
            </button>
            <button
              type="button"
              disabled={loadingCode || archived}
              onClick={generateBindCode}
              className="px-3 py-1.5 rounded-lg text-gray-400 text-sm hover:text-cyan-400"
            >
              {loadingCode ? '生成中…' : '刷新绑定码'}
            </button>
          </div>
          <p className="text-xs text-gray-500">
            绑定 Agent：<span className="text-gray-300">{bindCode.agentName}</span> · {bindCode.expiresInMinutes} 分钟内有效
            {deployments.length === 0 && (
              <span className="text-yellow-500/90 ml-2">等待群内发送口令…</span>
            )}
          </p>
        </div>
      )}

      {deployments.length > 0 && (
        <div className="space-y-2">
          <h3 className="text-sm font-medium text-green-400">已绑定群聊</h3>
          <ul className="space-y-2">
            {deployments.map((d) => (
              <li
                key={d.chatId}
                className="flex items-center justify-between text-sm border border-green-500/20 rounded-lg px-3 py-2 bg-green-950/10"
              >
                <span className="text-gray-400">
                  群已连接 <span className="font-mono text-green-300/80 text-xs ml-1">{d.chatId}</span>
                </span>
                <button type="button" onClick={() => undeploy(d.chatId)} className="text-xs text-red-400 hover:underline">
                  解绑
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      <details className="text-sm" open={showAdvanced} onToggle={(e) => setShowAdvanced(e.currentTarget.open)}>
        <summary className="text-gray-500 cursor-pointer hover:text-gray-400">管理员：手动填写 chat_id</summary>
        <div className="mt-3 flex flex-wrap gap-2">
          <input
            value={manualChatId}
            onChange={(e) => setManualChatId(e.target.value)}
            placeholder="oc_xxxxxxxx（仅管理员）"
            className="flex-1 min-w-[200px] bg-[#0a0a12] border border-[#1f1f2e] rounded-lg px-3 py-2 text-sm text-gray-200 font-mono"
          />
          <button
            type="button"
            disabled={manualLoading || archived}
            onClick={manualDeploy}
            className="px-4 py-2 rounded-lg border border-gray-600 text-gray-300 text-sm disabled:opacity-50"
          >
            手动绑定
          </button>
        </div>
      </details>
    </section>
  );
}
