import { Component, signal, computed, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { CustomerApiService } from '../../../core/services/customers/customers.service';
import { AlertService } from '../../../shared/services/alert.service';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { CustomerSummary, CustomerStats, CustomerFilter } from '../../../core/models/customers/customers.model';
import { PageParams } from '../../../core/models/common';

@Component({
  selector: 'app-customer-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, Dropdown],
  templateUrl: './customer-list.html',
  styleUrl: './customer-list.scss'
})
export class CustomerList implements OnInit, OnDestroy {
  readonly Math = Math;

  private customerSvc = inject(CustomerApiService);
  private alertSvc    = inject(AlertService);
  private destroy$    = new Subject<void>();
  private searchSubject = new Subject<string>();

  // ── View state ──────────────────────────────────────────────────────────
  showFilters      = signal(false);
  selectedCustomers = signal<string[]>([]);

  // ── Server data ─────────────────────────────────────────────────────────
  customers     = signal<CustomerSummary[]>([]);
  stats         = signal<CustomerStats | null>(null);
  isLoading     = signal(false);
  error         = signal<string | null>(null);
  totalElements = signal(0);
  totalPages    = signal(0);

  // ── Pagination ───────────────────────────────────────────────────────────
  currentPage  = signal(1);
  itemsPerPage = signal(20);

  // ── Filters ──────────────────────────────────────────────────────────────
  searchQuery      = signal('');
  filterStatus     = signal('');
  filterTier       = signal('');
  filterBlacklisted = signal('');
  filterFraud      = signal('');
  filterGroupId    = signal('');

  // ── Sort ─────────────────────────────────────────────────────────────────
  sortField     = signal('createdAt');
  sortDirection = signal<'asc' | 'desc'>('desc');

  quickFilters = [
    { label: 'All',         value: '' },
    { label: 'Active',      value: 'ACTIVE' },
    { label: 'Inactive',    value: 'INACTIVE' },
    { label: 'Suspended',   value: 'SUSPENDED' },
    { label: 'Blacklisted', value: 'BLACKLISTED' },
  ];

  tierFilters = [
    { label: 'All Tiers',  value: '' },
    { label: 'Regular',    value: 'REGULAR' },
    { label: 'Silver',     value: 'SILVER' },
    { label: 'Gold',       value: 'GOLD' },
    { label: 'Platinum',   value: 'PLATINUM' },
  ];

  exportItems: DropdownItem[] = [
    { id: 'csv',   label: 'Export CSV',   icon: 'filetype-csv' },
    { id: 'excel', label: 'Export Excel', icon: 'filetype-xlsx' }
  ];

  activeFiltersCount = computed(() =>
    [this.filterStatus(), this.filterTier(), this.filterBlacklisted(),
     this.filterFraud(), this.filterGroupId()].filter(Boolean).length
  );

  displayStats = computed(() => {
    const s = this.stats();
    if (!s) return [];
    return [
      { label: 'Total Customers',  value: s.totalCustomers,        icon: 'people-fill',      bg: 'bg-blue-100',    ic: 'text-blue-600' },
      { label: 'Active',           value: s.activeCustomers,       icon: 'person-check-fill', bg: 'bg-green-100',   ic: 'text-green-600' },
      { label: 'New This Month',   value: s.newCustomersThisMonth, icon: 'person-plus-fill',  bg: 'bg-indigo-100',  ic: 'text-indigo-600' },
      { label: 'Avg LTV',          value: '$' + (s.averageLifetimeValue ?? 0).toFixed(0), icon: 'cash-stack', bg: 'bg-emerald-100', ic: 'text-emerald-600' },
      { label: 'Blacklisted',      value: s.blacklistedCustomers,  icon: 'person-fill-slash', bg: 'bg-red-100',     ic: 'text-red-600' },
      { label: 'Fraud Flagged',    value: s.fraudFlaggedCustomers, icon: 'flag-fill',         bg: 'bg-orange-100',  ic: 'text-orange-600' },
    ];
  });

  ngOnInit(): void {
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => { this.currentPage.set(1); this.loadCustomers(); });

    this.loadStats();
    this.loadCustomers();
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  loadStats(): void {
    this.customerSvc.getStats().subscribe(s => this.stats.set(s));
  }

  loadCustomers(): void {
    this.isLoading.set(true);
    this.error.set(null);

    const pageParams: PageParams = {
      page: this.currentPage() - 1,
      size: this.itemsPerPage(),
      sort: this.sortField(),
      direction: this.sortDirection()
    };

    const filter: CustomerFilter = {};
    if (this.searchQuery())          filter.search          = this.searchQuery();
    if (this.filterStatus())         filter.status          = this.filterStatus();
    if (this.filterTier())           filter.tier            = this.filterTier();
    if (this.filterGroupId())        filter.groupId         = this.filterGroupId();
    if (this.filterBlacklisted() === 'true')  filter.isBlacklisted   = true;
    if (this.filterBlacklisted() === 'false') filter.isBlacklisted   = false;
    if (this.filterFraud() === 'true')        filter.isFraudFlagged  = true;
    if (this.filterFraud() === 'false')       filter.isFraudFlagged  = false;

    this.customerSvc.getCustomers(filter, pageParams)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: page => {
          this.customers.set(page.content);
          this.totalElements.set(page.totalElements);
          this.totalPages.set(page.totalPages);
          this.isLoading.set(false);
        },
        error: () => {
          this.error.set('Failed to load customers');
          this.isLoading.set(false);
        }
      });
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    this.searchSubject.next(value);
  }

  applyQuickFilter(status: string): void {
    this.filterStatus.set(status);
    this.filterBlacklisted.set('');
    this.currentPage.set(1);
    this.loadCustomers();
  }

  applyFilter(): void { this.currentPage.set(1); this.loadCustomers(); }

  clearAllFilters(): void {
    this.filterStatus.set(''); this.filterTier.set('');
    this.filterBlacklisted.set(''); this.filterFraud.set('');
    this.filterGroupId.set(''); this.searchQuery.set('');
    this.currentPage.set(1);
    this.loadCustomers();
  }

  sort(field: string): void {
    if (this.sortField() === field) this.sortDirection.update(d => d === 'asc' ? 'desc' : 'asc');
    else { this.sortField.set(field); this.sortDirection.set('desc'); }
    this.loadCustomers();
  }

  toggleFilters(): void { this.showFilters.update(v => !v); }

  // ── Selection ───────────────────────────────────────────────────────────
  toggleSelection(id: string): void {
    this.selectedCustomers.update(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]);
  }
  isSelected(id: string): boolean { return this.selectedCustomers().includes(id); }
  isAllSelected(): boolean {
    return this.customers().length > 0 && this.customers().every(c => this.isSelected(c.id));
  }
  toggleSelectAll(): void {
    if (this.isAllSelected()) this.selectedCustomers.set([]);
    else this.selectedCustomers.set(this.customers().map(c => c.id));
  }
  clearSelection(): void { this.selectedCustomers.set([]); }

  // ── Delete ──────────────────────────────────────────────────────────────
  deleteCustomer(c: CustomerSummary): void {
    this.alertSvc.confirm({
      title: 'Delete Customer',
      message: `Are you sure you want to delete "${c.fullName}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.customerSvc.deleteCustomer(c.id).subscribe({
        next: () => {
          this.alertSvc.success('Customer deleted', `"${c.fullName}" has been removed.`);
          this.loadStats();
          this.loadCustomers();
        },
        error: () => this.alertSvc.error('Failed to delete customer')
      });
    });
  }

  // ── Pagination ──────────────────────────────────────────────────────────
  goToPage(p: number): void { this.currentPage.set(p); this.loadCustomers(); }
  previousPage(): void { if (this.currentPage() > 1) { this.currentPage.update(p => p - 1); this.loadCustomers(); } }
  nextPage(): void { if (this.currentPage() < this.totalPages()) { this.currentPage.update(p => p + 1); this.loadCustomers(); } }

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

  getRowMenuItems(c: CustomerSummary): DropdownItem[] {
    return [
      { id: 'view',   label: 'View Details', icon: 'eye' },
      { id: 'edit',   label: 'Edit',         icon: 'pencil' },
      { id: 'divider1', label: '', divider: true },
      { id: 'delete', label: 'Delete',       icon: 'trash', danger: true }
    ];
  }

  onRowAction(item: DropdownItem, c: CustomerSummary, router: any): void {
    if (item.id === 'delete') this.deleteCustomer(c);
  }

  // ── Display helpers ─────────────────────────────────────────────────────
  getStatusConfig(s: string): { badge: string; dot: string; label: string } {
    const m: Record<string, any> = {
      ACTIVE:               { badge: 'bg-green-100 text-green-800',   dot: 'bg-green-500',   label: 'Active' },
      INACTIVE:             { badge: 'bg-gray-100 text-gray-700',     dot: 'bg-gray-400',    label: 'Inactive' },
      SUSPENDED:            { badge: 'bg-orange-100 text-orange-800', dot: 'bg-orange-500',  label: 'Suspended' },
      PENDING_VERIFICATION: { badge: 'bg-yellow-100 text-yellow-800', dot: 'bg-yellow-500',  label: 'Pending' },
      BLACKLISTED:          { badge: 'bg-red-100 text-red-800',       dot: 'bg-red-500',     label: 'Blacklisted' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', dot: 'bg-gray-400', label: s };
  }

  getTierConfig(t: string): { badge: string; icon: string; label: string } {
    const m: Record<string, any> = {
      REGULAR:  { badge: 'bg-gray-100 text-gray-700',     icon: 'person',         label: 'Regular' },
      SILVER:   { badge: 'bg-slate-100 text-slate-700',   icon: 'star',           label: 'Silver' },
      GOLD:     { badge: 'bg-yellow-100 text-yellow-800', icon: 'star-fill',      label: 'Gold' },
      PLATINUM: { badge: 'bg-indigo-100 text-indigo-800', icon: 'gem',            label: 'Platinum' },
    };
    return m[t] ?? { badge: 'bg-gray-100 text-gray-700', icon: 'person', label: t };
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso));
  }

  formatCurrency(val: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 0 }).format(val ?? 0);
  }
}
