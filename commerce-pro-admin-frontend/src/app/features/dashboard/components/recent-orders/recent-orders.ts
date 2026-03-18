import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../../../core/services/order/order.service';

interface RecentOrder{
  id: number;
  customerAvatar: string;
  customerName: string;
  items: [];
  total: number;
  status: string;
  date: Date;
}

@Component({
  selector: 'app-recent-orders',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recent-orders.html',
  styleUrl: './recent-orders.scss'
})
export class RecentOrders {
  private orderService = inject(OrderService);
  
  recentOrders = signal<RecentOrder[]>([]);
  isLoading = this.orderService.isLoading;
  
  // Order counts by status
  orderStats = this.orderService.orderStats;
  
  // Get status color class
  getStatusColor(status: string) {
    const colors: any = {
      pending: 'bg-yellow-100 text-yellow-800',
      processing: 'bg-blue-100 text-blue-800',
      shipped: 'bg-purple-100 text-purple-800',
      delivered: 'bg-green-100 text-green-800',
      cancelled: 'bg-red-100 text-red-800',
      returned: 'bg-gray-100 text-gray-800'
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
  }
  
  // Format relative time
  getRelativeTime(date: Date): string {
    const now = new Date();
    const diff = now.getTime() - new Date(date).getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes} min ago`;
    if (hours < 24) return `${hours} hours ago`;
    return `${days} days ago`;
  }
}
