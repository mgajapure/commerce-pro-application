import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { VendorInvoiceSummaryDTO, VendorInvoiceDTO, VendorInvoiceStatus, VendorSummaryDTO } from '../../../core/models/finance/finance.model';

@Component({ selector: 'app-vendor-invoices', standalone: true, imports: [CommonModule, FormsModule, RouterModule], templateUrl: './vendor-invoices.html', styleUrl: './vendor-invoices.scss' })
export class VendorInvoices implements OnInit {
  private svc = inject(FinanceService);
  invoices = signal<VendorInvoiceSummaryDTO[]>([]); detail = signal<VendorInvoiceDTO | null>(null);
  isLoading = signal(false); actionLoading = signal<string | null>(null);
  totalElements = signal(0); totalPages = signal(0); currentPage = signal(1);
  filterStatus = signal<VendorInvoiceStatus | ''>('');
  showDetail = signal(false);
  showApproveConfirm = signal(false); approveTarget = signal<string | null>(null);
  showPayModal = signal<string | null>(null); payAmount = signal('');
  showRejectModal = signal<string | null>(null); rejectReason = signal('');

  statusTabs: { label: string; value: VendorInvoiceStatus | '' }[] = [
    { label: 'All', value: '' }, { label: 'Received', value: 'RECEIVED' },
    { label: 'Approved', value: 'APPROVED' }, { label: 'Scheduled', value: 'SCHEDULED' },
    { label: 'Partially Paid', value: 'PARTIALLY_PAID' }, { label: 'Paid', value: 'PAID' },
    { label: 'Rejected', value: 'REJECTED' }, { label: 'Disputed', value: 'DISPUTED' },
  ];

  ngOnInit(): void { this.load(); }
  load(): void {
    this.isLoading.set(true);
    this.svc.getVendorInvoices({ status: this.filterStatus() as VendorInvoiceStatus || undefined },
      { page: this.currentPage()-1, size: 20, sort: 'dueDate', direction: 'asc' }).subscribe(p => {
      this.invoices.set(p.content); this.totalElements.set(p.totalElements); this.totalPages.set(p.totalPages); this.isLoading.set(false);
    });
  }
  setTab(v: VendorInvoiceStatus | ''): void { this.filterStatus.set(v); this.currentPage.set(1); this.load(); }
  openDetail(id: string): void { this.svc.getVendorInvoiceById(id).subscribe(d => { this.detail.set(d); this.showDetail.set(true); }); }
  approve(id: string): void {
    this.actionLoading.set(id);
    this.svc.approveVendorInvoice(id).subscribe(() => { this.actionLoading.set(null); this.showApproveConfirm.set(false); this.load(); this.showDetail.set(false); });
  }
  submitReject(): void {
    const id = this.showRejectModal(); if (!id) return;
    this.actionLoading.set(id); this.svc.rejectVendorInvoice(id, this.rejectReason() || 'Rejected').subscribe(() => { this.actionLoading.set(null); this.showRejectModal.set(null); this.load(); this.showDetail.set(false); });
  }
  submitPayment(): void {
    const id = this.showPayModal(); if (!id) return;
    this.actionLoading.set(id);
    this.svc.recordVendorPayment(id, { amount: parseFloat(this.payAmount()), paymentDate: new Date().toISOString().split('T')[0] }).subscribe(() => { this.actionLoading.set(null); this.showPayModal.set(null); this.load(); this.showDetail.set(false); });
  }
  goToPage(p: number): void { this.currentPage.set(p); this.load(); }
  prevPage(): void { if (this.currentPage() > 1) { this.currentPage.update(p => p-1); this.load(); } }
  nextPage(): void { if (this.currentPage() < this.totalPages()) { this.currentPage.update(p => p+1); this.load(); } }
  getStatusCfg(s: string) {
    const m: Record<string,any> = {
      RECEIVED:       { badge: 'bg-yellow-100 text-yellow-800', dot: 'bg-yellow-500', label: 'Received' },
      UNDER_REVIEW:   { badge: 'bg-blue-100 text-blue-800',     dot: 'bg-blue-500',   label: 'Under Review' },
      APPROVED:       { badge: 'bg-green-100 text-green-800',   dot: 'bg-green-500',  label: 'Approved' },
      SCHEDULED:      { badge: 'bg-indigo-100 text-indigo-800', dot: 'bg-indigo-500', label: 'Scheduled' },
      PARTIALLY_PAID: { badge: 'bg-cyan-100 text-cyan-800',     dot: 'bg-cyan-500',   label: 'Partial' },
      PAID:           { badge: 'bg-emerald-100 text-emerald-800',dot:'bg-emerald-500', label: 'Paid' },
      REJECTED:       { badge: 'bg-red-100 text-red-800',       dot: 'bg-red-500',    label: 'Rejected' },
      DISPUTED:       { badge: 'bg-orange-100 text-orange-800', dot: 'bg-orange-500', label: 'Disputed' },
      VOID:           { badge: 'bg-gray-100 text-gray-600',     dot: 'bg-gray-400',   label: 'Void' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', dot: 'bg-gray-400', label: s };
  }
  fmt(n: number): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
  daysUntil(iso: string): number { return Math.ceil((new Date(iso).getTime() - Date.now()) / 86400000); }
}
