import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import api from '../services/api';

export default function FeishuCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState('');

  useEffect(() => {
    const oauthError = searchParams.get('error');
    if (oauthError === 'access_denied') {
      setError('您已取消飞书授权');
      return;
    }

    const code = searchParams.get('code');
    if (!code) {
      setError('缺少授权码，请重新登录');
      return;
    }

    const state = searchParams.get('state');
    if (state && state !== 'agentforge') {
      setError('授权 state 校验失败，请重新登录');
      return;
    }

    (async () => {
      try {
        const res = await api.get<{ token: string }>(
          `/auth/feishu/callback?code=${encodeURIComponent(code)}`,
        );
        localStorage.setItem('token', res.token);
        navigate('/', { replace: true });
      } catch (e) {
        setError(e instanceof Error ? e.message : '飞书登录失败');
      }
    })();
  }, [searchParams, navigate]);

  return (
    <div className="min-h-screen bg-[#0a0a12] flex items-center justify-center p-4">
      <div className="text-center max-w-md">
        {error ? (
          <>
            <p className="text-red-400 mb-6">{error}</p>
            <Link
              to="/login"
              className="px-6 py-2 rounded-lg border border-[#333] hover:border-cyan-500/50 text-cyan-400 transition-colors"
            >
              返回登录
            </Link>
          </>
        ) : (
          <>
            <div className="spinner w-8 h-8 border-2 mx-auto mb-4" />
            <p className="text-cyan-400">飞书登录中...</p>
          </>
        )}
      </div>
    </div>
  );
}
