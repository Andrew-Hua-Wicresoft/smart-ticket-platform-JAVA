import type { TicketPriority } from '../types';

export const priorityConfig: Record<TicketPriority, { color: string; label: string }> = {
  HIGH: { color: '#ff4d4f', label: '高' },
  MEDIUM: { color: '#faad14', label: '中' },
  LOW: { color: '#52c41a', label: '低' },
};

export default function PriorityBadge({ priority }: { priority: TicketPriority }) {
  const cfg = priorityConfig[priority];
  return (
    <div style={{
      width: 24, height: 24, borderRadius: 4,
      background: cfg.color,
      color: '#fff', fontSize: 12, fontWeight: 600,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}>
      {cfg.label}
    </div>
  );
}
