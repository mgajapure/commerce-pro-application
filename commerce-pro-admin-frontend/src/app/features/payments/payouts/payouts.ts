import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentService } from '../../../core/services/payment/payment.service';
import { PayoutSummaryDTO, PayoutDTO, PayoutStatus, CreatePayoutRequest } from '../../../core/models/payment/payment.model';
import { PageParams } from '../../../core/models/common';

@Component({ selector: 'app-payouts', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './payouts.html', styleUrl: './payouts.scss' })
export class Payouts implements OnInit {
  private svc = inject(PaymentService);

  payouts       = signal<PayoutSummaryDTO[]>([]);
  detail        = signal<PayoutDTO | null>(null);
  isLoading     = signal(false);
  actionLoading = signal<string | null>(null);
  totalElements = signal(0);
  totalPages    = signal(0);
  currentPage   = signal(1);
  filterStatus  = signal<PayoutStatus | ''>('');
  showDetail    = signal(false);
  showCreateModal = signal(false);
  showCancelModal = signal<string | null>(null);
  cancelReason  = signal('');
  newPayout     = signal<Partial<CreatePayoutRequest>>({});

  statusTabs: { label: string; value: PayoutStatus | ''; icon: string }[] = [
    { label: 'All', value: '', icon: 'list' },
    { label: 'Pending', value: 'PENDING', icon: 'clock' },
    { label: 'Approved', value: 'APPROVED', icon: 'check-circle' },
    { label: 'Processing', value: 'PROCESSING', icon: 'gear' },
    { label: 'In Transit', value: 'IN_TRANSIT', icon: 'send' },
    { label: 'Completed', value: 'COMPLETED', icon: 'bank' },
    { label: 'Failed', value: 'FAILED', icon: 'x-circle' },
  ];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    const p: PageParams = { page: this.currentPage() - 1, size: 20, sort: 'createdAt', direction: 'desc' };
    this.svc.getPayouts(this.filterStatus() as PayoutStatus || undefined, p).subscribe(r => {
      this.payouts.set(r.content); this.totalElements.set(r.totalElements); this.totalPages.set(r.totalPages); this.isLoading.set(false);
    });
  }

  setTab(v: PayoutStatus | ''): void { this.filterStatus.set(v); this.currentPage.set(1); this.load(); }

  openDetail(id: string): void {
    this.svc.getPayoutById(id).subscribe(d => { this.detail.set(d); this.showDetail.set(true); });
  }

  create(): void {
    const f = this.newPayout();
    if (!f.periodStart || !f.periodEnd) return;
    this.actionLoading.set('create');
    this.svc.createPayout(f as CreatePayoutRequest).subscribe(() => {
      this.actionLoading.set(null); this.showCreateModal.set(false); this.newPayout.set({}); this.load();
    });
  }

  approve(id: string): void {
    this.actionLoading.set(id);
    this.svc.approvePayout(id).subscribe(() => { this.actionLoading.set(null); this.refreshDetail(id); this.load(); });
  }

  initiate(id: string): void {
    this.actionLoading.set(id);
    this.svc.initiatePayout(id).subscribe(() => { this.actionLoading.set(null); this.refreshDetail(id); this.load(); });
  }

  complete(id: string): void {
    this.actionLoading.set(id);
    this.svc.completePayout(id).subscribe(() => { this.actionLoading.set(null); this.refreshDetail(id); this.load(); });
  }

  openCancel(id: string): void { this.showCancelModal.set(id); this.cancelReason.set(''); }

  confirmCancel(): void {
    const id = this.showCancelModal();
    if (!id) return;
    this.actionLoading.set(id);
    this.svc.cancelPayout(id, this.cancelReason()).subscribe(() => {
      this.actionLoading.set(null); this.showCancelModal.set(null); this.showDetail.set(false); this.load();
    });
  }

  refreshDetail(id: string): void {
    if (this.showDetail()) this.svc.getPayoutById(id).subscribe(d => this.detail.set(d));
  }

  patchNew(patch: any): void { this.newPayout.update(v => ({ ...v, ...patch })); }

  goToPage(p: number): void { this.currentPage.set(p); this.load(); }
  prevPage(): void { if (this.currentPage() > 1) { this.currentPage.update(p => p - 1); this.load(); } }
  nextPage(): void { if (this.currentPage() < this.totalPages()) { this.currentPage.update(p => p + 1); this.load(); } }
  visiblePages(): (number | string)[] {
    const total = this.totalPages(), cur = this.currentPage(), pages: (number | string)[] = [];
    if (total <= 7) { for (let i = 1; i <= total; i++) pages.push(i); }
    else if (cur <= 3) pages.push(1, 2, 3, 4, '...', total);
    else if (cur >= total - 2) pages.push(1, '...', total-3, total-2, total-1, total);
    else pages.push(1, '...', cur-1, cur, cur+1, '...', total);
    return pages;
  }

  getStatusConfig(s: string): { badge: string; label: string; dot: string; step: number } {
    const m: Record<string, any> = {
      PENDING:    { badge: 'bg-yellow-100 text-yellow-800', dot: 'bg-yellow-500', label: 'Pending',    step: 1 },
      APPROVED:   { badge: 'bg-blue-100 text-blue-800',    dot: 'bg-blue-500',   label: 'Approved',   step: 2 },
      PROCESSING: { badge: 'bg-indigo-100 text-indigo-800',dot: 'bg-indigo-500', label: 'Processing', step: 3 },
      IN_TRANSIT: { badge: 'bg-purple-100 text-purple-800',dot: 'bg-purple-500', label: 'In Transit', step: 4 },
      COMPLETED:  { badge: 'bg-green-100 text-green-800',  dot: 'bg-green-500',  label: 'Completed',  step: 5 },
      FAILED:     { badge: 'bg-red-100 text-red-800',      dot: 'bg-red-500',    label: 'Failed',     step: 0 },
      CANCELLED:  { badge: 'bg-gray-100 text-gray-700',    dot: 'bg-gray-400',   label: 'Cancelled',  step: 0 },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', dot: 'bg-gray-400', label: s, step: 0 };
  }

  fmt(n: number): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso)); }
  fmtDateShort(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
}
