import { useEffect, useState } from 'react';
import api from '../services/api';

interface Invitation {
  id: number;
  code: string;
  tenantId: number;
  createdBy: number;
  usedBy?: number;
  expiresAt?: string;
  usedAt?: string;
  createdAt: string;
  description?: string;
}

export default function AdminPage() {
  const [invitations, setInvitations] = useState<Invitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [description, setDescription] = useState('');
  const [daysValid, setDaysValid] = useState(7);
  const [error, setError] = useState('');

  useEffect(() => {
    load();
  }, []);

  const load = async () => {
    try {
      const data = await api.get<Invitation[]>('/invitations');
      setInvitations(data);
    } catch (e: any) {
      setError(e.message || '加载失败');
    } finally {
      setLoading(false);
    }
  };

  const create = async () => {
    setCreating(true);
    setError('');
    try {
      await api.post<Invitation>('/invitations', { description, daysValid });
      setDescription('');
      await load();
    } catch (e: any) {
      setError(e.message || '创建失败');
    } finally {
      setCreating(false);
    }
  };

  const remove = async (id: number) => {
    if (!confirm('确认删除该邀请码？')) return;
    await api.delete(`/invitations/${id}`);
    await load();
  };

  const copyCode = (code: string) => {
    navigator.clipboard.writeText(code);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold bg-gradient-to-r from-purple-400 to-pink-500 bg-clip-text text-transparent">
          管理后台
        </h1>
        <div className="text-xs text-gray-500 font-mono">ADMIN · INVITATION CONTROL</div>
      </div>

      {error && (
        <div className="p-3 rounded-lg bg-red-900/20 border border-red-500/30 text-red-400 text-sm">
          {error}
        </div>
      )}

      <div className="rounded-lg border border-[#1f1f2e] bg-[#11111a] p-6">
        <h2 className="text-lg font-medium text-gray-200 mb-4">生成邀请码</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="md:col-span-2">
            <label className="text-sm text-gray-400 mb-1 block">说明</label>
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="例如：销售部新人 5月"
              className="w-full px-4 py-2 rounded-lg bg-[#0a0a12] border border-[#333] text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
            />
          </div>
          <div>
            <label className="text-sm text-gray-400 mb-1 block">有效期 (天)</label>
            <input
              type="number"
              min={1}
              max={365}
              value={daysValid}
              onChange={(e) => setDaysValid(Number(e.target.value))}
              className="w-full px-4 py-2 rounded-lg bg-[#0a0a12] border border-[#333] text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
            />
          </div>
        </div>
        <button
          onClick={create}
          disabled={creating}
          className="mt-4 px-6 py-2 rounded-lg bg-gradient-to-r from-purple-600 to-pink-600 text-white text-sm font-medium hover:from-purple-500 hover:to-pink-500 disabled:opacity-50"
        >
          {creating ? '生成中...' : '生成邀请码'}
        </button>
      </div>

      <div className="rounded-lg border border-[#1f1f2e] bg-[#11111a] overflow-hidden">
        <div className="px-6 py-4 border-b border-[#1f1f2e] flex justify-between items-center">
          <h2 className="text-lg font-medium text-gray-200">邀请码列表</h2>
          <span className="text-xs text-gray-500">{invitations.length} 条记录</span>
        </div>
        {loading ? (
          <div className="p-12 text-center text-cyan-400">加载中...</div>
        ) : invitations.length === 0 ? (
          <div className="p-12 text-center text-gray-500">暂无邀请码</div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-[#0a0a12] text-gray-400">
              <tr>
                <th className="px-4 py-3 text-left">邀请码</th>
                <th className="px-4 py-3 text-left">说明</th>
                <th className="px-4 py-3 text-left">状态</th>
                <th className="px-4 py-3 text-left">过期时间</th>
                <th className="px-4 py-3 text-left">操作</th>
              </tr>
            </thead>
            <tbody>
              {invitations.map((inv) => {
                const used = !!inv.usedBy;
                return (
                  <tr key={inv.id} className="border-t border-[#1f1f2e] hover:bg-[#1f1f2e]/30">
                    <td className="px-4 py-3 font-mono text-cyan-400">{inv.code}</td>
                    <td className="px-4 py-3 text-gray-300">{inv.description || '-'}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`text-xs px-2 py-1 rounded ${
                          used
                            ? 'bg-gray-800 text-gray-400'
                            : 'bg-green-900/30 text-green-400 border border-green-500/30'
                        }`}
                      >
                        {used ? '已使用' : '未使用'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-400">
                      {inv.expiresAt ? new Date(inv.expiresAt).toLocaleDateString() : '永久'}
                    </td>
                    <td className="px-4 py-3 space-x-2">
                      <button
                        onClick={() => copyCode(inv.code)}
                        className="text-cyan-400 hover:text-cyan-300 text-xs"
                      >
                        复制
                      </button>
                      <button
                        onClick={() => remove(inv.id)}
                        className="text-red-400 hover:text-red-300 text-xs"
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
