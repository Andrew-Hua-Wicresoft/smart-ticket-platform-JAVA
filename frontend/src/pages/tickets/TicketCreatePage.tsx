import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Typography, message, Spin, Empty } from 'antd';
import { SendOutlined, SearchOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { createTicket, aiSearch, logDeflection } from '../../api';
import type { AiSearchResult } from '../../types';
import { useDebouncedCallback } from '../../hooks/useDebounce';
import { useAuthStore } from '../../stores/authStore';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;
const MIN_SELF_SERVICE_QUERY_LENGTH = 20;

function getApiErrorMessage(error: unknown, fallback: string) {
  if (typeof error !== 'object' || error === null || !('response' in error)) {
    return fallback;
  }
  const response = (error as { response?: { data?: { message?: unknown } } }).response;
  return typeof response?.data?.message === 'string' ? response.data.message : fallback;
}

export default function TicketCreatePage() {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [suggestions, setSuggestions] = useState<AiSearchResult[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const searchRequestSeq = useRef(0);
  const navigate = useNavigate();
  const role = useAuthStore((state) => state.role);
  const title = Form.useWatch('title', form) || '';
  const description = Form.useWatch('description', form) || '';

  const [debouncedSearch, cancelDebouncedSearch] = useDebouncedCallback(async (query: string, requestSeq: number) => {
    if (query.length < MIN_SELF_SERVICE_QUERY_LENGTH) {
      setSuggestions([]);
      return;
    }
    setSearchLoading(true);
    try {
      const { data } = await aiSearch(query, 3);
      if (requestSeq === searchRequestSeq.current) {
        setSuggestions(data);
      }
    } catch {
      // Self-service search is non-blocking; users can still submit tickets.
      if (requestSeq === searchRequestSeq.current) {
        setSuggestions([]);
      }
    } finally {
      if (requestSeq === searchRequestSeq.current) {
        setSearchLoading(false);
      }
    }
  }, 800);

  useEffect(() => {
    const query = `${title}\n${description}`.trim();
    const requestSeq = searchRequestSeq.current + 1;
    searchRequestSeq.current = requestSeq;

    if (query.length < MIN_SELF_SERVICE_QUERY_LENGTH) {
      cancelDebouncedSearch();
      setSuggestions([]);
      setSearchLoading(false);
      return;
    }

    debouncedSearch(query, requestSeq);
  }, [title, description, debouncedSearch, cancelDebouncedSearch]);

  const handleDeflection = async (result: AiSearchResult) => {
    const description = form.getFieldValue('description');
    try {
      await logDeflection(result.kbId, description);
      message.success('问题已解决！感谢使用自助服务。');
      navigate(role === 'ADMIN' ? '/tickets' : '/my-tickets');
    } catch {
      message.info('已记录。');
    }
  };

  const onFinish = async (values: { title: string; description: string }) => {
    setSubmitting(true);
    try {
      const { data } = await createTicket(values.title, values.description);
      message.success(`工单 #${data.id} 创建成功`);
      navigate(role === 'ADMIN' ? '/tickets' : '/my-tickets');
    } catch (err) {
      message.error(getApiErrorMessage(err, '创建失败'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>提交新工单</Title>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 360px', gap: 16 }}>
        {/* Form */}
        <Card>
          <Form form={form} layout="vertical" onFinish={onFinish}>
            <Form.Item
              name="title"
              label="工单标题"
              rules={[
                { required: true, message: '请输入标题' },
                { min: 5, message: '标题至少5个字符' },
                { max: 200, message: '标题最多200个字符' },
              ]}
            >
              <Input placeholder="简要描述您遇到的问题（5-200字）" />
            </Form.Item>
            <Form.Item
              name="description"
              label="问题描述"
              rules={[
                { required: true, message: '请输入描述' },
                { min: 10, message: '描述至少10个字符' },
                { max: 5000, message: '描述最多5000个字符' },
              ]}
            >
              <TextArea
                rows={6}
                placeholder="详细描述您的问题，包括错误信息、操作步骤等（10-5000字）"
                showCount
                maxLength={5000}
              />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" icon={<SendOutlined />} loading={submitting}>
                提交工单
              </Button>
            </Form.Item>
          </Form>
        </Card>

        {/* AI Self-Service Panel (customer-facing) */}
        <div style={{
          background: '#fff',
          border: '1px solid #f0f0f0',
          borderRadius: 8,
          height: 'fit-content',
          overflow: 'hidden',
        }}>
          {/* Header */}
          <div style={{
            background: '#f9f0ff',
            borderBottom: '1px solid #d3adf7',
            padding: '12px 16px',
            display: 'flex', alignItems: 'center', gap: 8,
          }}>
            <SearchOutlined style={{ color: '#722ed1', fontSize: 16 }} />
            <Text strong style={{ color: '#722ed1', fontSize: 14 }}>自助知识库匹配</Text>
            <Text style={{ color: '#8c8c8c', fontSize: 11, marginLeft: 'auto' }}>不调用大模型</Text>
          </div>

          <div style={{ padding: 16 }}>
            {searchLoading ? (
              <div style={{ textAlign: 'center', padding: 24 }}>
                <Spin size="small" />
                <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 8 }}>正在匹配知识库...</div>
              </div>
            ) : suggestions.length > 0 ? (
              <>
                <div style={{
                  background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: 6,
                  padding: '8px 12px', marginBottom: 12, fontSize: 12, color: '#52c41a',
                  display: 'flex', alignItems: 'center', gap: 6,
                }}>
                  <CheckCircleOutlined />
                  找到 {suggestions.length} 篇可能相关的知识库文章：
                </div>
                {suggestions.map((s, i) => (
                  <div
                    key={s.kbId}
                    style={{
                      background: '#fafafa', border: '1px solid #f0f0f0', borderRadius: 6,
                      padding: 12, marginBottom: 8, cursor: 'pointer',
                      transition: 'all 0.2s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.borderColor = '#1677ff';
                      e.currentTarget.style.background = '#e6f4ff';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.borderColor = '#f0f0f0';
                      e.currentTarget.style.background = '#fafafa';
                    }}
                    onClick={() => handleDeflection(s)}
                  >
                    <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
                      <div style={{
                        width: 20, height: 20, borderRadius: '50%',
                        background: '#1677ff', color: '#fff', fontSize: 11,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        flexShrink: 0, marginTop: 2,
                      }}>{i + 1}</div>
                      <div style={{ flex: 1 }}>
                        <Text strong style={{ fontSize: 13 }}>{s.title}</Text>
                        <div style={{
                          fontSize: 11, color: '#722ed1', fontWeight: 500,
                          marginTop: 2,
                        }}>
                          匹配度 {Math.round(s.similarity * 100)}%
                        </div>
                        <Paragraph
                          ellipsis={{ rows: 2 }}
                          style={{ fontSize: 12, color: '#8c8c8c', margin: '4px 0 0' }}
                        >
                          {s.content}
                        </Paragraph>
                      </div>
                    </div>
                  </div>
                ))}
                <div style={{ fontSize: 11, color: '#8c8c8c', textAlign: 'center', marginTop: 8 }}>
                  点击文章即可标记为已解决，不会创建工单
                </div>
              </>
            ) : (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  <Text style={{ fontSize: 12, color: '#8c8c8c' }}>
                    输入至少 {MIN_SELF_SERVICE_QUERY_LENGTH} 个字符后，将用本地向量和关键词匹配知识库
                  </Text>
                }
              />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
