import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleFeishuLogin = async () => {
    setError('');
    setLoading(true);
    try {
      const { url } = await api.get<{ url: string }>('/auth/feishu/url');
      window.location.href = url;
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '无法跳转飞书登录');
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await api.post<{ token: string; user: any }>('/auth/login', {
        username,
        password,
      });

      localStorage.setItem('token', response.token);
      navigate('/');
    } catch (err: any) {
      setError(err.message || '登录失败，请检查用户名和密码');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Elements */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-[20%] -left-[10%] w-[50%] h-[50%] rounded-full bg-cyan-500/10 blur-[100px]"></div>
        <div className="absolute top-[40%] -right-[10%] w-[40%] h-[40%] rounded-full bg-purple-500/10 blur-[100px]"></div>
        <div className="absolute -bottom-[10%] left-[20%] w-[30%] h-[30%] rounded-full bg-pink-500/10 blur-[100px]"></div>
      </div>

      <div className="w-full max-w-md glass-gradient rounded-2xl p-8 shadow-2xl border border-[#333] relative z-10">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-purple-500 bg-clip-text text-transparent mb-2">
            AgentForge
          </h1>
          <p className="text-gray-400 text-sm">欢迎回来，请登录您的账户</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {error && (
            <div className="p-3 rounded-lg bg-red-900/20 border border-red-500/30 text-red-400 text-sm">
              {error}
            </div>
          )}

          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-300">用户名</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="请输入用户名"
              className="w-full px-4 py-3 rounded-lg bg-[#0a0a12] border border-[#333] text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-cyan-500/50 focus:border-cyan-500 transition-all"
              required
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-300">密码</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="请输入密码"
              className="w-full px-4 py-3 rounded-lg bg-[#0a0a12] border border-[#333] text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-cyan-500/50 focus:border-cyan-500 transition-all"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 rounded-lg bg-gradient-to-r from-cyan-600 to-purple-600 text-white font-medium hover:from-cyan-500 hover:to-purple-500 transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-cyan-900/20 flex items-center justify-center space-x-2"
          >
            {loading ? (
              <>
                <div className="spinner w-5 h-5 border-2"></div>
                <span>登录中...</span>
              </>
            ) : (
              <span>登录</span>
            )}
          </button>
        </form>

        <div className="mt-6 text-center">
          <p className="text-gray-400 text-sm">
            还没有账户？{' '}
            <Link to="/register" className="text-cyan-400 hover:text-cyan-300 font-medium">
              立即注册
            </Link>
          </p>
        </div>

        <div className="mt-8 pt-6 border-t border-[#1f1f2e]">
          <div className="text-center">
            <p className="text-xs text-gray-500 mb-2">或使用以下方式登录</p>
            <button
              type="button"
              onClick={handleFeishuLogin}
              disabled={loading}
              className="px-6 py-2 rounded-lg border border-[#333] hover:border-cyan-500/50 hover:text-cyan-400 transition-colors flex items-center justify-center space-x-2 mx-auto disabled:opacity-50"
            >
              <span>飞书登录</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
