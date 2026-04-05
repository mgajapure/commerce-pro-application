import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SettingsService } from '../settings.service';
import { AlertService } from '../../../shared/services/alert.service';
import { HelpSidebar, HelpSection } from '../../../shared/components/help-sidebar/help-sidebar';
import { TooltipLabel } from '../../../shared/components/tooltip-label/tooltip-label';

@Component({
  selector: 'app-checkout-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, HelpSidebar, TooltipLabel],
  templateUrl: './checkout.html',
  styles: [`:host { display: block; }`]
})
export class CheckoutSettings implements OnInit, OnDestroy {
  private svc = inject(SettingsService);
  private alertSvc = inject(AlertService);
  private destroy$ = new Subject<void>();

  loading = signal(true);
  saving = signal(false);

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

  showHelp = signal(false);
  helpSections: HelpSection[] = [
    { title: 'General Options', content: 'Configure guest checkout, order limits, confirmation emails, and terms requirements.' },
    { title: 'Payment Methods', content: 'Enable or disable payment options available to customers during checkout.' },
    { title: 'Shipping Methods', content: 'Choose which shipping methods are available for customer orders.' }
  ];

  allPaymentMethods = [
    { key: 'credit_card', label: 'Credit Card', icon: 'bi-credit-card' },
    { key: 'paypal', label: 'PayPal', icon: 'bi-paypal' },
    { key: 'bank_transfer', label: 'Bank Transfer', icon: 'bi-bank' },
    { key: 'apple_pay', label: 'Apple Pay', icon: 'bi-apple' },
    { key: 'google_pay', label: 'Google Pay', icon: 'bi-google' }
  ];

  allShippingMethods = [
    { key: 'standard', label: 'Standard Shipping', icon: 'bi-truck' },
    { key: 'express', label: 'Express Shipping', icon: 'bi-lightning' },
    { key: 'overnight', label: 'Overnight Shipping', icon: 'bi-airplane' },
    { key: 'pickup', label: 'In-Store Pickup', icon: 'bi-shop' }
  ];

  ngOnInit() {
    this.svc.getCheckoutSettings().pipe(takeUntil(this.destroy$)).subscribe(data => {
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

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  togglePayment(key: string) { this.paymentMethods.update(m => ({ ...m, [key]: !m[key] })); }
  toggleShipping(key: string) { this.shippingMethods.update(m => ({ ...m, [key]: !m[key] })); }

  save() {
    this.saving.set(true);
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

    this.svc.updateCheckoutSettings(payload).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => { this.saving.set(false); this.alertSvc.success('Settings Saved', 'Checkout settings updated successfully'); },
      error: () => { this.saving.set(false); this.alertSvc.error('Failed', 'Could not save checkout settings'); }
    });
  }
}
