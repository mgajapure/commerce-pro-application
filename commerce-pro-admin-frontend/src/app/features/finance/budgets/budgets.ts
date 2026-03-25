import { Component, signal, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { BudgetDTO, BudgetStatus } from '../../../core/models/finance/finance.model';
import { AiService } from '../../../core/services/ai/ai.service';
import { AiBudgetAnomalyResult } from '../../../core/models/ai/ai.model';
import { Subject, takeUntil } from 'rxjs';

@Component({ selector: 'app-budgets', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './budgets.html', styleUrl: './budgets.scss' })
export class Budgets implements OnInit, OnDestroy {
  private svc    = inject(FinanceService);
  private aiSvc  = inject(AiService);
  private destroy$ = new Subject<void>();
  budgets = signal<BudgetDTO[]>([]); detail = signal<BudgetDTO | null>(null);
  isLoading = signal(false); detailLoading = signal(false); actionLoading = signal<string | null>(null);
  totalElements = signal(0);

  // Accordion state
  expandedId = signal<string | null>(null);

  // AI Anomaly
  anomalyResults = signal<Record<string, AiBudgetAnomalyResult>>({});
  anomalyLoading = signal<string | null>(null);
  showAnomalyModal = signal(false);
  anomalyModalResult = signal<AiBudgetAnomalyResult | null>(null);
  currentYear = new Date().getFullYear();
  filterYear = signal(this.currentYear);
  readonly Math: typeof Math = Math;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading.set(true);
    this.svc.getBudgets(this.filterYear()).subscribe(p => { this.budgets.set(p.content); this.totalElements.set(p.totalElements); this.isLoading.set(false); });
  }

  toggleAccordion(id: string): void {
    if (this.expandedId() === id) {
      this.expandedId.set(null);
      this.detail.set(null);
    } else {
      this.expandedId.set(id);
      this.detail.set(null);
      this.detailLoading.set(true);
      this.svc.getBudgetVariance(id).subscribe(d => { this.detail.set(d); this.detailLoading.set(false); });
    }
  }

  approve(id: string): void {
    this.actionLoading.set(id); this.svc.approveBudget(id).subscribe(() => { this.actionLoading.set(null); this.load(); });
  }

  getStatusCfg(s: string) {
    const m: Record<string, any> = {
      DRAFT: { badge: 'bg-gray-100 text-gray-700', label: 'Draft' },
      PENDING_APPROVAL: { badge: 'bg-yellow-100 text-yellow-800', label: 'Pending Approval' },
      APPROVED: { badge: 'bg-green-100 text-green-800', label: 'Approved' },
      ACTIVE: { badge: 'bg-blue-100 text-blue-800', label: 'Active' },
      CLOSED: { badge: 'bg-gray-100 text-gray-600', label: 'Closed' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', label: s };
  }

  varianceClass(v: number): string { return v > 0 ? 'text-red-600' : v < 0 ? 'text-green-600' : 'text-gray-400'; }
  fmt(n: number | null | undefined): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }

  analyseAnomaly(budgetId: string): void {
    this.anomalyLoading.set(budgetId);
    this.aiSvc.analyseBudgetAnomaly(budgetId).pipe(takeUntil(this.destroy$)).subscribe({
      next: r => {
        this.anomalyResults.update(m => ({ ...m, [budgetId]: r }));
        this.anomalyModalResult.set(r);
        this.showAnomalyModal.set(true);
        this.anomalyLoading.set(null);
      },
      error: () => this.anomalyLoading.set(null)
    });
  }

  getAnomaly(id: string): AiBudgetAnomalyResult | null { return this.anomalyResults()[id] ?? null; }

  anomalySeverityBadge(s: string): string {
    if (s === 'CRITICAL') return 'bg-red-100 text-red-700';
    if (s === 'HIGH') return 'bg-orange-100 text-orange-700';
    if (s === 'MEDIUM') return 'bg-yellow-100 text-yellow-700';
    return 'bg-gray-100 text-gray-600';
  }
}
