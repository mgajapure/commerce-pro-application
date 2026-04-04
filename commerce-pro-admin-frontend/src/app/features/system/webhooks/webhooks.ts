import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SystemService, Webhook } from '../system.service';

@Component({
  selector: 'app-system-webhooks',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './webhooks.html',
  styles: [`:host { display: block; }`]
})
export class SystemWebhooks implements OnInit {
  private svc = inject(SystemService);

  webhooks = signal<Webhook[]>([]);
  isLoading = signal(true);
  message = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  showForm = signal(false);
  editingId = signal<string | null>(null);

  formUrl = signal('');
  formEvents = signal<Record<string, boolean>>({});
  formActive = signal(true);
  isSaving = signal(false);
  testingId = signal<string | null>(null);

  availableEvents = [
    'order.created', 'order.updated', 'order.fulfilled', 'order.cancelled',
    'product.created', 'product.updated', 'product.deleted',
    'customer.created', 'customer.updated',
    'payment.completed', 'refund.processed', 'inventory.low'
  ];

  ngOnInit() { this.load(); }

  load() {
    this.isLoading.set(true);
    this.svc.getWebhooks().subscribe(wh => {
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
    this.showForm.set(true);
  }

  openEdit(wh: Webhook) {
    this.editingId.set(wh.id);
    this.formUrl.set(wh.url);
    this.formActive.set(wh.active);
    const ev: Record<string, boolean> = {};
    this.availableEvents.forEach(e => ev[e] = wh.events.includes(e));
    this.formEvents.set(ev);
    this.showForm.set(true);
  }

  closeForm() { this.showForm.set(false); }

  toggleEvent(event: string) {
    this.formEvents.update(e => ({ ...e, [event]: !e[event] }));
  }

  save() {
    const events = Object.entries(this.formEvents()).filter(([, v]) => v).map(([k]) => k);
    if (!this.formUrl() || events.length === 0) {
      this.flash('error', 'URL and at least one event are required');
      return;
    }

    this.isSaving.set(true);
    const data = { url: this.formUrl(), events, active: this.formActive() };

    const obs = this.editingId()
      ? this.svc.updateWebhook(this.editingId()!, data)
      : this.svc.createWebhook(data);

    obs.subscribe(result => {
      this.isSaving.set(false);
      if (result) {
        this.flash('success', this.editingId() ? 'Webhook updated' : 'Webhook created');
        this.showForm.set(false);
        this.load();
      } else {
        this.flash('error', 'Operation failed');
      }
    });
  }

  deleteWebhook(wh: Webhook) {
    if (!confirm(`Delete webhook for ${wh.url}?`)) return;
    this.webhooks.update(list => list.filter(w => w.id !== wh.id));
    this.svc.deleteWebhook(wh.id).subscribe(ok => {
      if (ok) this.flash('success', 'Webhook deleted');
      else { this.flash('error', 'Failed to delete'); this.load(); }
    });
  }

  testWebhook(wh: Webhook) {
    this.testingId.set(wh.id);
    this.svc.testWebhook(wh.id).subscribe(result => {
      this.testingId.set(null);
      if (result.success) this.flash('success', `Test successful (${result.statusCode})`);
      else this.flash('error', result.message);
    });
  }

  private flash(type: 'success' | 'error', text: string) {
    this.message.set({ type, text });
    setTimeout(() => this.message.set(null), 4000);
  }
}
