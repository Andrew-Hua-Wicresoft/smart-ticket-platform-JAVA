import { useEffect, useState } from 'react';
import { Card, Table, Tag, Typography, message } from 'antd';
import dayjs from 'dayjs';
import { listAuditLogs } from '../../api';
import type { AuditLog } from '../../types';

const { Title } = Typography;

export default function AuditLogPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    listAuditLogs()
      .then(({ data }) => setLogs(data.content))
      .catch(() => message.error('加载审计日志失败'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <Title level={4} style={{ marginBottom: 16 }}>审计日志</Title>
      <Card>
        <Table<AuditLog>
          rowKey="id"
          loading={loading}
          dataSource={logs}
          pagination={false}
          columns={[
            {
              title: '时间',
              dataIndex: 'createdAt',
              key: 'createdAt',
              width: 180,
              render: (value: string) => dayjs(value).format('YYYY-MM-DD HH:mm:ss'),
            },
            {
              title: '操作人',
              key: 'actor',
              width: 180,
              render: (_, record) => record.actorName ? `${record.actorName} (${record.actorRole})` : '系统',
            },
            {
              title: '动作',
              dataIndex: 'action',
              key: 'action',
              width: 180,
              render: (value: string) => <Tag color="blue">{value}</Tag>,
            },
            {
              title: '资源',
              key: 'resource',
              width: 180,
              render: (_, record) => `${record.resourceType}${record.resourceId ? ` #${record.resourceId}` : ''}`,
            },
            {
              title: '摘要',
              dataIndex: 'summary',
              key: 'summary',
            },
            {
              title: 'Request ID',
              dataIndex: 'requestId',
              key: 'requestId',
              width: 220,
              render: (value: string | null) => value || '-',
            },
          ]}
        />
      </Card>
    </div>
  );
}
