import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Typography, message, Spin, Empty } from 'antd';
import { SendOutlined } from '@ant-design/icons';
import { createTicket, aiSearch, logDeflection } from '../../api';
import type { AiSearchResult } from '../../types';
import { useDebouncedCallback } from '../../hooks/useDebounce';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

export default function TicketCreatePage() {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [suggestions, setSuggestions] = useState<AiSearchResult[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const navigate = useNavigate();

  // Debounced search: fires 500ms after user stops typing
  const debouncedSearch = useDebouncedCallback(async (description: string) => {
    if (!description || description.length < 10) {
      setSuggestions([]);
      return;
    }
    setSearchLoading(true);
    try {
      const { data } = await aiSearch(description, 3);
      setSuggestions(data);
    } catch {
      // AI search failure is non-blocking
    } finally {
      setSearchLoading(false);
    }
  }, 500);

  const handleDescriptionChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    debouncedSearch(e.target.value);
  };

  const handleDeflection = async (result: AiSearchResult) => {
    const description = form.getFieldValue('description');
    try {
      await logDeflection(result.kbId, description);
      message.success('问题已解决！感谢使用自助服务。');
      navigate('/my-tickets');
    } catch {
      message.info('已记录。');
    }
  };

  const onFinish = async (values: { title: string; description: string }) => {
    setSubmitting(true);
    try {
      const { data } = await createTicket(values.title, values.description);
      message.success(`工单 #${data.id} 创建成功`);
      navigate('/my-tickets');
    } catch (err: any) {
      message.error(err.response?.data?.message || '创建失败');
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
                onChange={handleDescriptionChange}
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

        {/* AI Suggestions Panel */}
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
            <Text strong style={{ color: '#722ed1', fontSize: 14 }}>AI 智能建议</Text>
          </div>

          {searchLoading ? (
            <div style={{ textAlign: 'center', padding: 24 }}>
              <Spin size="small" />
              <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 8 }}>正在搜索知识库...</div>
            </div>
          ) : suggestions.length > 0 ? (
            <>
              <Text style={{ fontSize: 12, color: '#531dab', display: 'block', marginBottom: 12 }}>
                以下知识库文章可能帮助您解决问题：
              </Text>
              {suggestions.map((s) => (
                <div
                  key={s.kbId}
                  style={{
                    background: '#fff', border: '1px solid #d3adf7', borderRadius: 6,
                    padding: 12, marginBottom: 8, cursor: 'pointer',
                    transition: 'border-color 0.2s',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.borderColor = '#722ed1')}
                  onMouseLeave={(e) => (e.currentTarget.style.borderColor = '#d3adf7')}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Text strong style={{ fontSize: 14 }}>{s.title}</Text>
                    <Text style={{ fontSize: 11, color: '#722ed1', fontWeight: 500 }}>
                      {Math.round(s.similarity * 100)}% 匹配
                    </Text>
                  </div>
                  <Paragraph
                    ellipsis={{ rows: 2 }}
                    style={{ fontSize: 12, color: '#8c8c8c', margin: '4px 0 0' }}
                  >
                    {s.content}
                  </Paragraph>
                </div>
              ))}
              <Button
                block
                style={{ marginTop: 12, background: '#722ed1', borderColor: '#722ed1', color: '#fff' }}
                onClick={() => suggestions[0] && handleDeflection(suggestions[0])}
              >
                问题已解决，无需提交
              </Button>
            </>
          ) : (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <Text style={{ fontSize: 12, color: '#8c8c8c' }}>
                  输入问题描述后，AI 将自动搜索相关知识库文章
                </Text>
              }
            />
          )}
        </div>
      </div>
    </div>
  );
}
