import { useEffect, useState } from 'react';
import { Card, Table, Button, Typography, message, Modal, Input, Space, Skeleton } from 'antd';
import { CheckOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { listDraftArticles, publishArticle, updateArticle, deleteArticle } from '../../api';
import type { KbArticle } from '../../types';
import dayjs from 'dayjs';

const { Title, Paragraph } = Typography;
const { TextArea } = Input;

export default function KbDraftsPage() {
  const [articles, setArticles] = useState<KbArticle[]>([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const [editModal, setEditModal] = useState<KbArticle | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editContent, setEditContent] = useState('');

  const fetchDrafts = async () => {
    setLoading(true);
    try {
      const { data } = await listDraftArticles();
      setArticles(data.content);
      setTotal(data.totalElements);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchDrafts(); }, []);

  const handlePublish = async (id: number) => {
    try {
      await publishArticle(id);
      message.success('文章已发布');
      fetchDrafts();
    } catch (err: any) {
      message.error(err.response?.data?.message || '发布失败');
    }
  };

  const handleDelete = async (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '删除后无法恢复，确定要删除此文章吗？',
      onOk: async () => {
        await deleteArticle(id);
        message.success('文章已删除');
        fetchDrafts();
      },
    });
  };

  const handleEdit = (article: KbArticle) => {
    setEditModal(article);
    setEditTitle(article.title);
    setEditContent(article.content);
  };

  const handleSaveEdit = async () => {
    if (!editModal) return;
    try {
      await updateArticle(editModal.id, editTitle, editContent);
      message.success('文章已更新');
      setEditModal(null);
      fetchDrafts();
    } catch (err: any) {
      message.error(err.response?.data?.message || '更新失败');
    }
  };

  const columns = [
    { title: '标题', dataIndex: 'title', key: 'title', render: (t: string) => <strong>{t}</strong> },
    { title: '来源工单', dataIndex: 'sourceTicketId', key: 'sourceTicketId', width: 100,
      render: (id: number | null) => id ? `#${id}` : '-' },
    { title: '创建人', dataIndex: 'createdByName', key: 'createdByName', width: 100 },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 140,
      render: (t: string) => dayjs(t).format('YYYY-MM-DD HH:mm') },
    {
      title: '操作', key: 'actions', width: 200,
      render: (_: any, record: KbArticle) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Button size="small" type="primary" icon={<CheckOutlined />} onClick={() => handlePublish(record.id)}>发布</Button>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)} />
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>待审核文章</Title>
      <Card>
        {loading ? <Skeleton active /> : (
          <Table dataSource={articles} columns={columns} rowKey="id" size="middle"
            pagination={{ total, showTotal: (t) => `共 ${t} 篇待审核` }}
            expandable={{
              expandedRowRender: (record) => (
                <Paragraph style={{ whiteSpace: 'pre-wrap', maxHeight: 300, overflow: 'auto' }}>
                  {record.content}
                </Paragraph>
              ),
            }}
          />
        )}
      </Card>

      <Modal
        title="编辑文章"
        open={!!editModal}
        onOk={handleSaveEdit}
        onCancel={() => setEditModal(null)}
        width={700}
      >
        <Input value={editTitle} onChange={(e) => setEditTitle(e.target.value)}
          style={{ marginBottom: 12 }} placeholder="标题" />
        <TextArea rows={12} value={editContent} onChange={(e) => setEditContent(e.target.value)}
          placeholder="内容" />
      </Modal>
    </div>
  );
}
