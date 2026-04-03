import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Card, Table, Button, Typography, Statistic, Row, Col, Skeleton } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { listTickets } from '../../api';
import StatusDot from '../../components/StatusDot';
import type { TicketResponse, TicketStatus, TicketPriority } from '../../types';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { Title } = Typography;

const priorityConfig: Record<TicketPriority, { color: string; label: string }> = {
  HIGH: { color: '#ff4d4f', label: '高' },
  MEDIUM: { color: '#faad14', label: '中' },
  LOW: { color: '#52c41a', label: '低' },
};

export default function TicketListPage() {
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const fetchTickets = async (p = 0) => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await listTickets(p);
      setTickets(data.content);
      setTotal(data.totalElements);
    } catch {
      setError('工单列表加载失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchTickets(page); }, [page]);

  // Count by status from current data
  const openCount = tickets.filter((t) => t.status === 'OPEN').length;
  const inProgressCount = tickets.filter((t) => t.status === 'IN_PROGRESS').length;

  const columns = [
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 70,
      render: (p: TicketPriority) => (
        <div style={{
          width: 24, height: 24, borderRadius: 4,
          background: priorityConfig[p].color,
          color: '#fff', fontSize: 12, fontWeight: 600,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          {priorityConfig[p].label}
        </div>
      ),
    },
    {
      title: '工单标题',
      dataIndex: 'title',
      key: 'title',
      render: (title: string, record: TicketResponse) => (
        <a onClick={() => navigate(`/tickets/${record.id}`)} style={{ fontWeight: 500 }}>
          {title}
        </a>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (s: TicketStatus) => <StatusDot status={s} />,
    },
    {
      title: '提交人',
      dataIndex: 'customerName',
      key: 'customerName',
      width: 100,
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 120,
      render: (t: string) => (
        <span style={{ fontSize: 12, color: '#8c8c8c', fontVariantNumeric: 'tabular-nums' }}>
          {dayjs(t).fromNow()}
        </span>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>工单队列</Title>
        <Button icon={<ReloadOutlined />} onClick={() => fetchTickets(page)}>刷新</Button>
      </div>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card size="small">
            <Statistic title="总工单数" value={total} styles={{ content: { fontSize: 30, fontWeight: 600, fontVariantNumeric: 'tabular-nums' } }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="待处理" value={openCount} styles={{ content: { fontSize: 30, fontWeight: 600, color: '#1677ff', fontVariantNumeric: 'tabular-nums' } }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="处理中" value={inProgressCount} styles={{ content: { fontSize: 30, fontWeight: 600, color: '#faad14', fontVariantNumeric: 'tabular-nums' } }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="已完成" value={total - openCount - inProgressCount}
              styles={{ content: { fontSize: 30, fontWeight: 600, color: '#52c41a', fontVariantNumeric: 'tabular-nums' } }} />
          </Card>
        </Col>
      </Row>

      <Card>
        {error ? <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} /> : null}
        {loading ? <Skeleton active paragraph={{ rows: 8 }} /> : (
          <Table
            dataSource={tickets}
            columns={columns}
            rowKey="id"
            size="middle"
            pagination={{
              current: page + 1,
              pageSize: 20,
              total,
              onChange: (p) => setPage(p - 1),
              showTotal: (t) => `共 ${t} 条`,
            }}
            onRow={(record) => ({
              onClick: () => navigate(`/tickets/${record.id}`),
              style: { cursor: 'pointer' },
            })}
          />
        )}
      </Card>
    </div>
  );
}
