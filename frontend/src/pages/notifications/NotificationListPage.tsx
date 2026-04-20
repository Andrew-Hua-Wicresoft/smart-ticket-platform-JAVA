import { useEffect, useState } from 'react';
import { Badge, Button, Card, List, Space, Typography, message } from 'antd';
import { BellOutlined, CheckOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { listNotifications, markNotificationRead } from '../../api';
import { emitNotificationsChanged } from '../../lib/notifications';
import type { NotificationItem } from '../../types';

const { Title, Text, Paragraph } = Typography;

export default function NotificationListPage() {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [markingId, setMarkingId] = useState<number | null>(null);

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const { data } = await listNotifications();
      setItems(data.content);
    } catch {
      message.error('加载通知失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  const handleMarkRead = async (id: number) => {
    setMarkingId(id);
    try {
      const { data } = await markNotificationRead(id);
      setItems((current) => current.map((item) => item.id === id ? data : item));
      emitNotificationsChanged();
    } catch {
      message.error('标记已读失败');
    } finally {
      setMarkingId(null);
    }
  };

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>通知中心</Title>
      <Card>
        <List
          loading={loading}
          locale={{ emptyText: '暂无通知' }}
          dataSource={items}
          renderItem={(item) => (
            <List.Item
              actions={[
                item.read ? (
                  <Text type="secondary" key="read">已读</Text>
                ) : (
                  <Button
                    key="mark-read"
                    type="link"
                    icon={<CheckOutlined />}
                    loading={markingId === item.id}
                    onClick={() => handleMarkRead(item.id)}
                  >
                    标记已读
                  </Button>
                ),
              ]}
            >
              <List.Item.Meta
                avatar={
                  <Badge dot={!item.read}>
                    <BellOutlined style={{ fontSize: 18, color: item.read ? '#8c8c8c' : '#1677ff' }} />
                  </Badge>
                }
                title={
                  <Space>
                    <Text strong>{item.title}</Text>
                    {!item.read && <Badge status="processing" text="未读" />}
                    {item.ticketId && <Text type="secondary">工单 #{item.ticketId}</Text>}
                  </Space>
                }
                description={
                  <>
                    <Paragraph style={{ marginBottom: 8 }}>{item.content}</Paragraph>
                    <Text type="secondary">{dayjs(item.createdAt).format('YYYY-MM-DD HH:mm')}</Text>
                  </>
                }
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
}
