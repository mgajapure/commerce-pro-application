import { Component, signal, computed, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { OrderService } from '../../../core/services/order/order.service';
import { OrderSummaryResponse, OrderStatsResponse, OrderStatus } from '../../../core/models/order/order.model';
import { PageParams } from '../../../core/models/common';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, Dropdown],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss'
})
export class OrderList implements OnInit, OnDestroy {
  readonly Math = Math;
  private orderService = inject(OrderService);
  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  // ── View state ─────────────────────────────────────────────────────────────
  viewMode       = signal<'table' | 'grid'>('table');
  showFilters    = signal(false);
  selectedOrders = signal<string[]>([]);
  showConfirm    = signal(false);
  confirmAction  = signal<{ label: string; status: OrderStatus; ids: string[] } | null>(null);

  // ── Server-side data ───────────────────────────────────────────────────────
  orders        = signal<OrderSummaryResponse[]>([]);
  stats         = signal<OrderStatsResponse | null>(null);
  isLoading     = signal(false);
  error         = signal<string | null>(null);
  totalElements = signal(0);
  totalPages    = signal(0);

  // ── Pagination (0-based for API, 1-based for UI) ───────────────────────────
  currentPage  = signal(1);
  itemsPerPage = signal(20);

  // ── Filters ────────────────────────────────────────────────────────────────
  searchQuery   = signal('');
  filterStatus  = signal('');
  filterPayment = signal('');
  filterSource  = signal('');
  filterFlagged = signal('');
  filterFrom    = signal('');
  filterTo      = signal('');

  // ── Sort ───────────────────────────────────────────────────────────────────
  sortField     = signal('createdAt');
  sortDirection = signal<'asc' | 'desc'>('desc');

  exportItems: DropdownItem[] = [
    { id: 'csv',   label: 'Export as CSV',   icon: 'filetype-csv' },
    { id: 'excel', label: 'Export as Excel', icon: 'filetype-xlsx' },
    { id: 'pdf',   label: 'Export as PDF',   icon: 'filetype-pdf' }
  ];

  quickFilters = [
    { label: 'All',         value: '' },
    { label: 'Draft',       value: 'DRAFT' },
    { label: 'Pending',     value: 'PENDING_PAYMENT' },
    { label: 'Confirmed',   value: 'CONFIRMED' },
    { label: 'On Hold',     value: 'ON_HOLD' },
    { label: 'Processing',  value: 'PROCESSING' },
    { label: 'Shipped',     value: 'SHIPPED' },
    { label: 'Delivered',   value: 'DELIVERED' },
    { label: 'Cancelled',   value: 'CANCELLED' },
    { label: 'Refunded',    value: 'REFUNDED' },
    { label: 'Closed',      value: 'CLOSED' }
  ];

  activeFiltersCount = computed(() =>
    [this.filterStatus(), this.filterPayment(), this.filterSource(),
     this.filterFlagged(), this.filterFrom(), this.filterTo()].filter(Boolean).length
  );

  displayStats = computed(() => {
    const s = this.stats();
    if (!s) return [];
    return [
      { label: 'Total Orders',  value: s.totalOrders,       icon: 'bag-check',    bg: 'bg-blue-100',    ic: 'text-blue-600',    trend: 12.5 },
      { label: 'Revenue Today', value: '$' + s.revenueToday.toFixed(0), icon: 'cash-stack', bg: 'bg-emerald-100', ic: 'text-emerald-600', trend: 8.3 },
      { label: 'Pending',       value: s.pendingOrders,     icon: 'clock-history', bg: 'bg-yellow-100',  ic: 'text-yellow-600',  trend: -2.1 },
      { label: 'Processing',    value: s.processingOrders,  icon: 'gear',          bg: 'bg-indigo-100',  ic: 'text-indigo-600',  trend: 5.4 },
      { label: 'Shipped',       value: s.shippedOrders,     icon: 'truck',         bg: 'bg-purple-100',  ic: 'text-purple-600',  trend: 15.2 },
      { label: 'Flagged',       value: s.flaggedOrders,     icon: 'flag-fill',     bg: 'bg-red-100',     ic: 'text-red-600',     trend: -30.0 }
    ];
  });

  ngOnInit(): void {
    // Debounce search input
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.currentPage.set(1);
      this.loadOrders();
    });

    this.loadStats();
    this.loadOrders();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Data loading ───────────────────────────────────────────────────────────

  loadStats(): void {
    this.orderService.getStats().subscribe(s => this.stats.set(s));
  }

  loadOrders(): void {
    this.isLoading.set(true);
    this.error.set(null);

    const pageParams: PageParams = {
      page: this.currentPage() - 1,
      size: this.itemsPerPage(),
      sort: this.sortField(),
      direction: this.sortDirection()
    };

    const filter: any = {};
    if (this.searchQuery())   filter.search        = this.searchQuery();
    if (this.filterStatus())  filter.status        = this.filterStatus();
    if (this.filterPayment()) filter.paymentStatus = this.filterPayment();
    if (this.filterSource())  filter.source        = this.filterSource();
    if (this.filterFlagged() === 'true')  filter.isFlagged = true;
    if (this.filterFlagged() === 'false') filter.isFlagged = false;
    if (this.filterFrom())    filter.createdFrom   = this.filterFrom();
    if (this.filterTo())      filter.createdTo     = this.filterTo();

    this.orderService.getOrders(filter, pageParams)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: page => {
          this.orders.set(page.content);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.isLoading.set(false);
        },
        error: err => {
          this.error.set('Failed to load orders');
          this.isLoading.set(false);
        }
      });
  }

  // ── UI actions ─────────────────────────────────────────────────────────────

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    this.searchSubject.next(value);
  }

  applyFilter(): void {
    this.currentPage.set(1);
    this.loadOrders();
  }

  applyQuickFilter(status: string): void {
    this.filterStatus.set(status);
    this.filterFlagged.set('');
    this.currentPage.set(1);
    this.loadOrders();
  }

  clearAllFilters(): void {
    this.filterStatus.set(''); this.filterPayment.set(''); this.filterSource.set('');
    this.filterFlagged.set(''); this.filterFrom.set(''); this.filterTo.set('');
    this.searchQuery.set('');
    this.currentPage.set(1);
    this.loadOrders();
  }

  sort(field: string): void {
    if (this.sortField() === field) this.sortDirection.update(d => d === 'asc' ? 'desc' : 'asc');
    else { this.sortField.set(field); this.sortDirection.set('desc'); }
    this.loadOrders();
  }

  toggleViewMode(): void { this.viewMode.update(v => v === 'table' ? 'grid' : 'table'); }
  toggleFilters(): void  { this.showFilters.update(v => !v); }

  // ── Selection ──────────────────────────────────────────────────────────────

  toggleSelection(id: string): void {
    this.selectedOrders.update(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]);
  }
  isSelected(id: string): boolean { return this.selectedOrders().includes(id); }
  isAllSelected(): boolean {
    return this.orders().length > 0 && this.orders().every(o => this.isSelected(o.id));
  }
  toggleSelectAll(): void {
    if (this.isAllSelected()) this.selectedOrders.set([]);
    else this.selectedOrders.set(this.orders().map(o => o.id));
  }

  // ── Bulk actions ───────────────────────────────────────────────────────────

  openConfirm(label: string, status: OrderStatus): void {
    this.confirmAction.set({ label, status, ids: this.selectedOrders() });
    this.showConfirm.set(true);
  }

  executeBulk(): void {
    const a = this.confirmAction();
    if (!a) return;
    this.isLoading.set(true);
    this.orderService.bulkAction(a.ids, a.status).subscribe(() => {
      this.selectedOrders.set([]);
      this.showConfirm.set(false);
      this.loadStats();
      this.loadOrders();
    });
  }

  // ── Pagination ─────────────────────────────────────────────────────────────

  goToPage(p: number): void { this.currentPage.set(p); this.loadOrders(); }
  previousPage(): void { if (this.currentPage() > 1) { this.currentPage.update(p => p - 1); this.loadOrders(); } }
  nextPage(): void     { if (this.currentPage() < this.totalPages()) { this.currentPage.update(p => p + 1); this.loadOrders(); } }

  visiblePages(): (number | string)[] {
    const total = this.totalPages(), cur = this.currentPage();
    const pages: (number | string)[] = [];
    if (total <= 7) { for (let i = 1; i <= total; i++) pages.push(i); }
    else if (cur <= 3) pages.push(1, 2, 3, 4, '...', total);
    else if (cur >= total - 2) pages.push(1, '...', total-3, total-2, total-1, total);
    else pages.push(1, '...', cur-1, cur, cur+1, '...', total);
    return pages;
  }

  onExport(item: DropdownItem): void { console.log('Export', item.id); }

  getOrderMenuItems(order: OrderSummaryResponse): DropdownItem[] {
    return [
      { id: 'view',    label: 'View Details',  icon: 'eye' },
      { id: 'divider', label: '', divider: true },
      { id: 'cancel',  label: 'Cancel Order',  icon: 'x-circle', danger: true }
    ];
  }

  // ── Display helpers ─────────────────────────────────────────────────────────

  getStatusConfig(s: string): { badge: string; dot: string; label: string } {
    const m: Record<string, { badge: string; dot: string; label: string }> = {
      DRAFT:              { badge: 'bg-gray-100 text-gray-700',     dot: 'bg-gray-400',    label: 'Draft' },
      PENDING_PAYMENT:    { badge: 'bg-yellow-100 text-yellow-800', dot: 'bg-yellow-500',  label: 'Pending' },
      PAYMENT_FAILED:     { badge: 'bg-red-100 text-red-800',       dot: 'bg-red-500',     label: 'Pay Failed' },
      CONFIRMED:          { badge: 'bg-blue-100 text-blue-800',     dot: 'bg-blue-500',    label: 'Confirmed' },
      ON_HOLD:            { badge: 'bg-orange-100 text-orange-800', dot: 'bg-orange-500',  label: 'On Hold' },
      PROCESSING:         { badge: 'bg-indigo-100 text-indigo-800', dot: 'bg-indigo-500',  label: 'Processing' },
      PARTIALLY_FULFILLED:{ badge: 'bg-cyan-100 text-cyan-800',     dot: 'bg-cyan-500',    label: 'Partial' },
      FULFILLED:          { badge: 'bg-teal-100 text-teal-800',     dot: 'bg-teal-500',    label: 'Fulfilled' },
      SHIPPED:            { badge: 'bg-purple-100 text-purple-800', dot: 'bg-purple-500',  label: 'Shipped' },
      OUT_FOR_DELIVERY:   { badge: 'bg-violet-100 text-violet-800', dot: 'bg-violet-500',  label: 'Out for Delivery' },
      DELIVERED:          { badge: 'bg-green-100 text-green-800',   dot: 'bg-green-500',   label: 'Delivered' },
      CANCELLED:          { badge: 'bg-red-100 text-red-800',       dot: 'bg-red-500',     label: 'Cancelled' },
      RETURN_INITIATED:   { badge: 'bg-pink-100 text-pink-800',     dot: 'bg-pink-500',    label: 'Return Init.' },
      RETURN_IN_TRANSIT:  { badge: 'bg-rose-100 text-rose-800',     dot: 'bg-rose-500',    label: 'Return Transit' },
      RETURN_RECEIVED:    { badge: 'bg-fuchsia-100 text-fuchsia-800',dot: 'bg-fuchsia-500',label: 'Return Rcvd' },
      REFUNDED:           { badge: 'bg-gray-100 text-gray-800',     dot: 'bg-gray-500',    label: 'Refunded' },
      PARTIALLY_REFUNDED: { badge: 'bg-gray-100 text-gray-600',     dot: 'bg-gray-400',    label: 'Part. Refunded' },
      CLOSED:             { badge: 'bg-slate-100 text-slate-700',   dot: 'bg-slate-400',   label: 'Closed' }
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', dot: 'bg-gray-400', label: s };
  }

  getPaymentConfig(s: string): { color: string; icon: string } {
    const m: Record<string, { color: string; icon: string }> = {
      PENDING:            { color: 'text-yellow-600', icon: 'clock' },
      AUTHORIZED:         { color: 'text-blue-600',   icon: 'shield-check' },
      CAPTURED:           { color: 'text-green-600',  icon: 'check-circle' },
      FAILED:             { color: 'text-red-600',    icon: 'x-circle' },
      VOIDED:             { color: 'text-gray-500',   icon: 'slash-circle' },
      REFUNDED:           { color: 'text-gray-600',   icon: 'arrow-counterclockwise' },
      PARTIALLY_REFUNDED: { color: 'text-orange-600', icon: 'arrow-counterclockwise' },
      CHARGEBACK:         { color: 'text-red-700',    icon: 'exclamation-triangle' }
    };
    return m[s] ?? { color: 'text-gray-500', icon: 'dash' };
  }

  getSourceBadge(s: string): string {
    const m: Record<string, string> = {
      STOREFRONT: 'bg-blue-50 text-blue-700',
      MANUAL:     'bg-gray-100 text-gray-700',
      API:        'bg-indigo-50 text-indigo-700',
      IMPORT:     'bg-yellow-50 text-yellow-700',
      MOBILE_APP: 'bg-purple-50 text-purple-700'
    };
    return m[s] ?? 'bg-gray-100 text-gray-700';
  }

  formatDate(iso: string): string {
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  getTimeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const m = Math.floor(diff / 60000), h = Math.floor(diff / 3600000), d = Math.floor(diff / 86400000);
    if (m < 60) return `${m}m ago`;
    if (h < 24) return `${h}h ago`;
    return `${d}d ago`;
  }
}
