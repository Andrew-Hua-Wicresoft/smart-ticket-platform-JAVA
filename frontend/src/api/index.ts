import client from './client';
import type {
  LoginResponse, TicketResponse, AiSearchResult,
  KbArticle, AdminStats, Page, TicketComment,
  NotificationItem, UnreadNotificationCount, AuditLog,
} from '../types';

// Auth
export const login = (username: string, password: string) =>
  client.post<LoginResponse>('/auth/login', { username, password });

// Tickets
export const createTicket = (title: string, description: string) =>
  client.post<TicketResponse>('/tickets', { title, description });

export const listTickets = (page = 0, size = 20) =>
  client.get<Page<TicketResponse>>('/tickets', { params: { page, size, sort: 'createdAt,desc' } });

export const getTicket = (id: number) =>
  client.get<TicketResponse>(`/tickets/${id}`);

export const listTicketComments = (ticketId: number) =>
  client.get<TicketComment[]>(`/tickets/${ticketId}/comments`);

export const addTicketComment = (ticketId: number, content: string) =>
  client.post<TicketComment>(`/tickets/${ticketId}/comments`, { content });

export const assignTicket = (id: number) =>
  client.put<TicketResponse>(`/tickets/${id}/assign`);

export const resolveTicket = (id: number, resolutionNotes: string) =>
  client.put<TicketResponse>(`/tickets/${id}/resolve`, { resolutionNotes });

// AI
export const aiSearch = (query: string, topK = 3) =>
  client.post<AiSearchResult[]>('/ai/search', { query, topK });

export const aiRefine = (title: string, description: string) =>
  client.post<{ questions: string }>('/ai/refine', { title, description });

export const aiSuggest = (ticketId: number, title: string, description: string) =>
  client.post<{ suggestion: string }>('/ai/suggest', { ticketId, title, description });

export const aiSimilar = (query: string) =>
  client.post<AiSearchResult[]>('/ai/similar', { query, topK: 5 });

// Deflections
export const logDeflection = (matchedKbId: number, searchQuery: string) =>
  client.post('/deflections', { matchedKbId, searchQuery });

// Knowledge Base
export const listDraftArticles = (page = 0, size = 20) =>
  client.get<Page<KbArticle>>('/kb/drafts', { params: { page, size } });

export const listPublishedArticles = (page = 0, size = 20) =>
  client.get<Page<KbArticle>>('/kb/published', { params: { page, size } });

export const publishArticle = (id: number) =>
  client.put<KbArticle>(`/kb/${id}/publish`);

export const updateArticle = (id: number, title: string, content: string) =>
  client.put<KbArticle>(`/kb/${id}`, { title, content });

export const deleteArticle = (id: number) =>
  client.delete(`/kb/${id}`);

// Admin
export const getAdminStats = () =>
  client.get<AdminStats>('/admin/stats');

export const listNotifications = (page = 0, size = 20) =>
  client.get<Page<NotificationItem>>('/notifications', { params: { page, size } });

export const markNotificationRead = (id: number) =>
  client.put<NotificationItem>(`/notifications/${id}/read`);

export const getUnreadNotificationCount = () =>
  client.get<UnreadNotificationCount>('/notifications/unread-count');

export const listAuditLogs = (page = 0, size = 20) =>
  client.get<Page<AuditLog>>('/admin/audit-logs', { params: { page, size } });
