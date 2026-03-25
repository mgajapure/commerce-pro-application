import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentService } from '../../../core/services/payment/payment.service';
import { PaymentGatewayConfigDTO, GatewayProvider, CreateGatewayConfigRequest } from '../../../core/models/payment/payment.model';

@Component({ selector: 'app-gateways', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './gateways.html', styleUrl: './gateways.scss' })
export class Gateways implements OnInit {
  private svc = inject(PaymentService);

  gateways      = signal<PaymentGatewayConfigDTO[]>([]);
  isLoading     = signal(false);
  actionLoading = signal<string | null>(null);
  showCreateModal = signal(false);
  showTestResult  = signal<{ id: string; success: boolean; message: string } | null>(null);
  newGateway    = signal<Partial<CreateGatewayConfigRequest>>({ isTestMode: true });

  showEditModal = signal(false);
  editTarget    = signal<PaymentGatewayConfigDTO | null>(null);
  editForm      = signal<Partial<CreateGatewayConfigRequest>>({});

  providers: GatewayProvider[] = ['STRIPE', 'PAYPAL', 'SQUARE', 'BRAINTREE', 'RAZORPAY', 'MANUAL'];

  providerInfo: Record<GatewayProvider, { color: string; bg: string; description: string }> = {
    STRIPE:    { color: 'text-purple-700', bg: 'bg-purple-50 border-purple-200',   description: 'Industry-leading payment platform' },
    PAYPAL:    { color: 'text-blue-700',   bg: 'bg-blue-50 border-blue-200',       description: 'Trusted global payment network' },
    SQUARE:    { color: 'text-gray-700',   bg: 'bg-gray-50 border-gray-200',       description: 'Unified payments and commerce' },
    BRAINTREE: { color: 'text-teal-700',   bg: 'bg-teal-50 border-teal-200',       description: 'Full-stack payment platform by PayPal' },
    RAZORPAY:  { color: 'text-blue-800',   bg: 'bg-blue-50 border-blue-200',       description: 'Leading India payment gateway' },
    MANUAL:    { color: 'text-gray-600',   bg: 'bg-gray-50 border-gray-200',       description: 'Manual / Cash / Internal payments stub' },
  };

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    this.svc.getGateways().subscribe(gs => { this.gateways.set(gs); this.isLoading.set(false); });
  }

  create(): void {
    const f = this.newGateway();
    if (!f.gatewayProvider || !f.displayName) return;
    this.actionLoading.set('create');
    this.svc.createGatewayConfig(f as CreateGatewayConfigRequest).subscribe(() => {
      this.actionLoading.set(null); this.showCreateModal.set(false); this.newGateway.set({ isTestMode: true }); this.load();
    });
  }

  activate(id: string): void {
    this.actionLoading.set(id);
    this.svc.activateGateway(id).subscribe(() => { this.actionLoading.set(null); this.load(); });
  }

  setDefault(id: string): void {
    this.actionLoading.set(id + '-default');
    this.svc.setDefaultGateway(id).subscribe(() => { this.actionLoading.set(null); this.load(); });
  }

  patchNew(patch: any): void { this.newGateway.update(v => ({ ...v, ...patch })); }

  openEdit(gw: PaymentGatewayConfigDTO): void {
    this.editTarget.set(gw);
    this.editForm.set({ displayName: gw.displayName, webhookUrl: gw.webhookUrl, supportedCurrencies: gw.supportedCurrencies, isTestMode: gw.isTestMode });
    this.showEditModal.set(true);
  }

  patchEdit(patch: any): void { this.editForm.update(v => ({ ...v, ...patch })); }

  saveEdit(): void {
    const target = this.editTarget();
    if (!target) return;
    this.actionLoading.set('edit');
    const form = this.editForm();
    const payload: any = { displayName: form.displayName, webhookUrl: form.webhookUrl, supportedCurrencies: form.supportedCurrencies, isTestMode: form.isTestMode };
    if (form.apiKey) payload.apiKey = form.apiKey;
    if (form.apiSecret) payload.apiSecret = form.apiSecret;
    this.svc.updateGatewayConfig(target.id, payload).subscribe({
      next: () => { this.actionLoading.set(null); this.showEditModal.set(false); this.load(); },
      error: () => { this.actionLoading.set(null); }
    });
  }

  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
}
