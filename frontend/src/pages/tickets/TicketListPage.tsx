import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Alert, Card, Table, Button, Typography, Statistic, Row, Col, Skeleton, Input, Select, Space } from 'antd';
import type { TableProps } from 'antd';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { listTickets } from '../../api';
import StatusDot, { statusConfig } from '../../components/StatusDot';
import PriorityBadge, { priorityConfig } from '../../components/PriorityBadge';
import { useAuthStore } from '../../stores/authStore';
import type {
  TicketResponse,
  TicketStatus,
  TicketPriority,
  TicketAssigneeScope,
  TicketListFilters,
  TicketSortDirection,
  TicketSortField,
} from '../../types';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { Title, Text } = Typography;

const ALL_STATUSES: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
const ENGINEER_STATUSES: TicketStatus[] = ['OPEN', 'IN_PROGRESS'];
const ALL_PRIORITIES: TicketPriority[] = ['HIGH', 'MEDIUM', 'LOW'];
const ALL_ASSIGNEES: TicketAssigneeScope[] = ['ALL', 'UNASSIGNED', 'ME'];
const SORT_FIELDS: TicketSortField[] = ['priority', 'title', 'status', 'customer.name', 'assignedEngineer.name', 'createdAt'];
const DEFAULT_SORT: TicketSortState = { field: 'createdAt', order: 'descend' };

const assigneeLabels: Record<TicketAssigneeScope, string> = {
  ALL: '全部负责人',
  UNASSIGNED: '未分配',
  ME: '我负责',
};

type TicketSortOrder = 'ascend' | 'descend';

interface TicketSortState {
  field: TicketSortField;
  order: TicketSortOrder;
}

function parseCsvParam<T extends string>(value: string | null, allowed: readonly T[]) {
  if (!value) return [];
  return value
    .split(',')
    .map((item) => item.trim().toUpperCase())
    .filter((item): item is T => allowed.includes(item as T));
}

function parseFilters(searchParams: URLSearchParams): TicketListFilters {
  const assignee = searchParams.get('assignee')?.trim().toUpperCase() as TicketAssigneeScope | undefined;
  return {
    status: parseCsvParam(searchParams.get('status'), ALL_STATUSES),
    priority: parseCsvParam(searchParams.get('priority'), ALL_PRIORITIES),
    keyword: searchParams.get('keyword') || '',
    assignee: assignee && ALL_ASSIGNEES.includes(assignee) ? assignee : 'ALL',
  };
}

function isSortField(value: string): value is TicketSortField {
  return SORT_FIELDS.includes(value as TicketSortField);
}

function parseSort(searchParams: URLSearchParams): TicketSortState {
  const [field, direction] = (searchParams.get('sort') || '').split(',');
  if (field && direction && isSortField(field) && ['asc', 'desc'].includes(direction)) {
    return { field, order: direction === 'asc' ? 'ascend' : 'descend' };
  }
  return DEFAULT_SORT;
}

function buildSearchParams(filters: TicketListFilters, sort: TicketSortState = DEFAULT_SORT) {
  const params = new URLSearchParams();
  if (filters.status?.length) params.set('status', filters.status.join(','));
  if (filters.priority?.length) params.set('priority', filters.priority.join(','));
  if (filters.keyword?.trim()) params.set('keyword', filters.keyword.trim());
  if (filters.assignee && filters.assignee !== 'ALL') params.set('assignee', filters.assignee);
  if (sort.field !== DEFAULT_SORT.field || sort.order !== DEFAULT_SORT.order) {
    params.set('sort', toSortParam(sort));
  }
  return params;
}

function toSortParam(sort: TicketSortState): `${TicketSortField},${TicketSortDirection}` {
  return `${sort.field},${sort.order === 'ascend' ? 'asc' : 'desc'}`;
}

function sortOrderFor(currentSort: TicketSortState, field: TicketSortField) {
  return currentSort.field === field ? currentSort.order : null;
}

function resolveTableSort(sorter: Parameters<NonNullable<TableProps<TicketResponse>['onChange']>>[2]): TicketSortState {
  const activeSorter = Array.isArray(sorter) ? sorter[0] : sorter;
  const columnKey = activeSorter?.columnKey;
  const field = typeof columnKey === 'string' ? columnKey : '';
  if (activeSorter?.order && isSortField(field)) {
    return { field, order: activeSorter.order };
  }
  return DEFAULT_SORT;
}

export default function TicketListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [draftFilters, setDraftFilters] = useState<TicketListFilters>(() => parseFilters(searchParams));
  const navigate = useNavigate();
  const role = useAuthStore((state) => state.role);
  const appliedFilterKey = searchParams.toString();
  const sort = parseSort(searchParams);

  useEffect(() => {
    let cancelled = false;

    async function fetchTickets() {
      setLoading(true);
      setError(null);
      try {
        const currentSearchParams = new URLSearchParams(appliedFilterKey);
        const { data } = await listTickets(page, 20, parseFilters(currentSearchParams), toSortParam(parseSort(currentSearchParams)));
        if (!cancelled) {
          setTickets(data.content);
          setTotal(data.totalElements);
        }
      } catch {
        if (!cancelled) setError('工单列表加载失败，请稍后重试');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    fetchTickets();
    return () => { cancelled = true; };
  }, [page, appliedFilterKey, refreshKey]);

  useEffect(() => {
    setDraftFilters(parseFilters(new URLSearchParams(appliedFilterKey)));
  }, [appliedFilterKey]);

  // Count by status from current data
  const openCount = tickets.filter((t) => t.status === 'OPEN').length;
  const inProgressCount = tickets.filter((t) => t.status === 'IN_PROGRESS').length;
  const doneCount = tickets.filter((t) => t.status === 'RESOLVED' || t.status === 'CLOSED').length;
  const statusOptions = (role === 'ADMIN' ? ALL_STATUSES : ENGINEER_STATUSES).map((status) => ({
    label: statusConfig[status].label,
    value: status,
  }));
  const priorityOptions = ALL_PRIORITIES.map((priority) => ({
    label: priorityConfig[priority].label,
    value: priority,
  }));
  const assigneeOptions = ALL_ASSIGNEES.map((assignee) => ({
    label: assigneeLabels[assignee],
    value: assignee,
  }));

  const applyFilters = () => {
    setPage(0);
    setSearchParams(buildSearchParams(draftFilters, sort));
  };

  const resetFilters = () => {
    const emptyFilters: TicketListFilters = { status: [], priority: [], keyword: '', assignee: 'ALL' };
    setDraftFilters(emptyFilters);
    setPage(0);
    setSearchParams(buildSearchParams(emptyFilters));
  };

  const handleTableChange: TableProps<TicketResponse>['onChange'] = (pagination, _filters, sorter, extra) => {
    if (extra.action === 'sort') {
      setPage(0);
      setSearchParams(buildSearchParams(parseFilters(searchParams), resolveTableSort(sorter)));
      return;
    }

    setPage((pagination.current || 1) - 1);
  };

  const columns: TableProps<TicketResponse>['columns'] = [
    {
      title: '优先级',
      dataIndex: 'priority',
      key: 'priority',
      width: 70,
      sorter: true,
      sortDirections: ['descend', 'ascend'],
      sortOrder: sortOrderFor(sort, 'priority'),
      render: (p: TicketPriority) => <PriorityBadge priority={p} />,
    },
    {
      title: '工单标题',
      dataIndex: 'title',
      key: 'title',
      sorter: true,
      sortOrder: sortOrderFor(sort, 'title'),
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
      sorter: true,
      sortDirections: ['ascend', 'descend'],
      sortOrder: sortOrderFor(sort, 'status'),
      render: (s: TicketStatus) => <StatusDot status={s} />,
    },
    {
      title: '提交人',
      dataIndex: 'customerName',
      key: 'customer.name',
      width: 100,
      sorter: true,
      sortOrder: sortOrderFor(sort, 'customer.name'),
    },
    {
      title: '负责人',
      dataIndex: 'assignedEngineerName',
      key: 'assignedEngineer.name',
      width: 120,
      sorter: true,
      sortOrder: sortOrderFor(sort, 'assignedEngineer.name'),
      render: (name: string | null) => name || <span style={{ color: '#bfbfbf' }}>未分配</span>,
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 120,
      sorter: true,
      sortOrder: sortOrderFor(sort, 'createdAt'),
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
      </div>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card size="small">
            <Statistic title="筛选结果总数" value={total} styles={{ content: { fontSize: 30, fontWeight: 600, fontVariantNumeric: 'tabular-nums' } }} />
            <Text type="secondary" style={{ fontSize: 12 }}>当前筛选条件下全部页</Text>
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="本页待处理" value={openCount} styles={{ content: { fontSize: 30, fontWeight: 600, color: '#1677ff', fontVariantNumeric: 'tabular-nums' } }} />
            <Text type="secondary" style={{ fontSize: 12 }}>仅当前表格页 {tickets.length} 条内</Text>
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="本页处理中" value={inProgressCount} styles={{ content: { fontSize: 30, fontWeight: 600, color: '#faad14', fontVariantNumeric: 'tabular-nums' } }} />
            <Text type="secondary" style={{ fontSize: 12 }}>仅当前表格页 {tickets.length} 条内</Text>
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="本页已完成" value={doneCount}
              styles={{ content: { fontSize: 30, fontWeight: 600, color: '#52c41a', fontVariantNumeric: 'tabular-nums' } }} />
            <Text type="secondary" style={{ fontSize: 12 }}>仅当前表格页 {tickets.length} 条内</Text>
          </Card>
        </Col>
      </Row>

      <Card>
        <Space wrap style={{ marginBottom: 16 }}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索标题、描述、提交人、负责人"
            allowClear
            value={draftFilters.keyword}
            onChange={(event) => setDraftFilters((current) => ({ ...current, keyword: event.target.value }))}
            onPressEnter={applyFilters}
            style={{ width: 280 }}
          />
          <Select
            mode="multiple"
            allowClear
            placeholder="状态"
            value={draftFilters.status}
            options={statusOptions}
            onChange={(status) => setDraftFilters((current) => ({ ...current, status }))}
            style={{ width: 220 }}
          />
          <Select
            mode="multiple"
            allowClear
            placeholder="优先级"
            value={draftFilters.priority}
            options={priorityOptions}
            onChange={(priority) => setDraftFilters((current) => ({ ...current, priority }))}
            style={{ width: 180 }}
          />
          <Select
            value={draftFilters.assignee}
            options={assigneeOptions}
            onChange={(assignee) => setDraftFilters((current) => ({ ...current, assignee }))}
            style={{ width: 150 }}
          />
          <Button type="primary" onClick={applyFilters}>查询</Button>
          <Button onClick={resetFilters}>重置</Button>
          <Button icon={<ReloadOutlined />} onClick={() => setRefreshKey((key) => key + 1)}>刷新</Button>
        </Space>
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
              showTotal: (t) => `共 ${t} 条`,
            }}
            onChange={handleTableChange}
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
