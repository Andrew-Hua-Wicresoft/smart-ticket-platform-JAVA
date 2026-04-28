import { useEffect, useState } from 'react';
import { Alert, Card, Typography, Input, Empty, Skeleton } from 'antd';
import { BookOutlined, SearchOutlined } from '@ant-design/icons';
import { listPublishedArticles } from '../../api';
import MarkdownContent from '../../components/MarkdownContent';
import type { KbArticle } from '../../types';
import dayjs from 'dayjs';

const { Title, Text } = Typography;

export default function KbListPage() {
  const [articles, setArticles] = useState<KbArticle[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');

  useEffect(() => {
    listPublishedArticles(0, 100)
      .then(({ data }) => setArticles(data.content))
      .catch(() => setError('知识库加载失败，请稍后重试'))
      .finally(() => setLoading(false));
  }, []);

  const filtered = search
    ? articles.filter((a) =>
        a.title.toLowerCase().includes(search.toLowerCase()) ||
        a.content.toLowerCase().includes(search.toLowerCase()))
    : articles;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>知识库</Title>
        <Input
          prefix={<SearchOutlined />}
          placeholder="搜索知识库..."
          style={{ width: 300 }}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          allowClear
        />
      </div>

      {error ? <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} /> : null}
      {loading ? <Skeleton active paragraph={{ rows: 6 }} /> : (
        filtered.length === 0 ? (
          <Card><Empty description="暂无知识库文章" /></Card>
        ) : (
          <div>
            {filtered.map((article) => (
              <Card key={article.id} style={{ marginBottom: 12 }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                  <BookOutlined style={{ fontSize: 20, color: article.sourceTicketId ? '#722ed1' : '#1677ff', marginTop: 2 }} />
                  <div style={{ flex: 1 }}>
                    <Title level={5} style={{ margin: 0 }}>{article.title}</Title>
                    <div style={{ marginTop: 8, maxHeight: 360, overflow: 'auto' }}>
                      <MarkdownContent content={article.content} />
                    </div>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {dayjs(article.createdAt).format('YYYY-MM-DD')}
                      {article.createdByName && ` · ${article.createdByName}`}
                      {article.sourceTicketId && ` · 来源工单 #${article.sourceTicketId}`}
                    </Text>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )
      )}
    </div>
  );
}
