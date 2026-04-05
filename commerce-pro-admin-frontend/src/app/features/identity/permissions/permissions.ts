import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

import { CreateIdentityPermissionRequest, IdentityPermission } from '../../../core/models/identity';
import { IdentityService } from '../../../core/services/identity/identity.service';
import { AlertService } from '../../../shared/services/alert.service';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-identity-permissions',
  standalone: true,
  imports: [CommonModule, FormsModule, Dropdown, HelpSidebar, TooltipLabel],
  templateUrl: './permissions.html',
  styles: [`:host { display: block; }`]
})
export class IdentityPermissions implements OnInit, OnDestroy {
  private readonly svc = inject(IdentityService);
  private readonly alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  permissions = signal<IdentityPermission[]>([]);
  loading = signal(true);
  searchQuery = signal('');
  categoryFilter = signal('');
  riskFilter = signal<'' | '1' | '2' | '3' | '4' | '5'>('');
  showFilters = signal(true);

  // Create modal
  showCreateModal = signal(false);
  saving = signal(false);
  createForm = signal<CreateIdentityPermissionRequest>({
    code: '', name: '', description: '', category: '', riskLevel: 1, requiresApproval: false, applicableScopes: []
  });

  // Detail sidebar
  showDetail = signal(false);
  selectedPermission = signal<IdentityPermission | null>(null);

  // Pagination
  currentPage = signal(0);
  pageSize = signal(25);
  totalElements = signal(0);
  totalPages = signal(0);

  // Help
  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Permissions', content: 'Permissions are fine-grained access controls that define what actions can be performed in the system.' },
    { title: 'Risk Levels', content: '1 = Very Low, 2 = Low, 3 = Medium, 4 = High, 5 = Critical. Higher risk permissions may require approval.' },
    { title: 'Categories', content: 'Permissions are grouped by category (e.g., IDENTITY, CATALOG) for easier management.' }
  ];

  categories = computed(() => {
    const cats = new Set(this.permissions().map(p => p.category ?? 'UNCATEGORIZED'));
    return Array.from(cats).sort();
  });

  filteredPermissions = computed(() => {
    let list = this.permissions();
    const cat = this.categoryFilter();
    const q = this.searchQuery().toLowerCase().trim();
    const risk = this.riskFilter();
    if (cat) list = list.filter(p => (p.category ?? 'UNCATEGORIZED') === cat);
    if (q) list = list.filter(p => p.code.toLowerCase().includes(q) || p.name.toLowerCase().includes(q));
    if (risk) list = list.filter(p => p.riskLevel === +risk);
    return list;
  });

  permActions: DropdownItem[] = [
    { id: 'view', label: 'View Details', icon: 'bi-eye' },
    { id: 'divider1', label: '', divider: true },
    { id: 'delete', label: 'Delete', icon: 'bi-trash', danger: true }
  ];

  ngOnInit() { this.loadPermissions(); }
  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  loadPermissions(page = 0) {
    this.loading.set(true);
    this.currentPage.set(page);
    this.svc.getPermissions(page, this.pageSize()).pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.permissions.set(res.content);
      this.totalElements.set(res.totalElements);
      this.totalPages.set(res.totalPages);
      this.loading.set(false);
    });
  }

  reload() { this.loadPermissions(this.currentPage()); }

  openCreate() {
    this.createForm.set({ code: '', name: '', description: '', category: '', riskLevel: 1, requiresApproval: false, applicableScopes: [] });
    this.showCreateModal.set(true);
  }

  updateField(field: string, value: string | number | boolean) {
    this.createForm.update(f => ({ ...f, [field]: value }));
  }

  createPermission() {
    const form = this.createForm();
    if (!form.code || !form.name) { this.alertSvc.warning('Missing Fields', 'Code and name are required'); return; }
    this.saving.set(true);
    this.svc.createPermission(form).pipe(takeUntil(this.destroy$)).subscribe(perm => {
      this.saving.set(false);
      if (!perm) { this.alertSvc.error('Failed', 'Could not create permission'); return; }
      this.showCreateModal.set(false);
      this.alertSvc.success('Permission Created', `"${perm.code}" created`);
      this.reload();
    });
  }

  viewDetail(perm: IdentityPermission) {
    this.selectedPermission.set(perm);
    this.showDetail.set(true);
  }

  handleAction(item: DropdownItem, perm: IdentityPermission) {
    switch (item.id) {
      case 'view': this.viewDetail(perm); break;
      case 'delete': this.deletePermission(perm); break;
    }
  }

  async deletePermission(perm: IdentityPermission) {
    if (perm.system) { this.alertSvc.warning('Cannot Delete', 'System permissions cannot be deleted'); return; }
    const confirmed = await this.alertSvc.confirm({ title: 'Delete Permission', message: `Delete "${perm.code}"? This will remove it from all roles.`, danger: true });
    if (!confirmed) return;
    this.svc.deletePermission(perm.code).pipe(takeUntil(this.destroy$)).subscribe(ok => {
      if (ok) { this.alertSvc.success('Deleted', `"${perm.code}" removed`); this.reload(); }
      else this.alertSvc.error('Failed', 'Could not delete permission');
    });
  }

  riskLabel(level: number): string {
    return ['', 'Very Low', 'Low', 'Medium', 'High', 'Critical'][level] ?? `Level ${level}`;
  }

  riskClass(level: number): string {
    const map: Record<number, string> = { 1: 'bg-green-100 text-green-700', 2: 'bg-teal-100 text-teal-700', 3: 'bg-yellow-100 text-yellow-700', 4: 'bg-orange-100 text-orange-700', 5: 'bg-red-100 text-red-700' };
    return map[level] ?? 'bg-gray-100 text-gray-600';
  }

  goToPage(page: number) { if (page >= 0 && page < this.totalPages()) this.loadPermissions(page); }
}
