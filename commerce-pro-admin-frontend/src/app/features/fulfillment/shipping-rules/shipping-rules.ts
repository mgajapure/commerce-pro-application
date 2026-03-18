import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FulfillmentService } from '../../../core/services/fulfillment/fulfillment.service';
import { ShippingRule, ShippingRuleRequest, Carrier, CarrierRequest } from '../../../core/models/fulfillment/fulfillment.model';
import { AlertService } from '../../../shared/services/alert.service';

@Component({
  selector: 'app-shipping-rules',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './shipping-rules.html',
  styleUrl: './shipping-rules.scss'
})
export class ShippingRules implements OnInit {
  private svc      = inject(FulfillmentService);
  private alertSvc = inject(AlertService);

  carriers     = signal<Carrier[]>([]);
  rules        = signal<ShippingRule[]>([]);
  isLoading    = signal(false);

  activeTab    = signal<'rules' | 'carriers'>('rules');

  // Carrier form
  showCarrierModal = signal(false);
  editCarrier      = signal<Carrier | null>(null);
  carrierForm      = signal<CarrierRequest>({ name: '', code: '', trackingUrlTemplate: '', isDefault: false, notes: '' });
  carrierError     = signal<string | null>(null);
  savingCarrier    = signal(false);

  // Rule form
  showRuleModal    = signal(false);
  editRule         = signal<ShippingRule | null>(null);
  ruleForm         = signal<ShippingRuleRequest>({ name: '', conditionType: 'ALWAYS', shippingMethod: '', isActive: true, priority: 100 });
  ruleError        = signal<string | null>(null);
  savingRule       = signal(false);

  conditionTypes = ['ALWAYS', 'WEIGHT', 'PRICE', 'COUNTRY'];

  activeCarriers = computed(() => this.carriers().filter(c => c.status === 'ACTIVE'));
  activeRules    = computed(() => [...this.rules()].sort((a, b) => a.priority - b.priority));

  ngOnInit() { this.loadAll(); }

  loadAll() {
    this.isLoading.set(true);
    this.svc.getCarriers().subscribe(c => { this.carriers.set(c); this.isLoading.set(false); });
    this.svc.getRules().subscribe(r => this.rules.set(r));
  }

  // ── Carrier actions ────────────────────────────────────────────────────────
  openNewCarrier() {
    this.editCarrier.set(null);
    this.carrierForm.set({ name: '', code: '', trackingUrlTemplate: '', isDefault: false, notes: '' });
    this.carrierError.set(null);
    this.showCarrierModal.set(true);
  }
  openEditCarrier(c: Carrier) {
    this.editCarrier.set(c);
    this.carrierForm.set({ name: c.name, code: c.code, trackingUrlTemplate: c.trackingUrlTemplate ?? '', isDefault: c.isDefault, notes: c.notes ?? '' });
    this.carrierError.set(null);
    this.showCarrierModal.set(true);
  }
  closeCarrierModal() { this.showCarrierModal.set(false); }
  submitCarrier() {
    const f = this.carrierForm();
    if (!f.name || !f.code) { this.carrierError.set('Name and code are required'); return; }
    this.savingCarrier.set(true);
    const ec = this.editCarrier();
    const obs = ec ? this.svc.updateCarrier(ec.id, f) : this.svc.createCarrier(f);
    obs.subscribe({
      next: () => {
        this.savingCarrier.set(false); this.closeCarrierModal();
        this.svc.getCarriers().subscribe(c => this.carriers.set(c));
        this.alertSvc.success(ec ? 'Carrier updated' : 'Carrier created');
      },
      error: err => { this.savingCarrier.set(false); this.carrierError.set(err?.error?.message ?? 'Failed to save carrier'); this.alertSvc.error('Failed to save carrier', err?.error?.message); }
    });
  }
  deleteCarrier(id: string) {
    this.alertSvc.confirm({ title: 'Delete Carrier', message: 'Are you sure you want to delete this carrier?', confirmLabel: 'Delete', danger: true })
      .then(ok => {
        if (!ok) return;
        this.svc.deleteCarrier(id).subscribe({
          next: () => { this.svc.getCarriers().subscribe(c => this.carriers.set(c)); this.alertSvc.success('Carrier deleted'); },
          error: () => this.alertSvc.error('Failed to delete carrier')
        });
      });
  }

  // ── Rule actions ───────────────────────────────────────────────────────────
  openNewRule() {
    this.editRule.set(null);
    this.ruleForm.set({ name: '', conditionType: 'ALWAYS', shippingMethod: '', isActive: true, priority: 100 });
    this.ruleError.set(null);
    this.showRuleModal.set(true);
  }
  openEditRule(r: ShippingRule) {
    this.editRule.set(r);
    this.ruleForm.set({ name: r.name, description: r.description, carrierId: r.carrierId, conditionType: r.conditionType,
      conditionMin: r.conditionMin, conditionMax: r.conditionMax, conditionValue: r.conditionValue,
      shippingMethod: r.shippingMethod, serviceLevel: r.serviceLevel, priority: r.priority, isActive: r.isActive });
    this.ruleError.set(null);
    this.showRuleModal.set(true);
  }
  closeRuleModal() { this.showRuleModal.set(false); }
  submitRule() {
    const f = this.ruleForm();
    if (!f.name || !f.conditionType || !f.shippingMethod) { this.ruleError.set('Name, condition type, and shipping method are required'); return; }
    this.savingRule.set(true);
    const er = this.editRule();
    const obs = er ? this.svc.updateRule(er.id, f) : this.svc.createRule(f);
    obs.subscribe({
      next: () => {
        this.savingRule.set(false); this.closeRuleModal();
        this.svc.getRules().subscribe(r => this.rules.set(r));
        this.alertSvc.success(er ? 'Rule updated' : 'Shipping rule created');
      },
      error: err => { this.savingRule.set(false); this.ruleError.set(err?.error?.message ?? 'Failed to save rule'); this.alertSvc.error('Failed to save rule', err?.error?.message); }
    });
  }
  deleteRule(id: string) {
    this.alertSvc.confirm({ title: 'Delete Shipping Rule', message: 'Are you sure you want to delete this shipping rule?', confirmLabel: 'Delete', danger: true })
      .then(ok => {
        if (!ok) return;
        this.svc.deleteRule(id).subscribe({
          next: () => { this.svc.getRules().subscribe(r => this.rules.set(r)); this.alertSvc.success('Shipping rule deleted'); },
          error: () => this.alertSvc.error('Failed to delete rule')
        });
      });
  }

  updateRuleFormField(field: string, value: any) {
    this.ruleForm.update(f => ({ ...f, [field]: value }));
  }
  updateCarrierFormField(field: string, value: any) {
    this.carrierForm.update(f => ({ ...f, [field]: value }));
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso));
  }
}
