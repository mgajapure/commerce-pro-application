export type NotificationType = 'ORDER_UPDATE' | 'INVENTORY_ALERT' | 'SYSTEM' | 'CUSTOMER_UPDATE' | 'REPORT_READY' | 'SECURITY';
export type NotificationPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'BOTH';

export interface Notification {
  id: string;
  userId: string;
  title: string;
  message: string;
  type: NotificationType;
  channel: NotificationChannel;
  priority: NotificationPriority;
  isRead: boolean;
  readAt?: Date;
  link?: string;
  metadata?: string;
  createdAt: Date;
}

export interface NotificationStats {
  total: number;
  unread: number;
  byType: Record<string, number>;
  byPriority: Record<string, number>;
}

export interface CreateNotificationRequest {
  userId: string;
  title: string;
  message: string;
  type: NotificationType;
  channel?: NotificationChannel;
  priority?: NotificationPriority;
  link?: string;
  metadata?: string;
}
