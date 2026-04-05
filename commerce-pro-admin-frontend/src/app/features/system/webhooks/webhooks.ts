import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SystemService, Webhook } from '../system.service';
import { AlertService } from '../../../shared/services/alert.service';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-system-webhooks',
  standalone: true,
  imports: [CommonModule, FormsModule, Dropdown, HelpSidebar, TooltipLabel],
  templateUrl: './webhooks.html',
  styles: [`:host { display: block; }`]
})
export class SystemWebhooks implements OnInit, OnDestroy {
  private svc = inject(SystemService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  webhooks = signal<Webhook[]>([]);
  isLoading = signal(true);

  // Filters
  searchQuery = signal('');
  filterActive = signal('');

  // Selection
  selectedIds = signal<string[]>([]);

  // Modal
  showModal = signal(false);
  editingId = signal<string | null>(null);
  formUrl = signal('');
  formEvents = signal<Record<string, boolean>>({});
  formActive = signal(true);
  isSaving = signal(false);

  // Detail sidebar
  showDetail = signal(false);
  detailItem = signal<Webhook | null>(null);

  // Test state
  testingId = signal<string | null>(null);

  // Help
  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'What are Webhooks?', content: 'Webhooks send real-time HTTP notifications to your endpoints when events occur in the system.' },
    { title: 'Events', content: 'Subscribe to specific events like order.created, product.updated, or payment.completed to receive targeted notifications.' },
    { title: 'Failures', content: 'Failed webhook deliveries are retried automatically. Monitor the failure count and test webhooks to diagnose issues.' },
    { title: 'Security', content: 'Each webhook gets a secret key for signature verification. Always validate webhook signatures in your endpoint.' }
  ];

  // Pagination
  currentPage = signal(1);
  itemsPerPage = signal(10);

  availableEvents = [
    'order.created', 'order.updated', 'order.fulfilled', 'order.cancelled',
    'product.created', 'product.updated', 'product.deleted',
    'customer.created', 'customer.updated',
    'payment.completed', 'refund.processed', 'inventory.low'
  ];

  filtered = computed(() => {
    let list = this.webhooks();
    const q = this.searchQuery().toLowerCase();
    const a = this.filterActive();
    if (q) list = list.filter(w => w.url.toLowerCase().includes(q));
    if (a === 'active') list = list.filter(w => w.active);
    if (a === 'inactive') list = list.filter(w => !w.active);
    return list;
  });

  totalPages = computed(() => Math.ceil(this.filtered().length / this.itemsPerPage()));
  paginatedItems = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    return this.filtered().slice(start, start + this.itemsPerPage());
  });

  ngOnInit() { this.load(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  load() {
    this.isLoading.set(true);
    this.svc.getWebhooks().pipe(takeUntil(this.destroy$)).subscribe(wh => {
      this.webhooks.set(wh);
      this.isLoading.set(false);
    });
  }

  openCreate() {
    this.editingId.set(null);
    this.formUrl.set('');
    this.formActive.set(true);
    const ev: Record<string, boolean> = {};
    this.availableEvents.forEach(e => ev[e] = false);
    this.formEvents.set(ev);
    this.showModal.set(true);
  }

  openEdit(wh: Webhook) {
    this.editingId.set(wh.id);
    this.formUrl.set(wh.url);
    this.formActive.set(wh.active);
    const ev: Record<string, boolean> = {};
    this.availableEvents.forEach(e => ev[e] = wh.events.includes(e));
    this.formEvents.set(ev);
    this.showModal.set(true);
  }

  closeModal() { this.showModal.set(false); }

  toggleEvent(event: string) {
    this.formEvents.update(e => ({ ...e, [event]: !e[event] }));
  }

  save() {
    const events = Object.entries(this.formEvents()).filter(([, v]) => v).map(([k]) => k);
    if (!this.formUrl().trim()) {
      this.alertSvc.warning('Validation', 'Endpoint URL is required');
      return;
    }
    if (events.length === 0) {
      this.alertSvc.warning('Validation', 'Select at least one event');
      return;
    }

    this.isSaving.set(true);
    const data = { url: this.formUrl(), events, active: this.formActive() };
    const obs = this.editingId()
      ? this.svc.updateWebhook(this.editingId()!, data)
      : this.svc.createWebhook(data);

    obs.pipe(takeUntil(this.destroy$)).subscribe(result => {
      this.isSaving.set(false);
      if (result) {
        this.alertSvc.success(this.editingId() ? 'Webhook Updated' : 'Webhook Created');
        this.showModal.set(false);
        this.load();
      } else {
        this.alertSvc.error('Failed', 'Could not save webhook');
      }
    });
  }

  deleteWebhook(wh: Webhook) {
    this.alertSvc.confirm({
      title: 'Delete Webhook',
      message: `Delete webhook for "${wh.url}"? This cannot be undone.`,
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.svc.deleteWebhook(wh.id).pipe(takeUntil(this.destroy$)).subscribe(success => {
        if (success) { this.alertSvc.success('Deleted', 'Webhook removed'); this.load(); }
        else this.alertSvc.error('Failed', 'Could not delete webhook');
      });
    });
  }

  bulkDelete() {
    const count = this.selectedIds().length;
    this.alertSvc.confirm({
      title: 'Delete Selected',
      message: `Delete ${count} webhook(s)? This cannot be undone.`,
      confirmLabel: 'Delete All',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.selectedIds().forEach(id => this.svc.deleteWebhook(id).pipe(takeUntil(this.destroy$)).subscribe());
      this.selectedIds.set([]);
      this.alertSvc.success('Deleted', `${count} webhook(s) removed`);
      setTimeout(() => this.load(), 500);
    });
  }

  testWebhook(wh: Webhook) {
    this.testingId.set(wh.id);
    this.svc.testWebhook(wh.id).pipe(takeUntil(this.destroy$)).subscribe(result => {
      this.testingId.set(null);
      if (result.success) this.alertSvc.success('Test Successful', `Status: ${result.statusCode}`);
      else this.alertSvc.error('Test Failed', result.message);
    });
  }

  openDetail(wh: Webhook) { this.detailItem.set(wh); this.showDetail.set(true); }

  // Selection
  toggleSelection(id: string) { this.selectedIds.update(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]); }
  isSelected(id: string): boolean { return this.selectedIds().includes(id); }
  isAllSelected(): boolean { const items = this.paginatedItems(); return items.length > 0 && items.every(w => this.selectedIds().includes(w.id)); }
  toggleSelectAll() { if (this.isAllSelected()) this.selectedIds.set([]); else this.selectedIds.set(this.paginatedItems().map(w => w.id)); }

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
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  getRowActions(wh: Webhook): DropdownItem[] {
    return [
      { id: 'view', label: 'View Details', icon: 'eye' },
      { id: 'edit', label: 'Edit', icon: 'pencil' },
      { id: 'test', label: 'Test Webhook', icon: 'send' },
      { id: 'divider1', label: '', divider: true },
      { id: 'delete', label: 'Delete', icon: 'trash', danger: true }
    ];
  }

  onRowAction(action: DropdownItem, wh: Webhook) {
    switch (action.id) {
      case 'view': this.openDetail(wh); break;
      case 'edit': this.openEdit(wh); break;
      case 'test': this.testWebhook(wh); break;
      case 'delete': this.deleteWebhook(wh); break;
    }
  }
}
