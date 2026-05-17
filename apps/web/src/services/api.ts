const BASE_URL = '/api';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options?.headers as Record<string, string> | undefined),
  };

  const token = localStorage.getItem('token');
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (res.status === 401) {
    localStorage.removeItem('token');
    if (!path.startsWith('/auth/')) {
      window.location.href = '/login';
    }
    throw new Error('未登录或登录已过期');
  }

  if (!res.ok) {
    let detail = '';
    try {
      const data = await res.clone().json();
      detail = data.error || data.message || JSON.stringify(data);
    } catch {
      detail = await res.text();
    }
    throw new Error(detail || `请求失败 ${res.status}`);
  }

  if (res.status === 204) return undefined as T;
  return res.json();
}

export default {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};
