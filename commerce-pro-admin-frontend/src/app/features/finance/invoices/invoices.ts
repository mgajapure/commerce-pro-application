import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { CustomerInvoiceSummaryDTO, CustomerInvoiceDTO, InvoiceStatus, CreateCustomerInvoiceRequest, RecordPaymentRequest } from '../../../core/models/finance/finance.model';

@Component({ selector: 'app-invoices', standalone: true, imports: [CommonModule, FormsModule, RouterModule], templateUrl: './invoices.html', styleUrl: './invoices.scss' })
export class Invoices implements OnInit {
  private svc = inject(FinanceService);

  invoices      = signal<CustomerInvoiceSummaryDTO[]>([]);
  detail        = signal<CustomerInvoiceDTO | null>(null);
  isLoading     = signal(false);
  actionLoading = signal<string | null>(null);
  totalElements = signal(0);
  totalPages    = signal(0);
  currentPage   = signal(1);
  filterStatus  = signal<InvoiceStatus | ''>('');
  filterSearch  = signal('');
  filterOverdue = signal(false);
  showDetail    = signal(false);
  showCreateModal = signal(false);
  showPayModal  = signal<string | null>(null);
  showVoidModal = signal<string | null>(null);
  payAmount     = signal('');
  voidReason    = signal('');

  newInvoice = signal<Partial<CreateCustomerInvoiceRequest>>({ paymentTerms: 'Net 30', currency: 'USD', items: [] });

  statusTabs: { label: string; value: InvoiceStatus | '' }[] = [
    { label: 'All', value: '' }, { label: 'Draft', value: 'DRAFT' }, { label: 'Sent', value: 'SENT' },
    { label: 'Overdue', value: 'OVERDUE' }, { label: 'Partially Paid', value: 'PARTIALLY_PAID' },
    { label: 'Paid', value: 'PAID' }, { label: 'Void', value: 'VOID' },
  ];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    this.svc.getCustomerInvoices({
      status: this.filterStatus() as InvoiceStatus || undefined,
      search: this.filterSearch() || undefined,
      overdue: this.filterOverdue() || undefined,
    }, { page: this.currentPage() - 1, size: 20, sort: 'createdAt', direction: 'desc' }).subscribe(p => {
      this.invoices.set(p.content); this.totalElements.set(p.totalElements); this.totalPages.set(p.totalPages); this.isLoading.set(false);
    });
  }

  setTab(v: InvoiceStatus | ''): void { this.filterStatus.set(v); this.filterOverdue.set(v === 'OVERDUE'); this.currentPage.set(1); this.load(); }

  openDetail(id: string): void {
    this.svc.getCustomerInvoiceById(id).subscribe(d => { this.detail.set(d); this.showDetail.set(true); });
  }

  send(id: string): void {
    this.actionLoading.set(id);
    this.svc.sendInvoice(id).subscribe(() => { this.actionLoading.set(null); this.load(); this.showDetail.set(false); });
  }

  submitPayment(): void {
    const id = this.showPayModal(); if (!id) return;
    const req: RecordPaymentRequest = { amount: parseFloat(this.payAmount()), paymentDate: new Date().toISOString().split('T')[0] };
    this.actionLoading.set(id);
    this.svc.recordInvoicePayment(id, req).subscribe(() => { this.actionLoading.set(null); this.showPayModal.set(null); this.load(); this.showDetail.set(false); });
  }

  submitVoid(): void {
    const id = this.showVoidModal(); if (!id) return;
    this.actionLoading.set(id);
    this.svc.voidInvoice(id, this.voidReason() || 'Voided').subscribe(() => { this.actionLoading.set(null); this.showVoidModal.set(null); this.load(); this.showDetail.set(false); });
  }

  goToPage(p: number): void { this.currentPage.set(p); this.load(); }
  prevPage(): void { if (this.currentPage() > 1) { this.currentPage.update(p => p - 1); this.load(); } }
  nextPage(): void { if (this.currentPage() < this.totalPages()) { this.currentPage.update(p => p + 1); this.load(); } }

  getStatusCfg(s: string): { badge: string; dot: string; label: string } {
    const m: Record<string, any> = {
      DRAFT:         { badge: 'bg-gray-100 text-gray-700',     dot: 'bg-gray-400',    label: 'Draft' },
      SENT:          { badge: 'bg-blue-100 text-blue-800',     dot: 'bg-blue-500',    label: 'Sent' },
      PARTIALLY_PAID:{ badge: 'bg-yellow-100 text-yellow-800', dot: 'bg-yellow-500',  label: 'Partial' },
      PAID:          { badge: 'bg-green-100 text-green-800',   dot: 'bg-green-500',   label: 'Paid' },
      OVERDUE:       { badge: 'bg-red-100 text-red-800',       dot: 'bg-red-500',     label: 'Overdue' },
      VOID:          { badge: 'bg-gray-100 text-gray-500',     dot: 'bg-gray-300',    label: 'Void' },
      WRITTEN_OFF:   { badge: 'bg-slate-100 text-slate-600',   dot: 'bg-slate-400',   label: 'Written Off' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', dot: 'bg-gray-400', label: s };
  }

  fmt(n: number): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
  daysUntil(iso: string): number { return Math.ceil((new Date(iso).getTime() - Date.now()) / 86400000); }
}
