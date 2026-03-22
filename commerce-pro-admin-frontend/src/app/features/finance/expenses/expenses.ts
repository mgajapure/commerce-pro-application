import { Component, signal, computed, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FinanceService } from '../../../core/services/finance/finance.service';
import { ExpenseSummaryDTO, ExpenseDTO, ExpenseStatus, ExpenseCategoryDTO, ExpenseStatsDTO, CreateExpenseRequest } from '../../../core/models/finance/finance.model';

@Component({ selector: 'app-expenses', standalone: true, imports: [CommonModule, FormsModule, RouterModule], templateUrl: './expenses.html', styleUrl: './expenses.scss' })
export class Expenses implements OnInit {
  readonly parseFloat = parseFloat;
  private svc = inject(FinanceService);
  expenses = signal<ExpenseSummaryDTO[]>([]); stats = signal<ExpenseStatsDTO | null>(null);
  categories = signal<ExpenseCategoryDTO[]>([]); detail = signal<ExpenseDTO | null>(null);
  isLoading = signal(false); actionLoading = signal<string | null>(null);
  totalElements = signal(0); totalPages = signal(0); currentPage = signal(1);
  filterStatus = signal<ExpenseStatus | ''>(''); filterCategory = signal('');
  showDetail = signal(false); showCreateModal = signal(false);
  showRejectModal = signal<string | null>(null); rejectReason = signal('');
  newExpense = signal<Partial<CreateExpenseRequest>>({ currency: 'USD', isRecurring: false });
  statusTabs = [
    { label: 'All', value: '' as const }, { label: 'Pending', value: 'PENDING' as const },
    { label: 'Approved', value: 'APPROVED' as const }, { label: 'Paid', value: 'PAID' as const },
    { label: 'Rejected', value: 'REJECTED' as const },
  ];
  statsCards = computed(() => {
    const s = this.stats(); if (!s) return [];
    return [
      { label: 'Expenses MTD', value: this.fmt(s.totalMTD), icon: 'calendar3', bg: 'bg-blue-100', ic: 'text-blue-600' },
      { label: 'Expenses YTD', value: this.fmt(s.totalYTD), icon: 'graph-up', bg: 'bg-indigo-100', ic: 'text-indigo-600' },
      { label: 'Pending Review', value: String(s.pendingCount), icon: 'hourglass', bg: 'bg-amber-100', ic: 'text-amber-600' },
      { label: 'Pending Amount', value: this.fmt(s.pendingAmount), icon: 'wallet2', bg: 'bg-orange-100', ic: 'text-orange-600' },
    ];
  });
  ngOnInit(): void { this.svc.getExpenseStats().subscribe(s => this.stats.set(s)); this.svc.getExpenseCategories().subscribe(c => this.categories.set(c)); this.load(); }
  load(): void {
    this.isLoading.set(true);
    this.svc.getExpenses({ status: this.filterStatus() as ExpenseStatus || undefined, categoryId: this.filterCategory() || undefined },
      { page: this.currentPage()-1, size: 20, sort: 'expenseDate', direction: 'desc' }).subscribe(p => {
      this.expenses.set(p.content); this.totalElements.set(p.totalElements); this.totalPages.set(p.totalPages); this.isLoading.set(false);
    });
  }
  setTab(v: ExpenseStatus | ''): void { this.filterStatus.set(v); this.currentPage.set(1); this.load(); }
  openDetail(id: string): void { this.svc.getExpenseById(id).subscribe(d => { this.detail.set(d); this.showDetail.set(true); }); }
  approve(id: string): void { this.actionLoading.set(id); this.svc.approveExpense(id).subscribe(() => { this.actionLoading.set(null); this.load(); this.showDetail.set(false); this.svc.getExpenseStats().subscribe(s => this.stats.set(s)); }); }
  submitReject(): void {
    const id = this.showRejectModal(); if (!id) return;
    this.actionLoading.set(id); this.svc.rejectExpense(id, this.rejectReason() || 'Rejected').subscribe(() => { this.actionLoading.set(null); this.showRejectModal.set(null); this.load(); this.showDetail.set(false); });
  }
  markPaid(id: string): void { this.actionLoading.set(id); this.svc.markExpensePaid(id).subscribe(() => { this.actionLoading.set(null); this.load(); this.showDetail.set(false); }); }
  submitCreate(): void {
    const e = this.newExpense(); if (!e.title || !e.categoryId || !e.amount || !e.expenseDate) return;
    this.actionLoading.set('create'); this.svc.createExpense(e as CreateExpenseRequest).subscribe(() => { this.actionLoading.set(null); this.showCreateModal.set(false); this.newExpense.set({ currency: 'USD', isRecurring: false }); this.load(); });
  }
  patchNew(patch: Partial<CreateExpenseRequest>): void { this.newExpense.update(v => ({ ...v, ...patch })); }
  goToPage(p: number): void { this.currentPage.set(p); this.load(); }
  prevPage(): void { if (this.currentPage() > 1) { this.currentPage.update(p => p-1); this.load(); } }
  nextPage(): void { if (this.currentPage() < this.totalPages()) { this.currentPage.update(p => p+1); this.load(); } }
  getStatusCfg(s: string) {
    const m: Record<string,any> = {
      PENDING: { badge: 'bg-yellow-100 text-yellow-800', dot: 'bg-yellow-500', label: 'Pending' },
      UNDER_REVIEW: { badge: 'bg-blue-100 text-blue-800', dot: 'bg-blue-500', label: 'Under Review' },
      APPROVED: { badge: 'bg-green-100 text-green-800', dot: 'bg-green-500', label: 'Approved' },
      PAID: { badge: 'bg-emerald-100 text-emerald-800', dot: 'bg-emerald-500', label: 'Paid' },
      REJECTED: { badge: 'bg-red-100 text-red-800', dot: 'bg-red-500', label: 'Rejected' },
    };
    return m[s] ?? { badge: 'bg-gray-100 text-gray-700', dot: 'bg-gray-400', label: s };
  }
  fmt(n: number | null | undefined): string { return '$' + (n ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  fmtDate(iso?: string): string { if (!iso) return '—'; return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(new Date(iso)); }
}
