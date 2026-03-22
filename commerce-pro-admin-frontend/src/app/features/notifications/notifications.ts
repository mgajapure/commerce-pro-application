import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../../core/services/notification/notification.service';
import { Notification, NotificationType, NotificationPriority } from '../../core/models/notification/notification.model';
import { AlertService } from '../../shared/services/alert.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss'
})
export class NotificationsPage implements OnInit {
  private notificationService = inject(NotificationService);
  private alertSvc = inject(AlertService);

  // Data from service
  notifications = this.notificationService.notifications;
  isLoading = this.notificationService.isLoading;
  error = this.notificationService.currentError;
  stats = this.notificationService.notificationStats;
  unreadCount = this.notificationService.unreadCount;

  ngOnInit() {
    this.notificationService.loadNotifications();
    this.notificationService.loadStats();
  }

  // View state
  filterType = signal<NotificationType | ''>('');
  filterPriority = signal<NotificationPriority | ''>('');
  filterRead = signal<'all' | 'read' | 'unread'>('all');
  searchQuery = signal('');
  selectedIds = signal<Set<string>>(new Set());

  // Filter options
  typeOptions: { value: NotificationType; label: string; icon: string; color: string }[] = [
    { value: 'ORDER_UPDATE', label: 'Order Update', icon: 'bag-check', color: 'text-blue-600 bg-blue-100' },
    { value: 'INVENTORY_ALERT', label: 'Inventory Alert', icon: 'exclamation-triangle', color: 'text-yellow-600 bg-yellow-100' },
    { value: 'SYSTEM', label: 'System', icon: 'gear', color: 'text-gray-600 bg-gray-100' },
    { value: 'CUSTOMER_UPDATE', label: 'Customer', icon: 'person', color: 'text-green-600 bg-green-100' },
    { value: 'REPORT_READY', label: 'Report Ready', icon: 'file-earmark-bar-graph', color: 'text-purple-600 bg-purple-100' },
    { value: 'SECURITY', label: 'Security', icon: 'shield-lock', color: 'text-red-600 bg-red-100' }
  ];

  priorityOptions: { value: NotificationPriority; label: string; color: string }[] = [
    { value: 'LOW', label: 'Low', color: 'bg-gray-100 text-gray-600' },
    { value: 'NORMAL', label: 'Normal', color: 'bg-blue-100 text-blue-600' },
    { value: 'HIGH', label: 'High', color: 'bg-orange-100 text-orange-600' },
    { value: 'URGENT', label: 'Urgent', color: 'bg-red-100 text-red-600' }
  ];

  // Computed filtered list
  filteredNotifications = computed(() => {
    let list = this.notifications();
    const search = this.searchQuery().toLowerCase();
    const type = this.filterType();
    const priority = this.filterPriority();
    const readFilter = this.filterRead();

    if (search) {
      list = list.filter(n =>
        n.title.toLowerCase().includes(search) ||
        n.message.toLowerCase().includes(search)
      );
    }
    if (type) {
      list = list.filter(n => n.type === type);
    }
    if (priority) {
      list = list.filter(n => n.priority === priority);
    }
    if (readFilter === 'read') {
      list = list.filter(n => n.isRead);
    } else if (readFilter === 'unread') {
      list = list.filter(n => !n.isRead);
    }

    return list;
  });

  // Stats cards
  displayStats = computed(() => {
    const s = this.stats();
    const all = this.notifications();
    return [
      { label: 'Total', value: s?.total ?? all.length, icon: 'bell', bgColor: 'bg-indigo-50', iconColor: 'text-indigo-600' },
      { label: 'Unread', value: s?.unread ?? this.unreadCount(), icon: 'envelope', bgColor: 'bg-blue-50', iconColor: 'text-blue-600' },
      { label: 'High Priority', value: all.filter(n => n.priority === 'HIGH' || n.priority === 'URGENT').length, icon: 'exclamation-triangle', bgColor: 'bg-orange-50', iconColor: 'text-orange-600' },
      { label: 'Today', value: all.filter(n => this.isToday(n.createdAt)).length, icon: 'calendar-event', bgColor: 'bg-green-50', iconColor: 'text-green-600' }
    ];
  });

  allSelected = computed(() => {
    const filtered = this.filteredNotifications();
    const selected = this.selectedIds();
    return filtered.length > 0 && filtered.every(n => selected.has(n.id));
  });

  // Actions
  markAsRead(id: string) {
    this.notificationService.markAsRead(id).subscribe();
  }

  markAllAsRead() {
    this.notificationService.markAllAsRead().subscribe({
      next: (count) => this.alertSvc.success('Marked as read', `${count} notification(s) marked as read`),
      error: () => this.alertSvc.error('Failed to mark notifications as read')
    });
  }

  deleteNotification(id: string) {
    this.alertSvc.confirm({
      title: 'Delete Notification',
      message: 'Are you sure you want to delete this notification?',
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.notificationService.deleteNotification(id).subscribe({
        next: () => this.alertSvc.success('Deleted', 'Notification removed'),
        error: () => this.alertSvc.error('Failed to delete notification')
      });
    });
  }

  deleteSelected() {
    const ids = Array.from(this.selectedIds());
    if (ids.length === 0) return;

    this.alertSvc.confirm({
      title: 'Delete Notifications',
      message: `Delete ${ids.length} notification(s)? This cannot be undone.`,
      confirmLabel: `Delete ${ids.length}`,
      danger: true
    }).then(ok => {
      if (!ok) return;
      ids.forEach(id => this.notificationService.deleteNotification(id).subscribe());
      this.selectedIds.set(new Set());
      this.alertSvc.success('Deleted', `${ids.length} notification(s) removed`);
    });
  }

  toggleSelect(id: string) {
    this.selectedIds.update(set => {
      const next = new Set(set);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  toggleSelectAll() {
    if (this.allSelected()) {
      this.selectedIds.set(new Set());
    } else {
      this.selectedIds.set(new Set(this.filteredNotifications().map(n => n.id)));
    }
  }

  isSelected(id: string): boolean {
    return this.selectedIds().has(id);
  }

  setFilterType(type: NotificationType | '') {
    this.filterType.set(type);
  }

  setFilterPriority(priority: NotificationPriority | '') {
    this.filterPriority.set(priority);
  }

  setFilterRead(filter: 'all' | 'read' | 'unread') {
    this.filterRead.set(filter);
  }

  clearFilters() {
    this.filterType.set('');
    this.filterPriority.set('');
    this.filterRead.set('all');
    this.searchQuery.set('');
  }

  retry() {
    this.notificationService.retry();
  }

  getTypeOption(type: NotificationType) {
    return this.typeOptions.find(o => o.value === type) ?? this.typeOptions[2]; // default System
  }

  getPriorityOption(priority: NotificationPriority) {
    return this.priorityOptions.find(o => o.value === priority) ?? this.priorityOptions[1]; // default Normal
  }

  timeAgo(date: Date): string {
    const seconds = Math.floor((Date.now() - new Date(date).getTime()) / 1000);
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    if (seconds < 604800) return `${Math.floor(seconds / 86400)}d ago`;
    return new Date(date).toLocaleDateString();
  }

  private isToday(date: Date): boolean {
    const today = new Date();
    const d = new Date(date);
    return d.getFullYear() === today.getFullYear() &&
      d.getMonth() === today.getMonth() &&
      d.getDate() === today.getDate();
  }
}
