import { useEffect, useState } from 'react';
import type { CSSProperties } from 'react';
import { Alert, Card, Typography, Statistic, Row, Col, Skeleton, Progress, Tooltip } from 'antd';
import {
  FileTextOutlined, CheckCircleOutlined, ClockCircleOutlined, BookOutlined, InfoCircleOutlined,
} from '@ant-design/icons';
import { getAdminStats } from '../../api';
import { statusConfig } from '../../components/StatusDot';
import type { AdminStats, TicketStatus } from '../../types';

const { Title } = Typography;

const kpiColStyle: CSSProperties = { display: 'flex' };
const kpiCardStyle: CSSProperties = { width: '100%', minHeight: 178, height: '100%' };
const kpiMetaStyle: CSSProperties = {
  marginTop: 8,
  color: '#8c8c8c',
  fontSize: 12,
  lineHeight: '20px',
};

function toNumber(value: number | null | undefined) {
  return Number.isFinite(value) ? Number(value) : 0;
}

function roundOneDecimal(value: number) {
  return Math.round(value * 10) / 10;
}

function clampPercent(value: number | null | undefined) {
  return Math.max(0, Math.min(100, toNumber(value)));
}

function formatPercent(value: number) {
  return Number.isInteger(value) ? String(value) : value.toFixed(1);
}

function inferDeflectionCount(totalTickets: number, deflectionRate: number) {
  if (totalTickets <= 0 || deflectionRate <= 0 || deflectionRate >= 100) return 0;
  return Math.round((deflectionRate * totalTickets) / (100 - deflectionRate));
}

function normalizeStats(stats: AdminStats): AdminStats {
  const totalTickets = toNumber(stats.totalTickets);
  const deflectionRate = clampPercent(stats.deflectionRate);
  const deflectionCount = stats.deflectionCount != null
    ? toNumber(stats.deflectionCount)
    : inferDeflectionCount(totalTickets, deflectionRate);
  const deflectionOpportunityCount = stats.deflectionOpportunityCount != null
    ? toNumber(stats.deflectionOpportunityCount)
    : totalTickets + deflectionCount;
  const kbArticleCount = toNumber(stats.kbArticleCount);
  const kbPublishedCount = toNumber(stats.kbPublishedCount);
  const kbDraftCount = stats.kbDraftCount != null
    ? toNumber(stats.kbDraftCount)
    : Math.max(kbArticleCount - kbPublishedCount, 0);
  const kbPublicationRate = stats.kbPublicationRate != null
    ? clampPercent(stats.kbPublicationRate)
    : roundOneDecimal(kbArticleCount > 0 ? (kbPublishedCount / kbArticleCount) * 100 : 0);

  return {
    ...stats,
    totalTickets,
    deflectionRate,
    deflectionCount,
    deflectionOpportunityCount,
    kbArticleCount,
    kbPublishedCount,
    kbDraftCount,
    kbPublicationRate,
  };
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

  const normalizedStats = normalizeStats(stats);
  const deflectionRate = clampPercent(normalizedStats.deflectionRate);
  const kbPublicationRate = clampPercent(normalizedStats.kbPublicationRate);

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>数据分析</Title>

      {/* Main KPIs */}
      <Row gutter={16} style={{ marginBottom: 16 }} align="stretch">
        <Col span={6} style={kpiColStyle}>
          <Card style={kpiCardStyle}>
            <Statistic
              title="总工单数"
              value={normalizedStats.totalTickets}
              prefix={<FileTextOutlined />}
              styles={{ content: { fontSize: 30, fontWeight: 600, fontVariantNumeric: 'tabular-nums' } }}
            />
          </Card>
        </Col>
        <Col span={6} style={kpiColStyle}>
          <Card style={kpiCardStyle}>
            <Statistic
              title={(
                <span>
                  自助解决率{' '}
                  <Tooltip title="自助解决率 = 自助解决次数 / (自助解决次数 + 已提交工单数)">
                    <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
                  </Tooltip>
                </span>
              )}
              value={deflectionRate}
              suffix="%"
              prefix={<CheckCircleOutlined />}
              styles={{ content: { fontSize: 30, fontWeight: 600, color: '#722ed1', fontVariantNumeric: 'tabular-nums' } }}
            />
            <Progress
              percent={deflectionRate}
              format={(percent) => `${formatPercent(toNumber(percent))}%`}
              strokeColor="#722ed1"
              style={{ marginTop: 8 }}
            />
            <div style={kpiMetaStyle}>
              自助解决 {normalizedStats.deflectionCount} 次 · 总请求 {normalizedStats.deflectionOpportunityCount} 次
            </div>
          </Card>
        </Col>
        <Col span={6} style={kpiColStyle}>
          <Card style={kpiCardStyle}>
            <Statistic
              title="平均解决时间"
              value={normalizedStats.avgResolutionTimeHours != null ? normalizedStats.avgResolutionTimeHours.toFixed(1) : '-'}
              suffix="小时"
              prefix={<ClockCircleOutlined />}
              styles={{ content: { fontSize: 30, fontWeight: 600, fontVariantNumeric: 'tabular-nums' } }}
            />
          </Card>
        </Col>
        <Col span={6} style={kpiColStyle}>
          <Card style={kpiCardStyle}>
            <Statistic
              title={(
                <span>
                  知识库发布状态{' '}
                  <Tooltip title="已发布文章 / 知识库文章总数，草稿需审核后才会进入用户知识库">
                    <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
                  </Tooltip>
                </span>
              )}
              value={normalizedStats.kbPublishedCount}
              suffix={`/ ${normalizedStats.kbArticleCount}`}
              prefix={<BookOutlined />}
              styles={{ content: { fontSize: 30, fontWeight: 600, color: '#52c41a', fontVariantNumeric: 'tabular-nums' } }}
            />
            <Progress
              percent={kbPublicationRate}
              format={(percent) => `${formatPercent(toNumber(percent))}%`}
              strokeColor="#52c41a"
              style={{ marginTop: 8 }}
            />
            <div style={kpiMetaStyle}>
              已发布 {normalizedStats.kbPublishedCount} 篇 · 草稿 {normalizedStats.kbDraftCount} 篇 · 总计 {normalizedStats.kbArticleCount} 篇
            </div>
          </Card>
        </Col>
      </Row>

      {/* Status breakdown */}
      <Card title="工单状态分布">
        <Row gutter={16}>
          {Object.entries(normalizedStats.ticketsByStatus).map(([status, count]) => {
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
