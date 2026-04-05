import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SystemService, ApiKey, ApiUsageStats } from '../system.service';
import { AlertService } from '../../../shared/services/alert.service';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-api-management',
  standalone: true,
  imports: [CommonModule, FormsModule, Dropdown, HelpSidebar, TooltipLabel],
  templateUrl: './api-management.html',
  styles: [`:host { display: block; }`]
})
export class ApiManagement implements OnInit, OnDestroy {
  private svc = inject(SystemService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  apiKeys = signal<ApiKey[]>([]);
  usageStats = signal<ApiUsageStats>({ totalRequests: 0, requestsToday: 0, errorRate: 0, activeKeys: 0 });
  isLoading = signal(true);

  // Filters
  searchQuery = signal('');
  filterStatus = signal('');

  // Selection
  selectedIds = signal<string[]>([]);

  // Create modal
  showCreateModal = signal(false);
  formName = signal('');
  formScopes = signal<Record<string, boolean>>({
    read_products: false, write_products: false,
    read_orders: false, write_orders: false,
    read_customers: false, manage_settings: false
  });
  isCreating = signal(false);
  newlyCreatedKey = signal<string | null>(null);
  copiedKey = signal(false);

  // Detail sidebar
  showDetail = signal(false);
  detailItem = signal<ApiKey | null>(null);

  // Help
  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'What are API Keys?', content: 'API keys authenticate external applications to access your commerce platform APIs securely.' },
    { title: 'Scopes', content: 'Scopes control what an API key can access. Only grant the minimum permissions needed.' },
    { title: 'Security', content: 'API keys are shown only once upon creation. Store them securely. Revoke compromised keys immediately.' },
    { title: 'Rate Limits', content: 'Monitor usage stats to ensure API keys are within rate limits. High error rates may indicate issues.' }
  ];

  allScopes = [
    { key: 'read_products', label: 'Read Products', icon: 'box' },
    { key: 'write_products', label: 'Write Products', icon: 'box-seam' },
    { key: 'read_orders', label: 'Read Orders', icon: 'bag' },
    { key: 'write_orders', label: 'Write Orders', icon: 'bag-check' },
    { key: 'read_customers', label: 'Read Customers', icon: 'people' },
    { key: 'manage_settings', label: 'Manage Settings', icon: 'gear' }
  ];

  // Pagination
  currentPage = signal(1);
  itemsPerPage = signal(10);

  filtered = computed(() => {
    let list = this.apiKeys();
    const q = this.searchQuery().toLowerCase();
    const s = this.filterStatus();
    if (q) list = list.filter(k => k.name.toLowerCase().includes(q));
    if (s) list = list.filter(k => k.status === s);
    return list;
  });

  totalPages = computed(() => Math.ceil(this.filtered().length / this.itemsPerPage()));

  paginatedItems = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    return this.filtered().slice(start, start + this.itemsPerPage());
  });

  activeKeys = computed(() => this.apiKeys().filter(k => k.status === 'active'));

  displayStats = computed(() => {
    const s = this.usageStats();
    return [
      { label: 'Total Requests', value: s.totalRequests.toLocaleString(), icon: 'graph-up', bg: 'bg-blue-100', ic: 'text-blue-600' },
      { label: 'Today', value: s.requestsToday.toLocaleString(), icon: 'calendar-check', bg: 'bg-green-100', ic: 'text-green-600' },
      { label: 'Error Rate', value: s.errorRate + '%', icon: 'exclamation-triangle', bg: 'bg-orange-100', ic: 'text-orange-600' },
      { label: 'Active Keys', value: this.activeKeys().length.toString(), icon: 'key', bg: 'bg-indigo-100', ic: 'text-indigo-600' }
    ];
  });

  ngOnInit() { this.load(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  load() {
    this.isLoading.set(true);
    this.svc.getApiKeys().pipe(takeUntil(this.destroy$)).subscribe(keys => {
      this.apiKeys.set(keys);
      this.isLoading.set(false);
    });
    this.svc.getApiUsageStats().pipe(takeUntil(this.destroy$)).subscribe(stats => this.usageStats.set(stats));
  }

  toggleScope(scope: string) {
    this.formScopes.update(s => ({ ...s, [scope]: !s[scope] }));
  }

  openCreate() {
    this.showCreateModal.set(true);
    this.formName.set('');
    this.formScopes.set({
      read_products: false, write_products: false,
      read_orders: false, write_orders: false,
      read_customers: false, manage_settings: false
    });
    this.newlyCreatedKey.set(null);
  }

  closeCreate() { this.showCreateModal.set(false); this.newlyCreatedKey.set(null); }

  create() {
    const scopes = Object.entries(this.formScopes()).filter(([, v]) => v).map(([k]) => k);
    if (!this.formName().trim()) {
      this.alertSvc.warning('Validation', 'Key name is required');
      return;
    }
    if (scopes.length === 0) {
      this.alertSvc.warning('Validation', 'Select at least one scope');
      return;
    }

    this.isCreating.set(true);
    this.svc.createApiKey({ name: this.formName(), scopes }).pipe(takeUntil(this.destroy$)).subscribe(result => {
      this.isCreating.set(false);
      if (result) {
        this.newlyCreatedKey.set(result.fullKey || null);
        this.alertSvc.success('API Key Created', 'Copy the key now — it won\'t be shown again');
        this.load();
      } else {
        this.alertSvc.error('Failed', 'Could not create API key');
      }
    });
  }

  copyKey() {
    const key = this.newlyCreatedKey();
    if (key) {
      navigator.clipboard.writeText(key);
      this.copiedKey.set(true);
      this.alertSvc.success('Copied', 'API key copied to clipboard');
      setTimeout(() => this.copiedKey.set(false), 2000);
    }
  }

  revoke(key: ApiKey) {
    this.alertSvc.confirm({
      title: 'Revoke API Key',
      message: `Revoke "${key.name}"? This cannot be undone and will immediately stop all API access using this key.`,
      confirmLabel: 'Revoke',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.svc.revokeApiKey(key.id).pipe(takeUntil(this.destroy$)).subscribe(success => {
        if (success) {
          this.alertSvc.success('Revoked', `API key "${key.name}" has been revoked`);
          this.load();
        } else {
          this.alertSvc.error('Failed', 'Could not revoke API key');
        }
      });
    });
  }

  bulkRevoke() {
    const count = this.selectedIds().length;
    this.alertSvc.confirm({
      title: 'Revoke Selected Keys',
      message: `Revoke ${count} API key(s)? This cannot be undone.`,
      confirmLabel: 'Revoke All',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.selectedIds().forEach(id => {
        this.svc.revokeApiKey(id).pipe(takeUntil(this.destroy$)).subscribe();
      });
      this.selectedIds.set([]);
      this.alertSvc.success('Revoked', `${count} API key(s) revoked`);
      setTimeout(() => this.load(), 500);
    });
  }

  openDetail(key: ApiKey) { this.detailItem.set(key); this.showDetail.set(true); }

  // Selection
  toggleSelection(id: string) { this.selectedIds.update(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]); }
  isSelected(id: string): boolean { return this.selectedIds().includes(id); }
  isAllSelected(): boolean { const items = this.paginatedItems(); return items.length > 0 && items.every(k => this.selectedIds().includes(k.id)); }
  toggleSelectAll() {
    if (this.isAllSelected()) this.selectedIds.set([]);
    else this.selectedIds.set(this.paginatedItems().map(k => k.id));
  }

  // Pagination
  goToPage(p: number) { this.currentPage.set(p); }
  previousPage() { if (this.currentPage() > 1) this.currentPage.update(p => p - 1); }
  nextPage() { if (this.currentPage() < this.totalPages()) this.currentPage.update(p => p + 1); }
  visiblePages(): (number | string)[] {
    const total = this.totalPages(), cur = this.currentPage();
    if (total <= 7) { const p: number[] = []; for (let i = 1; i <= total; i++) p.push(i); return p; }
    if (cur <= 3) return [1, 2, 3, 4, '...', total];
    if (cur >= total - 2) return [1, '...', total-3, total-2, total-1, total];
    return [1, '...', cur-1, cur, cur+1, '...', total];
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso));
  }

  getRowActions(key: ApiKey): DropdownItem[] {
    const items: DropdownItem[] = [
      { id: 'view', label: 'View Details', icon: 'eye' },
    ];
    if (key.status === 'active') {
      items.push({ id: 'divider1', label: '', divider: true });
      items.push({ id: 'revoke', label: 'Revoke Key', icon: 'x-circle', danger: true });
    }
    return items;
  }

  onRowAction(action: DropdownItem, key: ApiKey) {
    switch (action.id) {
      case 'view': this.openDetail(key); break;
      case 'revoke': this.revoke(key); break;
    }
  }
}
