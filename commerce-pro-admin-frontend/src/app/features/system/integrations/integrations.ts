import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { SystemService, Integration } from '../system.service';
import { AlertService } from '../../../shared/services/alert.service';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-system-integrations',
  standalone: true,
  imports: [CommonModule, FormsModule, Dropdown, HelpSidebar, TooltipLabel],
  templateUrl: './integrations.html',
  styles: [`:host { display: block; }`]
})
export class SystemIntegrations implements OnInit, OnDestroy {
  private svc = inject(SystemService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  integrations = signal<Integration[]>([]);
  isLoading = signal(true);

  // Filters
  searchQuery = signal('');
  filterType = signal('');
  filterStatus = signal('');
  integrationTypes: Integration['type'][] = ['Payment', 'Shipping', 'CRM', 'ERP', 'Email', 'Analytics'];

  // Selection
  selectedIds = signal<string[]>([]);

  // Panel state
  showPanel = signal(false);
  editingIntegration = signal<Integration | null>(null);
  formName = signal('');
  formType = signal<Integration['type']>('Payment');
  formConfig = signal('{}');
  formCredentials = signal('{}');
  isSaving = signal(false);

  // Detail sidebar
  showDetail = signal(false);
  detailItem = signal<Integration | null>(null);

  // Test state
  testingId = signal<string | null>(null);

  // Help
  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'What are Integrations?', content: 'Integrations connect your commerce platform with external services like payment gateways, shipping providers, CRMs, and analytics tools.' },
    { title: 'Adding an Integration', content: 'Click "Add Integration" to connect a new service. Provide the name, type, configuration JSON, and credentials JSON.' },
    { title: 'Testing Connections', content: 'Use the lightning bolt icon to test if the integration can connect to the external service successfully.' },
    { title: 'Bulk Actions', content: 'Select multiple integrations using checkboxes to perform bulk delete operations.' }
  ];

  // Pagination
  currentPage = signal(1);
  itemsPerPage = signal(10);

  totalPages = computed(() => Math.ceil(this.filtered().length / this.itemsPerPage()));

  filtered = computed(() => {
    let list = this.integrations();
    const q = this.searchQuery().toLowerCase();
    const t = this.filterType();
    const s = this.filterStatus();
    if (q) list = list.filter(i => i.name.toLowerCase().includes(q));
    if (t) list = list.filter(i => i.type === t);
    if (s) list = list.filter(i => i.status === s);
    return list;
  });

  paginatedItems = computed(() => {
    const start = (this.currentPage() - 1) * this.itemsPerPage();
    return this.filtered().slice(start, start + this.itemsPerPage());
  });

  isAllSelected = computed(() => {
    const items = this.paginatedItems();
    return items.length > 0 && items.every(i => this.selectedIds().includes(i.id));
  });

  ngOnInit() {
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => { this.currentPage.set(1); });

    this.load();
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  load() {
    this.isLoading.set(true);
    this.svc.getIntegrations().pipe(takeUntil(this.destroy$)).subscribe(data => {
      this.integrations.set(data);
      this.isLoading.set(false);
    });
  }

  onSearchInput(value: string) {
    this.searchQuery.set(value);
    this.searchSubject.next(value);
  }

  openAdd() {
    this.editingIntegration.set(null);
    this.formName.set('');
    this.formType.set('Payment');
    this.formConfig.set('{}');
    this.formCredentials.set('{}');
    this.showPanel.set(true);
  }

  openEdit(item: Integration) {
    this.editingIntegration.set(item);
    this.formName.set(item.name);
    this.formType.set(item.type);
    this.formConfig.set(JSON.stringify(item.configuration, null, 2));
    this.formCredentials.set(JSON.stringify(item.credentials, null, 2));
    this.showPanel.set(true);
  }

  closePanel() { this.showPanel.set(false); }

  openDetail(item: Integration) {
    this.detailItem.set(item);
    this.showDetail.set(true);
  }

  save() {
    if (!this.formName().trim()) {
      this.alertSvc.warning('Validation Error', 'Integration name is required');
      return;
    }

    this.isSaving.set(true);
    let config: Record<string, any> = {};
    let credentials: Record<string, string> = {};
    try {
      config = JSON.parse(this.formConfig());
      credentials = JSON.parse(this.formCredentials());
    } catch {
      this.alertSvc.error('Invalid JSON', 'Configuration or credentials contain invalid JSON');
      this.isSaving.set(false);
      return;
    }

    const payload: Partial<Integration> = {
      name: this.formName(),
      type: this.formType(),
      configuration: config,
      credentials
    };

    const editing = this.editingIntegration();
    const op = editing
      ? this.svc.updateIntegration(editing.id, payload)
      : this.svc.createIntegration(payload);

    op.pipe(takeUntil(this.destroy$)).subscribe(result => {
      this.isSaving.set(false);
      if (result) {
        this.alertSvc.success(editing ? 'Integration Updated' : 'Integration Created');
        this.closePanel();
        this.load();
      } else {
        this.alertSvc.error('Failed', 'Could not save integration');
      }
    });
  }

  deleteItem(item: Integration) {
    this.alertSvc.confirm({
      title: 'Delete Integration',
      message: `Are you sure you want to delete "${item.name}"? This cannot be undone.`,
      confirmLabel: 'Delete',
      danger: true
    }).then(ok => {
      if (!ok) return;
      this.svc.deleteIntegration(item.id).pipe(takeUntil(this.destroy$)).subscribe(success => {
        if (success) {
          this.alertSvc.success('Deleted', `"${item.name}" has been removed`);
          this.load();
        } else {
          this.alertSvc.error('Failed', 'Could not delete integration');
        }
      });
    });
  }

  bulkDelete() {
    const count = this.selectedIds().length;
    this.alertSvc.confirm({
      title: 'Delete Selected',
      message: `Delete ${count} integration(s)? This cannot be undone.`,
      confirmLabel: 'Delete All',
      danger: true
    }).then(ok => {
      if (!ok) return;
      const ids = [...this.selectedIds()];
      ids.forEach(id => {
        this.svc.deleteIntegration(id).pipe(takeUntil(this.destroy$)).subscribe();
      });
      this.selectedIds.set([]);
      this.alertSvc.success('Deleted', `${count} integration(s) removed`);
      setTimeout(() => this.load(), 500);
    });
  }

  testConnection(item: Integration) {
    this.testingId.set(item.id);
    this.svc.testIntegration(item.id).pipe(takeUntil(this.destroy$)).subscribe(result => {
      this.testingId.set(null);
      if (result.success) this.alertSvc.success('Connection OK', result.message);
      else this.alertSvc.error('Connection Failed', result.message);
    });
  }

  // Selection
  toggleSelection(id: string) {
    this.selectedIds.update(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]);
  }
  isSelected(id: string): boolean { return this.selectedIds().includes(id); }
  toggleSelectAll() {
    if (this.isAllSelected()) this.selectedIds.set([]);
    else this.selectedIds.set(this.paginatedItems().map(i => i.id));
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

  getStatusColor(status: string): string {
    const m: Record<string, string> = { active: 'bg-green-100 text-green-700', inactive: 'bg-gray-100 text-gray-600', error: 'bg-red-100 text-red-700' };
    return m[status] || 'bg-gray-100 text-gray-600';
  }
  getStatusDot(status: string): string {
    const m: Record<string, string> = { active: 'bg-green-500', inactive: 'bg-gray-400', error: 'bg-red-500' };
    return m[status] || 'bg-gray-400';
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }

  getRowActions(item: Integration): DropdownItem[] {
    return [
      { id: 'view', label: 'View Details', icon: 'eye' },
      { id: 'edit', label: 'Edit', icon: 'pencil' },
      { id: 'test', label: 'Test Connection', icon: 'lightning-charge' },
      { id: 'divider1', label: '', divider: true },
      { id: 'delete', label: 'Delete', icon: 'trash', danger: true }
    ];
  }

  onRowAction(action: DropdownItem, item: Integration) {
    switch (action.id) {
      case 'view': this.openDetail(item); break;
      case 'edit': this.openEdit(item); break;
      case 'test': this.testConnection(item); break;
      case 'delete': this.deleteItem(item); break;
    }
  }
}
