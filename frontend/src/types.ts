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

export interface AiSearchResult {
  kbId: number;
  title: string;
  content: string;
  similarity: number;
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
  avgResolutionTimeHours: number | null;
  kbArticleCount: number;
  kbPublishedCount: number;
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
