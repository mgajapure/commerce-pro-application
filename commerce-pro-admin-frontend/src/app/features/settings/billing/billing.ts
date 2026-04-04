import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../settings.service';

@Component({
  selector: 'app-billing-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './billing.html',
  styles: [`:host { display: block; }`]
})
export class BillingSettings implements OnInit {
  private svc = inject(SettingsService);

  loading = signal(true);
  plan = signal('Enterprise');
  price = signal(299);
  billingCycle = signal('monthly');
  nextBillingDate = signal('2026-05-01');
  status = signal('active');
  usage = signal<any>({ ordersThisMonth: 0, products: 0, storageUsedMb: 0, apiCalls: 0 });
  paymentMethod = signal<any>({ type: 'Visa', last4: '4242', expiry: '12/27' });
  history = signal<any[]>([]);

  ngOnInit() {
    this.svc.getBillingInfo().subscribe(data => {
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
}
