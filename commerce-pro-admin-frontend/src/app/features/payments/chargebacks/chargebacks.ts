import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentService } from '../../../core/services/payment/payment.service';
import { ChargebackSummaryDTO, ChargebackDisputeDTO, ChargebackStatus, ChargebackReason, CreateChargebackRequest, SubmitEvidenceRequest, ResolveChargebackRequest } from '../../../core/models/payment/payment.model';
import { PageParams } from '../../../core/models/common';

@Component({ selector: 'app-chargebacks', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './chargebacks.html', styleUrl: './chargebacks.scss' })
export class Chargebacks implements OnInit {
  readonly parseFloat = parseFloat;
  readonly Object = Object;
  private svc = inject(PaymentService);

  disputes      = signal<ChargebackSummaryDTO[]>([]);
  deadlines     = signal<ChargebackSummaryDTO[]>([]);
  detail        = signal<ChargebackDisputeDTO | null>(null);
  isLoading     = signal(false);
  actionLoading = signal<string | null>(null);
  totalElements = signal(0);
  totalPages    = signal(0);
  currentPage   = signal(1);
  filterStatus  = signal<ChargebackStatus | ''>('');
  showDetail    = signal(false);
  showIntakeModal  = signal(false);
  showEvidenceModal = signal<string | null>(null);
  showResolveModal  = signal<string | null>(null);

  newDispute = signal<Partial<CreateChargebackRequest>>({ currency: 'USD' });
  evidenceForm = signal<Partial<SubmitEvidenceRequest>>({});
  resolveForm  = signal<Partial<ResolveChargebackRequest>>({});

  statusTabs: { label: string; value: ChargebackStatus | '' }[] = [
    { label: 'All', value: '' }, { label: 'Received', value: 'RECEIVED' },
    { label: 'Under Review', value: 'UNDER_REVIEW' }, { label: 'Evidence Required', value: 'EVIDENCE_REQUIRED' },
    { label: 'Submitted', value: 'EVIDENCE_SUBMITTED' }, { label: 'Won', value: 'WON' },
    { label: 'Lost', value: 'LOST' }, { label: 'Accepted', value: 'ACCEPTED' },
  ];

  reasonLabels: Record<ChargebackReason, string> = {
    FRAUDULENT: 'Fraudulent', DUPLICATE: 'Duplicate', PRODUCT_NOT_RECEIVED: 'Not Received',
    PRODUCT_UNACCEPTABLE: 'Unacceptable', CREDIT_NOT_PROCESSED: 'Credit Not Processed',
    GENERAL: 'General', SUBSCRIPTION_CANCELLED: 'Subscription Cancelled', UNRECOGNIZED: 'Unrecognized',
  };

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    const p: PageParams = { page: this.currentPage() - 1, size: 20, sort: 'createdAt', direction: 'desc' };
    this.svc.getChargebacks(this.filterStatus() as ChargebackStatus || undefined, undefined, p).subscribe(r => {
      this.disputes.set(r.content); this.totalElements.set(r.totalElements); this.totalPages.set(r.totalPages); this.isLoading.set(false);
    });
    this.svc.getDeadlineAlerts().subscribe(d => this.deadlines.set(d));
  }

  setTab(v: ChargebackStatus | ''): void { this.filterStatus.set(v); this.currentPage.set(1); this.load(); }

  openDetail(id: string): void { this.svc.getChargebackById(id).subscribe(d => { this.detail.set(d); this.showDetail.set(true); }); }

  submitIntake(): void {
    const r = this.newDispute();
    if (!r.orderId || !r.reason || !r.disputedAmount || !r.responseDeadline) return;
    this.actionLoading.set('intake');
    this.svc.intakeChargeback(r as CreateChargebackRequest).subscribe(() => {
      this.actionLoading.set(null); this.showIntakeModal.set(false); this.newDispute.set({ currency: 'USD' }); this.load();
    });
  }

  submitEvidence(): void {
    const id = this.showEvidenceModal(); const f = this.evidenceForm();
    if (!id || !f.evidenceNotes) return;
    this.actionLoading.set(id);
    this.svc.submitEvidence(id, f as SubmitEvidenceRequest).subscribe(() => {
      this.actionLoading.set(null); this.showEvidenceModal.set(null); this.showDetail.set(false); this.load();
    });
  }

  accept(id: string): void {
    this.actionLoading.set(id);
    this.svc.acceptChargeback(id).subscribe(() => { this.actionLoading.set(null); this.showDetail.set(false); this.load(); });
  }

  submitResolve(): void {
    const id = this.showResolveModal(); const f = this.resolveForm();
    if (!id || f.won === undefined) return;
    this.actionLoading.set(id);
    this.svc.resolveChargeback(id, f as ResolveChargebackRequest).subscribe(() => {
      this.actionLoading.set(null); this.showResolveModal.set(null); this.showDetail.set(false); this.load();
    });
  }

  patchNew(patch: any): void { this.newDispute.update(v => ({ ...v, ...patch })); }
  patchEvidence(patch: any): void { this.evidenceForm.update(v => ({ ...v, ...patch })); }
  patchResolve(patch: any): void { this.resolveForm.update(v => ({ ...v, ...patch })); }

  daysRemaining(iso?: string): number | null {
    if (!iso) return null;
    return Math.max(0, Math.ceil((new Date(iso).getTime() - Date.now()) / 86400000));
  }

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
      RECEIVED:           { badge: 'bg-yellow-100 text-yellow-800', dot: 'bg-yellow-500', label: 'Received' },
      UNDER_REVIEW:       { badge: 'bg-blue-100 text-blue-800',     dot: 'bg-blue-500',   label: 'Under Review' },
      EVIDENCE_REQUIRED:  { badge: 'bg-orange-100 text-orange-800', dot: 'bg-orange-500', label: 'Evidence Required' },
      EVIDENCE_SUBMITTED: { badge: 'bg-indigo-100 text-indigo-800', dot: 'bg-indigo-500', label: 'Submitted' },
      WON:                { badge: 'bg-green-100 text-green-800',   dot: 'bg-green-500',  label: 'Won' },
      LOST:               { badge: 'bg-red-100 text-red-800',       dot: 'bg-red-500',    label: 'Lost' },
      ACCEPTED:           { badge: 'bg-gray-100 text-gray-700',     dot: 'bg-gray-400',   label: 'Accepted' },
      WITHDRAWN:          { badge: 'bg-gray-100 text-gray-500',     dot: 'bg-gray-300',   label: 'Withdrawn' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', dot: 'bg-gray-400', label: s };
  }

  fmt(n: number): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
}
