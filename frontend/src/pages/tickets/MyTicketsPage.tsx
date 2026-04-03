import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Card, Table, Typography, Empty, Skeleton } from 'antd';
import { listTickets } from '../../api';
import StatusDot from '../../components/StatusDot';
import PriorityBadge from '../../components/PriorityBadge';
import type { TicketResponse, TicketStatus, TicketPriority } from '../../types';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { Title } = Typography;

export default function MyTicketsPage() {
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    setError(null);
    listTickets(page)
      .then(({ data }) => {
        setTickets(data.content);
        setTotal(data.totalElements);
      })
      .catch(() => setError('工单列表加载失败，请稍后重试'))
      .finally(() => setLoading(false));
  }, [page]);

  const columns = [
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 70,
      render: (p: TicketPriority) => <PriorityBadge priority={p} />,
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
      title: '负责工程师',
      dataIndex: 'assignedEngineerName',
      key: 'assignedEngineerName',
      width: 120,
      render: (name: string | null) => name || <span style={{ color: '#bfbfbf' }}>未分配</span>,
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
      <Title level={4} style={{ marginBottom: 16 }}>我的工单</Title>
      <Card>
        {error ? <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} /> : null}
        {loading ? <Skeleton active paragraph={{ rows: 6 }} /> : (
          tickets.length === 0 ? (
            <Empty description="暂无工单，去提交一个吧" />
          ) : (
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
          )
        )}
      </Card>
    </div>
  );
}
