export type UserRole = 'CUSTOMER' | 'ENGINEER' | 'ADMIN';
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type KbArticleStatus = 'DRAFT' | 'PUBLISHED';

export interface LoginResponse {
  token: string;
  expiresIn: number;
  userId: number;
  name: string;
  role: UserRole;
}

export interface TicketResponse {
  id: number;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  priorityReason: string | null;
  imageUrls: string[];
  customerId: number;
  customerName: string;
  assignedEngineerId: number | null;
  assignedEngineerName: string | null;
  resolutionNotes: string | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
}

export interface TicketComment {
  id: number;
  ticketId: number;
  authorId: number;
  authorName: string;
  authorRole: UserRole;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface AiSearchResult {
  kbId: number;
  title: string;
  content: string;
  similarity: number;
}

export interface AiSuggestionSnapshot {
  available: boolean;
  suggestion: string | null;
  createdAt: string | null;
}

export interface KbArticle {
  id: number;
  title: string;
  content: string;
  status: KbArticleStatus;
  sourceTicketId: number | null;
  createdByName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminStats {
  totalTickets: number;
  ticketsByStatus: Record<string, number>;
  deflectionRate: number;
  deflectionCount: number;
  deflectionOpportunityCount: number;
  avgResolutionTimeHours: number | null;
  kbArticleCount: number;
  kbPublishedCount: number;
  kbDraftCount: number;
  kbPublicationRate: number;
}

export type NotificationType =
  | 'TICKET_CREATED'
  | 'TICKET_ASSIGNED'
  | 'TICKET_RESOLVED'
  | 'TICKET_COMMENTED'
  | 'KB_ARTICLE_PUBLISHED';

export interface NotificationItem {
  id: number;
  type: NotificationType;
  title: string;
  content: string;
  ticketId: number | null;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}

export interface UnreadNotificationCount {
  unreadCount: number;
}

export type AuditAction =
  | 'LOGIN_SUCCESS'
  | 'TICKET_CREATED'
  | 'TICKET_ASSIGNED'
  | 'TICKET_RESOLVED'
  | 'TICKET_COMMENTED'
  | 'AI_ANALYSIS_COMPLETED'
  | 'KB_ARTICLE_DRAFT_GENERATED'
  | 'KB_ARTICLE_PUBLISHED'
  | 'KB_ARTICLE_UPDATED'
  | 'KB_ARTICLE_DELETED';

export interface AuditLog {
  id: number;
  actorId: number | null;
  actorName: string | null;
  actorRole: UserRole | null;
  action: AuditAction;
  resourceType: string;
  resourceId: number | null;
  summary: string;
  requestId: string | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ErrorResponse {
  code: string;
  message: string;
}
