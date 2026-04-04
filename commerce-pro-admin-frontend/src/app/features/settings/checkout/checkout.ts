import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SettingsService } from '../settings.service';

@Component({
  selector: 'app-checkout-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './checkout.html',
  styles: [`:host { display: block; }`]
})
export class CheckoutSettings implements OnInit {
  private svc = inject(SettingsService);

  loading = signal(true);
  saving = signal(false);
  success = signal(false);
  error = signal<string | null>(null);

  guestCheckoutEnabled = signal(true);
  minOrderAmount = signal(0);
  maxOrderAmount = signal(50000);
  orderConfirmationEmail = signal(true);
  termsRequired = signal(true);
  orderNotesEnabled = signal(true);

  paymentMethods = signal<Record<string, boolean>>({
    credit_card: true, paypal: true, bank_transfer: false, apple_pay: false, google_pay: false
  });

  shippingMethods = signal<Record<string, boolean>>({
    standard: true, express: true, overnight: false, pickup: false
  });

  allPaymentMethods = [
    { key: 'credit_card', label: 'Credit Card' },
    { key: 'paypal', label: 'PayPal' },
    { key: 'bank_transfer', label: 'Bank Transfer' },
    { key: 'apple_pay', label: 'Apple Pay' },
    { key: 'google_pay', label: 'Google Pay' }
  ];

  allShippingMethods = [
    { key: 'standard', label: 'Standard Shipping' },
    { key: 'express', label: 'Express Shipping' },
    { key: 'overnight', label: 'Overnight Shipping' },
    { key: 'pickup', label: 'In-Store Pickup' }
  ];

  ngOnInit() {
    this.svc.getCheckoutSettings().subscribe(data => {
      if (data) {
        this.guestCheckoutEnabled.set(data.guestCheckoutEnabled ?? true);
        this.minOrderAmount.set(data.minOrderAmount ?? 0);
        this.maxOrderAmount.set(data.maxOrderAmount ?? 50000);
        this.orderConfirmationEmail.set(data.orderConfirmationEmail ?? true);
        this.termsRequired.set(data.termsRequired ?? true);
        this.orderNotesEnabled.set(data.orderNotesEnabled ?? true);
        if (data.paymentMethods) {
          const pm: Record<string, boolean> = {};
          this.allPaymentMethods.forEach(m => pm[m.key] = (data.paymentMethods as string[]).includes(m.key));
          this.paymentMethods.set(pm);
        }
        if (data.shippingMethods) {
          const sm: Record<string, boolean> = {};
          this.allShippingMethods.forEach(m => sm[m.key] = (data.shippingMethods as string[]).includes(m.key));
          this.shippingMethods.set(sm);
        }
      }
      this.loading.set(false);
    });
  }

  togglePayment(key: string) { this.paymentMethods.update(m => ({ ...m, [key]: !m[key] })); }
  toggleShipping(key: string) { this.shippingMethods.update(m => ({ ...m, [key]: !m[key] })); }

  save() {
    this.saving.set(true);
    this.success.set(false);
    this.error.set(null);

    const payload = {
      guestCheckoutEnabled: this.guestCheckoutEnabled(),
      minOrderAmount: this.minOrderAmount(),
      maxOrderAmount: this.maxOrderAmount(),
      orderConfirmationEmail: this.orderConfirmationEmail(),
      termsRequired: this.termsRequired(),
      orderNotesEnabled: this.orderNotesEnabled(),
      paymentMethods: Object.entries(this.paymentMethods()).filter(([, v]) => v).map(([k]) => k),
      shippingMethods: Object.entries(this.shippingMethods()).filter(([, v]) => v).map(([k]) => k),
    };

    this.svc.updateCheckoutSettings(payload).subscribe({
      next: () => { this.saving.set(false); this.success.set(true); setTimeout(() => this.success.set(false), 3000); },
      error: () => { this.saving.set(false); this.error.set('Failed to save checkout settings.'); }
    });
  }
}
