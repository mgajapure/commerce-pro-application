import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CreateIdentityPermissionRequest, IdentityPermission } from '../../../core/models/identity';
import { IdentityService } from '../../../core/services/identity/identity.service';

@Component({
  selector: 'app-identity-permissions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './permissions.html',
  styleUrl: './permissions.scss'
})
export class IdentityPermissions implements OnInit {
  private readonly identityService = inject(IdentityService);

  readonly permissions = signal<IdentityPermission[]>([]);
  readonly isLoading = signal(false);
  readonly categoryFilter = signal('');
  readonly searchQuery = signal('');
  readonly actionMessage = signal('');
  readonly actionError = signal('');

  readonly createPermissionForm = signal<CreateIdentityPermissionRequest>({
    code: '',
    name: '',
    description: '',
    category: '',
    riskLevel: 1,
    requiresApproval: false,
    applicableScopes: []
  });

  readonly totalPermissions = computed(() => this.permissions().length);

  readonly categories = computed(() => {
    const cats = new Set(this.permissions().map(p => p.category ?? 'UNCATEGORIZED'));
    return Array.from(cats).sort();
  });

  readonly filteredPermissions = computed(() => {
    let list = this.permissions();
    const cat = this.categoryFilter();
    const q = this.searchQuery().toLowerCase().trim();
    if (cat) list = list.filter(p => (p.category ?? 'UNCATEGORIZED') === cat);
    if (q) list = list.filter(p => p.code.toLowerCase().includes(q) || p.name.toLowerCase().includes(q));
    return list;
  });

  ngOnInit(): void {
    this.loadPermissions();
  }

  loadPermissions(): void {
    this.isLoading.set(true);
    this.identityService.getPermissions(0, 200).subscribe(page => {
      this.permissions.set(page.content);
      this.isLoading.set(false);
    });
  }

  updateCreatePermissionField(field: keyof CreateIdentityPermissionRequest, value: string | number | boolean): void {
    this.createPermissionForm.update(current => ({ ...current, [field]: value }));
  }

  createPermission(): void {
    this.clearMessages();
    this.identityService.createPermission(this.createPermissionForm()).subscribe(permission => {
      if (!permission) {
        this.actionError.set('Failed to create permission.');
        return;
      }
      this.permissions.update(list => [permission, ...list]);
      this.createPermissionForm.set({ code: '', name: '', description: '', category: '', riskLevel: 1, requiresApproval: false, applicableScopes: [] });
      this.actionMessage.set(`Permission "${permission.code}" created.`);
    });
  }

  deletePermission(permission: IdentityPermission): void {
    this.clearMessages();
    this.identityService.deletePermission(permission.code).subscribe(success => {
      if (!success) {
        this.actionError.set(`Failed to delete permission "${permission.code}".`);
        return;
      }
      this.permissions.update(list => list.filter(p => p.code !== permission.code));
      this.actionMessage.set(`Permission "${permission.code}" deleted.`);
    });
  }

  riskLabel(level: number): string {
    return ['', 'Very Low', 'Low', 'Medium', 'High', 'Critical'][level] ?? `Level ${level}`;
  }

  riskClass(level: number): string {
    const map: Record<number, string> = {
      1: 'bg-green-100 text-green-700',
      2: 'bg-teal-100 text-teal-700',
      3: 'bg-yellow-100 text-yellow-700',
      4: 'bg-orange-100 text-orange-700',
      5: 'bg-red-100 text-red-700'
    };
    return map[level] ?? 'bg-gray-100 text-gray-600';
  }

  private clearMessages(): void {
    this.actionMessage.set('');
    this.actionError.set('');
  }
}
