import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { BudgetDTO, BudgetStatus } from '../../../core/models/finance/finance.model';

@Component({ selector: 'app-budgets', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './budgets.html', styleUrl: './budgets.scss' })
export class Budgets implements OnInit {
  private svc = inject(FinanceService);
  budgets = signal<BudgetDTO[]>([]); detail = signal<BudgetDTO | null>(null);
  isLoading = signal(false); actionLoading = signal<string | null>(null);
  showDetail = signal(false); totalElements = signal(0);
  currentYear = new Date().getFullYear();
  filterYear = signal(this.currentYear);
  // expose global Math for template
  readonly Math: typeof Math = Math;
  ngOnInit(): void { this.load(); }
  load(): void {
    this.isLoading.set(true);
    this.svc.getBudgets(this.filterYear()).subscribe(p => { this.budgets.set(p.content); this.totalElements.set(p.totalElements); this.isLoading.set(false); });
  }
  openDetail(id: string): void { this.svc.getBudgetVariance(id).subscribe(d => { this.detail.set(d); this.showDetail.set(true); }); }
  approve(id: string): void {
    this.actionLoading.set(id); this.svc.approveBudget(id).subscribe(() => { this.actionLoading.set(null); this.load(); this.showDetail.set(false); });
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
}
