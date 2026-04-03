import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card, Descriptions, Tag, Button, Typography, Space, Input,
  message, Skeleton, Alert, Divider,
} from 'antd';
import { CheckCircleOutlined, UserAddOutlined } from '@ant-design/icons';
import { getTicket, assignTicket, resolveTicket, aiSuggest } from '../../api';
import { useAuthStore } from '../../stores/authStore';
import StatusDot from '../../components/StatusDot';
import type { TicketResponse, TicketStatus, TicketPriority } from '../../types';
import dayjs from 'dayjs';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

const priorityConfig: Record<TicketPriority, { color: string; label: string }> = {
  HIGH: { color: 'red', label: '高优先级' },
  MEDIUM: { color: 'orange', label: '中优先级' },
  LOW: { color: 'green', label: '低优先级' },
};

export default function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { role, userId } = useAuthStore();

  const [ticket, setTicket] = useState<TicketResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [resolving, setResolving] = useState(false);
  const [assigning, setAssigning] = useState(false);
  const [suggestion, setSuggestion] = useState<string | null>(null);
  const [suggestLoading, setSuggestLoading] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getTicket(Number(id))
      .then(({ data }) => setTicket(data))
      .catch(() => message.error('工单不存在'))
      .finally(() => setLoading(false));
  }, [id]);

  const handleAssign = async () => {
    if (!ticket) return;
    setAssigning(true);
    try {
      const { data } = await assignTicket(ticket.id);
      setTicket(data);
      message.success('工单已接取');
    } catch (err: any) {
      message.error(err.response?.data?.message || '接取失败');
    } finally {
      setAssigning(false);
    }
  };

  const handleResolve = async () => {
    if (!ticket || resolutionNotes.length < 10) {
      message.warning('解决方案说明至少需要10个字符');
      return;
    }
    setResolving(true);
    try {
      const { data } = await resolveTicket(ticket.id, resolutionNotes);
      setTicket(data);
      message.success('工单已解决，KB文章正在后台生成');
    } catch (err: any) {
      message.error(err.response?.data?.message || '解决失败');
    } finally {
      setResolving(false);
    }
  };

  const handleGetSuggestion = async () => {
    if (!ticket) return;
    setSuggestLoading(true);
    try {
      const { data } = await aiSuggest(ticket.id, ticket.title, ticket.description);
      setSuggestion(data.suggestion);
    } catch {
      setSuggestion('AI暂时不可用');
    } finally {
      setSuggestLoading(false);
    }
  };

  if (loading) return <Skeleton active paragraph={{ rows: 10 }} />;
  if (!ticket) return <Alert type="error" message="工单不存在" />;

  const isEngineer = role === 'ENGINEER' || role === 'ADMIN';
  const isAssignedToMe = ticket.assignedEngineerId === userId;
  const canAssign = isEngineer && ticket.status === 'OPEN';
  const canResolve = isEngineer && ticket.status === 'IN_PROGRESS' && isAssignedToMe;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Space>
          <Title level={4} style={{ margin: 0 }}>工单 #{ticket.id}</Title>
          <StatusDot status={ticket.status} />
          <Tag color={priorityConfig[ticket.priority].color}>{priorityConfig[ticket.priority].label}</Tag>
        </Space>
        <Button onClick={() => navigate(-1)}>返回</Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: isEngineer ? '1fr 360px' : '1fr', gap: 16 }}>
        {/* Main content */}
        <div>
          <Card>
            <Descriptions column={2} size="small">
              <Descriptions.Item label="标题" span={2}>
                <Text strong>{ticket.title}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="提交人">{ticket.customerName}</Descriptions.Item>
              <Descriptions.Item label="提交时间">
                {dayjs(ticket.createdAt).format('YYYY-MM-DD HH:mm')}
              </Descriptions.Item>
              <Descriptions.Item label="负责工程师">
                {ticket.assignedEngineerName || <Text type="secondary">未分配</Text>}
              </Descriptions.Item>
              {ticket.resolvedAt && (
                <Descriptions.Item label="解决时间">
                  {dayjs(ticket.resolvedAt).format('YYYY-MM-DD HH:mm')}
                </Descriptions.Item>
              )}
              {ticket.priorityReason && (
                <Descriptions.Item label="优先级原因" span={2}>
                  <Text style={{ color: '#722ed1', fontSize: 12 }}>✦ {ticket.priorityReason}</Text>
                </Descriptions.Item>
              )}
            </Descriptions>

            <Divider style={{ margin: '16px 0' }} />

            <Title level={5}>问题描述</Title>
            <Paragraph style={{ whiteSpace: 'pre-wrap' }}>{ticket.description}</Paragraph>

            {ticket.resolutionNotes && (
              <>
                <Divider style={{ margin: '16px 0' }} />
                <Title level={5}>解决方案</Title>
                <Paragraph style={{ whiteSpace: 'pre-wrap' }}>{ticket.resolutionNotes}</Paragraph>
              </>
            )}
          </Card>

          {/* Resolve form */}
          {canResolve && (
            <Card style={{ marginTop: 16 }}>
              <Title level={5}>解决工单</Title>
              <TextArea
                rows={4}
                placeholder="详细描述解决方案（至少10个字符）"
                value={resolutionNotes}
                onChange={(e) => setResolutionNotes(e.target.value)}
                showCount
                maxLength={10000}
              />
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                onClick={handleResolve}
                loading={resolving}
                style={{ marginTop: 12 }}
                disabled={resolutionNotes.length < 10}
              >
                确认解决
              </Button>
            </Card>
          )}

          {/* Assign button */}
          {canAssign && (
            <Card style={{ marginTop: 16 }}>
              <Button
                type="primary"
                icon={<UserAddOutlined />}
                onClick={handleAssign}
                loading={assigning}
              >
                接取工单
              </Button>
            </Card>
          )}
        </div>

        {/* AI suggestion panel (engineer only) */}
        {isEngineer && (
          <div style={{
            background: '#f9f0ff',
            border: '1px solid #d3adf7',
            borderRadius: 8,
            padding: 16,
            height: 'fit-content',
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
              <div style={{
                width: 20, height: 20, background: '#722ed1', borderRadius: '50%',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                color: '#fff', fontSize: 11,
              }}>✦</div>
              <Text strong style={{ color: '#722ed1', fontSize: 14 }}>AI 解决方案建议</Text>
            </div>

            {suggestion ? (
              <div style={{
                background: '#fff', border: '1px solid #d3adf7', borderRadius: 6,
                padding: 12, fontSize: 13, lineHeight: 1.6, whiteSpace: 'pre-wrap',
              }}>
                {suggestion}
              </div>
            ) : (
              <Button
                block
                style={{ background: '#722ed1', borderColor: '#722ed1', color: '#fff' }}
                onClick={handleGetSuggestion}
                loading={suggestLoading}
              >
                获取 AI 建议
              </Button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
