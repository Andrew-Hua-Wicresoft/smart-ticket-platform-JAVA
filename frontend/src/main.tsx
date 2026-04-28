import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { useAuthStore } from './stores/authStore';
import AppLayout from './layouts/AppLayout';
import LoginPage from './pages/login/LoginPage';
import TicketListPage from './pages/tickets/TicketListPage';
import TicketCreatePage from './pages/tickets/TicketCreatePage';
import TicketDetailPage from './pages/tickets/TicketDetailPage';
import MyTicketsPage from './pages/tickets/MyTicketsPage';
import KbListPage from './pages/kb/KbListPage';
import KbDraftsPage from './pages/kb/KbDraftsPage';
import AdminStatsPage from './pages/admin/AdminStatsPage';
import AuditLogPage from './pages/admin/AuditLogPage';
import NotificationListPage from './pages/notifications/NotificationListPage';
import type { UserRole } from './types';
import './index.css';

function ProtectedRoute({ children, roles }: { children: React.ReactNode; roles?: UserRole[] }) {
  const { isAuthenticated, role } = useAuthStore();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (roles && (!role || !roles.includes(role))) return <Navigate to="/" replace />;
  return <>{children}</>;
}

function RootRedirect() {
  const { isAuthenticated, role } = useAuthStore();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (role === 'CUSTOMER') return <Navigate to="/tickets/create" replace />;
  if (role === 'ENGINEER') return <Navigate to="/tickets" replace />;
  return <Navigate to="/admin/stats" replace />;
}

function App() {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#1677ff',
          borderRadius: 6,
          fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', sans-serif",
        },
      }}
    >
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
            <Route index element={<RootRedirect />} />
            <Route path="tickets" element={
              <ProtectedRoute roles={['ENGINEER', 'ADMIN']}><TicketListPage /></ProtectedRoute>
            } />
            <Route path="tickets/create" element={
              <ProtectedRoute roles={['CUSTOMER', 'ADMIN']}><TicketCreatePage /></ProtectedRoute>
            } />
            <Route path="tickets/:id" element={<TicketDetailPage />} />
            <Route path="my-tickets" element={
              <ProtectedRoute roles={['CUSTOMER', 'ENGINEER']}><MyTicketsPage /></ProtectedRoute>
            } />
            <Route path="kb" element={<KbListPage />} />
            <Route path="kb/drafts" element={
              <ProtectedRoute roles={['ENGINEER', 'ADMIN']}><KbDraftsPage /></ProtectedRoute>
            } />
            <Route path="notifications" element={<NotificationListPage />} />
            <Route path="admin/stats" element={
              <ProtectedRoute roles={['ADMIN']}><AdminStatsPage /></ProtectedRoute>
            } />
            <Route path="admin/audit" element={
              <ProtectedRoute roles={['ADMIN']}><AuditLogPage /></ProtectedRoute>
            } />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
