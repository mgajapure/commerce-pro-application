import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { PaymentService } from '../../../core/services/payment/payment.service';
import { PaymentMethodDTO } from '../../../core/models/payment/payment.model';

@Component({ selector: 'app-methods', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './methods.html', styleUrl: './methods.scss' })
export class Methods implements OnInit {
  private svc = inject(PaymentService);
  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  methods       = signal<PaymentMethodDTO[]>([]);
  isLoading     = signal(false);
  actionLoading = signal<string | null>(null);
  searchQuery   = signal('');
  customerId    = signal('');
  error         = signal<string | null>(null);
  removeTarget  = signal<string | null>(null);
  showRemoveConfirm = signal(false);

  ngOnInit(): void {
    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(v => { if (v.trim().length >= 3) this.loadByCustomer(v.trim()); });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  onSearchInput(v: string): void { this.customerId.set(v); this.searchSubject.next(v); if (!v) this.methods.set([]); }

  loadByCustomer(id: string): void {
    this.isLoading.set(true); this.error.set(null);
    this.svc.getPaymentMethodsByCustomer(id).subscribe(ms => {
      this.methods.set(ms); this.isLoading.set(false);
    });
  }

  setDefault(id: string): void {
    this.actionLoading.set(id);
    this.svc.setDefaultPaymentMethod(id).subscribe(() => {
      this.actionLoading.set(null);
      if (this.customerId()) this.loadByCustomer(this.customerId());
    });
  }

  openRemoveConfirm(id: string): void { this.removeTarget.set(id); this.showRemoveConfirm.set(true); }

  confirmRemove(): void {
    const id = this.removeTarget();
    if (!id) return;
    this.actionLoading.set(id);
    this.svc.removePaymentMethod(id).subscribe(() => {
      this.actionLoading.set(null); this.showRemoveConfirm.set(false); this.removeTarget.set(null);
      if (this.customerId()) this.loadByCustomer(this.customerId());
    });
  }

  getMethodIcon(type: string): string {
    const m: Record<string, string> = {
      CREDIT_CARD: 'credit-card-2-front', DEBIT_CARD: 'credit-card', PAYPAL: 'paypal',
      APPLE_PAY: 'apple', GOOGLE_PAY: 'google', BANK_TRANSFER: 'bank', CRYPTO: 'currency-bitcoin',
      STORE_CREDIT: 'gift-card', MANUAL: 'cash-coin'
    };
    return m[type] ?? 'credit-card';
  }

  getBrandColor(brand: string): string {
    return brand === 'VISA' ? 'text-blue-700' : brand === 'MASTERCARD' ? 'text-red-600' : brand === 'AMEX' ? 'text-green-700' : 'text-gray-600';
  }

  isExpired(m: PaymentMethodDTO): boolean {
    if (!m.cardExpYear || !m.cardExpMonth) return false;
    const expiry = new Date(parseInt(m.cardExpYear), parseInt(m.cardExpMonth) - 1);
    return expiry < new Date();
  }

  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
}
