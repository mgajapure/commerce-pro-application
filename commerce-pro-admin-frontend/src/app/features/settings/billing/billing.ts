import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SettingsService } from '../settings.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';

@Component({
  selector: 'app-billing-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar],
  templateUrl: './billing.html',
  styles: [`:host { display: block; }`]
})
export class BillingSettings implements OnInit, OnDestroy {
  private svc = inject(SettingsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  loading = signal(true);
  plan = signal('Enterprise');
  price = signal(299);
  billingCycle = signal('monthly');
  nextBillingDate = signal('2026-05-01');
  status = signal('active');
  usage = signal<any>({ ordersThisMonth: 0, products: 0, storageUsedMb: 0, apiCalls: 0 });
  paymentMethod = signal<any>({ type: 'Visa', last4: '4242', expiry: '12/27' });
  history = signal<any[]>([]);

  currentPage = signal(1);
  pageSize = signal(10);

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'Current Plan', content: 'View your active subscription plan, pricing, and next billing date.' },
    { title: 'Usage', content: 'Monitor your current resource usage including orders, products, storage, and API calls.' },
    { title: 'Payment Method', content: 'Manage your payment method on file for automatic billing.' },
    { title: 'Billing History', content: 'Review past invoices and payment history with download options.' }
  ];

  statusConfig = computed(() => {
    const s = this.status();
    const configs: Record<string, { bg: string; text: string; dot: string }> = {
      active: { bg: 'bg-green-100', text: 'text-green-800', dot: 'bg-green-500' },
      past_due: { bg: 'bg-yellow-100', text: 'text-yellow-800', dot: 'bg-yellow-500' },
      cancelled: { bg: 'bg-red-100', text: 'text-red-800', dot: 'bg-red-500' },
    };
    return configs[s] || configs['active'];
  });

  paginatedHistory = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize();
    return this.history().slice(start, start + this.pageSize());
  });

  totalPages = computed(() => Math.ceil(this.history().length / this.pageSize()) || 1);

  storageDisplay = computed(() => {
    const mb = this.usage().storageUsedMb ?? 0;
    return mb >= 1024 ? (mb / 1024).toFixed(1) + ' GB' : mb + ' MB';
  });

  ngOnInit() {
    this.svc.getBillingInfo().pipe(takeUntil(this.destroy$)).subscribe(data => {
      if (data) {
        this.plan.set(data.plan ?? 'Enterprise');
        this.price.set(data.price ?? 299);
        this.billingCycle.set(data.billingCycle ?? 'monthly');
        this.nextBillingDate.set(data.nextBillingDate ?? '');
        this.status.set(data.status ?? 'active');
        if (data.usage) this.usage.set(data.usage);
        if (data.paymentMethod) this.paymentMethod.set(data.paymentMethod);
        if (data.history) this.history.set(data.history);
      }
      this.loading.set(false);
    });
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  changePlan() {
    this.alertSvc.warning('Coming Soon', 'Plan upgrade/downgrade will be available soon');
  }

  updatePayment() {
    this.alertSvc.warning('Coming Soon', 'Payment method update will be available soon');
  }

  downloadInvoice(item: any) {
    this.alertSvc.success('Download Started', `Invoice for ${item.date} is being downloaded`);
  }

  goToPage(page: number) {
    if (page >= 1 && page <= this.totalPages()) this.currentPage.set(page);
  }
}
