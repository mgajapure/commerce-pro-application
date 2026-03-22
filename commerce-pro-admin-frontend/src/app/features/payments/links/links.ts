import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentService } from '../../../core/services/payment/payment.service';
import { PaymentLinkSummaryDTO, PaymentLinkDTO, PaymentLinkStatus, CreatePaymentLinkRequest } from '../../../core/models/payment/payment.model';
import { PageParams } from '../../../core/models/common';

@Component({ selector: 'app-links', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './links.html', styleUrl: './links.scss' })
export class Links implements OnInit {
  readonly parseFloat = parseFloat;
  readonly parseInt = parseInt;
  private svc = inject(PaymentService);

  links         = signal<PaymentLinkSummaryDTO[]>([]);
  detail        = signal<PaymentLinkDTO | null>(null);
  isLoading     = signal(false);
  actionLoading = signal<string | null>(null);
  totalElements = signal(0);
  totalPages    = signal(0);
  currentPage   = signal(1);
  filterStatus  = signal<PaymentLinkStatus | ''>('');
  showDetail    = signal(false);
  showCreateModal = signal(false);
  copiedId      = signal<string | null>(null);
  newLink       = signal<Partial<CreatePaymentLinkRequest>>({ currency: 'USD', isOneTime: true });

  statusTabs: { label: string; value: PaymentLinkStatus | '' }[] = [
    { label: 'All', value: '' }, { label: 'Active', value: 'ACTIVE' },
    { label: 'Paid', value: 'PAID' }, { label: 'Expired', value: 'EXPIRED' }, { label: 'Cancelled', value: 'CANCELLED' },
  ];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    const p: PageParams = { page: this.currentPage() - 1, size: 20, sort: 'createdAt', direction: 'desc' };
    this.svc.getPaymentLinks(this.filterStatus() as PaymentLinkStatus || undefined, p).subscribe(r => {
      this.links.set(r.content); this.totalElements.set(r.totalElements); this.totalPages.set(r.totalPages); this.isLoading.set(false);
    });
  }

  setTab(v: PaymentLinkStatus | ''): void { this.filterStatus.set(v); this.currentPage.set(1); this.load(); }

  openDetail(id: string): void {
    this.svc.getPaymentLinkById(id).subscribe(d => { this.detail.set(d); this.showDetail.set(true); });
  }

  create(): void {
    const f = this.newLink();
    if (!f.title || !f.amount) return;
    this.actionLoading.set('create');
    this.svc.createPaymentLink(f as CreatePaymentLinkRequest).subscribe(() => {
      this.actionLoading.set(null); this.showCreateModal.set(false); this.newLink.set({ currency: 'USD', isOneTime: true }); this.load();
    });
  }

  deactivate(id: string, e: Event): void {
    e.stopPropagation();
    this.actionLoading.set(id);
    this.svc.deactivatePaymentLink(id).subscribe(() => {
      this.actionLoading.set(null);
      if (this.showDetail()) this.svc.getPaymentLinkById(id).subscribe(d => this.detail.set(d));
      this.load();
    });
  }

  copyUrl(url: string, id: string, e: Event): void {
    e.stopPropagation();
    navigator.clipboard.writeText(url).then(() => {
      this.copiedId.set(id);
      setTimeout(() => this.copiedId.set(null), 2000);
    });
  }

  patchNew(patch: any): void { this.newLink.update(v => ({ ...v, ...patch })); }

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

  getStatusConfig(s: string): { badge: string; label: string; dot: string } {
    const m: Record<string, any> = {
      ACTIVE:    { badge: 'bg-green-100 text-green-800',  dot: 'bg-green-500',  label: 'Active' },
      PAID:      { badge: 'bg-blue-100 text-blue-800',    dot: 'bg-blue-500',   label: 'Paid' },
      EXPIRED:   { badge: 'bg-yellow-100 text-yellow-800',dot: 'bg-yellow-500', label: 'Expired' },
      CANCELLED: { badge: 'bg-gray-100 text-gray-700',    dot: 'bg-gray-400',   label: 'Cancelled' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', dot: 'bg-gray-400', label: s };
  }

  expiresInDays(iso?: string): number | null {
    if (!iso) return null;
    return Math.max(0, Math.ceil((new Date(iso).getTime() - Date.now()) / 86400000));
  }

  fmt(n: number): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
}
