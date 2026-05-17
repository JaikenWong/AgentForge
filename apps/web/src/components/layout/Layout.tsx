import { useState, useEffect, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../services/api';

interface User {
  id: number;
  username: string;
  displayName: string;
  role: string;
  tenantId: number;
  avatarUrl: string;
}

export default function Layout({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const particles = useMemo(() =>
    [...Array(40)].map((_, i) => ({
      id: i,
      left: `${Math.random() * 100}%`,
      top: `${Math.random() * 100}%`,
      width: `${Math.random() * 4 + 2}px`,
      height: `${Math.random() * 4 + 2}px`,
      duration: `${Math.random() * 12 + 10}s`,
      delay: `${Math.random() * 5}s`,
    })), []);

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const user = await api.get<User>('/auth/me');
        setUser(user);
      } catch {
        localStorage.removeItem('token');
      }
    }
    setLoading(false);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setUser(null);
    navigate('/login');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#0a0a12] flex items-center justify-center">
        <div className="text-center">
          <div className="spinner mx-auto mb-4"></div>
          <p className="text-cyan-400">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full min-h-screen bg-[#0a0a12] relative flex flex-col">
      {/* Particle Background */}
      <div id="particles">
        {particles.map(p => (
          <div
            key={p.id}
            className="particle"
            style={{
              left: p.left,
              top: p.top,
              width: p.width,
              height: p.height,
              animationDuration: p.duration,
              animationDelay: p.delay,
            }}
          />
        ))}
      </div>

      {/* Tech grid backdrop */}
      <div className="tech-grid"></div>

      {/* Scanline Overlay */}
      <div className="scanlines"></div>

      <header className="glass sticky top-0 z-50 border-b border-[#1f1f2e]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            <div className="flex items-center space-x-4">
              <Link to="/" className="text-2xl font-bold bg-gradient-to-r from-cyan-400 to-purple-500 bg-clip-text text-transparent neon-text">
                AgentForge
              </Link>
              {user && (
                <nav className="hidden md:flex space-x-1">
                  <Link to="/" className="px-3 py-2 rounded-md text-sm font-medium text-gray-300 hover:text-cyan-400 hover:bg-[#1f1f2e] transition-colors">
                    首页
                  </Link>
                  <Link to="/agents" className="px-3 py-2 rounded-md text-sm font-medium text-gray-300 hover:text-cyan-400 hover:bg-[#1f1f2e] transition-colors">
                    Agent 列表
                  </Link>
                  <Link to="/create" className="px-3 py-2 rounded-md text-sm font-medium text-gray-300 hover:text-cyan-400 hover:bg-[#1f1f2e] transition-colors">
                    创建 Agent
                  </Link>
                  <Link to="/conversations" className="px-3 py-2 rounded-md text-sm font-medium text-gray-300 hover:text-cyan-400 hover:bg-[#1f1f2e] transition-colors">
                    对话历史
                  </Link>
                  {user.role === 'admin' && (
                    <Link to="/admin" className="px-3 py-2 rounded-md text-sm font-medium text-gray-300 hover:text-purple-400 hover:bg-[#1f1f2e] transition-colors">
                      管理后台
                    </Link>
                  )}
                </nav>
              )}
            </div>

            <div className="flex items-center space-x-4">
              {user ? (
                <>
                  <div className="hidden md:flex items-center space-x-2 text-sm text-gray-400">
                    <span className="px-2 py-1 rounded bg-[#1f1f2e] border border-[#333]">
                      {user.displayName}
                    </span>
                    <span className="px-2 py-1 rounded bg-[#1f1f2e] border border-[#333] text-purple-400">
                      {user.role}
                    </span>
                  </div>
                  <button
                    onClick={logout}
                    className="px-4 py-2 rounded-lg text-sm font-medium text-red-400 hover:text-red-300 hover:bg-red-900/20 transition-colors border border-red-900/30"
                  >
                    退出登录
                  </button>
                </>
              ) : (
                <>
                  <Link
                    to="/login"
                    className="px-4 py-2 rounded-lg text-sm font-medium text-cyan-400 hover:text-cyan-300 hover:bg-[#1f1f2e] transition-colors"
                  >
                    登录
                  </Link>
                  <Link
                    to="/register"
                    className="px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-r from-cyan-600 to-purple-600 text-white hover:from-cyan-500 hover:to-purple-500 transition-all shadow-lg shadow-cyan-900/20"
                  >
                    注册
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      </header>

      <main className="relative z-10 flex-1 w-full">{children}</main>

      <footer className="glass border-t border-[#1f1f2e] mt-auto py-8">
        <div className="max-w-7xl mx-auto px-4 text-center text-gray-500 text-sm">
          <p>© 2026 AgentForge - 对话驱动的 Agent 工厂平台</p>
        </div>
      </footer>
    </div>
  );
}