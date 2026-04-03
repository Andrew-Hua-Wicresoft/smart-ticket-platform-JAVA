import { useEffect, useState } from 'react';
import { Alert, Card, Typography, Statistic, Row, Col, Skeleton } from 'antd';
import {
  FileTextOutlined, CheckCircleOutlined, ClockCircleOutlined, BookOutlined,
} from '@ant-design/icons';
import { getAdminStats } from '../../api';
import { statusConfig } from '../../components/StatusDot';
import type { AdminStats, TicketStatus } from '../../types';

const { Title } = Typography;

export default function AdminStatsPage() {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminStats()
      .then(({ data }) => setStats(data))
      .catch(() => setError('统计数据加载失败，请稍后重试'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Skeleton active paragraph={{ rows: 8 }} />;
  if (error) return <Alert type="error" message={error} showIcon />;
  if (!stats) return null;

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>数据分析</Title>

      {/* Main KPIs */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总工单数"
              value={stats.totalTickets}
              prefix={<FileTextOutlined />}
              styles={{ content: { fontSize: 30, fontWeight: 600, fontVariantNumeric: 'tabular-nums' } }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="AI 偏转率"
              value={stats.deflectionRate}
              suffix="%"
              prefix={<CheckCircleOutlined />}
              styles={{ content: { fontSize: 30, fontWeight: 600, color: '#722ed1', fontVariantNumeric: 'tabular-nums' } }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="平均解决时间"
              value={stats.avgResolutionTimeHours != null ? stats.avgResolutionTimeHours.toFixed(1) : '-'}
              suffix="小时"
              prefix={<ClockCircleOutlined />}
              styles={{ content: { fontSize: 30, fontWeight: 600, fontVariantNumeric: 'tabular-nums' } }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="知识库文章"
              value={stats.kbPublishedCount}
              suffix={`/ ${stats.kbArticleCount} 总数`}
              prefix={<BookOutlined />}
              styles={{ content: { fontSize: 30, fontWeight: 600, color: '#52c41a', fontVariantNumeric: 'tabular-nums' } }}
            />
          </Card>
        </Col>
      </Row>

      {/* Status breakdown */}
      <Card title="工单状态分布">
        <Row gutter={16}>
          {Object.entries(stats.ticketsByStatus).map(([status, count]) => {
            const cfg = statusConfig[status as TicketStatus];
            return (
              <Col span={6} key={status}>
                <Statistic
                  title={cfg?.label || status}
                  value={count}
                  styles={{ content: { color: cfg?.dotColor, fontVariantNumeric: 'tabular-nums' } }}
                />
              </Col>
            );
          })}
        </Row>
      </Card>
    </div>
  );
}
