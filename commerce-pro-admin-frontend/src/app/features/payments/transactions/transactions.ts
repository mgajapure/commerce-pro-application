import { Component, signal, computed, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { PaymentService } from '../../../core/services/payment/payment.service';
import { PaymentTransactionSummaryDTO, PaymentStatsDTO, TransactionStatus, TransactionType, GatewayProvider } from '../../../core/models/payment/payment.model';
import { PageParams } from '../../../core/models/common';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transactions.html',
  styleUrl: './transactions.scss'
})
export class Transactions implements OnInit, OnDestroy {
  readonly Math = Math;
  private svc = inject(PaymentService);
  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  transactions  = signal<PaymentTransactionSummaryDTO[]>([]);
  stats         = signal<PaymentStatsDTO | null>(null);
  isLoading     = signal(false);
  error         = signal<string | null>(null);
  totalElements = signal(0);
  totalPages    = signal(0);
  currentPage   = signal(1);
  itemsPerPage  = signal(20);
  showFilters   = signal(false);
  toggleFilters(): void { this.showFilters.update(v => !v); }
  selected      = signal<string[]>([]);
  detailTxn     = signal<PaymentTransactionSummaryDTO | null>(null);
  showFlagModal = signal(false);
  flagTarget    = signal<{ id: string; flagged: boolean } | null>(null);
  flagReason    = signal('');
  actionLoading = signal<string | null>(null);

  searchQuery   = signal('');
  filterStatus  = signal('');
  filterType    = signal('');
  filterGateway = signal('');
  filterFlagged = signal('');
  filterFrom    = signal('');
  filterTo      = signal('');
  sortField     = signal('createdAt');
  sortDir       = signal<'asc' | 'desc'>('desc');

  quickTabs = [
    { label: 'All', value: '' },
    { label: 'Charges', value: 'CHARGE' },
    { label: 'Authorized', value: 'AUTHORIZE' },
    { label: 'Refunds', value: 'REFUND' },
    { label: 'Failed', value: 'FAILED' },
    { label: 'Flagged', value: 'FLAGGED' },
  ];
  activeTab = signal('');

  activeFiltersCount = computed(() =>
    [this.filterStatus(), this.filterType(), this.filterGateway(), this.filterFlagged(), this.filterFrom(), this.filterTo()].filter(Boolean).length
  );

  displayStats = computed(() => {
    const s = this.stats();
    if (!s) return [];
    return [
      { label: 'Total Revenue',    value: this.fmt(s.totalRevenue),         icon: 'cash-stack',         bg: 'bg-emerald-100', ic: 'text-emerald-600' },
      { label: 'Revenue Today',    value: this.fmt(s.revenueToday),         icon: 'graph-up-arrow',     bg: 'bg-blue-100',    ic: 'text-blue-600' },
      { label: 'Pending Capture',  value: this.fmt(s.pendingCaptureAmount), icon: 'hourglass-split',    bg: 'bg-amber-100',   ic: 'text-amber-600' },
      { label: 'Pending Refunds',  value: String(s.pendingRefunds),         icon: 'arrow-counterclockwise', bg: 'bg-orange-100', ic: 'text-orange-600' },
      { label: 'Active Disputes',  value: String(s.activeDisputes),         icon: 'shield-exclamation', bg: 'bg-red-100',     ic: 'text-red-600' },
      { label: 'Total Txns',       value: String(s.totalTransactions),      icon: 'credit-card',        bg: 'bg-indigo-100',  ic: 'text-indigo-600' },
    ];
  });

  ngOnInit(): void {
    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => { this.currentPage.set(1); this.loadTransactions(); });
    this.loadStats();
    this.loadTransactions();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  loadStats(): void {
    this.svc.getPaymentStats().subscribe(s => this.stats.set(s));
  }

  loadTransactions(): void {
    this.isLoading.set(true);
    this.error.set(null);
    const tab = this.activeTab();
    const filter: any = {
      search: this.searchQuery() || undefined,
      status: this.filterStatus() || undefined,
      type: this.filterType() || (tab && tab !== 'FAILED' && tab !== 'FLAGGED' ? tab as TransactionType : undefined),
      gateway: this.filterGateway() as GatewayProvider || undefined,
      isFlagged: tab === 'FLAGGED' ? true : (this.filterFlagged() === 'true' ? true : this.filterFlagged() === 'false' ? false : undefined),
      from: this.filterFrom() || undefined,
      to: this.filterTo() || undefined,
    };
    if (tab === 'FAILED') filter.status = 'FAILED';

    const pageParams: PageParams = {
      page: this.currentPage() - 1,
      size: this.itemsPerPage(),
      sort: this.sortField(),
      direction: this.sortDir()
    };
    this.svc.getTransactions(filter, pageParams).subscribe(p => {
      this.transactions.set(p.content);
      this.totalElements.set(p.totalElements);
      this.totalPages.set(p.totalPages);
      this.isLoading.set(false);
    });
  }

  onSearchInput(v: string): void { this.searchQuery.set(v); this.searchSubject.next(v); }
  applyFilter(): void { this.currentPage.set(1); this.loadTransactions(); }
  setTab(t: string): void { this.activeTab.set(t); this.filterStatus.set(''); this.filterType.set(''); this.currentPage.set(1); this.loadTransactions(); }
  clearFilters(): void { this.searchQuery.set(''); this.filterStatus.set(''); this.filterType.set(''); this.filterGateway.set(''); this.filterFlagged.set(''); this.filterFrom.set(''); this.filterTo.set(''); this.activeTab.set(''); this.currentPage.set(1); this.loadTransactions(); }

  sort(field: string): void {
    if (this.sortField() === field) this.sortDir.update(d => d === 'asc' ? 'desc' : 'asc');
    else { this.sortField.set(field); this.sortDir.set('desc'); }
    this.loadTransactions();
  }

  capture(txn: PaymentTransactionSummaryDTO, e: Event): void {
    e.stopPropagation();
    this.actionLoading.set(txn.id);
    this.svc.captureTransaction(txn.id).subscribe(() => { this.actionLoading.set(null); this.loadTransactions(); this.loadStats(); });
  }

  voidTxn(txn: PaymentTransactionSummaryDTO, e: Event): void {
    e.stopPropagation();
    this.actionLoading.set(txn.id);
    this.svc.voidTransaction(txn.id).subscribe(() => { this.actionLoading.set(null); this.loadTransactions(); this.loadStats(); });
  }

  openFlagModal(txn: PaymentTransactionSummaryDTO, e: Event): void {
    e.stopPropagation();
    this.flagTarget.set({ id: txn.id, flagged: !txn.isFlagged });
    this.flagReason.set('');
    this.showFlagModal.set(true);
  }

  submitFlag(): void {
    const t = this.flagTarget();
    if (!t) return;
    this.actionLoading.set(t.id);
    this.svc.flagTransaction(t.id, t.flagged, this.flagReason() || undefined).subscribe(() => {
      this.actionLoading.set(null);
      this.showFlagModal.set(false);
      this.loadTransactions();
    });
  }

  isSelected = (id: string) => this.selected().includes(id);
  isAllSelected = computed(() => this.transactions().length > 0 && this.transactions().every(t => this.isSelected(t.id)));
  toggleSelect(id: string): void { this.selected.update(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]); }
  toggleAll(): void { this.isAllSelected() ? this.selected.set([]) : this.selected.set(this.transactions().map(t => t.id)); }

  goToPage(p: number): void { this.currentPage.set(p); this.loadTransactions(); }
  prevPage(): void { if (this.currentPage() > 1) { this.currentPage.update(p => p - 1); this.loadTransactions(); } }
  nextPage(): void { if (this.currentPage() < this.totalPages()) { this.currentPage.update(p => p + 1); this.loadTransactions(); } }
  visiblePages(): (number | string)[] {
    const total = this.totalPages(), cur = this.currentPage(), pages: (number | string)[] = [];
    if (total <= 7) { for (let i = 1; i <= total; i++) pages.push(i); }
    else if (cur <= 3) pages.push(1, 2, 3, 4, '...', total);
    else if (cur >= total - 2) pages.push(1, '...', total - 3, total - 2, total - 1, total);
    else pages.push(1, '...', cur - 1, cur, cur + 1, '...', total);
    return pages;
  }

  getStatusConfig(s: string): { badge: string; label: string; dot: string } {
    const m: Record<string, { badge: string; label: string; dot: string }> = {
      PENDING:            { badge: 'bg-yellow-100 text-yellow-800', dot: 'bg-yellow-500',  label: 'Pending' },
      AUTHORIZED:         { badge: 'bg-blue-100 text-blue-800',     dot: 'bg-blue-500',    label: 'Authorized' },
      CAPTURED:           { badge: 'bg-green-100 text-green-800',   dot: 'bg-green-500',   label: 'Captured' },
      SETTLED:            { badge: 'bg-emerald-100 text-emerald-800',dot: 'bg-emerald-500', label: 'Settled' },
      FAILED:             { badge: 'bg-red-100 text-red-800',       dot: 'bg-red-500',     label: 'Failed' },
      VOIDED:             { badge: 'bg-gray-100 text-gray-700',     dot: 'bg-gray-400',    label: 'Voided' },
      REFUNDED:           { badge: 'bg-purple-100 text-purple-800', dot: 'bg-purple-500',  label: 'Refunded' },
      PARTIALLY_REFUNDED: { badge: 'bg-orange-100 text-orange-800', dot: 'bg-orange-500',  label: 'Part. Refunded' },
      CHARGEBACK:         { badge: 'bg-red-200 text-red-900',       dot: 'bg-red-700',     label: 'Chargeback' },
      CANCELLED:          { badge: 'bg-gray-100 text-gray-600',     dot: 'bg-gray-400',    label: 'Cancelled' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', dot: 'bg-gray-400', label: s };
  }

  getTypeConfig(t: string): { badge: string; icon: string } {
    const m: Record<string, { badge: string; icon: string }> = {
      CHARGE:     { badge: 'bg-blue-50 text-blue-700',    icon: 'credit-card' },
      AUTHORIZE:  { badge: 'bg-indigo-50 text-indigo-700',icon: 'shield-check' },
      CAPTURE:    { badge: 'bg-green-50 text-green-700',  icon: 'check-circle' },
      VOID:       { badge: 'bg-gray-100 text-gray-600',   icon: 'slash-circle' },
      REFUND:     { badge: 'bg-purple-50 text-purple-700',icon: 'arrow-counterclockwise' },
      CHARGEBACK: { badge: 'bg-red-50 text-red-700',      icon: 'exclamation-triangle' },
      ADJUSTMENT: { badge: 'bg-amber-50 text-amber-700',  icon: 'sliders' },
    };
    return m[t] ?? { badge: 'bg-gray-100 text-gray-600', icon: 'dash' };
  }

  getGatewayIcon(g: string): string {
    const m: Record<string, string> = { STRIPE: 'stripe', PAYPAL: 'paypal', MANUAL: 'cash-coin', SQUARE: 'square', RAZORPAY: 'bank' };
    return m[g] ?? 'credit-card';
  }

  getCardBrandColor(brand: string): string {
    return brand === 'VISA' ? 'text-blue-700' : brand === 'MASTERCARD' ? 'text-red-600' : brand === 'AMEX' ? 'text-green-700' : 'text-gray-600';
  }

  fmt(n: number): string {
    return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  timeAgo(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const m = Math.floor(diff / 60000), h = Math.floor(diff / 3600000), d = Math.floor(diff / 86400000);
    if (m < 60) return `${m}m ago`; if (h < 24) return `${h}h ago`; return `${d}d ago`;
  }

  fmtDate(iso: string): string {
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }
}
