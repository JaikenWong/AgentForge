import type { ReactElement } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import CreatePage from './pages/CreatePage';
import AgentListPage from './pages/AgentListPage';
import AgentDetailPage from './pages/AgentDetailPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import FeishuCallbackPage from './pages/FeishuCallbackPage';
import ConversationsPage from './pages/ConversationsPage';
import AdminPage from './pages/AdminPage';
import TemplatesPage from './pages/TemplatesPage';
import EmbedAgentPage from './pages/EmbedAgentPage';

function RequireAuth({ children }: { children: ReactElement }) {
  const location = useLocation();
  const token = localStorage.getItem('token');
  if (!token) return <Navigate to="/login" state={{ from: location }} replace />;
  return children;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/create" element={<RequireAuth><CreatePage /></RequireAuth>} />
      <Route path="/agents" element={<RequireAuth><AgentListPage /></RequireAuth>} />
      <Route path="/agents/:id" element={<RequireAuth><AgentDetailPage /></RequireAuth>} />
      <Route path="/templates" element={<RequireAuth><TemplatesPage /></RequireAuth>} />
      <Route path="/embed/:id" element={<RequireAuth><EmbedAgentPage /></RequireAuth>} />
      <Route path="/conversations" element={<RequireAuth><ConversationsPage /></RequireAuth>} />
      <Route path="/admin" element={<RequireAuth><AdminPage /></RequireAuth>} />
    </Routes>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/auth/feishu/callback" element={<FeishuCallbackPage />} />
      <Route
        path="*"
        element={
          <Layout>
            <AppRoutes />
          </Layout>
        }
      />
    </Routes>
  );
}
