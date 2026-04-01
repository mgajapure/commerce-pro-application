import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuditLogEntry, CreateIdentityUserRequest, IdentityUser, IdentityUserDetail } from '../../../core/models/identity';
import { IdentityService } from '../../../core/services/identity/identity.service';

type UsersPanel = 'create' | 'assign-roles' | 'user-detail';

@Component({
  selector: 'app-identity-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './users.html',
  styleUrl: './users.scss'
})
export class IdentityUsers implements OnInit {
  private readonly identityService = inject(IdentityService);

  readonly users = signal<IdentityUser[]>([]);
  readonly isLoading = signal(false);
  readonly searchQuery = signal('');
  readonly activePanel = signal<UsersPanel>('create');

  // Create user
  readonly createUserForm = signal<CreateIdentityUserRequest>({
    username: '',
    email: '',
    password: '',
    sendWelcomeEmail: true,
    initialRoleCodes: []
  });

  // Assign roles
  readonly selectedUserId = signal('');
  readonly roleCodesInput = signal('');

  // User detail / audit
  readonly selectedUser = signal<IdentityUserDetail | null>(null);
  readonly userAuditLogs = signal<AuditLogEntry[]>([]);
  readonly isDetailLoading = signal(false);
  readonly detailTab = signal<'info' | 'audit'>('info');

  // Action feedback
  readonly actionMessage = signal('');
  readonly actionError = signal('');

  readonly filteredUsers = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    if (!query) return this.users();
    return this.users().filter(user => [user.username, user.email].some(value => value.toLowerCase().includes(query)));
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.isLoading.set(true);
    this.identityService.getUsers(0, 50).subscribe(page => {
      this.users.set(page.content);
      this.isLoading.set(false);
    });
  }

  updateCreateUserField(field: keyof CreateIdentityUserRequest, value: string | boolean): void {
    this.createUserForm.update(current => ({ ...current, [field]: value }));
  }

  createUser(): void {
    this.clearMessages();
    this.identityService.createUser(this.createUserForm()).subscribe(user => {
      if (!user) {
        this.actionError.set('Failed to create user.');
        return;
      }
      this.users.update(list => [user, ...list]);
      this.createUserForm.set({ username: '', email: '', password: '', sendWelcomeEmail: true, initialRoleCodes: [] });
      this.actionMessage.set(`User "${user.username}" created successfully.`);
    });
  }

  toggleUserStatus(user: IdentityUser): void {
    this.clearMessages();
    this.identityService.toggleUserActivation(user.id, !user.active).subscribe(success => {
      if (!success) {
        this.actionError.set('Failed to update user status.');
        return;
      }
      this.users.update(list => list.map(value => value.id === user.id ? { ...value, active: !value.active } : value));
    });
  }

  resetUserPassword(user: IdentityUser): void {
    this.clearMessages();
    this.identityService.resetUserPassword(user.id, true).subscribe(success => {
      if (!success) {
        this.actionError.set(`Failed to reset password for "${user.username}".`);
        return;
      }
      this.actionMessage.set(`Password reset initiated for "${user.username}". Notification email sent.`);
    });
  }

  unlockUser(user: IdentityUser): void {
    this.clearMessages();
    this.identityService.unlockUserAccount(user.id).subscribe(success => {
      if (!success) {
        this.actionError.set(`Failed to unlock account for "${user.username}".`);
        return;
      }
      this.actionMessage.set(`Account "${user.username}" unlocked successfully.`);
    });
  }

  deleteUser(user: IdentityUser): void {
    this.clearMessages();
    this.identityService.deleteUser(user.id, 'Deleted via admin UI').subscribe(success => {
      if (!success) {
        this.actionError.set(`Failed to delete user "${user.username}".`);
        return;
      }
      this.users.update(list => list.filter(value => value.id !== user.id));
      this.actionMessage.set(`User "${user.username}" deleted.`);
      if (this.selectedUser()?.id === user.id) {
        this.selectedUser.set(null);
        this.activePanel.set('create');
      }
    });
  }

  viewUserDetail(user: IdentityUser): void {
    this.clearMessages();
    this.activePanel.set('user-detail');
    this.detailTab.set('info');
    this.isDetailLoading.set(true);
    this.userAuditLogs.set([]);

    this.identityService.getUserDetail(user.id).subscribe(detail => {
      this.isDetailLoading.set(false);
      this.selectedUser.set(detail);
    });
  }

  loadUserAudit(): void {
    const user = this.selectedUser();
    if (!user) return;
    this.detailTab.set('audit');
    this.isDetailLoading.set(true);

    this.identityService.getUserAudit(user.id, 0, 20).subscribe(page => {
      this.isDetailLoading.set(false);
      this.userAuditLogs.set(page.content);
    });
  }

  mapRolesToUser(): void {
    if (!this.selectedUserId()) return;

    const roleCodes = this.roleCodesInput().split(',').map(value => value.trim()).filter(Boolean);
    if (!roleCodes.length) return;

    this.clearMessages();
    this.identityService.assignRoles(this.selectedUserId(), roleCodes).subscribe(success => {
      if (!success) {
        this.actionError.set('Failed to assign roles.');
        return;
      }
      this.roleCodesInput.set('');
      this.actionMessage.set('Roles assigned successfully.');
    });
  }

  showPanel(panel: UsersPanel): void {
    this.activePanel.set(panel);
    this.clearMessages();
  }

  private clearMessages(): void {
    this.actionMessage.set('');
    this.actionError.set('');
  }
}
