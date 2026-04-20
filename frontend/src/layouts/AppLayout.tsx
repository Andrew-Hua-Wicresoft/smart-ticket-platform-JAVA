import React, { useEffect, useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Avatar, Dropdown, Typography, Badge } from 'antd';
import {
  FileTextOutlined,
  PlusCircleOutlined,
  BookOutlined,
  EditOutlined,
  BarChartOutlined,
  LogoutOutlined,
  UserOutlined,
  BellOutlined,
  HistoryOutlined,
} from '@ant-design/icons';
import { useAuthStore } from '../stores/authStore';
import { getUnreadNotificationCount } from '../api';
import { NOTIFICATIONS_CHANGED_EVENT } from '../lib/notifications';
import type { UserRole } from '../types';

const { Sider, Header, Content } = Layout;
const { Text } = Typography;

interface MenuItem {
  key: string;
  icon: React.ReactNode;
  label: string;
  group?: string;
  roles: UserRole[];
}

const allMenuItems: MenuItem[] = [
  { key: '/tickets/create', icon: <PlusCircleOutlined />, label: '提交工单', group: '工单管理', roles: ['CUSTOMER'] },
  { key: '/tickets', icon: <FileTextOutlined />, label: '工单队列', group: '工单管理', roles: ['ENGINEER', 'ADMIN'] },
  { key: '/my-tickets', icon: <FileTextOutlined />, label: '我的工单', group: '工单管理', roles: ['CUSTOMER', 'ENGINEER'] },
  { key: '/kb', icon: <BookOutlined />, label: '知识库', group: '知识库', roles: ['CUSTOMER', 'ENGINEER', 'ADMIN'] },
  { key: '/kb/drafts', icon: <EditOutlined />, label: '待审核文章', group: '知识库', roles: ['ENGINEER', 'ADMIN'] },
  { key: '/notifications', icon: <BellOutlined />, label: '通知中心', group: '协作', roles: ['CUSTOMER', 'ENGINEER', 'ADMIN'] },
  { key: '/admin/stats', icon: <BarChartOutlined />, label: '数据分析', group: '系统', roles: ['ADMIN'] },
  { key: '/admin/audit', icon: <HistoryOutlined />, label: '审计日志', group: '系统', roles: ['ADMIN'] },
];

const breadcrumbLabels: Record<string, string> = {
  tickets: '工单队列',
  'my-tickets': '我的工单',
  create: '提交工单',
  kb: '知识库',
  drafts: '待审核文章',
  notifications: '通知中心',
  admin: '系统',
  stats: '数据分析',
  audit: '审计日志',
};

export default function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { name, role, logout } = useAuthStore();
  const [unreadCount, setUnreadCount] = useState(0);

  const refreshUnreadCount = () => {
    getUnreadNotificationCount()
      .then(({ data }) => setUnreadCount(data.unreadCount))
      .catch(() => setUnreadCount(0));
  };

  const filteredItems = allMenuItems.filter((item) => role && item.roles.includes(role));

  useEffect(() => {
    if (!role) {
      setUnreadCount(0);
      return;
    }
    refreshUnreadCount();
  }, [role, location.pathname]);

  useEffect(() => {
    window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, refreshUnreadCount);
    return () => window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, refreshUnreadCount);
  }, [role]);

  // Group menu items
  const groups: Record<string, MenuItem[]> = {};
  filteredItems.forEach((item) => {
    const group = item.group || '其他';
    if (!groups[group]) groups[group] = [];
    groups[group].push(item);
  });

  const menuItems = Object.entries(groups).map(([group, items]) => ({
    type: 'group' as const,
    label: group,
    children: items.map((item) => ({
      key: item.key,
      icon: item.icon,
      label: item.key === '/notifications'
        ? (
          <span>
            {item.label}
            {unreadCount > 0 && <Badge count={unreadCount} size="small" style={{ marginInlineStart: 8 }} />}
          </span>
        )
        : item.label,
    })),
  }));

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const userMenu = {
    items: [
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出登录',
        onClick: handleLogout,
      },
    ],
  };

  // Find active key — match longest (most specific) path first
  const selectedKey = [...filteredItems]
    .sort((a, b) => b.key.length - a.key.length)
    .find((item) =>
      location.pathname === item.key || location.pathname.startsWith(item.key + '/')
    )?.key || '';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={220} theme="dark" style={{ position: 'fixed', height: '100vh', left: 0, top: 0 }}>
        <div style={{
          height: 48,
          display: 'flex',
          alignItems: 'center',
          padding: '0 16px',
          gap: 8,
          borderBottom: '1px solid rgba(255,255,255,0.06)',
        }}>
          <div style={{
            width: 28, height: 28, background: '#1677ff', borderRadius: 6,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: '#fff', fontSize: 14, fontWeight: 700,
          }}>智</div>
          <span style={{ color: '#fff', fontSize: 14, fontWeight: 500 }}>智能工单</span>
        </div>
        <style>{`.ant-menu-item-group-title { font-size: 11px !important; text-transform: uppercase; opacity: 0.3 !important; letter-spacing: 0.5px; }`}</style>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ borderRight: 0 }}
        />
      </Sider>
      <Layout style={{ marginLeft: 220 }}>
        <Header style={{
          background: '#fff',
          padding: '0 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid #f0f0f0',
          height: 48,
          lineHeight: '48px',
        }}>
          <Text type="secondary" style={{ fontSize: 14 }}>
            {location.pathname.split('/').filter(Boolean).map((seg) =>
              breadcrumbLabels[seg] || (/^\d+$/.test(seg) ? `#${seg}` : seg)
            ).join(' / ')}
          </Text>
          <Dropdown menu={userMenu} placement="bottomRight">
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
              <Text type="secondary" style={{ fontSize: 12 }}>{name}</Text>
              <Avatar size={28} icon={<UserOutlined />} style={{ background: '#1677ff' }} />
            </div>
          </Dropdown>
        </Header>
        <Content style={{ padding: 16, background: '#f5f5f5', minHeight: 'calc(100vh - 48px)' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
