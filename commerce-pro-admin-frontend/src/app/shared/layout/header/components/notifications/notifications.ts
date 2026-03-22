import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NotificationService } from '../../../../../core/services/notification/notification.service';
import { NotificationType } from '../../../../../core/models/notification/notification.model';
import { OutsideClickDirective } from '../../../../directives/outside-click.directive';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, RouterModule, OutsideClickDirective],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss'
})
export class Notifications {
  private notificationService = inject(NotificationService);

  isOpen = signal(false);
  notifications = this.notificationService.notifications;
  unreadCount = this.notificationService.unreadCount;

  toggle() {
    this.isOpen.update(v => !v);
  }

  markAllRead() {
    this.notificationService.markAllAsRead().subscribe();
  }

  markAsRead(id: string) {
    this.notificationService.markAsRead(id).subscribe();
  }

  getIcon(type: NotificationType): string {
    const icons: Record<string, string> = {
      ORDER_UPDATE: 'bag-check',
      INVENTORY_ALERT: 'exclamation-triangle',
      SYSTEM: 'gear',
      CUSTOMER_UPDATE: 'person',
      REPORT_READY: 'file-earmark-bar-graph',
      SECURITY: 'shield-lock'
    };
    return icons[type] || 'bell';
  }

  getIconBg(type: NotificationType): string {
    const bgs: Record<string, string> = {
      ORDER_UPDATE: 'bg-blue-100',
      INVENTORY_ALERT: 'bg-yellow-100',
      SYSTEM: 'bg-gray-100',
      CUSTOMER_UPDATE: 'bg-green-100',
      REPORT_READY: 'bg-purple-100',
      SECURITY: 'bg-red-100'
    };
    return bgs[type] || 'bg-gray-100';
  }

  getIconColor(type: NotificationType): string {
    const colors: Record<string, string> = {
      ORDER_UPDATE: 'text-blue-600',
      INVENTORY_ALERT: 'text-yellow-600',
      SYSTEM: 'text-gray-600',
      CUSTOMER_UPDATE: 'text-green-600',
      REPORT_READY: 'text-purple-600',
      SECURITY: 'text-red-600'
    };
    return colors[type] || 'text-gray-600';
  }

  timeAgo(date: Date): string {
    const seconds = Math.floor((Date.now() - new Date(date).getTime()) / 1000);
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return `${Math.floor(seconds / 86400)}d ago`;
  }
}
