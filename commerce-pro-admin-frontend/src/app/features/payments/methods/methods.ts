import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { PaymentService } from '../../../core/services/payment/payment.service';
import { PaymentMethodDTO } from '../../../core/models/payment/payment.model';
import { CustomerApiService } from '../../../core/services/customers/customers.service';
import { CustomerSummary } from '../../../core/models/customers/customers.model';

@Component({ selector: 'app-methods', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './methods.html', styleUrl: './methods.scss' })
export class Methods implements OnInit {
  private svc = inject(PaymentService);
  private customerSvc = inject(CustomerApiService);
  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  methods           = signal<PaymentMethodDTO[]>([]);
  isLoading         = signal(false);
  customersLoading  = signal(false);
  actionLoading     = signal<string | null>(null);
  searchQuery       = signal('');
  customerId        = signal('');
  error             = signal<string | null>(null);
  removeTarget      = signal<string | null>(null);
  showRemoveConfirm = signal(false);

  // Customer list state
  customers            = signal<CustomerSummary[]>([]);
  totalCustomers       = signal(0);
  totalCustomerPages   = signal(0);
  customerPage         = signal(1);
  showingMethods       = signal(false);
  selectedCustomerName = signal('');

  ngOnInit(): void {
    this.loadCustomers(1);
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  loadCustomers(page: number): void {
    this.customersLoading.set(true);
    this.customerPage.set(page);
    this.customerSvc.getCustomers({}, { page: page - 1, size: 20 }).subscribe(res => {
      this.customers.set(res.content);
      this.totalCustomers.set(res.totalElements);
      this.totalCustomerPages.set(res.totalPages);
      this.customersLoading.set(false);
    });
  }

  selectCustomer(id: string, name: string): void {
    this.customerId.set(id);
    this.selectedCustomerName.set(name);
    this.showingMethods.set(true);
    this.loadByCustomer(id);
  }

  backToList(): void {
    this.showingMethods.set(false);
    this.customerId.set('');
    this.selectedCustomerName.set('');
    this.methods.set([]);
  }

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

  customerPageRange(): number[] {
    const total = this.totalCustomerPages();
    const current = this.customerPage();
    const pages: number[] = [];
    const start = Math.max(1, current - 2);
    const end = Math.min(total, current + 2);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }
}
