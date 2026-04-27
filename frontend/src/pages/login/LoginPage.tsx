import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Typography, message, Space } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { login } from '../../api';
import { useAuthStore } from '../../stores/authStore';

const { Title, Text } = Typography;

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const { data } = await login(values.username, values.password);
      setAuth(data.token, data.userId, data.name, data.role);
      message.success(`欢迎回来，${data.name}`);

      // Route based on role
      if (data.role === 'CUSTOMER') navigate('/tickets/create');
      else if (data.role === 'ENGINEER') navigate('/tickets');
      else navigate('/admin/stats');
    } catch (err: any) {
      message.error(err.response?.data?.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: '#f5f5f5',
    }}>
      <Card style={{ width: 400, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div style={{
            width: 48, height: 48, background: '#1677ff', borderRadius: 12,
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            color: '#fff', fontSize: 24, fontWeight: 700, marginBottom: 12,
          }}>智</div>
          <Title level={4} style={{ margin: 0 }}>智能工单系统</Title>
          <Text type="secondary">AI-Powered Ticket System</Text>
        </div>

        <Form onFinish={onFinish} size="large">
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>
        </Form>

        <div style={{
          background: '#f9f0ff', border: '1px solid #d3adf7', borderRadius: 6,
          padding: 12, fontSize: 12, color: '#722ed1',
        }}>
          <Space orientation="vertical" size={2}>
            <Text strong style={{ color: '#722ed1', fontSize: 12 }}>✦ 演示账号</Text>
            <Text style={{ fontSize: 12 }}>客户: customer1 / demo123</Text>
            <Text style={{ fontSize: 12 }}>工程师: engineer1 / demo123</Text>
            <Text style={{ fontSize: 12 }}>管理员: admin1 / demo123</Text>
          </Space>
        </div>
      </Card>
    </div>
  );
}
