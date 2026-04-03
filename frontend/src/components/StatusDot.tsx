import type { TicketStatus } from '../types';

export const statusConfig: Record<TicketStatus, { dotColor: string; label: string }> = {
  OPEN: { dotColor: '#1677ff', label: '待处理' },
  IN_PROGRESS: { dotColor: '#faad14', label: '处理中' },
  RESOLVED: { dotColor: '#52c41a', label: '已解决' },
  CLOSED: { dotColor: '#8c8c8c', label: '已关闭' },
};

export default function StatusDot({ status }: { status: TicketStatus }) {
  const cfg = statusConfig[status];
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
      <span style={{ width: 6, height: 6, borderRadius: '50%', background: cfg.dotColor, flexShrink: 0 }} />
      <span style={{ fontSize: 14 }}>{cfg.label}</span>
    </span>
  );
}
