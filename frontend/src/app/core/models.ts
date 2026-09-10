export type Channel = 'EMAIL' | 'SMS' | 'WHATSAPP';
export type NotificationEnvironment = 'test' | 'prod' | 'TEST' | 'PROD';
export type NotificationStatus =
  | 'RECEIVED'
  | 'QUEUED'
  | 'RENDERING'
  | 'SENDING'
  | 'SENT'
  | 'DELIVERED'
  | 'FAILED'
  | 'DEAD'
  | 'CANCELLED'
  | 'BOUNCED'
  | 'REJECTED';

export interface NotificationItem {
  id: string;
  tenantId: string;
  channel: Channel;
  environment?: NotificationEnvironment | null;
  status: NotificationStatus;
  from: string;
  to: string[];
  subject: string | null;
  templateName: string | null;
  priority: string;
  attemptCount: number;
  maxAttempts: number;
  lastError: string | null;
  createdAt: string;
  updatedAt: string;
  sentAt: string | null;
}

export interface NotificationEvent {
  id: number;
  sequenceNo: number;
  eventType: string;
  payload: Record<string, unknown> | null;
  occurredAt: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TemplateItem {
  id: string;
  name: string;
  version: number;
  channel: Channel;
  engine: string;
  subjectTemplate: string | null;
  bodyTemplate: string;
  active: boolean;
  createdAt: string;
}

export interface KpiResponse {
  windowHours: number;
  total: number;
  sent: number;
  failed: number;
  dead: number;
  queued: number;
  successRate: number;
  avgAttempts: number;
  outboxPending: number;
  byChannel: { channel: Channel; count: number }[];
  byStatus: { status: string; count: number }[];
  volumeLast24hByHour: Record<string, number>;
  estimatedCostByChannel: {
    channel: Channel;
    count: number;
    unitCost: number;
    estimatedCost: number;
  }[];
  estimatedCostTotal: number;
}

export const STATUS_LABELS: Record<string, string> = {
  RECEIVED: 'Reçue',
  QUEUED: 'En file',
  RENDERING: 'Rendu',
  SENDING: 'Envoi',
  SENT: 'Envoyée',
  DELIVERED: 'Livrée',
  FAILED: 'Échec',
  DEAD: 'Dead letter',
  CANCELLED: 'Annulée',
  BOUNCED: 'Rejetée (bounce)',
  REJECTED: 'Rejetée',
};

export function statusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status;
}
