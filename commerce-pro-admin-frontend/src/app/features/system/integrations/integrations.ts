import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SystemService, Integration } from '../system.service';

@Component({
  selector: 'app-system-integrations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './integrations.html',
  styles: [`:host { display: block; }`]
})
export class SystemIntegrations implements OnInit {
  private svc = inject(SystemService);

  integrations = signal<Integration[]>([]);
  isLoading = signal(true);
  message = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  // Filters
  searchQuery = signal('');
  filterType = signal('');
  integrationTypes: Integration['type'][] = ['Payment', 'Shipping', 'CRM', 'ERP', 'Email', 'Analytics'];

  // Panel state
  showPanel = signal(false);
  editingIntegration = signal<Integration | null>(null);
  formName = signal('');
  formType = signal<Integration['type']>('Payment');
  formConfig = signal('{}');
  formCredentials = signal('{}');
  isSaving = signal(false);

  // Test state
  testingId = signal<string | null>(null);
  testResult = signal<{ id: string; success: boolean; message: string } | null>(null);

  filtered = computed(() => {
    let list = this.integrations();
    const q = this.searchQuery().toLowerCase();
    const t = this.filterType();
    if (q) list = list.filter(i => i.name.toLowerCase().includes(q));
    if (t) list = list.filter(i => i.type === t);
    return list;
  });

  ngOnInit() {
    this.load();
  }

  load() {
    this.isLoading.set(true);
    this.svc.getIntegrations().subscribe(data => {
      this.integrations.set(data);
      this.isLoading.set(false);
    });
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

  closePanel() {
    this.showPanel.set(false);
  }

  save() {
    this.isSaving.set(true);
    let config: Record<string, any> = {};
    let credentials: Record<string, string> = {};
    try {
      config = JSON.parse(this.formConfig());
      credentials = JSON.parse(this.formCredentials());
    } catch {
      this.flash('error', 'Invalid JSON in configuration or credentials');
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

    op.subscribe(result => {
      this.isSaving.set(false);
      if (result) {
        this.flash('success', editing ? 'Integration updated' : 'Integration created');
        this.closePanel();
        this.load();
      } else {
        this.flash('error', 'Failed to save integration');
      }
    });
  }

  delete(item: Integration) {
    if (!confirm(`Delete integration "${item.name}"?`)) return;
    // Optimistic removal
    this.integrations.update(list => list.filter(i => i.id !== item.id));
    this.svc.deleteIntegration(item.id).subscribe(ok => {
      if (ok) {
        this.flash('success', 'Integration deleted');
      } else {
        this.flash('error', 'Failed to delete integration');
        this.load();
      }
    });
  }

  testConnection(item: Integration) {
    this.testingId.set(item.id);
    this.testResult.set(null);
    this.svc.testIntegration(item.id).subscribe(result => {
      this.testingId.set(null);
      this.testResult.set({ id: item.id, ...result });
    });
  }

  getStatusColor(status: string): string {
    const m: Record<string, string> = {
      active: 'bg-green-100 text-green-700',
      inactive: 'bg-gray-100 text-gray-600',
      error: 'bg-red-100 text-red-700'
    };
    return m[status] || 'bg-gray-100 text-gray-600';
  }

  getStatusDot(status: string): string {
    const m: Record<string, string> = { active: 'bg-green-500', inactive: 'bg-gray-400', error: 'bg-red-500' };
    return m[status] || 'bg-gray-400';
  }

  private flash(type: 'success' | 'error', text: string) {
    this.message.set({ type, text });
    setTimeout(() => this.message.set(null), 4000);
  }
}
