import { useEffect, useState } from 'react';
import { Alert, Card, Typography, Statistic, Row, Col, Skeleton, Progress, Tooltip } from 'antd';
import {
  FileTextOutlined, CheckCircleOutlined, ClockCircleOutlined, BookOutlined, InfoCircleOutlined,
} from '@ant-design/icons';
import { getAdminStats } from '../../api';
import { statusConfig } from '../../components/StatusDot';
import type { AdminStats, TicketStatus } from '../../types';

const { Title } = Typography;

function clampPercent(value: number) {
  return Math.max(0, Math.min(100, value));
}

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
          <Card style={{ height: '100%' }}>
            <Statistic
              title={(
                <span>
                  自助解决率{' '}
                  <Tooltip title="自助解决率 = 自助解决次数 / (自助解决次数 + 已提交工单数)">
                    <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
                  </Tooltip>
                </span>
              )}
              value={stats.deflectionRate}
              suffix="%"
              prefix={<CheckCircleOutlined />}
              styles={{ content: { fontSize: 30, fontWeight: 600, color: '#722ed1', fontVariantNumeric: 'tabular-nums' } }}
            />
            <Progress
              percent={clampPercent(stats.deflectionRate)}
              showInfo={false}
              strokeColor="#722ed1"
              style={{ marginTop: 8 }}
            />
            <div style={{ marginTop: 8, color: '#8c8c8c', fontSize: 12 }}>
              {stats.deflectionCount} 次自助解决 / {stats.deflectionOpportunityCount} 次总请求
            </div>
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
          <Card style={{ height: '100%' }}>
            <Statistic
              title={(
                <span>
                  知识库发布状态{' '}
                  <Tooltip title="已发布文章 / 知识库文章总数，草稿需审核后才会进入用户知识库">
                    <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
                  </Tooltip>
                </span>
              )}
              value={stats.kbPublishedCount}
              suffix={`/ ${stats.kbArticleCount}`}
              prefix={<BookOutlined />}
              styles={{ content: { fontSize: 30, fontWeight: 600, color: '#52c41a', fontVariantNumeric: 'tabular-nums' } }}
            />
            <Progress
              percent={clampPercent(stats.kbPublicationRate)}
              showInfo={false}
              strokeColor="#52c41a"
              style={{ marginTop: 8 }}
            />
            <div style={{ marginTop: 8, color: '#8c8c8c', fontSize: 12 }}>
              草稿 {stats.kbDraftCount} 篇 · 发布率 {stats.kbPublicationRate}%
            </div>
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
