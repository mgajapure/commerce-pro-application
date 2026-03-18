import { Component, signal, computed, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { OrderService } from '../../../core/services/order/order.service';
import { OrderSummaryResponse, OrderStatus, OrderStatsResponse } from '../../../core/models/order/order.model';

@Component({
  selector: 'app-order-bulk',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, Dropdown],
  templateUrl: './order-bulk.html',
  styleUrl: './order-bulk.scss'
})
export class OrderBulk implements OnInit, OnDestroy {
  private orderService = inject(OrderService);
  private destroy$ = new Subject<void>();

  orders = signal<OrderSummaryResponse[]>([]);
  stats = signal<OrderStatsResponse | null>(null);
  isLoading = signal(false);
  isBulkLoading = signal(false);
  error = signal<string | null>(null);
  totalElements = signal(0);
  totalPages = signal(0);
  selectedOrders = signal<string[]>([]);
  targetStatus = signal<OrderStatus | ''>('');
  bulkReason = signal('');
  filterStatus = signal('');
  searchQuery = signal('');
  currentPage = signal(1);
  showConfirm = signal(false);
  resultMessage = signal('');
  resultType = signal<'success' | 'error'>('success');

  availableActions: { value: OrderStatus; label: string; color: string; icon: string; desc: string }[] = [
    { value: 'CONFIRMED', label: 'Confirm', color: 'bg-blue-600', icon: 'check-circle', desc: 'Confirm payment and reserve inventory' },
    { value: 'PROCESSING', label: 'Mark Processing', color: 'bg-indigo-600', icon: 'gear', desc: 'Move to active fulfilment queue' },
    { value: 'DELIVERED', label: 'Mark Delivered', color: 'bg-green-600', icon: 'house-check', desc: 'Confirm delivery for shipped orders' },
    { value: 'ON_HOLD', label: 'Place on Hold', color: 'bg-orange-600', icon: 'pause-circle', desc: 'Pause processing for selected orders' },
    { value: 'CLOSED', label: 'Close', color: 'bg-slate-600', icon: 'lock', desc: 'Close settled orders — no further action' },
    { value: 'CANCELLED', label: 'Cancel', color: 'bg-red-600', icon: 'x-circle', desc: 'Cancel and release inventory reservations' }
  ];

  ngOnInit(): void { this.loadStats(); this.loadOrders(); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  loadStats(): void {
    this.orderService.getStats().subscribe(s => this.stats.set(s));
  }

  loadOrders(): void {
    this.isLoading.set(true);
    this.error.set(null);
    const filter: any = {};
    if (this.filterStatus()) filter.status = this.filterStatus();
    if (this.searchQuery()) filter.search = this.searchQuery();

    this.orderService.getOrders(filter, {
      page: this.currentPage() - 1, size: 50, sort: 'createdAt', direction: 'desc'
    })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: page => {
          this.orders.set(page.content);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.isLoading.set(false);
        },
        error: () => {
          this.error.set('Failed to load orders');
          this.isLoading.set(false);
        }
      });
  }

  toggleSelection(id: string): void {
    this.selectedOrders.update(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]);
  }
  isSelected(id: string): boolean { return this.selectedOrders().includes(id); }
  isAllSelected(): boolean { return this.orders().length > 0 && this.orders().every(o => this.isSelected(o.id)); }
  toggleSelectAll(): void {
    if (this.isAllSelected()) this.selectedOrders.set([]);
    else this.selectedOrders.set(this.orders().map(o => o.id));
  }

  openConfirm(): void {
    if (!this.targetStatus() || this.selectedOrders().length === 0) return;
    this.showConfirm.set(true);
  }

  /** Calls POST /api/v1/orders/bulk-action */
  executeBulkAction(): void {
    const status = this.targetStatus() as OrderStatus;
    if (!status) return;
    this.isBulkLoading.set(true);

    this.orderService.bulkAction(this.selectedOrders(), status, this.bulkReason() || undefined)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: failed => {
          const done = this.selectedOrders().length - (failed?.length ?? 0);
          this.resultType.set(failed?.length ? 'error' : 'success');
          this.resultMessage.set(
            failed?.length
              ? `${done} succeeded, ${failed.length} failed`
              : `Applied ${status} to ${done} orders successfully`
          );
          this.selectedOrders.set([]);
          this.targetStatus.set('');
          this.bulkReason.set('');
          this.isBulkLoading.set(false);
          this.showConfirm.set(false);
          this.loadStats();
          this.loadOrders();
          setTimeout(() => this.resultMessage.set(''), 4000);
        },
        error: () => {
          this.resultType.set('error');
          this.resultMessage.set('Bulk action failed. Check the backend logs.');
          this.isBulkLoading.set(false);
          this.showConfirm.set(false);
        }
      });
  }

  getActionConfig(status: string) {
    return this.availableActions.find(a => a.value === status);
  }

  getStatusConfig(s: string): { badge: string; label: string } {
    return {
      badge: this.getStatusBadge(s),
      label: this.getStatusLabel(s)
    };
  }

  getStatusBadge(s: string): string {
    const m: Record<string, string> = {
      DRAFT: 'bg-gray-100 text-gray-700', PENDING_PAYMENT: 'bg-yellow-100 text-yellow-800',
      CONFIRMED: 'bg-blue-100 text-blue-800', ON_HOLD: 'bg-orange-100 text-orange-800',
      PROCESSING: 'bg-indigo-100 text-indigo-800', SHIPPED: 'bg-purple-100 text-purple-800',
      DELIVERED: 'bg-green-100 text-green-800', CANCELLED: 'bg-red-100 text-red-800',
      REFUNDED: 'bg-gray-100 text-gray-800', CLOSED: 'bg-slate-100 text-slate-700'
    };
    return m[s] ?? 'bg-gray-100 text-gray-700';
  }

  getStatusLabel(s: string): string {
    return s.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  }

  displayStats = computed(() => {
    const s = this.stats();
    if (!s) return [];
    return [
      { label: 'Total', value: s.totalOrders, bg: 'bg-blue-100', ic: 'text-blue-600', icon: 'bag-check' },
      { label: 'Pending', value: s.pendingOrders, bg: 'bg-yellow-100', ic: 'text-yellow-600', icon: 'clock' },
      { label: 'Processing', value: s.processingOrders, bg: 'bg-indigo-100', ic: 'text-indigo-600', icon: 'gear' },
      { label: 'Flagged', value: s.flaggedOrders, bg: 'bg-red-100', ic: 'text-red-600', icon: 'flag-fill' }
    ];
  });

  formatDate(iso: string): string {
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' }).format(new Date(iso));
  }
}
