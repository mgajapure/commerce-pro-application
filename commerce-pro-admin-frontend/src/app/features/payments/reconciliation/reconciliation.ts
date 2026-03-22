import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentService } from '../../../core/services/payment/payment.service';
import { ReconciliationSummaryDTO, ReconciliationReportDTO, GenerateReconciliationRequest } from '../../../core/models/payment/payment.model';

@Component({ selector: 'app-reconciliation', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './reconciliation.html', styleUrl: './reconciliation.scss' })
export class Reconciliation implements OnInit {
  private svc = inject(PaymentService);

  reports       = signal<ReconciliationSummaryDTO[]>([]);
  detail        = signal<ReconciliationReportDTO | null>(null);
  isLoading     = signal(false);
  actionLoading = signal<string | null>(null);
  showDetail    = signal(false);
  showGenModal  = signal(false);
  genForm       = signal<Partial<GenerateReconciliationRequest>>({});

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    this.svc.getReconciliationReports({ page: 0, size: 20, sort: 'createdAt', direction: 'desc' }).subscribe(p => {
      this.reports.set(p.content); this.isLoading.set(false);
    });
  }

  openDetail(id: string): void {
    this.svc.getReconciliationById(id).subscribe(r => { this.detail.set(r); this.showDetail.set(true); });
  }

  generate(): void {
    const f = this.genForm();
    if (!f.periodStart || !f.periodEnd) return;
    this.actionLoading.set('gen');
    this.svc.generateReconciliation(f as GenerateReconciliationRequest).subscribe(() => {
      this.actionLoading.set(null); this.showGenModal.set(false); this.genForm.set({}); this.load();
    });
  }

  close(id: string): void {
    this.actionLoading.set(id);
    this.svc.closeReconciliation(id).subscribe(() => {
      this.actionLoading.set(null); this.showDetail.set(false); this.load();
    });
  }

  patchGen(patch: any): void { this.genForm.update(v => ({ ...v, ...patch })); }

  getStatusConfig(s: string): { badge: string; label: string } {
    const m: Record<string, any> = {
      PENDING:                    { badge: 'bg-yellow-100 text-yellow-800', label: 'Pending' },
      RUNNING:                    { badge: 'bg-blue-100 text-blue-800',     label: 'Running' },
      COMPLETED:                  { badge: 'bg-green-100 text-green-800',   label: 'Completed' },
      COMPLETED_WITH_VARIANCE:    { badge: 'bg-orange-100 text-orange-800', label: 'Has Variance' },
      FAILED:                     { badge: 'bg-red-100 text-red-800',       label: 'Failed' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', label: s };
  }

  fmt(n: number): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
  fmtPeriod(s: string, e: string): string { return `${this.fmtDate(s)} – ${this.fmtDate(e)}`; }
}
