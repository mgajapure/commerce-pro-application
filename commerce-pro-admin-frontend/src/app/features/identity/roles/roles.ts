import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

import { CreateIdentityRoleRequest, IdentityPermission, IdentityRole, UpdateIdentityRoleRequest } from '../../../core/models/identity';
import { IdentityService } from '../../../core/services/identity/identity.service';
import { AlertService } from '../../../shared/services/alert.service';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-identity-roles',
  standalone: true,
  imports: [CommonModule, FormsModule, Dropdown, HelpSidebar, TooltipLabel],
  templateUrl: './roles.html',
  styles: [`:host { display: block; }`]
})
export class IdentityRoles implements OnInit, OnDestroy {
  private readonly svc = inject(IdentityService);
  private readonly alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  // List
  roles = signal<IdentityRole[]>([]);
  allPermissions = signal<IdentityPermission[]>([]);
  loading = signal(true);
  searchQuery = signal('');
  typeFilter = signal<'' | 'system' | 'custom'>('');
  showFilters = signal(true);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(0);
  pageSize = signal(10);

  // Create/Edit modal
  showModal = signal(false);
  editingRole = signal<IdentityRole | null>(null);
  modalStep = signal(1);
  saving = signal(false);
  createForm = signal<CreateIdentityRoleRequest>({ code: '', name: '', description: '', permissionCodes: [] });
  editForm = signal<UpdateIdentityRoleRequest>({ name: '', description: '' });
  selectedPermissions = signal<string[]>([]);
  permSearchQuery = signal('');

  // Help
  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Roles', content: 'Roles group permissions together. Assign roles to users to grant them a set of permissions at once.' },
    { title: 'System vs Custom', content: 'System roles are built-in and cannot be deleted. Custom roles can be created and managed freely.' },
    { title: 'Permissions Mapping', content: 'During role creation/editing, select permissions from the available list to define what the role can do.' }
  ];

  totalRoles = computed(() => this.roles().length);
  systemRoles = computed(() => this.roles().filter(r => r.system).length);
  customRoles = computed(() => this.roles().filter(r => !r.system).length);

  filteredRoles = computed(() => {
    let list = this.roles();
    const q = this.searchQuery().toLowerCase().trim();
    const type = this.typeFilter();
    if (q) list = list.filter(r => r.name.toLowerCase().includes(q) || r.code.toLowerCase().includes(q));
    if (type === 'system') list = list.filter(r => r.system);
    else if (type === 'custom') list = list.filter(r => !r.system);
    return list;
  });

  filteredPermissions = computed(() => {
    const q = this.permSearchQuery().toLowerCase().trim();
    if (!q) return this.allPermissions();
    return this.allPermissions().filter(p => p.code.toLowerCase().includes(q) || p.name.toLowerCase().includes(q));
  });

  roleActions: DropdownItem[] = [
    { id: 'edit', label: 'Edit Role', icon: 'pencil' },
    { id: 'permissions', label: 'Manage Permissions', icon: 'shield-check' },
    { id: 'divider1', label: '', divider: true },
    { id: 'delete', label: 'Delete Role', icon: 'trash', danger: true }
  ];

  ngOnInit() {
    this.loadRoles();
    this.loadPermissions();
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  loadRoles(page = 0) {
    this.loading.set(true);
    this.currentPage.set(page);
    this.svc.getRoles(page, this.pageSize()).pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.roles.set(res.content);
      this.totalElements.set(res.totalElements);
      this.totalPages.set(res.totalPages);
      this.loading.set(false);
    });
  }

  loadPermissions() {
    this.svc.getPermissions(0, 200).pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.allPermissions.set(res.content);
    });
  }

  reload() { this.loadRoles(this.currentPage()); }

  openCreate() {
    this.editingRole.set(null);
    this.modalStep.set(1);
    this.createForm.set({ code: '', name: '', description: '', permissionCodes: [] });
    this.selectedPermissions.set([]);
    this.permSearchQuery.set('');
    this.showModal.set(true);
  }

  openEdit(role: IdentityRole) {
    this.editingRole.set(role);
    this.modalStep.set(1);
    this.editForm.set({ name: role.name, description: role.description ?? '' });
    this.selectedPermissions.set([]); // Will be populated from current role permissions
    this.permSearchQuery.set('');
    this.showModal.set(true);
  }

  openPermissions(role: IdentityRole) {
    this.editingRole.set(role);
    this.modalStep.set(2);
    this.selectedPermissions.set([]);
    this.permSearchQuery.set('');
    this.showModal.set(true);
  }

  updateCreateField(field: string, value: string) {
    this.createForm.update(f => ({ ...f, [field]: value }));
  }

  updateEditField(field: string, value: string) {
    this.editForm.update(f => ({ ...f, [field]: value }));
  }

  togglePermission(code: string) {
    this.selectedPermissions.update(codes => codes.includes(code) ? codes.filter(c => c !== code) : [...codes, code]);
  }

  saveRole() {
    this.saving.set(true);
    if (this.editingRole()) {
      const role = this.editingRole()!;
      this.svc.updateRole(role.id, this.editForm()).pipe(takeUntil(this.destroy$)).subscribe(updated => {
        if (!updated) { this.saving.set(false); this.alertSvc.error('Failed', 'Could not update role'); return; }
        // Grant selected permissions
        if (this.selectedPermissions().length > 0) {
          this.svc.grantPermissions(role.id, this.selectedPermissions()).pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.saving.set(false); this.showModal.set(false);
            this.alertSvc.success('Role Updated', `"${updated.name}" updated successfully`);
            this.reload();
          });
        } else {
          this.saving.set(false); this.showModal.set(false);
          this.alertSvc.success('Role Updated', `"${updated.name}" updated successfully`);
          this.reload();
        }
      });
    } else {
      const form = this.createForm();
      if (!form.code || !form.name) {
        this.saving.set(false); this.alertSvc.warning('Missing Fields', 'Code and name are required'); return;
      }
      const payload = { ...form, permissionCodes: this.selectedPermissions() };
      this.svc.createRole(payload).pipe(takeUntil(this.destroy$)).subscribe(role => {
        this.saving.set(false);
        if (!role) { this.alertSvc.error('Failed', 'Could not create role'); return; }
        this.showModal.set(false);
        this.alertSvc.success('Role Created', `"${role.name}" created successfully`);
        this.reload();
      });
    }
  }

  grantPermissions() {
    const role = this.editingRole();
    if (!role || !this.selectedPermissions().length) return;
    this.saving.set(true);
    this.svc.grantPermissions(role.id, this.selectedPermissions()).pipe(takeUntil(this.destroy$)).subscribe(ok => {
      this.saving.set(false);
      if (ok) {
        this.alertSvc.success('Permissions Granted', `${this.selectedPermissions().length} permission(s) granted to "${role.name}"`);
        this.selectedPermissions.set([]);
        this.reload();
      } else this.alertSvc.error('Failed', 'Could not grant permissions');
    });
  }

  handleAction(item: DropdownItem, role: IdentityRole) {
    switch (item.id) {
      case 'edit': this.openEdit(role); break;
      case 'permissions': this.openPermissions(role); break;
      case 'delete': this.deleteRole(role); break;
    }
  }

  async deleteRole(role: IdentityRole) {
    if (role.system) { this.alertSvc.warning('Cannot Delete', 'System roles cannot be deleted'); return; }
    const confirmed = await this.alertSvc.confirm({ title: 'Delete Role', message: `Are you sure you want to delete "${role.name}"?`, danger: true });
    if (!confirmed) return;
    this.svc.deleteRole(role.id).pipe(takeUntil(this.destroy$)).subscribe(ok => {
      if (ok) { this.alertSvc.success('Role Deleted', `"${role.name}" removed`); this.reload(); }
      else this.alertSvc.error('Failed', 'Could not delete role');
    });
  }

  goToPage(page: number) { if (page >= 0 && page < this.totalPages()) this.loadRoles(page); }
}
