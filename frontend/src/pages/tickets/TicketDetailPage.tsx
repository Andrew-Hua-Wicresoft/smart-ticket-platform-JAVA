import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card, Descriptions, Tag, Button, Typography, Space, Input,
  message, Skeleton, Alert, Divider, List, Avatar,
} from 'antd';
import { CheckCircleOutlined, UserAddOutlined, RobotOutlined } from '@ant-design/icons';
import {
  getTicket, assignTicket, resolveTicket, aiSuggest,
  getLatestAiSuggestion, listTicketComments, addTicketComment,
} from '../../api';
import MarkdownContent from '../../components/MarkdownContent';
import { useAuthStore } from '../../stores/authStore';
import StatusDot from '../../components/StatusDot';
import type { TicketResponse, TicketPriority, TicketComment } from '../../types';
import dayjs from 'dayjs';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

function getApiErrorMessage(error: unknown, fallback: string) {
  if (typeof error !== 'object' || error === null || !('response' in error)) {
    return error instanceof Error ? error.message : fallback;
  }
  const response = (error as { response?: { data?: { message?: unknown; error?: unknown } } }).response;
  if (typeof response?.data?.message === 'string') return response.data.message;
  if (typeof response?.data?.error === 'string') return response.data.error;
  return fallback;
}

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
  const [suggestionCreatedAt, setSuggestionCreatedAt] = useState<string | null>(null);
  const [suggestLoading, setSuggestLoading] = useState(false);
  const [comments, setComments] = useState<TicketComment[]>([]);
  const [commentContent, setCommentContent] = useState('');
  const [commentLoading, setCommentLoading] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setSuggestion(null);
    setSuggestionCreatedAt(null);
    const ticketId = Number(id);
    const canViewAiSuggestion = role === 'ENGINEER' || role === 'ADMIN';
    Promise.all([
      getTicket(ticketId),
      listTicketComments(ticketId),
      canViewAiSuggestion ? getLatestAiSuggestion(ticketId).catch(() => null) : Promise.resolve(null),
    ])
      .then(([ticketResponse, commentsResponse, suggestionResponse]) => {
        setTicket(ticketResponse.data);
        setComments(commentsResponse.data);
        if (suggestionResponse?.data.available && suggestionResponse.data.suggestion) {
          setSuggestion(suggestionResponse.data.suggestion);
          setSuggestionCreatedAt(suggestionResponse.data.createdAt);
        }
      })
      .catch(() => message.error('工单不存在'))
      .finally(() => setLoading(false));
  }, [id, role]);

  const handleAssign = async () => {
    if (!ticket) return;
    setAssigning(true);
    try {
      const { data } = await assignTicket(ticket.id);
      setTicket(data);
      message.success('工单已接取');
    } catch (err) {
      message.error(getApiErrorMessage(err, '接取失败'));
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
    } catch (err) {
      message.error(getApiErrorMessage(err, '解决失败'));
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
      setSuggestionCreatedAt(new Date().toISOString());
    } catch (err) {
      const reason = getApiErrorMessage(err, 'AI暂时不可用');
      setSuggestion(reason ? `AI诊断失败：${reason}` : 'AI暂时不可用');
      setSuggestionCreatedAt(null);
    } finally {
      setSuggestLoading(false);
    }
  };

  const handleAddComment = async () => {
    if (!ticket || commentContent.trim().length === 0) {
      message.warning('评论内容不能为空');
      return;
    }
    setCommentLoading(true);
    try {
      const { data } = await addTicketComment(ticket.id, commentContent.trim());
      setComments((current) => [...current, data]);
      setCommentContent('');
      message.success('评论已添加');
    } catch (err) {
      message.error(getApiErrorMessage(err, '添加评论失败'));
    } finally {
      setCommentLoading(false);
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

          <Card style={{ marginTop: 16 }}>
            <Title level={5}>协作评论</Title>
            <List
              locale={{ emptyText: '暂无评论' }}
              dataSource={comments}
              renderItem={(comment) => (
                <List.Item style={{ paddingInline: 0 }}>
                  <List.Item.Meta
                    avatar={<Avatar>{comment.authorName.slice(0, 1)}</Avatar>}
                    title={
                      <Space>
                        <Text strong>{comment.authorName}</Text>
                        <Tag>{comment.authorRole}</Tag>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {dayjs(comment.createdAt).format('YYYY-MM-DD HH:mm')}
                        </Text>
                      </Space>
                    }
                    description={<Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>{comment.content}</Paragraph>}
                  />
                </List.Item>
              )}
            />
            <Divider style={{ margin: '16px 0' }} />
            <TextArea
              rows={3}
              placeholder="补充处理进展、沟通记录或需要协助的信息"
              value={commentContent}
              onChange={(e) => setCommentContent(e.target.value)}
              showCount
              maxLength={5000}
            />
            <Button
              type="primary"
              style={{ marginTop: 12 }}
              loading={commentLoading}
              onClick={handleAddComment}
            >
              添加评论
            </Button>
          </Card>
        </div>

        {/* AI suggestion panel (engineer only) */}
        {isEngineer && (
          <div style={{
            background: '#fafafa',
            border: '1px solid #f0f0f0',
            borderRadius: 8,
            height: 'fit-content',
            overflow: 'hidden',
          }}>
            {/* Header bar */}
            <div style={{
              background: '#722ed1',
              padding: '12px 16px',
              display: 'flex', alignItems: 'center', gap: 8,
            }}>
              <RobotOutlined style={{ color: '#fff', fontSize: 16 }} />
              <Text strong style={{ color: '#fff', fontSize: 14 }}>AI 诊断助手</Text>
              <Text style={{ color: 'rgba(255,255,255,0.65)', fontSize: 11, marginLeft: 'auto' }}>
                DeepSeek V4 Pro
              </Text>
            </div>

            <div style={{ padding: 16 }}>
              {suggestion ? (
                <>
                  {suggestionCreatedAt && (
                    <div style={{ color: '#8c8c8c', fontSize: 12, marginBottom: 8 }}>
                      最近诊断：{dayjs(suggestionCreatedAt).format('YYYY-MM-DD HH:mm')}
                    </div>
                  )}
                  <div style={{
                    background: '#fff', border: '1px solid #f0f0f0', borderRadius: 6,
                    padding: '12px 16px', fontSize: 13, lineHeight: 1.8,
                    maxHeight: 600, overflowY: 'auto',
                  }}>
                    <MarkdownContent content={suggestion} />
                  </div>
                  <Button
                    block
                    style={{ marginTop: 12, background: '#722ed1', borderColor: '#722ed1', color: '#fff' }}
                    onClick={handleGetSuggestion}
                    loading={suggestLoading}
                    icon={<RobotOutlined />}
                  >
                    重新生成 AI 诊断
                  </Button>
                </>
              ) : (
                <>
                  <div style={{ textAlign: 'center', padding: '16px 0 12px', color: '#8c8c8c', fontSize: 12 }}>
                    基于工单描述，AI 将分析可能的原因并提供解决步骤
                  </div>
                  <Button
                    block
                    size="large"
                    style={{ background: '#722ed1', borderColor: '#722ed1', color: '#fff' }}
                    onClick={handleGetSuggestion}
                    loading={suggestLoading}
                    icon={<RobotOutlined />}
                  >
                    获取 AI 诊断
                  </Button>
                </>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
