import { Component, signal, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { OrderService } from '../../../core/services/order/order.service';
import { OrderSummaryResponse, OrderStatus } from '../../../core/models/order/order.model';

@Component({
  selector: 'app-order-processing',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, Dropdown],
  templateUrl: './order-processing.html',
  styleUrl: './order-processing.scss'
})
export class OrderProcessing implements OnInit, OnDestroy {
  private orderService = inject(OrderService);
  private destroy$ = new Subject<void>();

  orders        = signal<OrderSummaryResponse[]>([]);
  isLoading     = signal(false);
  error         = signal<string | null>(null);
  totalElements = signal(0);
  totalPages    = signal(0);
  searchQuery   = signal('');
  currentPage   = signal(1);
  itemsPerPage  = signal(20);
  actionLoading = signal<string | null>(null);

  // All statuses this page handles
  private readonly targetStatuses: OrderStatus[] = ['CONFIRMED', 'PROCESSING', 'ON_HOLD'];

  ngOnInit(): void { this.load(); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  load(): void {
    this.isLoading.set(true);
    this.error.set(null);

    // When only one status: pass it as a filter param for backend-side filtering.
    // When multiple: load without status filter, then filter client-side.
    // (The backend supports a single status param; multiple-status OR queries
    //  would need a custom backend endpoint — client-side is the pragmatic fallback.)
    const filter: any = {};
    if (this.targetStatuses.length === 1) {
      filter.status = this.targetStatuses[0];
    }
    if (this.searchQuery()) filter.search = this.searchQuery();

    this.orderService
      .getOrders(filter, {
        page: this.currentPage() - 1,
        size: this.itemsPerPage(),
        sort: 'createdAt',
        direction: 'desc'
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: page => {
          const items = this.targetStatuses.length > 1
            ? page.content.filter(o => this.targetStatuses.includes(o.status))
            : page.content;
          this.orders.set(items);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.isLoading.set(false);
        },
        error: () => {
          this.error.set('Failed to load orders. Make sure the backend is running.');
          this.isLoading.set(false);
        }
      });
  }

  onSearch(value: string): void {
    this.searchQuery.set(value);
    this.currentPage.set(1);
    this.load();
  }

  goToPage(p: number): void { this.currentPage.set(p); this.load(); }
  previousPage(): void { if (this.currentPage() > 1) { this.currentPage.update(p => p-1); this.load(); } }
  nextPage(): void { if (this.currentPage() < this.totalPages()) { this.currentPage.update(p => p+1); this.load(); } }

  getMenuItems(): DropdownItem[] {
    return [
      { id: 'view',  label: 'View Details',  icon: 'eye' },
      { id: 'email', label: 'Email Customer', icon: 'envelope' }
    ];
  }

  formatDate(iso: string): string {
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  getTimeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const h = Math.floor(diff / 3600000), d = Math.floor(diff / 86400000);
    if (h < 1) return 'Just now';
    if (h < 24) return `${h}h ago`;
    return `${d}d ago`;
  }

  getStatusBadge(s: string): string {
    const m: Record<string, string> = {
      DRAFT: 'bg-gray-100 text-gray-700',
      PENDING_PAYMENT: 'bg-yellow-100 text-yellow-800',
      CONFIRMED: 'bg-blue-100 text-blue-800',
      ON_HOLD: 'bg-orange-100 text-orange-800',
      PROCESSING: 'bg-indigo-100 text-indigo-800',
      SHIPPED: 'bg-purple-100 text-purple-800',
      OUT_FOR_DELIVERY: 'bg-violet-100 text-violet-800',
      DELIVERED: 'bg-green-100 text-green-800',
      CLOSED: 'bg-slate-100 text-slate-700',
      CANCELLED: 'bg-red-100 text-red-800',
      RETURN_INITIATED: 'bg-pink-100 text-pink-800',
      RETURN_IN_TRANSIT: 'bg-rose-100 text-rose-800',
      RETURN_RECEIVED: 'bg-fuchsia-100 text-fuchsia-800',
      REFUNDED: 'bg-gray-100 text-gray-800',
      PARTIALLY_REFUNDED: 'bg-gray-100 text-gray-600',
    };
    return m[s] ?? 'bg-gray-100 text-gray-700';
  }

  getStatusLabel(s: string): string {
    return s.replace(/_/g, ' ').replace(/\w/g, c => c.toUpperCase());
  }
}
