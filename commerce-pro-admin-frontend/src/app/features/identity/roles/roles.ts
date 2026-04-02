import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CreateIdentityRoleRequest, IdentityRole, UpdateIdentityRoleRequest } from '../../../core/models/identity';
import { IdentityService } from '../../../core/services/identity/identity.service';

type RolesPanel = 'create' | 'permissions' | 'edit';

@Component({
  selector: 'app-identity-roles',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './roles.html',
  styleUrl: './roles.scss'
})
export class IdentityRoles implements OnInit {
  private readonly identityService = inject(IdentityService);

  readonly roles = signal<IdentityRole[]>([]);
  readonly isLoading = signal(false);
  readonly activePanel = signal<RolesPanel>('create');
  readonly selectedRole = signal<IdentityRole | null>(null);

  readonly actionMessage = signal('');
  readonly actionError = signal('');

  // Create
  readonly createRoleForm = signal<CreateIdentityRoleRequest>({
    code: '',
    name: '',
    description: '',
    permissionCodes: []
  });

  // Edit
  readonly editForm = signal<UpdateIdentityRoleRequest>({ name: '', description: '' });

  // Permissions
  readonly grantCodesInput = signal('');
  readonly revokeCodesInput = signal('');

  readonly totalRoles = computed(() => this.roles().length);
  readonly systemRoles = computed(() => this.roles().filter(r => r.system).length);
  readonly customRoles = computed(() => this.roles().filter(r => !r.system).length);

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles(): void {
    this.isLoading.set(true);
    this.identityService.getRoles(0, 50).subscribe(page => {
      this.roles.set(page.content);
      this.isLoading.set(false);
    });
  }

  updateCreateRoleField(field: keyof CreateIdentityRoleRequest, value: string): void {
    this.createRoleForm.update(current => ({ ...current, [field]: value }));
  }

  createRole(): void {
    this.clearMessages();
    this.identityService.createRole(this.createRoleForm()).subscribe(role => {
      if (!role) {
        this.actionError.set('Failed to create role.');
        return;
      }
      this.roles.update(list => [role, ...list]);
      this.createRoleForm.set({ code: '', name: '', description: '', permissionCodes: [] });
      this.actionMessage.set(`Role "${role.name}" created successfully.`);
    });
  }

  selectRole(role: IdentityRole, panel: RolesPanel): void {
    this.selectedRole.set(role);
    this.activePanel.set(panel);
    this.clearMessages();
    if (panel === 'edit') {
      this.editForm.set({ name: role.name, description: role.description ?? '' });
    }
    if (panel === 'permissions') {
      this.grantCodesInput.set('');
      this.revokeCodesInput.set('');
    }
  }

  saveEdit(): void {
    const role = this.selectedRole();
    if (!role) return;
    this.clearMessages();
    this.identityService.updateRole(role.id, this.editForm()).subscribe(updated => {
      if (!updated) {
        this.actionError.set('Failed to update role.');
        return;
      }
      this.roles.update(list => list.map(r => r.id === updated.id ? updated : r));
      this.selectedRole.set(updated);
      this.actionMessage.set(`Role "${updated.name}" updated.`);
    });
  }

  deleteRole(role: IdentityRole): void {
    this.clearMessages();
    this.identityService.deleteRole(role.id).subscribe(success => {
      if (!success) {
        this.actionError.set(`Failed to delete role "${role.name}".`);
        return;
      }
      this.roles.update(list => list.filter(r => r.id !== role.id));
      this.actionMessage.set(`Role "${role.name}" deleted.`);
      if (this.selectedRole()?.id === role.id) {
        this.selectedRole.set(null);
        this.activePanel.set('create');
      }
    });
  }

  grantPermissions(): void {
    const role = this.selectedRole();
    if (!role) return;
    const codes = this.grantCodesInput().split(',').map(c => c.trim()).filter(Boolean);
    if (!codes.length) return;
    this.clearMessages();
    this.identityService.grantPermissions(role.id, codes).subscribe(success => {
      if (!success) {
        this.actionError.set('Failed to grant permissions.');
        return;
      }
      this.grantCodesInput.set('');
      this.actionMessage.set(`${codes.length} permission(s) granted to "${role.name}".`);
      this.loadRoles();
    });
  }

  revokePermissions(): void {
    const role = this.selectedRole();
    if (!role) return;
    const codes = this.revokeCodesInput().split(',').map(c => c.trim()).filter(Boolean);
    if (!codes.length) return;
    this.clearMessages();
    this.identityService.revokePermissions(role.id, codes).subscribe(success => {
      if (!success) {
        this.actionError.set('Failed to revoke permissions.');
        return;
      }
      this.revokeCodesInput.set('');
      this.actionMessage.set(`${codes.length} permission(s) revoked from "${role.name}".`);
      this.loadRoles();
    });
  }

  showPanel(panel: RolesPanel): void {
    this.activePanel.set(panel);
    this.clearMessages();
  }

  private clearMessages(): void {
    this.actionMessage.set('');
    this.actionError.set('');
  }
}
