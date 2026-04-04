import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SystemService, ApiKey, ApiUsageStats } from '../system.service';

@Component({
  selector: 'app-api-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './api-management.html',
  styles: [`:host { display: block; }`]
})
export class ApiManagement implements OnInit {
  private svc = inject(SystemService);

  apiKeys = signal<ApiKey[]>([]);
  usageStats = signal<ApiUsageStats>({ totalRequests: 0, requestsToday: 0, errorRate: 0, activeKeys: 0 });
  isLoading = signal(true);
  message = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  // Create form
  showCreateForm = signal(false);
  formName = signal('');
  formScopes = signal<Record<string, boolean>>({
    read_products: false,
    write_products: false,
    read_orders: false,
    write_orders: false,
    read_customers: false,
    manage_settings: false
  });
  isCreating = signal(false);

  // Newly created key (shown once)
  newlyCreatedKey = signal<string | null>(null);
  copiedKey = signal(false);

  allScopes = [
    { key: 'read_products', label: 'Read Products' },
    { key: 'write_products', label: 'Write Products' },
    { key: 'read_orders', label: 'Read Orders' },
    { key: 'write_orders', label: 'Write Orders' },
    { key: 'read_customers', label: 'Read Customers' },
    { key: 'manage_settings', label: 'Manage Settings' }
  ];

  activeKeys = computed(() => this.apiKeys().filter(k => k.status === 'active'));
  revokedKeys = computed(() => this.apiKeys().filter(k => k.status === 'revoked'));

  ngOnInit() {
    this.load();
  }

  load() {
    this.isLoading.set(true);
    this.svc.getApiKeys().subscribe(keys => {
      this.apiKeys.set(keys);
      this.isLoading.set(false);
    });
    this.svc.getApiUsageStats().subscribe(stats => this.usageStats.set(stats));
  }

  toggleScope(scope: string) {
    this.formScopes.update(s => ({ ...s, [scope]: !s[scope] }));
  }

  openCreate() {
    this.showCreateForm.set(true);
    this.formName.set('');
    this.formScopes.set({
      read_products: false, write_products: false,
      read_orders: false, write_orders: false,
      read_customers: false, manage_settings: false
    });
    this.newlyCreatedKey.set(null);
  }

  closeCreate() {
    this.showCreateForm.set(false);
    this.newlyCreatedKey.set(null);
  }

  create() {
    const scopes = Object.entries(this.formScopes())
      .filter(([, v]) => v)
      .map(([k]) => k);

    if (!this.formName() || scopes.length === 0) {
      this.flash('error', 'Name and at least one scope are required');
      return;
    }

    this.isCreating.set(true);
    this.svc.createApiKey({ name: this.formName(), scopes }).subscribe(result => {
      this.isCreating.set(false);
      if (result) {
        this.newlyCreatedKey.set(result.fullKey || null);
        this.flash('success', 'API key created');
        this.load();
      } else {
        this.flash('error', 'Failed to create API key');
      }
    });
  }

  copyKey() {
    const key = this.newlyCreatedKey();
    if (key) {
      navigator.clipboard.writeText(key);
      this.copiedKey.set(true);
      setTimeout(() => this.copiedKey.set(false), 2000);
    }
  }

  revoke(key: ApiKey) {
    if (!confirm(`Revoke API key "${key.name}"? This cannot be undone.`)) return;
    // Optimistic update
    this.apiKeys.update(list => list.map(k => k.id === key.id ? { ...k, status: 'revoked' as const } : k));
    this.svc.revokeApiKey(key.id).subscribe(ok => {
      if (ok) {
        this.flash('success', 'API key revoked');
      } else {
        this.flash('error', 'Failed to revoke API key');
        this.load();
      }
    });
  }

  private flash(type: 'success' | 'error', text: string) {
    this.message.set({ type, text });
    setTimeout(() => this.message.set(null), 4000);
  }
}
