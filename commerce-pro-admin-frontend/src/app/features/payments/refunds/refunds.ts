import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { PaymentService } from '../../../core/services/payment/payment.service';
import { RefundRequestSummaryDTO, RefundRequestDTO, RefundStatus, RefundReason, CreateRefundRequest } from '../../../core/models/payment/payment.model';
import { PageParams } from '../../../core/models/common';

@Component({
  selector: 'app-refunds',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './refunds.html',
  styleUrl: './refunds.scss'
})
export class Refunds implements OnInit {
  readonly parseFloat = parseFloat;
  readonly Object = Object;
  private svc = inject(PaymentService);
  private destroy$ = new Subject<void>();

  refunds       = signal<RefundRequestSummaryDTO[]>([]);
  detail        = signal<RefundRequestDTO | null>(null);
  isLoading     = signal(false);
  actionLoading = signal<string | null>(null);
  totalElements = signal(0);
  totalPages    = signal(0);
  currentPage   = signal(1);
  itemsPerPage  = signal(20);
  filterStatus  = signal<RefundStatus | ''>('');
  filterOrder   = signal('');
  showDetail    = signal(false);
  showCreateModal = signal(false);
  showReviewModal = signal<'approve' | 'reject' | null>(null);
  reviewTarget  = signal<string | null>(null);
  reviewNotes   = signal('');
  processTarget = signal<string | null>(null);
  showProcessConfirm = signal(false);

  // Create refund form
  newRefund = signal<Partial<CreateRefundRequest>>({
    refundShipping: false
  });

  statusTabs: { label: string; value: RefundStatus | '' }[] = [
    { label: 'All', value: '' },
    { label: 'Pending', value: 'PENDING' },
    { label: 'Under Review', value: 'UNDER_REVIEW' },
    { label: 'Approved', value: 'APPROVED' },
    { label: 'Completed', value: 'COMPLETED' },
    { label: 'Rejected', value: 'REJECTED' },
    { label: 'Failed', value: 'FAILED' },
  ];

  reasonLabels: Record<RefundReason, string> = {
    CUSTOMER_REQUEST: 'Customer Request', DAMAGED_ITEM: 'Damaged Item',
    WRONG_ITEM_SHIPPED: 'Wrong Item', ITEM_NOT_RECEIVED: 'Not Received',
    QUALITY_ISSUE: 'Quality Issue', DUPLICATE_CHARGE: 'Duplicate Charge',
    ORDER_CANCELLED: 'Order Cancelled', PRICE_ADJUSTMENT: 'Price Adjustment',
    FRAUD: 'Fraud', OTHER: 'Other'
  };

  ngOnInit(): void { this.load(); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  load(): void {
    this.isLoading.set(true);
    const pageParams: PageParams = { page: this.currentPage() - 1, size: this.itemsPerPage(), sort: 'createdAt', direction: 'desc' };
    this.svc.getRefunds(this.filterStatus() as RefundStatus || undefined, this.filterOrder() || undefined, pageParams).subscribe(p => {
      this.refunds.set(p.content);
      this.totalElements.set(p.totalElements);
      this.totalPages.set(p.totalPages);
      this.isLoading.set(false);
    });
  }

  setTab(v: RefundStatus | ''): void { this.filterStatus.set(v); this.currentPage.set(1); this.load(); }

  openDetail(id: string): void {
    this.svc.getRefundById(id).subscribe(r => { this.detail.set(r); this.showDetail.set(true); });
  }

  openReview(id: string, type: 'approve' | 'reject'): void {
    this.reviewTarget.set(id); this.reviewNotes.set(''); this.showReviewModal.set(type);
  }

  submitReview(): void {
    const id = this.reviewTarget(); const type = this.showReviewModal();
    if (!id || !type) return;
    this.actionLoading.set(id);
    const obs = type === 'approve'
      ? this.svc.approveRefund(id, this.reviewNotes() || undefined)
      : this.svc.rejectRefund(id, this.reviewNotes() || undefined);
    obs.subscribe(() => {
      this.actionLoading.set(null); this.showReviewModal.set(null); this.detail.set(null); this.showDetail.set(false); this.load();
    });
  }

  openProcessConfirm(id: string): void { this.processTarget.set(id); this.showProcessConfirm.set(true); }

  confirmProcess(): void {
    const id = this.processTarget();
    if (!id) return;
    this.actionLoading.set(id);
    this.svc.processRefund(id).subscribe(() => {
      this.actionLoading.set(null); this.showProcessConfirm.set(false); this.showDetail.set(false); this.detail.set(null); this.load();
    });
  }

  submitCreate(): void {
    const r = this.newRefund();
    if (!r.paymentTransactionId || !r.refundAmount || !r.reason) return;
    this.actionLoading.set('create');
    this.svc.createRefund(r as CreateRefundRequest).subscribe(() => {
      this.actionLoading.set(null); this.showCreateModal.set(false); this.newRefund.set({ refundShipping: false }); this.load();
    });
  }

  patchNew(patch: Partial<CreateRefundRequest>): void { this.newRefund.update(v => ({ ...v, ...patch })); }

  goToPage(p: number): void { this.currentPage.set(p); this.load(); }
  prevPage(): void { if (this.currentPage() > 1) { this.currentPage.update(p => p - 1); this.load(); } }
  nextPage(): void { if (this.currentPage() < this.totalPages()) { this.currentPage.update(p => p + 1); this.load(); } }
  visiblePages(): (number | string)[] {
    const total = this.totalPages(), cur = this.currentPage(), pages: (number | string)[] = [];
    if (total <= 7) { for (let i = 1; i <= total; i++) pages.push(i); }
    else if (cur <= 3) pages.push(1, 2, 3, 4, '...', total);
    else if (cur >= total - 2) pages.push(1, '...', total - 3, total - 2, total - 1, total);
    else pages.push(1, '...', cur - 1, cur, cur + 1, '...', total);
    return pages;
  }

  getStatusConfig(s: string): { badge: string; label: string; dot: string } {
    const m: Record<string, { badge: string; label: string; dot: string }> = {
      PENDING:      { badge: 'bg-yellow-100 text-yellow-800', dot: 'bg-yellow-500', label: 'Pending' },
      UNDER_REVIEW: { badge: 'bg-blue-100 text-blue-800',    dot: 'bg-blue-500',   label: 'Under Review' },
      APPROVED:     { badge: 'bg-green-100 text-green-800',  dot: 'bg-green-500',  label: 'Approved' },
      PROCESSING:   { badge: 'bg-indigo-100 text-indigo-800',dot: 'bg-indigo-500', label: 'Processing' },
      COMPLETED:    { badge: 'bg-emerald-100 text-emerald-800',dot:'bg-emerald-500',label: 'Completed' },
      REJECTED:     { badge: 'bg-red-100 text-red-800',      dot: 'bg-red-500',    label: 'Rejected' },
      FAILED:       { badge: 'bg-rose-100 text-rose-800',    dot: 'bg-rose-500',   label: 'Failed' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', dot: 'bg-gray-400', label: s };
  }

  fmt(n: number): string {
    return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  fmtDate(iso?: string): string {
    if (!iso) return '—';
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
  }
}
