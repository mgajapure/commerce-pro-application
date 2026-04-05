import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';

import { AuditLogEntry, CreateIdentityUserRequest, IdentityUser, IdentityUserDetail, IdentityRole, ImpersonationToken } from '../../../core/models/identity';
import { IdentityService } from '../../../core/services/identity/identity.service';
import { AlertService } from '../../../shared/services/alert.service';
import { Dropdown, DropdownItem } from '../../../shared/components/dropdown/dropdown';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-identity-users',
  standalone: true,
  imports: [CommonModule, FormsModule, Dropdown, HelpSidebar, TooltipLabel],
  templateUrl: './users.html',
  styles: [`:host { display: block; }`]
})
export class IdentityUsers implements OnInit, OnDestroy {
  private readonly svc = inject(IdentityService);
  private readonly alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();
  private search$ = new Subject<string>();

  // List state
  users = signal<IdentityUser[]>([]);
  allRoles = signal<IdentityRole[]>([]);
  loading = signal(true);
  totalElements = signal(0);
  totalPages = signal(0);
  currentPage = signal(0);
  pageSize = signal(10);
  searchQuery = signal('');
  statusFilter = signal<'' | 'active' | 'inactive'>('');
  mfaFilter = signal<'' | 'enabled' | 'disabled'>('');
  showFilters = signal(true);

  // Create/Edit modal
  showCreateModal = signal(false);
  editingUser = signal<IdentityUser | null>(null);
  createStep = signal(1);
  saving = signal(false);
  createForm = signal<CreateIdentityUserRequest>({
    username: '', email: '', password: '', firstName: '', lastName: '', phone: '',
    sendWelcomeEmail: true, initialRoleCodes: []
  });
  editForm = signal<{ email: string; firstName: string; lastName: string; phone: string; active: boolean; mfaEnabled: boolean }>({
    email: '', firstName: '', lastName: '', phone: '', active: true, mfaEnabled: false
  });
  selectedRoleCodes = signal<string[]>([]);

  // Detail sidebar
  showDetail = signal(false);
  selectedUser = signal<IdentityUserDetail | null>(null);
  userAuditLogs = signal<AuditLogEntry[]>([]);
  detailLoading = signal(false);
  detailTab = signal<'info' | 'roles' | 'audit'>('info');

  // Impersonation
  impersonationToken = signal<ImpersonationToken | null>(null);

  // Help
  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'User Management', content: 'Create, edit, and manage user accounts. Assign roles and permissions to control access.' },
    { title: 'User Actions', content: 'Use the action menu on each row to view details, reset passwords, lock/unlock accounts, or impersonate users.' },
    { title: 'Filters', content: 'Filter users by status, MFA status, or search by username/email.' },
    { title: 'Roles', content: 'Assign roles during user creation or via the detail panel. Roles determine what permissions a user has.' }
  ];

  filteredUsers = computed(() => {
    let list = this.users();
    const status = this.statusFilter();
    const mfa = this.mfaFilter();
    if (status === 'active') list = list.filter(u => u.active);
    else if (status === 'inactive') list = list.filter(u => !u.active);
    if (mfa === 'enabled') list = list.filter(u => u.mfaEnabled);
    else if (mfa === 'disabled') list = list.filter(u => !u.mfaEnabled);
    return list;
  });

  userActions: DropdownItem[] = [
    { id: 'view', label: 'View Details', icon: 'eye' },
    { id: 'edit', label: 'Edit User', icon: 'pencil' },
    { id: 'divider1', label: '', divider: true },
    { id: 'reset-pwd', label: 'Reset Password', icon: 'key' },
    { id: 'unlock', label: 'Unlock Account', icon: 'unlock' },
    { id: 'toggle', label: 'Toggle Status', icon: 'toggle-on' },
    { id: 'impersonate', label: 'Impersonate', icon: 'person-badge' },
    { id: 'divider2', label: '', divider: true },
    { id: 'delete', label: 'Delete User', icon: 'trash', danger: true }
  ];

  ngOnInit() {
    this.loadUsers();
    this.loadRoles();
    this.search$.pipe(
      debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$)
    ).subscribe(() => this.loadUsers(0));
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  onSearchChange(q: string) {
    this.searchQuery.set(q);
    this.search$.next(q);
  }

  loadUsers(page = 0) {
    this.loading.set(true);
    this.currentPage.set(page);
    this.svc.getUsers(page, this.pageSize(), this.searchQuery()).pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.users.set(res.content);
      this.totalElements.set(res.totalElements);
      this.totalPages.set(res.totalPages);
      this.loading.set(false);
    });
  }

  loadRoles() {
    this.svc.getRoles(0, 100).pipe(takeUntil(this.destroy$)).subscribe(res => {
      this.allRoles.set(res.content);
    });
  }

  reload() { this.loadUsers(this.currentPage()); }

  // Create / Edit
  openCreate() {
    this.editingUser.set(null);
    this.createStep.set(1);
    this.createForm.set({ username: '', email: '', password: '', firstName: '', lastName: '', phone: '', sendWelcomeEmail: true, initialRoleCodes: [] });
    this.selectedRoleCodes.set([]);
    this.showCreateModal.set(true);
  }

  openEdit(user: IdentityUser) {
    this.editingUser.set(user);
    this.createStep.set(1);
    this.editForm.set({
      email: user.email, firstName: user.firstName ?? '', lastName: user.lastName ?? '',
      phone: user.phone ?? '', active: user.active, mfaEnabled: user.mfaEnabled
    });
    this.selectedRoleCodes.set([...user.roleCodes]);
    this.showCreateModal.set(true);
  }

  updateCreateField(field: string, value: any) {
    this.createForm.update(f => ({ ...f, [field]: value }));
  }

  updateEditField(field: string, value: any) {
    this.editForm.update(f => ({ ...f, [field]: value }));
  }

  toggleRoleSelection(code: string) {
    this.selectedRoleCodes.update(codes => codes.includes(code) ? codes.filter(c => c !== code) : [...codes, code]);
  }

  saveUser() {
    this.saving.set(true);
    if (this.editingUser()) {
      const user = this.editingUser()!;
      this.svc.updateUser(user.id, this.editForm()).pipe(takeUntil(this.destroy$)).subscribe(updated => {
        if (!updated) { this.saving.set(false); this.alertSvc.error('Failed', 'Could not update user'); return; }
        // Assign roles if changed
        this.svc.assignRoles(user.id, this.selectedRoleCodes()).pipe(takeUntil(this.destroy$)).subscribe(() => {
          this.saving.set(false);
          this.showCreateModal.set(false);
          this.alertSvc.success('User Updated', `"${updated.username}" updated successfully`);
          this.reload();
        });
      });
    } else {
      const form = this.createForm();
      if (!form.username || !form.email || !form.password) {
        this.saving.set(false);
        this.alertSvc.warning('Missing Fields', 'Username, email, and password are required');
        return;
      }
      const payload = { ...form, initialRoleCodes: this.selectedRoleCodes() };
      this.svc.createUser(payload).pipe(takeUntil(this.destroy$)).subscribe(user => {
        this.saving.set(false);
        if (!user) { this.alertSvc.error('Failed', 'Could not create user'); return; }
        this.showCreateModal.set(false);
        this.alertSvc.success('User Created', `"${user.username}" created successfully`);
        this.reload();
      });
    }
  }

  // User actions
  handleAction(item: DropdownItem, user: IdentityUser) {
    switch (item.id) {
      case 'view': this.viewDetail(user); break;
      case 'edit': this.openEdit(user); break;
      case 'toggle': this.toggleStatus(user); break;
      case 'reset-pwd': this.resetPassword(user); break;
      case 'unlock': this.unlockAccount(user); break;
      case 'impersonate': this.impersonate(user); break;
      case 'delete': this.deleteUser(user); break;
    }
  }

  toggleStatus(user: IdentityUser) {
    this.svc.toggleUserActivation(user.id, !user.active).pipe(takeUntil(this.destroy$)).subscribe(ok => {
      if (ok) {
        this.alertSvc.success('Status Changed', `"${user.username}" ${user.active ? 'deactivated' : 'activated'}`);
        this.reload();
      } else this.alertSvc.error('Failed', 'Could not change user status');
    });
  }

  async resetPassword(user: IdentityUser) {
    const confirmed = await this.alertSvc.confirm({ title: 'Reset Password', message: `Send password reset email to "${user.username}"?` });
    if (!confirmed) return;
    this.svc.resetUserPassword(user.id, true).pipe(takeUntil(this.destroy$)).subscribe(ok => {
      if (ok) this.alertSvc.success('Password Reset', 'Reset email sent successfully');
      else this.alertSvc.error('Failed', 'Could not reset password');
    });
  }

  unlockAccount(user: IdentityUser) {
    this.svc.unlockUserAccount(user.id).pipe(takeUntil(this.destroy$)).subscribe(ok => {
      if (ok) { this.alertSvc.success('Account Unlocked', `"${user.username}" unlocked`); this.reload(); }
      else this.alertSvc.error('Failed', 'Could not unlock account');
    });
  }

  impersonate(user: IdentityUser) {
    this.svc.impersonateUser(user.id).pipe(takeUntil(this.destroy$)).subscribe(token => {
      if (!token) { this.alertSvc.error('Failed', 'Could not start impersonation'); return; }
      this.impersonationToken.set(token);
      this.alertSvc.success('Impersonation Started', `Now impersonating "${user.username}"`);
    });
  }

  async deleteUser(user: IdentityUser) {
    const confirmed = await this.alertSvc.confirm({ title: 'Delete User', message: `Are you sure you want to delete "${user.username}"? This action cannot be undone.`, danger: true });
    if (!confirmed) return;
    this.svc.deleteUser(user.id, 'Deleted via admin UI').pipe(takeUntil(this.destroy$)).subscribe(ok => {
      if (ok) {
        this.alertSvc.success('User Deleted', `"${user.username}" removed`);
        if (this.selectedUser()?.id === user.id) this.showDetail.set(false);
        this.reload();
      } else this.alertSvc.error('Failed', 'Could not delete user');
    });
  }

  clearImpersonation() { this.impersonationToken.set(null); }

  // Detail sidebar
  viewDetail(user: IdentityUser) {
    this.showDetail.set(true);
    this.detailTab.set('info');
    this.detailLoading.set(true);
    this.userAuditLogs.set([]);
    this.svc.getUserDetail(user.id).pipe(takeUntil(this.destroy$)).subscribe(detail => {
      this.detailLoading.set(false);
      this.selectedUser.set(detail);
    });
  }

  loadUserAudit() {
    const user = this.selectedUser();
    if (!user) return;
    this.detailTab.set('audit');
    this.detailLoading.set(true);
    this.svc.getUserAudit(user.id, 0, 20).pipe(takeUntil(this.destroy$)).subscribe(page => {
      this.detailLoading.set(false);
      this.userAuditLogs.set(page.content);
    });
  }

  // Pagination
  goToPage(page: number) { if (page >= 0 && page < this.totalPages()) this.loadUsers(page); }
}
